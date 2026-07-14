package com.metrogenesis.workorder;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 宸ュ崟绠＄悊鍣?鈥?绠＄悊鎵€鏈夊缓閫犲伐鍗? * <p>
 * 鍙傝€?MineColonies WorkManager + BuildCraft 浠诲姟闃熷垪妯″紡銆? * 姣忎釜宸ュ崟鎸囧悜涓€涓?ConstructionSite锛岀敱 Builder AI 璁ら鍜屾墽琛屻€? */
public class WorkManager {

    private final Map<Integer, BuildOrder> orders = new LinkedHashMap<>();
    private int nextId = 1;

    // 鈹€鈹€ 鍒涘缓宸ュ崟 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public BuildOrder createOrder(int siteId, BlockPos sitePos, String buildingTypeId) {
        BuildOrder order = new BuildOrder(nextId++, siteId, sitePos, buildingTypeId);
        orders.put(order.getId(), order);
        MetroGenesis.LOGGER.info("[WorkOrder] 宸ュ崟 #{} 鍒涘缓 鈥?{} at {} (site #{})",
                order.getId(), buildingTypeId, sitePos.toShortString(), siteId);
        return order;
    }

    // 鈹€鈹€ 鏌ユ壘 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public BuildOrder getOrder(int id) { return orders.get(id); }

    public List<BuildOrder> getUnclaimedOrders() {
        return orders.values().stream()
                .filter(o -> !o.isClaimed())
                .collect(Collectors.toList());
    }

    public Optional<BuildOrder> getUnclaimedOrder() {
        return getUnclaimedOrders().stream().findFirst();
    }

    public void claimOrder(int orderId, UUID builderId) {
        BuildOrder order = orders.get(orderId);
        if (order != null && !order.isClaimed()) {
            order.setClaimedBy(builderId);
            MetroGenesis.LOGGER.info("[WorkOrder] 宸ュ崟 #{} 琚缓閫犲笀 {} 璁ら", orderId,
                    builderId.toString().substring(0, 8));
        }
    }

    public void completeOrder(int orderId) {
        BuildOrder order = orders.get(orderId);
        if (order != null) {
            order.setCompleted(true);
            MetroGenesis.LOGGER.info("[WorkOrder] 宸ュ崟 #{} 搂a瀹屾垚", orderId);
        }
    }

    public void removeOrder(int orderId) {
        orders.remove(orderId);
    }

    // 鈹€鈹€ 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BuildOrder order : orders.values()) {
            list.add(order.save());
        }
        tag.put("Orders", list);
        tag.putInt("NextId", nextId);
        return tag;
    }

    public void load(CompoundTag tag) {
        orders.clear();
        ListTag list = tag.getList("Orders", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            BuildOrder order = BuildOrder.load(list.getCompound(i));
            orders.put(order.getId(), order);
        }
        nextId = tag.getInt("NextId");
    }
}
