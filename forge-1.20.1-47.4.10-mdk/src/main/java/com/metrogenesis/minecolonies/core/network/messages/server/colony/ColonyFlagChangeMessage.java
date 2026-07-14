package com.metrogenesis.minecolonies.core.network.messages.server.colony;

import com.metrogenesis.minecolonies.api.IMinecoloniesAPI;
import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.eventbus.events.colony.ColonyFlagChangedModEvent;
import com.metrogenesis.minecolonies.api.util.constant.Constants;
import com.metrogenesis.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import static com.metrogenesis.minecolonies.api.util.constant.NbtTagConstants.TAG_BANNER_PATTERNS;

/**
 * Message to update the colony flag once set in the {@link com.metrogenesis.minecolonies.core.client.gui.WindowBannerPicker}.
 */
public class ColonyFlagChangeMessage extends AbstractColonyServerMessage
{
    /**
     * The chosen list of patterns from the window
     */
    private ListTag patterns;

    /**
     * Default constructor
     **/
    public ColonyFlagChangeMessage() {super();}

    /**
     * Spawn a new change message
     *
     * @param colony      the colony the player changed the banner in
     * @param patternList the list of patterns they set in the banner picker
     */
    public ColonyFlagChangeMessage(IColony colony, ListTag patternList)
    {
        super(colony);

        this.patterns = patternList;
    }

    @Override
    protected void onExecute(NetworkEvent.Context ctxIn, boolean isLogicalServer, IColony colony)
    {
        colony.setColonyFlag(patterns);
        IMinecoloniesAPI.getInstance().getEventBus().post(new ColonyFlagChangedModEvent(colony));
    }

    @Override
    protected void toBytesOverride(FriendlyByteBuf buf)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.put(TAG_BANNER_PATTERNS, this.patterns);
        buf.writeNbt(nbt);
    }

    @Override
    protected void fromBytesOverride(FriendlyByteBuf buf)
    {
        CompoundTag nbt = buf.readNbt();
        if (nbt != null)
        {
            this.patterns = nbt.getList(TAG_BANNER_PATTERNS, Constants.TAG_COMPOUND);
        }
    }
}
