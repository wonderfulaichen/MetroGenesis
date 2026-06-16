package com.metrogenesis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * 分区数据持久化 — 存储在世界存档中的分区矩形列表
 * 参考 Minecraft 的 SavedData 机制（1.20.1）
 *
 * 每个 Overworld 保存一份分区数据，所有市长共享。
 * 结构：
 *   zones: [
 *     { minX, minZ, maxX, maxZ, zoneType },
 *     ...
 *   ]
 *
 * @deprecated 旧 16×16 网格分区系统。MayorBookScreen 仍依赖此类，
 *             后续迁移到 CityState SavedData。暂时保留不删除。
 */
@Deprecated
public class ZoneData extends SavedData {

    private static final String DATA_NAME = "metrogenesis_zones";

    private final List<int[]> zones = new ArrayList<>();
    // 每个 int[] = { minX, minZ, maxX, maxZ, zoneType }

    public ZoneData() {
    }

    /**
     * 从 NBT 加载
     */
    public static ZoneData load(CompoundTag tag) {
        ZoneData data = new ZoneData();
        ListTag list = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.zones.add(new int[]{
                entry.getInt("minX"),
                entry.getInt("minZ"),
                entry.getInt("maxX"),
                entry.getInt("maxZ"),
                entry.getInt("type"),
                entry.contains("direction") ? entry.getInt("direction") : 0
            });
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int[] zone : zones) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("minX", zone[0]);
            entry.putInt("minZ", zone[1]);
            entry.putInt("maxX", zone[2]);
            entry.putInt("maxZ", zone[3]);
            entry.putInt("type", zone[4]);
            entry.putInt("direction", zone.length >= 6 ? zone[5] : 0);
            list.add(entry);
        }
        tag.put("zones", list);
        return tag;
    }

    /**
     * 获取所有分区数据
     */
    public List<int[]> getZones() {
        return zones;
    }

    /**
     * 替换全部分区数据
     */
    public void setZones(List<int[]> newZones) {
        zones.clear();
        zones.addAll(newZones);
        setDirty(); // 标记需要保存
    }

    /**
     * 获取或创建 ZoneData（服务端）
     */
    public static ZoneData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            ZoneData::load,
            ZoneData::new,
            DATA_NAME
        );
    }
}
