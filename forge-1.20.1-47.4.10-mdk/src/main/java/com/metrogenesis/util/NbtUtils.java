package com.metrogenesis.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Map;

/**
 * Small helpers for safe NBT reads to reduce boilerplate.
 */
public final class NbtUtils {
    private NbtUtils() {
    }

    /**
     * Reads an enum value from NBT. If the key is missing or the value is invalid,
     * returns the provided default.
     */
    public static <E extends Enum<E>> E getEnumOrDefault(CompoundTag tag, String key, Class<E> type, E def) {
        if (!tag.contains(key)) return def;
        String raw = tag.getString(key);
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    /* ===================================================================== */
    /* Long↔value list helpers (k/v fields)                                   */
    /* ===================================================================== */

    private static final String K = "k";
    private static final String V = "v";

    public static ListTag toLongIntList(Map<Long, Integer> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Integer> e : map.entrySet()) {
            CompoundTag elem = new CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putInt(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongIntMap(ListTag list, Map<Long, Integer> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getInt(V));
        }
    }

    public static ListTag toLongDoubleList(Map<Long, Double> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Double> e : map.entrySet()) {
            CompoundTag elem = new CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putDouble(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongDoubleMap(ListTag list, Map<Long, Double> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getDouble(V));
        }
    }

    public static ListTag toLongStringList(Map<Long, String> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, String> e : map.entrySet()) {
            CompoundTag elem = new CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putString(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongStringMap(ListTag list, Map<Long, String> out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getString(V));
        }
    }
}
