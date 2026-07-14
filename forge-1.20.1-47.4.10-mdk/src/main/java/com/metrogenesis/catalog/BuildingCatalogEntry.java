package com.metrogenesis.catalog;

import net.minecraft.core.BlockPos;

import java.util.Set;
import java.util.TreeSet;

/**
 * 建筑图鉴条目 — 元数据 record，兼容殖民地 StructurePacks 生态。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>{@code packName} + {@code resourcePath} 可直接调用 StructurePacks.getBlueprint() 加载蓝图</li>
 *   <li>{@code mcCategory} 保留殖民地原始目录名，后续接入殖民地建造系统时可直接使用</li>
 *   <li>{@code category} 是 MetroGenesis 的分类，对玩家透明</li>
 * </ul>
 *
 * @param name         显示名（从文件名提取，如 "farmer", "townhall"）
 * @param packName     结构包名（如 "medievaloak", "colonial", "shire"）
 * @param resourcePath 包内相对路径（如 "agriculture/horticulture/farmer"）
 * @param category     MetroGenesis 分类（如 "生产建筑", "住宅"）
 * @param mcCategory   殖民地原始目录名（如 "agriculture", "fundamentals"）
 * @param size         建筑尺寸（x=宽, y=高, z=深）
 * @param levels       可用等级集合（如 {1,2,3,4,5}），单级建筑为 {1}
 * @param hasIcon      是否有图标（icon.png）
 * @param buildingType 设施方块类型名（如 "farmer", "townhall", "warehouse"），空字符串=无设施方块
 * @param isMixedUse   是否混合用途（含多个不同类型的设施方块，如楼下商楼上住）
 * @param materialCost 所有方块材料的总 C-Value 造价
 * @param description  功能说明的 lang key（如 "gui.metrogenesis.catalog.desc.farmer"），由 BuildingDescriptionProvider 提供；面板内用 Language 解析
 */
public record BuildingCatalogEntry(
    String name,
    String packName,
    String resourcePath,
    String category,
    String mcCategory,
    BlockPos size,
    Set<Integer> levels,
    boolean hasIcon,
    String buildingType,
    boolean isMixedUse,
    long materialCost,
    String description
) {
    /**
     * 创建单级建筑条目（默认等级 1），无设施方块检测。
     */
    public static BuildingCatalogEntry single(
        String name, String packName, String resourcePath,
        String category, String mcCategory, BlockPos size, boolean hasIcon
    ) {
        return new BuildingCatalogEntry(
            name, packName, resourcePath, category, mcCategory, size,
            new TreeSet<>(Set.of(1)), hasIcon,
            "", false, 0L, ""
        );
    }

    /**
     * 获取尺寸显示字符串（宽×深×高）。
     */
    public String sizeDisplay() {
        return size.getX() + "×" + size.getZ() + "×" + size.getY();
    }

    /**
     * 获取等级范围显示字符串（如 "1-5" 或 "3"）。
     */
    public String levelsDisplay() {
        if (levels.size() == 1) {
            return String.valueOf(levels.iterator().next());
        }
        int min = levels.iterator().next();
        int max = ((TreeSet<Integer>) levels).last();
        if (max - min + 1 == levels.size()) {
            return min + "-" + max;
        }
        return String.join(",", levels.stream().map(String::valueOf).toList());
    }

    /**
     * 是否为多级建筑（可升级）。
     */
    public boolean isMultiLevel() {
        return levels.size() > 1;
    }

    /**
     * 获取结构包内的完整蓝图路径（含 .blueprint 后缀）。
     * 用于 StructurePacks.getBlueprint(packName, path) 加载。
     */
    public String blueprintSubPath() {
        // 从 resourcePath 中提取目录部分（去掉建筑名）
        // resourcePath 格式: "agriculture/horticulture/farmer"
        // 需要返回目录部分: "agriculture/horticulture"
        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash > 0) {
            return resourcePath.substring(0, lastSlash);
        }
        return "";
    }

    /**
     * 获取蓝图文件名（不含路径和后缀）。
     * 用于 StructurePacks.getBlueprint(packName, subPath) 的第二参数。
     */
    public String blueprintFileName() {
        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < resourcePath.length() - 1) {
            return resourcePath.substring(lastSlash + 1);
        }
        return resourcePath;
    }
}
