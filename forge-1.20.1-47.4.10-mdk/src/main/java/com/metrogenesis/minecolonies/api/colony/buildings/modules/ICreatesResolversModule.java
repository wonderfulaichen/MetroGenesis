package com.metrogenesis.minecolonies.api.colony.buildings.modules;

import com.metrogenesis.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;

import java.util.List;

/**
 * Interface for modules that creates resolvers.
 */
public interface ICreatesResolversModule extends IBuildingModule
{
    /**
     * Get a list of resolvers for this building.
     * @return the list of resolvers.
     */
    List<IRequestResolver<?>> createResolvers();
}
