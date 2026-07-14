package com.metrogenesis.gov;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.core.economy.MarketData;
import org.jetbrains.annotations.Nullable;

/**
 * CityState — ColonyState 的轻量 Facade，供部门系统使用。
 * <p>
 * 不在 {@link Department} 接口中直接暴露 ColonyState，遵循接口隔离原则。
 * 部门系统仅通过此 Facade 访问城市核心数据（国库、人口、市场）。
 */
public class CityState {

    private final ColonyState colony;
    private final EconomyEngine economy;

    public CityState(ColonyState colony, EconomyEngine economy) {
        this.colony = colony;
        this.economy = economy;
    }

    // ══ ColonyState 委派 ════════════════════════════

    /** @return 当前国库余额（C-Value） */
    public int getFunds() { return colony.getFunds(); }

    /** 设置国库余额 */
    public void setFunds(int funds) { colony.setFunds(funds); }

    /**
     * 从国库支出。如果余额不足则返回 false。
     *
     * @param amount 支出金额
     * @return 是否成功扣款
     */
    public boolean spend(int amount) { return colony.spend(amount); }

    /** 向国库存入 */
    public void addToTreasury(int amount) { colony.addToTreasury(amount); }

    /** @return 当前游戏天数（从经济引擎获取） */
    public int getDay() { return (int) economy.getDayCounter(); }

    // ══ Economy 委派 ════════════════════════════════

    /** @return 经济引擎实例 */
    public EconomyEngine getEconomyEngine() { return economy; }

    /** @return 市场数据（价格/供需） */
    public MarketData getMarketData() { return economy.getMarketData(); }

    /** @return 当前城市使用的建筑风格包名 */
    @Nullable
    public String getActiveStylePack() { return colony.getActiveStylePack(); }

    /** 设置当前城市的建筑风格包 */
    public void setActiveStylePack(@Nullable String packName) { colony.setActiveStylePack(packName); }

    // ══ 人口快捷 ════════════════════════════════════

    /** @return 当前城市人口 */
    public int getPopulation() { return colony.getPopulation(); }
}
