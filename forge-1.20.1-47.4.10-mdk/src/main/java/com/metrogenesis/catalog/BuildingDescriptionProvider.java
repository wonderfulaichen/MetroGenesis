package com.metrogenesis.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * 建筑功能说明提供器 — 为图鉴条目解析「功能说明」卡片的 lang key。
 * <p>
 * 设计：
 * <ul>
 *   <li>优先按 {@code buildingType}（设施方块类型，如 "farmer"）匹配专用说明</li>
 *   <li>无专用说明时回退到分类默认说明（保证 100% 覆盖，每张卡片都有文案）</li>
 *   <li>所有说明文本走 lang key（zh_cn / en_us 双语），在面板内用 {@code Language} 解析</li>
 * </ul>
 */
public final class BuildingDescriptionProvider
{
    /** 设施类型 → lang key（专用功能说明，覆盖分类默认）。 */
    private static final Map<String, String> TYPE_TO_KEY = new HashMap<>();
    static {
        TYPE_TO_KEY.put("farmer",        "gui.metrogenesis.catalog.desc.farmer");
        TYPE_TO_KEY.put("townhall",      "gui.metrogenesis.catalog.desc.townhall");
        TYPE_TO_KEY.put("warehouse",     "gui.metrogenesis.catalog.desc.warehouse");
        TYPE_TO_KEY.put("builder",       "gui.metrogenesis.catalog.desc.builder");
        TYPE_TO_KEY.put("miner",         "gui.metrogenesis.catalog.desc.miner");
        TYPE_TO_KEY.put("lumberjack",    "gui.metrogenesis.catalog.desc.lumberjack");
        TYPE_TO_KEY.put("cook",          "gui.metrogenesis.catalog.desc.cook");
        TYPE_TO_KEY.put("guard",         "gui.metrogenesis.catalog.desc.guard");
        TYPE_TO_KEY.put("archery",       "gui.metrogenesis.catalog.desc.archery");
        TYPE_TO_KEY.put("barracks",      "gui.metrogenesis.catalog.desc.barracks");
        TYPE_TO_KEY.put("school",        "gui.metrogenesis.catalog.desc.school");
        TYPE_TO_KEY.put("library",       "gui.metrogenesis.catalog.desc.library");
        TYPE_TO_KEY.put("hospital",      "gui.metrogenesis.catalog.desc.hospital");
        TYPE_TO_KEY.put("blacksmith",    "gui.metrogenesis.catalog.desc.blacksmith");
        TYPE_TO_KEY.put("bakery",        "gui.metrogenesis.catalog.desc.bakery");
        TYPE_TO_KEY.put("sawmill",       "gui.metrogenesis.catalog.desc.sawmill");
        TYPE_TO_KEY.put("smelter",       "gui.metrogenesis.catalog.desc.smelter");
        TYPE_TO_KEY.put("stonequarry",   "gui.metrogenesis.catalog.desc.stonequarry");
        TYPE_TO_KEY.put("fisherman",     "gui.metrogenesis.catalog.desc.fisherman");
        TYPE_TO_KEY.put("cowboy",        "gui.metrogenesis.catalog.desc.cowboy");
        TYPE_TO_KEY.put("shepherd",      "gui.metrogenesis.catalog.desc.shepherd");
        TYPE_TO_KEY.put("fletcher",      "gui.metrogenesis.catalog.desc.fletcher");
        TYPE_TO_KEY.put("composter",     "gui.metrogenesis.catalog.desc.composter");
        TYPE_TO_KEY.put("chickenherder", "gui.metrogenesis.catalog.desc.chickenherder");
        TYPE_TO_KEY.put("swineherd",     "gui.metrogenesis.catalog.desc.swineherd");
        TYPE_TO_KEY.put("beekeeper",     "gui.metrogenesis.catalog.desc.beekeeper");
        TYPE_TO_KEY.put("glassblower",   "gui.metrogenesis.catalog.desc.glassblower");
        TYPE_TO_KEY.put("university",    "gui.metrogenesis.catalog.desc.university");
        TYPE_TO_KEY.put("enchantment",   "gui.metrogenesis.catalog.desc.enchantment");
        TYPE_TO_KEY.put("tavern",        "gui.metrogenesis.catalog.desc.tavern");
        TYPE_TO_KEY.put("market",        "gui.metrogenesis.catalog.desc.market");
    }

    /** 分类 → lang key（默认说明，保证 100% 覆盖）。 */
    private static final Map<String, String> CATEGORY_TO_KEY = new HashMap<>();
    static {
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_RESIDENTIAL,    "gui.metrogenesis.catalog.desc.cat.residential");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_PUBLIC,         "gui.metrogenesis.catalog.desc.cat.public");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_INFRASTRUCTURE,  "gui.metrogenesis.catalog.desc.cat.infrastructure");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_PRODUCTION,      "gui.metrogenesis.catalog.desc.cat.production");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_COMMERCIAL,      "gui.metrogenesis.catalog.desc.cat.commercial");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_AGRICULTURE,     "gui.metrogenesis.catalog.desc.cat.agriculture");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_MILITARY,        "gui.metrogenesis.catalog.desc.cat.military");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_SEAMLESS,        "gui.metrogenesis.catalog.desc.cat.seamless");
        CATEGORY_TO_KEY.put(CategoryMapper.CAT_OTHER,           "gui.metrogenesis.catalog.desc.cat.other");
    }

    private BuildingDescriptionProvider() {}

    /**
     * 解析某条目的功能说明 lang key。
     *
     * @param buildingType 设施方块类型（可能为 ""）
     * @param category     MetroGenesis 分类
     * @return lang key（专用说明优先，否则分类默认，再否则「其他」兜底）
     */
    public static String getDescriptionKey(final String buildingType, final String category) {
        if (buildingType != null && !buildingType.isEmpty()) {
            final String k = TYPE_TO_KEY.get(buildingType.toLowerCase());
            if (k != null) return k;
        }
        final String catKey = CATEGORY_TO_KEY.get(category);
        return catKey != null ? catKey : "gui.metrogenesis.catalog.desc.cat.other";
    }

    /** 卡片标题 key。 */
    public static final String TITLE_KEY = "gui.metrogenesis.catalog.desc.title";
}
