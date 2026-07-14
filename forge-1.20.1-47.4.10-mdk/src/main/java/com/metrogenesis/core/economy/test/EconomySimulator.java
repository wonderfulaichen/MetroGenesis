package com.metrogenesis.core.economy.test;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.core.economy.MarketData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Automated simulation that runs economic activity in a test colony.
 * Used by /cvalue simulate command to verify C-Value engine behavior.
 */
public class EconomySimulator
{
    private final ColonyState colony;
    private final ServerLevel level;
    private final MarketData market;

    // Test items
    public static final ResourceLocation IRON_ORE   = ResourceLocation.tryParse("minecraft:iron_ore");
    public static final ResourceLocation OAK_LOG    = ResourceLocation.tryParse("minecraft:oak_log");
    public static final ResourceLocation WHEAT      = ResourceLocation.tryParse("minecraft:wheat");
    public static final ResourceLocation BREAD      = ResourceLocation.tryParse("minecraft:bread");
    public static final ResourceLocation COBBLESTONE = ResourceLocation.tryParse("minecraft:cobblestone");

    public EconomySimulator(ColonyState colony, ServerLevel level)
    {
        this.colony = colony;
        this.level = level;
        this.market = colony.getEconomyEngine().getMarketData();
    }

    /**
     * Run a full simulation for the given number of game days.
     * Returns a report string summarizing the results.
     */
    public String simulate(int days)
    {
        StringBuilder report = new StringBuilder();
        report.append("=== Economy Simulation (")
            .append(days).append(" days) ===\n");

        // Day 1-3: Mining phase — miners dig iron
        report.append("\nPhase 1: Mining (days 1-3)\n");
        for (int d = 0; d < Math.min(3, days); d++)
        {
            simulateDay(new String[] {"mine"});
            report.append(String.format("  Day %d: iron_ore price = %.2f\n",
                d + 1, market.getSmoothedPrice(IRON_ORE)));
        }

        // Day 4-6: Farming phase — farmers grow wheat, bakers consume
        if (days > 3)
        {
            report.append("\nPhase 2: Farming + Baking (days 4-6)\n");
            for (int d = 3; d < Math.min(6, days); d++)
            {
                simulateDay(new String[] {"farm", "bake"});
                report.append(String.format("  Day %d: wheat price = %.2f, bread price = %.2f\n",
                    d + 1, market.getSmoothedPrice(WHEAT), market.getSmoothedPrice(BREAD)));
            }
        }

        // Day 7+: Mixed economy
        if (days > 6)
        {
            report.append("\nPhase 3: Mixed economy (days 7+)\n");
            for (int d = 6; d < days; d++)
            {
                simulateDay(new String[] {"mine", "farm", "bake", "consume"});
            }
            report.append(String.format("  Final: iron=%.2f, wheat=%.2f, bread=%.2f, cobble=%.2f\n",
                market.getSmoothedPrice(IRON_ORE),
                market.getSmoothedPrice(WHEAT),
                market.getSmoothedPrice(BREAD),
                market.getSmoothedPrice(COBBLESTONE)));
        }

        // Summary: price range
        report.append("\n=== Summary ===\n");
        double min = Double.MAX_VALUE, max = 0;
        ResourceLocation minItem = null, maxItem = null;
        for (ResourceLocation id : market.getAllItems())
        {
            double p = market.getSmoothedPrice(id);
            if (p < min) { min = p; minItem = id; }
            if (p > max) { max = p; maxItem = id; }
        }
        if (minItem != null)
        {
            report.append(String.format("  Lowest price: %s = %.2f\n", minItem, min));
            report.append(String.format("  Highest price: %s = %.2f\n", maxItem, max));
        }

        // Tracked items count
        int tracked = market.getAllItems().size();
        report.append(String.format("  Items tracked: %d\n", tracked));

        // Economy engine ticks
        report.append(String.format("  Days simulated: %d\n", days));
        report.append("========================\n");
        return report.toString();
    }

    /** Simulate one day of economic activity. */
    private void simulateDay(String[] activities)
    {
        // Morning: produce/consume
        for (String activity : activities)
        {
            switch (activity)
            {
                case "mine":
                    // 5 miners each mine 3 iron ore
                    for (int i = 0; i < 5; i++)
                    {
                        market.recordProduction(IRON_ORE, 3);
                        market.recordProduction(COBBLESTONE, 8); // byproduct
                    }
                    break;
                case "farm":
                    // 3 farmers each harvest 6 wheat
                    for (int i = 0; i < 3; i++)
                        market.recordProduction(WHEAT, 6);
                    break;
                case "bake":
                    // 2 bakers each use 3 wheat to make 2 bread
                    for (int i = 0; i < 2; i++)
                    {
                        market.recordConsumption(WHEAT, 3);
                        market.recordProduction(BREAD, 2);
                    }
                    break;
                case "consume":
                    // 10 citizens each eat 1 bread
                    for (int i = 0; i < 10; i++)
                        market.recordConsumption(BREAD, 1);
                    break;
            }
        }

        // Run economy ticks (1200 ticks = 1 minute intervals)
        for (int t = 0; t < 20; t++) // simulate 20 price updates per day
        {
            colony.getEconomyEngine().tick();
        }

        // End-of-day settlement
        colony.getEconomyEngine().settle();
    }
}
