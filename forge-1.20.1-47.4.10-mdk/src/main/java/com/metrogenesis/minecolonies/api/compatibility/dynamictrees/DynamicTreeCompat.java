package com.metrogenesis.minecolonies.api.compatibility.dynamictrees;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stub: DynamicTrees mod not available at compile time.
 * All methods return safe defaults.
 */
public final class DynamicTreeCompat extends DynamicTreeProxy
{
    public DynamicTreeCompat() { }

    @Override public boolean isDynamicTreePresent() { return false; }
    @Override public boolean checkForDynamicTreeBlock(Block block) { return false; }
    @Override public boolean checkForDynamicLeavesBlock(Block block) { return false; }
    @Override public boolean checkForDynamicTrunkShellBlock(Block block) { return false; }
    @Override public NonNullList<ItemStack> getDropsForLeaf(LevelAccessor world, BlockPos pos, BlockState blockstate, int fortune, Block leaf) { return NonNullList.create(); }
    @Override public boolean checkForDynamicSapling(net.minecraft.world.item.Item item) { return false; }
    @Override public Runnable getTreeBreakActionCompat(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos blockToBreak, net.minecraft.world.item.ItemStack toolToUse, net.minecraft.core.BlockPos workerPos) { return () -> { }; }
    @Override public boolean plantDynamicSaplingCompat(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos location, net.minecraft.world.item.ItemStack saplingStack) { return false; }
    @Override public net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> getDynamicTreeDamage() { return null; }
    @Override public boolean hasFittingTreeFamilyCompat(net.minecraft.core.BlockPos block1, net.minecraft.core.BlockPos block2, net.minecraft.world.level.LevelAccessor world) { return false; }
}
