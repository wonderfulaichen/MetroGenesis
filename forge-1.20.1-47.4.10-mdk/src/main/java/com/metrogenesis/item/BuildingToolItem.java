package com.metrogenesis.item;

import com.metrogenesis.structurize.Structurize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import static com.metrogenesis.structurize.api.util.constant.Constants.GROUNDSTYLE_RELATIVE;

/**
 * 寤虹瓚宸ュ叿 鈥?濮旀墭缁?Structurize 鐨?BuildToolWindow
 * <p>
 * 鍙抽敭鏂瑰潡鎴栫┖姘?鈫?鎵撳紑 MineColonies 寤虹瓚閫夋嫨鐣岄潰
 * 锛堝寘鎷摑鍥惧眰绾с€佹牱寮忛€夊彇銆佹畺姘戝湴鍏ㄥ寤虹瓚绫诲瀷锛? */
public class BuildingToolItem extends Item {

    public BuildingToolItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // 鍙抽敭绌烘皵 鈫?鎵撳紑 Structurize 寤虹瓚宸ュ叿锛堟棤鐩爣浣嶇疆锛?            Structurize.proxy.openBuildToolWindow(null, GROUNDSTYLE_RELATIVE);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            // 鍙抽敭鏂瑰潡 鈫?璁＄畻鏀剧疆浣嶇疆鍚庢墦寮€ Structurize 寤虹瓚宸ュ叿
            BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
            Structurize.proxy.openBuildToolWindow(targetPos, GROUNDSTYLE_RELATIVE);
        }
        return InteractionResult.SUCCESS;
    }
}
