package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v2.BlueprintLibraryData;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.structurize.util.RotationMirror;
import com.metrogenesis.util.BlueprintUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: 玩家确认保存蓝图 → 服务端扫描区域并保存到蓝图库。
 * <p>
 * v2: 新增 {@link #rotationMirror} 字段，保存时先应用旋转变换再存档。
 */
public class CaptureBlueprintPacket
{
    private final BlockPos start;
    private final BlockPos end;
    private final String name;
    private final RotationMirror rotationMirror;

    public CaptureBlueprintPacket(BlockPos start, BlockPos end, String name, RotationMirror rotationMirror)
    {
        this.start = start;
        this.end = end;
        this.name = name;
        this.rotationMirror = rotationMirror;
    }

    public static void encode(CaptureBlueprintPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.start);
        buf.writeBlockPos(msg.end);
        buf.writeUtf(msg.name, 64);
        buf.writeEnum(msg.rotationMirror);
    }

    public static CaptureBlueprintPacket decode(FriendlyByteBuf buf)
    {
        return new CaptureBlueprintPacket(
            buf.readBlockPos(),
            buf.readBlockPos(),
            buf.readUtf(64),
            buf.readEnum(RotationMirror.class)
        );
    }

    public static void handle(CaptureBlueprintPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 只允许生存模式/创造模式玩家
            if (player.isSpectator()) return;

            ServerLevel world = player.serverLevel();

            // 校验距离（防止作弊扫描远处建筑）
            double dist = Math.sqrt(msg.start.distToCenterSqr(player.getX(), player.getY(), player.getZ()));
            if (dist > 64) return; // 最大扫描半径 64 格

            // 执行扫描
            Blueprint bp = BlueprintUtil.scanRegion(world, msg.start, msg.end, msg.name);
            int blockCount = bp.getBlockInfoAsList().size();

            if (blockCount == 0)
            {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c该区域没有可扫描的方块！"), false);
                return;
            }

            // 应用旋转/镜像后再保存
            if (msg.rotationMirror != RotationMirror.NONE)
            {
                bp.setRotationMirror(msg.rotationMirror);
                blockCount = bp.getBlockInfoAsList().size();
            }

            // 保存到蓝图库
            BlueprintLibraryData library = BlueprintLibraryData.get(world);
            library.addBlueprint(msg.name, bp);

            MetroGenesis.LOGGER.info("[Blueprint] {} saved '{}' ({} blocks, {}×{}×{}, rot={})",
                player.getName().getString(), msg.name, blockCount,
                bp.getSizeX(), bp.getSizeY(), bp.getSizeZ(), msg.rotationMirror);

            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "§a蓝图保存成功: §e" + msg.name + " §7(" + blockCount + " 方块, " + msg.rotationMirror + ")"), false);
        });
        ctx.get().setPacketHandled(true);
    }
}
