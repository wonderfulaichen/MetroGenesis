package com.metrogenesis.catalog;

import java.util.LinkedHashMap;
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
    private static final Map<String, String> MC_TO_MG = new LinkedHashMap<>();
    static {
        // 基础设施
        MC_TO_MG.put("infrastructure", "基础设施");
        MC_TO_MG.put("walls",           "基础设施");

        // 住宅
        MC_TO_MG.put("fundamentals",    "住宅");

        // 公共设施
        MC_TO_MG.put("education",       "公共设施");
        MC_TO_MG.put("mystic",          "公共设施");

        // 生产建筑
        MC_TO_MG.put("agriculture",     "生产建筑");
        MC_TO_MG.put("craftsmanship",   "生产建筑");

        // 商业
        // （殖民地没有直接对应的 commercial 目录，后续可扩展）

        // 军事
        MC_TO_MG.put("military",        "军事");

        // 其他
        MC_TO_MG.put("decorations",     "其他");
    }

    /**
     * MetroGenesis 分类显示名列表（保持顺序）。
     */
    private static final String[] MG_CATEGORIES = {
        "全部", "住宅", "公共设施", "基础设施", "生产建筑", "商业", "军事", "其他"
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
}
