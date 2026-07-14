package com.metrogenesis.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 城市需求数据快照 — 从服务端 ColonyState 采集，同步到客户端显示。
 * 包含人口、财政、建设进度、经济供需等核心指标。
 */
public class CityDemandsData
{
    // ════════════════════════════════════════════════════════
    //  字段
    // ════════════════════════════════════════════════════════

    /** 城市名称 */
    public String cityName = "未命名";
    /** 是否有市政厅 */
    public boolean hasTownHall = false;
    /** 国库余额 */
    public int funds = 0;
    /** 当前人口 */
    public int population = 0;
    /** 最大人口容量 */
    public int maxPopulation = 10;

    /** 活跃施工地块数 */
    public int activeConstructionSites = 0;
    /** 等待承建商的施工地块 */
    public int unclaimedSites = 0;

    /** 已绘制的功能区数量 */
    public int zoneCount = 0;
    /** 已放置的蓝图建筑数量 */
    public int placedBuildingCount = 0;

    /** 市场商品列表（缺省时显示前 N 条） */
    public final List<ItemSnapshot> topItems = new ArrayList<>();
    /** 赤字商品（消耗>产出） */
    public final List<ItemSnapshot> deficitItems = new ArrayList<>();

    // ════════════════════════════════════════════════════════
    //  内部记录
    // ════════════════════════════════════════════════════════

    public static class ItemSnapshot
    {
        public ResourceLocation itemId;
        public String displayName;
        public long production;
        public long consumption;
        public double price;

        public ItemSnapshot() {}

        public ItemSnapshot(ResourceLocation id, String name, long prod, long cons, double price)
        {
            this.itemId = id;
            this.displayName = name;
            this.production = prod;
            this.consumption = cons;
            this.price = price;
        }
    }

    // ════════════════════════════════════════════════════════
    //  NBT 序列化（可选的压缩方案，也可以用自定义编码）
    // ════════════════════════════════════════════════════════

    public CompoundTag toNBT()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString("cityName", cityName);
        tag.putBoolean("hasTownHall", hasTownHall);
        tag.putInt("funds", funds);
        tag.putInt("population", population);
        tag.putInt("maxPop", maxPopulation);
        tag.putInt("activeSites", activeConstructionSites);
        tag.putInt("unclaimedSites", unclaimedSites);
        tag.putInt("zoneCount", zoneCount);
        tag.putInt("placedBuildings", placedBuildingCount);

        // 前 10 条商品
        final ListTag topTag = new ListTag();
        for (int i = 0; i < Math.min(topItems.size(), 10); i++)
        {
            final ItemSnapshot snap = topItems.get(i);
            final CompoundTag st = new CompoundTag();
            st.putString("id", snap.itemId.toString());
            st.putString("name", snap.displayName);
            st.putLong("prod", snap.production);
            st.putLong("cons", snap.consumption);
            st.putDouble("price", snap.price);
            topTag.add(st);
        }
        tag.put("topItems", topTag);

        // 赤字商品
        final ListTag defTag = new ListTag();
        for (int i = 0; i < Math.min(deficitItems.size(), 6); i++)
        {
            final ItemSnapshot snap = deficitItems.get(i);
            final CompoundTag st = new CompoundTag();
            st.putString("id", snap.itemId.toString());
            st.putString("name", snap.displayName);
            st.putLong("prod", snap.production);
            st.putLong("cons", snap.consumption);
            st.putDouble("price", snap.price);
            defTag.add(st);
        }
        tag.put("deficitItems", defTag);

        return tag;
    }

    public static CityDemandsData fromNBT(CompoundTag tag)
    {
        final CityDemandsData data = new CityDemandsData();
        data.cityName = tag.getString("cityName");
        data.hasTownHall = tag.getBoolean("hasTownHall");
        data.funds = tag.getInt("funds");
        data.population = tag.getInt("population");
        data.maxPopulation = tag.getInt("maxPop");
        data.activeConstructionSites = tag.getInt("activeSites");
        data.unclaimedSites = tag.getInt("unclaimedSites");
        data.zoneCount = tag.getInt("zoneCount");
        data.placedBuildingCount = tag.getInt("placedBuildings");

        final ListTag topTag = tag.getList("topItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < topTag.size(); i++)
        {
            final CompoundTag st = topTag.getCompound(i);
            final ResourceLocation id = ResourceLocation.tryParse(st.getString("id"));
            if (id != null)
            {
                data.topItems.add(new ItemSnapshot(
                    id, st.getString("name"),
                    st.getLong("prod"), st.getLong("cons"),
                    st.getDouble("price")
                ));
            }
        }

        final ListTag defTag = tag.getList("deficitItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < defTag.size(); i++)
        {
            final CompoundTag st = defTag.getCompound(i);
            final ResourceLocation id = ResourceLocation.tryParse(st.getString("id"));
            if (id != null)
            {
                data.deficitItems.add(new ItemSnapshot(
                    id, st.getString("name"),
                    st.getLong("prod"), st.getLong("cons"),
                    st.getDouble("price")
                ));
            }
        }

        return data;
    }
}
