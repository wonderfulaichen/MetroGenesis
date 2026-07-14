package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkFletcher;
import com.metrogenesis.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * Class of the Fletcher job.
 */
public class JobFletcher extends AbstractJobCrafter<EntityAIWorkFletcher, JobFletcher>
{
    /**
     * Instantiates the job for the Fletcher.
     *
     * @param entity the citizen who becomes a Fletcher
     */
    public JobFletcher(final ICitizenData entity)
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
    public EntityAIWorkFletcher generateAI()
    {
        return new EntityAIWorkFletcher(this);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.FLETCHER_ID;
    }

    @Override
    public void playSound(final BlockPos blockPos, final EntityCitizen worker)
    {
        worker.queueSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, blockPos, 5, 0);
    }
}
