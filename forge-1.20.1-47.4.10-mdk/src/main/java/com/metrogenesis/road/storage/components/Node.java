package com.metrogenesis.road.storage.components;

import net.minecraft.core.BlockPos;

/**
 * Represents a road network node.
 *
 * @param id   unique node identifier
 * @param pos  node position in the world
 * @param type node type identifier (e.g. "junction", "terminus", structure id)
 */
public record Node(String id, BlockPos pos, String type) {
}
