package com.metrogenesis.core.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks supply and demand data per item for C-Value pricing.
 *
 * Adopts mars-sim's VP formula:
 *   C-Value = DemandScore / (1 + SupplyScore)
 *
 * Where:
 *   SupplyScore = sqrt(0.1 + stock + inTransit)  — square-root compression
 *   DemandScore = tracked as cumulative consumption in this cycle
 *
 * Prices are smoothed via EMA: newPrice = (1 - alpha) * oldPrice + alpha * calculated
 */
public class MarketData
{
    // --- Price cache (EMA-smoothed, persisted) ---
    private final Map<ResourceLocation, Double> priceCache = new HashMap<>();

    // --- Cycle counters (reset after settle) ---
    private final Map<ResourceLocation, ItemMarketSnapshot> cycleData = new HashMap<>();

    // --- Supply tracking (external systems push stock levels here) ---
    private final Map<ResourceLocation, Long> currentStock = new HashMap<>();
    private final Map<ResourceLocation, Long> inTransit = new HashMap<>();

    // ========================================================================
    //  Data recording (called by buildings/citizens each tick)
    // ========================================================================

    /** Record that an item was produced (e.g., farmer harvested wheat). */
    public void recordProduction(ResourceLocation itemId, int amount)
    {
        cycleData.computeIfAbsent(itemId, k -> new ItemMarketSnapshot())
            .production += amount;
    }

    /** Record that an item was consumed (e.g., citizen ate bread). */
    public void recordConsumption(ResourceLocation itemId, int amount)
    {
        cycleData.computeIfAbsent(itemId, k -> new ItemMarketSnapshot())
            .consumption += amount;
    }

    /** Update the current stock level for an item (from warehouse/node inventory). */
    public void updateStock(ResourceLocation itemId, long stockAmount)
    {
        currentStock.put(itemId, Math.max(0, stockAmount));
    }

    /** Update items currently in transit (e.g., being delivered by logistics). */
    public void updateInTransit(ResourceLocation itemId, long transitAmount)
    {
        inTransit.put(itemId, Math.max(0, transitAmount));
    }

    // ========================================================================
    //  Core pricing formula (mars-sim style)
    // ========================================================================

    /**
     * Calculate the raw C-Value for one item based on current supply/demand data.
     *
     * Formula: raw = DemandScore / (1 + SupplyScore)
     *   SupplyScore = sqrt(0.1 + stock + inTransit)
     *   DemandScore = cycleConsumption (more consumption → higher price)
     *
     * Clamped to [EconomyConfig.minCValue, EconomyConfig.maxCValue].
     */
    public double calculateRawPrice(ResourceLocation itemId)
    {
        ItemMarketSnapshot snap = cycleData.get(itemId);
        long demand = (snap != null) ? snap.consumption : 0;

        long stock = currentStock.getOrDefault(itemId, 0L);
        long transit = inTransit.getOrDefault(itemId, 0L);

        // SupplyScore: sqrt compression prevents hoarding from crashing prices
        double supplyScore = Math.sqrt(0.1 + stock + transit);

        // VP = Demand / (1 + Supply)
        double raw = demand / (1.0 + supplyScore);

        // Boundary clamping
        double minVp = EconomyConfig.minCValue();
        double maxVp = EconomyConfig.maxCValue();
        if (raw < minVp) raw = minVp;
        if (raw > maxVp) raw = maxVp;

        return raw;
    }

    /** Get all item IDs tracked in cycle or price cache (for commands/debug). */
    public Set<ResourceLocation> getAllItems()
    {
        Set<ResourceLocation> all = new HashSet<>(cycleData.keySet());
        all.addAll(priceCache.keySet());
        return all;
    }

    /** Reset all economy data (prices, stock, cycle). */
    public void resetAll()
    {
        priceCache.clear();
        cycleData.clear();
        currentStock.clear();
        inTransit.clear();
    }

    /**
     * Get the EMA-smoothed C-Value from the price cache.
     * If no cached value exists, calculates raw and seeds the cache.
     */
    public double getSmoothedPrice(ResourceLocation itemId)
    {
        Double cached = priceCache.get(itemId);
        return (cached != null) ? cached : 1.0; // fallback: neutral
    }

    /**
     * Apply EMA smoothing and update the price cache.
     * newPrice = (1 - alpha) * oldPrice + alpha * rawPrice
     *
     * Called by EconomyEngine during each price-update tick.
     */
    public void smoothPrice(ResourceLocation itemId, double rawPrice)
    {
        double alpha = EconomyConfig.emaAlpha();
        double oldPrice = priceCache.getOrDefault(itemId, 1.0);
        double newPrice = (1.0 - alpha) * oldPrice + alpha * rawPrice;

        // Re-apply boundary clamp after smoothing
        double minVp = EconomyConfig.minCValue();
        double maxVp = EconomyConfig.maxCValue();
        if (newPrice < minVp) newPrice = minVp;
        if (newPrice > maxVp) newPrice = maxVp;

        priceCache.put(itemId, newPrice);
    }

    // ========================================================================
    //  Cycle management
    // ========================================================================

    /** Reset all cycle-level counters (production/consumption). Called after settle. */
    public void resetCycle()
    {
        cycleData.values().forEach(ItemMarketSnapshot::reset);
    }

    /** Get the consumption total for an item in the current cycle. */
    public long getCurrentConsumption(ResourceLocation itemId)
    {
        ItemMarketSnapshot snap = cycleData.get(itemId);
        return (snap != null) ? snap.consumption : 0;
    }

    /** Get the production total for an item in the current cycle. */
    public long getCurrentProduction(ResourceLocation itemId)
    {
        ItemMarketSnapshot snap = cycleData.get(itemId);
        return (snap != null) ? snap.production : 0;
    }

    // ========================================================================
    //  NBT persistence
    // ========================================================================

    public CompoundTag toNBT()
    {
        CompoundTag root = new CompoundTag();

        // Price cache
        ListTag priceList = new ListTag();
        for (var entry : priceCache.entrySet())
        {
            CompoundTag pt = new CompoundTag();
            pt.putString("id", entry.getKey().toString());
            pt.putDouble("price", entry.getValue());
            priceList.add(pt);
        }
        root.put("prices", priceList);

        // Cycle data
        ListTag cycleList = new ListTag();
        for (var entry : cycleData.entrySet())
        {
            CompoundTag ct = new CompoundTag();
            ct.putString("id", entry.getKey().toString());
            ct.putLong("prod", entry.getValue().production);
            ct.putLong("cons", entry.getValue().consumption);
            cycleList.add(ct);
        }
        root.put("cycle", cycleList);

        return root;
    }

    public static MarketData fromNBT(CompoundTag tag)
    {
        MarketData md = new MarketData();

        // Restore price cache
        ListTag priceList = tag.getList("prices", Tag.TAG_COMPOUND);
        for (int i = 0; i < priceList.size(); i++)
        {
            CompoundTag pt = priceList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(pt.getString("id"));
            if (id != null)
                md.priceCache.put(id, pt.getDouble("price"));
        }

        // Restore cycle data
        ListTag cycleList = tag.getList("cycle", Tag.TAG_COMPOUND);
        for (int i = 0; i < cycleList.size(); i++)
        {
            CompoundTag ct = cycleList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(ct.getString("id"));
            if (id != null)
            {
                ItemMarketSnapshot snap = new ItemMarketSnapshot();
                snap.production = ct.getLong("prod");
                snap.consumption = ct.getLong("cons");
                md.cycleData.put(id, snap);
            }
        }

        return md;
    }

    // ========================================================================
    //  Internal snapshot
    // ========================================================================

    private static class ItemMarketSnapshot
    {
        long production;
        long consumption;

        void reset()
        {
            production = 0;
            consumption = 0;
        }
    }
}
