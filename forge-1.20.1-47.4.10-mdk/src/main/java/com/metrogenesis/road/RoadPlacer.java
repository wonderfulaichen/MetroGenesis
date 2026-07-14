package com.metrogenesis.road;

import com.metrogenesis.RoadData;
import com.metrogenesis.RoadData.NodePos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 道路放置系统 — 逐格铺设（弯道专用）。
 * <p>
 * 核心思路：沿贝塞尔曲线密集采样，每个采样点沿道路宽度方向
 * 逐格放置方块，不再使用模板 stamp 方式。
 * </p>
 *
 * <h3>为什么要改为逐格铺设</h3>
 * <ul>
 *   <li>原方案：在每个采样点 stamp 整个 3×3 模板 → 非 90° 角度时
 *       {@code Math.round()} 导致相邻 stamp 间出现楔形空隙</li>
 *   <li>新方案：对每个采样点沿垂直方向逐格放置方块 → 无缝连接</li>
 * </ul>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从 {@link RoadTemplateManager} 获取当前活跃模板</li>
 *   <li>沿贝塞尔曲线密集采样</li>
 *   <li>每个采样点：读取模板 Z=0 层的方块，沿垂直方向逐格放置</li>
 *   <li>路基填充（可选）</li>
 * </ol>
 */
public class RoadPlacer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RoadPlacer() {}

    /**
     * 沿贝塞尔曲线铺设道路（逐格铺设版本）。
     * <p>
     * 算法：
     * 1. 密集采样曲线（密度根据曲率自动调整）
     * 2. 每个采样点获取地面高度作为 Y 基准
     * 3. 对每个采样点，沿垂直（右）方向逐格放置方块
     * 4. 方块类型从模板的 Z=0 层读取
     * 5. 路基填充（可选）
     * </p>
     *
     * <h3>与原 stamp 方案的关键区别</h3>
     * <ul>
     *   <li>不再调用 {@link RoadTemplate#placeInWorld}，改为逐格放置</li>
     *   <li>不再使用端点内移逻辑（halfZ offset），因只读 Z=0 层</li>
     *   <li>每个宽度位置独立计算世界坐标，避免累积取整误差</li>
     * </ul>
     *
     * @param level         世界
     * @param segment       贝塞尔曲线段
     * @param roadWidth     道路宽度（方块数，仅用于采样密度，方块来自模板）
     * @param useFoundation 是否填充路基
     * @param changeStorage 撤销存储（null=不记录）
     * @return 放置结果
     */
    public static PlaceResult placeRoad(
            Level level,
            BezierSegment segment,
            int roadWidth,
            boolean useFoundation,
            com.metrogenesis.structurize.util.ChangeStorage changeStorage
    ) {
        LOGGER.debug("开始道路放置(逐格模式): segment={}, roadWidth={}, useFoundation={}",
                segment, roadWidth, useFoundation);

        // 1. 获取路径采样点（高密度，确保弯道无间隙）
        float curvature = Math.abs(segment.getCurvature());
        Vec3 startPos = Vec3.atCenterOf(segment.getStart());
        Vec3 endPos = Vec3.atCenterOf(segment.getEnd());

        // 使用弧长计算采样密度，大曲率时弧长远大于直线距离
        double straightDist = startPos.distanceTo(endPos);
        double arcLen = BezierRoad.arcLength(startPos, segment.getControlPoint(), endPos,
                Math.max(16, (int) Math.ceil(straightDist)));
        // 每 0.5 格至少 1 个采样点，确保大曲率时无缝隙
        int sampleCount = Math.max(4, (int) Math.ceil(arcLen / 0.5));
        List<BezierSegment.PathPoint> pathPoints = segment.samplePath(sampleCount);
        LOGGER.debug("路径采样完成: {} 个采样点 (curvature={}, arcLen={})",
                pathPoints.size(), curvature, Math.round(arcLen * 10) / 10.0);

        RoadTemplate template = RoadTemplateManager.getInstance().getActiveTemplate();

        // 模板宽度一半，用于遍历宽度方向
        int halfW = template.getSizeX() / 2;

        // 2. 线性插值高度 — 起终点平滑过渡（避免每个采样点独立查询导致阶梯坡道）
        float hFrom = findGroundHeight(level, (int) Math.floor(startPos.x), (int) Math.floor(startPos.z));
        float hTo   = findGroundHeight(level, (int) Math.floor(endPos.x), (int) Math.floor(endPos.z));

        int blocksPlaced = 0;
        int foundationBlocks = 0;

        // 3. 像素填充法 — 把每个方块当成像素，判断是否在道路范围内
        //    算法：遍历曲线包围盒内每个整数坐标，计算它到曲线最近采样点的
        //    欧氏距离，≤ halfW + 0.5 则放置方块。跟像素画笔画粗线完全一样。
        int lastIdx = pathPoints.size() - 1;

        // 3a. 计算包围盒（半径 = halfW + 1 防止边缘遗漏）
        int minBX = Integer.MAX_VALUE, maxBX = Integer.MIN_VALUE;
        int minBZ = Integer.MAX_VALUE, maxBZ = Integer.MIN_VALUE;
        for (BezierSegment.PathPoint p : pathPoints) {
            int bx = (int) Math.floor(p.position().x);
            int bz = (int) Math.floor(p.position().z);
            minBX = Math.min(minBX, bx - halfW - 1);
            maxBX = Math.max(maxBX, bx + halfW + 1);
            minBZ = Math.min(minBZ, bz - halfW - 1);
            maxBZ = Math.max(maxBZ, bz + halfW + 1);
        }

        // 3b. 遍历每个像素，用欧氏距离判断是否在道路内
        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int bz = minBZ; bz <= maxBZ; bz++) {
                double px = bx + 0.5;
                double pz = bz + 0.5;
                double minDist = Double.MAX_VALUE;
                float bestYaw = 0;
                float bestT = 0;

                // 找最近采样点（欧氏距离）
                for (int si = 0; si < pathPoints.size(); si++) {
                    BezierSegment.PathPoint sp = pathPoints.get(si);
                    double dx = px - sp.position().x;
                    double dz = pz - sp.position().z;
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist < minDist) {
                        minDist = dist;
                        bestYaw = sp.yaw();
                        bestT = (float) si / lastIdx;
                    }
                }

                // 欧氏距离 ≤ halfW + 0.5 → 在道路范围内
                if (minDist <= halfW + 0.5) {
                    // 计算 localX（模板列索引）：根据垂直投影方向
                    int si = (int) Math.min(Math.round(bestT * lastIdx), lastIdx);
                    BezierSegment.PathPoint best = pathPoints.get(si);
                    double dx = px - best.position().x;
                    double dz = pz - best.position().z;
                    double perpX = -Math.sin(best.yaw());
                    double perpZ = Math.cos(best.yaw());
                    double signedPerp = dx * perpX + dz * perpZ;
                    int localX = (int) Math.round(signedPerp) + halfW;
                    localX = Math.max(0, Math.min(template.getSizeX() - 1, localX));

                    // 高度插值
                    int groundH = (int) Math.floor(hFrom + (hTo - hFrom) * bestT);

                    // 放置模板 Z=0 层方块
                    for (int wy = 0; wy < template.getSizeY(); wy++) {
                        BlockState blockType = template.getBlock(localX, wy, 0);
                        if (!blockType.isAir()) {
                            BlockPos pos = new BlockPos(bx, groundH + wy, bz);
                            BlockState existing = level.getBlockState(pos);
                            if (existing.getBlock() == net.minecraft.world.level.block.Blocks.SMOOTH_STONE
                                || existing.getBlock() == net.minecraft.world.level.block.Blocks.STONE_BRICKS) {
                                continue;
                            }
                            if (changeStorage != null) changeStorage.addPreviousDataFor(pos, level);
                            level.setBlock(pos, blockType, 3);
                            if (changeStorage != null) changeStorage.addPostDataFor(pos, level);
                            blocksPlaced++;
                        }
                    }

                    // 路基填充
                    if (useFoundation) {
                        for (int y = groundH - 1; y > level.getMinBuildHeight(); y--) {
                            BlockPos pos = new BlockPos(bx, y, bz);
                            if (level.getBlockState(pos).isSolid()) break;
                            level.setBlock(pos, template.getFoundationBlock(), 3);
                            foundationBlocks++;
                        }
                    }
                }
            }
        }

        LOGGER.debug("道路放置完成: blocks={}, foundation={}",
                blocksPlaced, foundationBlocks);

        return new PlaceResult(blocksPlaced, foundationBlocks, new ArrayList<>());
    }

    /**
     * 兼容旧版调用 — 使用指定模板放置道路。
     * <p>
     * 注意：主方法从 {@link RoadTemplateManager} 读取活跃模板。
     * 此重载仅用于兼容旧调用方，内部将模板宽度作为参数传递，
     * 活跃模板由管理器决定。
     * </p>
     */
    public static PlaceResult placeRoad(
            Level level,
            BezierSegment segment,
            RoadTemplate template,
            boolean useFoundation,
            int slopeLimit
    ) {
        return placeRoad(level, segment, template.getWidth(), useFoundation, null);
    }

    /**
     * 使用指定模板放置道路（带撤销支持）。
     */
    public static PlaceResult placeRoad(
            Level level,
            BezierSegment segment,
            RoadTemplate template,
            boolean useFoundation,
            int slopeLimit,
            com.metrogenesis.structurize.util.ChangeStorage changeStorage
    ) {
        return placeRoad(level, segment, template.getWidth(), useFoundation, changeStorage);
    }

    // ════════════════════════════════════════════════════════
    //  内部工具方法
    // ════════════════════════════════════════════════════════

    /**
     * 搜索指定 XZ 位置的地面高度（路面放置的 Y 坐标）。
     * 返回最高实心方块上方 1 格的 Y（路面坐在地面上，不替换地面方块）。
     * 跳过已有的道路方块（路面/路基），确保重绘时不会堆叠。
     * 与 RoadBuilder.getTrueTerrainHeight 保持一致。
     */
    private static int findGroundHeight(Level level, int x, int z) {
        int h = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        // 向下跳过已有的道路方块（路面/路缘/路基），找到原始地面
        while (h > level.getMinBuildHeight()) {
            BlockPos pos = new BlockPos(x, h - 1, z);
            BlockState state = level.getBlockState(pos);
            if (state == RoadBuilder.ROAD_SURFACE || state == RoadBuilder.ROAD_CURB || state == RoadBuilder.ROAD_BASE) {
                h--;
            } else {
                break;
            }
        }
        return h;
    }

    // ════════════════════════════════════════════════════════
    //  结果记录
    // ════════════════════════════════════════════════════════

    public record PlaceResult(
            int blocksPlaced,
            int foundationBlocks,
            List<BlockPos> jumpPoints
    ) {
        public int totalBlocks() { return blocksPlaced + foundationBlocks; }
        public boolean hasJumpPoints() { return !jumpPoints.isEmpty(); }

        @Override
        public String toString() {
            return "PlaceResult[placed=" + blocksPlaced +
                    ", foundation=" + foundationBlocks +
                    ", jumps=" + jumpPoints.size() + "]";
        }
    }
}
