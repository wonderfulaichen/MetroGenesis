package com.metrogenesis.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 市长管理书 — 右键打开城市管理 GUI
 */
public class MayorBookItem extends Item {

    public MayorBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            ClientHandler.openScreen();
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.metrogenesis.mayor_book.desc"));
    }

    /** 客户端专用处理 — 仅在 CLIENT 侧加载 */
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void openScreen() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.metrogenesis.gui.MayorBookScreen());
        }
    }
}
