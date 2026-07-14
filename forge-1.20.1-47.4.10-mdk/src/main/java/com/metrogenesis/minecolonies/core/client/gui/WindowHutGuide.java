package com.metrogenesis.minecolonies.core.client.gui;

import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.client.gui.huts.WindowHutBuilderModule;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.resources.ResourceLocation;

import static com.metrogenesis.minecolonies.api.util.constant.WindowConstants.GUIDE_CLOSE;
import static com.metrogenesis.minecolonies.api.util.constant.WindowConstants.GUIDE_CONFIRM;

/**
 * BOWindow for the builder hut.
 */
public class WindowHutGuide extends AbstractWindowSkeleton
{
    public static final ResourceLocation WINDOW_ID = new ResourceLocation(Constants.MOD_ID, "gui/windowhutguide.xml");

    /**
     * Color constants for builder list.
     */
    private final BuildingBuilder.View building;

    /**
     * Constructor for window builder hut.
     *
     * @param building {@link BuildingBuilder.View}.
     */
    public WindowHutGuide(final BuildingBuilder.View building)
    {
        super(WINDOW_ID);
        this.building = building;

        registerButton(GUIDE_CONFIRM, this::closeGuide);
        registerButton(GUIDE_CLOSE, this::closeGuide);
    }

    private void closeGuide()
    {
        close();
        new WindowHutBuilderModule(building, false).open();
    }
}
