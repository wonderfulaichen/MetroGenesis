package com.metrogenesis.hologram;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side hologram management.
 * Coordinates between server and client for hologram rendering.
 */
public class MetroGenesisHologramMod
{
    // Server-side tracking of active holograms per dimension
    private static final Map<Integer, Map<BlockPos, HologramInfo>> dimensionHolograms = new ConcurrentHashMap<>();

    /**
     * Initialize the hologram module.
     */
    public static void init()
    {
        MetroGenesis.LOGGER.info("[Hologram] 鑷爺鍏ㄦ伅钃濆浘绯荤粺宸插垵濮嬪寲");
    }

    /**
     * Activate hologram rendering at a position.
     */
    public static void activateHologram(BlockPos pos, Zone zone, BuildingType type)
    {
        if (pos == null || zone == null || type == null)
        {
            return;
        }

        int dimId = System.identityHashCode(Thread.currentThread());

        dimensionHolograms.computeIfAbsent(dimId, k -> new ConcurrentHashMap<>())
            .put(pos, new HologramInfo(zone, type));

        MetroGenesis.LOGGER.debug("[Hologram] 婵€娲诲叏鎭? {} 绫诲瀷: {}", pos, type.getId());
    }

    /**
     * Deactivate hologram at a position.
     */
    public static void deactivateHologram(BlockPos pos)
    {
        int dimId = System.identityHashCode(Thread.currentThread());
        Map<BlockPos, HologramInfo> holograms = dimensionHolograms.get(dimId);
        if (holograms != null)
        {
            holograms.remove(pos);
        }
    }

    /**
     * Clear all holograms in a dimension.
     */
    public static void clearAll()
    {
        dimensionHolograms.clear();
    }

    /**
     * Get hologram info at position.
     */
    public static HologramInfo getHologramInfo(BlockPos pos)
    {
        for (Map<BlockPos, HologramInfo> holograms : dimensionHolograms.values())
        {
            HologramInfo info = holograms.get(pos);
            if (info != null)
            {
                return info;
            }
        }
        return null;
    }

    /**
     * Hologram information.
     */
    public static class HologramInfo
    {
        private final Zone zone;
        private final BuildingType type;

        public HologramInfo(Zone zone, BuildingType type)
        {
            this.zone = zone;
            this.type = type;
        }

        public Zone getZone() { return zone; }
        public BuildingType getType() { return type; }
    }
}
