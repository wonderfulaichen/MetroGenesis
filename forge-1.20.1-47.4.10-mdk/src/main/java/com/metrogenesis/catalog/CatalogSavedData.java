package com.metrogenesis.catalog;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图鉴数据持久化 — 将 BuildingCatalogEntry 列表存储到世界存档中。
 * <p>
 * 避免每次打开图鉴都重新扫描所有蓝图文件。
 * 文件位置：world/data/metrogenesis_catalog.dat
 * 使用方式：{@link #get(ServerLevel)} 加载，{@link #save(List)} 保存。
 */
public class CatalogSavedData extends SavedData
{
    private static final String DATA_NAME = "metrogenesis_catalog";

    // NBT 键名
    private static final String TAG_ENTRIES      = "entries";
    private static final String TAG_NAME         = "name";
    private static final String TAG_PACK         = "pack";
    private static final String TAG_RESOURCE     = "resource";
    private static final String TAG_CATEGORY     = "category";
    private static final String TAG_MC_CAT       = "mc_cat";
    private static final String TAG_MG_CAT       = "mg_cat";
    private static final String TAG_SIZE_X       = "sx";
    private static final String TAG_SIZE_Y       = "sy";
    private static final String TAG_SIZE_Z       = "sz";
    private static final String TAG_LEVELS       = "levels";
    private static final String TAG_HAS_ICON     = "icon";
    private static final String TAG_BUILDING_TYPE = "type";
    private static final String TAG_MIXED        = "mixed";
    private static final String TAG_COST         = "cost";
    private static final String TAG_DESC         = "desc";
    private static final String TAG_CATEGORY_IDX = "cat_idx"; // 扫描时的分类索引数，用于校验

    private List<BuildingCatalogEntry> entries = List.of();
    private int categoryCount = 0; // 扫描时的分类总数，检测分类映射变化

    public CatalogSavedData() {}

    public CatalogSavedData(CompoundTag tag)
    {
        ListTag list = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        List<BuildingCatalogEntry> loaded = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag t = list.getCompound(i);
            try
            {
                String name         = t.getString(TAG_NAME);
                String packName     = t.getString(TAG_PACK);
                String resourcePath = t.getString(TAG_RESOURCE);
                String category     = t.getString(TAG_CATEGORY);
                String mcCategory   = t.getString(TAG_MC_CAT);
                String mgCategory   = t.getString(TAG_MG_CAT);
                int sx = t.getInt(TAG_SIZE_X);
                int sy = t.getInt(TAG_SIZE_Y);
                int sz = t.getInt(TAG_SIZE_Z);
                BlockPos size = new BlockPos(sx, sy, sz);

                Set<Integer> levels = new TreeSet<>();
                ListTag lvlList = t.getList(TAG_LEVELS, Tag.TAG_INT);
                for (int j = 0; j < lvlList.size(); j++)
                    levels.add(lvlList.getInt(j));

                boolean hasIcon     = t.getBoolean(TAG_HAS_ICON);
                String buildingType = t.getString(TAG_BUILDING_TYPE);
                boolean isMixedUse  = t.getBoolean(TAG_MIXED);
                long cost           = t.getLong(TAG_COST);
                String desc         = t.getString(TAG_DESC);

                loaded.add(new BuildingCatalogEntry(
                    name, packName, resourcePath, category, mcCategory, mgCategory,
                    size, levels, hasIcon, buildingType, isMixedUse, cost, desc
                ));
            }
            catch (Exception e)
            {
                MetroGenesis.LOGGER.warn("[CatalogSavedData] Failed to load entry {}: {}", i, e.getMessage());
            }
        }
        this.entries = Collections.unmodifiableList(loaded);
        this.categoryCount = tag.getInt(TAG_CATEGORY_IDX);
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (BuildingCatalogEntry e : entries)
        {
            CompoundTag t = new CompoundTag();
            t.putString(TAG_NAME, e.name());
            t.putString(TAG_PACK, e.packName());
            t.putString(TAG_RESOURCE, e.resourcePath());
            t.putString(TAG_CATEGORY, e.category());
            t.putString(TAG_MC_CAT, e.mcCategory());
            t.putString(TAG_MG_CAT, e.mgCategory());
            t.putInt(TAG_SIZE_X, e.size().getX());
            t.putInt(TAG_SIZE_Y, e.size().getY());
            t.putInt(TAG_SIZE_Z, e.size().getZ());

            ListTag lvlList = new ListTag();
            for (int lvl : e.levels())
                lvlList.add( net.minecraft.nbt.IntTag.valueOf(lvl));
            t.put(TAG_LEVELS, lvlList);

            t.putBoolean(TAG_HAS_ICON, e.hasIcon());
            t.putString(TAG_BUILDING_TYPE, e.buildingType());
            t.putBoolean(TAG_MIXED, e.isMixedUse());
            t.putLong(TAG_COST, e.materialCost());
            t.putString(TAG_DESC, e.description());
            list.add(t);
        }
        tag.put(TAG_ENTRIES, list);
        tag.putInt(TAG_CATEGORY_IDX, categoryCount);
        return tag;
    }

    // ════════════════════════════════════════════════════════
    //  工厂方法
    // ════════════════════════════════════════════════════════

    /** 获取当前世界的图鉴持久化实例 */
    public static CatalogSavedData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
            CatalogSavedData::new,
            CatalogSavedData::new,
            DATA_NAME
        );
    }

    /** 注入从扫描器获得的最新数据 */
    public void setEntries(List<BuildingCatalogEntry> newEntries, int catCount)
    {
        this.entries = Collections.unmodifiableList(new ArrayList<>(newEntries));
        this.categoryCount = catCount;
        setDirty();
    }

    /** 获取缓存的条目列表 */
    public List<BuildingCatalogEntry> getEntries()
    {
        return entries;
    }

    /** 持久化数据是否可用（非空 + 分类映射版本匹配） */
    public boolean isValid(int expectedCategoryCount)
    {
        return !entries.isEmpty() && categoryCount == expectedCategoryCount;
    }
}
