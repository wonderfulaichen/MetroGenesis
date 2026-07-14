package com.metrogenesis.minecolonies.core.blocks.huts;

import com.metrogenesis.minecolonies.api.blocks.AbstractBlockHut;
import com.metrogenesis.minecolonies.api.colony.buildings.ModBuildings;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;

import org.jetbrains.annotations.NotNull;

public class BlockHutComposter extends AbstractBlockHut<BlockHutComposter>
{

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutcomposter";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.composter.get();
    }
}
