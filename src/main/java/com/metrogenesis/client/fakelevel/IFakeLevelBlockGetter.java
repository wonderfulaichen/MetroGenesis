package com.metrogenesis.client.fakelevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Data source interface for fake world rendering.
 * Adapted from Structurize.
 */
public interface IFakeLevelBlockGetter extends BlockGetter
{
    short getSizeX();
    short getSizeZ();
    int getSizeY();

    default int getMinX() { return 0; }
    @Override default int getMinBuildHeight() { return 0; }
    default int getMinZ() { return 0; }

    default int getMaxX() { return getMinX() + getSizeX(); }
    default int getMaxZ() { return getMinZ() + getSizeZ(); }
    default int getMaxBuildHeight() { return getSizeY(); }

    @Override
    default int getHeight()
    {
        return getSizeY();
    }

    default boolean isPosInside(final BlockPos pos)
    {
        return getMinX() <= pos.getX() && pos.getX() < getMaxX() &&
            getMinBuildHeight() <= pos.getY() && pos.getY() < getMaxBuildHeight() &&
            getMinZ() <= pos.getZ() && pos.getZ() < getMaxZ();
    }

    @Override
    default FluidState getFluidState(final BlockPos pos)
    {
        return isPosInside(pos) ? getBlockState(pos).getFluidState() : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    @Nullable
    default BlockState getBlockState(final BlockPos pos)
    {
        return isPosInside(pos) ? getBlockStateDirect(pos) : null;
    }

    /**
     * Get block state directly (without bounds check)
     */
    BlockState getBlockStateDirect(BlockPos pos);
}
