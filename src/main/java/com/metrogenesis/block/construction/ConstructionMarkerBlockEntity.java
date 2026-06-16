package com.metrogenesis.block.construction;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.gui.ConstructionMarkerMenu;
import com.metrogenesis.hologram.HologramRenderer;
import com.metrogenesis.hologram.MetroGenesisHologramMod;
import com.metrogenesis.init.BuildingType;
import com.metrogenesis.blueprint.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 鏂藉伐鏂瑰潡瀹炰綋 鈥?鏀寔閫愬潡寤洪€?+ 鑷畾涔夎摑鍥惧姞杞? * <p>
 * 钃濆浘鏉ユ簮锛? * <ol>
 *   <li>鍐呯疆妯℃澘锛團arm/Hall/House锛夆€?BuildingType 鑷姩鐢熸垚</li>
 *   <li>鎵弿宸ュ叿 ScanToolItem 鈥?鎵弿宸叉湁寤虹瓚鐢熸垚</li>
 * </ol>
 */
public class ConstructionMarkerBlockEntity extends BlockEntity implements MenuProvider {

    // 鈹€鈹€ 瀛樺偍鏁版嵁 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private String buildingTypeId = "";
    private int progress = 0;
    private boolean completed = false;
    private String assignedBuilder = "";
    private Zone zone;

    /** 璁炬柦鏂瑰潡浣嶇疆锛堝缓绛戜腑蹇冿級锛岄粯璁?鏂藉伐鏍囪鑷韩浣嶇疆 */
    private BlockPos buildingPos = BlockPos.ZERO;

    // 鈹€鈹€ 閫愬潡寤洪€?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private com.metrogenesis.blueprint.v1.Blueprint blueprint;
    private com.metrogenesis.blueprint.v1.BlueprintIterator iterator;
    private int blocksTotal = 0;
    private int blocksPlaced = 0;

    // 鈹€鈹€ 钃濆浘鏉ユ簮璁板綍 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private String blueprintName = ""; // 鑷畾涔夎摑鍥惧悕绉帮紙鎵弿鏉ョ殑锛?
    // 鈹€鈹€ 瀹㈡埛绔悓姝?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private final ContainerData dataAccess = new SimpleContainerData(4) {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> completed ? 1 : 0;
                case 2 -> blocksTotal;
                case 3 -> blocksPlaced;
                default -> 0;
            };
        }
        @Override public int getCount() { return 4; }
    };

    // 鈹€鈹€ 鏋勯€?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public ConstructionMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(MetroGenesis.CONSTRUCTION_MARKER_BE.get(), pos, state);
    }

    // 鈹€鈹€ tick 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!completed && zone != null) {
            BuildingType type = BuildingType.fromId(buildingTypeId);
            if (type != null) {
                // 鍏ㄦ伅鎶曞奖娓叉煋鍦ㄨ鏂戒綅缃紙buildingPos锛夛紝鑰岄潪濉斿悐浣嶇疆
                BlockPos renderPos = hasBuildingPos() ? buildingPos : worldPosition;
                MetroGenesisHologramMod.activateHologram(renderPos, zone, type);
            }
        }
    }

    /**
     * 鍒濆鍖栧缓绛?鈥?鐢ㄤ簬璁炬柦宸叉斁缃紝濉斿悐鍦ㄦ梺杈圭殑鍦烘櫙
     *
     * @param typeId   寤虹瓚绫诲瀷 ID
     * @param facilityPos 璁炬柦鏂瑰潡浣嶇疆
     * @param radius   鍖哄煙鍗婂緞
     */
    public void startConstructionWithFacility(String typeId, BlockPos facilityPos, int radius) {
        this.buildingTypeId = typeId;
        this.buildingPos = facilityPos;
        this.progress = 0;
        this.completed = false;
        this.blocksTotal = 0;
        this.blocksPlaced = 0;

        BuildingType type = BuildingType.fromId(typeId);

        // 鍖哄煙浠ヨ鏂戒綅缃负涓績
        this.zone = new Zone(facilityPos, radius);

        if (type != null) {
            this.blueprint = HologramRenderer.createBlueprint(zone, type);
            this.iterator = new com.metrogenesis.blueprint.v1.BlueprintIterator(blueprint);
            for (var info : blueprint.getBlockInfoAsList()) {
                if (info.state() != null && !info.state().isAir()) blocksTotal++;
            }
        }

        // 鎵ｆ垚鏈?
        int cost = type != null ? type.getBuildTime() / 2 : 50;
        if (level instanceof ServerLevel sl) {
            ColonyState colony = ColonyState.get(sl);
            if (!colony.spend(cost)) {
                MetroGenesis.LOGGER.warn("[Construction] 国库不足 {}，投影仍可显示", typeId);
            }
        }

        setChanged();
    }

    // 鈹€鈹€ 鍔犺浇钃濆浘 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 浠庢壂鎻忓伐鍏峰姞杞借嚜瀹氫箟钃濆浘
     *
     * @param bp   鎵弿鏉ョ殑钃濆浘
     * @param name 钃濆浘鍚嶇О
     */
    public void loadBlueprint(com.metrogenesis.blueprint.v1.Blueprint bp, String name) {
        if (level == null) return;

        this.buildingTypeId = "custom_" + name;
        this.blueprint = bp;
        this.iterator = new com.metrogenesis.blueprint.v1.BlueprintIterator(bp);
        this.blueprintName = name;

        // 缁熻鏂瑰潡
        this.blocksTotal = 0;
        this.blocksPlaced = 0;
        this.progress = 0;
        this.completed = false;

        for (var info : bp.getBlockInfoAsList()) {
            if (info.state() != null && !info.state().isAir()) {
                blocksTotal++;
            }
        }

        // 鍒涘缓鍥存爮鍖哄煙锛堟寜钃濆浘灏哄锛?        this.zone = new Zone(worldPosition, Math.max(bp.getSizeX(), bp.getSizeZ()) / 2 + 1);

        // 鎵ｆ垚鏈?
        int cost = Math.max(blocksTotal, 50);
        if (level instanceof ServerLevel sl) {
            ColonyState colony = ColonyState.get(sl);
            if (!colony.spend(cost)) {
                MetroGenesis.LOGGER.warn("[Blueprint] 鍥藉簱涓嶈冻锛屾棤娉曞姞杞借摑鍥?{} ({})", name, colony.getFunds());
                return;
            }
            MetroGenesis.LOGGER.info("[Blueprint] 鍔犺浇钃濆浘 '{}' 鈥?{} 鏂瑰潡锛屾垚鏈?{} C-Value", name, blocksTotal, cost);
        }

        setChanged();
    }

    /**
     * 鍒濆鍖栧缓绛戠被鍨嬶紙鍐呯疆妯℃澘锛?     * <p>
     * 娉ㄦ剰锛氬厛璁剧疆 zone/blueprint 鍐嶆墸閽憋紝纭繚鍏ㄦ伅娓叉煋鏉′欢濮嬬粓婊¤冻銆?     */
    public void startConstruction(String typeId) {
        this.buildingTypeId = typeId;
        this.progress = 0;
        this.completed = false;
        this.blocksTotal = 0;
        this.blocksPlaced = 0;

        BuildingType type = BuildingType.fromId(typeId);

        // 鍏堣缃?zone 鍜?blueprint锛岃鍏ㄦ伅鎶曞奖绔嬪嵆鍙
        int radius = type != null ? type.getZoneRadius() : 3;
        this.zone = new Zone(worldPosition, radius);

        if (type != null) {
            this.blueprint = HologramRenderer.createBlueprint(zone, type);
            this.iterator = new com.metrogenesis.blueprint.v1.BlueprintIterator(blueprint);
            for (var info : blueprint.getBlockInfoAsList()) {
                if (info.state() != null && !info.state().isAir()) blocksTotal++;
            }
        }

        // 鍐嶆墸鎴愭湰锛堝け璐ヤ篃涓嶅奖鍝嶆姇褰辨樉绀猴級
        int cost = type != null ? type.getBuildTime() / 2 : 50;
        if (level instanceof ServerLevel sl) {
            ColonyState colony = ColonyState.get(sl);
            if (!colony.spend(cost)) {
                MetroGenesis.LOGGER.warn("[Construction] 国库不足 {}，投影仍可显示", typeId);
            }
        }

        setChanged();
    }

    // 鈹€鈹€ 杩唬鍣ㄨ闂?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Nullable
    public com.metrogenesis.blueprint.v1.BlueprintIterator.PlacedBlock getNextBlock() {
        if (iterator == null) return null;
        if (iterator.isFinished()) {
            complete();
            return null;
        }

        while (true) {
            var next = iterator.next();
            if (next == null) {
                complete();
                return null;
            }

            if (level != null) {
                BlockPos anchor = getBuildingAnchor();
                BlockPos worldPos = anchor.offset(next.localPos());
                var existing = level.getBlockState(worldPos);
                if (existing.equals(next.state())) continue;
                return next;
            }
            return next;
        }
    }

    public void markBlockPlaced() {
        blocksPlaced++;
        progress = blocksTotal > 0 ? (blocksPlaced * 100 / blocksTotal) : 100;
        setChanged();
    }

    public boolean addProgress(int amount) {
        if (completed) return false;
        progress = Math.min(100, progress + amount);
        setChanged();
        if (progress >= 100) { complete(); return true; }
        return false;
    }

    // 鈹€鈹€ 瀹屾垚 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private void complete() {
        if (level == null || level.isClientSide) return;
        completed = true;
        progress = 100;

        MetroGenesis.LOGGER.info("[Construction] 搂a瀹屾垚 搂r{} at {}", buildingTypeId,
                hasBuildingPos() ? buildingPos.toShortString() : worldPosition.toShortString());

        if (level instanceof ServerLevel sl) {
            ColonyState.get(sl).addToTreasury(50);
        }

        // 璁炬柦鏂瑰潡宸茬粡鍦?targetPos 浜嗭紝鍙渶瑕佺Щ闄ゅ鍚?        // 濡傛灉鏄嚜瀹氫箟钃濆浘锛堟壂鎻忕殑锛夛紝鎵嶆浛鎹㈠鍚婁负璁炬柦鏂瑰潡
        if (buildingTypeId.startsWith("custom_")) {
            var block = ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.tryParse("metrogenesis:" + buildingTypeId));
            if (block != null) {
                level.destroyBlock(worldPosition, false);
                level.setBlock(worldPosition, block.defaultBlockState(), 3);
            }
        } else {
            // 娓呴櫎濉斿悐锛堣鏂藉潡宸插湪 buildingPos锛?
            level.destroyBlock(worldPosition, false);
        }
    }

    /**
     * 鑾峰彇钃濆浘鐨勯敋鐐逛綅缃紙寤虹瓚涓績鐨勫簳閮ㄤ笢鍖楄锛?     */
    private BlockPos getBuildingAnchor() {
        BlockPos center = hasBuildingPos() ? buildingPos : worldPosition;
        int hx = blueprint != null ? blueprint.getSizeX() / 2 : 0;
        int hz = blueprint != null ? blueprint.getSizeZ() / 2 : 0;
        return center.offset(-hx, 0, -hz);
    }

    private boolean hasBuildingPos() {
        return !buildingPos.equals(BlockPos.ZERO);
    }

    // 鈹€鈹€ NBT + 鏁版嵁鍚屾 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("BuildingType", buildingTypeId);
        tag.putInt("Progress", progress);
        tag.putBoolean("Completed", completed);
        tag.putString("Builder", assignedBuilder);
        tag.putInt("BlocksTotal", blocksTotal);
        tag.putInt("BlocksPlaced", blocksPlaced);
        tag.putString("BlueprintName", blueprintName);
        tag.put("BuildingPos", NbtUtils.writeBlockPos(buildingPos));
        if (zone != null) tag.put("Zone", zone.save());
        if (iterator != null) tag.put("Iterator", iterator.save());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buildingTypeId = tag.getString("BuildingType");
        progress = tag.getInt("Progress");
        completed = tag.getBoolean("Completed");
        assignedBuilder = tag.getString("Builder");
        blocksTotal = tag.getInt("BlocksTotal");
        blocksPlaced = tag.getInt("BlocksPlaced");
        blueprintName = tag.getString("BlueprintName");
        if (tag.contains("BuildingPos")) {
            buildingPos = NbtUtils.readBlockPos(tag.getCompound("BuildingPos"));
        }
        if (tag.contains("Zone")) zone = Zone.load(tag.getCompound("Zone"));
        if (tag.contains("Iterator")) {
            iterator = new com.metrogenesis.blueprint.v1.BlueprintIterator(null);
            iterator.load(tag.getCompound("Iterator"));
        }
    }

    // 鈹€鈹€ MenuProvider 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.MetroGenesis.construction_marker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ConstructionMarkerMenu(id, inv, this, dataAccess);
    }

    // 鈹€鈹€ Getter 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public String getBuildingTypeId() { return buildingTypeId; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public Zone getZone() { return zone; }
    public String getAssignedBuilder() { return assignedBuilder; }
    public BlockPos getMarkerPos() { return worldPosition; }
    /** 鑾峰彇璁炬柦鏂瑰潡浣嶇疆锛堝缓閫犵洰鏍囦腑蹇冿級 */
    public BlockPos getBuildingPos() { return hasBuildingPos() ? buildingPos : worldPosition; }
    public int getBlocksTotal() { return blocksTotal; }
    public int getBlocksPlaced() { return blocksPlaced; }
    public String getBlueprintName() { return blueprintName; }
    public com.metrogenesis.blueprint.v1.Blueprint getBlueprint() { return blueprint; }
    public ContainerData getDataAccess() { return dataAccess; }

    // 鈹€鈹€ Setter锛堜緵 BuildingToolItem 棰勫垵濮嬪寲浣跨敤锛?鈹€鈹€

    public void setBuildingTypeId(String id) { this.buildingTypeId = id; }
    public void setZone(Zone zone) { this.zone = zone; }
    public void setBlueprint(com.metrogenesis.blueprint.v1.Blueprint bp) {
        this.blueprint = bp;
        this.iterator = new com.metrogenesis.blueprint.v1.BlueprintIterator(bp);
        this.blocksTotal = 0;
        this.blocksPlaced = 0;
        for (var info : bp.getBlockInfoAsList()) {
            if (info.state() != null && !info.state().isAir()) blocksTotal++;
        }
    }

    public void setAssignedBuilder(String uuid) {
        this.assignedBuilder = uuid;
        setChanged();
    }

    public boolean hasCustomBlueprint() { return buildingTypeId.startsWith("custom_"); }
}
