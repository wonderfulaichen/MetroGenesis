package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.core.colony.buildings.modules.BuildingModules;
import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.metrogenesis.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkComposter;
import org.jetbrains.annotations.NotNull;

public class JobComposter extends AbstractJob<EntityAIWorkComposter, JobComposter>
{

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobComposter(final ICitizenData entity)
    {
        super(entity);
    }

    /**
     * Get the RenderBipedCitizen.Model to use when the Citizen performs this job role.
     *
     * @return Model of the citizen.
     */
    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.COMPOSTER_ID;
    }

    @Override
    public EntityAIWorkComposter generateAI()
    {
        return new EntityAIWorkComposter(this);
    }

    @Override
    public double getDiseaseModifier()
    {
        final int skill = getCitizen().getCitizenSkillHandler().getLevel(getCitizen().getWorkBuilding().getModule(BuildingModules.COMPOSTER_WORK).getPrimarySkill());
        return (int) ((100 - skill)/25.0);
    }
}
