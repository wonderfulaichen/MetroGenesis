package com.metrogenesis.minecolonies.core.network.messages.server.colony.building.builder;

import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.colony.buildings.views.IBuildingView;
import com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.metrogenesis.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class BuilderSelectWorkOrderMessage extends AbstractBuildingServerMessage<BuildingBuilder>
{
    private int workOrder;

    /**
     * Empty standard constructor.
     */
    public BuilderSelectWorkOrderMessage()
    {
        super();
    }

    /**
     * Creates a new BuilderSetManualModeMessage.
     *
     * @param building View of the building to read data from.
     * @param workOrder workorder id.
     */
    public BuilderSelectWorkOrderMessage(@NotNull final IBuildingView building, final int workOrder)
    {
        super(building);
        this.workOrder = workOrder;
    }

    @Override
    public void fromBytesOverride(final FriendlyByteBuf buf)
    {
        workOrder = buf.readInt();
    }

    @Override
    public void toBytesOverride(final FriendlyByteBuf buf)
    {
        buf.writeInt(workOrder);
    }

    /**
     * Executes the action of setting a workorder on the builder.
     *
     * @param ctxIn            NetworkEvent.Context of the packet.
     * @param isLogicalServer  Whether the server is logical.
     * @param colony           Colony the building is in.
     * @param building         The builder to set the workorder on.
     */
    @Override
    protected void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final BuildingBuilder building)
    {
        building.setWorkOrder(workOrder, ctxIn);
    }
}
