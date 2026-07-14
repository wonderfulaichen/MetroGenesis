package com.metrogenesis.workorder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

/**
 * 寤洪€犲伐鍗?鈥?鎸囧悜涓€涓?ConstructionSite
 * <p>
 * 鍙傝€?MineColonies WorkOrderBuilding锛? * - 涓€涓伐鍗?= 涓€涓缓閫犱换鍔? * - 琚?Builder AI 璁ら鍜屾墽琛? * - 瀹屾垚鍚庝粠绠＄悊鍣ㄧЩ闄? */
public class BuildOrder {

    private final int id;
    private final int siteId;
    private final BlockPos sitePos;
    private final String buildingTypeId;
    private UUID claimedBy;
    private boolean completed;

    public BuildOrder(int id, int siteId, BlockPos sitePos, String buildingTypeId) {
        this.id = id;
        this.siteId = siteId;
        this.sitePos = sitePos;
        this.buildingTypeId = buildingTypeId;
        this.claimedBy = null;
        this.completed = false;
    }

    // 鈹€鈹€ getter 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public int getId() { return id; }
    public int getSiteId() { return siteId; }
    public BlockPos getSitePos() { return sitePos; }
    public String getBuildingTypeId() { return buildingTypeId; }
    public UUID getClaimedBy() { return claimedBy; }
    public boolean isClaimed() { return claimedBy != null; }
    public boolean isCompleted() { return completed; }

    public void setClaimedBy(UUID uuid) { this.claimedBy = uuid; }
    public void setCompleted(boolean c) { this.completed = c; }

    // 鈹€鈹€ 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putInt("siteId", siteId);
        tag.put("sitePos", NbtUtils.writeBlockPos(sitePos));
        tag.putString("buildingType", buildingTypeId);
        if (claimedBy != null) tag.putUUID("claimedBy", claimedBy);
        tag.putBoolean("completed", completed);
        return tag;
    }

    public static BuildOrder load(CompoundTag tag) {
        BuildOrder order = new BuildOrder(
                tag.getInt("id"),
                tag.getInt("siteId"),
                NbtUtils.readBlockPos(tag.getCompound("sitePos")),
                tag.getString("buildingType"));
        if (tag.contains("claimedBy")) order.claimedBy = tag.getUUID("claimedBy");
        order.completed = tag.getBoolean("completed");
        return order;
    }
}
