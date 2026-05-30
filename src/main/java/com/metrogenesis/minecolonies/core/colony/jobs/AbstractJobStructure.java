package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.api.colony.buildings.IBuilding;
import com.metrogenesis.minecolonies.api.colony.workorders.IBuilderWorkOrder;
import com.metrogenesis.minecolonies.api.colony.workorders.IWorkOrder;
import com.metrogenesis.minecolonies.api.util.Log;
import com.metrogenesis.minecolonies.api.util.Utils;
import com.metrogenesis.minecolonies.api.util.constant.NbtTagConstants;
import com.metrogenesis.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.metrogenesis.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import static com.metrogenesis.structurize.blockentities.interfaces.IBlueprintDataProviderBE.TAG_BLUEPRINTDATA;
import static com.metrogenesis.structurize.blockentities.interfaces.IBlueprintDataProviderBE.TAG_SCHEMATIC_NAME;

/**
 * Common job object for all structure AIs.
 */
public abstract class AbstractJobStructure<AI extends AbstractAISkeleton<J>, J extends AbstractJobStructure<AI, J>> extends AbstractJob<AI, J>
{
    /**
     * Tag to store the workOrder id.
     */
    public static final String TAG_WORK_ORDER = "workorder";

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public AbstractJobStructure(final ICitizenData entity)
    {
        super(entity);
    }


    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);
        if (compound.contains(TAG_WORK_ORDER) && workBuilding instanceof AbstractBuildingStructureBuilder abstractBuildingStructureBuilder)
        {
            abstractBuildingStructureBuilder.setWorkOrderId(compound.getInt(TAG_WORK_ORDER));
        }
    }
}
