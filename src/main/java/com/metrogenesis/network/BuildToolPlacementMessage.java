package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * 寤虹瓚鏀剧疆璇锋眰 鈥?瀹㈡埛绔啋鏈嶅姟绔? * <p>
 * 1. 鍦ㄧ洰鏍囦綅缃斁缃鏂芥柟鍧楋紙濡傚競鏀垮巺锛? * 2. 鍦ㄦ梺杈瑰亸绉讳綅缃斁缃柦宸ユ爣璁帮紙濉斿悐锛夛紝甯﹀叏鎭姇褰? * 3. Builder 鍥寸潃璁炬柦鏂瑰潡鎼缓缁撴瀯
 */
public class BuildToolPlacementMessage
{
    private final BlockPos pos;
    private final String buildingTypeId;

    /** 鏂藉伐鏍囪鐩稿浜庤鏂戒綅缃殑鍋忕Щ閲?*/
    private static final int CRANE_OFFSET = 7;

    public BuildToolPlacementMessage(BlockPos pos, String buildingTypeId)
    {
        this.pos = pos;
        this.buildingTypeId = buildingTypeId;
    }

    // 鈹€鈹€ 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public static void encode(BuildToolPlacementMessage msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.buildingTypeId);
    }

    public static BuildToolPlacementMessage decode(FriendlyByteBuf buf)
    {
        return new BuildToolPlacementMessage(buf.readBlockPos(), buf.readUtf());
    }

    // 鈹€鈹€ 澶勭悊 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public static void handle(BuildToolPlacementMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.isClientSide) return;

            BlockPos buildPos = msg.pos;
            String typeId = msg.buildingTypeId;
            BuildingType type = BuildingType.fromId(typeId);
            if (type == null) {
                player.sendSystemMessage(Component.literal("\u00A7c\u672A\u77E5\u5EFA\u7B51\u7C7B\u578B: " + typeId));
                return;
            }

            int radius = type.getZoneRadius();

            // 1. 妫€鏌ヤ綅缃槸鍚﹀彲鐢?
            if (!level.getBlockState(buildPos).isAir()) {
                player.sendSystemMessage(Component.literal("\u00A7c\u8BE5\u4F4D\u7F6E\u5DF2\u6709\u65B9\u5757"));
                return;
            }

            // 2. 鏀剧疆璁炬柦鏂瑰潡锛堝缓绛戠殑"鏍稿績"锛?
        Block facilityBlock = ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.tryParse("metrogenesis:" + typeId));
            if (facilityBlock == null) {
                player.sendSystemMessage(Component.literal("\u00A7c\u627E\u4E0D\u5230\u8BBE\u65BD\u65B9\u5757 " + typeId));
                return;
            }
            level.setBlock(buildPos, facilityBlock.defaultBlockState(), 3);

            // 3. 鍦ㄦ梺杈规斁缃柦宸ユ爣璁帮紙濉斿悐浣嶇疆锛?
            BlockPos cranePos = buildPos.offset(CRANE_OFFSET, 0, 0);
            // 鎵句竴涓湴闈笂鐨勭┖浣?
            for (int dx = CRANE_OFFSET; dx >= 3; dx--) {
                BlockPos test = buildPos.offset(dx, 0, 0);
                if (level.getBlockState(test).isAir()) {
                    cranePos = test;
                    break;
                }
            }
            level.setBlock(cranePos, MetroGenesis.CONSTRUCTION_MARKER_BLOCK.get().defaultBlockState(), 3);

            // 4. 鍒濆鍖栨柦宸ユ爣璁帮紙鍏宠仈鍒拌鏂戒綅缃級
            BlockEntity be = level.getBlockEntity(cranePos);
            if (be instanceof ConstructionMarkerBlockEntity te)
            {
                te.startConstructionWithFacility(typeId, buildPos, radius);
                player.sendSystemMessage(Component.literal(
                        "\u00A7a\u5DF2\u653E\u7F6E\u00A7e" + type.getDisplayName() + " \u00A77\u65BD\u5DE5\u5854\u540A"));
                MetroGenesis.LOGGER.info("[Placement] {} 璁炬柦 at {}, 濉斿悐 at {}",
                        typeId, buildPos.toShortString(), cranePos.toShortString());
            } else {
                // 鏂藉伐鏍囪鏀剧疆澶辫触锛屽洖婊?
                level.destroyBlock(buildPos, false);
                level.destroyBlock(cranePos, false);
                player.sendSystemMessage(Component.literal("\u00A7c\u65BD\u5DE5\u6807\u8BB0\u521D\u59CB\u5316\u5931\u8D25"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
