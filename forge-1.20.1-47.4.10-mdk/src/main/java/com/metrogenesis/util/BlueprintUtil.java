package com.metrogenesis.util;

import com.metrogenesis.blueprint.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 钃濆浘宸ュ叿绫?鈥?鎵弿涓栫晫鍖哄煙鐢熸垚 Blueprint锛屾敮鎸?NBT 搴忓垪鍖? */
public class BlueprintUtil
{
    /**
     * 鎵弿涓栫晫涓殑涓€涓尯鍩燂紝鐢熸垚 Blueprint
     */
    public static Blueprint scanRegion(Level level, BlockPos from, BlockPos to, String name)
    {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        short sizeX = (short) (maxX - minX + 1);
        short sizeY = (short) (maxY - minY + 1);
        short sizeZ = (short) (maxZ - minZ + 1);

        Blueprint bp = new Blueprint(sizeX, sizeY, sizeZ);

        for (int y = 0; y < sizeY; y++)
        {
            for (int z = 0; z < sizeZ; z++)
            {
                for (int x = 0; x < sizeX; x++)
                {
                    BlockPos worldPos = new BlockPos(minX + x, minY + y, minZ + z);
                    BlockState state = level.getBlockState(worldPos);

                    if (state.isAir() || state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    bp.addBlockState(new BlockPos(x, y, z), state);

                    BlockEntity be = level.getBlockEntity(worldPos);
                    if (be != null)
                    {
                        CompoundTag teTag = be.saveWithoutMetadata();
                        teTag.putInt("x", x);
                        teTag.putInt("y", y);
                        teTag.putInt("z", z);
                        bp.setTileEntity(new BlockPos(x, y, z), teTag);
                    }
                }
            }
        }

        bp.setName(name);
        return bp;
    }

    /**
     * 灏?Blueprint 搴忓垪鍖栦负 NBT
     */
    public static CompoundTag writeBlueprintToNBT(Blueprint bp)
    {
        CompoundTag tag = new CompoundTag();
        tag.putShort("sizeX", bp.getSizeX());
        tag.putShort("sizeY", (short) bp.getSizeY());
        tag.putShort("sizeZ", bp.getSizeZ());

        // Palette
        ListTag paletteTag = new ListTag();
        for (BlockState state : bp.getPalette())
        {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("palette", paletteTag);

        // Structure data
        short[][][] structure = bp.getStructure();
        int total = bp.getSizeX() * bp.getSizeY() * bp.getSizeZ();
        int[] blocks = new int[(total + 1) / 2];
        int idx = 0;
        for (int y = 0; y < bp.getSizeY(); y++)
        {
            for (int z = 0; z < bp.getSizeZ(); z++)
            {
                for (int x = 0; x < bp.getSizeX(); x++)
                {
                    short val = structure[y][z][x];
                    if (idx % 2 == 0)
                    {
                        blocks[idx / 2] = (val & 0xFFFF) << 16;
                    }
                    else
                    {
                        blocks[idx / 2] |= (val & 0xFFFF);
                    }
                    idx++;
                }
            }
        }
        tag.putIntArray("blocks", blocks);

        if (bp.getName() != null && !bp.getName().isEmpty())
        {
            tag.putString("name", bp.getName());
        }

        return tag;
    }

    /**
     * 浠?NBT 鍙嶅簭鍒楀寲 Blueprint
     */
    public static Blueprint readBlueprintFromNBT(CompoundTag tag)
    {
        short sizeX = tag.getShort("sizeX");
        short sizeY = tag.getShort("sizeY");
        short sizeZ = tag.getShort("sizeZ");

        Blueprint bp = new Blueprint(sizeX, sizeY, sizeZ);
        bp.getPalette().clear();

        // Palette
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        var lookup = BuiltInRegistries.BLOCK.asLookup();
        for (int i = 0; i < paletteTag.size(); i++)
        {
            bp.getPalette().add(NbtUtils.readBlockState(lookup, paletteTag.getCompound(i)));
        }

        // Structure
        int[] blocks = tag.getIntArray("blocks");
        short[][][] structure = bp.getStructure();
        int total = sizeX * sizeY * sizeZ;
        for (int i = 0; i < total; i++)
        {
            int val;
            if (i % 2 == 0)
            {
                val = (blocks[i / 2] >> 16) & 0xFFFF;
            }
            else
            {
                val = blocks[i / 2] & 0xFFFF;
            }
            int x = i % sizeX;
            int z = (i / sizeX) % sizeZ;
            int y = i / (sizeX * sizeZ);
            if (y < sizeY && z < sizeZ && x < sizeX)
            {
                structure[y][z][x] = (short) val;
            }
        }

        if (tag.contains("name"))
        {
            bp.setName(tag.getString("name"));
        }

        return bp;
    }
}
