package com.metrogenesis.domumornamentum;

import com.metrogenesis.domumornamentum.api.DomumOrnamentumAPI;
import com.metrogenesis.domumornamentum.block.ModBlocks;
import com.metrogenesis.domumornamentum.block.ModCreativeTabs;
import com.metrogenesis.domumornamentum.container.ModContainerTypes;
import com.metrogenesis.domumornamentum.entity.block.ModBlockEntityTypes;
import com.metrogenesis.domumornamentum.recipe.ModRecipeSerializers;
import com.metrogenesis.domumornamentum.recipe.ModRecipeTypes;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Constants.MOD_ID)
public class DomumOrnamentum
{
    public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

    public DomumOrnamentum()
    {
        IDomumOrnamentumApi.Holder.setInstance(DomumOrnamentumAPI.getInstance());
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlocks.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlockEntityTypes.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModContainerTypes.CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeTypes.RECIPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeSerializers.SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModCreativeTabs.TAB_REG.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
