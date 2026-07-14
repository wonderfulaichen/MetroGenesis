package com.metrogenesis.minecolonies.core.blocks.huts;

import com.metrogenesis.minecolonies.api.blocks.AbstractBlockHut;
import com.metrogenesis.minecolonies.api.colony.buildings.ModBuildings;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the fisherman. No different from {@link AbstractBlockHut}
 */
public class BlockHutFisherman extends AbstractBlockHut<BlockHutFisherman>
{
    public BlockHutFisherman()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutfisherman";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.fisherman.get();
    }
}
