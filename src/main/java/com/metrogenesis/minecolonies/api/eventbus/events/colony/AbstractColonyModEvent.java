package com.metrogenesis.minecolonies.api.eventbus.events.colony;

import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.eventbus.events.AbstractModEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Any colony related event, provides the target colony the event occurred in.
 */
public abstract class AbstractColonyModEvent extends AbstractModEvent
{
    /**
     * The colony this event was called in.
     */
    @NotNull
    private final IColony colony;

    /**
     * Constructs a colony-based event.
     *
     * @param colony The colony related to the event.
     */
    protected AbstractColonyModEvent(@NotNull final IColony colony)
    {
        this.colony = colony;
    }

    /**
     * Gets the colony related to the event.
     */
    @NotNull
    public IColony getColony()
    {
        return colony;
    }
}
