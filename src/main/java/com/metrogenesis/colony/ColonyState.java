package com.metrogenesis.colony;

import com.metrogenesis.Config;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.construction.ConstructionManager;
import com.metrogenesis.workorder.WorkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 鍩庡競鍏ㄥ眬鏁版嵁 鈥?闄勫姞鍒颁笘鐣屽瓨妗ｏ紝瀛?璇昏嚜鍔ㄨ繘琛? * <p>
 * 鍖呭惈鍩庡競璧勯噾銆佷汉鍙ｃ€佸競姘戠鐞嗐€佹柦宸ョ鐞嗐€佸伐鍗曠鐞嗙瓑銆? */
public class ColonyState extends SavedData {

    private static final String DATA_NAME = "MetroGenesis_colony";

    private int funds;
    private int population;
    private int citizenCapacity = 3; // 鍒濆鍙绾?3 浜猴紝姣忓缓涓€涓缓绛?+1
    private String cityName = "鍩庨偊";
    private final List<UUID> citizenUUIDs = new ArrayList<>();

    /** 甯傛皯绠＄悊鍣紙鏂颁綋绯伙級 */
    private CitizenManager citizenManager;

    private ConstructionManager constructionManager;
    private WorkManager workManager;
    private ServerLevel level; // 缁戝畾鐨勪笘鐣?
    /** 甯傛斂鍘呬綅缃紙寤烘垚鍚庤褰曪紝鐢ㄤ簬甯傛皯鍑虹敓鐐圭瓑锛?*/
    private BlockPos townHallPos = BlockPos.ZERO;

    public ColonyState() {
        this.funds = Config.cityStartingFunds;
        this.population = 0;
    }

    /**
     * 缁戝畾涓栫晫锛堟瘡娆″姞杞芥椂璁剧疆锛?     */
    public void setLevel(ServerLevel level) {
        this.level = level;
        if (citizenManager == null) {
            this.citizenManager = new CitizenManager(this);
        }
        if (constructionManager == null) {
            this.constructionManager = new ConstructionManager(level);
        }
        if (workManager == null) {
            this.workManager = new WorkManager();
        }
    }

    // 鈹€鈹€ 宸ュ巶鏂规硶 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public static ColonyState get(ServerLevel level) {
        ColonyState state = level.getDataStorage()
                .computeIfAbsent(ColonyState::load, ColonyState::new, DATA_NAME);
        state.setLevel(level); // 姣忔鑾峰彇鏃舵洿鏂?level 寮曠敤
        return state;
    }

    public static ColonyState load(CompoundTag tag) {
        ColonyState state = new ColonyState();
        state.funds = tag.getInt("Funds");
        state.population = tag.getInt("Population");
        state.citizenCapacity = tag.getInt("CitizenCapacity");
        if (state.citizenCapacity < 3) state.citizenCapacity = 3;
        state.cityName = tag.getString("CityName");
        if (tag.contains("TownHallPos")) {
            state.townHallPos = NbtUtils.readBlockPos(tag.getCompound("TownHallPos"));
        }

        // 杩佺Щ鏃ф暟鎹紙CitizenUUIDs 鈫?鏂颁綋绯伙級
        if (tag.contains("CitizenManager")) {
            state.citizenManager = new CitizenManager(state);
            state.citizenManager.load(tag.getCompound("CitizenManager"));
        } else {
            // 鏃ф牸寮忓吋瀹癸細浠?CitizenUUIDs 鎭㈠
            ListTag uuidList = tag.getList("CitizenUUIDs", Tag.TAG_STRING);
            for (int i = 0; i < uuidList.size(); i++) {
                state.citizenUUIDs.add(UUID.fromString(uuidList.getString(i)));
            }
        }

        // 鎭㈠鏂藉伐鏁版嵁
        if (tag.contains("ConstructionManager")) {
            state.constructionManager = new ConstructionManager(null);
            state.constructionManager.load(tag.getCompound("ConstructionManager"));
        }
        if (tag.contains("WorkManager")) {
            state.workManager = new WorkManager();
            state.workManager.load(tag.getCompound("WorkManager"));
        }

        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Funds", this.funds);
        tag.putInt("Population", this.population);
        tag.putInt("CitizenCapacity", this.citizenCapacity);
        tag.putString("CityName", this.cityName);
        tag.put("TownHallPos", NbtUtils.writeBlockPos(this.townHallPos));

        // 甯傛皯绠＄悊鍣ㄦ寔涔呭寲
        if (citizenManager != null) {
            tag.put("CitizenManager", citizenManager.save());
        }

        // 鏃ф牸寮忓吋瀹癸紙淇濈暀锛?
        ListTag uuidList = new ListTag();
        for (UUID uuid : citizenUUIDs) {
            uuidList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("CitizenUUIDs", uuidList);

        if (constructionManager != null) {
            tag.put("ConstructionManager", constructionManager.save());
        }
        if (workManager != null) {
            tag.put("WorkManager", workManager.save());
        }
        return tag;
    }

    // 鈹€鈹€ tick 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 姣?tick 璋冪敤锛岄┍鍔ㄥ煄甯傚瓙绯荤粺
     */
    public void tick(ServerLevel level) {
        this.level = level;
        if (citizenManager != null) {
            citizenManager.onColonyTick(level);
        }
        if (constructionManager != null) {
            constructionManager.tick();
        }
    }

    // 鈹€鈹€ 绠＄悊鍣ㄨ闂?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CitizenManager getCitizenManager() {
        return citizenManager;
    }

    public ConstructionManager getConstructionManager() {
        return constructionManager;
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    // 鈹€鈹€ 甯傛皯娉ㄥ唽锛堟棫鍏煎锛?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public void registerCitizen(UUID uuid) {
        if (!citizenUUIDs.contains(uuid)) {
            citizenUUIDs.add(uuid);
            population = citizenUUIDs.size();
            setDirty();
        }
    }

    public List<UUID> getCitizenUUIDs() {
        return citizenUUIDs;
    }

    @Override
    public void setDirty() {
        super.setDirty();
    }

    // 鈹€鈹€ 璧勯噾绠＄悊 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public int getFunds() { return funds; }

    public void setFunds(int funds) {
        this.funds = funds;
        setDirty();
    }

    public void addToTreasury(int amount) {
        this.funds += amount;
        setDirty();
    }

    /** 鏀嚭锛岃繑鍥炴槸鍚︽垚鍔?*/
    public boolean spend(int amount) {
        if (funds >= amount) {
            funds -= amount;
            setDirty();
            return true;
        }
        return false;
    }

    // 鈹€鈹€ 浜哄彛 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public int getPopulation() { return population; }

    public String getCityName() { return cityName; }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        setDirty();
    }

    // 鈹€鈹€ 浜哄彛瀹归噺 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public int getMaxPopulation() { return citizenCapacity; }

    /** 姣忓缓涓€涓缓绛戯紝澧炲姞浜哄彛瀹归噺 */
    public void increaseCapacity() {
        this.citizenCapacity++;
        setDirty();
    }

    // 鈹€鈹€ 甯傛斂鍘?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public boolean hasTownHall() {
        return townHallPos != null && !townHallPos.equals(BlockPos.ZERO);
    }

    public BlockPos getTownHallPos() {
        return hasTownHall() ? townHallPos : (level != null ? level.getSharedSpawnPos() : BlockPos.ZERO);
    }

    public void setTownHallPos(BlockPos pos) {
        this.townHallPos = pos;
        setDirty();
    }
}
