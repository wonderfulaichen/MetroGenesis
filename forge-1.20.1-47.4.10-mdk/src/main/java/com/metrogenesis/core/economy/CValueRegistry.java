package com.metrogenesis.core.economy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry mapping items to their base C-Value.
 * Base values represent the intrinsic worth of an item before supply/demand adjustments.
 * Default values are provided for vanilla items; modded items can be registered via API.
 */
public final class CValueRegistry
{
    private static final Map<ResourceLocation, Long> BASE_VALUES = new HashMap<>();

    private CValueRegistry() {}

    // --- Registration ---

    public static void register(ResourceLocation itemId, long baseValue)
    {
        if (baseValue < 0)
            throw new IllegalArgumentException("Base C-Value cannot be negative: " + itemId);
        BASE_VALUES.put(itemId, baseValue);
    }

    public static void register(Item item, long baseValue)
    {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) register(id, baseValue);
    }

    // --- Query ---

    public static Optional<Long> getBaseValue(ResourceLocation itemId)
    {
        return Optional.ofNullable(BASE_VALUES.get(itemId));
    }

    public static long getBaseValueOrDefault(ResourceLocation itemId, long defaultValue)
    {
        return BASE_VALUES.getOrDefault(itemId, defaultValue);
    }

    /** 默认 0 — 未注册价值的物品不显示 C-Value。 */
    public static long getBaseValueOrZero(ResourceLocation itemId) {
        return BASE_VALUES.getOrDefault(itemId, 0L);
    }

    public static Collection<Map.Entry<ResourceLocation, Long>> allEntries()
    {
        return BASE_VALUES.entrySet();
    }

    /** 当前已注册的物品数量（含基准值 + 配方推升值）。 */
    public static int size() {
        return BASE_VALUES.size();
    }

    // --- Defaults ---

    public static void registerDefaults()
    {
        // ════════════════════════════════════════════════════════
        //  ⚒ 矿物与宝石
        // ════════════════════════════════════════════════════════
        // 锭/宝石
        reg("iron_ingot", 100); reg("gold_ingot", 250); reg("copper_ingot", 45);
        reg("netherite_ingot", 2000); reg("netherite_scrap", 400);
        reg("diamond", 800); reg("emerald", 400); reg("coal", 50);
        reg("redstone", 20); reg("lapis_lazuli", 25); reg("quartz", 15);
        reg("amethyst_shard", 10);
        // 原矿
        reg("raw_iron", 90); reg("raw_gold", 220); reg("raw_copper", 40);
        reg("raw_iron_block", 810); reg("raw_gold_block", 1980); reg("raw_copper_block", 360);
        // 矿物块
        reg("iron_block", 900); reg("gold_block", 2250); reg("diamond_block", 7200);
        reg("coal_block", 450); reg("emerald_block", 3600); reg("redstone_block", 180);
        reg("lapis_block", 225); reg("netherite_block", 18000); reg("copper_block", 405);
        // 下界
        reg("netherite_scrap", 400); reg("ancient_debris", 400);
        reg("glowstone_dust", 10); reg("glowstone", 40);
        reg("nether_wart", 8); reg("netherrack", 2);
        reg("soul_sand", 4); reg("soul_soil", 4);

        // ════════════════════════════════════════════════════════
        //  🪵 原木 / 木材
        // ════════════════════════════════════════════════════════
        for (String wood : new String[]{"oak","spruce","birch","jungle","acacia",
                "dark_oak","mangrove","cherry","crimson","warped"}) {
            reg(wood + "_log", 20); reg(wood + "_planks", 5);
        }

        // ════════════════════════════════════════════════════════
        //  🪨 石头 / 矿物
        // ════════════════════════════════════════════════════════
        reg("stone", 30); reg("cobblestone", 20); reg("deepslate", 25);
        reg("cobbled_deepslate", 20); reg("granite", 20); reg("diorite", 20);
        reg("andesite", 20); reg("tuff", 15); reg("calcite", 15);
        reg("dripstone_block", 15); reg("smooth_basalt", 15);
        reg("sandstone", 15); reg("red_sandstone", 15);
        reg("end_stone", 10); reg("obsidian", 50); reg("crying_obsidian", 100);
        reg("blackstone", 10); reg("basalt", 10);

        // ════════════════════════════════════════════════════════
        //  🏜 自然方块（无配方的生成物）
        // ════════════════════════════════════════════════════════
        reg("dirt", 4); reg("grass_block", 5); reg("podzol", 5);
        reg("mycelium", 6); reg("coarse_dirt", 4); reg("rooted_dirt", 4);
        reg("sand", 3); reg("red_sand", 3); reg("gravel", 4);
        reg("clay", 6); reg("mud", 3); reg("muddy_mangrove_roots", 5);
        reg("snow_block", 4); reg("ice", 4); reg("packed_ice", 8);
        reg("blue_ice", 24); reg("magma_block", 8);

        // ════════════════════════════════════════════════════════
        //  🐔 掉落物（无配方的生物产物）
        // ════════════════════════════════════════════════════════
        reg("rotten_flesh", 3); reg("bone", 8); reg("string", 8);
        reg("feather", 5); reg("leather", 15); reg("rabbit_hide", 4);
        reg("spider_eye", 5); reg("gunpowder", 15);
        reg("slime_ball", 10); reg("ender_pearl", 60);
        reg("blaze_rod", 80); reg("blaze_powder", 20);
        reg("ghast_tear", 40); reg("magma_cream", 20);
        reg("ink_sac", 5); reg("glow_ink_sac", 8);
        reg("prismarine_shard", 5); reg("prismarine_crystals", 8);
        reg("nautilus_shell", 15); reg("heart_of_the_sea", 60);
        reg("scute", 10); reg("armadillo_scute", 8);
        reg("turtle_helmet", 30); reg("honeycomb", 12);
        reg("shulker_shell", 60); reg("phantom_membrane", 15);
        reg("experience_bottle", 25);
        // 1.20 新掉落物
        reg("breeze_rod", 40); reg("heavy_core", 60);
        reg("armadillo_scute", 8); reg("wolf_armor", 40);

        // ════════════════════════════════════════════════════════
        //  🌾 农作物 / 食物
        // ════════════════════════════════════════════════════════
        reg("wheat", 30); reg("wheat_seeds", 15); reg("bread", 50);
        reg("apple", 40); reg("carrot", 25); reg("potato", 20);
        reg("baked_potato", 30); reg("poisonous_potato", 5);
        reg("beetroot", 15); reg("beetroot_seeds", 8);
        reg("pumpkin", 30); reg("melon_slice", 8); reg("sweet_berries", 10);
        reg("glow_berries", 12); reg("cocoa_beans", 15);
        reg("sugar_cane", 10); reg("kelp", 5); reg("dried_kelp", 8);
        reg("cactus", 8); reg("bamboo", 4);
        reg("beef", 30); reg("cooked_beef", 80);
        reg("porkchop", 25); reg("cooked_porkchop", 70);
        reg("chicken", 20); reg("cooked_chicken", 55);
        reg("cod", 15); reg("cooked_cod", 35);
        reg("salmon", 18); reg("cooked_salmon", 40);
        reg("tropical_fish", 10); reg("pufferfish", 8);
        reg("mutton", 20); reg("cooked_mutton", 50);
        reg("rabbit", 20); reg("cooked_rabbit", 45);
        reg("rabbit_stew", 80); reg("mushroom_stew", 30);
        reg("beetroot_soup", 35); reg("suspicious_stew", 25);
        reg("cookie", 15); reg("cake", 200); reg("pumpkin_pie", 80);
        reg("golden_apple", 600); reg("enchanted_golden_apple", 3000);
        reg("golden_carrot", 150); reg("milk_bucket", 100);
        reg("egg", 12); reg("sugar", 10); reg("honey_bottle", 30);
        reg("chorus_fruit", 20); reg("popped_chorus_fruit", 25);

        // ════════════════════════════════════════════════════════
        //  🏭 常用合成品
        // ════════════════════════════════════════════════════════
        reg("stick", 4); reg("crafting_table", 80); reg("furnace", 150);
        reg("chest", 150); reg("barrel", 120);
        reg("enchanting_table", 1200); reg("anvil", 3000);
        reg("beacon", 10000); reg("bookshelf", 300);
        reg("lectern", 200); reg("grindstone", 100);
        reg("stonecutter", 80); reg("cartography_table", 80);
        reg("fletching_table", 80); reg("smithing_table", 200);
        reg("loom", 60); reg("composter", 60);
        reg("brewing_stand", 150); reg("cauldron", 100);
        reg("hopper", 200); reg("piston", 100); reg("sticky_piston", 120);
        reg("observer", 100); reg("dispenser", 150); reg("dropper", 100);
        reg("note_block", 80); reg("jukebox", 400);
        reg("daylight_detector", 100);
        // 铁制品
        reg("iron_pickaxe", 300); reg("iron_axe", 300);
        reg("iron_shovel", 200); reg("iron_sword", 250);
        reg("iron_hoe", 150); reg("iron_helmet", 500);
        reg("iron_chestplate", 800); reg("iron_leggings", 700);
        reg("iron_boots", 400); reg("iron_door", 200);
        reg("iron_trapdoor", 200); reg("iron_bars", 50);
        reg("bucket", 150); reg("shears", 100); reg("shield", 200);
        reg("compass", 100); reg("clock", 200);
        // 石制品
        reg("stone_pickaxe", 50); reg("stone_axe", 50);
        reg("stone_shovel", 30); reg("stone_sword", 40);
        reg("stone_hoe", 30);
        // 木制品
        reg("wooden_pickaxe", 25); reg("wooden_axe", 25);
        reg("wooden_shovel", 15); reg("wooden_sword", 20);
        reg("wooden_hoe", 15);
        // 金制品
        reg("golden_pickaxe", 500); reg("golden_axe", 500);
        reg("golden_shovel", 300); reg("golden_sword", 400);
        reg("golden_helmet", 800); reg("golden_chestplate", 1200);
        reg("golden_leggings", 1000); reg("golden_boots", 600);
        // 钻石制品
        reg("diamond_pickaxe", 2000); reg("diamond_axe", 2000);
        reg("diamond_shovel", 1000); reg("diamond_sword", 1600);
        reg("diamond_helmet", 3200); reg("diamond_chestplate", 4800);
        reg("diamond_leggings", 4000); reg("diamond_boots", 2400);

        // ════════════════════════════════════════════════════════
        //  📖 其他
        // ════════════════════════════════════════════════════════
        reg("book", 120); reg("writable_book", 130); reg("written_book", 140);
        reg("painting", 180); reg("item_frame", 40);
        reg("flower_pot", 40); reg("map", 160);
        reg("ender_chest", 600); reg("shulker_box", 500);
        reg("lava_bucket", 500); reg("water_bucket", 20);
        reg("flint_and_steel", 60); reg("fishing_rod", 40);
        reg("carrot_on_a_stick", 200); reg("warped_fungus_on_a_stick", 160);
        reg("lead", 30); reg("saddle", 200);
        reg("name_tag", 50);
    }

    /** 简写：注册 minecraft:name */
    private static void reg(String name, long value) {
        register(new ResourceLocation("minecraft", name), value);
    }
}
