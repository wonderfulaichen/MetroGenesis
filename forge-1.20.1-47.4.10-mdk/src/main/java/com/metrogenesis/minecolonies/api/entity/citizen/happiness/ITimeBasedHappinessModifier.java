package com.metrogenesis.minecolonies.api.entity.citizen.happiness;

import com.metrogenesis.minecolonies.api.colony.ICitizenData;

/**
 * Interface describing possible happiness factors.
 */
public interface ITimeBasedHappinessModifier extends IHappinessModifier
{
    /**
     * Called at the end of each day.
     */
    default void dayEnd(final ICitizenData data) { }

    /**
     * Reset the modifier.
     */
    default void reset() { }

    /**
     * Get the days this is active.
     *
     * @return the days.
     */
    int getDays();
}
