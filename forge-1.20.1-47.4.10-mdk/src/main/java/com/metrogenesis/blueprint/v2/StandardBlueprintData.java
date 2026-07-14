package com.metrogenesis.blueprint.v2;

import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * StandardBlueprintData — 标准化蓝图元数据。
 * <p>
 * 编码标准化蓝图约定：
 * <ul>
 *   <li><b>门朝向</b> {@link DoorDirection}：建筑入口方向，即俯视时建筑的底边方向。
 *       玩家站在门外正对建筑时看到的那个面，就是门所在的面。</li>
 *   <li><b>角点约定</b>：正对门时，左下角 = 起点（左键单击），右上角 = 终点（右键单击）。
 *       这意味着在蓝图网格坐标系中，Z 轴正方向为建筑内部进深方向（远离门）。</li>
 *   <li><b>分类体系</b>：{@link #mainCategory} / {@link #subCategory} 用于管理手册的建筑编目。</li>
 *   <li><b>旋转变换</b> {@link RotationMirror}：当前已应用的旋转/镜像状态。</li>
 * </ul>
 * <p>
 * 生命周期：
 * <ol>
 *   <li>蓝图扫描后 → {@link BlueprintCaptureResultScreen} 计算门方向 → 创建此元数据</li>
 *   <li>保存时 → {@link #serializeNBT()} 序列化为 NBT，随蓝图存入 {@link BlueprintLibraryData}</li>
 *   <li>放置时 → 反序列化，根据门方向+旋转状态计算正确放置朝向</li>
 *   <li>旋转/镜像操作 → 更新 {@link #rotationMirror} 字段</li>
 * </ol>
 */
public class StandardBlueprintData
{
    // ═══════════════════════════════════════════════════════════
    //  Enums
    // ═══════════════════════════════════════════════════════════

    /**
     * 门朝向 — 建筑入口方向。
     * <p>
     * 定义：从建筑正上方俯视时，门所在的边所对应的方向。
     * 例如门朝北（NORTH）意味着建筑的正门在俯视图的下侧（Z 负方向）。
     * <p>
     * 与 {@link Direction} 的对应关系：
     * <ul>
     *   <li>NORTH → Z-（俯视下方），建筑延伸方向为 +Z</li>
     *   <li>SOUTH → Z+（俯视上方），建筑延伸方向为 -Z</li>
     *   <li>EAST → X+（俯视右方），建筑延伸方向为 -X</li>
     *   <li>WEST → X-（俯视左方），建筑延伸方向为 +X</li>
     * </ul>
     */
    public enum DoorDirection
    {
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH),
        EAST(Direction.EAST),
        WEST(Direction.WEST);

        private final Direction facing;

        DoorDirection(Direction facing)
        {
            this.facing = facing;
        }

        /** 返回对应的 Minecraft {@link Direction} */
        public Direction toFacing()
        {
            return facing;
        }

        /** 从 {@link Direction} 转换 */
        public static DoorDirection fromFacing(Direction facing)
        {
            for (DoorDirection d : values())
            {
                if (d.facing == facing) return d;
            }
            return NORTH;
        }

        /** 从字符串解析（大小写不敏感），无效值返回 {@link #NORTH} */
        public static DoorDirection fromString(String name)
        {
            if (name == null || name.isEmpty()) return NORTH;
            try
            {
                return valueOf(name.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException e)
            {
                return NORTH;
            }
        }

        /** 顺时针旋转 90° */
        public DoorDirection rotateCW()
        {
            switch (this)
            {
                case NORTH:  return EAST;
                case EAST:   return SOUTH;
                case SOUTH:  return WEST;
                case WEST:   return NORTH;
                default:     return NORTH;
            }
        }

        /** 逆时针旋转 90° */
        public DoorDirection rotateCCW()
        {
            switch (this)
            {
                case NORTH:  return WEST;
                case WEST:   return SOUTH;
                case SOUTH:  return EAST;
                case EAST:   return NORTH;
                default:     return NORTH;
            }
        }

        /** 反向（180°旋转） */
        public DoorDirection opposite()
        {
            switch (this)
            {
                case NORTH:  return SOUTH;
                case SOUTH:  return NORTH;
                case EAST:   return WEST;
                case WEST:   return EAST;
                default:     return NORTH;
            }
        }
    }

    /**
     * 拼图式缝类型 — 定义建筑哪几个面是对齐边。
     * <p>
     * 蓝图放在风格包的 {@code seamless/} 子目录中，
     * 建筑在建模时就确保指定面是平的，放置时自动对齐隔壁。
     */
    public enum SeamlessType
    {
        /** 普通建筑，无对齐要求 */
        NONE(""),

        /** 左侧面平（俯视时 X- 方向） */
        LEFT_WALL("left"),

        /** 右侧面平（俯视时 X+ 方向） */
        RIGHT_WALL("right"),

        /** 左右双面平 */
        BOTH_WALLS("both"),

        /** 内转角：左侧面 + 背面平 */
        INNER_CORNER("inner"),

        /** 外转角：左侧面 + 前面平 */
        OUTER_CORNER("outer");

        private final String dirSuffix;

        SeamlessType(String dirSuffix)
        {
            this.dirSuffix = dirSuffix;
        }

        /** 子目录名（如 "left", "right"），用于 {@code seamless/} 目录 */
        public String getDirSuffix()
        {
            return dirSuffix;
        }

        /** 是否为无缝建筑 */
        public boolean isSeamless()
        {
            return this != NONE;
        }

        /** 从字符串解析，大小写不敏感 */
        public static SeamlessType fromString(String name)
        {
            if (name == null || name.isEmpty()) return NONE;
            try
            {
                return valueOf(name.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException e)
            {
                return NONE;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  NBT Keys
    // ═══════════════════════════════════════════════════════════

    private static final String TAG_BLUEPRINT_NAME = "blueprint_name";
    private static final String TAG_DOOR_DIRECTION = "door_direction";
    private static final String TAG_MAIN_CATEGORY  = "main_category";
    private static final String TAG_SUB_CATEGORY   = "sub_category";
    private static final String TAG_ROTATION       = "rotation_mirror";
    private static final String TAG_CREATED_AT     = "created_at";
    private static final String TAG_SEAMLESS_TYPE  = "seamless_type";

    // ═══════════════════════════════════════════════════════════
    //  Fields
    // ═══════════════════════════════════════════════════════════

    /** 关联的蓝图名称（在 {@link BlueprintLibraryData} 中的查找键） */
    private String blueprintName = "";

    /** 门朝向（capture 时的原始朝向） */
    private DoorDirection doorDirection = DoorDirection.NORTH;

    /** 主分类，例如 "public_facilities"、"infrastructure"、"residence" */
    private String mainCategory = "";

    /** 子分类，例如 "hospital"、"road"、"apartment" */
    private String subCategory = "";

    /** 当前旋转/镜像状态 */
    private RotationMirror rotationMirror = RotationMirror.NONE;

    /** 创建时间戳（毫秒） */
    private long createdAt = 0L;

    /** 拼图式缝类型 */
    private SeamlessType seamlessType = SeamlessType.NONE;

    // ═══════════════════════════════════════════════════════════
    //  Constructors
    // ═══════════════════════════════════════════════════════════

    public StandardBlueprintData() {}

    /**
     * 创建标准蓝图元数据。
     *
     * @param blueprintName 蓝图名称
     * @param doorDirection 门朝向
     */
    public StandardBlueprintData(String blueprintName, DoorDirection doorDirection)
    {
        this.blueprintName = blueprintName;
        this.doorDirection = doorDirection;
        this.createdAt = System.currentTimeMillis();
    }

    // ═══════════════════════════════════════════════════════════
    //  Getters & Setters
    // ═══════════════════════════════════════════════════════════

    public String getBlueprintName()
    {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName)
    {
        this.blueprintName = blueprintName;
    }

    public DoorDirection getDoorDirection()
    {
        return doorDirection;
    }

    public void setDoorDirection(DoorDirection doorDirection)
    {
        this.doorDirection = doorDirection;
    }

    public String getMainCategory()
    {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory)
    {
        this.mainCategory = mainCategory;
    }

    public String getSubCategory()
    {
        return subCategory;
    }

    public void setSubCategory(String subCategory)
    {
        this.subCategory = subCategory;
    }

    public RotationMirror getRotationMirror()
    {
        return rotationMirror;
    }

    public void setRotationMirror(RotationMirror rotationMirror)
    {
        this.rotationMirror = rotationMirror;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(long createdAt)
    {
        this.createdAt = createdAt;
    }

    /** @return 拼图式缝类型，{@link SeamlessType#NONE} 表示普通建筑 */
    public SeamlessType getSeamlessType()
    {
        return seamlessType;
    }

    public void setSeamlessType(SeamlessType seamlessType)
    {
        this.seamlessType = seamlessType != null ? seamlessType : SeamlessType.NONE;
    }

    // ═══════════════════════════════════════════════════════════
    //  Convenience Methods
    // ═══════════════════════════════════════════════════════════

    /**
     * 获取完整分类路径，格式 "main/sub"。
     * 如果没有分类信息返回空字符串。
     */
    public String getCategoryPath()
    {
        if (mainCategory.isEmpty()) return "";
        if (subCategory.isEmpty()) return mainCategory;
        return mainCategory + "/" + subCategory;
    }

    /**
     * 是否有分类信息。
     */
    public boolean hasCategory()
    {
        return !mainCategory.isEmpty();
    }

    /**
     * 设置分类。
     *
     * @param mainCategory 主分类
     * @param subCategory  子分类（可为空）
     */
    public void setCategory(String mainCategory, @Nullable String subCategory)
    {
        this.mainCategory = mainCategory != null ? mainCategory : "";
        this.subCategory = subCategory != null ? subCategory : "";
    }

    /**
     * 获取建筑在蓝图网格中的"内部方向"（从门指向建筑内部）。
     * <p>
     * 例如门朝 NORTH 时，内部方向为 SOUTH（Z+方向）。
     */
    public Direction getInteriorDirection()
    {
        return doorDirection.toFacing().getOpposite();
    }

    /**
     * 从世界方向获取初始放置偏移量（相对放置点）。
     * <p>
     * 返回在 XZ 平面上使建筑正门面向指定方向所需的旋转后的相对偏移基准。
     * 用于在放置时计算建筑的路边退让距离。
     *
     * @param worldFacing 建筑在世界的朝向（门面向的方向）
     * @return 一个长度 2 的数组 [xOffset, zOffset]，表示相对于放置点的偏移方向
     */
    public int[] getPlacementOffset(Direction worldFacing)
    {
        // 基准：门朝 NORTH 时，建筑在 Z+ 方向延伸
        // 转换公式：以 worldFacing 为基准，建筑主体在 opposite(worldFacing) 方向
        switch (worldFacing)
        {
            case NORTH: return new int[]{0, 1};  // 建筑在 Z+（北侧门，建筑在门后方即南面）
            case SOUTH: return new int[]{0, -1}; // 建筑在 Z-
            case EAST:  return new int[]{-1, 0}; // 建筑在 X-
            case WEST:  return new int[]{1, 0};  // 建筑在 X+
            default:    return new int[]{0, 0};
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  NBT Serialization
    // ═══════════════════════════════════════════════════════════

    /**
     * 序列化为 NBT。
     */
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();

        tag.putString(TAG_BLUEPRINT_NAME, blueprintName);
        tag.putString(TAG_DOOR_DIRECTION, doorDirection.name());
        tag.putString(TAG_MAIN_CATEGORY,  mainCategory);
        tag.putString(TAG_SUB_CATEGORY,   subCategory);
        tag.putString(TAG_ROTATION,       rotationMirror.name());
        tag.putLong(TAG_CREATED_AT,       createdAt);
        tag.putString(TAG_SEAMLESS_TYPE,  seamlessType.name());

        return tag;
    }

    /**
     * 从 NBT 反序列化。
     *
     * @param tag NBT 数据
     * @return 反序列化后的实例，失败时返回默认构造的实例
     */
    public static StandardBlueprintData deserializeNBT(CompoundTag tag)
    {
        StandardBlueprintData data = new StandardBlueprintData();

        if (tag.contains(TAG_BLUEPRINT_NAME, Tag.TAG_STRING))
            data.blueprintName = tag.getString(TAG_BLUEPRINT_NAME);

        if (tag.contains(TAG_DOOR_DIRECTION, Tag.TAG_STRING))
            data.doorDirection = DoorDirection.fromString(tag.getString(TAG_DOOR_DIRECTION));

        if (tag.contains(TAG_MAIN_CATEGORY, Tag.TAG_STRING))
            data.mainCategory = tag.getString(TAG_MAIN_CATEGORY);

        if (tag.contains(TAG_SUB_CATEGORY, Tag.TAG_STRING))
            data.subCategory = tag.getString(TAG_SUB_CATEGORY);

        if (tag.contains(TAG_ROTATION, Tag.TAG_STRING))
        {
            try
            {
                data.rotationMirror = RotationMirror.valueOf(tag.getString(TAG_ROTATION));
            }
            catch (IllegalArgumentException e)
            {
                data.rotationMirror = RotationMirror.NONE;
            }
        }

        if (tag.contains(TAG_CREATED_AT, Tag.TAG_ANY_NUMERIC))
            data.createdAt = tag.getLong(TAG_CREATED_AT);

        if (tag.contains(TAG_SEAMLESS_TYPE, Tag.TAG_STRING))
            data.seamlessType = SeamlessType.fromString(tag.getString(TAG_SEAMLESS_TYPE));

        return data;
    }

    // ═══════════════════════════════════════════════════════════
    //  Object Overrides
    // ═══════════════════════════════════════════════════════════

    @Override
    public String toString()
    {
        return "StandardBlueprintData{" +
            "blueprintName='" + blueprintName + '\'' +
            ", doorDirection=" + doorDirection +
            ", category=" + getCategoryPath() +
            ", rotationMirror=" + rotationMirror +
            '}';
    }
}
