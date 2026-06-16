package com.metrogenesis.core.economy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Core economy engine that runs periodic C-Value price updates.
 *
 * Now uses mars-sim inspired formula:
 *   C-Value = DemandScore / (1 + sqrt(0.1 + stock + inTransit))
 *
 * With EMA smoothing and inflation boundary control.
 *
 * Two cycles:
 * 1. Price update (every vpUpdateInterval ticks, default 1200 = 1 min)
 *    — recalculates all registered item prices
 * 2. Full settlement (every settlementInterval ticks, default 24000 = 1 day)
 *    — resets cycle counters, runs tax/immigration/etc
 */
public class EconomyEngine
{
    private final MarketData marketData;

    // All registered items and their latest smoothed C-Value
    private final Map<ResourceLocation, Double> currentPrices = new HashMap<>();

    // Inflation tracking (mars-sim style deflation index)
    private final Map<String, Double> inflationIndex = new HashMap<>();

    private long tickCounter = 0;
    private long dayCounter = 0;

    public EconomyEngine(MarketData marketData)
    {
        this.marketData = marketData;
        // Seed all registered items with base value as initial price
        for (var entry : CValueRegistry.allEntries())
            currentPrices.put(entry.getKey(), (double) entry.getValue());
    }

    // ========================================================================
    //  Tick handler — call from ColonyState.tick()
    // ========================================================================

    /**
     * Called each server tick. Handles both price-update and settlement timing.
     */
    public void tick()
    {
        tickCounter++;

        // Price update cycle (mars-sim: 20 times/day = every 1200 ticks)
        if (tickCounter % EconomyConfig.vpUpdateInterval() == 0)
        {
            updateAllPrices();
        }

        // Full settlement cycle (daily)
        if (tickCounter % EconomyConfig.settlementInterval() == 0)
        {
            settle();
        }
    }

    // ========================================================================
    //  Price update (mars-sim GoodsManager.updatedMetrics)
    // ========================================================================

    /**
     * Recalculate C-Value for ALL registered items using current supply/demand data.
     */
    private void updateAllPrices()
    {
        for (var entry : CValueRegistry.allEntries())
        {
            ResourceLocation itemId = entry.getKey();
            updatePrice(itemId);
        }
    }

    /**
     * Calculate new C-Value for one item:
     * 1. Compute raw price from MarketData (mars-sim formula)
     * 2. Apply EMA smoothing against previous price
     * 3. Check inflation boundaries
     * 4. Update currentPrices cache
     */
    private void updatePrice(ResourceLocation itemId)
    {
        // Step 1: raw calculation
        double rawPrice = marketData.calculateRawPrice(itemId);

        // Step 2: EMA smoothing
        double oldPrice = currentPrices.getOrDefault(itemId, 1.0);
        double alpha = EconomyConfig.emaAlpha();
        double smoothed = (1.0 - alpha) * oldPrice + alpha * rawPrice;

        // Step 3: inflation boundary control (mars-sim checkDeflation)
        smoothed = applyInflationControl(itemId, smoothed);

        // Step 4: update cache
        currentPrices.put(itemId, smoothed);
        marketData.smoothPrice(itemId, smoothed);
    }

    /**
     * Apply mars-sim style inflation/紧缩 boundary control.
     * When price exceeds maxCValue: inflationIndex += 2 (tighten)
     * When price below minCValue: inflationIndex -= 2 (loosen)
     */
    private double applyInflationControl(ResourceLocation itemId, double price)
    {
        double maxVp = EconomyConfig.maxCValue();
        double minVp = EconomyConfig.minCValue();
        String key = itemId.toString();

        if (price > maxVp)
        {
            inflationIndex.merge(key, 2.0, Double::sum);
            price *= 0.81; // mars-sim's PERCENT_81
        }
        else if (price < minVp)
        {
            inflationIndex.merge(key, -2.0, Double::sum);
            price *= 1.1;  // gentle inflation boost
        }
        else
        {
            // Decay inflation index toward 0
            double idx = inflationIndex.getOrDefault(key, 0.0);
            if (Math.abs(idx) > 0.01)
                inflationIndex.put(key, idx * 0.95);
            else
                inflationIndex.remove(key);
        }

        // Re-clamp
        if (price < minVp) price = minVp;
        if (price > maxVp) price = maxVp;

        return price;
    }

    // ========================================================================
    //  Daily settlement (mars-sim's 1-sol resource review)
    // ========================================================================

    /**
     * Run the full daily settlement cycle.
     * 1. Estimate consumption from player/citizen activity (simplified)
     * 2. Reset cycle counters
     * 3. Prepare for next day
     */
    public void settle()
    {
        dayCounter++;

        // Placeholder: future enhancement - scan building inventories to infer real consumption
        // For now, we rely on explicit recordConsumption() calls (e.g., food consumption)
        // and approximate consumption based on known colony size and activity

        marketData.resetCycle();
    }

    // ========================================================================
    //  Public price query API
    // ========================================================================

    /**
     * Get the current smoothed C-Value for an item.
     * Falls back to base value if not yet priced.
     */
    public double getCurrentPrice(ResourceLocation itemId)
    {
        Double price = currentPrices.get(itemId);
        if (price != null) return price;

        // Fallback: use base value from registry
        long base = CValueRegistry.getBaseValueOrDefault(itemId, 1L);
        return (double) base;
    }

    /**
     * Get the current C-Value with technology coefficient applied.
     * Price = currentPrice * (1 + techCoefficient)
     */
    public double getCurrentPriceWithTech(@NotNull ResourceLocation itemId, double techCoefficient)
    {
        return getCurrentPrice(itemId) * (1.0 + techCoefficient);
    }

    /**
     * Get the price for an Item (convenience wrapper).
     */
    public double getCurrentPrice(Item item)
    {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return (id != null) ? getCurrentPrice(id) : 1.0;
    }

    // ========================================================================
    //  Wage & tax calculation (unchanged, but now uses C-Value prices)
    // ========================================================================

    /**
     * Calculate a citizen's daily wage based on work output and skill.
     * wage = dailyOutputValue * efficiency * wageCoefficient
     */
    public double calculateWage(double dailyOutputValue, int skillLevel, double wageCoefficient)
    {
        double baseEff = EconomyConfig.baseSkillEfficiency();
        double incr = EconomyConfig.skillEfficiencyIncrement();
        double efficiency = baseEff + (skillLevel - 1) * incr;
        if (efficiency > 1.0) efficiency = 1.0;
        if (efficiency < 0.1) efficiency = 0.1;

        return dailyOutputValue * efficiency * wageCoefficient;
    }

    /**
     * Calculate tax due on a given income amount.
     */
    public double calculateIncomeTax(double income, double taxRate)
    {
        return income * taxRate;
    }

    // ========================================================================
    //  Accessors
    // ========================================================================

    public MarketData getMarketData() { return marketData; }
    public long getTickCounter()      { return tickCounter; }
    public long getDayCounter()       { return dayCounter; }

    /**
     * Get all current prices as a read-only map.
     */
    public Map<ResourceLocation, Double> getAllPrices()
    {
        return new HashMap<>(currentPrices);
    }
}
