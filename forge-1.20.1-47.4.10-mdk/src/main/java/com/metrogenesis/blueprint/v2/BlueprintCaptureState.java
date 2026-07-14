package com.metrogenesis.blueprint.v2;

import net.minecraft.core.BlockPos;

/**
 * 客户端扫描状态 — 跟踪玩家正在进行的框选扫描。
 * <p>
 * 交互流程：
 * 1. 玩家左键方块 → startCapture(pos)
 * 2. 每帧渲染时从 hitResult 获取终点位置
 * 3. 玩家右键空气 → 结束扫描
 */
public class BlueprintCaptureState
{
    private static BlockPos startPos = null;
    private static float startYaw = 0f; // 左键时玩家朝向，用于确定门口方向
    private static long startTime = 0;

    /** 是否正在扫描中（已定起点，等待终点） */
    public static boolean isActive() { return startPos != null; }

    /** 获取起点 */
    public static BlockPos getStartPos() { return startPos; }

    /** 获取左键时的玩家朝向（yaw），用于确定入口面方向 */
    public static float getStartYaw() { return startYaw; }

    /** 开始扫描（左键点击方块时触发） */
    public static void startCapture(BlockPos pos, float playerYaw)
    {
        startPos = pos;
        startYaw = playerYaw;
        startTime = System.currentTimeMillis();
    }

    /** 结束扫描（右键确认时触发） */
    public static void finishCapture()
    {
        startPos = null;
        startTime = 0;
    }

    /** 取消扫描 */
    public static void cancelCapture()
    {
        startPos = null;
        startTime = 0;
    }
}
