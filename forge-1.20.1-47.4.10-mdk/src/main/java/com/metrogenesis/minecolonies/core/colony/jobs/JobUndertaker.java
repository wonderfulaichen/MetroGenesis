package com.metrogenesis.minecolonies.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.service.EntityAIWorkUndertaker;
import org.jetbrains.annotations.NotNull;

/**
 * Job class of the undertaker, handles the graves of dead citizen.
 */
public class JobUndertaker extends AbstractJobCrafter<EntityAIWorkUndertaker, JobUndertaker>
{
    /**
     * Public constructor of the farmer job.
     *
     * @param entity the entity to assign to the job.
     */
    public JobUndertaker(final ICitizenData entity)
    {
        super(entity);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.UNDERTAKER_ID;
    }

    /**
     * Override to add Job-specific AI tasks to the given EntityAITask list.
     */
    @NotNull
    @Override
    public EntityAIWorkUndertaker generateAI()
    {
        return new EntityAIWorkUndertaker(this);
    }
}
