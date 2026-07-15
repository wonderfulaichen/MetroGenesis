package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.placement.StructurePlacementUtils;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 蓝图放置请求 — 客户端→服务端（图鉴面板"放置"按钮触发）
 * <p>
 * 从图鉴选中的 {@link com.metrogenesis.catalog.BuildingCatalogEntry} 携带 packName + resourcePath，
 * 服务端通过 {@link StructurePacks#getBlueprint(String, String, boolean)} 直接从文件加载蓝图，
 * 最终调用 {@link StructurePlacementUtils#loadAndPlaceStructureWithRotation} 放置。
 * <p>
 * <b>注册 ID</b>：2（在 {@link NetworkHandler} 中注册，紧接 id=0 CaptureBlueprintPacket 和 id=1 BuildToolPlacementMessage）
 */
public class BlueprintPlacementMessage
{
    private final String blueprintName;
    private final String packName;
    private final String resourcePath;
    private final BlockPos pos;
    private final RotationMirror rotationMirror;
    /** 建筑造价（C-Value），用于国库扣费 */
    private final long cost;

    /**
     * @param blueprintName 蓝图名称（仅用于显示/日志）
     * @param packName      风格包名称（如 "medievaloak"）
     * @param resourcePath  蓝图在风格包中的相对路径（如 "buildings/farm/farmer"）
     * @param pos           放置位置（世界坐标，建筑的锚点）
     * @param rotationMirror 旋转/镜像状态
     * @param cost          建筑造价（C-Value），0 表示免费
     */
    public BlueprintPlacementMessage(String blueprintName, String packName, String resourcePath,
                                     BlockPos pos, RotationMirror rotationMirror, long cost)
    {
        this.blueprintName = blueprintName;
        this.packName = packName;
        this.resourcePath = resourcePath;
        this.pos = pos;
        this.rotationMirror = rotationMirror;
        this.cost = cost;
    }

    // ═══════════════════════════════════════════════════════════
    //  Serialization
    // ═══════════════════════════════════════════════════════════

    public static void encode(BlueprintPlacementMessage msg, FriendlyByteBuf buf)
    {
        buf.writeUtf(msg.blueprintName);
        buf.writeUtf(msg.packName);
        buf.writeUtf(msg.resourcePath);
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.rotationMirror.ordinal());
        buf.writeLong(msg.cost);
    }

    public static BlueprintPlacementMessage decode(FriendlyByteBuf buf)
    {
        String name = buf.readUtf();
        String pack = buf.readUtf();
        String rp = buf.readUtf();
        BlockPos pos = buf.readBlockPos();
        int rotOrd = buf.readInt();
        RotationMirror rotMir = RotationMirror.values()[rotOrd];
        long cost = buf.readLong();
        return new BlueprintPlacementMessage(name, pack, rp, pos, rotMir, cost);
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

            // ── 国库扣费 ──
            if (msg.cost > 0) {
                ColonyState colony = ColonyState.get(player.serverLevel());
                if (!colony.spend((int) msg.cost)) {
                    player.sendSystemMessage(Component.literal(
                        "\u00A7c资金不足！需要 " + msg.cost + " C-Value，当前国库 " + colony.getFunds() + " C-Value"));
                    MetroGenesis.LOGGER.warn("[BlueprintPlacement] Insufficient funds for '{}': need {} have {}",
                        msg.blueprintName, msg.cost, colony.getFunds());
                    return;
                }
                MetroGenesis.LOGGER.info("[BlueprintPlacement] Charged {} C-Value for '{}', remaining {}",
                    msg.cost, msg.blueprintName, colony.getFunds());
            }

            // 1. 从 StructurePacks 加载蓝图（与图鉴预览使用相同数据源）
            final String fullPath = msg.resourcePath + ".blueprint";
            Blueprint structBp = StructurePacks.getBlueprint(msg.packName, fullPath, true);

            if (structBp == null)
            {
                player.sendSystemMessage(Component.literal(
                    "\u00A7c蓝图不存在: " + msg.blueprintName
                    + " (" + msg.packName + "/" + fullPath + ")"));
                MetroGenesis.LOGGER.warn("[BlueprintPlacement] Blueprint not found: {}/{}",
                    msg.packName, fullPath);
                return;
            }

            // 2. 应用旋转/镜像
            if (msg.rotationMirror != RotationMirror.NONE)
            {
                structBp.setRotationMirror(msg.rotationMirror, level);
            }

            // 3. 调用 structurize 放置管线（进入 Manager 队列，逐块施工）
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
                + " \u00A77by pack: " + msg.packName
                + " at " + msg.pos.toShortString()
                + " \u00A77rot=" + msg.rotationMirror));

            MetroGenesis.LOGGER.info("[BlueprintPlacement] Player {} placed blueprint '{}' (pack={}) at {} rot={}",
                player.getName().getString(), msg.blueprintName, msg.packName,
                msg.pos.toShortString(), msg.rotationMirror);
        });
        ctx.get().setPacketHandled(true);
    }
}
