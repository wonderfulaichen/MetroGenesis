package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 施工菜单 — 按钮选择建筑类型
 * <p>
 * 流程：
 * 1. BuildingToolItem 右键地面 → 直接弹出 GUI（此时还没有 BE）
 * 2. 玩家点击按钮选择建筑类型 → clickMenuButton 在服务端创建施工标记
 * 3. startConstruction → zone + blueprint + 全息投影
 * <p>
 * Phase 0e: 改为动态列表，从 BuildingType.ALL 获取可用建筑类型
 */
public class ConstructionMarkerMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private BlockPos pos;
    private ConstructionMarkerBlockEntity be;

    // 客户端构造（从网络包读取 pos）
    public ConstructionMarkerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    // 服务端构造（无 BE — BuildingToolItem 预选阶段使用）
    public ConstructionMarkerMenu(int id, Inventory inv, BlockPos pos) {
        super(MetroGenesis.CONSTRUCTION_MARKER_MENU.get(), id);
        this.pos = pos;
        this.be = null;
        this.data = new SimpleContainerData(4);
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        addDataSlots(data);
    }

    // 服务端构造（已有 BE — 右键使用）：
    public ConstructionMarkerMenu(int id, Inventory inv,
                                   ConstructionMarkerBlockEntity be,
                                   ContainerData data) {
        super(MetroGenesis.CONSTRUCTION_MARKER_MENU.get(), id);
        this.be = be;
        this.data = data;
        this.pos = be != null ? be.getBlockPos() : BlockPos.ZERO;
        this.access = be != null
                ? ContainerLevelAccess.create(be.getLevel(), be.getBlockPos())
                : ContainerLevelAccess.NULL;
        addDataSlots(data);
    }

    // ══ 按钮事件 ═════════════════════════════════════

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        // 动态列表：从 BuildingType.ALL 获取（仅 enabled 的类型）
        var enabledTypes = com.metrogenesis.init.BuildingType.ALL.stream()
                .filter(com.metrogenesis.init.BuildingType::isEnabled)
                .toList();
        if (buttonId < 0 || buttonId >= enabledTypes.size()) return false;

        String typeId = enabledTypes.get(buttonId).getId();
        Level level = player.level();
        if (!level.isClientSide) {
            ConstructionMarkerBlockEntity markerBe = null;

            // 如果还没有施工标记，在 pos 处创建
            if (level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity existingBe) {
                markerBe = existingBe;
            } else {
                level.setBlock(pos, MetroGenesis.CONSTRUCTION_MARKER_BLOCK.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity newBe) {
                    markerBe = newBe;
                    this.be = newBe;
                }
            }

            if (markerBe != null) {
                markerBe.startConstruction(typeId);
                addDataSlots(markerBe.getDataAccess());
                MetroGenesis.LOGGER.info("[GUI] 玩家 {} 选择建造 {} at {}",
                        player.getName().getString(), typeId, pos.toShortString());
            }
        }
        return true;
    }

    // ══ 数据读取（供 Screen 使用）════════════════════

    public int getProgress() { return data.get(0); }
    public boolean isCompleted() { return data.get(1) == 1; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
