package com.metrogenesis.entity;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.citizen.types.CitizenData;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.init.ModJobs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/**
 * 鑷畾涔夊競姘戝疄浣?鈥?缁ф壙 PathfinderMob锛屼笉鏄?Villager
 * <p>
 * 鍙傝€?MineColonies EntityCitizen 鈫?PathfinderMob 缁ф壙閾俱€? * 浣跨敤鏉戞皯绾圭悊鍜屾ā鍨嬫覆鏌擄紝浣嗘嫢鏈夌嫭绔?AI 绯荤粺锛屼笉鍙楀師鐗堟潙姘戣剳绯荤粺骞叉壈銆? */
public class MetroGenesisCitizen extends PathfinderMob {

    // 鈹€鈹€ 鍚屾鏁版嵁锛圫ynchedEntityData锛岃嚜鍔ㄥ悓姝ュ埌瀹㈡埛绔級 鈹€

    private static final EntityDataAccessor<String> DATA_NAME =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_FEMALE =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE_ID =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TEXTURE_SUFFIX =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_JOB =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_WALLET =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SATISFACTION =
            SynchedEntityData.defineId(MetroGenesisCitizen.class, EntityDataSerializers.INT);

    /** 褰撳墠鎸傝浇鐨勮亴涓?AI Goal锛岀敤浜庤亴涓氬彉鏇存椂鍘熷瓙鏇挎崲 */
    private Goal currentJobGoal;

    public MetroGenesisCitizen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
    }

    /**
     * 閲嶆柊鎸傝浇鑱屼笟 AI 鈥?鍘熷瓙鏇挎崲锛?     * 1. 绉婚櫎鏃х殑鑱屼笟 Goal锛堝鏋滄湁锛?     * 2. 鏍规嵁褰撳墠 SynchedEntityData 鐨?DATA_JOB 鍒涘缓骞舵寕杞芥柊 Goal
     * <p>
     * 鍦ㄤ互涓嬫椂鏈鸿璋冪敤锛?     * - 瀹炰綋鐢熸垚锛圗ntityJoinLevelEvent 鈫?CitizenEvents锛?     * - 鑱屼笟鍙樻洿锛坰etCitizenJob / TownHallMenu / assignNearbyUnemployed锛?     */
    public void reloadJobAI() {
        String job = getCitizenJob();
        // 绉婚櫎鏃?Goal
        if (currentJobGoal != null) {
            goalSelector.removeGoal(currentJobGoal);
            currentJobGoal = null;
        }
        // 澶变笟 鈫?涓嶆寕杞?AI
        if (job == null || "unemployed".equals(job)) return;
        // 鍒涘缓鏂?Goal
        Goal goal = ModJobs.createGoal(job, this);
        if (goal != null) {
            currentJobGoal = goal;
            goalSelector.addGoal(1, goal);
            MetroGenesis.LOGGER.debug("[Citizen] {} 閲嶆寕 AI 鈫?{}", getCitizenName(), job);
        }
    }

    // 鈹€鈹€ 灞炴€у拰鍒濆鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_NAME, "甯傛皯");
        this.entityData.define(DATA_FEMALE, false);
        this.entityData.define(DATA_TEXTURE_ID, 0);
        this.entityData.define(DATA_TEXTURE_SUFFIX, "_a");
        this.entityData.define(DATA_JOB, "unemployed");
        this.entityData.define(DATA_WALLET, 0);
        this.entityData.define(DATA_SATISFACTION, 50);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
    }

    // 鈹€鈹€ 浠?CitizenData 鍚屾鏁版嵁鍒板疄浣?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 灏嗘湇鍔″櫒绔殑 CitizenData 鍚屾鍒板疄浣撶殑 SynchedEntityData
     * 搴斿湪瀹炰綋鐢熸垚鍚庤皟鐢ㄤ竴娆?     */
    public void syncFromData(final CitizenData data) {
        if (data == null) return;
        this.entityData.set(DATA_NAME, data.getName());
        this.entityData.set(DATA_FEMALE, data.isFemale());
        this.entityData.set(DATA_TEXTURE_ID, data.getTextureId());
        this.entityData.set(DATA_TEXTURE_SUFFIX, data.getTextureSuffix());
        this.entityData.set(DATA_JOB, data.getJob());
        this.entityData.set(DATA_WALLET, data.getWallet());
        this.entityData.set(DATA_SATISFACTION, data.getSatisfaction());
        // 鏇存柊澶撮《鍚嶇О
        this.setCustomName(Component.literal(data.getName()));
    }

    /**
     * 浠庡疄浣撴暟鎹啓鍥?CitizenData锛堥挶鍖呯瓑杩愯鏃跺彉鍖栵級
     */
    public void syncToData(final ServerLevel level) {
        final ColonyState colony = ColonyState.get(level);
        final CitizenData data = CitizenManager.fromEntity(this, colony);
        if (data != null) {
            data.setWallet(this.entityData.get(DATA_WALLET));
            data.setJob(this.entityData.get(DATA_JOB));
            data.setSatisfaction(this.entityData.get(DATA_SATISFACTION));
        }
    }

    // 鈹€鈹€ 鏁版嵁璁块棶 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public String getCitizenName() {
        return entityData.get(DATA_NAME);
    }

    public boolean isFemale() {
        return entityData.get(DATA_FEMALE);
    }

    public int getTextureId() {
        return entityData.get(DATA_TEXTURE_ID);
    }

    public String getTextureSuffix() {
        return entityData.get(DATA_TEXTURE_SUFFIX);
    }

    public String getCitizenJob() {
        return entityData.get(DATA_JOB);
    }

    public void setCitizenJob(String job) {
        entityData.set(DATA_JOB, job);
        MetroGenesis.LOGGER.debug("[Citizen] {} 鑱屼笟鏇存柊 -> {}", getCitizenName(), job);
        // 鑱屼笟鍙樻洿 鈫?鑷姩閲嶆柊鎸傝浇 AI锛堝師瀛愭浛鎹級
        reloadJobAI();
    }

    public int getWallet() {
        return entityData.get(DATA_WALLET);
    }

    public void setWallet(int amount) {
        entityData.set(DATA_WALLET, amount);
    }

    public int getSatisfaction() {
        return entityData.get(DATA_SATISFACTION);
    }

    public void setSatisfaction(int value) {
        entityData.set(DATA_SATISFACTION, Math.max(0, Math.min(100, value)));
    }

    // 鈹€鈹€ 浜や簰锛堝彸閿級 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            // 瀹㈡埛绔細鐢?Forge 浜嬩欢 ClientModEvents 鎵撳紑 GUI
            return InteractionResult.SUCCESS;
        }
        // 鏈嶅姟绔細娑堣垂浜や簰锛堥樆姝㈤粯璁よ涓猴級锛孏UI 鍦ㄥ鎴风鎵撳紑
        return InteractionResult.CONSUME;
    }

    // 鈹€鈹€ 鎸佷箙鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CitizenName")) entityData.set(DATA_NAME, tag.getString("CitizenName"));
        if (tag.contains("CitizenFemale")) entityData.set(DATA_FEMALE, tag.getBoolean("CitizenFemale"));
        if (tag.contains("CitizenTextureId")) entityData.set(DATA_TEXTURE_ID, tag.getInt("CitizenTextureId"));
        if (tag.contains("CitizenTexSuffix")) entityData.set(DATA_TEXTURE_SUFFIX, tag.getString("CitizenTexSuffix"));
        if (tag.contains("CitizenJob")) entityData.set(DATA_JOB, tag.getString("CitizenJob"));
        if (tag.contains("CitizenWallet")) entityData.set(DATA_WALLET, tag.getInt("CitizenWallet"));
        if (tag.contains("CitizenSatisfaction")) entityData.set(DATA_SATISFACTION, tag.getInt("CitizenSatisfaction"));

        // 鎭㈠澶撮《鍚嶇О
        this.setCustomName(Component.literal(entityData.get(DATA_NAME)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CitizenName", entityData.get(DATA_NAME));
        tag.putBoolean("CitizenFemale", entityData.get(DATA_FEMALE));
        tag.putInt("CitizenTextureId", entityData.get(DATA_TEXTURE_ID));
        tag.putString("CitizenTexSuffix", entityData.get(DATA_TEXTURE_SUFFIX));
        tag.putString("CitizenJob", entityData.get(DATA_JOB));
        tag.putInt("CitizenWallet", entityData.get(DATA_WALLET));
        tag.putInt("CitizenSatisfaction", entityData.get(DATA_SATISFACTION));
    }

    // 鈹€鈹€ 鍙椾激/姝讳骸 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public void die(DamageSource source) {
        MetroGenesis.LOGGER.info("[Citizen] {} 姝讳骸 鈥?鑱屼笟: {}, 閽卞寘: {}",
                getCitizenName(), getCitizenJob(), getWallet());
        super.die(source);
    }
}
