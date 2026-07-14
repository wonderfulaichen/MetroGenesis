package com.metrogenesis.minecolonies.core.colony.requestsystem.init;

import com.metrogenesis.minecolonies.api.colony.requestsystem.manager.RequestMappingHandler;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.*;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.crafting.PrivateCrafting;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.metrogenesis.minecolonies.api.colony.requestsystem.requestable.deliveryman.Pickup;
import com.metrogenesis.minecolonies.api.util.Log;
import com.metrogenesis.minecolonies.core.colony.requestable.SmeltableOre;
import com.metrogenesis.minecolonies.core.colony.requestsystem.requests.StandardRequests;

public class RequestSystemInitializer
{

    public static void onPostInit()
    {
        Log.getLogger().warn("Register mappings");
        RequestMappingHandler.registerRequestableTypeMapping(Stack.class, StandardRequests.ItemStackRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(Burnable.class, StandardRequests.BurnableRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(Delivery.class, StandardRequests.DeliveryRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(Pickup.class, StandardRequests.PickupRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(Food.class, StandardRequests.FoodRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(Tool.class, StandardRequests.ToolRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(SmeltableOre.class, StandardRequests.SmeltAbleOreRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(StackList.class, StandardRequests.ItemStackListRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(PublicCrafting.class, StandardRequests.PublicCraftingRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(PrivateCrafting.class, StandardRequests.PrivateCraftingRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(RequestTag.class, StandardRequests.ItemTagRequest.class);
        RequestMappingHandler.registerRequestableTypeMapping(MinimumStack.class, StandardRequests.MinStackRequest.class);
    }
}
