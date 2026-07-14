package com.metrogenesis.block;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 鍐滃満璁炬柦鏍囪鏂瑰潡 鈥?浣跨敤鍫嗚偉妗舵ā鍨? * <p>
 * 鏀剧疆鍚庤嚜鍔ㄦ敞鍐屼负鍐滃満璁炬柦锛屽啘澶?AI 鍙瘑鍒苟鍓嶅線宸ヤ綔銆? */
public class FarmFacilityBlock extends Block {

    public FarmFacilityBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()
        );
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            MetroGenesis.LOGGER.info("[Facility] 鍐滃満璁炬柦 搂a娉ㄥ唽 搂r浜?{}", pos.toShortString());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            MetroGenesis.LOGGER.info("[Facility] 鍐滃満璁炬柦 搂c娉ㄩ攢 搂r浜?{}", pos.toShortString());
        }
    }
}
