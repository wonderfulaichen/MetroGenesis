package com.metrogenesis.minecolonies.core.entity.ai.workers.crafting;

import com.metrogenesis.minecolonies.api.colony.requestsystem.request.IRequest;
import com.metrogenesis.minecolonies.api.crafting.IRecipeStorage;
import com.metrogenesis.minecolonies.api.util.StatsUtil;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingBlacksmith;
import com.metrogenesis.minecolonies.core.colony.jobs.JobBlacksmith;
import org.jetbrains.annotations.NotNull;

import static com.metrogenesis.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;

/**
 * Crafts tools and armour.
 */
public class EntityAIWorkBlacksmith extends AbstractEntityAICrafting<JobBlacksmith, BuildingBlacksmith>
{
    /**
     * Initialize the blacksmith and add all his tasks.
     *
     * @param blacksmith the job he has.
     */
    public EntityAIWorkBlacksmith(@NotNull final JobBlacksmith blacksmith)
    {
        super(blacksmith);
    }

    @Override
    public Class<BuildingBlacksmith> getExpectedBuildingClass()
    {
        return BuildingBlacksmith.class;
    }

}
