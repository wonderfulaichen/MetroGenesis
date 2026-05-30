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
 * 鏂藉伐鑿滃崟 鈥?鎸夐挳閫夋嫨寤虹瓚绫诲瀷
 * <p>
 * 娴佺▼锛? * 1. BuildingToolItem 鍙抽敭鍦伴潰 鈫?鐩存帴寮规 GUI锛堟鏃惰繕娌℃湁 BE锛? * 2. 鐜╁鐐瑰嚮鎸夐挳閫夋嫨寤虹瓚绫诲瀷 鈫?clickMenuButton 鍦ㄦ湇鍔＄鍒涘缓鏂藉伐鏍囪
 * 3. startConstruction 鈫?zone + blueprint + 鍏ㄦ伅鎶曞奖
 */
public class ConstructionMarkerMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private BlockPos pos;
    private ConstructionMarkerBlockEntity be;

    // 瀹㈡埛绔瀯閫狅紙浠庣綉缁滃寘璇诲彇 pos锛?
    public ConstructionMarkerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    // 鏈嶅姟绔瀯閫狅紙鏃?BE 鈥?BuildingToolItem 棰勯€夐樁娈典娇鐢級
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

    // ══ 鎸夐挳浜嬩欢 ═════════════════════════════════════

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        String[] types = {"farm_facility", "town_hall"};
        if (buttonId < 0 || buttonId >= types.length) return false;

        Level level = player.level();
        if (!level.isClientSide) {
            ConstructionMarkerBlockEntity markerBe = null;

            // 濡傛灉杩樻病鏈夋柦宸ユ爣璁帮紝在 pos 处创建
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
                markerBe.startConstruction(types[buttonId]);
                // 閲嶈 data slots 涓虹湡瀹?BE 鏁版嵁锛堝悗缁?Screen 鍙锛?                addDataSlots(markerBe.getDataAccess());
                MetroGenesis.LOGGER.info("[GUI] 玩家 {} 选择建造 {} at {}",
                        player.getName().getString(), types[buttonId], pos.toShortString());
            }
        }
        return true;
    }

    // ══ 鏁版嵁璇诲彇锛堜緵 Screen 浣跨敤锛?═══════════════════

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
