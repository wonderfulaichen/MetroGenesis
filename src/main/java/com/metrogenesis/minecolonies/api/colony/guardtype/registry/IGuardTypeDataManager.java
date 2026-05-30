package com.metrogenesis.minecolonies.api.colony.guardtype.registry;

import com.metrogenesis.minecolonies.api.IMinecoloniesAPI;
import com.metrogenesis.minecolonies.api.colony.guardtype.GuardType;
import net.minecraft.resources.ResourceLocation;

public interface IGuardTypeDataManager
{

    static IGuardTypeDataManager getInstance()
    {
        return IMinecoloniesAPI.getInstance().getGuardTypeDataManager();
    }

    GuardType getFrom(ResourceLocation jobName);
}
