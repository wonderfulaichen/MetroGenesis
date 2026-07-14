package com.metrogenesis.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * RTS 视角相机实体 — 不可见的纯逻辑实体，用于承载俯视视角
 * 参考 RTSbuilding 的 RtsCameraEntity 设计
 *
 * 核心设计：
 * - noPhysics=true：无物理碰撞
 * - noGravity=true：无重力
 * - snapTo()：同时设置当前位置和旧位置，禁用 Minecraft 内置实体插值
 * - 完全不参与渲染（由 RtsCameraEntityRenderer 返回 false）
 */
public class RtsCameraEntity extends Entity {

    public RtsCameraEntity(EntityType<? extends RtsCameraEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * 瞬间传送到目标位置 — 同时设置旧位置，禁止插值
     * 参考 RTSbuilding.snapTo()
     */
    public void snapTo(double x, double y, double z, float yaw, float pitch) {
        this.setPos(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setOldPosAndRot();
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected void defineSynchedData() {
        // No synced data needed — client-only entity
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // No save data — not persisted
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // No save data — not persisted
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false; // 不参与可见性判断
    }
}
