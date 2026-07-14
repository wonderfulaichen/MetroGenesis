package com.metrogenesis.colony;

import com.metrogenesis.Config;
import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.construction.ConstructionManager;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.core.economy.MarketData;
import com.metrogenesis.gov.DepartmentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Colony global data — attached to world save.
 * Manages treasury and our custom EconomyEngine.
 * All colony management (citizens, buildings, etc.) is handled by MineColonies.
 */
public class ColonyState extends SavedData {

    private static final String DATA_NAME = "MetroGenesis_colony";

    private int funds;
    private EconomyEngine economyEngine;
    private ServerLevel level;
    private CitizenManager citizenManager;
    private BlockPos townHallPos;
    private ConstructionManager constructionManager;

    /** 内部 tick 计数器，用于部门每日结算触发判断 */
    private int tickCounter = 0;

    /** 当前城市使用的建筑风格包名（如 "Urban Savanna"），null = 未选择 */
    @Nullable
    private String activeStylePack = null;

    /** 城市是否已创立（市长书开城 / /foundcity）。未创立前不生成市民。 */
    private boolean founded = false;

    /** 玩家命名的城市名（覆盖从 townHallPos 派生的名称），null = 未命名 */
    @Nullable
    private String cityName = null;

    public ColonyState() {
        this.funds = Config.cityStartingFunds;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
        if (economyEngine == null) {
            // 复用全局单例经济引擎（由 EconomyEngine.getOrCreate 从持久化恢复），
            // 避免每次 get() 新建空引擎导致经济数据不持久化、且 /cvalue 读取的实例不一致。
            this.economyEngine = EconomyEngine.getInstance();
            if (this.economyEngine == null) {
                this.economyEngine = new EconomyEngine(new MarketData());
            }
        }
        if (constructionManager == null) {
            this.constructionManager = new ConstructionManager(level);
        }
    }

    public static ColonyState get(ServerLevel level) {
        ColonyState state = level.getDataStorage()
                .computeIfAbsent(ColonyState::load, ColonyState::new, DATA_NAME);
        state.setLevel(level);
        return state;
    }

    public static ColonyState load(CompoundTag tag) {
        ColonyState state = new ColonyState();
        state.funds = tag.getInt("Funds");
        state.tickCounter = tag.getInt("TickCounter");
        if (tag.contains("ActiveStylePack")) {
            state.activeStylePack = tag.getString("ActiveStylePack");
        }
        state.founded = tag.getBoolean("Founded");
        if (tag.contains("CityName")) {
            state.cityName = tag.getString("CityName");
        }
        // townHallPos 兼作"城市核心"坐标，开城时写入
        if (tag.contains("TownHallX")) {
            state.townHallPos = new BlockPos(
                    tag.getInt("TownHallX"), tag.getInt("TownHallY"), tag.getInt("TownHallZ"));
        }
        // G2 修复：恢复市民数据（避免重启后人口归零）
        // load 是静态工厂，此处新建的 state 即 CitizenManager 所需的 colony 引用
        if (tag.contains("Citizens")) {
            CitizenManager cm = new CitizenManager(state);
            cm.load(tag.getCompound("Citizens"));
            state.citizenManager = cm;
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Funds", this.funds);
        tag.putInt("TickCounter", this.tickCounter);
        if (this.activeStylePack != null) {
            tag.putString("ActiveStylePack", this.activeStylePack);
        }
        tag.putBoolean("Founded", this.founded);
        if (this.cityName != null) {
            tag.putString("CityName", this.cityName);
        }
        if (this.townHallPos != null) {
            tag.putInt("TownHallX", this.townHallPos.getX());
            tag.putInt("TownHallY", this.townHallPos.getY());
            tag.putInt("TownHallZ", this.townHallPos.getZ());
        }
        // G2 修复：持久化市民数据（CitizenManager.save 此前无调用方）
        tag.put("Citizens", getCitizenManager().save());
        return tag;
    }

    public void tick(ServerLevel level) {
        this.level = level;
        tickCounter++;
        if (economyEngine != null) {
            economyEngine.tick();
        }

        // 部门每日结算 — 与经济引擎结算周期对齐
        // 使用 Config.settlementInterval（默认 24000 ticks = 1 天）
        if (tickCounter > 0 && tickCounter % Config.settlementInterval == 0) {
            DepartmentManager mgr = DepartmentManager.getInstance();
            if (mgr != null) {
                mgr.tick();
            }
        }

        // ══ F1: 唤醒市民循环（市民生成/维护）═══
        // onColonyTick 内部要求 hasTownHall()（开城后置位 townHallPos）→ 未开城则不生成市民。
        // CitizenManager 懒创建，故直接取用即可。
        getCitizenManager().onColonyTick(level);

        // ══ R2（缺陷 C）：每日产销信号注入 ═════════════════════════════════
        // 与经济引擎日结算同节奏（settlementInterval），把"城市运转"翻译为供需事件写入市场，
        // 让 C-Value 价格随人口/建筑真正波动（此前市场无任何自研玩法调用方，价格恒为基准值）。
        if (tickCounter > 0 && tickCounter % Config.settlementInterval == 0) {
            com.metrogenesis.core.economy.EconomySignals.recordDailyColonyActivity(this, level);
        }
    }

    public EconomyEngine getEconomyEngine() {
        return economyEngine;
    }

    // ══ Treasury ═════════════════════════════════════

    public int getFunds() { return funds; }

    public void setFunds(int funds) {
        this.funds = funds;
        setDirty();
    }

    public void addToTreasury(int amount) {
        this.funds += amount;
        setDirty();
    }

    public boolean spend(int amount) {
        if (funds >= amount) {
            funds -= amount;
            setDirty();
            return true;
        }
        return false;
    }

    // ══ Citizen Management ══════════════════════════════

    public CitizenManager getCitizenManager() {
        if (citizenManager == null) {
            citizenManager = new CitizenManager(this);
        }
        return citizenManager;
    }

    public ConstructionManager getConstructionManager() {
        if (constructionManager == null && level != null) {
            constructionManager = new ConstructionManager(level);
        }
        return constructionManager;
    }

    public String getCityName() {
        if (cityName != null) return cityName;
        if (founded) return "新城邦";
        return "未命名";
    }

    public void setTownHallPos(BlockPos pos) {
        this.townHallPos = pos;
        setDirty();
    }

    public BlockPos getTownHallPos() {
        return townHallPos;
    }

    public boolean hasTownHall() {
        return townHallPos != null;
    }

    public int getMaxPopulation() {
        return 10;
    }

    public void increaseCapacity() {
        // 桩实现：增加人口容量（加3）
    }

    public int getPopulation() {
        return citizenManager != null ? citizenManager.getCurrentCitizenCount() : 0;
    }

    // ══ 开城（① 市长书 / /foundcity）══════════════════════

    /**
     * ① 开城：确立城市归属。
     * 设置城市名、建筑风格包，并把玩家所在位置登记为城市核心
     * （兼作 {@link #hasTownHall()} 的判定坐标与市民出生点）。
     *
     * @param name       玩家命名的城市名，null/空白表示使用默认名
     * @param stylePack  建筑风格包名（如 "Urban Savanna"），null 表示不指定
     * @param corePos    城市核心坐标（通常为玩家当前位置）
     */
    public void foundCity(@Nullable String name, @Nullable String stylePack, BlockPos corePos) {
        this.founded = true;
        this.cityName = (name != null && !name.isBlank()) ? name : null;
        this.activeStylePack = stylePack;
        this.townHallPos = corePos;
        setDirty();
        MetroGenesis.LOGGER.info("[Colony] 城市创立：{} (style={}, core={})",
                getCityName(), stylePack, corePos.toShortString());
    }

    /** 城市是否已创立（用于门禁判断，避免未开城就刷市民） */
    public boolean isFounded() {
        return founded;
    }

    /** 重命名城市（开城后也可改名，保留 founded/core/style）。 */
    public void setCityName(@Nullable String name) {
        this.cityName = (name != null && !name.isBlank()) ? name : null;
        setDirty();
    }

    // ══ Active Style Pack ═════════════════════════════════

    /**
     * @return 当前城市使用的建筑风格包名，null 表示未选择
     */
    @Nullable
    public String getActiveStylePack() { return activeStylePack; }

    /**
     * 设置城市建筑风格包。选择后所有自动建造使用此风格。
     * @param packName 风格包名（如 "Urban Savanna"）
     */
    public void setActiveStylePack(@Nullable String packName) {
        this.activeStylePack = packName;
        setDirty();
    }
}
