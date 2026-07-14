package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.metrogenesis.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkCrusher;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * The crusher job class.
 */
public class JobCrusher extends AbstractJobCrafter<EntityAIWorkCrusher, JobCrusher>
{
    /**
     * Create a crusher job.
     *
     * @param entity the lumberjack.
     */
    public JobCrusher(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public double getDiseaseModifier()
    {
        final int skill = getCitizen().getCitizenSkillHandler().getLevel(getCitizen().getWorkBuilding().getModule(BuildingModules.CRUSHER_WORK).getPrimarySkill());
        return (int) ((100 - skill)/25.0);
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
        return ModModelTypes.SMELTER_ID;
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @NotNull
    @Override
    public EntityAIWorkCrusher generateAI()
    {
        return new EntityAIWorkCrusher(this);
    }

    @Override
    public void playSound(final BlockPos blockPos, final EntityCitizen worker)
    {
        worker.queueSound(SoundEvents.GRINDSTONE_USE, blockPos, 1, 20);
    }
}
