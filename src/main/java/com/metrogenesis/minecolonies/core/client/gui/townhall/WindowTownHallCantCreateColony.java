package com.metrogenesis.minecolonies.core.client.gui.townhall;

import com.metrogenesis.blockui.PaneBuilders;
import com.metrogenesis.blockui.controls.Text;
import com.metrogenesis.minecolonies.core.Network;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.metrogenesis.minecolonies.core.network.messages.server.PickupBlockMessage;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import static com.metrogenesis.minecolonies.api.util.constant.WindowConstants.*;

/**
 *  UI to notify the player that a colony can't be created here.
 */
public class WindowTownHallCantCreateColony extends AbstractWindowSkeleton
{
    /**
     * Townhall position
     */
    private BlockPos pos;

    public WindowTownHallCantCreateColony(final BlockPos pos, final MutableComponent warningMsg, final boolean displayConfigTooltip)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/townhall/windowcantfoundcolony.xml"));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        this.pos = pos;
        registerButton(BUTTON_CANCEL, this::close);
        registerButton(BUTTON_PICKUP_BUILDING, this::pickup);
        final Text text = this.findPaneOfTypeByID("text1", Text.class);
        text.setText(warningMsg);
        if (displayConfigTooltip)
        {
            PaneBuilders.singleLineTooltip(Component.translatable("com.metrogenesis.minecolonies.core.configsetting"), text);
        }
    }

    /**
     * When the pickup building button was clicked.
     */
    private void pickup()
    {
        Network.getNetwork().sendToServer(new PickupBlockMessage(pos));
        close();
    }
}
