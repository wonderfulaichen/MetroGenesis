package com.metrogenesis.gov.data;

import com.metrogenesis.gov.DepartmentData;
import net.minecraft.nbt.CompoundTag;

/**
 * 财政部数据 — 税率配置 + 日收支流水。
 */
public class TreasuryData extends DepartmentData {

    // ══ 税率（可被政策调整）═══════════════════════════
    private double incomeTaxRate = 0.15;        // 个人所得税 15%
    private double corporateTaxRate = 0.10;     // 企业所得税 10%
    private double consumptionTaxRate = 0.08;   // 消费税 8%
    private double landTaxRate = 0.05;          // 地价税 5%

    // ══ 货币政策 ════════════════════════════════════
    private double baseInterestRate = 0.03;     // 基准利率 3%
    private double minWageCoefficient = 1.0;    // 最低工资系数

    // ══ 日收支流水（仅当日，每日结算时重置）═══════════
    private int dailyIncome = 0;
    private int dailyExpense = 0;

    // ══ NBT 键 ═════════════════════════════════════
    private static final String KEY_INCOME_TAX = "incomeTaxRate";
    private static final String KEY_CORP_TAX = "corporateTaxRate";
    private static final String KEY_CONSUMP_TAX = "consumptionTaxRate";
    private static final String KEY_LAND_TAX = "landTaxRate";
    private static final String KEY_INTEREST = "baseInterestRate";
    private static final String KEY_MIN_WAGE = "minWageCoefficient";
    private static final String KEY_DAILY_INCOME = "dailyIncome";
    private static final String KEY_DAILY_EXPENSE = "dailyExpense";

    @Override
    public void save(CompoundTag tag) {
        tag.putDouble(KEY_INCOME_TAX, incomeTaxRate);
        tag.putDouble(KEY_CORP_TAX, corporateTaxRate);
        tag.putDouble(KEY_CONSUMP_TAX, consumptionTaxRate);
        tag.putDouble(KEY_LAND_TAX, landTaxRate);
        tag.putDouble(KEY_INTEREST, baseInterestRate);
        tag.putDouble(KEY_MIN_WAGE, minWageCoefficient);
        tag.putInt(KEY_DAILY_INCOME, dailyIncome);
        tag.putInt(KEY_DAILY_EXPENSE, dailyExpense);
    }

    @Override
    public void load(CompoundTag tag) {
        this.incomeTaxRate = tag.getDouble(KEY_INCOME_TAX);
        this.corporateTaxRate = tag.getDouble(KEY_CORP_TAX);
        this.consumptionTaxRate = tag.getDouble(KEY_CONSUMP_TAX);
        this.landTaxRate = tag.getDouble(KEY_LAND_TAX);
        this.baseInterestRate = tag.getDouble(KEY_INTEREST);
        this.minWageCoefficient = tag.getDouble(KEY_MIN_WAGE);
        this.dailyIncome = tag.getInt(KEY_DAILY_INCOME);
        this.dailyExpense = tag.getInt(KEY_DAILY_EXPENSE);
    }

    // ══ Getters / Setters ═══════════════════════════

    public double getIncomeTaxRate() { return incomeTaxRate; }
    public void setIncomeTaxRate(double rate) { this.incomeTaxRate = rate; }

    public double getCorporateTaxRate() { return corporateTaxRate; }
    public void setCorporateTaxRate(double rate) { this.corporateTaxRate = rate; }

    public double getConsumptionTaxRate() { return consumptionTaxRate; }
    public void setConsumptionTaxRate(double rate) { this.consumptionTaxRate = rate; }

    public double getLandTaxRate() { return landTaxRate; }
    public void setLandTaxRate(double rate) { this.landTaxRate = rate; }

    public double getBaseInterestRate() { return baseInterestRate; }
    public void setBaseInterestRate(double rate) { this.baseInterestRate = rate; }

    public double getMinWageCoefficient() { return minWageCoefficient; }
    public void setMinWageCoefficient(double coeff) { this.minWageCoefficient = coeff; }

    public int getDailyIncome() { return dailyIncome; }
    public int getDailyExpense() { return dailyExpense; }

    /** 重置日收支流水（每日结算开始时调用） */
    public void resetDailyFlow() {
        this.dailyIncome = 0;
        this.dailyExpense = 0;
    }

    /** 记录一笔收入 */
    public void recordIncome(int amount) { this.dailyIncome += amount; }

    /** 记录一笔支出 */
    public void recordExpense(int amount) { this.dailyExpense += amount; }
}
