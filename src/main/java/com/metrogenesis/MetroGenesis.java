package com.metrogenesis;

import com.mojang.logging.LogUtils;
import com.metrogenesis.block.FarmFacilityBlock;
import com.metrogenesis.block.TownHallBlock;
import com.metrogenesis.block.construction.BlockConstructionTape;
import com.metrogenesis.block.construction.ConstructionMarkerBlock;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.entity.MetroGenesisCitizen;
import com.metrogenesis.entity.client.CitizenRenderer;
import com.metrogenesis.gui.CitizenInteractionScreen;
import com.metrogenesis.gui.ConstructionMarkerMenu;
import com.metrogenesis.gui.ConstructionMarkerScreen;
import com.metrogenesis.gui.TownHallMenu;
import com.metrogenesis.gui.TownHallScreen;
import com.metrogenesis.colony.citizen.CitizenNameListener;
import com.metrogenesis.construction.ConstructionTapeHelper;
import com.metrogenesis.hologram.MetroGenesisHologramMod;
import com.metrogenesis.item.BuildingToolItem;
import com.metrogenesis.item.ScanToolItem;
import com.metrogenesis.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
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

    // ══ 寤惰繜娉ㄥ唽鍣?══════════════════════════════════════

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // ══ 鏂瑰潡 & 鐗╁搧 ═════════════════════════════════════

    public static final RegistryObject<Block> TOWN_HALL_BLOCK = BLOCKS.register("town_hall",
            TownHallBlock::new);

    public static final RegistryObject<Block> FARM_FACILITY_BLOCK = BLOCKS.register("farm_facility",
            FarmFacilityBlock::new);

    public static final RegistryObject<Block> CONSTRUCTION_MARKER_BLOCK = BLOCKS.register("construction_marker",
            ConstructionMarkerBlock::new);

    public static final RegistryObject<Block> CONSTRUCTION_TAPE_BLOCK = BLOCKS.register("construction_tape",
            BlockConstructionTape::new);

    public static final RegistryObject<Block> HOUSE_BLOCK = BLOCKS.register("house",
            () -> new Block(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> WORKSHOP_BLOCK = BLOCKS.register("workshop",
            () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> WAREHOUSE_BLOCK = BLOCKS.register("warehouse",
            () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0f).sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Item> TOWN_HALL_ITEM = ITEMS.register("town_hall",
            () -> new BlockItem(TOWN_HALL_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> FARM_FACILITY_ITEM = ITEMS.register("farm_facility",
            () -> new BlockItem(FARM_FACILITY_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CONSTRUCTION_MARKER_ITEM = ITEMS.register("construction_marker",
            () -> new BlockItem(CONSTRUCTION_MARKER_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BUILDING_TOOL = ITEMS.register("building_tool",
            BuildingToolItem::new);
    public static final RegistryObject<Item> SCAN_TOOL = ITEMS.register("scan_tool",
            ScanToolItem::new);

    // 鏂板寤虹瓚鐨勭墿鍝?
    public static final RegistryObject<Item> HOUSE_ITEM = ITEMS.register("house",
            () -> new BlockItem(HOUSE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> WORKSHOP_ITEM = ITEMS.register("workshop",
            () -> new BlockItem(WORKSHOP_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> WAREHOUSE_ITEM = ITEMS.register("warehouse",
            () -> new BlockItem(WAREHOUSE_BLOCK.get(), new Item.Properties()));

    // ══ 鑿滃崟绫诲瀷 ════════════════════════════════════════

    public static final RegistryObject<MenuType<TownHallMenu>> TOWN_HALL_MENU =
            MENUS.register("town_hall", () -> IForgeMenuType.create(TownHallMenu::new));
    public static final RegistryObject<MenuType<ConstructionMarkerMenu>> CONSTRUCTION_MARKER_MENU =
            MENUS.register("construction_marker", () -> IForgeMenuType.create(ConstructionMarkerMenu::new));

    // ══ 瀹炰綋绫诲瀷 ════════════════════════════════════════

    public static final RegistryObject<EntityType<MetroGenesisCitizen>> CITIZEN_ENTITY =
            ENTITY_TYPES.register("citizen", () -> EntityType.Builder.of(
                            MetroGenesisCitizen::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("metrogenesis:citizen"));

    // ══ 鏂瑰潡瀹炰綋绫诲瀷 ════════════════════════════════════

    public static final RegistryObject<BlockEntityType<ConstructionMarkerBlockEntity>> CONSTRUCTION_MARKER_BE =
            BLOCK_ENTITIES.register("construction_marker",
                    () -> BlockEntityType.Builder.of(
                            ConstructionMarkerBlockEntity::new,
                            CONSTRUCTION_MARKER_BLOCK.get()).build(null));

    // ══ 鍒涢€犳ā寮忔爣绛鹃〉 ═════════════════════════════════

    public static final RegistryObject<CreativeModeTab> TAB_METROGENESIS =
            CREATIVE_MODE_TABS.register("metrogenesis", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(TOWN_HALL_ITEM.get()))
                    .title(Component.translatable("itemGroup.metrogenesis"))
                    .displayItems((params, output) -> {
                        output.accept(BUILDING_TOOL.get());
                        output.accept(SCAN_TOOL.get());
                        output.accept(CONSTRUCTION_MARKER_ITEM.get());
                        output.accept(TOWN_HALL_ITEM.get());
                        output.accept(FARM_FACILITY_ITEM.get());
                        output.accept(HOUSE_ITEM.get());
                        output.accept(WORKSHOP_ITEM.get());
                        output.accept(WAREHOUSE_ITEM.get());
                    })
                    .build());

    // ══ 鏋勯€?═══════════════════════════════════════════

    public MetroGenesis(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("metrogenesis loading...");
        // 鍒濆鍖栫綉缁滈€氶亾
        NetworkHandler.init();
        // 鍒濆鍖栧叏鎭浘妯″潡
        MetroGenesisHologramMod.init();
        // 鍒濆鍖栨柦宸ュ洿鏍?        event.enqueueWork(() -> ConstructionTapeHelper.setTapeBlock(CONSTRUCTION_TAPE_BLOCK.get()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 这里可以往其他标签页添加物品
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("metrogenesis server starting");
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CitizenNameListener());
        LOGGER.info("metrogenesis → CitizenNameListener registered");
    }

    // ══ 瀹炰綋灞炴€ф敞鍐岋紙Forge 浜嬩欢锛?══════════════════════

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EntityAttributes {

        @SubscribeEvent
        public static void onEntityAttributeCreation(
                net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
            event.put(CITIZEN_ENTITY.get(), MetroGenesisCitizen.createAttributes().build());
        }
    }

    // ══ 瀹㈡埛绔笓灞?══════════════════════════════════════

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(TOWN_HALL_MENU.get(), TownHallScreen::new);
                MenuScreens.register(CONSTRUCTION_MARKER_MENU.get(), ConstructionMarkerScreen::new);
                EntityRenderers.register(CITIZEN_ENTITY.get(), CitizenRenderer::new);
                LOGGER.info("metrogenesis client setup complete → screens + entity renderer registered");
            });
        }
    }

    // ══ 瀹㈡埛绔氦浜掍簨浠?══════════════════════════════

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientInteractionEvents {

        @SubscribeEvent
        public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
            if (event.getTarget() instanceof MetroGenesisCitizen citizen) {
                Minecraft.getInstance().setScreen(new CitizenInteractionScreen(citizen));
                event.setCanceled(true);
            }
        }
    }

    // ══ 瀹㈡埛绔寜閿簨浠?══════════════════════════════

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientKeyEvents {

        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            // 鎸?O 閿墦寮€宸插畬鎴愬唴瀹规€昏锛堜粎褰撴病鏈夊叾浠?GUI 鎵撳紑鏃讹級
            if (event.getKey() == 79 && mc.screen == null) { // 79 = 'O'
                mc.setScreen(new com.metrogenesis.gui.FeaturesOverviewScreen());
            }
        }
    }
}
