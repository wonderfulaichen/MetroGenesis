package com.metrogenesis.blueprint.v1;

import com.metrogenesis.structurize.blueprints.FacingFixer;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Blueprint data class for storing structure information.
 * Adapted from Structurize.
 * <p>
 * 支持旋转/镜像变换（通过 {@link RotationMirror}），变换操作原地修改体素和调色板数据。
 * 变换后尺寸发生变化的蓝图，需重新获取尺寸信息。
 */
public class Blueprint
{
    private short sizeX, sizeY, sizeZ;
    private List<BlockState> palette;
    private short palleteSize;
    private short[][][] structure;
    private CompoundTag[][][] tileEntities;
    private List<BlockInfo> cacheBlockInfo = null;
    private String name = "";

    /** 当前旋转/镜像状态（相对于原始扫描方向） */
    private RotationMirror rotationMirror = RotationMirror.NONE;

    public Blueprint(short sizeX, short sizeY, short sizeZ)
    {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.structure = new short[sizeY][sizeZ][sizeX];
        this.tileEntities = new CompoundTag[sizeY][sizeZ][sizeX];
        this.palette = new ArrayList<>();
        this.palette.add(0, Blocks.AIR.defaultBlockState());
        this.palleteSize = 1;
    }

    public void addBlockState(final BlockPos pos, final BlockState state)
    {
        int index = -1;
        for (int i = 0; i < this.palette.size(); i++)
        {
            if (this.palette.get(i).equals(state))
            {
                index = i;
                break;
            }
        }

        if (index == -1)
        {
            index = this.palleteSize;
            this.palleteSize++;
            this.palette.add(state);
        }

        this.structure[pos.getY()][pos.getZ()][pos.getX()] = (short) index;
        cacheBlockInfo = null;
    }

    public void setTileEntity(final BlockPos pos, final CompoundTag tag)
    {
        this.tileEntities[pos.getY()][pos.getZ()][pos.getX()] = tag;
    }

    public short getSizeX() { return sizeX; }
    public short getSizeY() { return sizeY; }
    public short getSizeZ() { return sizeZ; }

    public List<BlockState> getPalette() { return palette; }
    public short getPalleteSize() { return palleteSize; }
    public short[][][] getStructure() { return structure; }
    public CompoundTag[][][] getTileEntities() { return tileEntities; }

    public int getMinBuildHeight() { return 0; }

    public int getMaxBuildHeight() { return sizeY; }

    /**
     * @param pos the position to test
     * @return true if pos is within blueprint bounds (inclusive min, exclusive max)
     */
    public boolean isPosInside(final BlockPos pos)
    {
        return pos.getX() >= 0 && pos.getX() < sizeX &&
            pos.getY() >= 0 && pos.getY() < sizeY &&
            pos.getZ() >= 0 && pos.getZ() < sizeZ;
    }

    public BlockState getBlockStateDirect(final BlockPos pos)
    {
        if (pos.getX() < 0 || pos.getX() >= sizeX ||
            pos.getY() < 0 || pos.getY() >= sizeY ||
            pos.getZ() < 0 || pos.getZ() >= sizeZ)
        {
            return Blocks.AIR.defaultBlockState();
        }
        int index = structure[pos.getY()][pos.getZ()][pos.getX()];
        if (index >= 0 && index < palette.size())
        {
            return palette.get(index);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Nullable
    public BlockEntity getBlockEntity(final BlockPos pos)
    {
        return null;
    }

    public List<BlockInfo> getBlockInfoAsList()
    {
        if (cacheBlockInfo == null)
        {
            cacheBlockInfo = new ArrayList<>();
            for (short y = 0; y < sizeY; y++)
            {
                for (short z = 0; z < sizeZ; z++)
                {
                    for (short x = 0; x < sizeX; x++)
                    {
                        BlockPos tempPos = new BlockPos(x, y, z);
                        int index = structure[y][z][x];
                        BlockState state = index >= 0 && index < palette.size() ? palette.get(index) : Blocks.AIR.defaultBlockState();
                        CompoundTag te = tileEntities[y][z][x];
                        cacheBlockInfo.add(new BlockInfo(tempPos, state, te));
                    }
                }
            }
        }
        return cacheBlockInfo;
    }

    /**
     * 获取构建的主锚点偏移量（默认取 XZ 平面中心）。
     */
    public BlockPos getPrimaryBlockOffset()
    {
        return new BlockPos(sizeX / 2, 0, sizeZ / 2);
    }

    public int getVolume()
    {
        return (int) sizeX * sizeY * sizeZ;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // ═══════════════════════════════════════════════════════════
    //  Rotation / Mirror
    // ═══════════════════════════════════════════════════════════

    /**
     * @return 当前旋转/镜像状态
     */
    public RotationMirror getRotationMirror()
    {
        return rotationMirror;
    }

    /**
     * 设置旋转/镜像状态（原地变换体素数据）。
     * 计算从当前状态到目标状态的差值，应用相对变换。
     *
     * @param rotationMirror 目标旋转/镜像状态
     */
    public void setRotationMirror(final RotationMirror rotationMirror)
    {
        final RotationMirror transformBy = this.rotationMirror.calcDifferenceTowards(rotationMirror);
        setRotationMirrorRelative(transformBy);
    }

    /**
     * 相对变换：在当前状态基础上叠加 {@code transformBy}。
     * <p>
     * 变换操作：
     * <ol>
     *   <li>调色板中所有 BlockState 按变换方向旋转</li>
     *   <li>体素数据重新排列到新尺寸</li>
     *   <li>TileEntity 位置同步更新</li>
     *   <li>缓存失效</li>
     * </ol>
     * <p>
     * 注意：不处理实体（Entity）变换——自研版蓝图暂不包含实体数据。
     *
     * @param transformBy 应用的相对旋转变换
     */
    public void setRotationMirrorRelative(final RotationMirror transformBy)
    {
        if (transformBy == RotationMirror.NONE)
        {
            return;
        }

        final Rotation rotation = transformBy.rotation();
        final Mirror mirror = transformBy.mirror();

        // 计算新尺寸（90° 旋转交换 XZ）
        final short newSizeY = sizeY;
        final short newSizeX, newSizeZ;
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
        {
            newSizeX = sizeZ;
            newSizeZ = sizeX;
        }
        else
        {
            newSizeX = sizeX;
            newSizeZ = sizeZ;
        }

        // 1. 变换调色板（先 mirror+FacingFixer，再 rotate）
        final List<BlockState> newPalette = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++)
        {
            BlockState bs = palette.get(i);
            if (transformBy.isMirrored())
            {
                bs = FacingFixer.fixMirroredFacing(bs.mirror(mirror), bs);
            }
            if (rotation != Rotation.NONE)
            {
                bs = bs.rotate(rotation);
            }
            newPalette.add(bs);
        }
        this.palette = newPalette; // structurize 在循环前赋值，供后续 STRUCTURE_VOID 判断

        // 2. 创建新体素和 TE 数组
        final short[][][] newStructure = new short[newSizeY][newSizeZ][newSizeX];
        final CompoundTag[][][] newTileEntities = new CompoundTag[newSizeY][newSizeZ][newSizeX];

        // 3. 计算变换后的最小偏移（structurize 三轴公式）
        final BlockPos extremes = transformBy.applyToPos(new BlockPos(sizeX, sizeY, sizeZ));
        final int minX = extremes.getX() < 0 ? -extremes.getX() - 1 : 0;
        final int minY = extremes.getY() < 0 ? -extremes.getY() - 1 : 0;
        final int minZ = extremes.getZ() < 0 ? -extremes.getZ() - 1 : 0;

        // 4. 遍历所有体素，变换位置
        for (short x = 0; x < sizeX; x++)
        {
            for (short y = 0; y < sizeY; y++)
            {
                for (short z = 0; z < sizeZ; z++)
                {
                    final short value = structure[y][z][x];
                    if (palette.get(value & 0xFFFF).getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    final BlockPos newPos = transformBy.applyToPos(new BlockPos(x, y, z))
                        .offset(minX, minY, minZ);

                    newStructure[newPos.getY()][newPos.getZ()][newPos.getX()] = value;

                    // 变换 TE 位置
                    final CompoundTag te = tileEntities[y][z][x];
                    if (te != null)
                    {
                        te.putInt("x", newPos.getX());
                        te.putInt("y", newPos.getY());
                        te.putInt("z", newPos.getZ());
                        newTileEntities[newPos.getY()][newPos.getZ()][newPos.getX()] = te;
                    }
                }
            }
        }

        // 5. 提交新数据
        this.structure = newStructure;
        this.tileEntities = newTileEntities;
        this.sizeX = newSizeX;
        this.sizeZ = newSizeZ;
        this.rotationMirror = this.rotationMirror.add(transformBy);

        // 6. 缓存失效
        cacheBlockInfo = null;
    }

    @Override
    public String toString()
    {
        return "Blueprint{" +
            "size=[" + sizeX + "," + sizeY + "," + sizeZ +
            "], name='" + name + '\'' +
            ", rotMir=" + rotationMirror +
            '}';
    }
}
