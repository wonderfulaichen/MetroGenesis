package com.metrogenesis.minecolonies.core.colony.buildings.moduleviews;

import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.client.gui.modules.building.ToolModuleWindow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Client side version of the abstract class for all buildings which allows to select tools.
 */
public class ToolModuleView extends AbstractBuildingModuleView
{
    /**
     * The worker specific tool.
     */
    private final Item tool;

    /**
     * The tool of the worker.
     * @param tool the item.
     */
    public ToolModuleView(final Item tool)
    {
        super();
        this.tool = tool;
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.metrogenesis.minecolonies.coremod.gui.workerhuts.tools");
    }

    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BOWindow getWindow()
    {
        return new ToolModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/scepter.png");
    }

    /**
     * Get the correct tool.
     * @return the tool to give.
     */
    public Item getTool()
    {
        return tool;
    }
}
