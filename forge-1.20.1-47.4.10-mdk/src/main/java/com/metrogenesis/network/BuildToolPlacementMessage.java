package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import com.metrogenesis.structurize.management.Manager;
import com.metrogenesis.structurize.util.ChangeStorage;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * 建筑放置请求 — 客户端→服务端
 * <p>
 * 1. 在目标位置放置设施方块（如市政厅）
 * 2. 在旁边偏移位置放置施工标记（塔吊），带全息投影
 * 3. Builder 围着设施方块搭建结构
 * <p>
 * Phase 0d-4: 增加 RotationMirror 字段 + ChangeStorage 撤销记录
 */
public class BuildToolPlacementMessage
{
    private final BlockPos pos;
    private final String buildingTypeId;
    private final RotationMirror rotationMirror;

    /** 施工标记相对于设施位置的偏移量 */
    private static final int CRANE_OFFSET = 7;

    public BuildToolPlacementMessage(BlockPos pos, String buildingTypeId)
    {
        this(pos, buildingTypeId, RotationMirror.NONE);
    }

    public BuildToolPlacementMessage(BlockPos pos, String buildingTypeId, RotationMirror rotationMirror)
    {
        this.pos = pos;
        this.buildingTypeId = buildingTypeId;
        this.rotationMirror = rotationMirror;
    }

    // ══ 序列化 ═══════════════════════════════════════════

    public static void encode(BuildToolPlacementMessage msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.buildingTypeId);
        buf.writeInt(msg.rotationMirror.ordinal());
    }

    public static BuildToolPlacementMessage decode(FriendlyByteBuf buf)
    {
        BlockPos pos = buf.readBlockPos();
        String typeId = buf.readUtf();
        RotationMirror rotMir = RotationMirror.values()[buf.readInt()];
        return new BuildToolPlacementMessage(pos, typeId, rotMir);
    }

    // ══ 处理 ═════════════════════════════════════════════

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

            // 1. 检查位置是否可用
            if (!level.getBlockState(buildPos).isAir()) {
                player.sendSystemMessage(Component.literal("\u00A7c\u8BE5\u4F4D\u7F6E\u5DF2\u6709\u65B9\u5757"));
                return;
            }

            // 初始化撤销追踪器
            ChangeStorage changeStorage = new ChangeStorage(
                    Component.translatable("metrogenesis.placement", type.getDisplayName()),
                    player.getUUID());

            // 2. 放置设施方块（建筑的"核心"）
            Block facilityBlock = ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.tryParse("metrogenesis:" + typeId));
            if (facilityBlock == null) {
                player.sendSystemMessage(Component.literal("\u00A7c\u627E\u4E0D\u5230\u8BBE\u65BD\u65B9\u5757 " + typeId));
                return;
            }
            changeStorage.addPreviousDataFor(buildPos, level);
            level.setBlock(buildPos, facilityBlock.defaultBlockState(), 3);
            changeStorage.addPostDataFor(buildPos, level);

            // 3. 在旁边放置施工标记（塔吊位置）
            BlockPos cranePos = buildPos.offset(CRANE_OFFSET, 0, 0);
            // 找一个地面上的空位
            for (int dx = CRANE_OFFSET; dx >= 3; dx--) {
                BlockPos test = buildPos.offset(dx, 0, 0);
                if (level.getBlockState(test).isAir()) {
                    cranePos = test;
                    break;
                }
            }
            changeStorage.addPreviousDataFor(cranePos, level);
            level.setBlock(cranePos, MetroGenesis.CONSTRUCTION_MARKER_BLOCK.get().defaultBlockState(), 3);
            changeStorage.addPostDataFor(cranePos, level);

            // 4. 初始化施工标记（关联到设施位置）
            BlockEntity be = level.getBlockEntity(cranePos);
            if (be instanceof ConstructionMarkerBlockEntity te)
            {
                te.startConstructionWithFacility(typeId, buildPos, radius);
                player.sendSystemMessage(Component.literal(
                        "\u00A7a\u5DF2\u653E\u7F6E\u00A7e" + type.getDisplayName() + " \u00A77\u65BD\u5DE5\u5854\u540A [rot=" + msg.rotationMirror + "]"));
                MetroGenesis.LOGGER.info("[Placement] {} 设施 at {}, 塔吊 at {}, rot={}",
                        typeId, buildPos.toShortString(), cranePos.toShortString(), msg.rotationMirror);
            } else {
                // 施工标记放置失败，回滚
                level.destroyBlock(buildPos, false);
                level.destroyBlock(cranePos, false);
                player.sendSystemMessage(Component.literal("\u00A7c\u65BD\u5DE5\u6807\u8BB0\u521D\u59CB\u5316\u5931\u8D25"));
                return;
            }

            // 5. 提交撤销记录
            changeStorage.resetUnRedo();
            Manager.addToUndoRedoCache(changeStorage);
            MetroGenesis.LOGGER.info("[Placement] undo record submitted ({} blocks)", changeStorage.getID());
        });
        ctx.get().setPacketHandled(true);
    }
}
