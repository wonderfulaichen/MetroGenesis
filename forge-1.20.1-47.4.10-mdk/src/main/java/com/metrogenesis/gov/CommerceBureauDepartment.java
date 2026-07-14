package com.metrogenesis.gov;

import com.metrogenesis.core.economy.MarketData;
import com.metrogenesis.gov.data.CommerceBureauData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 商务部 — 供需看板 + 价格干预。
 * <p>
 * 当价格干预启用时，每日结算时检查所有设置了上下限的物品，
 * 将 MarketData 中的对应价格限制覆盖为部门配置值。
 */
public class CommerceBureauDepartment implements Department<CommerceBureauData> {

    private final CommerceBureauData data = new CommerceBureauData();

    @Override
    public String getId() {
        return "commerce";
    }

    @Override
    public String getDisplayName() {
        return "商务部";
    }

    @Override
    public CommerceBureauData getData() {
        return data;
    }

    @Override
    public void onDailyTick(CityState city) {
        if (!data.isPriceInterventionEnabled()) return;

        MarketData marketData = city.getMarketData();
        if (marketData == null) return;

        // 遍历价格上限设置
        for (var entry : data.getPriceCeilings().entrySet()) {
            String itemId = entry.getKey();
            double ceiling = entry.getValue();
            applyPriceCap(marketData, itemId, ceiling);
        }

        // 遍历价格下限设置
        for (var entry : data.getPriceFloors().entrySet()) {
            String itemId = entry.getKey();
            double floor = entry.getValue();
            applyPriceFloor(marketData, itemId, floor);
        }
    }

    /**
     * 对 MarketData 设置价格上限。
     * MarketData 当前无直接的 setPriceCeiling API，通过设置供需比间接控制。
     * 此方法为扩展预留 —— 等价格干预接口就绪后接入。
     */
    private void applyPriceCap(MarketData marketData, String itemId, double ceiling) {
        // TODO: 当 MarketData 提供价格干预 API 后接入
        // 当前为桩实现：打印日志或记录到 MarketData 的自定义价格映射
    }

    /**
     * 对 MarketData 设置价格下限。
     */
    private void applyPriceFloor(MarketData marketData, String itemId, double floor) {
        // TODO: 当 MarketData 提供价格干预 API 后接入
    }

    @Override
    public List<Policy> getPolicies() {
        return List.of(
                new Policy("price_control", Component.literal("价格管制"),
                        Component.literal("启用/禁用价格干预模式"), false, 10),
                new Policy("free_market", Component.literal("自由市场"),
                        Component.literal("取消所有价格上下限限制"), false, 0),
                new Policy("trade_embargo", Component.literal("贸易禁运"),
                        Component.literal("禁止指定物品交易"), false, 5)
        );
    }
}
