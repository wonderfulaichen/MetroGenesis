package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network channel — handles CaptureBlueprintPacket, BuildToolPlacementMessage, and future custom packets.
 */
public class NetworkHandler
{
    private static final String PROTOCOL_VERSION = "1.0";
    private static int id = 0;

    public static SimpleChannel CHANNEL;

    public static void init()
    {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MetroGenesis.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        // ══ 注册数据包 ═══════════════════════════════════
        CHANNEL.registerMessage(id++, CaptureBlueprintPacket.class,
            CaptureBlueprintPacket::encode,
            CaptureBlueprintPacket::decode,
            CaptureBlueprintPacket::handle);

        CHANNEL.registerMessage(id++, BuildToolPlacementMessage.class,
            BuildToolPlacementMessage::encode,
            BuildToolPlacementMessage::decode,
            BuildToolPlacementMessage::handle);

        // id=2: 蓝图放置请求（图鉴面板 → 服务端 structurize 放置管线）
        CHANNEL.registerMessage(id++, BlueprintPlacementMessage.class,
            BlueprintPlacementMessage::encode,
            BlueprintPlacementMessage::decode,
            BlueprintPlacementMessage::handle);

        MetroGenesis.LOGGER.info("[Network] channel initialized");
    }
}
