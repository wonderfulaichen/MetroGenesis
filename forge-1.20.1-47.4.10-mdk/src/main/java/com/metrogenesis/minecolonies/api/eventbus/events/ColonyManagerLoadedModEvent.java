package com.metrogenesis.minecolonies.api.eventbus.events;

import com.metrogenesis.minecolonies.api.colony.IColonyManager;
import org.jetbrains.annotations.NotNull;

/**
 * Colony manager loaded event.
 */
public final class ColonyManagerLoadedModEvent extends AbstractModEvent
{
    /**
     * The colony manager instance.
     */
    @NotNull
    private final IColonyManager colonyManager;

    /**
     * Event for colony manager loaded.
     */
    public ColonyManagerLoadedModEvent(final @NotNull IColonyManager colonyManager)
    {
        this.colonyManager = colonyManager;
    }

    /**
     * Get the colony manager instance.
     *
     * @return the colony manager.
     */
    @NotNull
    public IColonyManager getColonyManager()
    {
        return colonyManager;
    }
}
