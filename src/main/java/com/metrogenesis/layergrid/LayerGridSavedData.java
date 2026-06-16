package com.metrogenesis.layergrid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * LayerGrid 的世界存储 — 基于 Minecraft SavedData 机制的持久化。
 * <p>
 * 每个 overworld 维度存储一份 LayerGrid 数据，含所有已注册层及其宣告方块的坐标。
 * </p>
 *
 * <h3>NBT 结构</h3>
 * <pre>
 * {
 *   layers: [
 *     {
 *       name: "zone_residential",
 *       owner: "player_uuid",
 *       gridSize: 512,
 *       positions: [ {x, z}, {x, z}, ... ]
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 */
public class LayerGridSavedData extends SavedData
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DATA_NAME = "metrogenesis_layergrid";

    /** 核心数据 — LayerGrid 实例 */
    private final LayerGrid layerGrid;

    // ════════════════════════════════════════════════════════
    //  构造
    // ════════════════════════════════════════════════════════

    public LayerGridSavedData()
    {
        this.layerGrid = new LayerGrid();
    }

    public LayerGridSavedData(final LayerGrid layerGrid)
    {
        this.layerGrid = layerGrid;
    }

    // ════════════════════════════════════════════════════════
    //  访问
    // ════════════════════════════════════════════════════════

    /**
     * 获取存储的 LayerGrid 实例。
     */
    public LayerGrid getLayerGrid()
    {
        return layerGrid;
    }

    // ════════════════════════════════════════════════════════
    //  NBT 序列化
    // ════════════════════════════════════════════════════════

    /**
     * 从 NBT 加载 LayerGridSavedData。
     */
    public static LayerGridSavedData load(final CompoundTag tag)
    {
        final LayerGrid grid = new LayerGrid();
        final ListTag layersList = tag.getList("layers", Tag.TAG_COMPOUND);

        for (int i = 0; i < layersList.size(); i++)
        {
            final CompoundTag layerTag = layersList.getCompound(i);
            final String name = layerTag.getString("name");
            final String owner = layerTag.getString("owner");
            final int gridSize = layerTag.getInt("gridSize");

            if (name.isEmpty())
            {
                LOGGER.warn("Skipping unnamed layer at index {}", i);
                continue;
            }

            try
            {
                final Layer layer = grid.createLayer(name, owner, gridSize);
                final ListTag posList = layerTag.getList("positions", Tag.TAG_COMPOUND);

                for (int j = 0; j < posList.size(); j++)
                {
                    final CompoundTag posTag = posList.getCompound(j);
                    final int x = posTag.getInt("x");
                    final int z = posTag.getInt("z");
                    layer.claim(x, z);
                }

                LOGGER.debug("Loaded layer '{}' with {} positions", name, posList.size());
            }
            catch (IllegalArgumentException e)
            {
                LOGGER.warn("Skipping duplicate/conflicting layer '{}': {}", name, e.getMessage());
            }
        }

        LOGGER.info("Loaded LayerGrid with {} layers", layersList.size());
        return new LayerGridSavedData(grid);
    }

    @Override
    public CompoundTag save(final CompoundTag tag)
    {
        final ListTag layersList = new ListTag();

        for (final String layerName : layerGrid.getLayerNames())
        {
            final Layer layer = layerGrid.getLayer(layerName);
            if (layer == null) continue;

            final CompoundTag layerTag = new CompoundTag();
            layerTag.putString("name", layer.getName());
            layerTag.putString("owner", layer.getOwner());
            layerTag.putInt("gridSize", layer.getGridSize()); // 需要公开此方法

            final ListTag posList = new ListTag();
            for (final BlockPos pos : layer.getPositions())
            {
                final CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", pos.getX());
                posTag.putInt("z", pos.getZ());
                posList.add(posTag);
            }
            layerTag.put("positions", posList);

            layersList.add(layerTag);
        }

        tag.put("layers", layersList);
        LOGGER.debug("Saved LayerGrid with {} layers", layersList.size());
        return tag;
    }

    // ════════════════════════════════════════════════════════
    //  工厂方法
    // ════════════════════════════════════════════════════════

    /**
     * 从服务端维度获取或创建 LayerGridSavedData。
     *
     * @param level 服务端世界
     * @return LayerGridSavedData 实例
     */
    public static LayerGridSavedData getOrCreate(final ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
            LayerGridSavedData::load,
            LayerGridSavedData::new,
            DATA_NAME
        );
    }
}
