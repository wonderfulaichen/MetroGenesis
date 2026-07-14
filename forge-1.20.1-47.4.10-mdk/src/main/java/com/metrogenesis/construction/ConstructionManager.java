package com.metrogenesis.construction;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鏂藉伐宸ュ湴绠＄悊鍣?鈥?鍗曚緥锛岃拷韪墍鏈夋椿璺冨拰宸插畬鎴愮殑宸ュ湴
 * <p>
 * 鏁版嵁瀛樺偍鍦?ColonyState 鐨?SavedData 涓€? * 閫氳繃 {@link #get(ServerLevel)} 鑾峰彇瀵瑰簲涓栫晫鐨勫疄渚嬨€? */
public class ConstructionManager {

    private final Map<Integer, ConstructionSite> sites = new LinkedHashMap<>();
    private int nextOrderId = 1;
    private ServerLevel level; // 缁戝畾鐨勪笘鐣?
    public ConstructionManager(ServerLevel level) {
        this.level = level;
    }

    /**
     * 浠?ColonyState 鑾峰彇绠＄悊鍣ㄥ疄渚?     */
    public static ConstructionManager get(ServerLevel level) {
        return com.metrogenesis.colony.ColonyState.get(level).getConstructionManager();
    }

    // 鈹€鈹€ 鍒涘缓宸ュ湴 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public ConstructionSite createSite(BlockPos markerPos, String buildingTypeId, Zone zone) {
        BuildingType type = BuildingType.fromId(buildingTypeId);
        if (type == null) {
            MetroGenesis.LOGGER.warn("[Construction] Unknown building type: {}", buildingTypeId);
            return null;
        }
        ConstructionSite site = new ConstructionSite(markerPos, type, zone);
        sites.put(site.getId(), site);
        // 鏀剧疆鏂藉伐鍥存爮
        if (level != null) {
            site.placeTape(level);
        }
        MetroGenesis.LOGGER.info("[Construction] 鏂板伐鍦?#{} 鈥?{} at {} (zone: {}x{})",
                site.getId(), type.getDisplayName(),
                markerPos.toShortString(),
                zone.getMax().getX() - zone.getMin().getX(),
                zone.getMax().getZ() - zone.getMin().getZ());
        return site;
    }

    // 鈹€鈹€ 鏌ユ壘 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public ConstructionSite getSite(int id) { return sites.get(id); }

    public ConstructionSite getSiteByPos(BlockPos pos) {
        return sites.values().stream()
                .filter(s -> s.getMarkerPos().equals(pos))
                .findFirst().orElse(null);
    }

    public List<ConstructionSite> getActiveSites() {
        return sites.values().stream()
                .filter(s -> !s.isCompleted())
                .collect(Collectors.toList());
    }

    /** 返回全部工地（含已建成），供经济信号采集（R2）等用途。 */
    public List<ConstructionSite> getAllSites() {
        return new ArrayList<>(sites.values());
    }

    public List<ConstructionSite> getUnclaimedSites() {
        return sites.values().stream()
                .filter(ConstructionSite::needsBuilder)
                .collect(Collectors.toList());
    }

    // 鈹€鈹€ 绉婚櫎 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public void removeSite(int id) {
        sites.remove(id);
    }

    // 鈹€鈹€ tick 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** 姣?tick 璋冪敤锛岄┍鍔ㄦ墍鏈夋椿璺冨伐鍦扮殑鍥存爮绮掑瓙鍜屾棩蹇?*/
    public void tick() {
        if (level == null) return;
        for (ConstructionSite site : getActiveSites()) {
            site.tick(level);
        }
    }

    // 鈹€鈹€ 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ConstructionSite site : sites.values()) {
            list.add(site.save());
        }
        tag.put("Sites", list);
        tag.putInt("NextOrderId", nextOrderId);
        return tag;
    }

    public void load(CompoundTag tag) {
        sites.clear();
        ListTag list = tag.getList("Sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ConstructionSite site = ConstructionSite.load(list.getCompound(i));
            if (site != null) {
                sites.put(site.getId(), site);
            }
        }
        nextOrderId = tag.getInt("NextOrderId");
    }
}
