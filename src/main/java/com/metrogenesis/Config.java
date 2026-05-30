package com.metrogenesis;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue CITY_STARTING_FUNDS = BUILDER
            .comment("Starting funds for the city treasury (in C-Value)")
            .defineInRange("cityStartingFunds", 1000, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int cityStartingFunds;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        cityStartingFunds = CITY_STARTING_FUNDS.get();
    }
}
