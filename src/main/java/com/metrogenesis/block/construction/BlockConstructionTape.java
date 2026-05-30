package com.metrogenesis.block.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 鏂藉伐鍥存爮鏂瑰潡 鈥?鏍囪寤虹瓚鑼冨洿鐨勫彲瑙嗗寲杈圭晫
 * <p>
 * 鍙傝€?MineColonies {@code BlockConstructionTape}锛? * <ul>
 *   <li>鏃犵鎾炵锛堢帺瀹跺彲绌胯繃锛?/li>
 *   <li>鍙鏇挎崲锛堝缓閫犳椂鍙洿鎺ヨ寤虹瓚鏂瑰潡瑕嗙洊锛?/li>
 *   <li>鏃犳帀钀界墿</li>
 *   <li>鎺ㄦ媺鐮村潖</li>
 * </ul>
 * <p>
 * 绠€鍖栫増锛氫笉浣滆繛鎺ラ€昏緫锛圢ORTH/EAST/SOUTH/WEST锛夛紝绾柟鍧椼€? */
public class BlockConstructionTape extends Block {

    public BlockConstructionTape() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .sound(SoundType.WOOL)
                .replaceable()
                .pushReaction(PushReaction.DESTROY)
                .noCollission()
                .noLootTable()
                .strength(0.0f));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty(); // 鏃犵鎾炵
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
}
