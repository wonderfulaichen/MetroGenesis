package com.metrogenesis.road;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * 道路蓝图模板。
 * <p>
 * 玩家录制的一小段路面（如 3×1×3），用于沿贝塞尔曲线铺设。
 * 模板以中心线对齐 — X 轴中线 = 道路中心线，向两侧对称展开。
 * </p>
 *
 * <h3>坐标约定</h3>
 * <ul>
 *   <li>sizeX = 道路宽度方向（垂直于前进方向）</li>
 *   <li>sizeZ = 道路前进方向</li>
 *   <li>sizeY = 道路高度方向（通常为 1）</li>
 *   <li>blocks[x][y][z] — 本地坐标</li>
 * </ul>
 *
 * <h3>旋转</h3>
 * 模板可绕 Y 轴旋转 90° 的整数倍，旋转结果缓存在 {@code rotatedBlocks} 中。
 *
 * @author program-yuan (Phase 1)
 */
public class RoadTemplate {

    // ══ 模板类型 ══════════════════════════════════════════

    /**
     * 模板类型枚举。
     */
    public enum TemplateType {
        /** 直道 */
        STRAIGHT,
        /** 左转 */
        TURN_LEFT,
        /** 右转 */
        TURN_RIGHT,
        /** T 字路口 */
        JUNCTION_T,
        /** 十字路口 */
        JUNCTION_CROSS,
        /** 终点 / 端头 */
        ENDPOINT
    }

    // ══ 字段 ══════════════════════════════════════════════

    /** 模板名称（玩家自定义） */
    private final String name;

    /** 模板类型 */
    private final TemplateType type;

    /** 尺寸：宽（X），高（Y），深（Z） */
    private final int sizeX, sizeY, sizeZ;

    /** 方块数组 [x][y][z]，原始朝向 */
    private final BlockState[][][] blocks;

    /** 路基方块（用于填充路面下方空隙） */
    private final BlockState foundationBlock;

    /** 旋转后的方块缓存 [rotationSteps]，索引 0 = 原始 */
    private BlockState[][][] rotatedBlocksCache;

    // ══ 构造 ══════════════════════════════════════════════

    /**
     * 创建道路模板。
     *
     * @param name             模板名称
     * @param type             模板类型
     * @param blocks           方块数组 [x][y][z]
     * @param foundationBlock  路基方块
     */
    public RoadTemplate(String name, TemplateType type, BlockState[][][] blocks, BlockState foundationBlock) {
        Objects.requireNonNull(name, "Template name must not be null");
        Objects.requireNonNull(type, "Template type must not be null");
        Objects.requireNonNull(blocks, "Blocks array must not be null");

        this.name = name;
        this.type = type;
        this.blocks = blocks;
        this.foundationBlock = foundationBlock != null ? foundationBlock : Blocks.DIRT.defaultBlockState();

        // 推断尺寸
        this.sizeX = blocks.length;
        this.sizeY = sizeX > 0 ? blocks[0].length : 0;
        this.sizeZ = sizeX > 0 && sizeY > 0 ? blocks[0][0].length : 0;
    }

    /**
     * 创建空模板（所有 AIR），用于占位。
     */
    public RoadTemplate(String name, TemplateType type, int sizeX, int sizeY, int sizeZ, BlockState foundationBlock) {
        this.name = name;
        this.type = type;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.foundationBlock = foundationBlock != null ? foundationBlock : Blocks.DIRT.defaultBlockState();
        this.blocks = new BlockState[sizeX][sizeY][sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    blocks[x][y][z] = Blocks.AIR.defaultBlockState();
                }
            }
        }
    }

    // ══ 旋转 ══════════════════════════════════════════════

    /**
     * 绕 Y 轴旋转模板。
     * <p>
     * 旋转步数：0 = 原始，1 = 90°，2 = 180°，3 = 270°（顺时针）。
     * 旋转后 X/Z 尺寸可能交换（奇数步时）。
     * </p>
     *
     * @param steps 旋转步数 [0, 3]
     * @return 旋转后的新模板（不修改原始模板）
     */
    public RoadTemplate rotate(int steps) {
        steps = ((steps % 4) + 4) % 4; // 归一化到 [0, 3]
        if (steps == 0) return this;

        // 计算旋转后尺寸
        int newSizeX, newSizeZ;
        if (steps == 1 || steps == 3) {
            newSizeX = sizeZ;
            newSizeZ = sizeX;
        } else {
            newSizeX = sizeX;
            newSizeZ = sizeZ;
        }

        BlockState[][][] rotated = new BlockState[newSizeX][sizeY][newSizeZ];

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int[] newPos = rotateCoords(x, z, sizeX, sizeZ, steps);
                    // 方块状态自身也需要旋转（如楼梯、门等方向性方块）
                    Rotation mcRotation = switch (steps) {
                        case 1 -> Rotation.CLOCKWISE_90;
                        case 2 -> Rotation.CLOCKWISE_180;
                        case 3 -> Rotation.COUNTERCLOCKWISE_90;
                        default -> Rotation.NONE;
                    };
                    rotated[newPos[0]][y][newPos[1]] = blocks[x][y][z].rotate(mcRotation);
                }
            }
        }

        return new RoadTemplate(name + "_r" + (steps * 90), type, rotated, foundationBlock);
    }

    /**
     * 获取缓存的旋转版本。
     *
     * @param steps 旋转步数 [0, 3]
     * @return 旋转后的模板
     */
    public RoadTemplate getRotated(int steps) {
        if (rotatedBlocksCache == null) {
            // 只缓存 180° 版本（最常用的反转），其余直接计算
        }
        return rotate(steps);
    }

    // ══ 方块访问 ═════════════════════════════════════════

    /**
     * 获取指定本地坐标的方块状态。
     *
     * @param x 本地 X [0, sizeX)
     * @param y 本地 Y [0, sizeY)
     * @param z 本地 Z [0, sizeZ)
     * @return 方块状态，越界返回 AIR
     */
    public BlockState getBlock(int x, int y, int z) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            return Blocks.AIR.defaultBlockState();
        }
        return blocks[x][y][z];
    }

    /**
     * 设置指定本地坐标的方块状态。
     *
     * @param x     本地 X
     * @param y     本地 Y
     * @param z     本地 Z
     * @param state 方块状态
     */
    public void setBlock(int x, int y, int z, BlockState state) {
        if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ) {
            blocks[x][y][z] = state;
        }
    }

    /**
     * 检查指定位置是否为实心方块（用于碰撞/放置判断）。
     */
    public boolean isSolid(int x, int y, int z) {
        BlockState state = getBlock(x, y, z);
        return !state.isAir() && state.canOcclude();
    }

    // ══ 模板放置到世界 ═══════════════════════════════════

    /**
     * 将模板放置到世界中。
     * <p>
     * origin 为模板中心线起点（Z 方向中心点）。
     * 模板以中心线对齐展开。
     * </p>
     */
    public void placeInWorld(net.minecraft.world.level.Level level, BlockPos origin, float yaw, boolean useFoundation) {
        placeInWorld(level, origin, yaw, useFoundation, false);
    }

    /**
     * 将模板放置到世界中（带覆盖保护）。
     *
     * @param skipExisting 为 true 时，跳过已是道路方块（SMOOTH_STONE / STONE_BRICKS）的格位
     */
    public void placeInWorld(net.minecraft.world.level.Level level, BlockPos origin, float yaw,
                              boolean useFoundation, boolean skipExisting) {
        // 方向向量：Z 轴→前进方向，X 轴→垂直方向（右）
        float fwdX  = (float) -Math.sin(yaw);
        float fwdZ  = (float)  Math.cos(yaw);
        float rightX = (float)  Math.cos(yaw);
        float rightZ = (float)  Math.sin(yaw);

        int halfW = sizeX / 2;
        int halfZ = sizeZ / 2;

        // 块状态旋转
        int mcSteps = yawToRotationSteps(yaw);
        Rotation mcRotation = switch (mcSteps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        // 1. 放置模板方块
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockState state = blocks[x][y][z];
                    if (state.isAir()) continue;
                    state = state.rotate(mcRotation);

                    float localX = x - halfW;
                    float localZ = z - halfZ;

                    int wx = origin.getX() + Math.round(localX * rightX + localZ * fwdX);
                    int wy = origin.getY() + y;
                    int wz = origin.getZ() + Math.round(localX * rightZ + localZ * fwdZ);

                    BlockPos worldPos = new BlockPos(wx, wy, wz);

                    // 覆盖保护：跳过已是道路方块的格位
                    if (skipExisting) {
                        BlockState existing = level.getBlockState(worldPos);
                        if (existing.getBlock() == net.minecraft.world.level.block.Blocks.SMOOTH_STONE
                            || existing.getBlock() == net.minecraft.world.level.block.Blocks.STONE_BRICKS) {
                            continue;
                        }
                    }

                    level.setBlock(worldPos, state, 3);
                }
            }
        }

        // 2. 路基填充（用方向向量计算 XZ 范围）
        if (useFoundation) {
            // 计算模板在世界空间的 XZ bounding box
            int minWx = Integer.MAX_VALUE, maxWx = Integer.MIN_VALUE;
            int minWz = Integer.MAX_VALUE, maxWz = Integer.MIN_VALUE;
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    if (blocks[x][0][z].isAir()) continue; // 只看 Y=0 层
                    float localX = x - halfW;
                    float localZ = z - halfZ;
                    int wx = origin.getX() + Math.round(localX * rightX + localZ * fwdX);
                    int wz = origin.getZ() + Math.round(localX * rightZ + localZ * fwdZ);
                    minWx = Math.min(minWx, wx);
                    maxWx = Math.max(maxWx, wx);
                    minWz = Math.min(minWz, wz);
                    maxWz = Math.max(maxWz, wz);
                }
            }
            for (int wx = minWx; wx <= maxWx; wx++) {
                for (int wz = minWz; wz <= maxWz; wz++) {
                    for (int y = origin.getY() - 1; y > level.getMinBuildHeight(); y--) {
                        BlockPos pos = new BlockPos(wx, y, wz);
                        if (level.getBlockState(pos).isSolid()) break;
                        level.setBlock(pos, foundationBlock, 3);
                    }
                }
            }
        }
    }

    // ══ NBT 序列化 ═══════════════════════════════════════

    /**
     * 保存模板为 NBT。
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("type", type.name());
        tag.putInt("sizeX", sizeX);
        tag.putInt("sizeY", sizeY);
        tag.putInt("sizeZ", sizeZ);
        tag.put("foundation", NbtUtils.writeBlockState(foundationBlock));

        // 方块数组 → 列表
        ListTag blockList = new ListTag();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("x", x);
                    entry.putInt("y", y);
                    entry.putInt("z", z);
                    entry.put("state", NbtUtils.writeBlockState(blocks[x][y][z]));
                    blockList.add(entry);
                }
            }
        }
        tag.put("blocks", blockList);
        return tag;
    }

    /**
     * 从 NBT 加载模板。
     */
    public static RoadTemplate load(CompoundTag tag) {
        String name = tag.getString("name");
        TemplateType type = TemplateType.valueOf(tag.getString("type"));
        int sizeX = tag.getInt("sizeX");
        int sizeY = tag.getInt("sizeY");
        int sizeZ = tag.getInt("sizeZ");
        BlockState foundation = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("foundation"));

        BlockState[][][] blocks = new BlockState[sizeX][sizeY][sizeZ];
        // 初始化为空气
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    blocks[x][y][z] = Blocks.AIR.defaultBlockState();
                }
            }
        }

        // 从列表恢复
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag entry = blockList.getCompound(i);
            int x = entry.getInt("x");
            int y = entry.getInt("y");
            int z = entry.getInt("z");
            if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ) {
                blocks[x][y][z] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"));
            }
        }

        return new RoadTemplate(name, type, blocks, foundation);
    }

    // ══ Getters ═══════════════════════════════════════════

    public String getName() { return name; }
    public TemplateType getType() { return type; }
    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }
    public BlockState getFoundationBlock() { return foundationBlock; }
    public BlockState[][][] getBlocks() { return blocks; }

    /**
     * 获取道路宽度（X 方向）。
     */
    public int getWidth() { return sizeX; }

    /**
     * 获取前进段长度（Z 方向）。
     */
    public int getLength() { return sizeZ; }

    // ══ 内部工具 ═════════════════════════════════════════

    /**
     * 绕原点旋转坐标。
     * <p>
     * 以 (0,0) 为中心，对 (x, z) 做 steps×90° 顺时针旋转。
     * 注意：旋转中心为数组中心 (sizeX/2, sizeZ/2)。
     * </p>
     *
     * @return [newX, newZ]
     */
    private static int[] rotateCoords(int x, int z, int sizeX, int sizeZ, int steps) {
        // 以中心为原点
        double cx = (sizeX - 1) / 2.0;
        double cz = (sizeZ - 1) / 2.0;
        double rx = x - cx;
        double rz = z - cz;

        for (int i = 0; i < steps; i++) {
            // 顺时针90°: (x,z) → (z, -x)
            double tmp = rx;
            rx = rz;
            rz = -tmp;
        }

        int newX = (int) Math.round(rx + cx);
        int newZ = (int) Math.round(rz + cz);

        // 边界检查
        newX = Math.max(0, Math.min(sizeX - 1, newX));
        newZ = Math.max(0, Math.min(sizeZ - 1, newZ));

        return new int[]{newX, newZ};
    }

    /**
     * 将 yaw 弧度量化到最近的旋转步数。
     *
     * @param yaw 弧度
     * @return 步数 [0, 3]
     */
    public static int yawToRotationSteps(float yaw) {
        // 归一化到 [0, 2π)
        double normalized = ((yaw % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        // 量化到 0/1/2/3
        int steps = (int) Math.round(normalized / (Math.PI / 2));
        return ((steps % 4) + 4) % 4;
    }

    // ══ 预制模板工厂 ═════════════════════════════════════

    /**
     * 创建默认 3×1×3 直道模板。
     * <pre>
     *   S S S
     *   P P P    S = 路缘 (石砖)
     *   S S S    P = 路面 (平滑石)
     * </pre>
     */
    public static RoadTemplate createDefaultStraight() {
        BlockState curb = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState surface = Blocks.SMOOTH_STONE.defaultBlockState();

        BlockState[][][] blocks = new BlockState[3][1][3];
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks[x][0][z] = (x == 0 || x == 2) ? curb : surface;
            }
        }

        return new RoadTemplate("default_straight", TemplateType.STRAIGHT, blocks, Blocks.DIRT.defaultBlockState());
    }

    @Override
    public String toString() {
        return "RoadTemplate[name=" + name + ", type=" + type
            + ", size=" + sizeX + "x" + sizeY + "x" + sizeZ + "]";
    }
}
