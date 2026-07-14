package com.metrogenesis.item;

import com.metrogenesis.blueprint.v2.BlueprintCaptureState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 蓝图之眼 — MetroGenesis 全新扫描/放置二合一工具。
 * <p>
 * 扫描模式（默认）：
 * <ol>
 *   <li>攻击方块（左键）→ 设置起点，进入框选状态</li>
 *   <li>右键方块 → 以点击位置为终点弹出扫描结果</li>
 *   <li>右键空气 → 以鼠标指向方块为终点弹出扫描结果</li>
 * </ol>
 * 交互流程对大型建筑友好：左手可以完全放开，无需一直按住鼠标。
 * <p>
 * 所有客户端交互逻辑在客户端完成（弹窗），服务端仅在保存时收到 CaptureBlueprintPacket。
 */
public class BlueprintEyeItem extends Item
{
    public BlueprintEyeItem()
    {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // ══ 左键点击方块 — 设置起点 ═══════════════════════

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                   Level level, BlockPos pos, Player player)
    {
        // 仅在客户端设置 CaptureState（选框渲染用）
        if (level.isClientSide)
        {
            BlueprintCaptureState.startCapture(pos, player.getYRot());
            player.displayClientMessage(
                Component.literal("§7起点已定: §e" + pos.toShortString()), false);
        }
        return false; // 不破坏方块
    }

    // ══ 右键 — 结束扫描 ═════════════════════════════

    @Override
    public InteractionResult useOn(UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        if (level.isClientSide && BlueprintCaptureState.isActive())
        {
            ClientHandler.handleUseOn(ctx);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (level.isClientSide && BlueprintCaptureState.isActive())
        {
            return ClientHandler.handleUse(level, player, hand);
        }

        // 潜行+右键：模式切换（留空）
        if (player.isShiftKeyDown() && !level.isClientSide)
        {
            player.displayClientMessage(
                Component.literal("§7模式切换暂未实现"), false);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tips, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tips, flag);
        tips.add(Component.literal("§7左键起点 → 右键终点"));
        tips.add(Component.literal("§7框选后自动弹出扫描结果"));
    }

    /** 客户端专用处理 — 仅在 CLIENT 侧加载 */
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handleUseOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos start = BlueprintCaptureState.getStartPos();
            if (start != null)
            {
                // 优先使用射线-平面交点，fallback 到点击方块位置
                BlockPos end = ctx.getPlayer() != null
                    ? com.metrogenesis.client.renderer.RayPlaneUtil.getFreeEndPos(ctx.getPlayer(), start)
                    : null;
                if (end == null) end = ctx.getClickedPos();

                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.metrogenesis.gui.BlueprintCaptureResultScreen(start, end));
            }
            BlueprintCaptureState.cancelCapture();
        }

        static InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand) {
            BlockPos start = BlueprintCaptureState.getStartPos();
            if (start != null)
            {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                // 方式 1: 射线-平面交点（自由3D定位）
                BlockPos end = com.metrogenesis.client.renderer.RayPlaneUtil.getFreeEndPos(player, start);
                // 方式 2: 准星打方块（fallback）
                if (end == null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr)
                    end = bhr.getBlockPos();
                if (end == null)
                    end = player.blockPosition(); // 终极 fallback

                mc.setScreen(new com.metrogenesis.gui.BlueprintCaptureResultScreen(start, end));
            }
            BlueprintCaptureState.cancelCapture();
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), true);
        }
    }
}
