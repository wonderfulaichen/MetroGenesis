package com.metrogenesis.minecolonies.core.client.gui.townhall;

import com.metrogenesis.blockui.controls.Button;
import com.metrogenesis.blockui.controls.ButtonHandler;
import com.metrogenesis.blockui.controls.TextField;
import com.metrogenesis.blockui.views.BOWindow;
import com.metrogenesis.minecolonies.api.colony.IColonyView;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.colony.ColonyView;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static com.metrogenesis.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for a town hall name entry.
 */
public class WindowTownHallNameEntry extends BOWindow implements ButtonHandler
{
    private final IColonyView colony;

    /**
     * Constructor for a town hall rename entry window.
     *
     * @param c {@link ColonyView}
     */
    public WindowTownHallNameEntry(final IColonyView c)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/townhall/windowtownhallnameentry.xml"));
        this.colony = c;
    }

    @Override
    public void onOpened()
    {
        findPaneOfTypeByID(INPUT_NAME, TextField.class).setText(Component.translatable(colony.getName().toLowerCase(Locale.US)).getString());
    }

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        if (button.getID().equals(BUTTON_DONE))
        {
            final String name = findPaneOfTypeByID(INPUT_NAME, TextField.class).getText();
            if (!name.isEmpty())
            {
                colony.setName(name);
            }
        }
        else if (!button.getID().equals(BUTTON_CANCEL))
        {
            return;
        }

        if (colony.getClientBuildingManager().getTownHall() != null)
        {
            colony.getClientBuildingManager().getTownHall().openGui(false);
        }
    }
}
