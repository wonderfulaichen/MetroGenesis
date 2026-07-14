package com.metrogenesis.core.economy;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.construction.ConstructionManager;
import com.metrogenesis.construction.ConstructionSite;
import com.metrogenesis.init.BuildingType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * R2（缺陷 C）经济信号注入器。
 * <p>
 * 经济引擎 {@link EconomyEngine} 与 {@link MarketData} 早已实现，但自研玩法代码中
 * 从无调用方 → 市场永远收不到供需信号 → 价格恒为基准值。
 * 本类在每日循环（与 {@link EconomyEngine#tick()} 的日结算同节奏）把"城市运转"翻译为
 * 供需事件，写入 {@link MarketData}：
 * <ul>
 *   <li><b>市民消费（需求）</b>：每名市民每日消耗一份食物篮（面包/小麦/苹果）。</li>
 *   <li><b>建筑产出（供给）</b>：每栋已建成建筑按类型产出对应物资
 *       （农场→小麦+面包；工坊→圆石+铁锭；仓库→箱子）。</li>
 *   <li><b>建筑维护（需求）</b>：每栋已建成建筑每日消耗少量建材用于维护
 *       （住宅/市政厅消耗原木+石头）。</li>
 * </ul>
 * 通过 {@link MarketAccess#purchase(ItemStack, int)}（记录消耗）与
 * {@link MarketAccess#sell(ItemStack, int)}（记录产量）写入，二者已正确路由到
 * {@code MarketData.recordConsumption/recordProduction}。
 * <p>
 * 这样市场才会出现真实的供需波动：人口多于农场 → 食物涨价；工坊多于维护需求 → 建材降价。
 */
public final class EconomySignals
{
    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis-EconomySignals");

    private EconomySignals() {}

    // ══ 单日产销系数（每市民 / 每栋建筑） ════════════════════════════════════

    /** 每名市民每日食物消费篮。 */
    private static final String[] CITIZEN_FOOD = { "minecraft:bread", "minecraft:wheat", "minecraft:apple" };
    private static final int CITIZEN_FOOD_PER_ITEM = 1;

    /** 各建筑类型每日产出（id → 物资:数量）。 */
    private static final String[][] FARM_OUTPUT   = { { "minecraft:wheat", "5" }, { "minecraft:bread", "3" } };
    private static final String[][] WORKSHOP_OUTPUT = { { "minecraft:cobblestone", "4" }, { "minecraft:iron_ingot", "1" } };
    private static final String[][] WAREHOUSE_OUTPUT = { { "minecraft:chest", "1" } };

    /** 各建筑类型每日维护消耗（id → 物资:数量）。 */
    private static final String[][] HOUSE_UPKEEP  = { { "minecraft:oak_log", "1" }, { "minecraft:stone", "1" } };
    private static final String[][] TOWNHALL_UPKEEP = { { "minecraft:oak_log", "2" }, { "minecraft:stone", "1" } };

    // ════════════════════════════════════════════════════════════════════════

    /**
     * 记录当日城市运转产生的全部供需信号。
     * 由 {@link ColonyState#tick(ServerLevel)} 的每日闸门调用（与日结算同节奏）。
     *
     * @param colony 殖民地状态（取市民数与建筑工地）
     * @param level  服务器世界
     */
    public static void recordDailyColonyActivity(final ColonyState colony, final ServerLevel level)
    {
        if (colony == null || !colony.hasTownHall()) return; // 未开城不运转

        recordCitizenConsumption(colony);
        recordBuildingActivity(colony, level);
    }

    // ══ 市民消费（需求） ════════════════════════════════════════════════════

    private static void recordCitizenConsumption(final ColonyState colony)
    {
        final CitizenManager mgr = colony.getCitizenManager();
        final int pop = (mgr != null) ? mgr.getCurrentCitizenCount() : 0;
        if (pop <= 0) return;

        for (final String foodId : CITIZEN_FOOD)
        {
            final ItemStack stack = stackOf(foodId);
            if (!stack.isEmpty())
            {
                MarketAccess.purchase(stack, pop * CITIZEN_FOOD_PER_ITEM);
            }
        }
    }

    // ══ 建筑产出 + 维护（供给 / 需求） ══════════════════════════════════════

    private static void recordBuildingActivity(final ColonyState colony, final ServerLevel level)
    {
        final ConstructionManager cm = ConstructionManager.get(level);
        if (cm == null) return;

        for (final ConstructionSite site : cm.getAllSites())
        {
            if (!site.isCompleted()) continue; // 仅已建成建筑持续运转

            final String typeId = site.getBuildingType().getId();
            recordProduction(typeId);
            recordUpkeep(typeId);
        }
    }

    private static void recordProduction(final String typeId)
    {
        switch (typeId)
        {
            case "farm_facility" -> emit(FARM_OUTPUT, true);
            case "workshop"       -> emit(WORKSHOP_OUTPUT, true);
            case "warehouse"      -> emit(WAREHOUSE_OUTPUT, true);
            default -> { /* 住宅/市政厅不产出物资 */ }
        }
    }

    private static void recordUpkeep(final String typeId)
    {
        switch (typeId)
        {
            case "house"      -> emit(HOUSE_UPKEEP, false);
            case "town_hall"  -> emit(TOWNHALL_UPKEEP, false);
            default -> { /* 生产型建筑维护在产出中已隐含，不重复计 */ }
        }
    }

    /** 把一组 [物品id, 数量] 写入市场：produce=true 记产量，否则记消耗。 */
    private static void emit(final String[][] entries, final boolean produce)
    {
        for (final String[] e : entries)
        {
            final ItemStack stack = stackOf(e[0]);
            if (stack.isEmpty()) continue;
            final int amount = Integer.parseInt(e[1]);
            if (produce) MarketAccess.sell(stack, amount);
            else          MarketAccess.purchase(stack, amount);
        }
    }

    // ══ 工具 ══════════════════════════════════════════════════════════════

    private static ItemStack stackOf(final String registryKey)
    {
        final ResourceLocation rl = ResourceLocation.tryParse(registryKey);
        if (rl == null) return ItemStack.EMPTY;
        final Item item = ForgeRegistries.ITEMS.getValue(rl);
        return (item != null) ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
