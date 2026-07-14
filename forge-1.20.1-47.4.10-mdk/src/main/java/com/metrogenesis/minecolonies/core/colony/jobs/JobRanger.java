package com.metrogenesis.minecolonies.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.guard.EntityAIRanger;

/**
 * The Ranger's Job class
 *
 * @author Asherslab
 */
public class JobRanger extends AbstractJobGuard<JobRanger>
{

    /**
     * The name associated with the job.
     */
    public static final String DESC = "com.metrogenesis.minecolonies.coremod.job.Ranger";

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobRanger(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public EntityAIRanger generateGuardAI()
    {
        return new EntityAIRanger(this);
    }

    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.ARCHER_GUARD_ID;
    }
}
