package com.metrogenesis.minecolonies.api.eventbus.events.colony;

import com.metrogenesis.minecolonies.api.colony.IColony;
import org.jetbrains.annotations.NotNull;

/**
 * Colony deleted event.
 */
public final class ColonyDeletedModEvent extends AbstractColonyModEvent
{
    /**
     * Constructs a colony deleted event.
     *
     * @param colony The colony related to the event.
     */
    public ColonyDeletedModEvent(final @NotNull IColony colony)
    {
        super(colony);
    }
}
