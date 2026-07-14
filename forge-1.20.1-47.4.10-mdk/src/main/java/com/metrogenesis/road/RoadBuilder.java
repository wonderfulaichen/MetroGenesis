package com.metrogenesis.road;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.RoadData;
import com.metrogenesis.RoadData.NodePos;
import com.metrogenesis.RoadData.RoadSegment;
import com.metrogenesis.structurize.management.Manager;
import com.metrogenesis.structurize.util.ChangeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Road construction algorithm.
 *
 * Places actual road blocks (road_surface + road_base) on the world,
 * adapting to terrain height with linear interpolation for smooth slopes.
 *
 * ── Lifecycle ────────────────────────────────────────────
 * 1. {@link #commitSegment} = addSegment to RoadData + placeRoad + rebuildNode (both ends + neighbors)
 * 2. {@link #removeSegment} = removeSegment from RoadData + rebuildNode (both ends)
 * 3. {@link #placeRoad} = place blocks along the interpolated line between two nodes
 * 4. {@link #rebuildNode} = fill node area with road pattern matching connected directions
 *
 * ── Curve / diagonal handling ───────────────────────────
 * When a node is connected to curved or diagonal segments, {@link #rebuildNode}
 * fills the entire node area with road surface, avoiding intersection pattern
 * mismatch between the Bezier road blocks and the generated intersection.
 *
 * ── Overwrite protection ────────────────────────────────
 * {@link #rebuildNode} skips blocks already matching road surface/curb/base,
 * preventing overlapping road segments from overwriting each other.
 */
public class RoadBuilder {

    // ══ Block states ══════════════════════════════════
    // v1 uses vanilla blocks; can switch to registered custom blocks later
    // by replacing these constants with registered block references.

    /** Road surface block — the drivable/passable top layer. */
    public static final BlockState ROAD_SURFACE = Blocks.SMOOTH_STONE.defaultBlockState();
    /** 路缘方块 */
    public static final BlockState ROAD_CURB = Blocks.STONE_BRICKS.defaultBlockState();

    /** Road base fill — supports the surface above terrain. */
    public static final BlockState ROAD_BASE = Blocks.DIRT.defaultBlockState();

    // ══ Constants ═════════════════════════════════════

    /** Width of the road in blocks (perpendicular to direction of travel). */
    private static int roadWidth = 3;

    /** Set the road width (clamped to [1, 16]). */
    public static void setRoadWidth(int width) { roadWidth = Math.max(1, Math.min(16, width)); }

    /** Get the current road width. */
    public static int getRoadWidth() { return roadWidth; }

    /** Thickness of the road surface layer. */
    private static final int ROAD_HEIGHT = 1;

    // ══ Public API ════════════════════════════════════

    /**
     * Add a segment to RoadData + place blocks + rebuild affected nodes.
     *
     * This is the main entry point for creating a road:
     * 1. Persist the segment (with curvature for curved roads)
     * 2. Place road blocks between the two nodes
     * 3. Rebuild the intersection pattern at both nodes
     * 4. Rebuild neighbor nodes (shared intersections may change shape)
     *
     * @param curvature road curvature [-1, 1], 0 = straight (Phase 1)
     */
    public static void commitSegment(Level level, RoadData data, NodePos from, NodePos to, int type, float curvature, @Nullable ChangeStorage changeStorage) {
        // ══ 自环守卫：起点 == 终点时什么也不做 ═══════════════
        if (from.equals(to)) {
            MetroGenesis.LOGGER.warn("Ignoring self-loop road: {} == {}", from, to);
            return;
        }
        data.addSegment(new RoadSegment(from, to, type, curvature));

        // Place the road blocks between nodes
        placeRoad(level, from, to, type, changeStorage);

        // Rebuild intersection at both endpoints
        rebuildNode(level, from, data, changeStorage);
        rebuildNode(level, to, data, changeStorage);

        // Rebuild immediate neighbors of both endpoints
        // (intersections may change shape when a new road connects)
        Set<NodePos> affected = new HashSet<>();
        collectNeighbors(data, from, affected);
        collectNeighbors(data, to, affected);

        for (NodePos n : affected) {
            if (!n.equals(from) && !n.equals(to)) {
                rebuildNode(level, n, data, changeStorage);
            }
        }
    }

    /**
     * Convenience overload for straight roads (curvature = 0).
     */
    public static void commitSegment(Level level, RoadData data, NodePos from, NodePos to, int type, @Nullable ChangeStorage changeStorage) {
        commitSegment(level, data, from, to, type, 0f, changeStorage);
    }

    /**
     * Remove a segment from RoadData + rebuild affected nodes.
     *
     * @param curvature curvature of the segment to remove (Phase 1)
     */
    public static void removeSegment(Level level, RoadData data, NodePos from, NodePos to, int type, float curvature, @Nullable ChangeStorage changeStorage) {
        data.removeSegment(new RoadSegment(from, to, type, curvature));

        // Rebuild both endpoints to clear the road
        rebuildNode(level, from, data, changeStorage);
        rebuildNode(level, to, data, changeStorage);
    }

    /**
     * Convenience overload for straight roads (curvature = 0).
     */
    public static void removeSegment(Level level, RoadData data, NodePos from, NodePos to, int type, @Nullable ChangeStorage changeStorage) {
        removeSegment(level, data, from, to, type, 0f, changeStorage);
    }

    // ══ Road placement ════════════════════════════════

    /**
     * Sample terrain height at (x, z), skipping past our own road blocks.
     *
     * Without this, {@link Heightmap.Types#WORLD_SURFACE} returns the top of
     * previously placed road blocks, causing road stacking when the player
     * draws over the same area multiple times.
     */
    public static int getTrueTerrainHeight(Level level, int x, int z) {
        int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        while (h > level.getMinBuildHeight()) {
            BlockPos pos = new BlockPos(x, h - 1, z);
            BlockState state = level.getBlockState(pos);
            if (state == ROAD_SURFACE || state == ROAD_CURB || state == ROAD_BASE) {
                h--;
            } else {
                break;
            }
        }
        return h;
    }

    /**
     * Place road blocks between two block-level nodes.
     *
     * Algorithm:
     * 1. Sample terrain height at both node positions
     * 2. Walk along the path (major-axis steps), interpolating height linearly
     * 3. At each step, fill a roadWidth-wide strip centered on the path:
     *    - Place ROAD_BASE from terrain surface up to interpolated road floor
     *    - Place ROAD_SURFACE on top (1 block thick)
     *
     * @param changeStorage if non-null, records block changes for undo/redo
     *                      (must call resetUnRedo() before submitting to Manager)
     */
    public static void placeRoad(Level level, NodePos from, NodePos to, int type, @Nullable ChangeStorage changeStorage) {
        RoadTemplate template = RoadTemplateManager.getInstance().getActiveTemplate();

        int fromX = from.blockX(), fromZ = from.blockZ();
        int toX   = to.blockX(),   toZ   = to.blockZ();

        int dx = toX - fromX;
        int dz = toZ - fromZ;
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) steps = 1; // at least one step for same-node case

        // Sample terrain height at both endpoints, exclude road blocks
        float hFrom = getTrueTerrainHeight(level, fromX, fromZ);
        float hTo   = getTrueTerrainHeight(level, toX, toZ);

        // Path direction → Minecraft yaw (0 = +Z / South, -π/2 = +X / East)
        float yaw = (float) Math.atan2(-dx, dz);

        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int bx = fromX + Math.round(dx * t);
            int bz = fromZ + Math.round(dz * t);

            // Clamp to segment bounds
            bx = clamp(bx, Math.min(fromX, toX), Math.max(fromX, toX));
            bz = clamp(bz, Math.min(fromZ, toZ), Math.max(fromZ, toZ));

            // Interpolated target height at this column
            float targetH = hFrom + (hTo - hFrom) * t;
            int roadFloor = (int) Math.floor(targetH);

            BlockPos origin = new BlockPos(bx, roadFloor, bz);

            // ═══ 端点处理 ═══
            // 起止点不放置模板，由 rebuildNode 统一覆盖端点区域，
            // 避免 placeInWorld 的 halfZ=1 导致模板超出节点范围多一格。
            if ((i == 0 || i == steps) && steps > 1) {
                continue;
            }

            // ══ Undo/redo: capture blocks before placement ═══════════
            recordTemplateFootprint(level, template, origin, yaw, true, changeStorage);

            // ══ Place rotated template via placeInWorld ═══════════════
            // Handles: rotate blocks to match yaw, place non-air blocks,
            //          fill foundation from road floor down to solid ground
            template.placeInWorld(level, origin, yaw, true, true);

            // ══ Undo/redo: capture blocks after placement ════════════
            recordTemplateFootprint(level, template, origin, yaw, false, changeStorage);
        }
    }

    // ══ Intersection rebuilding ══════════════════════

    /**
     * Rebuild the intersection at a given node.
     *
     * Analyzes connected road directions and places the correct pattern:
     * - 4 connections → Cross intersection (full 4-way)
     * - 3 connections → T-junction
     * - 2 opposite    → Straight road strip
     * - 2 adjacent    → Corner
     * - 1 connection  → Dead end
     * - 0 connections → Cleared (no road)
     *
     * <p>Uses the active template's width ({@code template.getSizeX()}) rather than
     * the global {@link #roadWidth} so that custom templates are respected.</p>
     *
     * @param changeStorage if non-null, records block changes for undo/redo
     */
    public static void rebuildNode(Level level, NodePos node, RoadData data, @Nullable ChangeStorage changeStorage) {
        RoadTemplate template = RoadTemplateManager.getInstance().getActiveTemplate();

        Set<Direction> connections = getConnectedDirections(data, node);

        int cx = node.blockX();
        int cz = node.blockZ();
        int baseY = getTrueTerrainHeight(level, cx, cz);
        int roadFloor = (int) Math.floor(baseY);

        // Use template width so custom templates align correctly
        int nodeSize = template.getSizeX();
        int halfW = nodeSize / 2;

        // ══ 弯道/斜向检测 ═══════════════════════════════
        // 当节点有曲线或斜向连接时，不生成交叉口图案，
        // 直接全部填充路面，防止 Bezier 道路方块与交叉口图案不匹配。
        boolean curveMode = hasCurvedOrDiagonalConnection(data, node);

        // Fill a nodeSize × nodeSize area centered on the node
        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dz = -halfW; dz <= halfW; dz++) {
                int wx = cx + dx;
                int wz = cz + dz;
                int terrainH = getTrueTerrainHeight(level, wx, wz);

                // Normalize local coords to [0, nodeSize-1] for pattern lookup
                int localX = dx + halfW;
                int localZ = dz + halfW;

                // 弯道模式：全部填充；否则用图案匹配
                boolean isRoad;
                BlockState nodeBlock;
                if (curveMode) {
                    isRoad = true;
                    nodeBlock = getSurfaceBlock(template);
                } else {
                    isRoad = determineIfRoad(localX, localZ, connections, nodeSize);
                    nodeBlock = isRoad ? getNodeBlock(template, localX, localZ, connections, nodeSize) : null;
                }

                // Place blocks
                if (isRoad) {
                    // Fill base from terrain to road floor (using template's foundation block)
                    if (terrainH < roadFloor) {
                        for (int y = terrainH; y < roadFloor; y++) {
                            BlockPos pos = new BlockPos(wx, y, wz);
                            if (changeStorage != null) changeStorage.addPreviousDataForIfAbsent(pos, level);
                            level.setBlock(pos, template.getFoundationBlock(), 3);
                            if (changeStorage != null) changeStorage.addPostDataFor(pos, level);
                        }
                    }
                    // Surface block — 覆盖保护：已有路面方块则跳过
                    BlockPos surfacePos = new BlockPos(wx, roadFloor, wz);
                    BlockState existing = level.getBlockState(surfacePos);
                    if (existing == ROAD_SURFACE || existing == ROAD_CURB || existing == ROAD_BASE) {
                        continue; // 已有道路方块，不覆盖
                    }
                    if (changeStorage != null) changeStorage.addPreviousDataForIfAbsent(surfacePos, level);
                    level.setBlock(surfacePos, nodeBlock, 3);
                    if (changeStorage != null) changeStorage.addPostDataFor(surfacePos, level);
                } else if (terrainH < roadFloor) {
                    // Sidewalk/curb area — just base fill for even terrain
                    for (int y = terrainH; y < roadFloor; y++) {
                        BlockPos pos = new BlockPos(wx, y, wz);
                        if (changeStorage != null) changeStorage.addPreviousDataForIfAbsent(pos, level);
                        level.setBlock(pos, template.getFoundationBlock(), 3);
                        if (changeStorage != null) changeStorage.addPostDataFor(pos, level);
                    }
                }
            }
        }
    }

    /**
     * 检测节点是否有曲线或斜向连接。
     * <p>
     * 当节点连接了曲率非零的曲线段，或与邻居节点构成斜向关系时，
     * 返回 true 表示需要启用弯道模式（填充整个节点区域）。
     * </p>
     */
    private static boolean hasCurvedOrDiagonalConnection(RoadData data, NodePos node) {
        for (RoadSegment seg : data.getSegments()) {
            if (seg.from().equals(node) || seg.to().equals(node)) {
                // 曲率检测
                if (seg.curvature() != 0f) return true;
                // 斜向检测：dx != 0 && dz != 0
                NodePos other = seg.from().equals(node) ? seg.to() : seg.from();
                int dx = other.blockX() - node.blockX();
                int dz = other.blockZ() - node.blockZ();
                if (dx != 0 && dz != 0) return true;
            }
        }
        return false;
    }

    /**
     * 获取路口节点处指定位置的方块类型。
     * 直接读取模板对应位置的方块，适配任意模板宽度/图案。
     *
     * <p>localX/localZ 范围是 [0, nodeSize-1]，与模板宽度一致（nodeSize = template.getSizeX()）。</p>
     */
    private static BlockState getNodeBlock(RoadTemplate template, int localX, int localZ,
                                            Set<Direction> connections, int nodeSize) {
        boolean hasNorth = connections.contains(Direction.NORTH);
        boolean hasSouth = connections.contains(Direction.SOUTH);
        boolean hasEast  = connections.contains(Direction.EAST);
        boolean hasWest  = connections.contains(Direction.WEST);

        boolean inNS = (hasNorth || hasSouth);
        boolean inEW = (hasEast || hasWest);
        int maxIndex = nodeSize - 1;

        // 交叉区（多条路重叠）→ 保留各方向的路缘/边缘图案
        if (inNS && inEW) {
            boolean atNSEdge = (localX == 0 || localX == maxIndex);
            boolean atEWEdge = (localZ == 0 || localZ == maxIndex);

            if (atNSEdge && atEWEdge) {
                // 四角 → 路缘（模板最边缘的方块）
                BlockState edge = template.getBlock(0, 0, 0);
                return edge.isAir() ? ROAD_SURFACE : edge;
            } else if (atNSEdge) {
                // NS 臂的侧边缘 → 模板对应边缘
                int templateX = (localX == 0) ? 0 : template.getSizeX() - 1;
                BlockState edge = template.getBlock(templateX, 0, 0);
                return edge.isAir() ? ROAD_SURFACE : edge;
            } else if (atEWEdge) {
                // EW 臂的侧边缘 → 模板对应边缘
                BlockState edge = template.getBlock(0, 0, 0);
                return edge.isAir() ? ROAD_SURFACE : edge;
            }
            // 交叉区内部 → 路面
            return getSurfaceBlock(template);
        }

        // 仅在 NS 方向 → 直接映射到模板的宽度方向（X 轴）
        if (inNS) {
            BlockState block = template.getBlock(localX, 0, 0);
            return block.isAir() ? getSurfaceBlock(template) : block;
        }

        // 仅在 EW 方向 → Z 映射到模板的 X 轴
        if (inEW) {
            BlockState block = template.getBlock(localZ, 0, 0);
            return block.isAir() ? getSurfaceBlock(template) : block;
        }

        // 死路或端头
        return getSurfaceBlock(template);
    }

    /** 获取模板的第一个路面中心方块 */
    private static BlockState getSurfaceBlock(RoadTemplate template) {
        BlockState s = template.getBlock(template.getSizeX() / 2, 0, template.getSizeZ() / 2);
        return s.isAir() ? ROAD_SURFACE : s;
    }

    // ══ Intersection pattern logic ═══════════════════

    /**
     * Determine if a block at local (x, z) within the node area should be road.
     * Local coords are in range [0, W-1] where W is the node size (template width).
     *
     * @param W node area size in blocks (equals template width)
     */
    private static boolean determineIfRoad(int x, int z, Set<Direction> connections, int W) {
        int connCount = connections.size();

        if (connCount == 4) {
            return isRoadCross(x, z, W);
        } else if (connCount == 3) {
            return isRoadTee(x, z, connections, W);
        } else if (connCount == 2) {
            return isRoadTwoWay(x, z, connections, W);
        } else if (connCount == 1) {
            return isRoadEnd(x, z, connections.iterator().next(), W);
        } else {
            return false;
        }
    }

    /**
     * Cross intersection (4 connections).
     * Central plaza + 4 arms, scaled to width W.
     */
    private static boolean isRoadCross(int x, int z, int W) {
        int armC = W / 2;
        int armHW = Math.max(1, W / 4);
        boolean center = (x >= armC - armHW && x <= armC + armHW)
                      && (z >= armC - armHW && z <= armC + armHW);
        boolean northSouth = (x >= armC - armHW && x <= armC + armHW);
        boolean eastWest = (z >= armC - armHW && z <= armC + armHW);
        return center || northSouth || eastWest;
    }

    /**
     * T-junction (3 connections).
     * Missing one arm, so the road pattern is asymmetric.
     */
    private static boolean isRoadTee(int x, int z, Set<Direction> connections, int W) {
        int armC = W / 2;
        int armHW = Math.max(1, W / 4);
        boolean hasNorth = connections.contains(Direction.NORTH);
        boolean hasSouth = connections.contains(Direction.SOUTH);
        boolean hasEast  = connections.contains(Direction.EAST);
        boolean hasWest  = connections.contains(Direction.WEST);

        boolean center = (x >= armC - armHW && x <= armC + armHW)
                      && (z >= armC - armHW && z <= armC + armHW);
        boolean arm1 = false, arm2 = false;

        if (hasNorth || hasSouth) {
            arm1 = (x >= armC - armHW && x <= armC + armHW);
        }
        if (hasEast || hasWest) {
            arm2 = (z >= armC - armHW && z <= armC + armHW);
        }

        return center || arm1 || arm2;
    }

    /**
     * Two-way intersection: either straight road or corner.
     *
     * <p><b>Coordinate reminder</b>: localX=0 is west edge, localX=W-1 is east edge;
     * localZ=0 is north edge, localZ=W-1 is south edge.</p>
     */
    private static boolean isRoadTwoWay(int x, int z, Set<Direction> connections, int W) {
        int armC = W / 2;
        int armHW = Math.max(1, W / 4);
        Iterator<Direction> it = connections.iterator();
        Direction d1 = it.next();
        Direction d2 = it.next();
        boolean opposite = areOpposite(d1, d2);

        if (opposite) {
            boolean northSouth = connections.contains(Direction.NORTH) ||
                                 connections.contains(Direction.SOUTH);
            if (northSouth) {
                return (x >= armC - armHW && x <= armC + armHW);
            } else {
                return (z >= armC - armHW && z <= armC + armHW);
            }
        } else {
            boolean north = connections.contains(Direction.NORTH);
            boolean south = connections.contains(Direction.SOUTH);
            boolean east  = connections.contains(Direction.EAST);
            boolean west  = connections.contains(Direction.WEST);

            // 每个方向用标准臂公式（全宽度 + 半平面）
            boolean inNorth = (x >= armC - armHW && x <= armC + armHW) && (z < armC);
            boolean inSouth = (x >= armC - armHW && x <= armC + armHW) && (z >= armC);
            boolean inEast  = (z >= armC - armHW && z <= armC + armHW) && (x >= armC);
            boolean inWest  = (z >= armC - armHW && z <= armC + armHW) && (x < armC);

            // 节点中心确保覆盖
            boolean isCenter = (x == armC && z == armC);

            boolean road = false;
            if (north) road |= inNorth;
            if (south) road |= inSouth;
            if (east)  road |= inEast;
            if (west)  road |= inWest;
            road |= isCenter;
            return road;
        }
    }

    /**
     * Dead end (1 connection). Road only on the connected side.
     */
    private static boolean isRoadEnd(int x, int z, Direction dir, int W) {
        int armC = W / 2;
        int armHW = Math.max(1, W / 4);
        return switch (dir) {
            case NORTH -> (x >= armC - armHW && x <= armC + armHW) && (z <= armC);
            case SOUTH -> (x >= armC - armHW && x <= armC + armHW) && (z >= armC);
            case EAST  -> (x >= armC) && (z >= armC - armHW && z <= armC + armHW);
            case WEST  -> (x <= armC) && (z >= armC - armHW && z <= armC + armHW);
            default -> false;
        };
    }

    // ══ Direction helpers ════════════════════════════

    /**
     * Get all connected directions for a node.
     */
    public static Set<Direction> getConnectedDirections(RoadData data, NodePos node) {
        Set<Direction> dirs = new HashSet<>();
        for (RoadSegment seg : data.getSegments()) {
            if (seg.from().equals(node)) {
                dirs.add(directionFrom(node, seg.to()));
            } else if (seg.to().equals(node)) {
                dirs.add(directionFrom(node, seg.from()));
            }
        }
        return dirs;
    }

    /**
     * Determine cardinal direction from one node to another.
     * Uses block-level coordinates.
     */
    private static Direction directionFrom(NodePos from, NodePos to) {
        int dx = to.blockX() - from.blockX();
        int dz = to.blockZ() - from.blockZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * Check if two directions are opposite (N↔S or E↔W).
     */
    private static boolean areOpposite(Direction a, Direction b) {
        return (a == Direction.NORTH && b == Direction.SOUTH) ||
               (a == Direction.SOUTH && b == Direction.NORTH) ||
               (a == Direction.EAST  && b == Direction.WEST)  ||
               (a == Direction.WEST  && b == Direction.EAST);
    }

    // ══ Template change-storage helper ═════════════════

    /**
     * 记录模板放置前后涉及的所有方块到 ChangeStorage。
     * <p>
     * 对模板放置的每个非空气方块及路基填充区域，记录前/后状态。
     * 该方法完全复制 {@link RoadTemplate#placeInWorld} 的放置逻辑
     * 以确保记录范围一致。
     * </p>
     *
     * @param level          世界
     * @param template       模板
     * @param origin         放置原点
     * @param yaw            朝向弧度
     * @param previous       true=记录放置前状态, false=记录放置后状态
     * @param changeStorage  变更存储（null 时无操作）
     */
    private static void recordTemplateFootprint(Level level, RoadTemplate template, BlockPos origin,
                                                 float yaw, boolean previous, @Nullable ChangeStorage changeStorage) {
        if (changeStorage == null) return;

        // 方向向量 — 与 placeInWorld() 完全一致
        float fwdX   = (float) -Math.sin(yaw);
        float fwdZ   = (float)  Math.cos(yaw);
        float rightX = (float)  Math.cos(yaw);
        float rightZ = (float)  Math.sin(yaw);

        int halfW = template.getSizeX() / 2;
        int halfZ = template.getSizeZ() / 2;

        // 1. 模板方块区域
        for (int x = 0; x < template.getSizeX(); x++) {
            for (int y = 0; y < template.getSizeY(); y++) {
                for (int z = 0; z < template.getSizeZ(); z++) {
                    if (template.getBlock(x, y, z).isAir()) continue;
                    float localX = x - halfW;
                    float localZ = z - halfZ;
                    BlockPos pos = new BlockPos(
                            origin.getX() + Math.round(localX * rightX + localZ * fwdX),
                            origin.getY() + y,
                            origin.getZ() + Math.round(localX * rightZ + localZ * fwdZ));
                    if (previous) changeStorage.addPreviousDataForIfAbsent(pos, level);
                    else          changeStorage.addPostDataFor(pos, level);
                }
            }
        }

        // 2. 路基填充区域 — 用方向向量计算 bounding box
        int minWx = Integer.MAX_VALUE, maxWx = Integer.MIN_VALUE;
        int minWz = Integer.MAX_VALUE, maxWz = Integer.MIN_VALUE;
        for (int x = 0; x < template.getSizeX(); x++) {
            for (int z = 0; z < template.getSizeZ(); z++) {
                if (template.getBlock(x, 0, z).isAir()) continue;
                float localX = x - halfW;
                float localZ = z - halfZ;
                int wx = origin.getX() + Math.round(localX * rightX + localZ * fwdX);
                int wz = origin.getZ() + Math.round(localX * rightZ + localZ * fwdZ);
                minWx = Math.min(minWx, wx);
                maxWx = Math.max(maxWx, wx);
                minWz = Math.min(minWz, wz);
                maxWz = Math.max(maxWz, wz);
            }
        }
        for (int wx = minWx; wx <= maxWx; wx++) {
            for (int wz = minWz; wz <= maxWz; wz++) {
                for (int wy = origin.getY() - 1; wy > level.getMinBuildHeight(); wy--) {
                    BlockPos pos = new BlockPos(wx, wy, wz);
                    BlockState existingState = level.getBlockState(pos);
                    if (previous) changeStorage.addPreviousDataForIfAbsent(pos, level);
                    else          changeStorage.addPostDataFor(pos, level);
                    // 已到实心层则停止向下（只影响 PRE/POST 录入深度，不阻止录入当前层）
                    if (existingState.isSolid()) break;
                }
            }
        }
    }

    // ══ Utility ═══════════════════════════════════════

    /**
     * Clamp a value to the range [min, max].
     */
    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Collect all neighbor nodes of a given node into the set.
     */
    public static void collectNeighbors(RoadData data, NodePos node, Set<NodePos> result) {
        for (RoadSegment seg : data.getSegments()) {
            if (seg.from().equals(node)) {
                result.add(seg.to());
            } else if (seg.to().equals(node)) {
                result.add(seg.from());
            }
        }
    }
}
