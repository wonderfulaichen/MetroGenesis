package com.metrogenesis.minecolonies.core.colony.buildings.modules;

import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.api.colony.buildings.IBuilding;
import com.metrogenesis.minecolonies.api.colony.buildings.IBuildingWorkerModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.*;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.JobEntry;
import com.metrogenesis.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.metrogenesis.minecolonies.api.entity.citizen.Skill;
import com.metrogenesis.minecolonies.core.util.AttributeModifierUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;
import java.util.function.Function;

import static com.metrogenesis.minecolonies.api.util.constant.CitizenConstants.SKILL_BONUS_ADD;

/**
 * Assignment module for couriers
 */
public class HospitalAssignmentModule extends WorkerBuildingModule implements IBuildingEventsModule, ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public HospitalAssignmentModule(final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit);
    }

    @Override
    void onRemoval(final ICitizenData citizen)
    {
        super.onRemoval(citizen);
        final Optional<AbstractEntityCitizen> optCitizen = citizen.getEntity();
        optCitizen.ifPresent(entityCitizen -> AttributeModifierUtils.removeModifier(entityCitizen, SKILL_BONUS_ADD, Attributes.MOVEMENT_SPEED));
    }
}
