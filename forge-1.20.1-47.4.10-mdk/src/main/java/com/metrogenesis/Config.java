package com.metrogenesis;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * Central Forge config for MetroGenesis.
 */
@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue CITY_STARTING_FUNDS = BUILDER
            .comment("Starting funds for the city treasury (in C-Value)")
            .defineInRange("cityStartingFunds", 1000, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SETTLEMENT_INTERVAL = BUILDER
        .comment("Game ticks between economy settlement cycles.")
        .defineInRange("settlementInterval", 24000, 1200, 72000);

    private static final ForgeConfigSpec.IntValue VP_UPDATE_INTERVAL = BUILDER
        .comment("Ticks between C-Value price updates.")
        .defineInRange("vpUpdateInterval", 1200, 200, 6000);

    private static final ForgeConfigSpec.DoubleValue EMA_ALPHA = BUILDER
        .comment("EMA smoothing weight. Lower = smoother.")
        .defineInRange("emaAlpha", 0.1d, 0.01d, 0.5d);

    private static final ForgeConfigSpec.DoubleValue MIN_CVALUE = BUILDER
        .comment("Minimum C-Value for any item.")
        .defineInRange("minCValue", 0.01d, 0.001d, 1.0d);

    private static final ForgeConfigSpec.DoubleValue MAX_CVALUE = BUILDER
        .comment("Maximum C-Value for any item.")
        .defineInRange("maxCValue", 10000d, 100d, 100000d);

    private static final ForgeConfigSpec.DoubleValue BASE_SKILL_EFFICIENCY = BUILDER
        .comment("Base work efficiency for a skill-1 citizen.")
        .defineInRange("baseSkillEfficiency", 0.5d, 0.1d, 1.0d);

    private static final ForgeConfigSpec.DoubleValue SKILL_EFFICIENCY_INCREMENT = BUILDER
        .comment("Efficiency gained per skill level.")
        .defineInRange("skillEfficiencyIncrement", 0.05d, 0.01d, 0.10d);

    private static final ForgeConfigSpec.DoubleValue DEFAULT_INCOME_TAX = BUILDER
        .comment("Default income tax rate.")
        .defineInRange("defaultIncomeTax", 0.15d, 0.0d, 0.5d);

    private static final ForgeConfigSpec.DoubleValue DEFAULT_CORPORATE_TAX = BUILDER
        .comment("Default corporate tax rate.")
        .defineInRange("defaultCorporateTax", 0.08d, 0.0d, 0.5d);

    private static final ForgeConfigSpec.IntValue MAX_CITIZEN_POPULATION = BUILDER
        .comment("Maximum number of citizens.")
        .defineInRange("maxCitizenPopulation", 500, 50, 5000);

    private static final ForgeConfigSpec.DoubleValue SUPPLY_DEMAND_ELASTICITY = BUILDER
        .comment("Supply-demand elasticity.")
        .defineInRange("supplyDemandElasticity", 0.5d, 0.1d, 1.0d);

    private static final ForgeConfigSpec.DoubleValue MAX_PRICE_FLUCTUATION = BUILDER
        .comment("Max daily price fluctuation.")
        .defineInRange("maxPriceFluctuation", 0.20d, 0.05d, 0.50d);

    private static final ForgeConfigSpec.DoubleValue PRICE_CEILING_MULTIPLIER = BUILDER
        .comment("Price ceiling = baseValue × this multiplier.")
        .defineInRange("priceCeilingMultiplier", 3.0d, 1.5d, 10.0d);

    private static final ForgeConfigSpec.DoubleValue PRICE_FLOOR_MULTIPLIER = BUILDER
        .comment("Price floor = baseValue × this multiplier.")
        .defineInRange("priceFloorMultiplier", 0.3d, 0.05d, 1.0d);

    private static final ForgeConfigSpec.DoubleValue SUPPLY_DEMAND_VOLATILITY = BUILDER
        .comment("Volatility coefficient for supply-demand price adjustment. Higher = more responsive.")
        .defineInRange("supplyDemandVolatility", 0.5d, 0.1d, 2.0d);

    // ══ Road / Graph config ══════════════════════════════════

    private static final ForgeConfigSpec.IntValue PIPELINE_INTERVAL = BUILDER
        .comment("Seconds between periodic road pipeline triggers.")
        .defineInRange("pipelineIntervalSeconds", 60, 5, 600);

    private static final ForgeConfigSpec.DoubleValue MAX_CONNECTION_DISTANCE = BUILDER
        .comment("Maximum distance (blocks) for a road graph edge connection.")
        .defineInRange("maxConnectionDistance", 100.0d, 10.0d, 1000.0d);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_SELECTORS = BUILDER
        .comment("Structure selectors for auto road generation. Use #tag or structure id.")
        .defineList("structureSelectors", List.of(), s -> s instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached values
    public static int    cityStartingFunds;
    public static int    settlementInterval;
    public static int    vpUpdateInterval;
    public static double emaAlpha;
    public static double minCValue;
    public static double maxCValue;
    public static double baseSkillEfficiency;
    public static double skillEfficiencyIncrement;
    public static double defaultIncomeTax;
    public static double defaultCorporateTax;
    public static int    maxCitizenPopulation;
    public static double supplyDemandElasticity;
    public static double maxPriceFluctuation;
    public static double priceCeilingMultiplier;
    public static double priceFloorMultiplier;
    public static double supplyDemandVolatility;

    // Road cache
    public static int    pipelineIntervalSeconds;
    public static double maxConnectionDistance;
    public static List<String> structureSelectors;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        cityStartingFunds        = CITY_STARTING_FUNDS.get();
        settlementInterval       = SETTLEMENT_INTERVAL.get();
        vpUpdateInterval         = VP_UPDATE_INTERVAL.get();
        emaAlpha                 = EMA_ALPHA.get();
        minCValue                = MIN_CVALUE.get();
        maxCValue                = MAX_CVALUE.get();
        baseSkillEfficiency      = BASE_SKILL_EFFICIENCY.get();
        skillEfficiencyIncrement = SKILL_EFFICIENCY_INCREMENT.get();
        defaultIncomeTax         = DEFAULT_INCOME_TAX.get();
        defaultCorporateTax      = DEFAULT_CORPORATE_TAX.get();
        maxCitizenPopulation     = MAX_CITIZEN_POPULATION.get();
        supplyDemandElasticity   = SUPPLY_DEMAND_ELASTICITY.get();
        maxPriceFluctuation      = MAX_PRICE_FLUCTUATION.get();
        priceCeilingMultiplier   = PRICE_CEILING_MULTIPLIER.get();
        priceFloorMultiplier     = PRICE_FLOOR_MULTIPLIER.get();
        supplyDemandVolatility   = SUPPLY_DEMAND_VOLATILITY.get();

        pipelineIntervalSeconds  = PIPELINE_INTERVAL.get();
        maxConnectionDistance    = MAX_CONNECTION_DISTANCE.get();
        structureSelectors       = List.copyOf(STRUCTURE_SELECTORS.get());
    }
}
