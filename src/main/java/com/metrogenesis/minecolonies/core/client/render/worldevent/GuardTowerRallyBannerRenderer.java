package com.metrogenesis.minecolonies.core.client.render.worldevent;

import com.metrogenesis.structurize.client.rendertask.util.WorldRenderMacros;
import com.metrogenesis.minecolonies.api.colony.requestsystem.location.ILocation;
import com.metrogenesis.minecolonies.api.items.ModItems;
import com.metrogenesis.minecolonies.core.items.ItemBannerRallyGuards;

public class GuardTowerRallyBannerRenderer
{
    /**
     * Renders the rallying banner guard tower indicators into the world.
     * 
     * @param ctx rendering context
     */
    static void render(final WorldEventContext ctx)
    {
        if (ctx.mainHandItem.getItem() != ModItems.bannerRallyGuards)
        {
            return;
        }

        for (final ILocation guardTower : ItemBannerRallyGuards.getGuardTowerLocations(ctx.mainHandItem))
        {
            if (ctx.clientLevel.dimension() != guardTower.getDimension())
            {
                WorldRenderMacros.renderBlackLineBox(ctx.bufferSource,
                    ctx.poseStack,
                    guardTower.getInDimensionLocation(),
                    guardTower.getInDimensionLocation(),
                    0.02f);
            }
        }
    }
}
