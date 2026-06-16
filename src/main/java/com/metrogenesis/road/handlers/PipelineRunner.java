package com.metrogenesis.road.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Stub: placeholder for the pipeline execution logic.
 * Will be filled in future rounds with actual path finding and road building.
 */
public final class PipelineRunner {

    public enum PipelineMode {
        INIT,
        CHUNK,
        PERIODIC
    }

    private PipelineRunner() {
    }

    /**
     * Executes the road pipeline at the given position.
     * Currently a no-op stub — logs intent only.
     */
    public static void runPipeline(ServerLevel world, BlockPos origin, PipelineMode mode) {
        // TODO: implement actual road generation pipeline
        // 1. SCANNING_STRUCTURES: scan nearby chunks for target structures
        // 2. PATH_FINDING: run A* between detected nodes
        // 3. POST_PROCESSING: smooth paths, place road blocks
        // 4. COMPLETE: mark edges/paths as ready
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Object());
    }
}
