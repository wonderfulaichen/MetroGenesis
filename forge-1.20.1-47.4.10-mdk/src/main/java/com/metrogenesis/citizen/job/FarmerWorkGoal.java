package com.metrogenesis.citizen.job;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.block.FarmFacilityBlock;
import com.metrogenesis.citizen.CitizenData;
import com.metrogenesis.colony.ColonyState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 // AI
 * <p>
 // 6:00 18:00
 // 1.
 // 2. 3. bell 4.
 // 5. */
public class FarmerWorkGoal extends Goal {

    private final Mob mob;
    private BlockPos targetPos;
    private BlockPos villageCenter;   // Discovered village bell position
    private boolean headingToVillage; // //
    private int workTicks;
    private static final int SCAN_NEAR = 16;
    private static final int SCAN_VILLAGE = 64;
    private static final int SCAN_INTERVAL = 100;
    private int scanTimer;

    private enum Mode { TEND_CROPS, WORK_AT_FACILITY, PLANT_SEEDS, TILL_LAND, GO_TO_VILLAGE, IDLE }
    private Mode currentMode = Mode.IDLE;

    public FarmerWorkGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // //

    @Override
    public boolean canUse() {
        if (!isWorkHours()) {
            this.targetPos = null;
            return false;
        }
        this.targetPos = findWorkTarget();
        MetroGenesis.LOGGER.debug("[Farmer] {} canUse 鈫?mode={}, target={}",
                mob.getName().getString(), currentMode,
                targetPos != null ? targetPos.toShortString() : "null");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isWorkHours()) return false;
        if (targetPos == null) return false;
        return mob.distanceToSqr(Vec3.atCenterOf(targetPos)) < 500;
    }

    @Override
    public void start() {
        this.workTicks = 0;
        this.scanTimer = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        this.targetPos = null;
        this.villageCenter = null;
        this.headingToVillage = false;
        this.workTicks = 0;
    }

    // //

    @Override
    public void tick() {
        // //
        if (targetPos == null) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                targetPos = findWorkTarget();
                moveToTarget();
            }
            return;
        }

        // 瀵昏矾涓?
        if (mob.getNavigation().isDone()) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                BlockPos newTarget = findWorkTarget();
                if (newTarget != null && !newTarget.equals(targetPos)) {
                    targetPos = newTarget;
                    moveToTarget();
                }
            }
            return;
        }

        // //
        if (mob.distanceToSqr(Vec3.atCenterOf(targetPos)) < 4) {
            mob.getNavigation().stop();

            // //
            if (currentMode == Mode.GO_TO_VILLAGE) {
                headingToVillage = false;
                targetPos = findWorkTarget(); // Re-scan in current range
                if (currentMode != Mode.GO_TO_VILLAGE && targetPos != null) {
                    moveToTarget();
                }
                return;
            }

            workTicks++;

            if (workTicks % 40 == 0) { // 每 2 秒
                doFarmWork();
            }
        }
    }

    // ══ 鎵炬椿骞?═══════════════════════════════════════

    /** @return Found work target, or null to idle */
    private BlockPos findWorkTarget() {
        BlockPos origin = mob.blockPosition();
        Level level = mob.level();
        BlockPos target;

        // // 1.
        target = scanBlocks(origin, level, SCAN_NEAR, (state) ->
                state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state));
        if (target != null) { currentMode = Mode.TEND_CROPS; return target; }

        // // 2.
        target = scanBlocks(origin, level, SCAN_NEAR,
                (state) -> state.is(MetroGenesis.FARM_FACILITY_BLOCK.get()));
        if (target != null) {
            MetroGenesis.LOGGER.debug("[Farmer] {} 鍙戠幇鍐滃満璁炬柦 at {}", mob.getName().getString(), target.toShortString());
            currentMode = Mode.WORK_AT_FACILITY;
            return target;
        }

        // // 3.
        target = scanBlocks(origin, level, SCAN_NEAR, (state) ->
                state.is(Blocks.FARMLAND));
        if (target != null) { currentMode = Mode.PLANT_SEEDS; return target; }

        // // 4.
        if (headingToVillage && villageCenter != null) {
            return villageCenter;
        }

        // // 5.
        BlockPos bell = scanBlocks(origin, level, SCAN_VILLAGE,
                (state) -> state.is(Blocks.BELL));
        if (bell != null) {
            villageCenter = bell;
            headingToVillage = true;
            currentMode = Mode.GO_TO_VILLAGE;
            return bell;
        }

        // 6. Find dirt to till
        target = scanBlocks(origin, level, 12, (state) ->
                state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                        || state.is(Blocks.COARSE_DIRT) || state.is(BlockTags.DIRT));
        if (target != null) { currentMode = Mode.TILL_LAND; return target; }

        // // 7.
        currentMode = Mode.IDLE;
        return null;
    }

    @FunctionalInterface
    private interface BlockMatcher {
        boolean matches(BlockState state);
    }

    private BlockPos scanBlocks(BlockPos origin, Level level, int radius, BlockMatcher matcher) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos check = origin.offset(dx, dy, dz);
                    if (matcher.matches(level.getBlockState(check))) {
                        double dist = origin.distSqr(check);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = check;
                        }
                    }
                }
            }
        }
        return best;
    }

    // ══ Farm Work ═════════════════════════════════════

    private void doFarmWork() {
        Level level = mob.level();

        switch (currentMode) {
            case TEND_CROPS -> tendCrops(level);
            case WORK_AT_FACILITY -> workAtFacility(level);
            case PLANT_SEEDS -> plantSeeds(level);
            case TILL_LAND -> tillLand(level);
            case GO_TO_VILLAGE -> {} // // case IDLE idleWork(level)
        }

        // // C-Value
        if (level instanceof ServerLevel sl) {
            CitizenData.payToTreasury(mob, sl, 1);
        }
    }

    // /** */
    private void tendCrops(Level level) {
        boolean did = false;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos check = mob.blockPosition().offset(dx, 0, dz);
                BlockState state = level.getBlockState(check);
                if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                    int age = state.getValue(CropBlock.AGE);
                    int newAge = Math.min(age + 1 + level.random.nextInt(2), crop.getMaxAge());
                    level.setBlock(check, state.setValue(CropBlock.AGE, newAge), 3);
                    spawnParticles(level, check);
                    did = true;
                }
            }
        }
        if (!did) spawnParticles(level, mob.blockPosition().above());
    }

    // /** */
    private void workAtFacility(Level level) {
        if (level instanceof ServerLevel sl) {
            // 鍫嗚偉缁胯壊绮掑瓙
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(),
                    6, 0.4, 0.2, 0.4, 0.03);
        }
    }

    // /** */
    private void plantSeeds(Level level) {
        BlockPos above = targetPos.above();
        if (level.getBlockState(above).isAir()) {
            level.setBlock(above, Blocks.WHEAT.defaultBlockState(), 3);
            spawnParticles(level, above);
        } else {
            spawnParticles(level, mob.blockPosition().above());
        }
    }

    // /** */
    private void tillLand(Level level) {
        if (targetPos != null) {
            BlockState before = level.getBlockState(targetPos);
            level.setBlock(targetPos, Blocks.FARMLAND.defaultBlockState(), 3);
            spawnParticles(level, targetPos.above());
        }
    }

    // /** */
    private void idleWork(Level level) {
        spawnParticles(level, mob.blockPosition().above());
    }

    private void spawnParticles(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    6, 0.4, 0.4, 0.4, 0.05);
        }
    }

    // //

    private boolean isWorkHours() {
        long time = mob.level().getDayTime() % 24000;
        return time >= 6000 && time <= 18000;
    }

    private void moveToTarget() {
        if (targetPos != null) {
            mob.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 0.5);
        }
    }
}
