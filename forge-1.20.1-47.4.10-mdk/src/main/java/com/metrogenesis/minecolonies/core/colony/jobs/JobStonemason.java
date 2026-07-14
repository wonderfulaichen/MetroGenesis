package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkStonemason;
import com.metrogenesis.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * Class of the Stonemason job.
 */
public class JobStonemason extends AbstractJobCrafter<EntityAIWorkStonemason, JobStonemason>
{
    /**
     * Instantiates the job for the Stonemason.
     *
     * @param entity the citizen who becomes a Sawmill
     */
    public JobStonemason(final ICitizenData entity)
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
    public EntityAIWorkStonemason generateAI()
    {
        return new EntityAIWorkStonemason(this);
    }

    @Override
    public void playSound(final BlockPos blockPos, final EntityCitizen worker)
    {
        worker.queueSound(SoundEvents.DEEPSLATE_TILES_HIT, blockPos, 5, 1, 1.0f, 2.0f);
    }
}
