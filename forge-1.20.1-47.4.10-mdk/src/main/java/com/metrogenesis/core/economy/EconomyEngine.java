package com.metrogenesis.core.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core economy engine that runs periodic C-Value price updates.
 *
 * With supply/demand adjustment and DimensionSavedData persistence.
 *
 * Two cycles:
 * 1. Price update (every vpUpdateInterval ticks, default 1200 = 1 min)
 *    — recalculates all registered item prices
 * 2. Full settlement (every settlementInterval ticks, default 24000 = 1 day)
 *    — adjusts prices by supply/demand ratio, persists to saved data
 */
public class EconomyEngine
{
    /** Singleton instance for static access (used by MarketAccess). */
    private static EconomyEngine INSTANCE = null;

    private final MarketData marketData;

    /** Reference to persistent saved data (null on client side). */
    @Nullable
    private EconomySavedData savedData;

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
        INSTANCE = this;
    }

    /**
     * Get the singleton instance. May be null if not yet initialized.
     */
    @Nullable
    public static EconomyEngine getInstance()
    {
        return INSTANCE;
    }

    // ========================================================================
    //  Factory method — load from saved data or create fresh
    // ========================================================================

    /**
     * 从 overworld 加载或创建经济引擎。
     * <p>
     * 如果已有持久化的 MarketData，则从 saved data 恢复；
     * 否则创建新的 MarketData。
     *
     * @param level overworld 服务器世界
     * @return 经济引擎实例（全局单例）
     */
    public static EconomyEngine getOrCreate(final ServerLevel level)
    {
        // 如果已经初始化，直接返回
        if (INSTANCE != null) return INSTANCE;

        final EconomySavedData savedData = EconomySavedData.get(level);
        final MarketData md = savedData.getMarketData();

        final EconomyEngine engine = new EconomyEngine(md);
        engine.savedData = savedData;

        return engine;
    }

    /**
     * 设置持久化引用。在 getOrCreate() 调用时自动设置。
     */
    public void setSavedData(@Nullable final EconomySavedData savedData)
    {
        this.savedData = savedData;
    }

    /**
     * 强制持久化当前经济数据（服务器停止时调用）。
     */
    public void forceSave()
    {
        markDirty();
    }

    /**
     * 标记数据为脏（需要持久化）。
     */
    private void markDirty()
    {
        if (savedData != null)
        {
            savedData.setDirty();
        }
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
     * 1. Adjust prices based on supply/demand ratio
     * 2. Reset cycle counters
     * 3. Prepare for next day
     */
    public void settle()
    {
        dayCounter++;
        adjustPricesBySupplyDemand();
        marketData.resetCycle();
    }

    // ========================================================================
    //  Supply/Demand price adjustment (based on today's production/consumption)
    // ========================================================================

    /**
     * Adjust all item prices based on today's supply/demand ratio.
     * <p>
     * Formula for each item:
     *   supplyRatio = todayConsumed / max(todayProduced, 1)
     *   price *= 1 + (supplyRatio - 1) × volatilityCoefficient
     * <p>
     * Price is clamped to [baseValue × floorMultiplier, baseValue × ceilingMultiplier].
     */
    private void adjustPricesBySupplyDemand()
    {
        // 收集价格变化消息，用于发送通知
        final List<PriceChangeInfo> changes = new ArrayList<>();

        for (var entry : CValueRegistry.allEntries())
        {
            ResourceLocation itemId = entry.getKey();
            long baseValue = entry.getValue();

            long todayConsumed = marketData.getCurrentConsumption(itemId);
            long todayProduced = marketData.getCurrentProduction(itemId);

            // Fix 1: 无任何交易活动时跳过价格调整，维持当前价格
            if (todayConsumed == 0 && todayProduced == 0) continue;

            // Fix 2: 对称供需比公式 (consumed+1)/(produced+1)
            // 0/0 → 1.0（中性，价格不变）
            // 10/0 → 11/1 = 11.0（暴涨）
            // 0/100 → 1/101 ≈ 0.01（暴跌但不会到 0）
            // 5/5 → 6/6 = 1.0（平衡，中性）
            double supplyRatio = (double) (todayConsumed + 1) / (todayProduced + 1);

            // Adjust current price
            double currentPrice = getCurrentPrice(itemId);
            double volatility = EconomyConfig.supplyDemandVolatility();
            double priceMultiplier = 1.0 + (supplyRatio - 1.0) * volatility;
            double adjustedPrice = currentPrice * priceMultiplier;

            // Apply floor/ceiling per item based on its base value
            double ceiling = baseValue * EconomyConfig.priceCeilingMultiplier();
            double floor = Math.max(
                baseValue * EconomyConfig.priceFloorMultiplier(),
                EconomyConfig.minCValue()
            );
            if (adjustedPrice > ceiling) adjustedPrice = ceiling;
            if (adjustedPrice < floor)   adjustedPrice = floor;

            // Record price change for notification (before updating cache)
            // Fix 3: 记录变化，稍后统一发送通知
            double priceChangePct = Math.abs(adjustedPrice - currentPrice) / Math.max(currentPrice, 0.01);
            if (priceChangePct > 0.05)
            {
                changes.add(new PriceChangeInfo(itemId, currentPrice, adjustedPrice, priceChangePct));
            }

            // Update the price cache
            currentPrices.put(itemId, adjustedPrice);
            marketData.smoothPrice(itemId, adjustedPrice);
        }

        // Fix 3: 向所有在线玩家发送前 5 条变化最大的消息
        sendPriceChangeNotifications(changes);

        // 价格调整后标记持久化
        markDirty();
    }

    /**
     * 向所有在线玩家发送价格变化通知（前 5 条变化最大的物品）。
     */
    private void sendPriceChangeNotifications(final List<PriceChangeInfo> changes)
    {
        if (changes.isEmpty()) return;

        final var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        final var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        // 按变化幅度降序排列，取前 5 条
        final List<PriceChangeInfo> topChanges = changes.stream()
            .sorted(Comparator.comparingDouble(PriceChangeInfo::changePct).reversed())
            .limit(5)
            .toList();

        final Component header = Component.literal("§e━━━ [经济日报] 价格波动 TOP" + topChanges.size() + " ━━━");

        for (final var pc : topChanges)
        {
            final Item item = ForgeRegistries.ITEMS.getValue(pc.itemId());
            final String itemName = item != null
                ? item.getDescription().getString()
                : pc.itemId().getPath();

            final String direction = pc.adjustedPrice() > pc.currentPrice() ? "§a上涨" : "§c下跌";
            final double pct = (pc.adjustedPrice() - pc.currentPrice()) / pc.currentPrice() * 100;
            final String msg = String.format(
                "§e[经济] §7%s §f%s §7%.1f%% (%.1f → %.1f C)",
                itemName, direction, Math.abs(pct), pc.currentPrice(), pc.adjustedPrice()
            );

            players.forEach(p -> p.sendSystemMessage(Component.literal(msg)));
        }
    }

    /**
     * 价格变化信息记录。
     */
    private record PriceChangeInfo(ResourceLocation itemId, double currentPrice, double adjustedPrice, double changePct) {}

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
