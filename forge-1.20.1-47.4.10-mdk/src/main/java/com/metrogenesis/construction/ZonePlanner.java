package com.metrogenesis.construction;

import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.catalog.CategoryMapper;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 区域规划器 — 装箱算法。
 * <p>
 * 将一个 ZoneRect（或合并后的同类型相邻区）划分为建筑位，
 * 从 catalog 中匹配蓝图，生成放置计划。
 * <p>
 * 核心原则：蓝图有什么就填什么，不预设固定尺寸。
 */
public class ZonePlanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonePlanner.class);

    /** 密度对应的覆盖率 */
    private static final double[] DENSITY_COVERAGE = { 0.30, 0.50, 0.70 };

    /** 密度对应的最大建筑面积过滤：低/中/高 */
    private static final int[] DENSITY_MAX_AREA = { 36, 64, Integer.MAX_VALUE };

    /** 密度对应的最小建筑面积过滤 */
    private static final int[] DENSITY_MIN_AREA = { 4, 16, 36 };

    /** 密度对应的格宽 */
    private static final int[] DENSITY_DEPTH = { 6, 8, 10 };

    public record BuildingSlot(
        int minX, int minZ, int maxX, int maxZ,  // 该格在世界中的坐标
        boolean isStreetFront,                     // 是否沿街
        BuildingCatalogEntry assignedBlueprint,    // 匹配到的蓝图（null=留空）
        int rotation                               // 放置旋转（0~3）
    ) {}

    /**
     * 为单个 ZoneRect 生成放置计划。
     *
     * @param zone 区域
     * @param entries 当前风格包的所有蓝图条目（已按 zoneType 和 density 预筛选）
     * @return 放置计划（BuildingSlot 列表）
     */
    public List<BuildingSlot> plan(ZoneRect zone, List<BuildingCatalogEntry> entries) {
        List<BuildingSlot> result = new ArrayList<>();
        if (zone.area() <= 0) return result;

        int zoneType = zone.zoneType();
        int density = zone.density();
        int direction = zone.direction();

        // 从 entries 中筛选出匹配此 zoneType 的
        List<BuildingCatalogEntry> candidates = filterByZoneType(entries, zoneType);

        // 按密度过滤尺寸
        int minArea = DENSITY_MIN_AREA[density];
        int maxArea = DENSITY_MAX_AREA[density];
        List<BuildingCatalogEntry> filtered = candidates.stream()
            .filter(e -> {
                int area = e.size().getX() * e.size().getZ();
                return area >= minArea && area <= maxArea;
            })
            .sorted((a, b) -> {
                int areaA = a.size().getX() * a.size().getZ();
                int areaB = b.size().getX() * b.size().getZ();
                return Integer.compare(areaB, areaA); // 大优先
            })
            .toList();

        if (filtered.isEmpty()) {
            LOGGER.info("ZonePlanner: no matching blueprints for zoneType={} density={}", zoneType, density);
            return result;
        }

        // 计算区域在 road-facing 方向上的宽×深
        int zoneW = zone.width();
        int zoneD = zone.depth();
        int depthGoal = DENSITY_DEPTH[density];
        int streetFrontDepth = Math.min(depthGoal, zoneD / 3);

        if (streetFrontDepth < 3 && zoneD > 0) {
            streetFrontDepth = Math.min(zoneD, 8);
        }

        double coverage = DENSITY_COVERAGE[density];
        long maxBlocks = (long) (zone.area() * coverage);

        // 沿街面 + 内部两轮装箱
        BinPackContext ctx = new BinPackContext(zone, filtered, maxBlocks);

        // 第一轮：沿街面
        fillStreetFront(ctx, streetFrontDepth);
        // 第二轮：内部
        fillInterior(ctx, streetFrontDepth, zoneD);

        return ctx.slots;
    }

    /**
     * 筛选匹配区域类型的建筑。
     * zoneType: 0=住宅 1=工业 2=商业 3=农业 4=公共 5=混合
     * 优先用 buildingType 匹配，无设施方块时降级到 category 字符串匹配。
     */
    private List<BuildingCatalogEntry> filterByZoneType(List<BuildingCatalogEntry> entries, int zoneType) {
        if (zoneType == 5) return entries; // 混合区不限类型

        // 从 CategoryMapper 获取该 zoneType 对应的分类名列表
        List<String> cats = CategoryMapper.getCategoriesForZoneType(zoneType);

        // 设施方块优先、目录兜底：用 getZoneCategoryForEntry 取有效分类后匹配
        List<BuildingCatalogEntry> result = new ArrayList<>();
        for (BuildingCatalogEntry e : entries) {
            // 取 MetroGenesis 自有分类（mgCategory，扫描时由我们的规则设定）
            String effectiveCategory = CategoryMapper.getZoneCategoryForEntry(e);

            // 跳过无缝建筑（不在区内自动生长）
            if (CategoryMapper.CAT_SEAMLESS.equals(effectiveCategory)) continue;

            // 检查有效分类是否匹配该 zoneType
            boolean catMatch = false;
            for (String c : cats) {
                if (c.equals(effectiveCategory)) {
                    catMatch = true;
                    break;
                }
            }
            if (!catMatch) continue;

            result.add(e);
        }
        return result;
    }

    /** 装箱上下文（闭包替代） */
    private static class BinPackContext {
        final ZoneRect zone;
        final List<BuildingCatalogEntry> candidates; // 已按尺寸降序排列
        final long maxBlocks;
        final List<BuildingSlot> slots = new ArrayList<>();
        long usedBlocks = 0;
        int direction;

        BinPackContext(ZoneRect zone, List<BuildingCatalogEntry> candidates, long maxBlocks) {
            this.zone = zone;
            this.candidates = candidates;
            this.maxBlocks = maxBlocks;
            this.direction = zone.direction();
        }
    }

    /** 填充沿街面 */
    private void fillStreetFront(BinPackContext ctx, int depth) {
        ZoneRect zone = ctx.zone;
        int direction = zone.direction();
        int minX = zone.minX(), minZ = zone.minZ();
        int maxX = zone.maxX(), maxZ = zone.maxZ();

        // 沿 road-facing 边的坐标范围
        int alongStart, alongEnd, perpStart;
        if (direction == 0 || direction == 2) {
            alongStart = minZ;
            alongEnd = maxZ;
            perpStart = (direction == 0) ? maxX - depth : minX;
        } else {
            alongStart = minX;
            alongEnd = maxX;
            perpStart = (direction == 1) ? maxZ - depth : minZ;
        }

        int pos = alongStart;
        while (pos < alongEnd && ctx.usedBlocks < ctx.maxBlocks) {
            int remaining = alongEnd - pos;
            String hint = "street-front";
            BuildingCatalogEntry best = findBestFit(remaining, depth, ctx.candidates, hint);

            if (best == null) break; // 放不下了

            int bw = best.size().getX();
            int bd = best.size().getZ();

            int fitW = Math.min(bw, remaining);
            int fitD = Math.min(bd, depth);

            int slotMinX, slotMinZ, slotMaxX, slotMaxZ;
            if (direction == 0 || direction == 2) {
                slotMinZ = pos;
                slotMaxZ = pos + fitW;
                if (direction == 0) {
                    slotMinX = maxX - fitD;
                    slotMaxX = maxX;
                } else {
                    slotMinX = minX;
                    slotMaxX = minX + fitD;
                }
            } else {
                slotMinX = pos;
                slotMaxX = pos + fitW;
                if (direction == 1) {
                    slotMinZ = maxZ - fitD;
                    slotMaxZ = maxZ;
                } else {
                    slotMinZ = minZ;
                    slotMaxZ = minZ + fitD;
                }
            }

            int area = fitW * fitD;
            ctx.slots.add(new BuildingSlot(slotMinX, slotMinZ, slotMaxX, slotMaxZ, true, best, 0));
            ctx.usedBlocks += area;
            pos += fitW;
        }
    }

    /** 填充内部区域 */
    private void fillInterior(BinPackContext ctx, int streetFrontDepth, int zoneD) {
        if (zoneD <= streetFrontDepth) return;
        int interiorDepth = zoneD - streetFrontDepth;
        if (interiorDepth < 3) return;

        ZoneRect zone = ctx.zone;
        int direction = zone.direction();

        // 内部沿同一方向排，从 street-front 后面开始
        int alongStart, alongEnd, perpStart;
        if (direction == 0 || direction == 2) {
            alongStart = zone.minZ();
            alongEnd = zone.maxZ();
            if (direction == 0) {
                perpStart = zone.maxX() - streetFrontDepth - interiorDepth;
            } else {
                perpStart = zone.minX() + streetFrontDepth;
            }
        } else {
            alongStart = zone.minX();
            alongEnd = zone.maxX();
            if (direction == 1) {
                perpStart = zone.maxZ() - streetFrontDepth - interiorDepth;
            } else {
                perpStart = zone.minZ() + streetFrontDepth;
            }
        }

        int pos = alongStart;
        while (pos < alongEnd && ctx.usedBlocks < ctx.maxBlocks) {
            int remaining = alongEnd - pos;
            BuildingCatalogEntry best = findBestFit(remaining, interiorDepth, ctx.candidates, "interior");
            if (best == null) break;

            int bw = best.size().getX();
            int bd = best.size().getZ();
            int fitW = Math.min(bw, remaining);
            int fitD = Math.min(bd, interiorDepth);

            int slotMinX, slotMinZ, slotMaxX, slotMaxZ;
            if (direction == 0 || direction == 2) {
                slotMinZ = pos;
                slotMaxZ = pos + fitW;
                if (direction == 0) {
                    slotMinX = perpStart;
                    slotMaxX = perpStart + fitD;
                } else {
                    slotMinX = perpStart - fitD;
                    slotMaxX = perpStart;
                }
            } else {
                slotMinX = pos;
                slotMaxX = pos + fitW;
                if (direction == 1) {
                    slotMinZ = perpStart;
                    slotMaxZ = perpStart + fitD;
                } else {
                    slotMinZ = perpStart - fitD;
                    slotMaxZ = perpStart;
                }
            }

            int area = fitW * fitD;
            ctx.slots.add(new BuildingSlot(slotMinX, slotMinZ, slotMaxX, slotMaxZ, false, best, 0));
            ctx.usedBlocks += area;
            pos += fitW;
        }
    }

    /** 在候选列表中找最匹配的蓝图 */
    private BuildingCatalogEntry findBestFit(int availableWidth, int availableDepth,
                                              List<BuildingCatalogEntry> candidates, String hint) {
        // 候选列表已按面积降序排列
        for (BuildingCatalogEntry e : candidates) {
            int bw = e.size().getX();
            int bd = e.size().getZ();
            if (bw <= availableWidth && bd <= availableDepth) {
                // 沿街面优先选混合用途或商业
                if ("street-front".equals(hint)) {
                    if (e.isMixedUse()) return e;
                }
                return e;
            }
        }
        return null;
    }
}
