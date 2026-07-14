package com.metrogenesis.minecolonies.api.colony.buildingextensions.modules;

import com.metrogenesis.minecolonies.api.colony.buildingextensions.IBuildingExtension;

/**
 * Default interface for all building extension modules.
 */
public interface IBuildingExtensionModule
{
    /**
     * Get the building extension of the module.
     *
     * @return the building extension instance.
     */
    IBuildingExtension getBuildingExtension();
}
