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

    public static Collection<Map.Entry<ResourceLocation, Long>> allEntries()
    {
        return BASE_VALUES.entrySet();
    }

    // --- Defaults ---

    public static void registerDefaults()
    {
        // Building materials
        register(new ResourceLocation("minecraft:oak_log"), 2);
        register(new ResourceLocation("minecraft:spruce_log"), 2);
        register(new ResourceLocation("minecraft:birch_log"), 2);
        register(new ResourceLocation("minecraft:stone"), 3);
        register(new ResourceLocation("minecraft:cobblestone"), 2);
        register(new ResourceLocation("minecraft:iron_ingot"), 10);
        register(new ResourceLocation("minecraft:gold_ingot"), 25);
        register(new ResourceLocation("minecraft:diamond"), 80);
        register(new ResourceLocation("minecraft:coal"), 5);

        // Food
        register(new ResourceLocation("minecraft:wheat"), 3);
        register(new ResourceLocation("minecraft:bread"), 5);
        register(new ResourceLocation("minecraft:apple"), 4);
        register(new ResourceLocation("minecraft:cooked_beef"), 8);
        register(new ResourceLocation("minecraft:cooked_porkchop"), 7);

        // Manufactured
        register(new ResourceLocation("minecraft:iron_pickaxe"), 30);
        register(new ResourceLocation("minecraft:iron_axe"), 30);
        register(new ResourceLocation("minecraft:iron_shovel"), 20);
        register(new ResourceLocation("minecraft:chest"), 15);

        // Luxury
        register(new ResourceLocation("minecraft:emerald"), 40);
        register(new ResourceLocation("minecraft:book"), 12);
        register(new ResourceLocation("minecraft:painting"), 18);
    }
}
