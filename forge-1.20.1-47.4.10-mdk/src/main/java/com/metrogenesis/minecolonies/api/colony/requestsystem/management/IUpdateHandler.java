package com.metrogenesis.minecolonies.api.colony.requestsystem.management;

import com.metrogenesis.minecolonies.api.colony.requestsystem.management.update.UpdateType;
import com.metrogenesis.minecolonies.api.colony.requestsystem.manager.IRequestManager;

public interface IUpdateHandler
{
    IRequestManager getManager();

    void handleUpdate(final UpdateType type);

    int getCurrentVersion();
}
