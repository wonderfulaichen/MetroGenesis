package com.metrogenesis.minecolonies.core.colony.buildings.modules;

import com.metrogenesis.minecolonies.api.colony.buildings.IBuilding;
import com.metrogenesis.minecolonies.api.colony.buildings.IBuildingWorkerModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.ICreatesResolversModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.ITickingModule;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.JobEntry;
import com.metrogenesis.minecolonies.api.entity.citizen.Skill;

import java.util.function.Function;

/**
 * Assignment module for miners.
 */
public class MinerBuildingModule extends WorkerBuildingModule implements ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public MinerBuildingModule(
      final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit);
    }

    @Override
    public boolean isFull()
    {
        return building.getAllAssignedCitizen().size() >= getModuleMax();
    }
}
