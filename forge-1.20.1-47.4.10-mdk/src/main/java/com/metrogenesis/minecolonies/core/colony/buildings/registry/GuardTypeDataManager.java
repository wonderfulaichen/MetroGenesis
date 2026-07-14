package com.metrogenesis.minecolonies.core.colony.buildings.registry;

import com.metrogenesis.minecolonies.api.colony.guardtype.GuardType;
import com.metrogenesis.minecolonies.api.colony.guardtype.registry.IGuardTypeDataManager;
import com.metrogenesis.minecolonies.api.colony.guardtype.registry.IGuardTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public final class GuardTypeDataManager implements IGuardTypeDataManager
{
    @Override
    public GuardType getFrom(final ResourceLocation jobName)
    {
        if (jobName == null)
        {
            return null;
        }

        return IGuardTypeRegistry.getInstance().getValue(jobName);
    }
}
