package com.metrogenesis.minecolonies.core.colony.requestsystem.resolvers;

import com.metrogenesis.minecolonies.api.colony.requestsystem.location.ILocation;
import com.metrogenesis.minecolonies.api.colony.requestsystem.request.IRequest;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.IConcreteDeliverable;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.metrogenesis.minecolonies.api.colony.requestsystem.token.IToken;
import com.metrogenesis.minecolonies.api.util.InventoryUtils;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.metrogenesis.minecolonies.core.colony.requestsystem.resolvers.core.AbstractWarehouseRequestResolver;
import org.jetbrains.annotations.NotNull;

/**
 * ----------------------- Not Documented Object ---------------------
 */
public class WarehouseRequestResolver extends AbstractWarehouseRequestResolver
{
    public WarehouseRequestResolver(
      @NotNull final ILocation location,
      @NotNull final IToken<?> token)
    {
        super(location, token);
    }

    @Override
    protected int getWarehouseInternalCount(final BuildingWareHouse wareHouse, final IRequest<? extends IDeliverable> requestToCheck)
    {
        if (requestToCheck.getRequest() instanceof IConcreteDeliverable)
        {
            return 0;
        }

        return InventoryUtils.hasBuildingEnoughElseCount(wareHouse, itemStack -> requestToCheck.getRequest().matches(itemStack), requestToCheck.getRequest().getCount());
    }
}
