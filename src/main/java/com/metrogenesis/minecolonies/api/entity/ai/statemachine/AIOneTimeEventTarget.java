package com.metrogenesis.minecolonies.api.entity.ai.statemachine;

import com.metrogenesis.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.metrogenesis.minecolonies.api.entity.ai.statemachine.states.IState;
import com.metrogenesis.minecolonies.api.entity.ai.statemachine.tickratestatemachine.IBooleanConditionSupplier;
import com.metrogenesis.minecolonies.api.entity.ai.statemachine.tickratestatemachine.IStateSupplier;
import com.metrogenesis.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingOneTimeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * One time usage AITarget, unregisters itself after usage
 */
public class AIOneTimeEventTarget<S extends IState> extends TickingOneTimeEvent<S>
{
    /**
     * Event to trigger a one time transition.
     *
     * @param predicate which has to be true to execute
     * @param action    Supplier for the state to transition into
     */
    public AIOneTimeEventTarget(@NotNull final IBooleanConditionSupplier predicate, @NotNull final IStateSupplier<S> action)
    {
        super(AIBlockingEventType.EVENT, predicate, action, 1);
    }

    /**
     * Event to trigger a one time transition.
     *
     * @param predicate which has to be true to execute
     * @param state     state to transition into
     */
    public AIOneTimeEventTarget(@NotNull final IBooleanConditionSupplier predicate, @NotNull final S state)
    {
        super(AIBlockingEventType.EVENT, predicate, () -> state, 1);
    }

    /**
     * Event to trigger a one time transition.
     *
     * @param action Supplier for the state to transition into
     */
    public AIOneTimeEventTarget(@NotNull final IStateSupplier<S> action)
    {
        super(AIBlockingEventType.EVENT, () -> true, action, 1);
    }

    /**
     * Event to trigger a one time transition.
     *
     * @param state state to transition into
     */
    public AIOneTimeEventTarget(@NotNull final S state)
    {
        super(AIBlockingEventType.EVENT, () -> true, () -> state, 1);
    }
}
