package com.metrogenesis.network.messages;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 鎶借薄缃戠粶娑堟伅鍩虹被 鈥?鍊熼壌 MineColonies BlockUI 鐨勫眰绾фā寮? * <p>
 * 鏇夸唬鎵嬪姩鍦ㄦ瘡涓?packet 涓啓 enqueueWork + Supplier銆? * 涓ょ娑堟伅绔細
 * <ul>
 *   <li>{@link AbstractPlayMessage} 鈥?鍙屽悜锛堝鎴风+鏈嶅姟绔潎鍙敹鍒帮級</li>
 *   <li>{@link AbstractClientMessage} 鈥?浠呭鎴风鏀跺埌锛堟湇鍔＄鈫掑鎴风锛?/li>
 *   <li>{@link AbstractServerMessage} 鈥?浠呮湇鍔＄鏀跺埌锛堝鎴风鈫掓湇鍔＄锛?/li>
 * </ul>
 */
public abstract class AbstractPlayMessage
{
    /**
     * 搴忓垪鍖栧埌缂撳啿鍖?     */
    protected abstract void toBytes(final FriendlyByteBuf buf);

    /**
     * 浠庣紦鍐插尯鍙嶅簭鍒楀寲锛堝簲璋冪敤鏃犲弬鏋勯€?+ 姝ゆ柟娉曪級
     */
    protected abstract void fromBytes(final FriendlyByteBuf buf);

    /**
     * 瀹㈡埛绔墽琛?     */
    protected abstract void onClient(final Player player);

    /**
     * 鏈嶅姟绔墽琛?     */
    protected abstract void onServer(final ServerPlayer player);

    /**
     * 缁熶竴澶勭悊鍣?     */
    public void handle(final Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            final Player player = ctx.get().getSender();
            if (ctx.get().getDirection().getReceptionSide().isClient())
            {
                // 鏈嶅姟绔啋瀹㈡埛绔?                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> onClient(net.minecraft.client.Minecraft.getInstance().player));
            }
            else
            {
                // 瀹㈡埛绔啋鏈嶅姟绔?
                if (player instanceof ServerPlayer sp)
                {
                    onServer(sp);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
