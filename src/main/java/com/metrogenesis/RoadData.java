package com.metrogenesis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Road network data persisted to world save.
 * Stores segments as connections between chunk-aligned nodes.
 *
 * Pattern follows ZoneData.java — using vanilla SavedData mechanism.
 * Each overworld dimension gets its own RoadData instance.
 *
 * Data structure:
 *   segments: [
 *     { fromX, fromZ, toX, toZ, type },
 *     ...
 *   ]
 *
 * @see ZoneData
 */
public class RoadData extends SavedData {
    private static final String DATA_NAME = "metrogenesis_roads";

    /**
     * A node in the road network — block-level coordinates.
     * (v2: changed from chunk-aligned to block-level for finer road placement)
     */
    public record NodePos(int blockX, int blockZ) {}

    /**
     * A road segment connecting two block-level nodes.
     * type: 0=dirt, 1=stone, 2=brick (v1 uses only 0)
     * curvature: [-1.0, 1.0]，0=直线，正=右弯，负=左弯（Phase 1 新增）
     */
    public record RoadSegment(NodePos from, NodePos to, int type, float curvature) {
        /**
         * Creates a canonical segment: always stores the "smaller" node as 'from'
         * so that (A,B) and (B,A) are treated as the same segment.
         */
        public RoadSegment {
            // Ensure canonical ordering: sort by blockX, then blockZ
            if (from.blockX() > to.blockX() ||
                (from.blockX() == to.blockX() && from.blockZ() > to.blockZ())) {
                NodePos tmp = from;
                from = to;
                to = tmp;
            }
        }

        /**
         * Convenience constructor for straight segments (curvature = 0).
         */
        public RoadSegment(NodePos from, NodePos to, int type) {
            this(from, to, type, 0f);
        }
    }

    private final Set<RoadSegment> segments = new HashSet<>();

    public RoadData() {
        // Default constructor for SavedData factory
    }

    /**
     * Deserialize from NBT — called by SavedData on world load.
     * Supports both legacy chunk-coordinate format (key "cx") and
     * new block-coordinate format (key "bx").
     */
    public static RoadData load(CompoundTag tag) {
        RoadData data = new RoadData();
        ListTag list = tag.getList("segments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            NodePos from, to;
            if (entry.contains("cx")) {
                // Legacy chunk-coordinate format: convert to block coordinates
                from = new NodePos(entry.getInt("cx") * 16, entry.getInt("cz") * 16);
                to   = new NodePos(entry.getInt("tx") * 16, entry.getInt("tz") * 16);
            } else {
                // New block-coordinate format
                from = new NodePos(entry.getInt("bx"), entry.getInt("bz"));
                to   = new NodePos(entry.getInt("tx"), entry.getInt("tz"));
            }
            int type = entry.getInt("type");
            float curvature = entry.contains("curvature") ? entry.getFloat("curvature") : 0f;
            data.segments.add(new RoadSegment(from, to, type, curvature));
        }
        return data;
    }

    /**
     * Serialize to NBT — called by vanilla save system.
     * Uses block-coordinate keys (bx/bz/tx/tz) and curvature.
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (RoadSegment seg : segments) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("bx", seg.from().blockX());
            entry.putInt("bz", seg.from().blockZ());
            entry.putInt("tx", seg.to().blockX());
            entry.putInt("tz", seg.to().blockZ());
            entry.putInt("type", seg.type());
            // Phase 1: 保存曲率（旧存档加载时不含此键，默认为 0）
            if (seg.curvature() != 0f) {
                entry.putFloat("curvature", seg.curvature());
            }
            list.add(entry);
        }
        tag.put("segments", list);
        return tag;
    }

    // ══ Mutation methods ════════════════════════════════

    /**
     * Add a road segment and mark data as dirty (needs save).
     */
    public void addSegment(RoadSegment seg) {
        segments.add(seg);
        setDirty();
    }

    /**
     * Remove a road segment and mark data as dirty.
     */
    public void removeSegment(RoadSegment seg) {
        segments.remove(seg);
        setDirty();
    }

    /**
     * Remove all segments connected to the given node.
     */
    public void removeNode(NodePos node) {
        segments.removeIf(seg ->
            seg.from().equals(node) || seg.to().equals(node));
        setDirty();
    }

    /**
     * Remove all segments of a given type.
     */
    public void removeByType(int type) {
        segments.removeIf(seg -> seg.type() == type);
        setDirty();
    }

    // ══ Query methods ═══════════════════════════════════

    /**
     * Get all road segments (unmodifiable view).
     */
    public Set<RoadSegment> getSegments() {
        return Collections.unmodifiableSet(segments);
    }

    /**
     * Get total number of segments.
     */
    public int segmentCount() {
        return segments.size();
    }

    /**
     * Check if a segment exists between two nodes (ignoring type and curvature).
     */
    public boolean hasSegment(NodePos from, NodePos to) {
        // 需要遍历比较，因为 RoadSegment 是 record，equals 包含所有字段（含 curvature）
        for (RoadSegment seg : segments) {
            if (seg.from().equals(from) && seg.to().equals(to)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all segments connected to a specific node.
     */
    public Set<RoadSegment> getSegmentsForNode(NodePos node) {
        Set<RoadSegment> result = new HashSet<>();
        for (RoadSegment seg : segments) {
            if (seg.from().equals(node) || seg.to().equals(node)) {
                result.add(seg);
            }
        }
        return result;
    }

    /**
     * Get all unique nodes in the road network.
     */
    public Set<NodePos> getAllNodes() {
        Set<NodePos> nodes = new HashSet<>();
        for (RoadSegment seg : segments) {
            nodes.add(seg.from());
            nodes.add(seg.to());
        }
        return nodes;
    }

    // ══ Factory ════════════════════════════════════════

    /**
     * Get or create the RoadData for the given server level.
     * Call this on the server thread only (e.g., in commands or server events).
     */
    public static RoadData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            RoadData::load,
            RoadData::new,
            DATA_NAME
        );
    }
}
