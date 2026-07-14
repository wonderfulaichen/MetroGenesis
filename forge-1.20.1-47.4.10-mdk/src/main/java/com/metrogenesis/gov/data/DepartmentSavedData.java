package com.metrogenesis.gov.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * 部门持久化容器 — 纯数据层，不持有部门引用。
 * <p>
 * 使用 DimensionSavedData 机制存储到 {@code {worldDir}/data/metrogenesis_departments.dat}。
 * DepartmentManager 在 tick() 后写入全部部门数据到此容器。
 * <p>
 * 数据结构：
 * <pre>
 * {
 *   "treasury": { ... TreasuryData fields ... },
 *   "commerce": { ... CommerceBureauData fields ... },
 *   ...
 * }
 * </pre>
 */
public class DepartmentSavedData extends SavedData {

    private static final String DATA_NAME = "metrogenesis_departments";

    /** 原始的部门数据 CompoundTag（key = 部门 ID, value = 该部门的 data CompoundTag） */
    private CompoundTag rawTags = new CompoundTag();

    // ══ 工厂方法 ═════════════════════════════════════

    /**
     * 从指定世界获取或创建 DepartmentSavedData 实例。
     *
     * @param level 服务端世界（overworld）
     * @return DepartmentSavedData 实例
     */
    public static DepartmentSavedData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(DepartmentSavedData::load, DepartmentSavedData::new, DATA_NAME);
    }

    // ══ 序列化 ═══════════════════════════════════════

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        // 将所有部门数据写入持久化 tag
        for (String key : rawTags.getAllKeys()) {
            tag.put(key, rawTags.getCompound(key).copy());
        }
        return tag;
    }

    /**
     * 从 NBT 反序列化。
     *
     * @param tag 源 CompoundTag
     * @return DepartmentSavedData 实例
     */
    public static DepartmentSavedData load(CompoundTag tag) {
        DepartmentSavedData data = new DepartmentSavedData();
        data.rawTags = tag.copy();
        return data;
    }

    // ══ 读写 ═════════════════════════════════════════

    /**
     * @return 缓存的原始部门数据（只读视图）
     */
    public CompoundTag getSavedTags() {
        return rawTags;
    }

    /**
     * 替换全部缓存的部门数据并标记脏。
     *
     * @param tags 新的部门数据
     */
    public void setSavedTags(CompoundTag tags) {
        this.rawTags = tags;
        setDirty();
    }

    /**
     * 标记数据为脏（需要持久化）—— 覆盖以保持可见性。
     */
    @Override
    public void setDirty() {
        super.setDirty();
    }
}
