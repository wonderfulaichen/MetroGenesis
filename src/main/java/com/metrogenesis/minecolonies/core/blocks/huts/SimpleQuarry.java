package com.metrogenesis.minecolonies.core.blocks.huts;

import com.metrogenesis.minecolonies.api.blocks.AbstractBlockHut;
import com.metrogenesis.minecolonies.api.colony.buildings.ModBuildings;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the simple quarry. No different from {@link AbstractBlockHut}
 */
public class SimpleQuarry extends AbstractBlockHut<SimpleQuarry>
{
    public SimpleQuarry()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return ModBuildings.SIMPLE_QUARRY_ID;
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.simpleQuarry.get();
    }
}
