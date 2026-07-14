package com.metrogenesis.minecolonies.api.entity.pathfinding.registry;

import com.metrogenesis.minecolonies.api.IMinecoloniesAPI;
import com.metrogenesis.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;
import java.util.function.Predicate;

public interface IPathNavigateRegistry
{

    static IPathNavigateRegistry getInstance()
    {
        return IMinecoloniesAPI.getInstance().getPathNavigateRegistry();
    }

    IPathNavigateRegistry registerNewPathNavigate(Predicate<Mob> selectionPredicate, Function<Mob, AbstractAdvancedPathNavigate> navigateProducer);

    AbstractAdvancedPathNavigate getNavigateFor(Mob entityLiving);
}
