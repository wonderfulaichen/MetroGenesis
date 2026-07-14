package com.metrogenesis.network;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.core.economy.MarketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * 城市数据同步网络包。
 * 客户端 → 服务端：请求数据
 * 服务端 → 客户端：响应 CityDemandsData
 */
public class SyncCityDataMessage
{
    private final CompoundTag dataNBT;

    public SyncCityDataMessage()
    {
        this.dataNBT = null; // 请求包
    }

    public SyncCityDataMessage(CityDemandsData data)
    {
        this.dataNBT = data != null ? data.toNBT() : null;
    }

    // ══ 编码 / 解码 ════════════════════════════════════

    public static void encode(SyncCityDataMessage msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.dataNBT != null);
        if (msg.dataNBT != null)
        {
            buf.writeNbt(msg.dataNBT);
        }
    }

    public static SyncCityDataMessage decode(FriendlyByteBuf buf)
    {
        if (buf.readBoolean())
        {
            CompoundTag tag = buf.readNbt();
            return new SyncCityDataMessage(CityDemandsData.fromNBT(tag));
        }
        return new SyncCityDataMessage(); // 请求包
    }

    // ══ 处理 ═══════════════════════════════════════════

    public static void handle(SyncCityDataMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            final ServerPlayer player = ctx.get().getSender();
            if (player == null)
            {
                // 服务端 → 客户端：客户端接收数据
                handleClientResponse(msg);
                return;
            }

            // 客户端 → 服务端：服务端生成数据并回复
            handleServerRequest(msg, player);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleServerRequest(SyncCityDataMessage msg, ServerPlayer player)
    {
        final ServerLevel level = player.serverLevel();
        final ColonyState colony = ColonyState.get(level);

        final CityDemandsData data = new CityDemandsData();
        data.cityName = colony.getCityName();
        data.hasTownHall = colony.hasTownHall();
        data.funds = colony.getFunds();
        data.population = colony.getPopulation();
        data.maxPopulation = colony.getMaxPopulation();

        // 施工地块
        final var cm = colony.getConstructionManager();
        if (cm != null)
        {
            data.activeConstructionSites = cm.getActiveSites().size();
            data.unclaimedSites = cm.getUnclaimedSites().size();
        }

        // 经济数据
        final EconomyEngine economy = colony.getEconomyEngine();
        if (economy != null)
        {
            final MarketData market = economy.getMarketData();
            if (market != null)
            {
                // 收集所有商品
                for (final ResourceLocation itemId : market.getAllItems())
                {
                    final long prod = market.getCurrentProduction(itemId);
                    final long cons = market.getCurrentConsumption(itemId);
                    final double price = market.getSmoothedPrice(itemId);
                    final String name = itemId.getPath(); // 简化显示

                    // 按消耗量排序
                    data.topItems.add(new CityDemandsData.ItemSnapshot(itemId, name, prod, cons, price));

                    // 赤字：消耗 > 产出
                    if (cons > prod)
                    {
                        data.deficitItems.add(new CityDemandsData.ItemSnapshot(itemId, name, prod, cons, price));
                    }
                }

                // 按消耗排序
                data.topItems.sort(Comparator.<CityDemandsData.ItemSnapshot>comparingLong(
                    s -> s.consumption).reversed());
                // 赤字按缺口排序
                data.deficitItems.sort(Comparator.<CityDemandsData.ItemSnapshot>comparingLong(
                    s -> s.consumption - s.production).reversed());
            }
        }

        // 回复客户端
        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncCityDataMessage(data)
        );
    }

    /** 客户端缓存接收到的数据 */
    private static CityDemandsData cachedData = null;

    private static void handleClientResponse(SyncCityDataMessage msg)
    {
        if (msg.dataNBT != null)
        {
            cachedData = CityDemandsData.fromNBT(msg.dataNBT);
        }
    }

    /** 获取缓存的城市数据（客户端调用） */
    public static CityDemandsData getCachedData()
    {
        return cachedData;
    }

    /** 请求刷新城市数据（客户端调用） */
    public static void requestRefresh()
    {
        cachedData = null;
        NetworkHandler.CHANNEL.sendToServer(new SyncCityDataMessage());
    }
}
