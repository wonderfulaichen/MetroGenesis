package com.metrogenesis.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类映射器 — MetroGenesis 自有分类系统。
 * <p>
 * 设计原则（消化而非照搬）：
 * <ul>
 *   <li><b>设施方块优先</b>（buildingType）：每个建筑方块决定其分类，覆盖所有已知 MineColonies 类型</li>
 *   <li><b>我们的路径规则兜底</b>（resourcePath）：无设施方块的建筑按自定规则分类</li>
 *   <li><b>不依赖 MineColonies 目录结构</b>：mcCategory 仅保留作引用，不参与区划生长等核心逻辑</li>
 *   <li>{@code CATALOG_DISPLAY_MAP} 仅用于图鉴 UI 的分类 tab 显示，<b>不用于区划生长</b></li>
 * </ul>
 */
public final class CategoryMapper {

    // ══════════════════════════════════════════════════════════════
    //  MetroGenesis 自有分类常量
    // ══════════════════════════════════════════════════════════════

    /** 基础设施 */
    public static final String CAT_INFRASTRUCTURE = "基础设施";
    /** 住宅 */
    public static final String CAT_RESIDENTIAL   = "住宅";
    /** 公共设施 */
    public static final String CAT_PUBLIC        = "公共设施";
    /** 生产建筑（工业） */
    public static final String CAT_PRODUCTION    = "生产建筑";
    /** 商业 */
    public static final String CAT_COMMERCIAL    = "商业";
    /** 农业 */
    public static final String CAT_AGRICULTURE   = "农业";
    /** 军事 */
    public static final String CAT_MILITARY      = "军事";
    /** 其他 */
    public static final String CAT_OTHER         = "其他";
    /** 无缝建筑 */
    public static final String CAT_SEAMLESS      = "无缝建筑";

    // ══════════════════════════════════════════════════════════════
    //  设施方块 → MetroGenesis 分类（核心映射）
    //  涵盖所有 MineColonies 54 个已注册 buildingType
    //  这是区划生长的首要判定依据
    // ══════════════════════════════════════════════════════════════

    private static final Map<String, String> BUILDING_TYPE_TO_ZONE_CATEGORY = new LinkedHashMap<>();
    static {
        // ── 农业 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("farmer",        CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("cowboy",        CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("chickenherder", CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("fisherman",     CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("shepherd",      CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("swineherder",   CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("rabbithutch",   CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("beekeeper",     CAT_AGRICULTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("plantation",    CAT_AGRICULTURE);

        // ── 商业 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("baker",         CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("tavern",        CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("cook",          CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("deliveryman",   CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("warehouse",     CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("postbox",       CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("florist",       CAT_COMMERCIAL);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("kitchen",       CAT_COMMERCIAL);

        // ── 公共设施 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("townhall",      CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("school",        CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("library",       CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("university",    CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("hospital",      CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("enchanter",     CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("mysticalsite",  CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("graveyard",     CAT_PUBLIC);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("combatacademy", CAT_PUBLIC);

        // ── 住宅 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("residence",     CAT_RESIDENTIAL);

        // ── 生产建筑（工业） ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("blacksmith",    CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("sawmill",       CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("crusher",       CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("stonemason",    CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("smeltery",      CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("stonesmeltery", CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("glassblower",   CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("dyer",          CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("fletcher",      CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("mechanic",      CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("sifter",        CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("composter",     CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("miner",         CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("lumberjack",    CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("concretemixer", CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("simplequarry",  CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("mediumquarry",  CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("alchemist",     CAT_PRODUCTION);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("netherworker",  CAT_PRODUCTION);

        // ── 基础设施 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("builder",       CAT_INFRASTRUCTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("stable",        CAT_INFRASTRUCTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("gatehouse",     CAT_INFRASTRUCTURE);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("stash",         CAT_INFRASTRUCTURE);

        // ── 军事 ──
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("archery",       CAT_MILITARY);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("barracks",      CAT_MILITARY);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("barrackstower", CAT_MILITARY);
        BUILDING_TYPE_TO_ZONE_CATEGORY.put("guardtower",    CAT_MILITARY);

        // ── 其他（暂不归类的边缘类型） ──
        // 注意："composter"、"stash" 已在上面归类
        // "largequarry" 在 ModBuildings 中已定义但注册代码被注释，不加入
    }

    // ══════════════════════════════════════════════════════════════
    //  图鉴 UI 显示映射（仅用于 catalog tab，不参与区划生长）
    //  保留 mccCategory → MG 分类的映射关系，供图鉴按目录显示
    // ══════════════════════════════════════════════════════════════

    private static final Map<String, String> CATALOG_DISPLAY_MAP = new LinkedHashMap<>();
    static {
        CATALOG_DISPLAY_MAP.put("infrastructure", CAT_INFRASTRUCTURE);
        CATALOG_DISPLAY_MAP.put("walls",           CAT_INFRASTRUCTURE);

        CATALOG_DISPLAY_MAP.put("fundamentals",    CAT_RESIDENTIAL);

        CATALOG_DISPLAY_MAP.put("education",       CAT_PUBLIC);
        CATALOG_DISPLAY_MAP.put("mystic",          CAT_PUBLIC);

        CATALOG_DISPLAY_MAP.put("craftsmanship",   CAT_PRODUCTION);
        CATALOG_DISPLAY_MAP.put("agriculture",     CAT_AGRICULTURE); // 修正：原误映射到 CAT_PRODUCTION

        CATALOG_DISPLAY_MAP.put("military",        CAT_MILITARY);

        CATALOG_DISPLAY_MAP.put("decorations",     CAT_OTHER);

        CATALOG_DISPLAY_MAP.put("seamless",        CAT_SEAMLESS);
    }

    // ══════════════════════════════════════════════════════════════
    //  MetroGenesis 路径分类规则（兜底：无设施方块时按路径关键词归类）
    //  这是"我们的规则"而非 MineColonies 目录的直接映射
    // ══════════════════════════════════════════════════════════════

    /**
     * 路径关键词 → MG 分类（用于无设施方块的建筑兜底分类）。
     * 关键词按优先级排序，从上到下匹配。
     */
    private static final Map<String, String> METROGENESIS_PATH_CLASSIFICATION = new LinkedHashMap<>();
    static {
        METROGENESIS_PATH_CLASSIFICATION.put("agriculture",   CAT_AGRICULTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("horticulture",  CAT_AGRICULTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("fields",        CAT_AGRICULTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("ranch",         CAT_AGRICULTURE);

        METROGENESIS_PATH_CLASSIFICATION.put("craftsmanship", CAT_PRODUCTION);
        METROGENESIS_PATH_CLASSIFICATION.put("blacksmith",    CAT_PRODUCTION);
        METROGENESIS_PATH_CLASSIFICATION.put("sawmill",       CAT_PRODUCTION);
        METROGENESIS_PATH_CLASSIFICATION.put("miner",         CAT_PRODUCTION);
        METROGENESIS_PATH_CLASSIFICATION.put("quarry",        CAT_PRODUCTION);

        METROGENESIS_PATH_CLASSIFICATION.put("commercial",    CAT_COMMERCIAL);
        METROGENESIS_PATH_CLASSIFICATION.put("market",        CAT_COMMERCIAL);
        METROGENESIS_PATH_CLASSIFICATION.put("delivery",      CAT_COMMERCIAL);

        METROGENESIS_PATH_CLASSIFICATION.put("fundamentals",  CAT_RESIDENTIAL);
        METROGENESIS_PATH_CLASSIFICATION.put("residence",     CAT_RESIDENTIAL);
        METROGENESIS_PATH_CLASSIFICATION.put("home",          CAT_RESIDENTIAL);

        METROGENESIS_PATH_CLASSIFICATION.put("education",     CAT_PUBLIC);
        METROGENESIS_PATH_CLASSIFICATION.put("library",       CAT_PUBLIC);
        METROGENESIS_PATH_CLASSIFICATION.put("mystic",        CAT_PUBLIC);
        METROGENESIS_PATH_CLASSIFICATION.put("townhall",      CAT_PUBLIC);

        METROGENESIS_PATH_CLASSIFICATION.put("infrastructure", CAT_INFRASTRUCTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("walls",          CAT_INFRASTRUCTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("gate",           CAT_INFRASTRUCTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("stable",         CAT_INFRASTRUCTURE);
        METROGENESIS_PATH_CLASSIFICATION.put("stash",          CAT_INFRASTRUCTURE);

        METROGENESIS_PATH_CLASSIFICATION.put("military",       CAT_MILITARY);
        METROGENESIS_PATH_CLASSIFICATION.put("barracks",       CAT_MILITARY);
        METROGENESIS_PATH_CLASSIFICATION.put("guard",          CAT_MILITARY);
        METROGENESIS_PATH_CLASSIFICATION.put("archery",        CAT_MILITARY);

        METROGENESIS_PATH_CLASSIFICATION.put("decorations",    CAT_OTHER);

        METROGENESIS_PATH_CLASSIFICATION.put("seamless",       CAT_SEAMLESS);
    }

    // ══════════════════════════════════════════════════════════════
    //  分类列表
    // ══════════════════════════════════════════════════════════════

    private static final String[] MG_CATEGORIES = {
        "全部", CAT_RESIDENTIAL, CAT_PUBLIC, CAT_INFRASTRUCTURE,
        CAT_PRODUCTION, CAT_COMMERCIAL, CAT_AGRICULTURE,
        CAT_MILITARY, CAT_SEAMLESS, CAT_OTHER
    };

    // ══════════════════════════════════════════════════════════════
    //  API
    // ══════════════════════════════════════════════════════════════

    /**
     * 将 MineColonies 目录名映射到图鉴 UI 分类。
     * <p>
     * <b>仅用于图鉴的分类 tab 显示</b>，不参与区划生长。
     *
     * @param mcCategory 殖民地原始目录名（如 "agriculture", "fundamentals"）
     * @return UI 分类名，未知返回 CAT_OTHER
     */
    public static String toMetroGenesisCategory(String mcCategory) {
        return CATALOG_DISPLAY_MAP.getOrDefault(mcCategory, CAT_OTHER);
    }

    /**
     * MetroGenesis 自有分类：判定一个建筑应归属哪个分类。
     * <p>
     * 规则（消化吸收）：
     * <ol>
     *   <li><b>设施方块优先</b>：有 buildingType 且在映射表中 → 返回 MAP 值</li>
     *   <li><b>路径兜底</b>：无设施方块的建筑按 resourcePath 关键词匹配我们的规则</li>
     *   <li><b>都不匹配 → CAT_OTHER</b>（"其他"分类，不强制归入 MC 目录）</li>
     * </ol>
     * <p>
     * 此方法在 {@link BuildingCatalogScanner} 扫描时调用，结果写入 {@link BuildingCatalogEntry#mgCategory()}。
     *
     * @param buildingType 设施方块类型名（空字符串=无设施方块）
     * @param resourcePath 包内相对路径（如 "agriculture/horticulture/farmer"）
     * @return MetroGenesis 自有分类
     */
    public static String classifyForMetroGenesis(String buildingType, String resourcePath) {
        // 1. 设施方块优先
        if (!buildingType.isEmpty()) {
            String mapped = BUILDING_TYPE_TO_ZONE_CATEGORY.get(buildingType);
            if (mapped != null) {
                return mapped;
            }
        }

        // 2. 路径兜底（我们的规则，非 MineColonies 目录的直接映射）
        String pathLower = resourcePath.toLowerCase();
        for (var entry : METROGENESIS_PATH_CLASSIFICATION.entrySet()) {
            if (pathLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. 都不匹配 → CAT_OTHER
        return CAT_OTHER;
    }

    /**
     * 获取条目在区划生长中应归属的分类。
     * <p>
     * 直接返回 {@link BuildingCatalogEntry#mgCategory()}，该值在扫描时由
     * {@link #classifyForMetroGenesis} 设定（设施方块优先 + 路径兜底），
     * 不依赖 MineColonies 的 mcCategory 目录。
     *
     * @param entry 图鉴条目
     * @return MetroGenesis 自有分类
     */
    public static String getZoneCategoryForEntry(BuildingCatalogEntry entry) {
        return entry.mgCategory();
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

    private CategoryMapper() {
        // 工具类，禁止实例化
    }
}
