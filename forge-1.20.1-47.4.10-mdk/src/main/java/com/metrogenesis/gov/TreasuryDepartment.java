package com.metrogenesis.gov;

import com.metrogenesis.gov.data.TreasuryData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 财政部 — 税率管理 + 每日收支结算。
 * <p>
 * 每日结算时根据税率计算预估税收并上缴国库。
 * 税率可通过 Policy 或 GUI 调整。具体市民税收由 DailyScheduler 执行，
 * 财政部在此做策略层决策和宏观记录。
 */
public class TreasuryDepartment implements Department<TreasuryData> {

    private final TreasuryData data = new TreasuryData();

    @Override
    public String getId() {
        return "treasury";
    }

    @Override
    public String getDisplayName() {
        return "财政部";
    }

    @Override
    public TreasuryData getData() {
        return data;
    }

    @Override
    public void onDailyTick(CityState city) {
        // 重置日收支流水
        data.resetDailyFlow();

        int population = city.getPopulation();
        if (population <= 0) return;

        // 估算日均收入基数：假设每个市民日均产出 20 C-Value
        int avgIncomePerCapita = 20;

        // 各项税收
        int incomeTax = (int) (population * avgIncomePerCapita * data.getIncomeTaxRate());
        // 企业所得税按经济活动估算：假设企业数量 ≈ population / 5
        int corporateTax = (int) ((population / 5.0) * avgIncomePerCapita * data.getCorporateTaxRate());
        // 消费税按消费估算：假设人均日消费 10 C-Value
        int consumptionTax = (int) (population * 10.0 * data.getConsumptionTaxRate());
        // 地价税按面积估算：假设人均占地 2 单位
        int landTax = (int) (population * 2.0 * data.getLandTaxRate());

        int totalTax = incomeTax + corporateTax + consumptionTax + landTax;
        int totalExpense = 0; // 政策维护费由各政策自己通过 Policy 扣除

        if (totalTax > 0) {
            city.addToTreasury(totalTax);
            data.recordIncome(totalTax);
        }

        if (totalExpense > 0) {
            city.spend(totalExpense);
            data.recordExpense(totalExpense);
        }
    }

    @Override
    public List<Policy> getPolicies() {
        return List.of(
                new Policy("tax_cut_small", Component.literal("减税（小）"),
                        Component.literal("个人所得税降至 10%"), false, 5),
                new Policy("tax_cut_large", Component.literal("减税（大）"),
                        Component.literal("所有税率减半"), false, 20),
                new Policy("tax_hike", Component.literal("加税"),
                        Component.literal("所有税率上浮 50%"), false, 0),
                new Policy("stimulus", Component.literal("经济刺激"),
                        Component.literal("发放全民基本收入（每日 5 C/人）"), false, 50)
        );
    }
}
