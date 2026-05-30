package com.metrogenesis.domumornamentum.datagen.global;

import com.google.gson.JsonObject;
import com.metrogenesis.domumornamentum.block.IMateriallyTexturedBlock;
import com.metrogenesis.domumornamentum.recipe.ModRecipeSerializers;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class MateriallyTexturedBlockRecipeProvider extends RecipeProvider
{

    public MateriallyTexturedBlockRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> finishedRecipe) {
        ForgeRegistries.BLOCKS.forEach(
                block -> {
                    if (Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getNamespace().equals(Constants.MOD_ID) && block instanceof IMateriallyTexturedBlock materiallyTexturedBlock) {
                        materiallyTexturedBlock.getValidCutterRecipes().forEach(finishedRecipe);
                    }
                }
        );
    }

}
