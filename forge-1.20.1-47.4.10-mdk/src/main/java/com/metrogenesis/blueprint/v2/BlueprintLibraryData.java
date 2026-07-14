package com.metrogenesis.blueprint.v2;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.util.BlueprintUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 蓝图库 — 基于 SavedData 持久化存储所有玩家的蓝图和元数据。
 * <p>
 * 每个世界保存一份，存储在 world/data/metrogenesis_blueprints.dat。
 * Phase 3: 集成 StandardBlueprintData 元数据存储，支持图鉴分类。
 */
public class BlueprintLibraryData extends SavedData
{
    private static final String DATA_NAME = "metrogenesis_blueprints";

    private static final String TAG_BLUEPRINTS     = "blueprints";
    private static final String TAG_META           = "meta";
    private static final String TAG_NAME           = "name";
    private static final String TAG_BLUEPRINT      = "blueprint";
    private static final String TAG_META_ENTRIES   = "meta_entries";
    private static final String TAG_META_KEY       = "meta_key";
    private static final String TAG_META_DATA      = "meta_data";

    /** 蓝图存储：name → Blueprint */
    private final LinkedHashMap<String, Blueprint> blueprints = new LinkedHashMap<>();

    /** Phase 3: 蓝图元数据存储：name → StandardBlueprintData */
    private final LinkedHashMap<String, StandardBlueprintData> metaStore = new LinkedHashMap<>();

    /** 获取当前世界的蓝图库实例 */
    public static BlueprintLibraryData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
            BlueprintLibraryData::new,
            BlueprintLibraryData::new,
            DATA_NAME
        );
    }

    public BlueprintLibraryData() {}

    public BlueprintLibraryData(CompoundTag tag)
    {
        // 加载蓝图数据
        ListTag list = tag.getList(TAG_BLUEPRINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            String name = entry.getString(TAG_NAME);
            CompoundTag bpTag = entry.getCompound(TAG_BLUEPRINT);
            try
            {
                Blueprint bp = BlueprintUtil.readBlueprintFromNBT(bpTag);
                if (bp != null)
                {
                    bp.setName(name);
                    blueprints.put(name, bp);
                }
            }
            catch (Exception e)
            {
                MetroGenesis.LOGGER.warn("[BlueprintLib] Failed to load '{}': {}", name, e.getMessage());
            }
        }

        // Phase 3: 加载元数据
        CompoundTag metaTag = tag.getCompound(TAG_META);
        if (metaTag.contains(TAG_META_ENTRIES, Tag.TAG_LIST))
        {
            ListTag metaList = metaTag.getList(TAG_META_ENTRIES, Tag.TAG_COMPOUND);
            for (int i = 0; i < metaList.size(); i++)
            {
                CompoundTag entry = metaList.getCompound(i);
                String key = entry.getString(TAG_META_KEY);
                CompoundTag data = entry.getCompound(TAG_META_DATA);
                try
                {
                    StandardBlueprintData meta = StandardBlueprintData.deserializeNBT(data);
                    metaStore.put(key, meta);
                }
                catch (Exception e)
                {
                    MetroGenesis.LOGGER.warn("[BlueprintLib] Failed to load meta '{}': {}", key, e.getMessage());
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        // 保存蓝图数据
        ListTag list = new ListTag();
        for (Map.Entry<String, Blueprint> entry : blueprints.entrySet())
        {
            CompoundTag item = new CompoundTag();
            item.putString(TAG_NAME, entry.getKey());
            item.put(TAG_BLUEPRINT, BlueprintUtil.writeBlueprintToNBT(entry.getValue()));
            list.add(item);
        }
        tag.put(TAG_BLUEPRINTS, list);

        // Phase 3: 保存元数据
        CompoundTag metaTag = new CompoundTag();
        ListTag metaList = new ListTag();
        for (Map.Entry<String, StandardBlueprintData> entry : metaStore.entrySet())
        {
            CompoundTag item = new CompoundTag();
            item.putString(TAG_META_KEY, entry.getKey());
            item.put(TAG_META_DATA, entry.getValue().serializeNBT());
            metaList.add(item);
        }
        metaTag.put(TAG_META_ENTRIES, metaList);
        tag.put(TAG_META, metaTag);

        return tag;
    }

    // ══ Blueprint API ══════════════════════════════════

    /** 添加蓝图（含元数据） */
    public void addBlueprint(String name, Blueprint bp, @Nullable StandardBlueprintData meta)
    {
        blueprints.put(name, bp);
        if (meta != null)
        {
            metaStore.put(name, meta);
        }
        setDirty();
    }

    /** 添加蓝图（无元数据，旧版兼容） */
    public void addBlueprint(String name, Blueprint bp)
    {
        addBlueprint(name, bp, null);
    }

    /** 按名称获取蓝图 */
    public Blueprint getBlueprint(String name)
    {
        return blueprints.get(name);
    }

    /** 删除蓝图 */
    public void removeBlueprint(String name)
    {
        blueprints.remove(name);
        metaStore.remove(name);
        setDirty();
    }

    // ══ Meta API (Phase 3) ═══════════════════════════

    /** 设置蓝图元数据 */
    public void setMeta(String name, StandardBlueprintData meta)
    {
        metaStore.put(name, meta);
        setDirty();
    }

    /** 获取蓝图元数据，如果没有则返回 null */
    @Nullable
    public StandardBlueprintData getMeta(String name)
    {
        return metaStore.get(name);
    }

    /** 获取蓝图的分类名（如 "public_facilities"），没有则返回空字符串 */
    public String getMainCategory(String name)
    {
        StandardBlueprintData meta = metaStore.get(name);
        return meta != null ? meta.getMainCategory() : "";
    }

    // ══ Query API ═════════════════════════════════════

    /** 获取所有蓝图名称 */
    public Set<String> getAllNames()
    {
        return blueprints.keySet();
    }

    /** 获取所有蓝图 */
    public Collection<Blueprint> getAllBlueprints()
    {
        return blueprints.values();
    }

    /** 获取蓝图数量 */
    public int size()
    {
        return blueprints.size();
    }
}
