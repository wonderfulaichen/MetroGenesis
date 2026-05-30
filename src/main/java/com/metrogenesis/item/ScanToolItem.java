package com.metrogenesis.item;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.util.BlueprintUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 钃濆浘鎵弿宸ュ叿 鈥?閫夋嫨鍖哄煙 鈫?鎵弿 鈫?淇濆瓨涓鸿摑鍥? * <p>
 * 鍙傝€?MineColonies/Structurize {@code AbstractItemWithPosSelector} + {@code ItemScanTool}銆? * <p>
 * 鐢ㄦ硶锛? * <ol>
 *   <li>鏀诲嚮鏂瑰潡 = 璁剧疆绗竴涓</li>
 *   <li>鍙抽敭鏂瑰潡 = 璁剧疆绗簩涓</li>
 *   <li>娼滆+鍙抽敭绌烘皵 = 鎵弿骞朵繚瀛樿摑鍥惧埌鐗╁搧</li>
 *   <li>鍙抽敭绌烘皵 = 鏌ョ湅褰撳墠閫夊尯淇℃伅</li>
 * </ol>
 */
public class ScanToolItem extends Item
{
    private static final String TAG_FIRST = "firstPos";
    private static final String TAG_SECOND = "secondPos";
    private static final String TAG_BLUEPRINT = "blueprintData";
    private static final String TAG_NAME = "blueprintName";

    public ScanToolItem()
    {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // ══ 鏀诲嚮璁剧疆绗竴涓 ═════════════════════════════

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
    {
        if (!level.isClientSide)
        {
            ItemStack stack = player.getMainHandItem();
            if (stack.is(this))
            {
                stack.getOrCreateTag().put(TAG_FIRST, NbtUtils.writeBlockPos(pos));
                player.displayClientMessage(
                    Component.literal("§7第一个角: §e" + pos.toShortString()), false);
            }
        }
        return false; // 不破坏方块
    }

    // ══ 鍙抽敭鏂瑰潡锛氳缃浜屼釜瑙?/ 鏀剧疆鑷畾涔夎摑鍥?════

    @Override
    public InteractionResult useOn(UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();

        // 濡傛灉鏈夊凡淇濆瓨鐨勮摑鍥?鈫?鏀剧疆鍒颁笘鐣?
        if (hasBlueprint(stack))
        {
            if (!level.isClientSide)
            {
                Blueprint bp = loadBlueprint(stack);
                if (bp == null) return InteractionResult.FAIL;

                BlockPos placePos = pos.relative(ctx.getClickedFace());
                String name = stack.getOrCreateTag().getString(TAG_NAME);
                if (name.isEmpty()) name = bp.getName();
                if (name == null || name.isEmpty()) name = "custom";

                // 鏀剧疆鏂藉伐鏍囪锛屽姞杞借嚜瀹氫箟钃濆浘
                level.setBlock(placePos, MetroGenesis.CONSTRUCTION_MARKER_BLOCK.get().defaultBlockState(), 3);
                BlockEntity be = level.getBlockEntity(placePos);
                if (be instanceof ConstructionMarkerBlockEntity te)
                {
                    te.setAssignedBuilder("");
                    te.loadBlueprint(bp, name);

                    // 閫氱煡
                    int blockCount = te.getBlocksTotal();
                    player.displayClientMessage(Component.literal(
                            "§a放置自定义蓝图 §e" + name + " §7(" + blockCount + " 鏂瑰潡)"), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 娌℃湁钃濆浘 鈫?璁剧疆绗簩涓
        if (!level.isClientSide)
        {
            stack.getOrCreateTag().put(TAG_SECOND, NbtUtils.writeBlockPos(pos));
            player.displayClientMessage(
                Component.literal("§7第二个角: §e" + pos.toShortString()), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        // 娼滆+鍙抽敭绌烘皵 = 鎵弿
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide)
            {
                CompoundTag tag = stack.getOrCreateTag();
                if (!tag.contains(TAG_FIRST) || !tag.contains(TAG_SECOND))
                {
                    player.displayClientMessage(
                        Component.literal("§c请先选择两个角！"), false);
                    return InteractionResultHolder.fail(stack);
                }

                BlockPos first = NbtUtils.readBlockPos(tag.getCompound(TAG_FIRST));
                BlockPos second = NbtUtils.readBlockPos(tag.getCompound(TAG_SECOND));

                // 鎵弿
                String name = tag.getString(TAG_NAME);
                if (name.isEmpty()) name = "blueprint_" + System.currentTimeMillis() % 100000;

                Blueprint bp = BlueprintUtil.scanRegion(level, first, second, name);

                // 搴忓垪鍖栧埌鐗╁搧 NBT
                CompoundTag bpTag = BlueprintUtil.writeBlueprintToNBT(bp);
                tag.put(TAG_BLUEPRINT, bpTag);

                int blockCount = bp.getBlockInfoAsList().size();
                player.displayClientMessage(
                    Component.literal("§a鎵弿瀹屾垚锛伮?钃濆浘: §e" + name
                        + " §7(" + blockCount + " 鏂瑰潡)"), false);
                MetroGenesis.LOGGER.info("[ScanTool] {} 扫描了区域{} ~ {} 鈫?{} ({} blocks)",
                    player.getName().getString(), first.toShortString(),
                    second.toShortString(), name, blockCount);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 鏅€氬彸閿┖姘?= 鏄剧ず褰撳墠閫夊尯
        if (!level.isClientSide)
        {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(TAG_BLUEPRINT))
            {
                String name = tag.getString(TAG_NAME);
                if (name.isEmpty()) name = "未命名";
                player.displayClientMessage(
                    Component.literal("§7当前蓝图: §e" + name), false);
            }
            else if (tag.contains(TAG_FIRST) && tag.contains(TAG_SECOND))
            {
                BlockPos f = NbtUtils.readBlockPos(tag.getCompound(TAG_FIRST));
                BlockPos s = NbtUtils.readBlockPos(tag.getCompound(TAG_SECOND));
                player.displayClientMessage(
                    Component.literal("§7选区: §e" + f.toShortString() + " §7~ §e" + s.toShortString()), false);
            }
            else
            {
                player.displayClientMessage(
                    Component.literal("§7请攻击一个方块设置第一个角，右键设置第二个角"), false);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ══ 杈呭姪鏂规硶 ═════════════════════════════════════

    /** 淇濆瓨钃濆浘 */
    public static void saveBlueprint(ItemStack stack, Blueprint bp, String name)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_BLUEPRINT, BlueprintUtil.writeBlueprintToNBT(bp));
        tag.putString(TAG_NAME, name != null ? name : "");
    }

    /** 璇诲彇钃濆浘 */
    @Nullable
    public static Blueprint loadBlueprint(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_BLUEPRINT))
        {
            return BlueprintUtil.readBlueprintFromNBT(tag.getCompound(TAG_BLUEPRINT));
        }
        return null;
    }

    /** 鏄惁鏈夎摑鍥?*/
    public static boolean hasBlueprint(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains(TAG_BLUEPRINT);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tips, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tips, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null)
        {
            if (tag.contains(TAG_BLUEPRINT))
            {
                String name = tag.getString(TAG_NAME);
                if (name.isEmpty()) name = "未命名";
                tips.add(Component.literal("§e§l[蓝图] §7" + name));
            }
            else if (tag.contains(TAG_FIRST))
            {
                tips.add(Component.literal("§7已选区域"));
            }
            else
            {
                tips.add(Component.literal("§7未选择区域"));
            }
        }
    }
}
