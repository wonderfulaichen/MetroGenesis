package com.metrogenesis.minecolonies.core.client.gui;

import com.metrogenesis.blockui.controls.Button;
import com.metrogenesis.blockui.controls.ButtonHandler;
import com.metrogenesis.blockui.controls.TextField;
import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.buildings.views.IBuildingView;
import com.metrogenesis.minecolonies.api.util.MessageUtils;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import static com.metrogenesis.minecolonies.api.util.constant.TranslationConstants.WARNING_NAME_TOO_LONG;
import static com.metrogenesis.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for a hut name entry.
 */
public class WindowHutNameEntry extends BOWindow implements ButtonHandler
{
    /**
     * The max length of the name.
     */
    private static final int MAX_NAME_LENGTH = 15;

    /**
     * The building associated to the GUI.
     */
    private final IBuildingView building;

    /**
     * Constructor for a hut rename entry window.
     *
     * @param b {@link AbstractBuilding}
     */
    public WindowHutNameEntry(final IBuildingView b)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/windowhutnameentry.xml"));
        this.building = b;
    }

    @Override
    public void onOpened()
    {
        findPaneOfTypeByID(INPUT_NAME, TextField.class).setText(Component.translatable(building.getCustomName().toLowerCase(Locale.US)).getString());
    }

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        if (button.getID().equals(BUTTON_DONE))
        {
            String name = findPaneOfTypeByID(INPUT_NAME, TextField.class).getText();

            if (name.length() > MAX_NAME_LENGTH)
            {
                name = name.substring(0, MAX_NAME_LENGTH);
                MessageUtils.format(WARNING_NAME_TOO_LONG, name).sendTo(Minecraft.getInstance().player);
            }

            building.setCustomName(name);
        }
        else if (!button.getID().equals(BUTTON_CANCEL))
        {
            return;
        }

        if (building != null)
        {
            building.openGui(false);
        }
    }
}
