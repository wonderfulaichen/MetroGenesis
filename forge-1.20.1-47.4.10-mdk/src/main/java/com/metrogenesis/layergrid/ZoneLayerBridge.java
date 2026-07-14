package com.metrogenesis.layergrid;

import com.metrogenesis.ZoneData;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

/**
 * ZoneData ↔ LayerGrid 双向桥接器。
 * <p>
 * 负责将旧的 {@link ZoneData} 矩形分区数据同步到新的 {@link LayerGrid} 分层体系。
 * 在 {@code ZoneData} 完全退役之前，本桥接器保证两套系统数据一致。
 * </p>
 *
 * <h3>分区类型 → 层名映射</h3>
 * <pre>
 *   0 → "zone_residential"
 *   1 → "zone_industrial"
 *   2 → "zone_commercial"
 *   3 → "zone_agriculture"
 *   4 → "zone_public"
 * </pre>
 *
 * <h3>同步策略</h3>
 * <ol>
 *   <li>全量同步 — 读取 ZoneData 的全部分区 int[][]，逐矩形 claim 到 LayerGrid</li>
 *   <li>增量同步 — 每次 ZoneData 变更后调用 {@link #syncFromZoneData}</li>
 *   <li>反向同步 — 从 LayerGrid 重建 ZoneData 的 int[][] 格式</li>
 * </ol>
 */
public class ZoneLayerBridge
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 分区类型 → 层名前缀映射 */
    private static final String[] ZONE_TYPE_LAYERS = {
        "zone_residential",   // 0
        "zone_industrial",    // 1
        "zone_commercial",    // 2
        "zone_agriculture",   // 3
        "zone_public"         // 4
    };

    private static final String BRIDGE_OWNER = "__bridge__";
    private static final int BRIDGE_GRID_SIZE = LayerGrid.DEFAULT_GRID_SIZE;

    // ════════════════════════════════════════════════════════
    //  公共 API
    // ════════════════════════════════════════════════════════

    /**
     * 将 ZoneData 中所有分区同步到 LayerGrid。
     * <p>
     * 先清除已有桥接层，再逐矩形 claim。
     * 此为全量同步操作（成本与分区面积成正比）。
     * </p>
     *
     * @param zoneData  源 ZoneData
     * @param layerGrid 目标 LayerGrid
     */
    public static void syncFromZoneData(final ZoneData zoneData, final LayerGrid layerGrid)
    {
        // 清除旧桥接层
        for (final String layerName : ZONE_TYPE_LAYERS)
        {
            final Layer existing = layerGrid.getLayer(layerName);
            if (existing != null)
            {
                // 逐个释放所有方块
                for (final BlockPos pos : existing.getPositions())
                {
                    layerGrid.release(pos.getX(), pos.getZ(), layerName);
                }
            }
        }

        // 重新创建桥接层并 claim 所有分区矩形
        final List<int[]> zones = zoneData.getZones();
        for (final int[] zone : zones)
        {
            if (zone.length < 5) continue;

            final int minX = zone[0];
            final int minZ = zone[1];
            final int maxX = zone[2];
            final int maxZ = zone[3];
            final int type = zone[4];

            if (type < 0 || type >= ZONE_TYPE_LAYERS.length)
            {
                LOGGER.warn("Unknown zone type {} at ({},{})~({},{})", type, minX, minZ, maxX, maxZ);
                continue;
            }

            final String layerName = ZONE_TYPE_LAYERS[type];

            // 确保层存在
            if (layerGrid.getLayer(layerName) == null)
            {
                layerGrid.createLayer(layerName, BRIDGE_OWNER, BRIDGE_GRID_SIZE);
                LOGGER.debug("Created bridge layer '{}'", layerName);
            }

            // 逐格子 claim（矩形区域）
            for (int x = minX; x <= maxX; x++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    layerGrid.claim(x, z, layerName);
                }
            }
        }

        LOGGER.info("Bridged {} zones to LayerGrid", zones.size());
    }

    /**
     * 从 LayerGrid 重建 ZoneData 格式的分区列表。
     * <p>
     * 反向操作：遍历所有桥接层，收集每个被 claim 的方块，
     * 按层名分组重建为矩形列表。
     * </p>
     *
     * @param layerGrid 源 LayerGrid
     * @return 分区列表，每个 int[] = {minX, minZ, maxX, maxZ, zoneType}
     */
    public static List<int[]> syncToZoneData(final LayerGrid layerGrid)
    {
        final List<int[]> result = new ArrayList<>();

        for (int type = 0; type < ZONE_TYPE_LAYERS.length; type++)
        {
            final String layerName = ZONE_TYPE_LAYERS[type];
            final Layer layer = layerGrid.getLayer(layerName);
            if (layer == null || layer.size() == 0) continue;

            // 将连续区域合并为矩形（简化实现：取整体包围盒）
            final Set<BlockPos> positions = layer.getPositions();
            if (positions.isEmpty()) continue;

            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (final BlockPos pos : positions)
            {
                minX = Math.min(minX, pos.getX());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxZ = Math.max(maxZ, pos.getZ());
            }

            result.add(new int[]{minX, minZ, maxX, maxZ, type});
        }

        return result;
    }

    /**
     * 获取分区类型对应的层名。
     *
     * @param zoneType 分区类型索引（0-4）
     * @return 层名，或 null（类型越界）
     */
    public static String getLayerNameForZoneType(final int zoneType)
    {
        if (zoneType >= 0 && zoneType < ZONE_TYPE_LAYERS.length)
        {
            return ZONE_TYPE_LAYERS[zoneType];
        }
        return null;
    }

    /**
     * 获取层名对应的分区类型。
     *
     * @param layerName 层名
     * @return 分区类型索引，或 -1（无匹配）
     */
    public static int getZoneTypeForLayerName(final String layerName)
    {
        for (int i = 0; i < ZONE_TYPE_LAYERS.length; i++)
        {
            if (ZONE_TYPE_LAYERS[i].equals(layerName))
            {
                return i;
            }
        }
        return -1;
    }
}
