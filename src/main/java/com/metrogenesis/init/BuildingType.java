package com.metrogenesis.init;

import java.util.List;

/**
 * 寤虹瓚绫诲瀷娉ㄥ唽鏉＄洰 鈥?鎻忚堪涓€绉嶅彲寤洪€犵殑璁炬柦
 * <p>
 * 鍙傝€?MineColonies BuildingEntry 妯″紡銆? * 姣忎釜寤虹瓚绫诲瀷缁戝畾锛氭柟鍧楁敞鍐屽悕銆佸悕绉般€佹弿杩般€佸缓閫犲弬鏁般€? */
public class BuildingType {

    private final String id;
    private final String displayName;
    private final String description;
    private final String iconChar;
    private final int buildTime;        // 寤洪€犳墍闇€鎬昏繘搴?tick
    private final int zoneRadius;       // 鍥存爮鍖哄煙鍗婂緞
    private final boolean isFacility;   // 鏄惁涓哄姛鑳芥€ц鏂斤紙vs 瑁呴グ锛?
    private final int buildHeight;      // 寤虹瓚楂樺害
    private final boolean enabled;      // 鏄惁鍦?GUI 涓彲瑙?
    private final boolean isHouse;      // 鏄惁鎻愪緵浣忔埧锛堝鍔犱汉鍙ｄ笂闄愶級
    private final String requiredJobId; // 瀵瑰簲鑱屼笟 ID锛坣ull 琛ㄧず鏃犻渶鍒嗛厤鑱屼笟锛?
    public BuildingType(String id, String displayName, String description, String iconChar,
                         int buildTime, int zoneRadius, int buildHeight,
                         boolean isFacility, boolean enabled, boolean isHouse) {
        this(id, displayName, description, iconChar, buildTime, zoneRadius, buildHeight,
             isFacility, enabled, isHouse, null);
    }

    public BuildingType(String id, String displayName, String description, String iconChar,
                         int buildTime, int zoneRadius, int buildHeight,
                         boolean isFacility, boolean enabled, boolean isHouse,
                         String requiredJobId) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.iconChar = iconChar;
        this.buildTime = buildTime;
        this.zoneRadius = zoneRadius;
        this.buildHeight = buildHeight;
        this.isFacility = isFacility;
        this.enabled = enabled;
        this.isHouse = isHouse;
        this.requiredJobId = requiredJobId;
    }

    // ══ 闈欐€侀缃?═════════════════════════════════════

    public static final BuildingType TOWN_HALL = new BuildingType(
            "town_hall", "\u5E02\u653F\u5385", "\u57CE\u5E02\u7BA1\u7406\u548C\u51B3\u7B56\u4E2D\u5FC3", "\uD83C\uDFDB",
            400, 5, 6, true, true, false, null);

    public static final BuildingType FARM = new BuildingType(
            "farm_facility", "\u519C\u573A", "\u751F\u4EA7\u98DF\u7269\u4F9B\u7ED9\u5E02\u6C11", "\uD83C\uDFDB",
            200, 3, 4, true, true, false, "farmer");

    public static final BuildingType HOUSE = new BuildingType(
            "house", "\u4F4F\u5B85", "\u4E3A\u5E02\u6C11\u63D0\u4F9B\u5C45\u4F4F\u7A7A\u95F4", "\uD83C\uDFE0",
            300, 3, 5, true, true, true, null);

    public static final BuildingType WORKSHOP = new BuildingType(
            "workshop", "\u5DE5\u574A", "\u751F\u4EA7\u5DE5\u5177\u548C\u5EFA\u7B51\u6750\u6599", "\uD83D\uDD27",
            250, 4, 4, true, true, false, "builder");

    public static final BuildingType WAREHOUSE = new BuildingType(
            "warehouse", "\u4ED3\u5E93", "\u5B58\u50A8\u7269\u8D44\u548C\u4EA7\u54C1", "\uD83D\uDCE6",
            200, 4, 4, true, true, false, "merchant");

    /** 鎵€鏈夋敞鍐岀殑寤虹瓚绫诲瀷锛堥亶鍘嗙敤锛?*/
    public static final List<BuildingType> ALL = List.of(
            TOWN_HALL, FARM, HOUSE, WORKSHOP, WAREHOUSE
    );

    // ══ 鏌ユ壘 ═════════════════════════════════════════

    public static BuildingType fromId(String id) {
        for (BuildingType t : ALL) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    // ══ getter ═══════════════════════════════════════

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIconChar() { return iconChar; }
    public int getBuildTime() { return buildTime; }
    public int getZoneRadius() { return zoneRadius; }
    public int getBuildHeight() { return buildHeight; }
    public boolean isFacility() { return isFacility; }
    public boolean isEnabled() { return enabled; }
    public boolean isHouse() { return isHouse; }
    /** 杩斿洖姝ゅ缓绛戝搴旂殑鑱屼笟 ID锛宯ull 琛ㄧず鏃犻渶鍒嗛厤鑱屼笟 */
    public String getRequiredJobId() { return requiredJobId; }
}
