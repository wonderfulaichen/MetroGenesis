package com.metrogenesis.hologram;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.client.BlueprintRenderer;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram renderer using self-developed Blueprint system.
 * Replaces Structurize dependency.
 */
public class HologramRenderer
{
    private static final Map<BlockPos, BlueprintRenderer> renderers = new ConcurrentHashMap<>();

    /**
     * Create a Blueprint from a Zone and building type.
     */
    public static Blueprint createBlueprint(Zone zone, BuildingType type)
    {
        int sizeX = zone.getSizeX();
        int sizeY = type.getBuildHeight();
        int sizeZ = zone.getSizeZ();

        Blueprint blueprint = new Blueprint((short) sizeX, (short) sizeY, (short) sizeZ);

        // Generate building structure based on building type
        for (int x = 0; x < sizeX; x++)
        {
            for (int z = 0; z < sizeZ; z++)
            {
                // Foundation layer
                blueprint.addBlockState(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }

        // Walls
        for (int y = 1; y < sizeY - 1; y++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                for (int z = 0; z < sizeZ; z++)
                {
                    // Walls on the perimeter
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1)
                    {
                        blueprint.addBlockState(new BlockPos(x, y, z), Blocks.OAK_LOG.defaultBlockState());
                    }
                }
            }
        }

        // Roof
        for (int x = 0; x < sizeX; x++)
        {
            for (int z = 0; z < sizeZ; z++)
            {
                blueprint.addBlockState(new BlockPos(x, sizeY - 1, z), Blocks.OAK_SLAB.defaultBlockState());
            }
        }

        // Entrance (front center, 1 block wide)
        int entranceY = 1;
        int entranceX = sizeX / 2;
        int entranceZ = 0;
        blueprint.addBlockState(new BlockPos(entranceX, entranceY, entranceZ), Blocks.AIR.defaultBlockState());
        blueprint.addBlockState(new BlockPos(entranceX, entranceY + 1, entranceZ), Blocks.AIR.defaultBlockState());

        return blueprint;
    }

    /**
     * Get or create a renderer for a position.
     */
    public static BlueprintRenderer getOrCreateRenderer(BlockPos pos, Zone zone, BuildingType type)
    {
        return renderers.computeIfAbsent(pos, p -> {
            Blueprint blueprint = createBlueprint(zone, type);
            return new BlueprintRenderer(blueprint);
        });
    }

    /**
     * Invalidate renderer at position.
     */
    public static void invalidate(BlockPos pos)
    {
        BlueprintRenderer renderer = renderers.remove(pos);
        if (renderer != null)
        {
            renderer.close();
        }
    }

    /**
     * Clear all renderers.
     */
    public static void clearAll()
    {
        renderers.values().forEach(BlueprintRenderer::close);
        renderers.clear();
    }
}
