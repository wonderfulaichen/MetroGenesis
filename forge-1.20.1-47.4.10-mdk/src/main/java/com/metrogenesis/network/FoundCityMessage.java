package com.metrogenesis.network;

import com.metrogenesis.colony.ColonyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ① 开城 / 重命名（市长书 UI → 服务端）。
 * 未开城时调用 {@link ColonyState#foundCity} 确立归属；已开城时仅改名（保留核心/风格）。
 */
public class FoundCityMessage
{
    private final String name;
    private final String stylePack; // 可能为 null

    public FoundCityMessage(String name, String stylePack) {
        this.name = name;
        this.stylePack = stylePack;
    }

    public static void encode(FoundCityMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeBoolean(msg.stylePack != null);
        if (msg.stylePack != null) buf.writeUtf(msg.stylePack);
    }

    public static FoundCityMessage decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String style = buf.readBoolean() ? buf.readUtf() : null;
        return new FoundCityMessage(name, style);
    }

    public static void handle(FoundCityMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ColonyState state = ColonyState.get(player.serverLevel());
            if (!state.isFounded()) {
                state.foundCity(msg.name, msg.stylePack, player.blockPosition());
                player.sendSystemMessage(Component.literal("\u00A7a城市『" + state.getCityName() + "』已创立，市民正在迁入…"));
            } else {
                state.setCityName(msg.name);
                if (msg.stylePack != null) state.setActiveStylePack(msg.stylePack);
                player.sendSystemMessage(Component.literal("\u00A7a城市已重命名为『" + state.getCityName() + "』"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
