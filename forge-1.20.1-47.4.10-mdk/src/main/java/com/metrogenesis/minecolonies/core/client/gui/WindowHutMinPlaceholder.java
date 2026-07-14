package com.metrogenesis.minecolonies.core.client.gui;

import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.colony.buildings.views.AbstractBuildingView;
import net.minecraft.resources.ResourceLocation;

/**
 * BOWindow for worker. Placeholder for many different jobs.
 *
 * @param <B> Object extending {@link AbstractBuildingView}.
 */
public class WindowHutMinPlaceholder<B extends AbstractBuildingView> extends AbstractBuildingMainWindow<B>
{
    /**
     * BOWindow for worker placeholder. Used by buildings not listed above this file.
     *
     * @param building AbstractBuilding extending {@link AbstractBuildingView}.
     */
    public WindowHutMinPlaceholder(final B building)
    {
        super(building, new ResourceLocation(Constants.MOD_ID, "gui/layouthuts/layouthutpageactionsmin.xml"));
    }
}
