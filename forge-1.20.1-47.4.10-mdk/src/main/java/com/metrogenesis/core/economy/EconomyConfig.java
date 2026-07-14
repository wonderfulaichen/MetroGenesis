package com.metrogenesis.core.economy;

import com.metrogenesis.Config;

/**
 * Convenience accessors for C-Value economy settings.
 * All values are stored in the shared {@link Config} ForgeConfigSpec.
 */
public final class EconomyConfig
{
    private EconomyConfig() {}

    public static double supplyDemandElasticity() { return Config.supplyDemandElasticity; }
    public static double maxPriceFluctuation()    { return Config.maxPriceFluctuation; }
    public static double priceCeilingMultiplier() { return Config.priceCeilingMultiplier; }
    public static double priceFloorMultiplier()   { return Config.priceFloorMultiplier; }
    public static double supplyDemandVolatility() { return Config.supplyDemandVolatility; }
    public static int    settlementInterval()     { return Config.settlementInterval; }
    public static int    vpUpdateInterval()       { return Config.vpUpdateInterval; }
    public static double emaAlpha()               { return Config.emaAlpha; }
    public static double minCValue()              { return Config.minCValue; }
    public static double maxCValue()              { return Config.maxCValue; }
    public static double baseSkillEfficiency()    { return Config.baseSkillEfficiency; }
    public static double skillEfficiencyIncrement() { return Config.skillEfficiencyIncrement; }
    public static double defaultIncomeTax()       { return Config.defaultIncomeTax; }
    public static double defaultCorporateTax()    { return Config.defaultCorporateTax; }
    public static int    maxCitizenPopulation()   { return Config.maxCitizenPopulation; }
    public static int    cityStartingFunds()      { return Config.cityStartingFunds; }
}
