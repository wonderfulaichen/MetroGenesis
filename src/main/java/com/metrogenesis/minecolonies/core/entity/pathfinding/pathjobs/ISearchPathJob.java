package com.metrogenesis.minecolonies.core.entity.pathfinding.pathjobs;

import com.metrogenesis.minecolonies.core.entity.pathfinding.MNode;

/**
 * Interface for area based search path jobs
 */
public interface ISearchPathJob
{
    public double getEndNodeScore(MNode n);
}
