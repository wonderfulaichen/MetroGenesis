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
 * 鏂藉伐宸ュ湴 鈥?鐩稿綋浜?BuildCraft 鐨勯噰鐭虫満/Builder 鐨勫伐浣滅姸鎬? * <p>
 * 璁板綍涓€澶勬柦宸ラ」鐩細浣嶇疆銆佺洰鏍囧缓绛戠被鍨嬨€佽繘搴︺€佹寚娲剧殑寤洪€犲笀銆? */
public class ConstructionSite {

    private static int NEXT_ID = 1;

    private final int id;
    private final BlockPos markerPos;       // 鏂藉伐鏋舵柟鍧椾綅缃?
    private final BuildingType buildingType; // 瑕佸缓浠€涔?
    private final Zone zone;                // 鍥存爮鍖哄煙
    private int progress;                   // 0~100
    private UUID assignedBuilder;           // 鎸囨淳鐨勫缓閫犲笀 UUID
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
     * 鍒涘缓宸ュ湴骞舵斁缃洿鏍忥紙鍦?ConstructionManager.createSite 涔嬪悗璋冪敤锛?     */
    public void placeTape(ServerLevel level) {
        ConstructionTapeHelper.placeConstructionTape(zone, level);
    }

    // ══ tick锛堟瘡 tick 璋冪敤锛?═════════════════════════

    public void tick(ServerLevel level) {
        if (completed) return;

        // 鍥存爮绮掑瓙锛堟瘡 5 tick 鍒锋柊涓€娆★級
        zoneRenderTimer++;
        if (zoneRenderTimer >= 5) {
            zoneRenderTimer = 0;
            zone.renderFrame(level);
        }
    }

    // ══ 杩涘害 ═════════════════════════════════════════

    /** 寤洪€犲笀姣?tick 璋冪敤锛屽鍔犺繘搴?*/
    public void addProgress(int amount) {
        if (completed) return;
        this.progress = Math.min(100, this.progress + amount);
        MetroGenesis.LOGGER.debug("[Construction] Site #{} progress: {}%", id, progress);
    }

    /** 寤洪€犲畬鎴愶紒绉婚櫎鍥存爮銆佹浛鎹㈡柟鍧椼€佹縺娲昏鏂?*/
    public void complete(ServerLevel level) {
        if (completed) return;
        this.completed = true;

        // 绉婚櫎鏂藉伐鍥存爮
        ConstructionTapeHelper.removeConstructionTape(zone, level);

        MetroGenesis.LOGGER.info("[Construction] §a完成 §r工地 #{} — {}", id, buildingType.getDisplayName());

        // 閫氱煡鍦ㄧ嚎鐜╁鏂藉伐瀹屾垚
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(Component.literal(
                    "§a✔ §f" + buildingType.getDisplayName() + " §7建造完成 at " + markerPos.toShortString()));
        }

        // 鏇挎崲鏂藉伐鏋朵负璁炬柦鏂瑰潡
        BuildingType type = buildingType;
        String blockId = "metrogenesis:" + type.getId();
        level.destroyBlock(markerPos, false);
        var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.tryParse(blockId));
        if (block != null) {
            level.setBlock(markerPos, block.defaultBlockState(), 3);
            MetroGenesis.LOGGER.info("[Building] {} 设施已放置", buildingType.getDisplayName());

            // ══ 甯傛斂鍘呭缓鎴?鈫?鍒濆鍖栨畺姘戝湴 ══
            if ("town_hall".equals(buildingType.getId())) {
                ColonyState colony = ColonyState.get(level);
                colony.setTownHallPos(markerPos);
                MetroGenesis.LOGGER.info("[Colony] 市政厅落成—初始化殖民地 at {}",
                        markerPos.toShortString());

                // 閫氱煡鎵€鏈夊湪绾跨帺瀹?
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal("§6✔ §e市政厅落成！§6 ✔"));
                    p.sendSystemMessage(Component.literal("§7城邦 §e" + colony.getCityName() + " §7正式建立"));
                }

                // 鍒峰嚭鍒濆甯傛皯
                for (int i = 0; i < 3; i++) {
                    var mgr = colony.getCitizenManager();
                    if (mgr.getCurrentCitizenCount() < 3) {
                        var data = mgr.createAndRegisterCivilianData();
                        mgr.spawnEntity(data, level, markerPos.offset(
                                (i % 2 == 0 ? -2 : 2), 0, (i < 2 ? -2 : 2)));
                    }
                }

                // 甯傛皯鎶佃揪閫氱煡
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal(
                            "§a3 位市民已抵达城邦！右键市民查看状态"));
                }
            }

            // ══ 闈炲競鏀垮巺寤虹瓚寤烘垚 鈫?鐪嬬被鍨嬪鐞?══
            ColonyState colony = ColonyState.get(level);

            if (buildingType.isHouse()) {
                // 浣忓畢 鈫?澧炲姞浜哄彛瀹归噺 + 鍒锋柊甯傛皯
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
                // 鍔熻兘鎬у缓绛?鈫?灏濊瘯缁欑┖闂插競姘戝垎閰嶅搴斿伐浣?
                ColonyState.get(level).getCitizenManager().assignNearbyUnemployed(
                        level, markerPos, buildingType.getId());
            }
        }
    }

    public boolean needsBuilder() {
        return assignedBuilder == null && !completed;
    }

    // ══ 搴忓垪鍖?═══════════════════════════════════════

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
