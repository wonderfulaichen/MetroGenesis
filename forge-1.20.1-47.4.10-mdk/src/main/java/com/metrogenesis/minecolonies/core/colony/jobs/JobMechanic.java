package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.metrogenesis.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkMechanic;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * Class of the Mechanic job.
 */
public class JobMechanic extends AbstractJobCrafter<EntityAIWorkMechanic, JobMechanic>
{
    /**
     * Instantiates the job for the Mechanic.
     *
     * @param entity the citizen who becomes a Sawmill
     */
    public JobMechanic(final ICitizenData entity)
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
    public EntityAIWorkMechanic generateAI()
    {
        return new EntityAIWorkMechanic(this);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.MECHANIST_ID;
    }

    @Override
    public void playSound(final BlockPos blockPos, final EntityCitizen worker)
    {
        worker.queueSound(SoundEvents.ARMOR_EQUIP_IRON, blockPos, 1, 0);
        worker.queueSound(SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, blockPos, 1, 0);
        worker.queueSound(SoundEvents.IRON_DOOR_OPEN, blockPos, 1, 0);
    }
}
