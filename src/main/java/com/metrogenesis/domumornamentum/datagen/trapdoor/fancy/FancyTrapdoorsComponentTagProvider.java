package com.metrogenesis.domumornamentum.datagen.trapdoor.fancy;

import com.metrogenesis.domumornamentum.tag.ModTags;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FancyTrapdoorsComponentTagProvider extends BlockTagsProvider
{
    public FancyTrapdoorsComponentTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Constants.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(ModTags.FANCY_TRAPDOORS_MATERIALS)
          .addTags(
            ModTags.TRAPDOORS_MATERIALS
          );
    }

    @Override
    @NotNull
    public String getName()
    {
        return "FancyTrapdoors Tag Provider";
    }
}
