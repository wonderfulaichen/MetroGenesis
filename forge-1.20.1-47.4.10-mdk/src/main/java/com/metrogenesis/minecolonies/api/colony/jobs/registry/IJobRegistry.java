package com.metrogenesis.minecolonies.api.colony.jobs.registry;

import com.metrogenesis.minecolonies.api.IMinecoloniesAPI;
import net.minecraftforge.registries.IForgeRegistry;

public interface IJobRegistry
{
    static IForgeRegistry<JobEntry> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getJobRegistry();
    }
}
