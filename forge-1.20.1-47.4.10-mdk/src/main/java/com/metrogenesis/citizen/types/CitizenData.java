package com.metrogenesis.citizen.types;

import com.metrogenesis.colony.citizen.CitizenNameFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 甯傛皯鏁版嵁 鈥?鍩庡競绯荤粺涓瘡浣嶅競姘戠殑瀹屾暣鐘舵€? * <p>
 * 涓嶅啀浣跨敤闈欐€佸伐鍏风被 + PersistentData 妯″紡锛岃€屾槸浣滀负鐙珛鏁版嵁瀵硅薄瀛樺湪锛? * 鐢?{@link com.metrogenesis.colony.managers.CitizenManager} 鎸佹湁鍜岀鐞嗐€? * 瀹炰綋閫氳繃 {@code citizenId} 鏄犲皠鍒版湰瀵硅薄銆? */
public class CitizenData {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis-CitizenData");

    // 鈹€鈹€ NBT 鏍囩甯搁噺 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private static final String TAG_ID = "citizenId";
    private static final String TAG_UUID = "citizenUUID";
    private static final String TAG_NAME = "citizenName";
    private static final String TAG_FEMALE = "citizenFemale";
    private static final String TAG_TEXTURE_ID = "textureId";
    private static final String TAG_TEXTURE_SUFFIX = "textureSuffix";
    private static final String TAG_JOB = "citizenJob";
    private static final String TAG_WALLET = "citizenWallet";
    private static final String TAG_SATISFACTION = "satisfaction";

    // 鈹€鈹€ 绾圭悊鍚庣紑鏋氫妇 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public static final String[] TEXTURE_SUFFIXES = {"_a", "_b", "_d", "_w"};

    // 鈹€鈹€ 瀛楁 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private int id;
    private UUID uuid;
    private String name = "甯傛皯";
    private boolean female;
    private int textureId;
    private String textureSuffix = "_a";
    private String job = "unemployed";
    private int wallet;
    private int satisfaction = 50; // 鍒濆涓瓑婊℃剰搴?
    // 鈹€鈹€ 涓存椂瀛楁锛堜笉搴忓垪鍖栵級 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private transient boolean isDirty;

    public CitizenData(final int id) {
        this.id = id;
        this.uuid = UUID.randomUUID();
    }

    // 鈹€鈹€ 鍒濆鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 鍒濆鍖栨柊甯傛皯 鈥?鐢熸垚鎬у埆銆佸悕瀛椼€佺汗鐞?     */
    public void initForNewCitizen(final Random random, final CitizenNameFile nameFile) {
        this.female = random.nextBoolean();
        this.textureId = random.nextInt(256);
        this.textureSuffix = TEXTURE_SUFFIXES[random.nextInt(TEXTURE_SUFFIXES.length)];
        this.name = generateName(random, female, nameFile, null);
        this.wallet = 0;
        this.satisfaction = 50;
        this.isDirty = true;
    }

    // 鈹€鈹€ 鍚嶅瓧鐢熸垚 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 鐢熸垚甯傛皯濮撳悕
     * <p>
     * 鍙傝€?MineColonies {@code CitizenData.generateName}
     *
     * @param random   闅忔満鏁?     * @param female   鏄惁涓哄コ鎬?     * @param nameFile 鍛藉悕鏂囦欢鍚?     * @param existing 鐜版湁濮撳悕鍒楄〃锛堢敤浜庨噸鍚嶆娴嬶紝鍙负 null锛?     * @return 鐢熸垚鐨勫鍚?     */
    public static String generateName(
            @NotNull final Random random,
            final boolean female,
            @NotNull final CitizenNameFile nameFile,
            final List<String> existing) {

        final List<String> firstNames = female
                ? nameFile.getFemaleFirstNames()
                : nameFile.getMaleFirstNames();
        final List<String> surnames = nameFile.getSurnames();

        final String firstName = firstNames.get(random.nextInt(firstNames.size()));
        final String lastName = surnames.get(random.nextInt(surnames.size()));
        final CitizenNameFile.NameOrder order = nameFile.getOrder();
        final int parts = nameFile.getParts();

        String name;
        if (order == CitizenNameFile.NameOrder.EASTERN) {
            // EASTERN: "濮?鍚?锛堟棤涓棿鍚嶏級
            name = lastName + " " + firstName;
        } else {
            // WESTERN
            if (parts >= 3) {
                // "鍚?M. 濮?
                char middleInitial = (char) ('A' + random.nextInt(26));
                name = firstName + " " + middleInitial + ". " + lastName;
            } else if (parts == 2) {
                // "鍚?濮?
                name = firstName + " " + lastName;
            } else {
                name = firstName;
            }
        }

        // 閲嶅悕妫€娴?鈥?濡傛灉瀛樺湪鍒欓€掑綊閲嶈瘯
        if (existing != null && existing.contains(name)) {
            return generateName(random, female, nameFile, existing);
        }

        return name;
    }

    // 鈹€鈹€ NBT 搴忓垪鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag writeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_ID, id);
        tag.putUUID(TAG_UUID, uuid);
        tag.putString(TAG_NAME, name);
        tag.putBoolean(TAG_FEMALE, female);
        tag.putInt(TAG_TEXTURE_ID, textureId);
        tag.putString(TAG_TEXTURE_SUFFIX, textureSuffix);
        tag.putString(TAG_JOB, job);
        tag.putInt(TAG_WALLET, wallet);
        tag.putInt(TAG_SATISFACTION, satisfaction);
        return tag;
    }

    public static CitizenData readNBT(final CompoundTag tag) {
        final int id = tag.getInt(TAG_ID);
        final CitizenData data = new CitizenData(id);
        data.uuid = tag.getUUID(TAG_UUID);
        data.name = tag.getString(TAG_NAME);
        data.female = tag.getBoolean(TAG_FEMALE);
        data.textureId = tag.getInt(TAG_TEXTURE_ID);
        data.textureSuffix = tag.getString(TAG_TEXTURE_SUFFIX);
        data.job = tag.getString(TAG_JOB);
        data.wallet = tag.getInt(TAG_WALLET);
        data.satisfaction = tag.getInt(TAG_SATISFACTION);
        data.isDirty = false;
        return data;
    }

    // 鈹€鈹€ 宸ヤ綔鏁堢巼 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 鏍规嵁婊℃剰搴﹁绠楀伐浣滄晥鐜囧€嶇巼
     */
    public float getWorkEfficiency() {
        if (satisfaction < 30) return 0.5f;
        if (satisfaction < 60) return 0.8f;
        if (satisfaction >= 80) return 1.2f;
        return 1.0f;
    }

    /**
     * 璁＄畻鍩虹宸ヨ祫锛堜緷鎹亴涓氾級
     */
    public int getBaseSalary() {
        return switch (job) {
            case "farmer" -> 15;
            case "merchant" -> 18;
            case "builder" -> 20;
            case "miner" -> 20;
            case "lumberjack" -> 16;
            case "crafter" -> 22;
            case "unemployed" -> 5;
            default -> 10;
        };
    }

    // 鈹€鈹€ Getter / Setter 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public int getId() { return id; }

    public UUID getUUID() { return uuid; }
    public void setUUID(final UUID uuid) { this.uuid = uuid; markDirty(); }

    public String getName() { return name; }
    public void setName(final String name) { this.name = name; markDirty(); }

    public boolean isFemale() { return female; }
    public void setFemale(final boolean female) { this.female = female; markDirty(); }

    public int getTextureId() { return textureId; }
    public void setTextureId(final int textureId) { this.textureId = textureId; markDirty(); }

    public String getTextureSuffix() { return textureSuffix; }
    public void setTextureSuffix(final String textureSuffix) { this.textureSuffix = textureSuffix; markDirty(); }

    public String getJob() { return job; }
    public void setJob(final String job) { this.job = job; markDirty(); }

    public int getWallet() { return wallet; }
    public void setWallet(final int wallet) { this.wallet = wallet; markDirty(); }
    public void addToWallet(final int delta) {
        this.wallet = Math.max(0, this.wallet + delta);
        markDirty();
    }

    public int getSatisfaction() { return satisfaction; }
    public void setSatisfaction(final int satisfaction) {
        this.satisfaction = Math.max(0, Math.min(100, satisfaction));
        markDirty();
    }
    public void addSatisfaction(final int delta) {
        setSatisfaction(this.satisfaction + delta);
    }

    public boolean isDirty() { return isDirty; }
    public void markDirty() { this.isDirty = true; }
    public void clearDirty() { this.isDirty = false; }
}
