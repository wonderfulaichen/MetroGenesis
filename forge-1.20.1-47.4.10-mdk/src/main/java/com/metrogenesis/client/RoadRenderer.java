package com.metrogenesis.client;

import com.metrogenesis.road.BezierRoad;
import com.metrogenesis.road.RoadBuilder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Road drawing preview — pure data holder (no event subscriber).
 *
 * Called by {@link com.metrogenesis.gui.MayorBookScreen} during drag to set
 * preview state; consumed by {@link ZoneRenderer} which renders it inside
 * its own begin/end block.
 *
 * ── Data flow ────────────────────────────────────────────
 * 1. MayorBookScreen calls {@link #setPreview} on mouse drag
 * 2. ZoneRenderer reads preview state via {@link #isActive}/{@link #getPreview}
 * 3. ZoneRenderer renders the road strip in its own RenderLevelStageEvent handler
 * 4. MayorBookScreen calls {@link #clearPreview} when drag ends or screen closes
 *
 * @see ZoneRenderer
 */
public class RoadRenderer {

    /** Start node block coordinates. */
    private static volatile int fromBX = Integer.MIN_VALUE, fromBZ = Integer.MIN_VALUE;

    /** End node block coordinates. */
    private static volatile int toBX = Integer.MIN_VALUE, toBZ = Integer.MIN_VALUE;

    /** Whether the preview is active. */
    private static volatile boolean active = false;

    /** Whether this is a "pending confirmation" projection (blue) vs drag preview (gray). */
    private static volatile boolean pending = false;

    /** Bezier curvature [-1, 1], 0 = straight line. */
    private static volatile float curvature = 0f;

    /** Whether the current preview uses bezier curve (true) or straight line (false). */
    private static volatile boolean useBezier = false;

    /** Sampled bezier curve path points. */
    private static volatile List<Vec3> curvePath = Collections.emptyList();

    /** Yaw angles at each curve path point (radians). */
    private static volatile List<Float> curveYaws = Collections.emptyList();

    /**
     * Activate the road preview between two block-level nodes.
     * Call this from the screen's tick/render method during drag.
     *
     * @param isPending true = 待确认投影（蓝绿色），false = 拖拽预览（暖灰色）
     */
    public static void setPreview(int fBX, int fBZ, int tBX, int tBZ, boolean isPending) {
        fromBX = fBX;
        fromBZ = fBZ;
        toBX   = tBX;
        toBZ   = tBZ;
        active = true;
        pending = isPending;
        // 重置贝塞尔状态，确保切换到直道模式后不走贝塞尔分支
        useBezier = false;
        curvature = 0f;
        curvePath = Collections.emptyList();
        curveYaws = Collections.emptyList();
    }

    /**
     * Activate bezier curve road preview between two block-level nodes.
     * Call this from the screen's tick/render method during drag or pending.
     *
     * <p>Bezier curve controls 2D XZ shape only; Y comes from terrain height.</p>
     *
     * @param level     client world (for terrain height sampling); null = fallback Y=0
     * @param fBX       start block X
     * @param fBZ       start block Z
     * @param tBX       end block X
     * @param tBZ       end block Z
     * @param curv      curvature [-1, 1], 0 = straight line
     * @param isPending true = pending confirmation (teal), false = drag preview (gray)
     */
    public static void setBezierPreview(Level level, int fBX, int fBZ, int tBX, int tBZ, float curv, boolean isPending) {
        fromBX = fBX;
        fromBZ = fBZ;
        toBX = tBX;
        toBZ = tBZ;
        active = true;
        pending = isPending;
        curvature = curv;
        useBezier = true;

        // 用地形高度代替 Y=0，贝塞尔只控制 XZ 平面弯道形状
        double startY = level != null ? RoadBuilder.getTrueTerrainHeight(level, fBX, fBZ) : 0;
        double endY   = level != null ? RoadBuilder.getTrueTerrainHeight(level, tBX, tBZ) : 0;

        // Compute bezier path (2D curvature in XZ plane)
        Vec3 A = new Vec3(fBX + 0.5, startY, fBZ + 0.5);
        Vec3 B = new Vec3(tBX + 0.5, endY,   tBZ + 0.5);
        Vec3 C = BezierRoad.controlPointFromCurvature(A, B, curv);
        int samples = BezierRoad.autoSampleCount(A, B);

        List<Vec3> path = new ArrayList<>(samples + 1);
        List<Float> yaws = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float t = (float) i / samples;
            Vec3 pos = BezierRoad.sample(A, C, B, t);
            Vec3 tan = BezierRoad.tangent(A, C, B, t);
            path.add(pos);
            yaws.add(BezierRoad.yawFromTangent(tan));
        }
        curvePath = path;
        curveYaws = yaws;
    }

    /**
     * Deactivate the road preview.
     * Call this when the drag operation ends or the screen closes.
     */
    public static void clearPreview() {
        active = false;
        pending = false;
        useBezier = false;
        curvature = 0f;
        curvePath = Collections.emptyList();
        curveYaws = Collections.emptyList();
    }

    // ══ Getters consumed by ZoneRenderer ═══════════════

    /** Whether a road preview is active. */
    public static boolean isActive() { return active; }

    /** Whether this is a pending confirmation projection. */
    public static boolean isPending() { return pending; }

    /**
     * @return int[] of [fromBX, fromBZ, toBX, toBZ] or null if inactive
     */
    public static int[] getPreview() {
        return active ? new int[]{fromBX, fromBZ, toBX, toBZ} : null;
    }

    /** Current bezier curvature [-1, 1]. */
    public static float getCurvature() { return curvature; }

    /** Whether the current preview uses bezier curve. */
    public static boolean isUseBezier() { return useBezier; }

    /** Sampled bezier curve path points. */
    public static List<Vec3> getCurvePath() { return curvePath; }

    /** Yaw angles at each curve path point (radians). */
    public static List<Float> getCurveYaws() { return curveYaws; }
}
