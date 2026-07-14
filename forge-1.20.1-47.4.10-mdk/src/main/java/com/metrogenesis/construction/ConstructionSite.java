package com.metrogenesis.construction;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 // BuildCraft /Builder
 // */
public class ConstructionSite {

    private static int NEXT_ID = 1;

    private final int id;
    private final BlockPos markerPos;       // //
    private final BuildingType buildingType; // //
    private final Zone zone;                // Zone
    private int progress;                   // 0~100
    private UUID assignedBuilder;           // Assigned builder UUID
    private boolean completed;
    private int zoneRenderTimer;            // 鍥存爮绮掑瓙娓叉煋璁℃椂

    public ConstructionSite(BlockPos markerPos, BuildingType buildingType, Zone zone) {
        this.id = NEXT_ID++;
        this.markerPos = markerPos;
        this.buildingType = buildingType;
        this.zone = zone;
        this.progress = 0;
        this.assignedBuilder = null;
        this.completed = false;
        this.zoneRenderTimer = 0;
    }

    /**
     // ConstructionManager.createSite */
    public void placeTape(ServerLevel level) {
        ConstructionTapeHelper.placeConstructionTape(zone, level);
    }

    // ══ tick锛堟瘡 tick 璋冪敤锛?═════════════════════════

    public void tick(ServerLevel level) {
        if (completed) return;

        // // tick
        zoneRenderTimer++;
        if (zoneRenderTimer >= 5) {
            zoneRenderTimer = 0;
            zone.renderFrame(level);
        }
    }

    // ══ 杩涘害 ═════════════════════════════════════════

    // /** tick */
    public void addProgress(int amount) {
        if (completed) return;
        this.progress = Math.min(100, this.progress + amount);
        MetroGenesis.LOGGER.debug("[Construction] Site #{} progress: {}%", id, progress);
    }

    // /** */
    public void complete(ServerLevel level) {
        if (completed) return;
        this.completed = true;

        // //
        ConstructionTapeHelper.removeConstructionTape(zone, level);

        MetroGenesis.LOGGER.info("[Construction] §a完成 §r工地 #{} — {}", id, buildingType.getDisplayName());

        // //
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(Component.literal(
                    "§a✔ §f" + buildingType.getDisplayName() + " §7建造完成 at " + markerPos.toShortString()));
        }

        // //
        BuildingType type = buildingType;
        String blockId = "metrogenesis:" + type.getId();
        level.destroyBlock(markerPos, false);
        var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.tryParse(blockId));
        if (block != null) {
            level.setBlock(markerPos, block.defaultBlockState(), 3);
            MetroGenesis.LOGGER.info("[Building] {} 设施已放置", buildingType.getDisplayName());

            // //
            if ("town_hall".equals(buildingType.getId())) {
                ColonyState colony = ColonyState.get(level);
                colony.setTownHallPos(markerPos);
                MetroGenesis.LOGGER.info("[Colony] 市政厅落成—初始化殖民地 at {}",
                        markerPos.toShortString());

                // //
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal("§6✔ §e市政厅落成！§6 ✔"));
                    p.sendSystemMessage(Component.literal("§7城邦 §e" + colony.getCityName() + " §7正式建立"));
                }

                // //
                for (int i = 0; i < 3; i++) {
                    var mgr = colony.getCitizenManager();
                    if (mgr.getCurrentCitizenCount() < 3) {
                        var data = mgr.createAndRegisterCivilianData();
                        mgr.spawnEntity(data, level, markerPos.offset(
                                (i % 2 == 0 ? -2 : 2), 0, (i < 2 ? -2 : 2)));
                    }
                }

                // //
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal(
                            "§a3 位市民已抵达城邦！右键市民查看状态"));
                }
            }

            // //
            ColonyState colony = ColonyState.get(level);

            if (buildingType.isHouse()) {
                // //
                colony.increaseCapacity();
                int currentPop = colony.getCitizenManager().getCurrentCitizenCount();
                if (currentPop < colony.getMaxPopulation()) {
                    var data = colony.getCitizenManager().createAndRegisterCivilianData();
                    colony.getCitizenManager().spawnEntity(data, level,
                            markerPos.offset(3, 0, 3));
                    for (ServerPlayer p : level.players()) {
                        p.sendSystemMessage(Component.literal(
                                "§a新市民 §f" + data.getName() + " §7已入住！("
                                + (currentPop + 1) + "/" + colony.getMaxPopulation() + ")"));
                    }
                }
            } else if (!"town_hall".equals(buildingType.getId())) {
                // //
                ColonyState.get(level).getCitizenManager().assignNearbyUnemployed(
                        level, markerPos, buildingType.getId());
            }
        }
    }

    public boolean needsBuilder() {
        return assignedBuilder == null && !completed;
    }

    // ══ Serialization ════════════════════════════════════════

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.put("markerPos", NbtUtils.writeBlockPos(markerPos));
        tag.putString("buildingType", buildingType.getId());
        tag.put("zone", zone.save());
        tag.putInt("progress", progress);
        if (assignedBuilder != null) tag.putUUID("builder", assignedBuilder);
        tag.putBoolean("completed", completed);
        return tag;
    }

    public static ConstructionSite load(CompoundTag tag) {
        BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("markerPos"));
        BuildingType type = BuildingType.fromId(tag.getString("buildingType"));
        if (type == null) {
            return null;
        }
        Zone zone = Zone.load(tag.getCompound("zone"));
        ConstructionSite site = new ConstructionSite(pos, type, zone);
        site.progress = tag.getInt("progress");
        if (tag.contains("builder")) site.assignedBuilder = tag.getUUID("builder");
        site.completed = tag.getBoolean("completed");
        return site;
    }

    // ══ getter ═══════════════════════════════════════

    public int getId() { return id; }
    public BlockPos getMarkerPos() { return markerPos; }
    public BuildingType getBuildingType() { return buildingType; }
    public Zone getZone() { return zone; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public UUID getAssignedBuilder() { return assignedBuilder; }

    public void setAssignedBuilder(UUID builder) {
        this.assignedBuilder = builder;
    }
}
