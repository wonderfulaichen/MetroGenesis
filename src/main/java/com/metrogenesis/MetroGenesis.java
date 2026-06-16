package com.metrogenesis;

import com.mojang.logging.LogUtils;
import com.metrogenesis.command.CValueCommand;
import com.metrogenesis.entity.RtsCameraEntity;
import com.metrogenesis.gui.FeaturesOverviewScreen;
import com.metrogenesis.gui.MayorBookScreen;
import com.metrogenesis.block.construction.ConstructionMarkerBlock;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.gui.ConstructionMarkerMenu;
import com.metrogenesis.item.BlueprintEyeItem;
import com.metrogenesis.item.BuildingToolItem;
import com.metrogenesis.item.MayorBookItem;
import com.metrogenesis.network.NetworkHandler;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import com.metrogenesis.road.RoadConstructionTask;
import com.metrogenesis.road.handlers.RoadPipelineController;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(MetroGenesis.MODID)
public class MetroGenesis {

    public static final String MODID = "metrogenesis";
    public static final Logger LOGGER = LogUtils.getLogger();

    // ══ Construction task queue (tick-by-tick visual building) ═══
    private static final List<RoadConstructionTask> ACTIVE_TASKS = new ArrayList<>();

    /** Enqueue a road construction task for tick-by-tick execution. */
    public static void enqueueConstruction(RoadConstructionTask task) {
        ACTIVE_TASKS.add(task);
        LOGGER.info("Construction task queued: {} -> {} ({} steps)",
                task.getFrom(), task.getTo(), task.getTotalSteps());
    }

    // ══ Items ═══════════════════════════════════════════

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> MAYOR_BOOK =
            ITEMS.register("mayor_book", MayorBookItem::new);

    public static final RegistryObject<Item> BLUEPRINT_EYE =
            ITEMS.register("blueprint_eye", BlueprintEyeItem::new);

    public static final RegistryObject<Item> BUILDING_TOOL =
            ITEMS.register("building_tool", BuildingToolItem::new);

    // ══ Entities ════════════════════════════════════════

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<RtsCameraEntity>> RTS_CAMERA =
            ENTITIES.register("rts_camera", () -> EntityType.Builder
                    .<RtsCameraEntity>of(RtsCameraEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .noSave()
                    .noSummon()
                    .build(MODID + ":rts_camera"));

    public static final RegistryObject<EntityType<com.metrogenesis.entity.MetroGenesisCitizen>> CITIZEN_ENTITY =
            ENTITIES.register("citizen", () -> EntityType.Builder
                    .<com.metrogenesis.entity.MetroGenesisCitizen>of(com.metrogenesis.entity.MetroGenesisCitizen::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build(MODID + ":citizen"));

    // ══ Blocks ════════════════════════════════════════

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final RegistryObject<Block> CONSTRUCTION_MARKER_BLOCK =
            BLOCKS.register("construction_marker", ConstructionMarkerBlock::new);

    public static final RegistryObject<Block> TOWN_HALL_BLOCK =
            BLOCKS.register("town_hall", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F)));

    public static final RegistryObject<Block> FARM_FACILITY_BLOCK =
            BLOCKS.register("farm_facility", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(2.0F)));

    // ══ Block Entities ═══════════════════════════════

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<ConstructionMarkerBlockEntity>> CONSTRUCTION_MARKER_BE =
            BLOCK_ENTITIES.register("construction_marker_be",
                    () -> BlockEntityType.Builder.of(ConstructionMarkerBlockEntity::new, CONSTRUCTION_MARKER_BLOCK.get())
                            .build(null));

    // ══ Menus ════════════════════════════════════════

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<MenuType<ConstructionMarkerMenu>> CONSTRUCTION_MARKER_MENU =
            MENUS.register("construction_marker_menu",
                    () -> IForgeMenuType.create(ConstructionMarkerMenu::new));

    // ══ Scan Tool — 已移除（与 blueprint_eye 功能重叠） ═══
    // public static final RegistryObject<Item> SCAN_TOOL =
    //         ITEMS.register("scan_tool", ScanToolItem::new);

    // ══ Only creative tab register — no custom items ═══

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ══ Creative Tab — 仅展示 MetroGenesis 自行注册的物品 ═════

    public static final RegistryObject<CreativeModeTab> TAB_METROGENESIS =
            CREATIVE_MODE_TABS.register("metrogenesis", () -> CreativeModeTab.builder()
                    .icon(() -> {
                        Item icon = ForgeRegistries.ITEMS.getValue(
                            net.minecraft.resources.ResourceLocation.parse("minecolonies:blockhuttownhall"));
                        return icon != null ? new ItemStack(icon) : ItemStack.EMPTY;
                    })
                    .title(Component.translatable("itemGroup.metrogenesis"))
                    .displayItems((params, output) -> {
                        // 仅添加 metrogenesis 命名空间的物品（目前无自研物品，后续添加）
                        for (Item item : ForgeRegistries.ITEMS.getValues()) {
                            if (ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("metrogenesis")) {
                                output.accept(item);
                            }
                        }
                    })
                    .build());

    // ══ Constructor ════════════════════════════════════

    public MetroGenesis(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);

        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("metrogenesis loading...");
        NetworkHandler.init();
        RoadPipelineController.configure(
                Config.pipelineIntervalSeconds,
                Config.maxConnectionDistance,
                Config.structureSelectors,
                List.of() // dimension selectors — default to overworld only
        );
    }

    /** 为模组注册的自定义实体注册 AttributeSupplier */
    public void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CITIZEN_ENTITY.get(), com.metrogenesis.entity.MetroGenesisCitizen.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("metrogenesis server starting");
        RoadPipelineController.init();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("metrogenesis server started — road pipeline ready");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RoadPipelineController.onServerStopping();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CValueCommand.register(event.getDispatcher());
        LOGGER.info("metrogenesis → /cvalue commands registered");
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel world) {
            ChunkAccess chunk = event.getChunk();
            RoadPipelineController.onSpawnChunkGenerated(world, chunk);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            RoadPipelineController.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RoadPipelineController.onServerTick(
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());

            // Tick structurize undo/redo operation queue for all loaded levels
            final net.minecraft.server.MinecraftServer server =
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (final ServerLevel world : server.getAllLevels()) {
                    com.metrogenesis.structurize.management.Manager.onWorldTick(world);
                }
            }

            // Tick construction tasks
            Iterator<RoadConstructionTask> it = ACTIVE_TASKS.iterator();
            while (it.hasNext()) {
                RoadConstructionTask task = it.next();
                boolean done = task.tick();
                if (done) {
                    it.remove();
                    LOGGER.info("Construction complete: {} -> {} ({} blocks)",
                            task.getFrom(), task.getTo(), task.getTotalBlocksPlaced());
                }
            }
        }
    }

    // ══ Client Key Events ════════════════════════════

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(RTS_CAMERA.get(), com.metrogenesis.client.RtsCameraEntityRenderer::new);
            LOGGER.info("metrogenesis → RTS camera renderer registered");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientKeyEvents {
        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();

            // O 键 → 打开功能总览
            if (event.getKey() == 79 && mc.screen == null) {
                mc.setScreen(new FeaturesOverviewScreen());
            }

            // Ctrl+Z / Ctrl+Y → 建筑撤销/重做（仅无屏幕时，避免与 MayorBookScreen 道路撤销冲突）
            if (mc.screen == null && net.minecraft.client.gui.screens.Screen.hasControlDown()) {
                if (event.getKey() == 90) { // Z
                    com.metrogenesis.structurize.Network.getNetwork()
                        .sendToServer(new com.metrogenesis.structurize.network.messages.UndoRedoMessage(-1, true));
                    LOGGER.info("Building undo triggered via Ctrl+Z");
                }
                if (event.getKey() == 89) { // Y
                    com.metrogenesis.structurize.Network.getNetwork()
                        .sendToServer(new com.metrogenesis.structurize.network.messages.UndoRedoMessage(-1, false));
                    LOGGER.info("Building redo triggered via Ctrl+Y");
                }
            }
        }
    }
}
