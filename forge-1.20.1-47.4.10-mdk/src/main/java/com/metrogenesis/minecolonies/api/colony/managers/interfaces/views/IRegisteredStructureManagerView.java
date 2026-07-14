package com.metrogenesis.minecolonies.api.colony.managers.interfaces.views;

import com.metrogenesis.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.metrogenesis.minecolonies.api.colony.buildings.views.IBuildingView;
import com.metrogenesis.minecolonies.api.colony.buildings.workerbuildings.ITownHallView;
import com.metrogenesis.minecolonies.api.colony.managers.interfaces.ICommonRegisteredStructureManager;
import com.metrogenesis.minecolonies.api.network.IMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface IRegisteredStructureManagerView extends ICommonRegisteredStructureManager<IBuildingView, ITownHallView>
{
    /**
     * Remove a building from the ColonyView.
     *
     * @param buildingId location of the building.
     * @return null == no response.
     */
    @Nullable IMessage handleColonyViewRemoveBuildingMessage(BlockPos buildingId);

    /**
     * Update a ColonyView's buildings given a network data ColonyView update packet. This uses a full-replacement - buildings do not get updated and are instead overwritten.
     *
     * @param buildingId location of the building.
     * @param buf        buffer containing ColonyBuilding information.
     * @return null == no response.
     */
    @Nullable IMessage handleColonyBuildingViewMessage(BlockPos buildingId, @NotNull FriendlyByteBuf buf);

    void handleColonyBuildingExtensionViewUpdateMessage(Set<IBuildingExtension> extensions);

    void deserializeFromView(boolean isNewSubscription, @NotNull FriendlyByteBuf buf);
}
