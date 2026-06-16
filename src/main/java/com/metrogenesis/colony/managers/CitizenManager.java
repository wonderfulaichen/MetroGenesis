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
 * <p>
 // MineColonies {@code com.minecolonies.core.colony.managers.CitizenManager}
 */
public class CitizenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis-CitizenMgr");

    private static final String TAG_CITIZENS = "citizens";
    private static final String TAG_TOP_ID = "topCitizenId";

    // /** id CitizenData */
    @NotNull
    private final Map<Integer, CitizenData> citizens = new HashMap<>();

    // /** ID */
    private int topCitizenId = 0;

    // /** */
    @NotNull
    private final ColonyState colony;

    // /** ick */
    private int respawnInterval = 600; // 30绉?= 600 ticks

    private final Random random = new Random();

    public CitizenManager(@NotNull final ColonyState colony) {
        this.colony = colony;
    }

    // //

    /**
     * <p>
     * @return Reuse gap ID (scan 1~N+1)
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
     // */
    private CitizenNameFile pickNameFile() {
        final Map<String, CitizenNameFile> nameFiles = CitizenNameListener.getNameFileMap();
        if (nameFiles.isEmpty()) return null;

        // // chinese default
        final List<String> keys = new ArrayList<>(nameFiles.keySet());
        return nameFiles.get(keys.get(random.nextInt(keys.size())));
    }

    /**
     // */
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

    // //

    /**
     */
    public void removeCivilian(final int citizenId) {
        citizens.remove(citizenId);
        colony.setDirty();
    }

    // //

    public CitizenData getCivilian(final int citizenId) {
        return citizens.get(citizenId);
    }

    /**
     // UUID
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
     */
    public List<String> getExistingNames() {
        return citizens.values().stream()
                .map(CitizenData::getName)
                .collect(Collectors.toList());
    }

    /**
     * @param level    涓栫晫
     * @param pos      Building position
     * @param buildingTypeId 寤虹瓚绫诲瀷 ID
     */
    public void assignNearbyUnemployed(final ServerLevel level, final BlockPos pos, final String buildingTypeId) {
        // // BuildingType.getRequiredJobId() switch-case
        BuildingType buildingType = BuildingType.fromId(buildingTypeId);
        if (buildingType == null) {
            MetroGenesis.LOGGER.warn("[Assign] 未知建筑类型: {}", buildingTypeId);
            return;
        }
        String jobId = buildingType.getRequiredJobId();
        if (jobId == null) return; // //
        // //
        for (final CitizenData data : citizens.values()) {
            if ("unemployed".equals(data.getJob()) || "".equals(data.getJob())) {
                data.setJob(jobId);
                // // AI reloadJobAI
                if (level.getEntity(data.getUUID()) instanceof MetroGenesisCitizen citizen) {
                    citizen.getPersistentData().putInt("metrogenesis:citizen_id", data.getId());
                    citizen.syncFromData(data);
                    citizen.reloadJobAI(); // // AI
                }
                // Notify
                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal(
                            "§e" + data.getName() + " §7被分配到新的工作岗位"));
                }
                MetroGenesis.LOGGER.info("[Assign] {} →{} at {}",
                        data.getName(), jobId, pos.toShortString());
                return;
            }
        }
    }

    // // tick

    /**
     // tick
     * <p>
     // MineColonies CitizenManager.onColonyTick
     */
    public void onColonyTick(final ServerLevel level) {
        // //
        if (!colony.hasTownHall()) return;

        // // final
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

        // //
        BlockPos spawnPos = colony.hasTownHall()
                ? colony.getTownHallPos()
                : level.getSharedSpawnPos();
        spawnEntity(newCitizen, level, spawnPos);

        LOGGER.info("[Colony] 新市民 #{} — {} ({}) 已出生 at {}",
                newCitizen.getId(), newCitizen.getName(),
                newCitizen.isFemale() ? "\u2640" : "\u2642",
                spawnPos.toShortString());
    }

    /**
     // Minecraft
     * <p>
     // MineColonies {@code spawnCitizenOnPosition}
     */
    public void spawnEntity(final CitizenData data, final ServerLevel level, final BlockPos pos) {
        // //
        MetroGenesisCitizen entity = MetroGenesis.CITIZEN_ENTITY.get().create(level);
        if (entity == null) {
            LOGGER.error("无法创建市民实体 #{}", data.getId());
            return;
        }

        // // entity.syncFromData(data)

        // // citizenId
        entity.getPersistentData().putInt("metrogenesis:citizen_id", data.getId());

        // // entity.setPos(pos.getX() 0.5, pos.getY() 1.5, pos.getZ() 0.5)

        // 鍔犲叆涓栫晫
        level.addFreshEntity(entity);

        LOGGER.debug("市民实体 #{} ({}) 已刷出 at {}",
                data.getId(), data.getName(), pos.toShortString());
    }

    // // NBT

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

    // //

    /**
     */
    public void linkEntity(final Entity entity, final int citizenId) {
        final CitizenData data = citizens.get(citizenId);
        if (data != null) {
            entity.getPersistentData().putInt("metrogenesis:citizen_id", citizenId);
        }
    }

    /**
     // */
    public static CitizenData fromEntity(final Entity entity, final ColonyState colony) {
        if (entity == null || colony == null) return null;
        final int id = entity.getPersistentData().getInt("metrogenesis:citizen_id");
        if (id == 0) return null;
        return colony.getCitizenManager().getCivilian(id);
    }
}
