package com.metrogenesis.gov;

import net.minecraft.nbt.CompoundTag;

/**
 * 部门数据基类 — 所有部门数据的持久化基类。
 * <p>
 * 每个具体部门（如 TreasuryData、PopulationData）继承此类，
 * 实现自己的 save/load 方法。数据由 {@link DepartmentManager} 统一存档
 * 到世界目录的 metrogenesis_departments.dat 文件中。
 */
public abstract class DepartmentData {

    /**
     * 将部门数据写入 NBT。
     *
     * @param tag 目标 CompoundTag
     */
    public abstract void save(CompoundTag tag);

    /**
     * 从 NBT 读取部门数据。
     *
     * @param tag 源 CompoundTag
     */
    public abstract void load(CompoundTag tag);
}
