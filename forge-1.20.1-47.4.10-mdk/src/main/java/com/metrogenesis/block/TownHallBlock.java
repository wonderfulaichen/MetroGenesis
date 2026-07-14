package com.metrogenesis.block;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.gui.TownHallMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * 甯傛斂鍘呮柟鍧?鈥?鐢卞缓绛戝伐鍏封啋鏂藉伐鏋垛啋寤洪€犲畬鎴愬悗鏇挎崲寰楀埌
 * <p>
 * 鏀剧疆锛堢敱 ConstructionSite.complete() 瑙﹀彂锛夋椂鍒濆鍖栨畺姘戝湴銆? */
public class TownHallBlock extends Block {

    public TownHallBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            ColonyState colony = ColonyState.get(serverLevel);

            NetworkHooks.openScreen(
                    (ServerPlayer) player,
                    new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("container.MetroGenesis.town_hall");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new TownHallMenu(id, inv, colony, pos);
                        }
                    },
                    buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeInt(colony.getFunds());
                        buf.writeInt(colony.getPopulation());
                        buf.writeInt(colony.getMaxPopulation());
                        TownHallMenu.writeCitizens(buf, colony);
                    }
            );
        }
        return InteractionResult.SUCCESS;
    }
}
