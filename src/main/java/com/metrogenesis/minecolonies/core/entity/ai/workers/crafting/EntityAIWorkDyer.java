package com.metrogenesis.minecolonies.core.entity.ai.workers.crafting;

import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingDyer;
import com.metrogenesis.minecolonies.core.colony.jobs.JobDyer;
import org.jetbrains.annotations.NotNull;

/**
 * Crafts dye related things.
 */
public class EntityAIWorkDyer extends AbstractEntityAIRequestSmelter<JobDyer, BuildingDyer>
{
    /**
     * Initialize the dyer.
     *
     * @param dyer the job he has.
     */
    public EntityAIWorkDyer(@NotNull final JobDyer dyer)
    {
        super(dyer);
    }

    @Override
    public Class<BuildingDyer> getExpectedBuildingClass()
    {
        return BuildingDyer.class;
    }
}
