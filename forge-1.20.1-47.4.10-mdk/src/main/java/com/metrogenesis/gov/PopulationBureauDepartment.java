package com.metrogenesis.gov;

import com.metrogenesis.gov.data.PopulationBureauData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 人口局 — 移民/生育政策（当前为骨架实现）。
 * <p>
 * 人口系统将于 Phase 5 与 CitizenManager 深度集成。
 * 当前仅提供政策配置存储，onDailyTick 为空实现。
 */
public class PopulationBureauDepartment implements Department<PopulationBureauData> {

    private final PopulationBureauData data = new PopulationBureauData();

    @Override
    public String getId() {
        return "population";
    }

    @Override
    public String getDisplayName() {
        return "人口局";
    }

    @Override
    public PopulationBureauData getData() {
        return data;
    }

    @Override
    public void onDailyTick(CityState city) {
        // 当前为空实现。人口系统集成将在 Phase 5 完成。
    }

    @Override
    public List<Policy> getPolicies() {
        return List.of(
                new Policy("open_borders", Component.literal("开放移民"),
                        Component.literal("启用移民流入"), false, 5),
                new Policy("baby_bonus", Component.literal("生育补贴"),
                        Component.literal("每个新生儿发放生育奖金"), false, 20),
                new Policy("skill_filter", Component.literal("技能筛选"),
                        Component.literal("仅允许高技能移民迁入"), false, 10)
        );
    }
}
