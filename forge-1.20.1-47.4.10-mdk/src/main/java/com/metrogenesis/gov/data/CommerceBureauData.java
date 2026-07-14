package com.metrogenesis.gov.data;

import com.metrogenesis.gov.DepartmentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 商务部数据 — 价格干预配置。
 * <p>
 * 维护每个物品的价格上下限覆盖，可启用/禁用价格干预模式。
 */
public class CommerceBureauData extends DepartmentData {

    private final Map<String, Double> priceCeilings = new HashMap<>();  // 物品ID → 价格上限
    private final Map<String, Double> priceFloors = new HashMap<>();    // 物品ID → 价格下限
    private boolean priceInterventionEnabled = false;

    // ══ NBT 键 ═════════════════════════════════════
    private static final String KEY_INTERVENTION = "priceInterventionEnabled";
    private static final String KEY_CEILINGS = "priceCeilings";
    private static final String KEY_FLOORS = "priceFloors";
    private static final String KEY_ITEM = "item";
    private static final String KEY_PRICE = "price";

    @Override
    public void save(CompoundTag tag) {
        tag.putBoolean(KEY_INTERVENTION, priceInterventionEnabled);

        ListTag ceilingsList = new ListTag();
        for (Map.Entry<String, Double> entry : priceCeilings.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(KEY_ITEM, entry.getKey());
            entryTag.putDouble(KEY_PRICE, entry.getValue());
            ceilingsList.add(entryTag);
        }
        tag.put(KEY_CEILINGS, ceilingsList);

        ListTag floorsList = new ListTag();
        for (Map.Entry<String, Double> entry : priceFloors.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(KEY_ITEM, entry.getKey());
            entryTag.putDouble(KEY_PRICE, entry.getValue());
            floorsList.add(entryTag);
        }
        tag.put(KEY_FLOORS, floorsList);
    }

    @Override
    public void load(CompoundTag tag) {
        this.priceInterventionEnabled = tag.getBoolean(KEY_INTERVENTION);

        this.priceCeilings.clear();
        ListTag ceilingsList = tag.getList(KEY_CEILINGS, Tag.TAG_COMPOUND);
        for (int i = 0; i < ceilingsList.size(); i++) {
            CompoundTag entryTag = ceilingsList.getCompound(i);
            priceCeilings.put(entryTag.getString(KEY_ITEM), entryTag.getDouble(KEY_PRICE));
        }

        this.priceFloors.clear();
        ListTag floorsList = tag.getList(KEY_FLOORS, Tag.TAG_COMPOUND);
        for (int i = 0; i < floorsList.size(); i++) {
            CompoundTag entryTag = floorsList.getCompound(i);
            priceFloors.put(entryTag.getString(KEY_ITEM), entryTag.getDouble(KEY_PRICE));
        }
    }

    // ══ Getters / Setters ═══════════════════════════

    public boolean isPriceInterventionEnabled() { return priceInterventionEnabled; }
    public void setPriceInterventionEnabled(boolean enabled) { this.priceInterventionEnabled = enabled; }

    public Map<String, Double> getPriceCeilings() { return Collections.unmodifiableMap(priceCeilings); }
    public Map<String, Double> getPriceFloors() { return Collections.unmodifiableMap(priceFloors); }

    /** 设置某个物品的价格上限（-1 表示移除限制） */
    public void setPriceCeiling(String itemId, double ceiling) {
        if (ceiling < 0) {
            priceCeilings.remove(itemId);
        } else {
            priceCeilings.put(itemId, ceiling);
        }
    }

    /** 设置某个物品的价格下限（-1 表示移除限制） */
    public void setPriceFloor(String itemId, double floor) {
        if (floor < 0) {
            priceFloors.remove(itemId);
        } else {
            priceFloors.put(itemId, floor);
        }
    }

    /** 查询某个物品的价格上限，没有设置则返回 -1 */
    public double getCeilingFor(String itemId) {
        return priceCeilings.getOrDefault(itemId, -1.0);
    }

    /** 查询某个物品的价格下限，没有设置则返回 -1 */
    public double getFloorFor(String itemId) {
        return priceFloors.getOrDefault(itemId, -1.0);
    }
}
