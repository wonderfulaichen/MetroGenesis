package com.metrogenesis.road.handlers;

import com.metrogenesis.util.DebugLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls execution of the road pipeline.
 *
 * <p>To integrate, call the suitable {@code onXxx(...)} methods from your
 * Forge event handlers.
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis/" + RoadPipelineController.class.getSimpleName());

    /** Worlds that have already had INIT run on their spawn chunk. */
    private static final Set<ResourceKey<Level>> INITIALIZED = ConcurrentHashMap.newKeySet();

    /** Cached structure selectors from config. */
    private static final Set<ResourceLocation> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();
    private static final Set<ResourceLocation> TARGET_DIMENSION_IDS = new HashSet<>();

    private static int tickCounter = 0;

    /** Pipeline interval config. Default: every 60 seconds. */
    private static int pipelineIntervalSeconds = 60;

    /** Default connection distance for road graph nodes. */
    private static double maxConnectionDistance = 100.0;

    /** Structure selectors: empty list means no automatic pipeline. */
    private static List<String> structureSelectors = List.of();

    /** Dimension selectors: empty means main world only. */
    private static List<String> dimensionSelectors = List.of();

    private RoadPipelineController() {
    }

    /**
     * Call once at server/mod start to cache selectors.
     */
    public static void init() {
        cacheStructureSelectors();
        tickCounter = 0;
        DebugLog.info(LOGGER, "RoadPipelineController initialized (selectors cached)");
    }

    /**
     * Configure the controller with settings.
     * Call from your mod's config load handler.
     */
    public static void configure(int intervalSeconds, double connectionDistance,
                                  List<String> structSelectors, List<String> dimSelectors) {
        pipelineIntervalSeconds = intervalSeconds;
        maxConnectionDistance = connectionDistance;
        structureSelectors = structSelectors;
        dimensionSelectors = dimSelectors;
        cacheStructureSelectors();
        DebugLog.info(LOGGER, "RoadPipelineController configured: interval={}s, maxDist={}",
                intervalSeconds, connectionDistance);
    }

    public static void refreshStructureSelectorCache() {
        cacheStructureSelectors();
        DebugLog.info(LOGGER, "RoadPipelineController reloaded selectors from config");
    }

    public static double getMaxConnectionDistance() {
        return maxConnectionDistance;
    }

    /* ───────────────────────── Trigger points ───────────────────────── */

    /**
     * 1) Spawn chunk generated for the first time → INIT pipeline.
     */
    public static void onSpawnChunkGenerated(ServerLevel world, ChunkAccess chunk) {
        if (!isDimensionEnabled(world.dimension())) return;

        ChunkPos spawnChunk = new ChunkPos(world.getSharedSpawnPos());
        if (!chunk.getPos().equals(spawnChunk)) return;

        if (INITIALIZED.add(world.dimension())) {
            DebugLog.info(LOGGER, "Spawn chunk {} generated in {}, starting INIT pipeline",
                    chunk.getPos(), world.dimension().location());
            PipelineRunner.runPipeline(world, world.getSharedSpawnPos(), PipelineRunner.PipelineMode.INIT);
        }
    }

    /**
     * 2) Any chunk generated; if it contains a target structure → CHUNK pipeline.
     */
    public static void onChunkGenerated(ServerLevel world, ChunkAccess chunk) {
        if (!isDimensionEnabled(world.dimension())) return;
        if (!containsTargetStructure(world, chunk)) return;

        BlockPos center = chunk.getPos().getMiddleBlockPosition(0);
        DebugLog.info(LOGGER, "Chunk {} generated with target structure, starting CHUNK pipeline", chunk.getPos());
        PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.CHUNK);
    }

    /**
     * 3) Player joins the server → PERIODIC pipeline.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        if (!isDimensionEnabled(world.dimension())) return;

        BlockPos pos = player.blockPosition();
        DebugLog.info(LOGGER, "Player {} joined at {}, starting PERIODIC pipeline",
                player.getName().getString(), pos);
        PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.PERIODIC);
    }

    /**
     * 4) Periodic trigger every N seconds (from config).
     */
    public static void onServerTick(MinecraftServer server) {
        int intervalTicks = Math.max(1, pipelineIntervalSeconds * 20);
        tickCounter++;
        if (tickCounter < intervalTicks) return;
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Level w = player.level();
            if (!isDimensionEnabled(w.dimension())) continue;

            BlockPos pos = player.blockPosition();
            DebugLog.info(LOGGER, "Periodic trigger at player {} pos {}, starting PERIODIC pipeline",
                    player.getName().getString(), pos);
            PipelineRunner.runPipeline((ServerLevel) w, pos, PipelineRunner.PipelineMode.PERIODIC);
        }
    }

    /**
     * 5) Server stopping — clear state.
     */
    public static void onServerStopping() {
        INITIALIZED.clear();
        tickCounter = 0;
        DebugLog.info(LOGGER, "Server stopping, state cleared");
    }

    /* ───────────────────────── Internal helpers ───────────────────────── */

    private static void cacheStructureSelectors() {
        TARGET_IDS.clear();
        TARGET_TAGS.clear();
        for (String sel : structureSelectors) {
            if (sel.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(sel.substring(1));
                if (tagId == null) {
                    LOGGER.warn("Skipping invalid structure tag selector '{}'", sel);
                    continue;
                }
                TARGET_TAGS.add(TagKey.create(Registries.STRUCTURE, tagId));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(sel);
                if (id == null) {
                    LOGGER.warn("Skipping invalid structure selector '{}'", sel);
                    continue;
                }
                TARGET_IDS.add(id);
            }
        }

        TARGET_DIMENSION_IDS.clear();
        if (dimensionSelectors == null || dimensionSelectors.isEmpty()) {
            TARGET_DIMENSION_IDS.add(Level.OVERWORLD.location());
        } else {
            for (String selector : dimensionSelectors) {
                if (selector.startsWith("#")) {
                    LOGGER.warn("Dimension selector tags are not supported (skipping '{}')", selector);
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(selector);
                if (id == null) {
                    LOGGER.warn("Skipping invalid dimension selector '{}'", selector);
                    continue;
                }
                TARGET_DIMENSION_IDS.add(id);
            }
            if (TARGET_DIMENSION_IDS.isEmpty()) {
                TARGET_DIMENSION_IDS.add(Level.OVERWORLD.location());
            }
        }
    }

    static boolean isDimensionEnabled(ResourceKey<Level> key) {
        if (TARGET_DIMENSION_IDS.isEmpty()) {
            return key == Level.OVERWORLD;
        }
        return TARGET_DIMENSION_IDS.contains(key.location());
    }

    private static boolean containsTargetStructure(ServerLevel world, ChunkAccess chunk) {
        if (!chunk.hasAnyStructureReferences()) return false;

        Registry<Structure> registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : chunk.getAllStarts().values()) {
            Structure structure = start.getStructure();
            ResourceLocation id = registry.getKey(structure);
            if (id != null && TARGET_IDS.contains(id)) return true;

            Holder<Structure> entry = registry.wrapAsHolder(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.is(tag)) return true;
                }
            }
        }
        return false;
    }
}
