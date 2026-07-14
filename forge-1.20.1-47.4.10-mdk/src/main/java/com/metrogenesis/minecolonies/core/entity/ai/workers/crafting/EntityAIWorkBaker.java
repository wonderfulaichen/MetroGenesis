package com.metrogenesis.minecolonies.core.entity.ai.workers.crafting;

import com.metrogenesis.minecolonies.api.colony.requestsystem.request.IRequest;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.metrogenesis.minecolonies.api.crafting.IRecipeStorage;
import com.metrogenesis.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.metrogenesis.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.metrogenesis.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.metrogenesis.minecolonies.api.util.StatsUtil;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingBaker;
import com.metrogenesis.minecolonies.core.colony.jobs.JobBaker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.metrogenesis.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;
import static com.metrogenesis.minecolonies.api.util.constant.StatisticsConstants.ITEMS_SMELTED_DETAIL;
import static com.metrogenesis.minecolonies.api.util.constant.StatisticsConstants.ITEMS_BAKED_DETAIL;
/**
 * Baker AI class.
 */
public class EntityAIWorkBaker extends AbstractEntityAIRequestSmelter<JobBaker, BuildingBaker>
{
    /**
     * Baking icon
     */
    private final static VisibleCitizenStatus BAKING =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/baker.png"), "com.metrogenesis.minecolonies.gui.visiblestatus.baker");

    /**
     * Constructor for the Baker. Defines the tasks the bakery executes.
     *
     * @param job a bakery job to use.
     */
    public EntityAIWorkBaker(@NotNull final JobBaker job)
    {
        super(job);
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingBaker> getExpectedBuildingClass()
    {
        return BuildingBaker.class;
    }

    /**
     * Returns the bakery's worker instance. Called from outside this class.
     *
     * @return citizen object.
     */
    @Nullable
    public AbstractEntityCitizen getCitizen()
    {
        return worker;
    }

    @Override
    protected IAIState craft()
    {
        worker.getCitizenData().setVisibleStatus(BAKING);
        return super.craft();
    }

    @Override
    public boolean isAfterDumpPickupAllowed()
    {
        return true;
    }

    /**
     * Returns the name of the smelting stat that is used in the building's statistics.
     * @return the name of the smelting stat.
     */
    protected String getSmeltingStatName()
    {
        return ITEMS_BAKED_DETAIL;
    }

}
