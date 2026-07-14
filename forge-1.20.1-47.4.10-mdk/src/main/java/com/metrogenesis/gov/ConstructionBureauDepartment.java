package com.metrogenesis.gov;

import com.metrogenesis.gov.data.ConstructionBureauData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 建设局 — 施工管理（当前为骨架实现）。
 * <p>
 * Zone 系统和自动建设由其他模块处理（Phase 3-4）。
 * 本部门负责配置最大同时建设数和自动填充开关。
 */
public class ConstructionBureauDepartment implements Department<ConstructionBureauData> {

    private final ConstructionBureauData data = new ConstructionBureauData();

    @Override
    public String getId() {
        return "construction";
    }

    @Override
    public String getDisplayName() {
        return "建设局";
    }

    @Override
    public ConstructionBureauData getData() {
        return data;
    }

    @Override
    public void onDailyTick(CityState city) {
        // 当前为空实现。Zone 系统集成将在 Phase 4 完成。
    }

    @Override
    public List<Policy> getPolicies() {
        return List.of(
                new Policy("construction_boost", Component.literal("加速建设"),
                        Component.literal("最大同时建设数翻倍"), false, 10),
                new Policy("auto_fill", Component.literal("自动填充"),
                        Component.literal("启用区内自动填充（需先完成 Phase 4 研究）"), false, 15)
        );
    }
}
