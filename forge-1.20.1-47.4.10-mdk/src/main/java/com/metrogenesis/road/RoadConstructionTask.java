package com.metrogenesis.road;

import com.metrogenesis.RoadData;
import com.metrogenesis.RoadData.NodePos;
import com.metrogenesis.structurize.management.Manager;
import com.metrogenesis.structurize.util.ChangeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * A tick-by-tick road construction task that visually places one "column"
 * of road blocks per tick, with block-breaking particles for visual feedback.
 *
 * ── Flow ─────────────────────────────────────────────────
 * 1. Created on right-click confirmation in road mode
 * 2. Registered into MetroGenesis.onServerTick
 * 3. Each tick: place one 16-wide strip → spawn particles → advance step
 * 4. On last step: rebuild both endpoint nodes + cleanup
 *
 * ── Design ────────────────────────────────────────────────
 * Inspired by OpenTTD's instant-placement visual feedback,
 * but adapted for Minecraft's block-level granularity.
 */
public class RoadConstructionTask {

    private static final BlockState ROAD_BASE    = Blocks.DIRT.defaultBlockState();

    /** Records block changes for undo/redo. */
    private final ChangeStorage changeStorage;

    private final ServerLevel level;
    private final RoadData roadData;
    private final NodePos from;
    private final NodePos to;
    private final int roadType;

    /** Pre-computed step positions (world coordinates). */
    private final List<Step> steps = new ArrayList<>();

    /** Current step index. */
    private int currentStep = 0;

    /** Total blocks placed (for summary). */
    private int totalBlocksPlaced = 0;

    public RoadConstructionTask(ServerLevel level, RoadData roadData,
                                NodePos from, NodePos to, int roadType,
                                java.util.UUID playerUUID) {
        this.level = level;
        this.roadData = roadData;
        this.from = from;
        this.to = to;
        this.roadType = roadType;
        this.changeStorage = new ChangeStorage(
                Component.translatable("metrogenesis.road.place", from, to), playerUUID);
        precomputeSteps();
    }

    // ══ Precompute path ═════════════════════════════════

    /**
     * Pre-compute the step positions from from to to.
     * Same algorithm as {@link RoadBuilder#placeRoad}, but stored for tick-by-tick playback.
     */
    private void precomputeSteps() {
        int fromX = from.blockX(), fromZ = from.blockZ();
        int toX   = to.blockX(),   toZ   = to.blockZ();
        int rw = RoadBuilder.getRoadWidth();

        int dx = toX - fromX;
        int dz = toZ - fromZ;
        int total = Math.max(Math.abs(dx), Math.abs(dz));
        if (total == 0) total = 1;

        float hFrom = RoadBuilder.getTrueTerrainHeight(level, fromX, fromZ);
        float hTo   = RoadBuilder.getTrueTerrainHeight(level, toX, toZ);

        int halfW = rw / 2;

        for (int i = 0; i <= total; i++) {
            float t = (float) i / total;
            int bx = fromX + Math.round(dx * t);
            int bz = fromZ + Math.round(dz * t);
            bx = clamp(bx, Math.min(fromX, toX), Math.max(fromX, toX));
            bz = clamp(bz, Math.min(fromZ, toZ), Math.max(fromZ, toZ));

            float targetH = hFrom + (hTo - hFrom) * t;
            int roadFloor = (int) Math.floor(targetH);

            List<BlockPlacement> placements = new ArrayList<>();

            for (int w = -halfW; w <= halfW; w++) {
                int wx, wz;
                if (dx != 0) {
                    wx = bx;
                    wz = bz + w;
                } else {
                    wx = bx + w;
                    wz = bz;
                }

                int terrainH = RoadBuilder.getTrueTerrainHeight(level, wx, wz);

                // Fill base below road floor
                if (terrainH < roadFloor) {
                    for (int y = terrainH; y < roadFloor; y++) {
                        placements.add(new BlockPlacement(wx, y, wz, ROAD_BASE));
                    }
                }

                // Surface block — 跳过端点（由 rebuildNode 填充交叉口图案）
                boolean isEndpoint = (i == 0 || i == total) && total > 1;
                if (!isEndpoint) {
                    placements.add(new BlockPlacement(wx, roadFloor, wz, RoadBuilder.ROAD_SURFACE));
                }
            }

            steps.add(new Step(bx, bz, roadFloor, Collections.unmodifiableList(placements)));
        }
    }

    // ══ Tick execution ═══════════════════════════════════

    /**
     * Execute one step's worth of block placements + particles.
     * @return true if the entire road is complete
     */
    public boolean tick() {
        if (currentStep >= steps.size()) {
            finish();
            return true;
        }

        Step step = steps.get(currentStep);

        // Place blocks with undo tracking + overwrite protection
        for (BlockPlacement bp : step.placements) {
            // 跳过已是道路方块的格位（防止新路覆盖旧路）
            BlockState existing = level.getBlockState(bp.pos());
            if (existing == RoadBuilder.ROAD_SURFACE || existing == RoadBuilder.ROAD_CURB) {
                continue;
            }
            changeStorage.addPreviousDataFor(bp.pos(), level);
            level.setBlock(bp.pos(), bp.state(), 3);
            changeStorage.addPostDataFor(bp.pos(), level);
            totalBlocksPlaced++;
        }

        // Particles: dust effect at the road surface
        spawnParticles(step);

        currentStep++;
        return currentStep >= steps.size();
    }

    /** Returns true when all steps are done. */
    public boolean isComplete() {
        return currentStep >= steps.size();
    }

    // ══ Particles ══════════════════════════════════════

    /**
     * Spawn stone-dust particles along the road surface for visual feedback.
     */
    private void spawnParticles(Step step) {
        // Stone-gray dust particle: RGB (0.7, 0.65, 0.55), size 0.6
        DustParticleOptions dust = new DustParticleOptions(
                new org.joml.Vector3f(0.72f, 0.66f, 0.54f), 0.6f);

        int placementsSize = step.placements.size();
        int surfaceCount = Math.min(RoadBuilder.getRoadWidth(), placementsSize);
        int surfaceOffset = placementsSize - surfaceCount;

        for (int i = 0; i < surfaceCount; i += 2) {  // every other block to avoid particle spam
            BlockPlacement bp = step.placements.get(surfaceOffset + i);
            BlockPos pos = bp.pos();
            double px = pos.getX() + 0.5;
            double py = pos.getY() + 1.05;
            double pz = pos.getZ() + 0.5;
            level.sendParticles(dust, px, py, pz, 2,
                    0.08, 0.05, 0.08, 0.02);
        }
    }

    // ══ Finalization ═══════════════════════════════════

    /** Called when all steps are complete. */
    private void finish() {
        // Rebuild both endpoint intersections (same as commitSegment)
        RoadBuilder.rebuildNode(level, from, roadData, changeStorage);
        RoadBuilder.rebuildNode(level, to, roadData, changeStorage);

        // Rebuild neighbors
        Set<NodePos> affected = new HashSet<>();
        RoadBuilder.collectNeighbors(roadData, from, affected);
        RoadBuilder.collectNeighbors(roadData, to, affected);
        for (NodePos n : affected) {
            if (!n.equals(from) && !n.equals(to)) {
                RoadBuilder.rebuildNode(level, n, roadData, changeStorage);
            }
        }

        // Submit to undo/redo cache — reset iterator first
        changeStorage.resetUnRedo();
        Manager.addToUndoRedoCache(changeStorage);
    }

    // ══ Accessors ═══════════════════════════════════

    public int getCurrentStep()     { return currentStep; }
    public int getTotalSteps()      { return steps.size(); }
    public int getTotalBlocksPlaced() { return totalBlocksPlaced; }
    public NodePos getFrom()        { return from; }
    public NodePos getTo()          { return to; }

    // ══ Records ═══════════════════════════════════════

    /** A single block placement instruction. */
    private record BlockPlacement(int x, int y, int z, BlockState state) {
        BlockPos pos() { return new BlockPos(x, y, z); }
    }

    /** One step along the road path + precomputed block placements. */
    private record Step(int centerX, int centerZ, int roadFloor,
                        List<BlockPlacement> placements) {}

    // ══ Utility ═══════════════════════════════════════

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
