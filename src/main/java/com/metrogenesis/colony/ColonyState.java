package com.metrogenesis.colony;

import com.metrogenesis.Config;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.construction.ConstructionManager;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.core.economy.MarketData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

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

    public ColonyState() {
        this.funds = Config.cityStartingFunds;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
        if (economyEngine == null) {
            this.economyEngine = new EconomyEngine(new MarketData());
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
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Funds", this.funds);
        return tag;
    }

    public void tick(ServerLevel level) {
        this.level = level;
        if (economyEngine != null) {
            economyEngine.tick();
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

    // ══ Citizen Management ════════════════════════════

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
        if (townHallPos != null) {
            return "城邦 " + townHallPos.toShortString();
        }
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
}
