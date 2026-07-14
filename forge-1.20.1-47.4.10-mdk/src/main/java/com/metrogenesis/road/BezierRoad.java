package com.metrogenesis.road;

import net.minecraft.world.phys.Vec3;

/**
 * 贝塞尔曲线道路计算工具类。
 * <p>
 * 提供二次贝塞尔曲线的采样、切线计算、曲率→控制点转换等核心算法。
 * 所有方法均为静态纯函数，无副作用。
 * </p>
 *
 * <h3>数学背景</h3>
 * 二次贝塞尔曲线：P(t) = (1-t)²·A + 2t(1-t)·C + t²·B
 * <ul>
 *   <li>A = 起点（方块坐标中心）</li>
 *   <li>B = 终点（方块坐标中心）</li>
 *   <li>C = 控制点</li>
 *   <li>t ∈ [0, 1] 参数</li>
 * </ul>
 *
 * @author program-yuan (Phase 1)
 */
public final class BezierRoad {

    /** 不可实例化 */
    private BezierRoad() {}

    // ════════════════════════════════════════════════════════
    //  核心采样
    // ════════════════════════════════════════════════════════

    /**
     * 二次贝塞尔曲线采样。
     * <pre>
     * P(t) = (1-t)²·A + 2t(1-t)·C + t²·B
     * </pre>
     *
     * @param A 起点（方块坐标，中心点）
     * @param C 控制点
     * @param B 终点（方块坐标，中心点）
     * @param t 参数 [0, 1]
     * @return 曲线上的点（世界坐标）
     */
    public static Vec3 sample(Vec3 A, Vec3 C, Vec3 B, float t) {
        float u = 1 - t;
        return new Vec3(
            u * u * A.x + 2 * u * t * C.x + t * t * B.x,
            u * u * A.y + 2 * u * t * C.y + t * t * B.y,
            u * u * A.z + 2 * u * t * C.z + t * t * B.z
        );
    }

    /**
     * 曲线在参数 t 处的切线方向（已归一化）。
     * <p>
     * 切线 = dP/dt，用于确定模板旋转角度。
     * </p>
     *
     * @param A 起点
     * @param C 控制点
     * @param B 终点
     * @param t 参数 [0, 1]
     * @return 归一化的切线向量；若切线长度为零则返回零向量
     */
    public static Vec3 tangent(Vec3 A, Vec3 C, Vec3 B, float t) {
        float u = 1 - t;
        Vec3 raw = new Vec3(
            2 * u * (C.x - A.x) + 2 * t * (B.x - C.x),
            2 * u * (C.y - A.y) + 2 * t * (B.y - C.y),
            2 * u * (C.z - A.z) + 2 * t * (B.z - C.z)
        );
        double len = raw.length();
        if (len < 1.0E-6) {
            return Vec3.ZERO;
        }
        return raw.scale(1.0 / len);
    }

    // ════════════════════════════════════════════════════════
    //  曲率 → 控制点
    // ════════════════════════════════════════════════════════

    /**
     * 根据曲率计算控制点坐标。
     * <p>
     * 控制点位于起终点中垂面上，沿水平垂直方向偏移：
     * <pre>
     * mid = (A + B) / 2
     * dir = B - A
     * perp = normalize(-dir.z, 0, dir.x)   // 水平面垂直方向
     * offset = curvature * distance(A, B) * 0.5
     * controlPoint = mid + perp * offset
     * </pre>
     * </p>
     *
     * @param start     起点（方块坐标中心）
     * @param end       终点（方块坐标中心）
     * @param curvature 曲率 [-1.0, 1.0]，0 = 直线，正 = 右弯，负 = 左弯
     * @return 控制点坐标
     */
    public static Vec3 controlPointFromCurvature(Vec3 start, Vec3 end, float curvature) {
        // 起终点中点
        Vec3 mid = start.add(end).scale(0.5);
        // 起→终方向向量
        Vec3 dir = end.subtract(start);
        // 水平面上的垂直方向（右手旋转90°）
        Vec3 perp = new Vec3(-dir.z, 0, dir.x);
        double perpLen = perp.length();
        if (perpLen < 1.0E-6) {
            // 起终点重合，控制点就是中点
            return mid;
        }
        perp = perp.scale(1.0 / perpLen);
        // 偏移量 = 曲率 × 距离的一半
        double offset = curvature * start.distanceTo(end) * 0.5;
        return mid.add(perp.scale(offset));
    }

    // ════════════════════════════════════════════════════════
    //  角度计算
    // ════════════════════════════════════════════════════════

    /**
     * 根据切线方向计算水平面上的 yaw 角度（弧度）。
     * <p>
     * 约定：0 = +X 方向，π/2 = +Z 方向（与 Minecraft atan2 一致）。
     * </p>
     *
     * @param tangent 归一化的切线向量
     * @return yaw 角度（弧度）
     */
    public static float yawFromTangent(Vec3 tangent) {
        return (float) Math.atan2(tangent.z, tangent.x);
    }

    /**
     * 根据切线方向计算 yaw 角度（度），方便调试输出。
     *
     * @param tangent 归一化的切线向量
     * @return yaw 角度（度）
     */
    public static float yawDegrees(Vec3 tangent) {
        return (float) Math.toDegrees(yawFromTangent(tangent));
    }

    // ════════════════════════════════════════════════════════
    //  实用工具
    // ════════════════════════════════════════════════════════

    /**
     * 估算曲线弧长（分段线性近似）。
     * <p>
     * 用于决定采样密度：长弧 → 更多采样点。
     * </p>
     *
     * @param A       起点
     * @param C       控制点
     * @param B       终点
     * @param segments 分段数（越大越精确，推荐 ≥ 起终点距离）
     * @return 估算弧长
     */
    public static double arcLength(Vec3 A, Vec3 C, Vec3 B, int segments) {
        double length = 0;
        Vec3 prev = A;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            Vec3 cur = sample(A, C, B, t);
            length += prev.distanceTo(cur);
            prev = cur;
        }
        return length;
    }

    /**
     * 根据起终点距离自动计算合适的采样数。
     * <p>
     * 规则：每 1 格至少 1 个采样点，最少 2。
     * </p>
     *
     * @param A 起点
     * @param B 终点
     * @return 推荐采样数量
     */
    public static int autoSampleCount(Vec3 A, Vec3 B) {
        return Math.max(2, (int) Math.ceil(A.distanceTo(B)));
    }
}
