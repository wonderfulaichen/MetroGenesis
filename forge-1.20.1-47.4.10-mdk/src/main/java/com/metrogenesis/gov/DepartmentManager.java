package com.metrogenesis.gov;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.gov.data.DepartmentSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 部门管理器 — 统一注册、调度、持久化编排所有部门。
 * <p>
 * 单例模式，世界加载时通过 {@link #getOrCreate(ServerLevel)} 初始化。
 * 持久化委托给 {@link DepartmentSavedData}，每次 {@link #tick()} 后写入全部数据。
 * <p>
 * 部门实例通过 {@link #registerDefaults()} 自动注册 5 个内置部门。
 * 注册时自动从持久化恢复各部门数据。
 */
public class DepartmentManager {

    /** 全局单例 */
    @Nullable
    private static DepartmentManager INSTANCE;

    /** 已注册部门（按注册顺序迭代） */
    private final Map<String, Department<?>> departments = new LinkedHashMap<>();

    /** 持久化容器 */
    private final DepartmentSavedData savedData;

    /** 城市状态 Facade */
    private final CityState cityState;

    // ══ 构造 / 单例 ═════════════════════════════════

    /**
     * 私有构造 — 通过 {@link #getOrCreate(ServerLevel)} 创建。
     *
     * @param level  服务端世界（overworld）
     * @param colony 殖民地状态
     */
    private DepartmentManager(ServerLevel level, ColonyState colony) {
        this.savedData = DepartmentSavedData.get(level);
        EconomyEngine economy = EconomyEngine.getInstance();
        this.cityState = new CityState(colony, economy);
    }

    /**
     * 获取或创建 DepartmentManager 单例。
     * <p>
     * 首次调用时自动注册 5 个默认部门并从持久化恢复数据。
     *
     * @param level 服务端世界（overworld）
     * @return DepartmentManager 单例
     */
    public static DepartmentManager getOrCreate(ServerLevel level) {
        if (INSTANCE == null) {
            ColonyState colony = ColonyState.get(level);
            INSTANCE = new DepartmentManager(level, colony);
            INSTANCE.registerDefaults();
        }
        return INSTANCE;
    }

    /**
     * @return 全局单例，可能为 null（未初始化时）
     */
    @Nullable
    public static DepartmentManager getInstance() {
        return INSTANCE;
    }

    // ══ 注册 ═════════════════════════════════════════

    /**
     * 注册 5 个内置部门。
     * 注册时自动从持久化恢复数据。
     */
    private void registerDefaults() {
        register(new TreasuryDepartment());
        register(new ConstructionBureauDepartment());
        register(new CommerceBureauDepartment());
        register(new PopulationBureauDepartment());
        register(new PlanningBureauDepartment());
    }

    /**
     * 注册一个部门并尝试从持久化恢复数据。
     *
     * @param dept 部门实例
     * @param <T>  部门数据类型
     */
    public <T extends DepartmentData> void register(Department<T> dept) {
        String id = dept.getId();

        // 从持久化恢复数据
        if (savedData.getSavedTags().contains(id)) {
            dept.getData().load(savedData.getSavedTags().getCompound(id));
        }

        departments.put(id, dept);
    }

    /**
     * 根据 ID 获取已注册的部门。
     *
     * @param id 部门 ID
     * @param <T> 部门数据类型
     * @return 部门实例，若未找到则返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends DepartmentData> Department<T> getDept(String id) {
        return (Department<T>) departments.get(id);
    }

    /**
     * @return 所有已注册部门的不可修改视图
     */
    public Collection<Department<?>> getAll() {
        return Collections.unmodifiableCollection(departments.values());
    }

    /**
     * @return 城市状态 Facade
     */
    public CityState getCityState() {
        return cityState;
    }

    // ══ 每日结算 ═════════════════════════════════════

    /**
     * 对所有已注册部门执行每日结算。
     * <p>
     * 由 {@link com.metrogenesis.colony.ColonyState#tick(ServerLevel)} 在每日边界触发。
     * 执行业务逻辑后自动持久化全部部门数据。
     */
    public void tick() {
        for (Department<?> dept : departments.values()) {
            dept.onDailyTick(cityState);
        }
        saveAll();
    }

    // ══ 持久化 ════════════════════════════════════════

    /**
     * 将所有部门数据写入持久化容器。
     * <p>
     * 数据结构（与 DepartmentSavedData 一致）：
     * <pre>
     * {
     *   "treasury": { TreasuryData fields },
     *   "construction": { ConstructionBureauData fields },
     *   "commerce": { CommerceBureauData fields },
     *   "population": { PopulationBureauData fields },
     *   "planning": { PlanningBureauData fields }
     * }
     * </pre>
     */
    public void saveAll() {
        CompoundTag tags = new CompoundTag();
        for (Map.Entry<String, Department<?>> entry : departments.entrySet()) {
            CompoundTag deptTag = new CompoundTag();
            entry.getValue().getData().save(deptTag);
            tags.put(entry.getKey(), deptTag);
        }
        savedData.setSavedTags(tags);
    }

    /**
     * 重置单例（用于测试或世界重载）。
     */
    public static void reset() {
        INSTANCE = null;
    }
}
