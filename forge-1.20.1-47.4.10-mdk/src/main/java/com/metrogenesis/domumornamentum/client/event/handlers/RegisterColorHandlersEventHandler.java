package com.metrogenesis.domumornamentum.client.event.handlers;

import com.metrogenesis.domumornamentum.block.ModBlocks;
import com.metrogenesis.domumornamentum.client.color.MateriallyTexturedBlockBlockColor;
import com.metrogenesis.domumornamentum.client.color.MateriallyTexturedBlockItemColor;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterColorHandlersEventHandler {

    @SubscribeEvent
    public static void onRegisterColorHandlersItem(RegisterColorHandlersEvent.Item event) {
        event.register(
                new MateriallyTexturedBlockItemColor(),
                ModBlocks.getMateriallyTexturableItems()
        );
    }

    @SubscribeEvent
    public static void onRegisterColorHandlersBlock(RegisterColorHandlersEvent.Block event) {
        event.register(
                new MateriallyTexturedBlockBlockColor(),
                ModBlocks.getMateriallyTexturableBlocks()
        );
    }
}
