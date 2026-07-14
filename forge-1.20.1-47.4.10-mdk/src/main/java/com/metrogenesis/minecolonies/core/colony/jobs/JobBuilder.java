package com.metrogenesis.minecolonies.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * The job of the builder.
 */
public class JobBuilder extends AbstractJobStructure<EntityAIStructureBuilder, JobBuilder>
{
    /**
     * Instantiates builder job.
     *
     * @param entity citizen.
     */
    public JobBuilder(final ICitizenData entity)
    {
        super(entity);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.BUILDER_ID;
    }

    @NotNull
    @Override
    public EntityAIStructureBuilder generateAI()
    {
        return new EntityAIStructureBuilder(this);
    }
}
