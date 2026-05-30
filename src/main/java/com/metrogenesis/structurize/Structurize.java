package com.metrogenesis.structurize;

import com.metrogenesis.structurize.blueprints.v1.DataFixerUtils;
import com.metrogenesis.structurize.blueprints.v1.DataVersion;
import com.metrogenesis.structurize.api.util.Log;
import com.metrogenesis.structurize.api.util.constant.Constants;
import com.metrogenesis.structurize.blocks.ModBlocks;
import com.metrogenesis.structurize.config.Configuration;
import com.metrogenesis.structurize.event.ClientEventSubscriber;
import com.metrogenesis.structurize.event.ClientLifecycleSubscriber;
import com.metrogenesis.structurize.event.EventSubscriber;
import com.metrogenesis.structurize.event.LifecycleSubscriber;
import com.metrogenesis.structurize.items.ModItemGroups;
import com.metrogenesis.structurize.items.ModItems;
import com.metrogenesis.structurize.proxy.ClientProxy;
import com.metrogenesis.structurize.proxy.IProxy;
import com.metrogenesis.structurize.proxy.ServerProxy;
import com.metrogenesis.structurize.blockentities.ModBlockEntities;
import com.metrogenesis.structurize.storage.ClientFutureProcessor;
import com.metrogenesis.structurize.storage.ServerFutureProcessor;
import com.metrogenesis.structurize.storage.ClientStructurePackLoader;
import com.metrogenesis.structurize.storage.ServerStructurePackLoader;
import com.metrogenesis.structurize.storage.rendering.ServerPreviewDistributor;
import net.minecraft.util.datafix.DataFixers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

/**
 * Mod main class.
 * The value in annotation should match an entry in the META-INF/mods.toml file.
 */
@Mod(Constants.MOD_ID)
public class Structurize
{
    /**
     * The proxy.
     */
    public static final IProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);

    /**
     * The config instance.
     */
    private static Configuration config;

    /**
     * Mod init, registers events to their respective busses
     */
    public Structurize()
    {
        config = new Configuration(ModLoadingContext.get().getActiveContainer());

        ModBlocks.getRegistry().register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.getRegistry().register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlockEntities.getRegistry().register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItemGroups.TAB_REG.register(FMLJavaModLoadingContext.get().getModEventBus());

        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(LifecycleSubscriber.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventSubscriber.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientStructurePackLoader.onClientLoading();
            Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientStructurePackLoader.class);
            Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientFutureProcessor.class);
            Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ClientLifecycleSubscriber.class);
            Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientEventSubscriber.class);
        });

        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER,  () -> ServerStructurePackLoader::onServerStarting);

        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ServerStructurePackLoader.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ServerPreviewDistributor.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ServerFutureProcessor.class);


        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(this.getClass());
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModItemGroups.class);

        if (DataFixerUtils.isVanillaDF)
        {
            if ((DataFixers.getDataFixer().getSchema(Integer.MAX_VALUE - 1).getVersionKey()) >= DataVersion.UPCOMING.getDataVersion() * 10)
            {
                throw new RuntimeException("You are trying to run old mod on much newer vanilla. Missing some newest data versions. Please update com/ldtteam/structures/blueprints/v1/DataVersion");
            }
            else if (!FMLEnvironment.production && DataVersion.CURRENT == DataVersion.UPCOMING)
            {
                throw new RuntimeException("Missing some newest data versions. Please update com/ldtteam/structures/blueprints/v1/DataVersion");
            }
        }
        else
        {
            Log.getLogger().error("----------------------------------------------------------------- \n "
                                    + "Invalid DataFixer detected, schematics might not paste correctly! \n"
                                    +  "The following DataFixer was added: " + DataFixers.getDataFixer().getClass() + "\n"
                                    + "-----------------------------------------------------------------");
        }
    }

    /**
     * Event handler for forge pre init event.
     *
     * @param event the forge pre init event.
     */
    @SubscribeEvent
    public static void preInit(@NotNull final FMLCommonSetupEvent event)
    {
        Network.getNetwork().registerCommonMessages();
    }


    /**
     * Get the config handler.
     *
     * @return the config handler.
     */
    public static Configuration getConfig()
    {
        return config;
    }
}
