package com.metrogenesis.blueprint.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.metrogenesis.client.fakelevel.IFakeLevelBlockGetter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Blueprint data class for storing structure information.
 * Adapted from Structurize.
 */
public class Blueprint implements IFakeLevelBlockGetter
{
    private short sizeX, sizeY, sizeZ;
    private List<BlockState> palette;
    private short palleteSize;
    private short[][][] structure;
    private CompoundTag[][][] tileEntities;
    private List<BlockInfo> cacheBlockInfo = null;
    private String name = "";

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

    @Override
    public short getSizeX() { return sizeX; }
    @Override
    public int getSizeY() { return sizeY; }
    @Override
    public short getSizeZ() { return sizeZ; }

    public List<BlockState> getPalette() { return palette; }
    public short getPalleteSize() { return palleteSize; }
    public short[][][] getStructure() { return structure; }

    @Override
    public int getMinBuildHeight() { return 0; }

    @Override
    public int getMaxBuildHeight() { return sizeY; }

    @Override
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

    @Override
    @javax.annotation.Nullable
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
}
