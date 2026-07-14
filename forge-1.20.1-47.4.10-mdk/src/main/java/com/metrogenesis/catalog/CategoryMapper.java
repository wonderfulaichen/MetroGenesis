package com.metrogenesis.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类映射器 — 将 MineColonies 目录名映射到 MetroGenesis 8 大分类。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>保留殖民地原始目录名（mcCategory），后续接入殖民地建造系统时可直接使用</li>
 *   <li>MetroGenesis 分类对玩家透明，隐藏殖民地内部结构</li>
 *   <li>映射可扩展，后续新增分类只需添加映射</li>
 * </ul>
 */
public final class CategoryMapper {

    /**
     * MineColonies 目录名 → MetroGenesis 分类显示名。
     * 保持插入顺序，用于 UI 显示。
     */
    /**
     * 基础设施
     */
    public static final String CAT_INFRASTRUCTURE = "基础设施";
    /**
     * 住宅
     */
    public static final String CAT_RESIDENTIAL   = "住宅";
    /**
     * 公共设施
     */
    public static final String CAT_PUBLIC        = "公共设施";
    /**
     * 生产建筑
     */
    public static final String CAT_PRODUCTION    = "生产建筑";
    /**
     * 商业
     */
    public static final String CAT_COMMERCIAL    = "商业";
    /**
     * 农业
     */
    public static final String CAT_AGRICULTURE   = "农业";
    /**
     * 军事
     */
    public static final String CAT_MILITARY      = "军事";
    /**
     * 其他
     */
    public static final String CAT_OTHER         = "其他";
    /**
     * 无缝建筑
     */
    public static final String CAT_SEAMLESS      = "无缝建筑";

    private static final Map<String, String> MC_TO_MG = new LinkedHashMap<>();
    static {
        // 基础设施
        MC_TO_MG.put("infrastructure", CAT_INFRASTRUCTURE);
        MC_TO_MG.put("walls",           CAT_INFRASTRUCTURE);

        // 住宅
        MC_TO_MG.put("fundamentals",    CAT_RESIDENTIAL);

        // 公共设施
        MC_TO_MG.put("education",       CAT_PUBLIC);
        MC_TO_MG.put("mystic",          CAT_PUBLIC);

        // 生产建筑
        MC_TO_MG.put("agriculture",     CAT_PRODUCTION);
        MC_TO_MG.put("craftsmanship",   CAT_PRODUCTION);

        // 商业（殖民地没有直接对应的 commercial 目录，预留）
        // 农业（殖民地没有直接对应的 agriculture 目录，预留）

        // 军事
        MC_TO_MG.put("military",        CAT_MILITARY);

        // 其他
        MC_TO_MG.put("decorations",     CAT_OTHER);

        // 无缝建筑（拼图式对齐）
        MC_TO_MG.put("seamless",        CAT_SEAMLESS);
    }

    /**
     * MetroGenesis 分类显示名列表（保持顺序）。
     */
    private static final String[] MG_CATEGORIES = {
        "全部", CAT_RESIDENTIAL, CAT_PUBLIC, CAT_INFRASTRUCTURE,
        CAT_PRODUCTION, CAT_COMMERCIAL, CAT_AGRICULTURE,
        CAT_MILITARY, CAT_SEAMLESS, CAT_OTHER
    };

    /**
     * 将 MineColonies 目录名映射到 MetroGenesis 分类。
     *
     * @param mcCategory 殖民地原始目录名（如 "agriculture", "fundamentals"）
     * @return MetroGenesis 分类名（如 "生产建筑", "住宅"），未知返回 "其他"
     */
    public static String toMetroGenesisCategory(String mcCategory) {
        return MC_TO_MG.getOrDefault(mcCategory, "其他");
    }

    /**
     * 获取所有 MetroGenesis 分类名（含"全部"）。
     */
    public static String[] getAllCategories() {
        return MG_CATEGORIES;
    }

    /**
     * 获取分类索引（用于 UI 选择）。
     *
     * @param category 分类名
     * @return 索引（0=全部, 1=住宅, ...），未找到返回 -1
     */
    public static int getCategoryIndex(String category) {
        for (int i = 0; i < MG_CATEGORIES.length; i++) {
            if (MG_CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据索引获取分类名。
     */
    public static String getCategoryByIndex(int index) {
        if (index >= 0 && index < MG_CATEGORIES.length) {
            return MG_CATEGORIES[index];
        }
        return "全部";
    }

    /**
     * 判断是否为"全部"分类。
     */
    public static boolean isAllCategory(String category) {
        return "全部".equals(category) || category.isEmpty();
    }

    private CategoryMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 根据 MetroGenesis zoneType（0=住宅,1=工业,2=商业,3=农业,4=公共,5=混合）
     * 返回对应的分类名列表（用于 ZonePlanner 筛选蓝图）。
     * <p>
     * 返回列表而非单字符串，因为一个 zoneType 可能对应多个分类（如"农业"也包含在"生产建筑"中）。
     */
    public static List<String> getCategoriesForZoneType(int zoneType) {
        return switch (zoneType) {
            case 0 -> List.of(CAT_RESIDENTIAL);
            case 1 -> List.of(CAT_PRODUCTION, CAT_INFRASTRUCTURE);
            case 2 -> List.of(CAT_COMMERCIAL, CAT_PRODUCTION);
            case 3 -> List.of(CAT_AGRICULTURE, CAT_PRODUCTION);
            case 4 -> List.of(CAT_PUBLIC);
            case 5 -> List.of(CAT_RESIDENTIAL, CAT_PUBLIC, CAT_INFRASTRUCTURE,
                              CAT_PRODUCTION, CAT_COMMERCIAL, CAT_AGRICULTURE,
                              CAT_MILITARY, CAT_OTHER, CAT_SEAMLESS);
            default -> List.of(CAT_OTHER);
        };
    }
}
