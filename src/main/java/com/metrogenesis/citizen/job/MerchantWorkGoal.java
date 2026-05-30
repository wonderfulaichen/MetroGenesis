package com.metrogenesis.citizen.job;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.citizen.CitizenData;
import com.metrogenesis.colony.ColonyState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MerchantWorkGoal extends Goal {

    private static final int SCAN_NEAR = 20;
    private static final int SCAN_VILLAGE = 64;

    private final Mob mob;
    private BlockPos shopPos;
    private boolean headingToVillage;
    private BlockPos villageCenter;
    private int workTicks;
    private int scanTimer;

    private enum Mode { AT_TOWNHALL, AT_VILLAGE, GOING_TO_VILLAGE, IDLE }
    private Mode currentMode = Mode.IDLE;

    public MerchantWorkGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // 鈹€鈹€ 鏉′欢 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public boolean canUse() {
        if (!isWorkHours()) {
            this.shopPos = null;
            return false;
        }
        this.shopPos = findShopSpot();
        MetroGenesis.LOGGER.debug("[Merchant] {} canUse 鈫?mode={}, shop={}",
                mob.getName().getString(), currentMode,
                shopPos != null ? shopPos.toShortString() : "null");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isWorkHours()) return false;
        if (shopPos == null) return false;
        return mob.distanceToSqr(Vec3.atCenterOf(shopPos)) < 1000;
    }

    @Override
    public void start() {
        this.workTicks = 0;
        this.scanTimer = 0;
        if (shopPos != null) {
            mob.getNavigation().moveTo(shopPos.getX() + 0.5, shopPos.getY(), shopPos.getZ() + 0.5, 0.5);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        this.shopPos = null;
        this.villageCenter = null;
        this.headingToVillage = false;
        this.workTicks = 0;
    }

    // 鈹€鈹€ 鎵ц 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public void tick() {
        if (shopPos == null) {
            idleTick();
            return;
        }

        if (mob.getNavigation().isDone()) {
            scanTimer++;
            if (scanTimer >= 120) {
                scanTimer = 0;
                BlockPos newSpot = findShopSpot();
                if (newSpot != null) {
                    shopPos = newSpot;
                    mob.getNavigation().moveTo(shopPos.getX() + 0.5, shopPos.getY(), shopPos.getZ() + 0.5, 0.5);
                }
            }
            return;
        }

        if (mob.distanceToSqr(Vec3.atCenterOf(shopPos)) < 4) {
            mob.getNavigation().stop();

            // 鍒氬埌鏉戝簞閽?鈫?灏卞湪閽熸梺杈硅惀涓?
            if (currentMode == Mode.GOING_TO_VILLAGE) {
                currentMode = Mode.AT_VILLAGE;
                headingToVillage = false;
            }

            workTicks++;
            if (workTicks % 20 == 0) {
                doMerchantWork(shopPos);
            }
        }
    }

    private void idleTick() {
        workTicks++;
        if (workTicks % 30 == 0) {
            doMerchantWork(mob.blockPosition().above());
        }
    }

    // 鈹€鈹€ 鎵炬憡浣?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /** 鎵惧紑搴椾綅缃細甯傛斂鍘?> 鏉戝簞閽?> null */
    private BlockPos findShopSpot() {
        BlockPos origin = mob.blockPosition();
        Level level = mob.level();

        // 1. 鎵惧競鏀垮巺
        BlockPos th = scanBlock(origin, level, SCAN_NEAR,
                (state) -> state.is(MetroGenesis.TOWN_HALL_BLOCK.get()));
        if (th != null) {
            currentMode = Mode.AT_TOWNHALL;
            return findAdjacentSpot(th, level);
        }

        // 2. 宸茬粡鍦ㄥ幓鏉戝簞鐨勮矾涓?
        if (headingToVillage && villageCenter != null) {
            currentMode = Mode.GOING_TO_VILLAGE;
            return findAdjacentSpot(villageCenter, level);
        }

        // 3. 鎵炬潙搴勯挓
        BlockPos bell = scanBlock(origin, level, SCAN_VILLAGE,
                (state) -> state.is(Blocks.BELL));
        if (bell != null) {
            villageCenter = bell;
            headingToVillage = true;
            currentMode = Mode.GOING_TO_VILLAGE;
            return findAdjacentSpot(bell, level);
        }

        currentMode = Mode.IDLE;
        return null;
    }

    /** 鍦ㄧ洰鏍囨柟鍧楁梺杈规壘绌哄湴鍋氭憡浣?*/
    private BlockPos findAdjacentSpot(BlockPos center, Level level) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos spot = center.relative(dir, 2);
            if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()) {
                return spot;
            }
        }
        return center.relative(Direction.NORTH, 2);
    }

    /** 鎵弿鎸囧畾鍗婂緞鍐呯殑鏂瑰潡 */
    private BlockPos scanBlock(BlockPos origin, Level level, int radius,
                                java.util.function.Predicate<BlockState> predicate) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    BlockPos check = origin.offset(dx, dy, dz);
                    if (predicate.test(level.getBlockState(check))) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    // 鈹€鈹€ 钀ヤ笟 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private void doMerchantWork(BlockPos pos) {
        Level level = mob.level();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    8, 0.5, 0.3, 0.5, 0.08);
            if (level.random.nextInt(3) == 0) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        pos.getX() + 0.5 + level.random.nextGaussian() * 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5 + level.random.nextGaussian() * 0.5,
                        3, 0.1, 0.1, 0.1, 0.2);
            }
        }
        // 璧?C-Value 鈫?鍏ュ浗搴擄紙鍟嗕汉缁熶竴 1/娆★紝涓?Farmer/Builder 涓€鑷达級
        CitizenData.payToTreasury(mob, (ServerLevel) level, 1);
    }

    private boolean isWorkHours() {
        long time = mob.level().getDayTime() % 24000;
        return time >= 6000 && time <= 18000;
    }
}
