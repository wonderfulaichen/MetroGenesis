package com.metrogenesis.network;

import com.metrogenesis.construction.ZoneBuilder;
import com.metrogenesis.construction.ZoneRect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ② 区划生长请求（市长书 G 键 → 服务端）。
 * <p>
 * 携带单个 ZoneRect 的 9 字段（toIntArray / fromIntArray），
 * 服务端收到后调用 {@link ZoneBuilder#startGrowth(ZoneRect, net.minecraft.server.level.ServerLevel)}
 * 启动逐 tick 放置管线。
 */
public class RequestZoneGrowthMessage
{
    private final int[] zoneData; // 9 ints: minX,minZ,maxX,maxZ,zoneType,direction,stage,density,ownership

    public RequestZoneGrowthMessage(ZoneRect zone) {
        this.zoneData = zone.toIntArray();
    }

    public RequestZoneGrowthMessage(int[] zoneData) {
        this.zoneData = zoneData;
    }

    public static void encode(RequestZoneGrowthMessage msg, FriendlyByteBuf buf) {
        for (int v : msg.zoneData) buf.writeInt(v);
    }

    public static RequestZoneGrowthMessage decode(FriendlyByteBuf buf) {
        int[] data = new int[9];
        for (int i = 0; i < 9; i++) data[i] = buf.readInt();
        return new RequestZoneGrowthMessage(data);
    }

    public static void handle(RequestZoneGrowthMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ZoneRect zone = ZoneRect.fromIntArray(msg.zoneData);
            ZoneBuilder.getInstance().startGrowth(zone, player.serverLevel());
        });
        ctx.get().setPacketHandled(true);
    }
}
