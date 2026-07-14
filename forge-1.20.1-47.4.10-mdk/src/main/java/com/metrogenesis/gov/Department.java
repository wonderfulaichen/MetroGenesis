package com.metrogenesis.gov;

import java.util.List;

/**
 * 部门接口 — 定义政府部门的行为契约。
 * <p>
 * 每个部门有唯一 ID、显示名称、数据持久化、每日结算回调和政策列表。
 * GUI 面板（getPanel()）将在 Phase 2c 中补充。
 *
 * @param <T> 部门数据类型，必须继承 {@link DepartmentData}
 */
public interface Department<T extends DepartmentData> {

    /**
     * @return 部门唯一标识，如 "treasury"
     */
    String getId();

    /**
     * @return 显示名称，如 "财政部"
     */
    String getDisplayName();

    /**
     * @return 部门数据实例
     */
    T getData();

    /**
     * 每日结算回调 — 每天由 {@link DepartmentManager#tick()} 统一调用。
     * 各部门在此实现税收计算、人口更新、政策维护扣费等周期性逻辑。
     *
     * @param city 当前城市状态 Facade，可访问国库、人口、市场等
     */
    void onDailyTick(CityState city);

    /**
     * @return 是否有 GUI 面板（默认 false，有 GUI 的部门覆盖为 true）
     */
    default boolean hasPanel() {
        return false;
    }

    /**
     * @return 该部门当前可用的政策列表
     */
    List<Policy> getPolicies();
}
