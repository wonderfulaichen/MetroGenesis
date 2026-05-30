package com.metrogenesis.block.construction;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.item.ScanToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 鏂藉伐鏋舵柟鍧?鈥?鏀寔涓ょ寤洪€犳ā寮? * <ol>
 *   <li>绌烘墜鍙抽敭 鈫?GUI 閫夋嫨鍐呯疆寤虹瓚绫诲瀷</li>
 *   <li>鎸佹壂鎻忚摑鍥?鈫?鐩存帴鍔犺浇鑷畾涔夎摑鍥?/li>
 * </ol>
 */
public class ConstructionMarkerBlock extends BaseEntityBlock {

    public ConstructionMarkerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollission()
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConstructionMarkerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ConstructionMarkerBlockEntity te) te.tick();
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity te)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // 妫€鏌ユ墜涓槸鍚︽湁甯﹁摑鍥剧殑鎵弿宸ュ叿
        ItemStack held = player.getItemInHand(hand);
        if (held.is(MetroGenesis.SCAN_TOOL.get()) && ScanToolItem.hasBlueprint(held)) {
            var bp = ScanToolItem.loadBlueprint(held);
            if (bp != null) {
                String name = held.getTag() != null ? held.getTag().getString("blueprintName") : "unnamed";
                if (name.isEmpty()) name = "scan_" + System.currentTimeMillis() % 10000;

                te.loadBlueprint(bp, name);
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "搂a钃濆浘宸插姞杞? 搂e" + name + " 搂7(" + te.getBlocksTotal() + " 鏂瑰潡)"), false);
                MetroGenesis.LOGGER.info("[Construction] 鐜╁ {} 鍔犺浇浜嗚摑鍥?'{}' 鍒版柦宸ユ灦 at {}",
                    player.getName().getString(), name, pos.toShortString());
                return InteractionResult.SUCCESS;
            }
        }

        // 绌烘墜鎴栨病钃濆浘 鈫?鎵撳紑 GUI
        NetworkHooks.openScreen(sp, te, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            com.metrogenesis.hologram.HologramRenderer.invalidate(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
