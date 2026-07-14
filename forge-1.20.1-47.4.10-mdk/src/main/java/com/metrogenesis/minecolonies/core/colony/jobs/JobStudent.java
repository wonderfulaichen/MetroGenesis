package com.metrogenesis.minecolonies.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.education.EntityAIStudy;
import org.jetbrains.annotations.NotNull;

/**
 * The student job class.
 */
public class JobStudent extends AbstractJob<EntityAIStudy, JobStudent>
{
    /**
     * Create a cook job.
     *
     * @param entity the student.
     */
    public JobStudent(final ICitizenData entity)
    {
        super(entity);
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @NotNull
    @Override
    public EntityAIStudy generateAI()
    {
        return new EntityAIStudy(this);
    }

    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.STUDENT_ID;
    }
}
