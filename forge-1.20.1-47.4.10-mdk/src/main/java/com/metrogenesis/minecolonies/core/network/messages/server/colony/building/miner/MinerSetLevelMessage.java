package com.metrogenesis.minecolonies.core.network.messages.server.colony.building.miner;

import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.colony.buildings.views.IBuildingView;
import com.metrogenesis.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.metrogenesis.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Message to set the level of the miner from the GUI.
 */
public class MinerSetLevelMessage extends AbstractBuildingServerMessage<BuildingMiner>
{
    private int level;

    /**
     * Empty constructor used when registering the
     */
    public MinerSetLevelMessage()
    {
        super();
    }

    /**
     * Creates object for the miner set level
     *
     * @param building View of the building to read data from.
     * @param level    Level of the miner.
     */
    public MinerSetLevelMessage(@NotNull final IBuildingView building, final int level)
    {
        super(building);
        this.level = level;
    }

    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        level = buf.readInt();
    }

    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(level);
    }

    @Override
    protected void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final BuildingMiner building)
    {
        building.getModule(BuildingModules.MINER_LEVELS).setCurrentLevel(level);
    }
}
