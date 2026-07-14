package com.metrogenesis.minecolonies.core.blocks.huts;

import com.metrogenesis.minecolonies.api.blocks.AbstractBlockHut;
import com.metrogenesis.minecolonies.api.colony.buildings.ModBuildings;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the shepherd. No different from {@link AbstractBlockHut}
 */
public class BlockHutShepherd extends AbstractBlockHut<BlockHutShepherd>
{
    public BlockHutShepherd()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutshepherd";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.shepherd.get();
    }
}
