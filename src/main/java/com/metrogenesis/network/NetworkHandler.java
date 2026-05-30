package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 缃戠粶閫氶亾娉ㄥ唽鍣?鈥?鍊熼壌 BlockUI/common/network 妯″紡
 * <p>
 * 浣跨敤 {@link com.metrogenesis.network.messages} 鍖呬腑鐨勬秷鎭熀绫荤畝鍖栨敞鍐屻€? */
public class NetworkHandler
{
    private static final String PROTOCOL_VERSION = "1.0";

    public static SimpleChannel CHANNEL;

    private static int packetId = 0;

    public static final int HOLOGRAM_SYNC_ID = nextId();
    public static final int BUILD_TOOL_PLACEMENT_ID = nextId();

    private static int nextId() { return packetId++; }

    /**
     * 鍒濆鍖栫綉缁滈€氶亾
     */
    public static void init()
    {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MetroGenesis.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        // 鈹€鈹€ 娉ㄥ唽娑堟伅 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

        CHANNEL.registerMessage(
            HOLOGRAM_SYNC_ID,
            HologramSyncPacket.class,
            HologramSyncPacket::toBytes,
            buf -> { var p = new HologramSyncPacket(); p.fromBytes(buf); return p; },
            (msg, ctx) -> ((HologramSyncPacket) msg).handle(ctx.get())
        );

        CHANNEL.registerMessage(
            BUILD_TOOL_PLACEMENT_ID,
            BuildToolPlacementMessage.class,
            BuildToolPlacementMessage::encode,
            BuildToolPlacementMessage::decode,
            BuildToolPlacementMessage::handle
        );

        MetroGenesis.LOGGER.info("[Network] 閫氶亾鍒濆鍖栧畬鎴愶紝鍏?{} 涓寘", packetId);
    }

    /**
     * 鍙戦€佸叏鎭浘鍚屾鍖呯粰鐜╁
     */
    public static void sendHologramSync(ServerPlayer player, HologramSyncPacket packet)
    {
        if (CHANNEL != null && player != null)
        {
            Connection connection = player.connection.connection;
            CHANNEL.sendTo(packet, connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    /**
     * 鍙戦€佸叏鎭浘鍚屾鍖呯粰鎵€鏈夌帺瀹?     */
    public static void sendToAll(ServerPlayer player, HologramSyncPacket packet)
    {
        for (var p : player.server.getPlayerList().getPlayers())
        {
            if (p.connection != null)
            {
                CHANNEL.sendTo(packet, p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }
}
