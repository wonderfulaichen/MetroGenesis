package com.metrogenesis.minecolonies.core.colony.requestsystem.requesters;

import com.metrogenesis.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.metrogenesis.minecolonies.api.colony.requestsystem.token.IToken;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface IBuildingBasedRequester extends IRequester
{
    /**
     * Get the building.
     *
     * @param manager the manager.
     * @param token   the token.
     * @return the IRequester or empty.
     */
    Optional<IRequester> getBuilding(@NotNull final IRequestManager manager, @NotNull final IToken<?> token);
}
