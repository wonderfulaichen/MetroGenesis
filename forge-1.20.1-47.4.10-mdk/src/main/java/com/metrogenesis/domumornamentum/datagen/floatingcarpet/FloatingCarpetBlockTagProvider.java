package com.metrogenesis.domumornamentum.datagen.floatingcarpet;

import com.metrogenesis.domumornamentum.block.ModBlocks;
import com.metrogenesis.domumornamentum.tag.ModTags;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FloatingCarpetBlockTagProvider extends BlockTagsProvider
{
    public FloatingCarpetBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Constants.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (final Block block : ModBlocks.getInstance().getFloatingCarpets())
        {
            this.tag(ModTags.FLOATING_CARPETS).add(block);
        }
    }

    @Override
    @NotNull
    public String getName()
    {
        return "Floating Carpets Tag Provider";
    }
}
