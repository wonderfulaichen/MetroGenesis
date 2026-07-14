package com.metrogenesis.road;

import com.metrogenesis.blueprint.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Converts a Bezier curve path into a 1-layer Blueprint for block-level road preview.
 * <p>
 * The Blueprint is a single-layer (Y=1) representation of the road surface,
 * suitable for rendering with {@link com.metrogenesis.blueprint.v2.BlueprintRenderer}.
 * <p>
 * Usage:
 * <pre>
 *   Blueprint bp = RoadBlueprintBuilder.buildFromPath(pathPoints, yaws, 3);
 *   blueprintRenderer.draw(level, bp, worldPos, partialTicks, false, BlueprintOverlayType.SEMI_TRANSPARENT);
 * </pre>
 */
public class RoadBlueprintBuilder {

    /**
     * 根据宽度偏移获取对应的方块类型。
     * 从当前活跃模板读取，而非硬编码。
     */
    private static BlockState getBlockForWidth(int w, int halfW) {
        RoadTemplate template = RoadTemplateManager.getInstance().getActiveTemplate();
        int localX = w + halfW; // [-halfW, halfW] → [0, roadWidth-1]
        // Z=0 层，沿路径重复
        return template.getBlock(localX, 0, 0);
    }

    /**
     * Build a 1-layer Blueprint from a list of Bezier curve path points.
     * <p>
     * Each path point is expanded by half the road width in the perpendicular direction
     * (calculated from yaw angle) to create the road surface. The Blueprint's local origin
     * is at (minX, 0, minZ) in world coordinates.
     * <p>
     * 方块类型：边缘 = 石砖（路缘），中间 = 平滑石（路面）。
     *
     * @param pathPoints The Bezier curve path points (world coordinates)
     * @param yaws       The yaw angles at each path point (radians)
     * @param roadWidth  The road width in blocks (perpendicular to direction of travel)
     * @return A Blueprint representing the road surface, or null if pathPoints is empty
     */
    public static Blueprint buildFromPath(List<Vec3> pathPoints, List<Float> yaws, int roadWidth) {
        if (pathPoints == null || pathPoints.isEmpty() || yaws == null || yaws.isEmpty()) {
            return null;
        }

        int halfW = roadWidth / 2;

        // 1. Calculate bounding box (account for perpendicular expansion)
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < pathPoints.size(); i++) {
            Vec3 point = pathPoints.get(i);
            float yaw = yaws.get(i);

            // Perpendicular direction: perp = (-sin(yaw), cos(yaw))
            double perpX = -Math.sin(yaw);
            double perpZ = Math.cos(yaw);

            // Expand bounding box by halfW in perpendicular direction
            int x = (int) Math.floor(point.x);
            int z = (int) Math.floor(point.z);
            minX = Math.min(minX, x - (int) Math.ceil(Math.abs(perpX * halfW)));
            maxX = Math.max(maxX, x + (int) Math.ceil(Math.abs(perpX * halfW)));
            minZ = Math.min(minZ, z - (int) Math.ceil(Math.abs(perpZ * halfW)));
            maxZ = Math.max(maxZ, z + (int) Math.ceil(Math.abs(perpZ * halfW)));
        }

        int sizeX = maxX - minX + 1;
        int sizeY = 1; // Single layer
        int sizeZ = maxZ - minZ + 1;

        Blueprint blueprint = new Blueprint((short) sizeX, (short) sizeY, (short) sizeZ);
        blueprint.setName("road_preview");

        // 2. Fill road blocks along the path, expanding perpendicular to direction
        for (int i = 0; i < pathPoints.size(); i++) {
            Vec3 point = pathPoints.get(i);
            float yaw = yaws.get(i);

            // Perpendicular direction: perp = (-sin(yaw), cos(yaw))
            double perpX = -Math.sin(yaw);
            double perpZ = Math.cos(yaw);

            // Fill road width perpendicular to path direction
            for (int w = -halfW; w <= halfW; w++) {
                int worldX = (int) Math.floor(point.x + perpX * w);
                int worldZ = (int) Math.floor(point.z + perpZ * w);

                int localX = worldX - minX;
                int localZ = worldZ - minZ;

                if (localX >= 0 && localX < sizeX && localZ >= 0 && localZ < sizeZ) {
                    blueprint.addBlockState(new BlockPos(localX, 0, localZ), getBlockForWidth(w, halfW));
                }
            }

            // ── Bresenham 填充相邻中心点之间的间隙 ──
            if (i > 0) {
                Vec3 prevPoint = pathPoints.get(i - 1);
                float prevYaw = yaws.get(i - 1);
                double prevPerpX = -Math.sin(prevYaw);
                double prevPerpZ = Math.cos(prevYaw);

                for (int w = -halfW; w <= halfW; w++) {
                    int prevWX = (int) Math.floor(prevPoint.x + prevPerpX * w);
                    int prevWZ = (int) Math.floor(prevPoint.z + prevPerpZ * w);
                    int currWX = (int) Math.floor(point.x + perpX * w);
                    int currWZ = (int) Math.floor(point.z + perpZ * w);

                    // Bresenham line connecting previous and current width offsets
                    int gdx = Math.abs(currWX - prevWX), gsx = prevWX < currWX ? 1 : -1;
                    int gdz = Math.abs(currWZ - prevWZ), gsz = prevWZ < currWZ ? 1 : -1;
                    int gerr = gdx - gdz;
                    int gx = prevWX, gz = prevWZ;
                    while (true) {
                        int glx = gx - minX;
                        int glz = gz - minZ;
                        if (glx >= 0 && glx < sizeX && glz >= 0 && glz < sizeZ) {
                            blueprint.addBlockState(new BlockPos(glx, 0, glz), getBlockForWidth(w, halfW));
                        }
                        if (gx == currWX && gz == currWZ) break;
                        int ge2 = gerr;
                        if (ge2 > -gdz) { gerr -= gdz; gx += gsx; }
                        if (ge2 < gdx)  { gerr += gdx; gz += gsz; }
                    }
                }
            }
        }

        return blueprint;
    }

    /**
     * Get the world origin of a Blueprint built from path points.
     * <p>
     * This returns the (minX, minZ) bounding box corner used during build.
     *
     * @param pathPoints The path points used to build the Blueprint
     * @param yaws       The yaw angles at each path point (radians)
     * @param roadWidth  The road width used during build
     * @return int[] {minX, minZ} or null if pathPoints is empty
     */
    public static int[] getBlueprintOrigin(List<Vec3> pathPoints, List<Float> yaws, int roadWidth) {
        if (pathPoints == null || pathPoints.isEmpty() || yaws == null || yaws.isEmpty()) {
            return null;
        }

        int halfW = roadWidth / 2;
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;

        for (int i = 0; i < pathPoints.size(); i++) {
            Vec3 point = pathPoints.get(i);
            float yaw = yaws.get(i);

            // Perpendicular direction (yaw + 90 degrees)
            double perpX = Math.cos(yaw + Math.PI / 2);
            double perpZ = Math.sin(yaw + Math.PI / 2);

            int x = (int) Math.floor(point.x);
            int z = (int) Math.floor(point.z);
            minX = Math.min(minX, x - (int) Math.ceil(Math.abs(perpX * halfW)));
            minZ = Math.min(minZ, z - (int) Math.ceil(Math.abs(perpZ * halfW)));
        }

        return new int[]{minX, minZ};
    }

    /**
     * Backward-compatible version without yaws (uses X-axis expansion).
     * @deprecated Use {@link #buildFromPath(List, List, int)} instead
     */
    @Deprecated
    public static Blueprint buildFromPath(List<Vec3> pathPoints, int roadWidth) {
        if (pathPoints == null || pathPoints.isEmpty()) {
            return null;
        }

        int halfW = roadWidth / 2;

        // Calculate bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Vec3 point : pathPoints) {
            int x = (int) Math.floor(point.x);
            int z = (int) Math.floor(point.z);
            minX = Math.min(minX, x - halfW);
            maxX = Math.max(maxX, x + halfW);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        int sizeX = maxX - minX + 1;
        int sizeY = 1;
        int sizeZ = maxZ - minZ + 1;

        Blueprint blueprint = new Blueprint((short) sizeX, (short) sizeY, (short) sizeZ);
        blueprint.setName("road_preview");

        for (Vec3 point : pathPoints) {
            int localX = (int) Math.floor(point.x) - minX;
            int localZ = (int) Math.floor(point.z) - minZ;

            for (int dx = -halfW; dx <= halfW; dx++) {
                int px = localX + dx;
                if (px >= 0 && px < sizeX) {
                    blueprint.addBlockState(new BlockPos(px, 0, localZ), RoadBuilder.ROAD_SURFACE);
                }
            }
        }

        return blueprint;
    }

    /**
     * Backward-compatible version without yaws.
     * @deprecated Use {@link #getBlueprintOrigin(List, List, int)} instead
     */
    @Deprecated
    public static int[] getBlueprintOrigin(List<Vec3> pathPoints, int roadWidth) {
        if (pathPoints == null || pathPoints.isEmpty()) {
            return null;
        }

        int halfW = roadWidth / 2;
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;

        for (Vec3 point : pathPoints) {
            int x = (int) Math.floor(point.x);
            int z = (int) Math.floor(point.z);
            minX = Math.min(minX, x - halfW);
            minZ = Math.min(minZ, z);
        }

        return new int[]{minX, minZ};
    }
}
