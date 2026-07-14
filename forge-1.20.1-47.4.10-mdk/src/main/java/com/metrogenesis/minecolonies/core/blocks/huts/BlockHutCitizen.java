package com.metrogenesis.minecolonies.core.blocks.huts;

import com.metrogenesis.minecolonies.api.blocks.AbstractBlockHut;
import com.metrogenesis.minecolonies.api.colony.buildings.ModBuildings;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the citizen. No different from {@link AbstractBlockHut}
 */
public class BlockHutCitizen extends AbstractBlockHut<BlockHutCitizen>
{
    public BlockHutCitizen()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutcitizen";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.home.get();
    }
}
