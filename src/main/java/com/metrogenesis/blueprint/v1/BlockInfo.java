package com.metrogenesis.blueprint.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Contains block information we need for placement.
 * Adapted from Structurize.
 */
public record BlockInfo(BlockPos pos, @Nullable BlockState state, @Nullable CompoundTag tileEntityData)
{
    public boolean hasTileEntityData()
    {
        return tileEntityData != null;
    }
}
