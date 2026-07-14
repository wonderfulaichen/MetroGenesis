package com.metrogenesis.core.economy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 市民消费/出售接口 — 供外部系统（Citizen AI、建筑逻辑）调用。
 * <p>
 * 所有操作记录到 MarketData 的供需统计中，由 EconomyEngine 在每日结算时
 * 根据供需比调整价格。
 * <p>
 * 用法示例：
 * <pre>{@code
 * // 市民在每个工作周期结束时：
 * PurchaseResult result = MarketAccess.purchase(wheatStack, 5);
 * MarketAccess.sell(面包Stack, 3);
 * }</pre>
 *
 * @see EconomyEngine
 * @see MarketData
 */
public final class MarketAccess
{
    private MarketAccess() {}

    // ========================================================================
    //  购买 API
    // ========================================================================

    /**
     * 市民尝试购买 {@code amount} 个 {@code item}。
     * <p>
     * 购买流程：
     * <ol>
     *   <li>查询 {@link CValueRegistry} 获取物品基准价</li>
     *   <li>通过 {@link EconomyEngine} 获取当前实际价格</li>
     *   <li>记录消耗量到 {@link MarketData#recordConsumption(ResourceLocation, int)}</li>
     *   <li>返回 {@link PurchaseResult}（暂不设库存限制，仅记录供需）</li>
     * </ol>
     *
     * @param item   要购买的物品（ItemStack，只需类型，数量意义不大）
     * @param amount 需求数量
     * @return 购买结果（actualBought 暂等于 amount，预留库存检查扩展点）
     */
    public static PurchaseResult purchase(final ItemStack item, final int amount)
    {
        final ResourceLocation itemId = getItemId(item);
        if (itemId == null || amount <= 0)
        {
            return new PurchaseResult(0, 0.0);
        }

        // 获取当前价格
        final double currentPrice = getCurrentPrice(itemId);

        // 记录消耗
        final EconomyEngine engine = EconomyEngine.getInstance();
        if (engine != null)
        {
            engine.getMarketData().recordConsumption(itemId, amount);
        }

        // 计算总价（暂不做货币扣除，预留接口）
        final double totalCost = currentPrice * amount;

        return new PurchaseResult(amount, totalCost);
    }

    // ========================================================================
    //  出售 API
    // ========================================================================

    /**
     * 市民出售 {@code amount} 个 {@code item} 到市场。
     * <p>
     * 出售流程：
     * <ol>
     *   <li>获取物品当前价格</li>
     *   <li>记录产量到 {@link MarketData#recordProduction(ResourceLocation, int)}</li>
     *   <li>按当前价格计算预估收入（留待后续货币系统对接）</li>
     * </ol>
     *
     * @param item   要出售的物品
     * @param amount 出售数量
     */
    public static void sell(final ItemStack item, final int amount)
    {
        final ResourceLocation itemId = getItemId(item);
        if (itemId == null || amount <= 0) return;

        // 记录产量
        final EconomyEngine engine = EconomyEngine.getInstance();
        if (engine != null)
        {
            engine.getMarketData().recordProduction(itemId, amount);
        }
    }

    // ========================================================================
    //  查询 API
    // ========================================================================

    /**
     * 获取物品当前单价（含供需波动后的价格）。
     *
     * @param item 物品
     * @return 当前单价，若无法获取则返回基准价
     */
    public static double getUnitPrice(final ItemStack item)
    {
        final ResourceLocation itemId = getItemId(item);
        if (itemId == null) return 0.0;
        return getCurrentPrice(itemId);
    }

    /**
     * 获取购买一定数量物品的预估总价（不实际扣款）。
     *
     * @param item   物品
     * @param amount 数量
     * @return 预估总价
     */
    public static double estimateCost(final ItemStack item, final int amount)
    {
        return getUnitPrice(item) * Math.max(0, amount);
    }

    // ========================================================================
    //  内部工具
    // ========================================================================

    @Nullable
    private static ResourceLocation getItemId(final ItemStack item)
    {
        if (item == null || item.isEmpty()) return null;
        return ForgeRegistries.ITEMS.getKey(item.getItem());
    }

    private static double getCurrentPrice(final ResourceLocation itemId)
    {
        final EconomyEngine engine = EconomyEngine.getInstance();
        if (engine != null)
        {
            return engine.getCurrentPrice(itemId);
        }
        // Fallback: use base value from CValueRegistry
        long base = CValueRegistry.getBaseValueOrDefault(itemId, 1L);
        return (double) base;
    }

    // ========================================================================
    //  数据类
    // ========================================================================

    /**
     * 购买结果记录。
     *
     * @param actualBought 实际购买数量
     * @param totalCost    总花费（C-Value）
     */
    public record PurchaseResult(int actualBought, double totalCost) {}
}
