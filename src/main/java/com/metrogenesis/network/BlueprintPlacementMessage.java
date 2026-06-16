package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.blueprint.v2.BlueprintConverter;
import com.metrogenesis.blueprint.v2.BlueprintLibraryData;
import com.metrogenesis.structurize.placement.StructurePlacementUtils;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 蓝图放置请求 — 客户端→服务端（图鉴面板"放置"按钮触发）
 * <p>
 * 从 {@link BlueprintLibraryData} 按名称查找蓝图，
 * 通过 {@link BlueprintConverter} 转换为 structurize Blueprint，
 * 最终调用 {@link StructurePlacementUtils#loadAndPlaceStructureWithRotation} 放置。
 * <p>
 * <b>注册 ID</b>：2（在 {@link NetworkHandler} 中注册，紧接 id=0 CaptureBlueprintPacket 和 id=1 BuildToolPlacementMessage）
 */
public class BlueprintPlacementMessage
{
    private final String blueprintName;
    private final BlockPos pos;
    private final RotationMirror rotationMirror;

    /**
     * @param blueprintName 蓝图名称（在 BlueprintLibraryData 中的查找键）
     * @param pos           放置位置（世界坐标，建筑的锚点）
     * @param rotationMirror 旋转/镜像状态
     */
    public BlueprintPlacementMessage(String blueprintName, BlockPos pos, RotationMirror rotationMirror)
    {
        this.blueprintName = blueprintName;
        this.pos = pos;
        this.rotationMirror = rotationMirror;
    }

    // ═══════════════════════════════════════════════════════════
    //  Serialization
    // ═══════════════════════════════════════════════════════════

    public static void encode(BlueprintPlacementMessage msg, FriendlyByteBuf buf)
    {
        buf.writeUtf(msg.blueprintName);
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.rotationMirror.ordinal());
    }

    public static BlueprintPlacementMessage decode(FriendlyByteBuf buf)
    {
        String name = buf.readUtf();
        BlockPos pos = buf.readBlockPos();
        int rotOrd = buf.readInt();
        RotationMirror rotMir = RotationMirror.values()[rotOrd];
        return new BlueprintPlacementMessage(name, pos, rotMir);
    }

    // ═══════════════════════════════════════════════════════════
    //  Server Handler
    // ═══════════════════════════════════════════════════════════

    public static void handle(BlueprintPlacementMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.isClientSide) return;

            // 1. 从蓝图库查找
            if (!(level instanceof ServerLevel serverLevel))
            {
                player.sendSystemMessage(Component.literal("\u00A7c无法访问服务端世界"));
                return;
            }

            BlueprintLibraryData library = BlueprintLibraryData.get(serverLevel);
            Blueprint v1Bp = library.getBlueprint(msg.blueprintName);

            if (v1Bp == null)
            {
                player.sendSystemMessage(Component.literal(
                    "\u00A7c蓝图不存在: " + msg.blueprintName));
                MetroGenesis.LOGGER.warn("[BlueprintPlacement] Blueprint not found: {}", msg.blueprintName);
                return;
            }

            // 2. 转换 v1 → structurize
            com.metrogenesis.structurize.blueprints.v1.Blueprint structBp =
                BlueprintConverter.convert(v1Bp);

            if (structBp == null)
            {
                player.sendSystemMessage(Component.literal(
                    "\u00A7c蓝图转换失败: " + msg.blueprintName));
                return;
            }

            // 3. 应用旋转/镜像到 structurize 蓝图
            if (msg.rotationMirror != RotationMirror.NONE)
            {
                structBp.setRotationMirror(msg.rotationMirror, level);
            }

            // 4. 调用 structurize 放置管线（进入 Manager 队列，逐块施工）
            StructurePlacementUtils.loadAndPlaceStructureWithRotation(
                level,
                structBp,
                msg.pos,
                msg.rotationMirror.rotation(),
                msg.rotationMirror.mirror(),
                true, // fancyPlacement = true（逐步放置，有视觉效果）
                player
            );

            player.sendSystemMessage(Component.literal(
                "\u00A7a已放置蓝图 \u00A7e" + msg.blueprintName
                + " \u00A77at " + msg.pos.toShortString()
                + " \u00A77rot=" + msg.rotationMirror));

            MetroGenesis.LOGGER.info("[BlueprintPlacement] Player {} placed blueprint '{}' at {} rot={}",
                player.getName().getString(), msg.blueprintName,
                msg.pos.toShortString(), msg.rotationMirror);
        });
        ctx.get().setPacketHandled(true);
    }
}
