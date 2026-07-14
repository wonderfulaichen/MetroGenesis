package com.metrogenesis.minecolonies.core.colony.buildings.modules;

import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.IColonyView;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.IDefinesCoreBuildingStatsModule;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.stat.IStat;
import com.metrogenesis.minecolonies.core.client.gui.huts.WindowHutLiving;
import com.metrogenesis.minecolonies.core.colony.buildings.views.LivingBuildingView;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * The class of the citizen hut.
 */
public class HomeBuildingModule extends AbstractBuildingModule implements IDefinesCoreBuildingStatsModule
{
    @Override
    public IStat<Integer> getMaxInhabitants()
    {
        return (prev) -> Math.max(prev, building.getBuildingLevel());
    }

    /**
     * ClientSide representation of the building.
     */
    public static class View extends LivingBuildingView
    {
        /**
         * Creates an instance of the citizen hut window.
         *
         * @param c the colonyView.
         * @param l the position the hut is at.
         */
        public View(final IColonyView c, final BlockPos l)
        {
            super(c, l);
        }

        @NotNull
        @Override
        public BOWindow getWindow()
        {
            return new WindowHutLiving(this);
        }
    }
}
