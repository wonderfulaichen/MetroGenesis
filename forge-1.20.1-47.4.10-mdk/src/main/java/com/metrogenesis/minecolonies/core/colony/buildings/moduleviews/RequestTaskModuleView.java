package com.metrogenesis.minecolonies.core.colony.buildings.moduleviews;

import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.metrogenesis.minecolonies.api.colony.requestsystem.token.IToken;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.client.gui.modules.building.WindowHutRequestTaskModule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Request task module to display tasks in the UI.
 */
public abstract class RequestTaskModuleView extends AbstractBuildingModuleView
{
    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new WindowHutRequestTaskModule(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/info.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.metrogenesis.minecolonies.coremod.gui.workerhuts.crafter.tasks");
    }

    /**
     * Get the specific task list.
     * @return the task list.
     */
    public abstract List<IToken<?>> getTasks();
}
