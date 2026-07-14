package com.metrogenesis.network.messages;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 浠呭鎴风娑堟伅鍩虹被锛堟湇鍔＄鈫掑鎴风锛? */
public abstract class AbstractClientMessage
{
    protected abstract void toBytes(final FriendlyByteBuf buf);
    protected abstract void fromBytes(final FriendlyByteBuf buf);
    protected abstract void onExecute(final Player player);

    public void handle(final net.minecraftforge.network.NetworkEvent.Context ctx)
    {
        ctx.enqueueWork(() -> {
            final Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) onExecute(player);
        });
        ctx.setPacketHandled(true);
    }
}
