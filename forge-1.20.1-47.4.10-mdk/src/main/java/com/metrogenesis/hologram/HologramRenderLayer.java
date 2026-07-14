package com.metrogenesis.hologram;

import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.client.BlueprintRenderer;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hologram rendering layer on the client.
 */
public class HologramRenderLayer
{
    private static final Map<BlockPos, HologramData> activeHolograms = new ConcurrentHashMap<>();

    /**
     * Queue a hologram for rendering.
     */
    public static void queueHologram(BlockPos pos, BuildingType type, float alpha)
    {
        HologramData data = activeHolograms.get(pos);
        if (data == null)
        {
            // Create a default zone based on building type
            Zone zone = new Zone(pos, type.getZoneRadius());
            BlueprintRenderer renderer = HologramRenderer.getOrCreateRenderer(pos, zone, type);
            data = new HologramData(pos, type, renderer);
            activeHolograms.put(pos, data);
        }
        data.setAlpha(alpha);
    }

    /**
     * Remove a hologram from rendering.
     */
    public static void removeHologram(BlockPos pos)
    {
        HologramData data = activeHolograms.remove(pos);
        if (data != null)
        {
            HologramRenderer.invalidate(pos);
        }
    }

    /**
     * Get all active holograms.
     */
    public static Map<HologramData, Float> getActiveHolograms()
    {
        Map<HologramData, Float> result = new ConcurrentHashMap<>();
        for (HologramData data : activeHolograms.values())
        {
            result.put(data, data.getAlpha());
        }
        return result;
    }

    /**
     * Clear all holograms.
     */
    public static void clearAll()
    {
        activeHolograms.values().forEach(d -> HologramRenderer.invalidate(d.getPosition()));
        activeHolograms.clear();
    }

    /**
     * Data holder for a hologram.
     */
    public static class HologramData
    {
        private final BlockPos position;
        private final BuildingType type;
        private final BlueprintRenderer renderer;
        private float alpha = 0.6f;

        public HologramData(BlockPos pos, BuildingType type, BlueprintRenderer renderer)
        {
            this.position = pos;
            this.type = type;
            this.renderer = renderer;
        }

        public BlockPos getPosition() { return position; }
        public BuildingType getType() { return type; }
        public BlueprintRenderer getRenderer() { return renderer; }
        public float getAlpha() { return alpha; }
        public void setAlpha(float alpha) { this.alpha = alpha; }
    }
}
