package com.metrogenesis.minecolonies.core.colony.interactionhandling.registry;

import com.metrogenesis.minecolonies.api.IMinecoloniesAPI;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.registry.InteractionResponseHandlerEntry;
import net.minecraftforge.registries.IForgeRegistry;

public interface IInteractionResponseHandlerRegistry
{
    static IForgeRegistry<InteractionResponseHandlerEntry> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getInteractionResponseHandlerRegistry();
    }
}
