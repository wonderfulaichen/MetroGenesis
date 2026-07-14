package com.metrogenesis.minecolonies.core.colony.buildings.workerbuildings;

import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.equipment.ModEquipmentTypes;
import com.metrogenesis.minecolonies.api.util.ItemStackUtils;
import com.metrogenesis.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import static com.metrogenesis.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;

/**
 * The fishermans building.
 */
public class BuildingFisherman extends AbstractBuilding
{
    /**
     * The maximum upgrade of the building.
     */
    private static final int    MAX_BUILDING_LEVEL = 5;
    /**
     * The job description.
     */
    private static final String FISHERMAN = "fisherman";

    /**
     * Public constructor of the building, creates an object of the building.
     *
     * @param c the colony.
     * @param l the position.
     */
    public BuildingFisherman(final IColony c, final BlockPos l)
    {
        super(c, l);
        keepX.put(itemStack -> ItemStackUtils.hasEquipmentLevel(itemStack, ModEquipmentTypes.fishing_rod.get(), TOOL_LEVEL_WOOD_OR_GOLD, getMaxEquipmentLevel()), new Tuple<>(1, true));
    }

    /**
     * Getter of the schematic name.
     *
     * @return the schematic name.
     */
    @NotNull
    @Override
    public String getSchematicName()
    {
        return FISHERMAN;
    }

    /**
     * Getter of the max building level.
     *
     * @return the integer.
     */
    @Override
    public int getMaxBuildingLevel()
    {
        return MAX_BUILDING_LEVEL;
    }
}
