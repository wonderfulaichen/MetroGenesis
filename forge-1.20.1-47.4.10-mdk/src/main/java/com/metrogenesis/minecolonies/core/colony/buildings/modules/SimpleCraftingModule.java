package com.metrogenesis.minecolonies.core.colony.buildings.modules;

import com.metrogenesis.minecolonies.api.colony.jobs.IJob;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.JobEntry;
import com.metrogenesis.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.metrogenesis.minecolonies.api.crafting.ModCraftingTypes;
import com.metrogenesis.minecolonies.api.crafting.registry.CraftingType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This module is used for the buildings that only support simple
 * 2x2 crafting.  They don't have any restrictions on what they can
 * learn to craft beyond this, but they won't craft for the request
 * system either, only for themselves.
 */
public class SimpleCraftingModule extends AbstractCraftingBuildingModule.Crafting
{
    /**
     * Create a new module.
     */
    public SimpleCraftingModule(final JobEntry entry)
    {
        super(entry);
    }

    @Nullable
    @Override
    public IJob<?> getCraftingJob()
    {
        // the building may have a job, but it's not a dedicated crafting job.
        // this hides the building from JEI.
        return null;
    }

    @Override
    public List<IRequestResolver<?>> createResolvers()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean canLearnManyRecipes()
    {
        return false;
    }

    @Override
    public Set<CraftingType> getSupportedCraftingTypes()
    {
        return Set.of(ModCraftingTypes.SMALL_CRAFTING.get());
    }
}
