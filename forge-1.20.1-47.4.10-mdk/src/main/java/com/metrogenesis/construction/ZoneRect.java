package com.metrogenesis.construction;

/**
 * 功能区矩形 — 模拟城市风格的区域规划数据。
 * <p>
 * 玩家在 MayorBook 中拖拽绘制的矩形功能区，包含类型、朝向、密度等参数。
 * 从 MayorBookScreen 的私有 record 抽取为独立类，以便 ZonePlanner/ZoneBuilder 访问。
 */
public class ZoneRect {

    /** 区划建造阶段常量 */
    public static final int STAGE_PLANNING  = 0;
    public static final int STAGE_PENDING   = 1;
    public static final int STAGE_BUILDING  = 2;
    public static final int STAGE_COMPLETED = 3;

    /** 密度常量 */
    public static final int DENSITY_LOW    = 0;
    public static final int DENSITY_MEDIUM = 1;
    public static final int DENSITY_HIGH   = 2;

    /** 所有权常量 */
    public static final int OWNERSHIP_PRIVATE = 0;
    public static final int OWNERSHIP_STATE   = 1;

    // ═══ 不变字段 ═══

    private final int minX, minZ, maxX, maxZ;
    private final int zoneType;      // 0=住宅 1=工业 2=商业 3=农业 4=公共 5=混合
    private int direction;           // 0=东 1=南 2=西 3=北，建筑正面
    private int stage;               // 建造阶段（planning/pending/building/completed）
    private int density;             // 密度（low/medium/high）
    private int ownership;           // 所有权（private/state）

    public ZoneRect(int minX, int minZ, int maxX, int maxZ, int zoneType, int direction, int stage) {
        this(minX, minZ, maxX, maxZ, zoneType, direction, stage, DENSITY_MEDIUM, OWNERSHIP_PRIVATE);
    }

    public ZoneRect(int minX, int minZ, int maxX, int maxZ, int zoneType, int direction, int stage,
                    int density, int ownership) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.zoneType = zoneType;
        this.direction = direction;
        this.stage = stage;
        this.density = density;
        this.ownership = ownership;
    }

    // ═══ getter ═══

    public int minX()  { return minX; }
    public int minZ()  { return minZ; }
    public int maxX()  { return maxX; }
    public int maxZ()  { return maxZ; }
    public int zoneType() { return zoneType; }
    public int direction() { return direction; }
    public int stage() { return stage; }
    public int density() { return density; }
    public int ownership() { return ownership; }

    // ═══ setter ═══

    public void setDirection(int direction) { this.direction = direction; }
    public void setStage(int stage) { this.stage = stage; }
    public void setDensity(int density) { this.density = density; }
    public void setOwnership(int ownership) { this.ownership = ownership; }

    // ═══ 便捷方法 ═══

    /** 区域面积（方块数） */
    public int area() { return (maxX - minX) * (maxZ - minZ); }

    /** 区域宽度（沿 road-facing 边的长度） */
    public int width() {
        return (direction == 0 || direction == 2) ? (maxZ - minZ) : (maxX - minX);
    }

    /** 区域深度（垂直于 road-facing 边的长度） */
    public int depth() {
        return (direction == 0 || direction == 2) ? (maxX - minX) : (maxZ - minZ);
    }

    /** 区域中心 X */
    public int centerX() { return (minX + maxX) / 2; }

    /** 区域中心 Z */
    public int centerZ() { return (minZ + maxZ) / 2; }

    /** 获取正面方向（朝向道路/建筑入口的一侧）对应的边坐标 */
    public int frontX() {
        return switch (direction) {
            case 0 -> maxX + 1;  // 东：maxX 边
            case 2 -> minX;      // 西：minX 边
            default -> centerX();
        };
    }
    public int frontZ() {
        return switch (direction) {
            case 1 -> maxZ + 1;  // 南：maxZ 边
            case 3 -> minZ;      // 北：minZ 边
            default -> centerZ();
        };
    }

    /** 序列化为 int 数组（用于网络传输和 ZoneData 持久化） */
    public int[] toIntArray() {
        return new int[]{minX, minZ, maxX, maxZ, zoneType, direction, stage, density, ownership};
    }

    /** 从 int 数组反序列化 */
    public static ZoneRect fromIntArray(int[] a) {
        int density = a.length >= 8 ? a[7] : DENSITY_MEDIUM;
        int ownership = a.length >= 9 ? a[8] : OWNERSHIP_PRIVATE;
        return new ZoneRect(a[0], a[1], a[2], a[3], a[4], a[5], a.length >= 7 ? a[6] : 0,
            density, ownership);
    }

    /** 检测是否与另一个矩形重叠 */
    public boolean overlaps(ZoneRect other) {
        return minX < other.maxX && maxX > other.minX
            && minZ < other.maxZ && maxZ > other.minZ;
    }

    /** 检测两个 ZoneRect 是否相邻（共享边长度 ≥ 1）且同类型 */
    public boolean isAdjacentSameType(ZoneRect other) {
        if (this.zoneType != other.zoneType) return false;
        // 共享垂直边：左/右边对齐，z 方向重叠
        if (this.maxX == other.minX || this.minX == other.maxX) {
            return this.minZ < other.maxZ && this.maxZ > other.minZ;
        }
        // 共享水平边：上/下边对齐，x 方向重叠
        if (this.maxZ == other.minZ || this.minZ == other.maxZ) {
            return this.minX < other.maxX && this.maxX > other.minX;
        }
        return false;
    }

    /** 合并两个同类型相邻区的外框（不检查类型/相邻性，调用前应先验证） */
    public static ZoneRect merge(ZoneRect a, ZoneRect b) {
        int dir = a.direction; // 用第一个区的朝向
        int stage = Math.min(a.stage, b.stage);
        int dens = Math.max(a.density, b.density);
        int own = a.ownership;
        return new ZoneRect(
            Math.min(a.minX, b.minX),
            Math.min(a.minZ, b.minZ),
            Math.max(a.maxX, b.maxX),
            Math.max(a.maxZ, b.maxZ),
            a.zoneType, dir, stage, dens, own
        );
    }

    @Override
    public String toString() {
        return "ZoneRect[" + minX + "," + minZ + "→" + maxX + "," + maxZ
            + " type=" + zoneType + " dir=" + direction + " stage=" + stage
            + " dens=" + density + " own=" + ownership + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZoneRect z)) return false;
        return minX == z.minX && minZ == z.minZ
            && maxX == z.maxX && maxZ == z.maxZ
            && zoneType == z.zoneType
            && direction == z.direction
            && stage == z.stage
            && density == z.density
            && ownership == z.ownership;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(minX, minZ, maxX, maxZ, zoneType, direction, stage, density, ownership);
    }
}
