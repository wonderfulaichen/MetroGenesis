package com.metrogenesis.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 射线-轴对齐平面交点工具（Effortless 风格）。
 * <p>
 * 核心思路：以起点定义三个轴对齐平面（X/Y/Z），
 * 玩家视线射线与每个平面求交，取有效交点中距离眼睛最近的那个，
 * 从而实现"自由3D拖拽"选框终点——不依赖准星打到方块。
 * <p>
 * 平面选择逻辑（对齐 Effortless 的 {@code snapToGrid}）：
 * 优先根据<strong>眼睛位置</strong>选择平面（眼睛在方块哪侧就选哪侧的面），
 * 仅当眼睛在方块内部时才回退到视线方向。
 * <p>
 * 公式（以 X 平面为例）：
 * <pre>
 *   eye + t * look = point
 *   point.x = planeX  →  t = (planeX - eye.x) / look.x
 *   point.y = eye.y + t * look.y
 *   point.z = eye.z + t * look.z
 * </pre>
 */
public final class RayPlaneUtil
{
    private RayPlaneUtil() {}

    /** 有效射线距离上限（方块） */
    private static final double MAX_REACH = 64.0;

    /** 数值精度阈值，防止接近平行时的数值爆炸 */
    private static final double EPSILON = 1e-6;

    /** look 向量分量的最小绝对值，防止除以 near-zero（对齐 Effortless LOOK_VEC_TOLERANCE） */
    private static final double LOOK_TOLERANCE = 0.01;

    /**
     * 根据玩家视线方向，计算选框终点的自由3D位置。
     *
     * @param player   玩家
     * @param startPos 起点（左键点击的方块坐标）
     * @return 选框终点方块坐标，无法计算时返回 null
     */
    public static BlockPos getFreeEndPos(Player player, BlockPos startPos)
    {
        if (player == null || startPos == null) return null;

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = protectLookVec(player.getLookAngle().normalize());

        // ── snapToGrid：根据眼睛位置选择平面 ──
        // 对齐 Effortless BlockStructure.snapToGrid 逻辑：
        //   眼睛在方块右侧 → 平面在方块右面 (start+1)
        //   眼睛在方块左侧 → 平面在方块左面 (start)
        //   眼睛在方块内部 → 回退到视线方向
        double planeX = snapToGrid(startPos.getX(), eye.x, look.x);
        double planeY = snapToGrid(startPos.getY(), eye.y, look.y);
        double planeZ = snapToGrid(startPos.getZ(), eye.z, look.z);

        // bestHolder = [distSq, x, y, z]
        double[] best = { Double.MAX_VALUE, 0, 0, 0 };
        double[] tempResult = new double[3];

        // 对 X / Y / Z 三个轴分别求射线-平面交点
        if (intersectAxis(eye, look, planeX, 'x', tempResult))
            tryUpdate(tempResult, eye, best);
        if (intersectAxis(eye, look, planeY, 'y', tempResult))
            tryUpdate(tempResult, eye, best);
        if (intersectAxis(eye, look, planeZ, 'z', tempResult))
            tryUpdate(tempResult, eye, best);

        if (best[0] == Double.MAX_VALUE) return null;

        // 取整到方块坐标（floor）
        return new BlockPos(
            (int) Math.floor(best[1]),
            (int) Math.floor(best[2]),
            (int) Math.floor(best[3]));
    }

    /**
     * 对齐 Effortless {@code BlockStructure.snapToGrid}：
     * 根据眼睛位置选择平面通过的方块边界。
     * <p>
     * 方块占据 [blockPos, blockPos+1]，中心在 blockPos+0.5。
     * <ul>
     *   <li>eye >= blockPos+0.5 → 平面在 blockPos+1（右/上/南面）</li>
     *   <li>eye <= blockPos-0.5 → 平面在 blockPos（左/下/北面）</li>
     *   <li>look > 0 → 平面在 blockPos+1（眼睛在方块内，看向正方向）</li>
     *   <li>look < 0 → 平面在 blockPos（眼睛在方块内，看向负方向）</li>
     * </ul>
     */
    private static double snapToGrid(int blockPos, double eye, double look)
    {
        double center = blockPos + 0.5;
        if (eye >= center)       return blockPos + 1.0;   // 眼睛在方块中心右侧 → 右面
        if (eye <= blockPos - 0.5) return (double) blockPos; // 眼睛在方块左面左侧 → 左面
        if (look > 0)            return blockPos + 1.0;   // 眼睛在方块内，看向正方向
        if (look < 0)            return (double) blockPos; // 眼睛在方块内，看向负方向
        return center;                                     // fallback（look≈0 时不会走到这里）
    }

    /**
     * 保护 look 向量，防止分量接近 0 或 ±1 时的数值爆炸。
     * 对齐 Effortless {@code BlockStructure.getEntityLookAngleGap}。
     */
    private static Vec3 protectLookVec(Vec3 look)
    {
        double x = look.x, y = look.y, z = look.z;

        // 防止分量接近 0（除以 near-zero）
        if (Math.abs(x) < LOOK_TOLERANCE) x = Math.signum(x) == 0 ? LOOK_TOLERANCE : Math.signum(x) * LOOK_TOLERANCE;
        if (Math.abs(y) < LOOK_TOLERANCE) y = Math.signum(y) == 0 ? LOOK_TOLERANCE : Math.signum(y) * LOOK_TOLERANCE;
        if (Math.abs(z) < LOOK_TOLERANCE) z = Math.signum(z) == 0 ? LOOK_TOLERANCE : Math.signum(z) * LOOK_TOLERANCE;

        // 防止分量接近 ±1（归一化后其他分量接近 0）
        if (Math.abs(x - 1.0) < LOOK_TOLERANCE) x = 1.0 - LOOK_TOLERANCE;
        if (Math.abs(x + 1.0) < LOOK_TOLERANCE) x = -1.0 + LOOK_TOLERANCE;
        if (Math.abs(y - 1.0) < LOOK_TOLERANCE) y = 1.0 - LOOK_TOLERANCE;
        if (Math.abs(y + 1.0) < LOOK_TOLERANCE) y = -1.0 + LOOK_TOLERANCE;
        if (Math.abs(z - 1.0) < LOOK_TOLERANCE) z = 1.0 - LOOK_TOLERANCE;
        if (Math.abs(z + 1.0) < LOOK_TOLERANCE) z = -1.0 + LOOK_TOLERANCE;

        return new Vec3(x, y, z).normalize();
    }

    /**
     * 计算视线射线与轴对齐平面的交点。
     */
    private static boolean intersectAxis(Vec3 eye, Vec3 look, double planeCoord, char axis, double[] result)
    {
        double denominator;
        switch (axis)
        {
            case 'x': denominator = look.x; break;
            case 'y': denominator = look.y; break;
            case 'z': denominator = look.z; break;
            default:  return false;
        }
        if (Math.abs(denominator) < EPSILON) return false;

        double t = (planeCoord - switch (axis) {
            case 'x' -> eye.x;
            case 'y' -> eye.y;
            case 'z' -> eye.z;
            default -> 0;
        }) / denominator;

        // 排除身后交点（t < 0），但允许浮点误差
        if (t < -EPSILON) return false;

        // 距离校验
        double distSq = t * t; // look 已归一化，t 就是距离
        if (distSq > MAX_REACH * MAX_REACH) return false;

        result[0] = eye.x + t * look.x;
        result[1] = eye.y + t * look.y;
        result[2] = eye.z + t * look.z;
        return true;
    }

    /**
     * 尝试用候选点更新最佳结果（取距离眼睛最近的）。
     */
    private static void tryUpdate(double[] point, Vec3 eye, double[] best)
    {
        if (point == null) return;
        double dx = point[0] - eye.x;
        double dy = point[1] - eye.y;
        double dz = point[2] - eye.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < best[0])
        {
            best[0] = distSq;
            best[1] = point[0];
            best[2] = point[1];
            best[3] = point[2];
        }
    }
}
