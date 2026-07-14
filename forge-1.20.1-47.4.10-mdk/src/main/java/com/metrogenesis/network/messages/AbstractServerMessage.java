package com.metrogenesis.network.messages;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 浠呮湇鍔＄娑堟伅鍩虹被锛堝鎴风鈫掓湇鍔＄锛? */
public abstract class AbstractServerMessage
{
    protected abstract void toBytes(final FriendlyByteBuf buf);
    protected abstract void fromBytes(final FriendlyByteBuf buf);
    protected abstract void onExecute(final ServerPlayer player);

    public void handle(final net.minecraftforge.network.NetworkEvent.Context ctx)
    {
        ctx.enqueueWork(() -> {
            final ServerPlayer player = ctx.getSender();
            if (player != null) onExecute(player);
        });
        ctx.setPacketHandled(true);
    }
}
