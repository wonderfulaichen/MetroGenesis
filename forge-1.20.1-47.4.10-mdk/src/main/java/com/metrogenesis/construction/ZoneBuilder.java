package com.metrogenesis.construction;

import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.structurize.util.BlockInfo;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 区域建筑放置管线 — 接收 ZonePlanner 生成的 BuildingSlot 列表，
 * 逐 tick 加载蓝图并放置方块，避免卡顿。
 * <p>
 * 在 ModEventBus 的 {@code ServerTickEvent} 中调用 {@link #tick(ServerLevel)}。
 */
public class ZoneBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneBuilder.class);

    /** 每 tick 放置的最大方块数 */
    private static final int BLOCKS_PER_TICK = 50;

    /** 队列：ZoneRect → 待放置的 BuildingSlot 列表 */
    private final Map<ZoneRect, List<ZonePlanner.BuildingSlot>> queue = new LinkedHashMap<>();

    /** 每个区域的放置进度状态 */
    private final Map<ZoneRect, PlacementState> activeStates = new HashMap<>();

    // ════════════════════════════════════════════════════════════
    //  PlacementState — 单区域的 tick 推进状态
    // ════════════════════════════════════════════════════════════

    private static class PlacementState {
        List<ZonePlanner.BuildingSlot> slots;
        int currentSlotIndex;

        Blueprint currentBlueprint;
        List<BlockInfo> currentBlockInfos;
        int currentBlockIndex;
        BlockPos origin;
    }

    // ════════════════════════════════════════════════════════════
    //  启动
    // ════════════════════════════════════════════════════════════

    /**
     * 启动区域生长：先通过 ZonePlanner 生成 slot，再加入放置队列。
     */
    public void startGrowth(ZoneRect zone, ServerLevel level) {
        if (queue.containsKey(zone)) {
            LOGGER.warn("ZoneBuilder: zone {} is already growing", zone);
            return;
        }

        // 通过 ZonePlanner 生成放置计划
        // 注意：getEntriesForZone 需要从外部注入建筑目录，此处返回空列表
        // 实际使用时建议直接调用 startGrowth(zone, prePlannedSlots, level)
        List<ZonePlanner.BuildingSlot> slots = new ZonePlanner().plan(zone, getEntriesForZone(zone));
        startGrowth(zone, slots, level);
    }

    /**
     * 启动区域生长：直接传入已生成的 slot 列表。
     */
    public void startGrowth(ZoneRect zone, List<ZonePlanner.BuildingSlot> slots, ServerLevel level) {
        if (queue.containsKey(zone)) {
            LOGGER.warn("ZoneBuilder: zone {} is already growing", zone);
            return;
        }

        List<ZonePlanner.BuildingSlot> slotCopy = new ArrayList<>(slots);
        queue.put(zone, slotCopy);
        zone.setStage(ZoneRect.STAGE_BUILDING);

        LOGGER.info("ZoneBuilder: started growth for zone {} with {} slots",
            zone, slotCopy.size());
    }

    // ════════════════════════════════════════════════════════════
    //  Tick 推进
    // ════════════════════════════════════════════════════════════

    /**
     * 逐 tick 推进放置。在 ModEventBus 的 ServerTickEvent 中调用。
     */
    public void tick(ServerLevel level) {
        if (queue.isEmpty()) return;

        // 处理队列中的第一个区域
        Map.Entry<ZoneRect, List<ZonePlanner.BuildingSlot>> entry = queue.entrySet().iterator().next();
        ZoneRect zone = entry.getKey();
        List<ZonePlanner.BuildingSlot> slots = entry.getValue();

        PlacementState state = activeStates.computeIfAbsent(zone, k -> {
            PlacementState ps = new PlacementState();
            ps.slots = slots;
            ps.currentSlotIndex = 0;
            return ps;
        });

        // 检查是否所有 slot 都已处理完毕
        if (state.currentSlotIndex >= state.slots.size()) {
            finishZone(zone);
            return;
        }

        // 处理当前 slot
        processCurrentSlot(state, level);

        // 如果当前 slot 刚刚完成，推进到下一个 slot
        // （processCurrentSlot 会在完成时重置 currentBlueprint = null）
    }

    /**
     * 处理当前正在放置的建筑。如果当前建筑完成，自动推进到下一 slot。
     */
    private void processCurrentSlot(PlacementState state, ServerLevel level) {
        ZonePlanner.BuildingSlot slot = state.slots.get(state.currentSlotIndex);

        // 第一次进入此 slot → 加载蓝图
        if (state.currentBlueprint == null) {
            if (!initSlotPlacement(state, slot, level)) {
                // 加载失败，跳过此 slot
                state.currentSlotIndex++;
                return;
            }
        }

        // 放置方块
        placeBlocks(state, level);

        // 检查是否完成
        if (state.currentBlockIndex >= state.currentBlockInfos.size()) {
            LOGGER.info("ZoneBuilder: finished blueprint '{}' for slot {}",
                slot.assignedBlueprint() != null ? slot.assignedBlueprint().name() : "null",
                state.currentSlotIndex);

            state.currentBlueprint = null;
            state.currentBlockInfos = null;
            state.currentBlockIndex = 0;

            // 推进到下一个 slot（下一 tick 会加载新的）
            // 但这里不能直接 +1，因为 tick() 的主循环可能在一 tick 内处理多个小建筑
            state.currentSlotIndex++;

            // 如果还有下一个 slot，递归尝试（可能有多个小建筑能在同一 tick 处理）
            if (state.currentSlotIndex < state.slots.size()) {
                processCurrentSlot(state, level);
            }
        }
    }

    /**
     * 初始化一个 slot 的放置：加载蓝图、应用旋转、计算原点。
     *
     * @return true 如果加载成功
     */
    private boolean initSlotPlacement(PlacementState state, ZonePlanner.BuildingSlot slot, ServerLevel level) {
        BuildingCatalogEntry entry = slot.assignedBlueprint();
        if (entry == null) {
            LOGGER.warn("ZoneBuilder: slot {} has no assigned blueprint, skipping", slot);
            return false;
        }

        // ── 加载蓝图 ──────────────────────────────────────────
        Blueprint blueprint = loadBlueprint(entry);
        if (blueprint == null) {
            LOGGER.error("ZoneBuilder: failed to load blueprint '{}' from pack '{}'",
                entry.name(), entry.packName());
            return false;
        }

        // ── 应用旋转 ──────────────────────────────────────────
        // rotation 参数（0~3）：0=0°, 1=90°, 2=180°, 3=270°
        Rotation rotation = switch (slot.rotation()) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        RotationMirror targetRot = RotationMirror.of(rotation, Mirror.NONE);

        if (targetRot != RotationMirror.NONE) {
            blueprint.setRotationMirror(targetRot, level);
        }

        // ── 计算 Y 坐标（地形高度） ────────────────────────────
        // 使用 slot 的中心点获取地表高度
        int cx = (slot.minX() + slot.maxX()) / 2;
        int cz = (slot.minZ() + slot.maxZ()) / 2;
        int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);

        // 蓝图原点 (0,0,0) 对齐到 slot 的 (minX, groundY, minZ)
        BlockPos origin = new BlockPos(slot.minX(), groundY, slot.minZ());

        // ── 扣除 C-Value 成本 ─────────────────────────────────
        // TODO: 从国库扣除 C-Value
        // long cost = entry.materialCost();
        // TreasuryManager.get(level).withdraw(cost);
        // LOGGER.info("ZoneBuilder: deducted {} C-Value for '{}'", cost, entry.name());

        state.currentBlueprint = blueprint;
        state.currentBlockInfos = blueprint.getBlockInfoAsList();
        state.currentBlockIndex = 0;
        state.origin = origin;

        LOGGER.debug("ZoneBuilder: loaded '{}' ({} blocks) at origin {} with rotation {}",
            entry.name(), state.currentBlockInfos.size(), origin, targetRot);

        return true;
    }

    /**
     * 放置最多 BLOCKS_PER_TICK 个方块。
     */
    private void placeBlocks(PlacementState state, ServerLevel level) {
        List<BlockInfo> infos = state.currentBlockInfos;
        int start = state.currentBlockIndex;
        int end = Math.min(start + BLOCKS_PER_TICK, infos.size());

        BlockPos origin = state.origin;

        for (int i = start; i < end; i++) {
            BlockInfo info = infos.get(i);
            BlockState blockState = info.getState();

            // 跳过空气，减少不必要的 setBlock 调用
            if (blockState == null || blockState.isAir()) {
                continue;
            }

            BlockPos worldPos = origin.offset(info.getPos());
            level.setBlock(worldPos, blockState, 3);

            // TODO: 处理 tile entity 数据
            // if (info.getTileEntityData() != null) {
            //     BlockEntity be = level.getBlockEntity(worldPos);
            //     if (be != null) {
            //         be.load(info.getTileEntityData());
            //     }
            // }
        }

        state.currentBlockIndex = end;
    }

    /**
     * 完成一个区域：标记 stage 并从队列中移除。
     */
    private void finishZone(ZoneRect zone) {
        zone.setStage(ZoneRect.STAGE_COMPLETED);
        queue.remove(zone);
        activeStates.remove(zone);
        LOGGER.info("ZoneBuilder: zone {} completed", zone);
    }

    // ════════════════════════════════════════════════════════════
    //  查询 / 取消
    // ════════════════════════════════════════════════════════════

    public boolean isGrowing(ZoneRect zone) {
        return queue.containsKey(zone);
    }

    public void cancelGrowth(ZoneRect zone) {
        queue.remove(zone);
        activeStates.remove(zone);
        LOGGER.info("ZoneBuilder: growth cancelled for zone {}", zone);
    }

    // ════════════════════════════════════════════════════════════
    //  内部辅助
    // ════════════════════════════════════════════════════════════

    /**
     * 从 StructurePacks 加载蓝图。
     */
    @Nullable
    private Blueprint loadBlueprint(BuildingCatalogEntry entry) {
        // entry.blueprintSubPath() 返回目录路径如 "agriculture/horticulture"
        // entry.blueprintFileName() 返回文件名如 "farmer"
        // 组合后完整路径如 "agriculture/horticulture/farmer"
        String subPath = entry.blueprintSubPath();
        String fileName = entry.blueprintFileName();
        String fullPath = subPath.isEmpty() ? fileName : subPath + "/" + fileName;

        // StructurePacks.getBlueprint(packName, subPath) 是同步 IO，
        // 考虑后续改为 getBlueprintFuture 异步加载
        return StructurePacks.getBlueprint(entry.packName(), fullPath);
    }

    /**
     * 获取区域匹配的建筑目录条目。
     * <p>
     * 注意：此方法需要外部注入 BuildingCatalog。当前返回空列表，
     * 实际使用时请调用 {@link #startGrowth(ZoneRect, List, ServerLevel)} 传入预规划 slot。
     */
    private List<BuildingCatalogEntry> getEntriesForZone(ZoneRect zone) {
        // TODO: 从 BuildingCatalog 获取当前风格包下的所有条目，
        //       并按 zone.zoneType() 和 zone.density() 预筛选
        return Collections.emptyList();
    }
}
