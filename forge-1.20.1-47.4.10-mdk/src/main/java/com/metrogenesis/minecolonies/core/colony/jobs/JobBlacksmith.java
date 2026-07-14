package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkBlacksmith;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * Class of the Blacksmith job.
 */
public class JobBlacksmith extends AbstractJobCrafter<EntityAIWorkBlacksmith, JobBlacksmith>
{
    /**
     * Instantiates the job for the Blacksmith.
     *
     * @param entity the citizen who becomes a Sawmill
     */
    public JobBlacksmith(final ICitizenData entity)
    {
        super(entity);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.BLACKSMITH_ID;
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @Override
    public EntityAIWorkBlacksmith generateAI()
    {
        return new EntityAIWorkBlacksmith(this);
    }

    @Override
    public void playSound(final BlockPos blockPos, final EntityCitizen worker)
    {
        if (worker.getRandom().nextInt(8) < 1)
        {
            if (worker.getRandom().nextBoolean())
            {
                worker.queueSound(SoundEvents.ANVIL_USE, blockPos, 80, 0);
            }
            else
            {
                worker.queueSound(SoundEvents.SMITHING_TABLE_USE, blockPos, 20, 3);
            }
        }
    }
}
