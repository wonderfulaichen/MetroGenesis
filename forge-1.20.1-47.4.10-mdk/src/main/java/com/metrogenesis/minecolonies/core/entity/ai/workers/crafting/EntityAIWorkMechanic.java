package com.metrogenesis.minecolonies.core.entity.ai.workers.crafting;

import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingMechanic;
import com.metrogenesis.minecolonies.core.colony.jobs.JobMechanic;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import org.jetbrains.annotations.NotNull;

/**
 * Crafts everything else basically (redstone stuff etc)
 */
public class EntityAIWorkMechanic extends AbstractEntityAICrafting<JobMechanic, BuildingMechanic>
{
    /**
     * Initialize the mechanic and add all his tasks.
     *
     * @param mechanic the job he has.
     */
    public EntityAIWorkMechanic(@NotNull final JobMechanic mechanic)
    {
        super(mechanic);
    }

    @Override
    public Class<BuildingMechanic> getExpectedBuildingClass()
    {
        return BuildingMechanic.class;
    }
}
