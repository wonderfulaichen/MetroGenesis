package com.metrogenesis.core.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 经济数据持久化 — 通过 DimensionSavedData 机制挂载到 overworld。
 * <p>
 * 存储 {@link MarketData}（价格缓存 + 周期计数），服务端重启后自动恢复。
 * <p>
 * 数据文件位置：{worldDir}/data/metrogenesis_economy.dat
 *
 * @see EconomyEngine#getOrCreate(ServerLevel)
 */
public class EconomySavedData extends SavedData
{
    private static final String DATA_NAME = "metrogenesis_economy";

    private MarketData marketData;

    // ========================================================================
    //  工厂方法
    // ========================================================================

    /**
     * 从 overworld 获取或创建经济数据。
     *
     * @param level 服务器世界（通常传入 overworld）
     * @return 经济数据实例
     */
    public static EconomySavedData get(final ServerLevel level)
    {
        return level.getDataStorage()
            .computeIfAbsent(EconomySavedData::load, EconomySavedData::new, DATA_NAME);
    }

    // ========================================================================
    //  序列化
    // ========================================================================

    @Override
    public CompoundTag save(final CompoundTag tag)
    {
        if (marketData != null)
        {
            tag.put("marketData", marketData.toNBT());
        }
        return tag;
    }

    /**
     * 从 NBT 反序列化。
     */
    public static EconomySavedData load(final CompoundTag tag)
    {
        final EconomySavedData data = new EconomySavedData();
        if (tag.contains("marketData"))
        {
            data.marketData = MarketData.fromNBT(tag.getCompound("marketData"));
        }
        else
        {
            data.marketData = new MarketData();
        }
        return data;
    }

    // ========================================================================
    //  访问器
    // ========================================================================

    public MarketData getMarketData()
    {
        if (marketData == null)
        {
            marketData = new MarketData();
        }
        return marketData;
    }

    public void setMarketData(final MarketData marketData)
    {
        this.marketData = marketData;
    }
}
