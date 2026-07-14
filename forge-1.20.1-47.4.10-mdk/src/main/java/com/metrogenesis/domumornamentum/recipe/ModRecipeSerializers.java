package com.metrogenesis.domumornamentum.recipe;

import com.metrogenesis.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipe;
import com.metrogenesis.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipeSerializer;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializers
{
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Constants.MOD_ID);

    public static RegistryObject<RecipeSerializer<ArchitectsCutterRecipe>> ARCHITECTS_CUTTER  = SERIALIZERS.register("architects_cutter", ArchitectsCutterRecipeSerializer::new);

    private ModRecipeSerializers()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModRecipeSerializers. This is a utility class");
    }
}
