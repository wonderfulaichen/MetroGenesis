package com.metrogenesis.domumornamentum.datagen.door;

import com.metrogenesis.domumornamentum.tag.ModTags;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DoorsComponentTagProvider extends BlockTagsProvider
{


    public DoorsComponentTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Constants.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(ModTags.DOORS_MATERIALS)
                .addTags(
                        ModTags.GLOBAL_DEFAULT,
                        BlockTags.PLANKS
                );
    }

    @Override
    @NotNull
    public String getName()
    {
        return "Doors Tag Provider";
    }
}
