package com.metrogenesis.minecolonies.core.colony.buildings.moduleviews;

import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.client.gui.modules.building.WorkOrderModuleWindow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Client side version of the abstract class for building that handle workorders.
 */
public class WorkOrderListModuleView extends AbstractBuildingModuleView
{
    /**
     * The tool of the worker.
     */
    public WorkOrderListModuleView()
    {
        super();
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.metrogenesis.minecolonies.coremod.gui.townhall.workorders");
    }

    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BOWindow getWindow()
    {
        return new WorkOrderModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/info.png");
    }
}
