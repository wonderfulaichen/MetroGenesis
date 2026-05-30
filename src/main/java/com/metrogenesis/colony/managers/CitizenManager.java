package com.metrogenesis.colony.managers;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.colony.citizen.CitizenNameFile;
import com.metrogenesis.colony.citizen.CitizenNameListener;
import com.metrogenesis.citizen.types.CitizenData;
import com.metrogenesis.entity.MetroGenesisCitizen;
import com.metrogenesis.init.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 甯傛皯绠＄悊鍣?鈥?绠＄悊鍩庡競涓墍鏈夊競姘戠殑鐢熷懡鍛ㄦ湡
 * <p>
 * 鍙傝€?MineColonies {@code com.minecolonies.core.colony.managers.CitizenManager}
 */
public class CitizenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis-CitizenMgr");

    private static final String TAG_CITIZENS = "citizens";
    private static final String TAG_TOP_ID = "topCitizenId";

    /** 甯傛皯鏁版嵁鏄犲皠 id 鈫?CitizenData */
    @NotNull
    private final Map<Integer, CitizenData> citizens = new HashMap<>();

    /** 鏈€楂樺凡鐢ㄧ殑甯傛皯 ID */
    private int topCitizenId = 0;

    /** 鎵€灞炲煄甯?*/
    @NotNull
    private final ColonyState colony;

    /** 鍒濆甯傛皯鐢熸垚闂撮殧锛坱ick锛?*/
    private int respawnInterval = 600; // 30绉?= 600 ticks

    private final Random random = new Random();

    public CitizenManager(@NotNull final ColonyState colony) {
        this.colony = colony;
    }

    // 鈹€鈹€ 甯傛皯鍒涘缓 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 鍒涘缓骞舵敞鍐屾柊甯傛皯
     * <p>
     * 澶嶇敤绌虹己 ID锛堟壂鎻?1~N+1 鎵剧己鍙ｏ級
     */
    public CitizenData createAndRegisterCivilianData() {
        // 澶嶇敤绌虹己 ID
        for (int i = 1; i <= getCurrentCitizenCount() + 1; i++) {
            if (citizens.get(i) == null) {
                topCitizenId = i;
                break;
            }
        }

        final CitizenData citizenData = new CitizenData(topCitizenId);
        final CitizenNameFile nameFile = pickNameFile();
        if (nameFile != null) {
            citizenData.initForNewCitizen(random, nameFile);
        }
        citizens.put(citizenData.getId(), citizenData);
        colony.setDirty();
        LOGGER.debug("Created citizen #{}: {}", citizenData.getId(), citizenData.getName());
        return citizenData;
    }

    /**
     * 閫夋嫨涓€涓懡鍚嶆枃浠讹紙浼樺厛閫夊凡鏈夌殑锛岃嫢娌℃湁鍒欑敤榛樿锛?     */
    private CitizenNameFile pickNameFile() {
        final Map<String, CitizenNameFile> nameFiles = CitizenNameListener.getNameFileMap();
        if (nameFiles.isEmpty()) return null;

        // 濡傛灉鏈?chinese 鍜?default锛岄殢鏈洪€変竴涓?
        final List<String> keys = new ArrayList<>(nameFiles.keySet());
        return nameFiles.get(keys.get(random.nextInt(keys.size())));
    }

    /**
     * 璁剧疆鎬у埆骞堕噸鏂扮敓鎴愬悕瀛楋紙鐢ㄤ簬鍒濆甯傛皯鎬у埆骞宠　锛?     */
    public CitizenData createWithGender(final boolean female) {
        final CitizenData data = createAndRegisterCivilianData();
        final CitizenNameFile nameFile = pickNameFile();
        if (nameFile != null) {
            final List<String> existing = citizens.values().stream()
                    .map(CitizenData::getName)
                    .collect(Collectors.toList());
            data.setFemale(female);
            data.setName(CitizenData.generateName(random, female, nameFile, existing));
        }
        return data;
    }

    // 鈹€鈹€ 绉婚櫎 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 绉婚櫎甯傛皯
     */
    public void removeCivilian(final int citizenId) {
        citizens.remove(citizenId);
        colony.setDirty();
    }

    // 鈹€鈹€ 鏌ヨ 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CitizenData getCivilian(final int citizenId) {
        return citizens.get(citizenId);
    }

    /**
     * 鏍规嵁瀹炰綋 UUID 鏌ユ壘甯傛皯
     */
    public CitizenData findByUUID(final UUID uuid) {
        for (final CitizenData data : citizens.values()) {
            if (data.getUUID().equals(uuid)) return data;
        }
        return null;
    }

    public List<CitizenData> getCitizens() {
        return new ArrayList<>(citizens.values());
    }

    public int getCurrentCitizenCount() {
        return citizens.size();
    }

    /**
     * 鑾峰彇鎵€鏈夊凡瀛樺湪鐨勫競姘戝鍚嶏紙鐢ㄤ簬閲嶅悕妫€娴嬶級
     */
    public List<String> getExistingNames() {
        return citizens.values().stream()
                .map(CitizenData::getName)
                .collect(Collectors.toList());
    }

    /**
     * 灏濊瘯灏嗛檮杩戠殑绌洪棽甯傛皯鍒嗛厤鍒板搴旇亴涓?     * <p>
     * 寤虹瓚寤烘垚鏃惰皟鐢細鑷姩鍖归厤绌洪棽甯傛皯鍒版柊寤虹瓚鐨勫伐浣滃矖浣?     *
     * @param level    涓栫晫
     * @param pos      寤虹瓚浣嶇疆
     * @param buildingTypeId 寤虹瓚绫诲瀷 ID
     */
    public void assignNearbyUnemployed(final ServerLevel level, final BlockPos pos, final String buildingTypeId) {
        // 鈽?淇锛氫娇鐢?BuildingType.getRequiredJobId() 鏇夸唬纭紪鐮?switch-case
        BuildingType buildingType = BuildingType.fromId(buildingTypeId);
        if (buildingType == null) {
            MetroGenesis.LOGGER.warn("[Assign] 鏈煡寤虹瓚绫诲瀷: {}", buildingTypeId);
            return;
        }
        String jobId = buildingType.getRequiredJobId();
        if (jobId == null) return; // 姝ゅ缓绛戜笉闇€瑕佸垎閰嶈亴涓?
        // 鎵剧涓€涓┖闂插競姘?
        for (final CitizenData data : citizens.values()) {
            if ("unemployed".equals(data.getJob()) || "".equals(data.getJob())) {
                data.setJob(jobId);
                // 鎵惧疄浣撳苟鎸傝浇 AI锛堢粺涓€璧?reloadJobAI 璺緞锛?
                if (level.getEntity(data.getUUID()) instanceof MetroGenesisCitizen citizen) {
                    citizen.getPersistentData().putInt("metrogenesis:citizen_id", data.getId());
                    citizen.syncFromData(data);
                    citizen.reloadJobAI(); // 鈽?缁熶竴璺緞锛氬師瀛愭浛鎹?AI
                }
                // 閫氱煡
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal(
                            "搂e" + data.getName() + " 搂7琚垎閰嶅埌鏂扮殑宸ヤ綔宀椾綅"));
                }
                MetroGenesis.LOGGER.info("[Assign] {} 鈫?{} at {}",
                        data.getName(), jobId, pos.toShortString());
                return;
            }
        }
    }

    // 鈹€鈹€ 甯傛皯 tick 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 娈栨皯鍦?tick 鈥?鍒濆甯傛皯鐢熸垚
     * <p>
     * 鍙傝€?MineColonies CitizenManager.onColonyTick
     */
    public void onColonyTick(final ServerLevel level) {
        // 鍙湪鏈夊競鏀垮巺鏃舵墠鐢熸垚甯傛皯
        if (!colony.hasTownHall()) return;

        // 妫€鏌ュ垵濮嬪競姘戞暟閲?        final
        int initialAmount = 3;
        if (getCurrentCitizenCount() < initialAmount) {
            respawnInterval -= 100;
            if (respawnInterval <= 0) {
                respawnInterval = 1200;
                spawnInitialCitizen(level);
            }
        }
    }

    /**
     * 鐢熸垚鍒濆甯傛皯锛堝惈鎬у埆骞宠　锛夛紝鍚屾椂鍒峰嚭瀹炰綋
     */
    private void spawnInitialCitizen(final ServerLevel level) {
        int femaleCount = 0;
        for (final CitizenData citizen : citizens.values()) {
            femaleCount += citizen.isFemale() ? 1 : 0;
        }

        final boolean firstCitizen = getCurrentCitizenCount() == 0;
        CitizenData newCitizen;

        if (firstCitizen) {
            newCitizen = createAndRegisterCivilianData();
        } else {
            final boolean needFemale = femaleCount < (getCurrentCitizenCount() - 1) / 2.0;
            newCitizen = createWithGender(needFemale);
        }

        // 鍒峰嚭瀹炰綋鍒颁笘鐣岋紙浼樺厛甯傛斂鍘呬綅缃級
        BlockPos spawnPos = colony.hasTownHall()
                ? colony.getTownHallPos()
                : level.getSharedSpawnPos();
        spawnEntity(newCitizen, level, spawnPos);

        LOGGER.info("[Colony] 鏂板競姘?#{} 鈥?{} ({}) 宸插嚭鐢?at {}",
                newCitizen.getId(), newCitizen.getName(),
                newCitizen.isFemale() ? "\u2640" : "\u2642",
                spawnPos.toShortString());
    }

    /**
     * 灏嗗競姘戞暟鎹埛鍑轰负 Minecraft 瀹炰綋
     * <p>
     * 鍙傝€?MineColonies {@code spawnCitizenOnPosition}
     */
    public void spawnEntity(final CitizenData data, final ServerLevel level, final BlockPos pos) {
        // 鍒涘缓瀹炰綋
        MetroGenesisCitizen entity = MetroGenesis.CITIZEN_ENTITY.get().create(level);
        if (entity == null) {
            LOGGER.error("鏃犳硶鍒涘缓甯傛皯瀹炰綋 #{}", data.getId());
            return;
        }

        // 鍚屾鏁版嵁鍒板疄浣?        entity.syncFromData(data);

        // 瀛樺偍 citizenId 鐢ㄤ簬鏌ユ壘
        entity.getPersistentData().putInt("metrogenesis:citizen_id", data.getId());

        // 璁剧疆浣嶇疆锛堝湪鏂瑰潡涓婃柟鍋忎竴鐐癸紝閬垮厤鍗℃ā锛?        entity.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);

        // 鍔犲叆涓栫晫
        level.addFreshEntity(entity);

        LOGGER.debug("甯傛皯瀹炰綋 #{} ({}) 宸插埛鍑?at {}",
                data.getId(), data.getName(), pos.toShortString());
    }

    // 鈹€鈹€ NBT 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag citizenList = new ListTag();
        for (final CitizenData data : citizens.values()) {
            citizenList.add(data.writeNBT());
        }
        tag.put(TAG_CITIZENS, citizenList);
        tag.putInt(TAG_TOP_ID, topCitizenId);
        return tag;
    }

    public void load(final CompoundTag tag) {
        citizens.clear();
        final ListTag citizenList = tag.getList(TAG_CITIZENS, Tag.TAG_COMPOUND);
        for (int i = 0; i < citizenList.size(); i++) {
            final CitizenData data = CitizenData.readNBT(citizenList.getCompound(i));
            citizens.put(data.getId(), data);
            topCitizenId = Math.max(topCitizenId, data.getId());
        }
        if (tag.contains(TAG_TOP_ID)) {
            topCitizenId = tag.getInt(TAG_TOP_ID);
        }
        LOGGER.info("Loaded {} citizen(s)", citizens.size());
    }

    // 鈹€鈹€ 瀹炰綋鍏宠仈 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 灏嗗疄浣撳叧鑱斿埌甯傛皯鏁版嵁
     */
    public void linkEntity(final Entity entity, final int citizenId) {
        final CitizenData data = citizens.get(citizenId);
        if (data != null) {
            entity.getPersistentData().putInt("metrogenesis:citizen_id", citizenId);
        }
    }

    /**
     * 浠庡疄浣撹幏鍙栧競姘戞暟鎹?     */
    public static CitizenData fromEntity(final Entity entity, final ColonyState colony) {
        if (entity == null || colony == null) return null;
        final int id = entity.getPersistentData().getInt("metrogenesis:citizen_id");
        if (id == 0) return null;
        return colony.getCitizenManager().getCivilian(id);
    }
}
