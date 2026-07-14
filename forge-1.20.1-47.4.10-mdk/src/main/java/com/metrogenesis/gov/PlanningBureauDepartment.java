package com.metrogenesis.gov;

import com.metrogenesis.gov.data.PlanningBureauData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 规划局 — 科技树 + 政策研究。
 * <p>
 * 每 3 天自动推进当前研究方向 5% 进度。
 * 提供可用政策列表供其他部门引用。
 */
public class PlanningBureauDepartment implements Department<PlanningBureauData> {

    private final PlanningBureauData data = new PlanningBureauData();

    /** 每 3 天触发一次进度推进 */
    private static final int RESEARCH_INTERVAL_DAYS = 3;
    /** 每次推进的进度量（百分比） */
    private static final int RESEARCH_PROGRESS_PER_STEP = 5;

    @Override
    public String getId() {
        return "planning";
    }

    @Override
    public String getDisplayName() {
        return "规划局";
    }

    @Override
    public PlanningBureauData getData() {
        return data;
    }

    @Override
    public void onDailyTick(CityState city) {
        String current = data.getCurrentResearch();
        if (current == null || current.isEmpty()) return;

        // 每 RESEARCH_INTERVAL_DAYS 天推进一次
        if (city.getDay() % RESEARCH_INTERVAL_DAYS == 0) {
            int newProgress = data.getResearchProgress() + RESEARCH_PROGRESS_PER_STEP;
            if (newProgress >= 100) {
                // 研究完成
                data.unlockTech(current);
                data.setCurrentResearch("");
                data.setResearchProgress(0);
            } else {
                data.setResearchProgress(newProgress);
            }
        }
    }

    @Override
    public List<Policy> getPolicies() {
        return List.of(
                new Policy("research_boost", Component.literal("研究加速"),
                        Component.literal("研究进度速度翻倍（每次 10%）"),
                        false, 15),
                new Policy("policy_cost_reduction", Component.literal("政策折扣"),
                        Component.literal("所有政策维护费用减半"),
                        false, 10),
                new Policy("infrastructure_focus", Component.literal("基建优先"),
                        Component.literal("建设局政策成本降低 30%"),
                        false, 8)
        );
    }
}
