package com.metrogenesis.road;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条贝塞尔曲线道路段。
 * <p>
 * 由起终点（BlockPos）和曲率定义，内部自动计算控制点。
 * 提供路径采样（生成 PathPoint 列表）和 NBT 序列化能力。
 * </p>
 *
 * <h3>数据流</h3>
 * <pre>
 * 曲率 curvature → controlPointFromCurvature() → 控制点
 * (start, controlPoint, end) → samplePath() → List&lt;PathPoint&gt;
 * 每个 PathPoint = (位置 Vec3, yaw 角度)
 * </pre>
 *
 * @author program-yuan (Phase 1)
 */
public class BezierSegment {

    // ══ 字段 ══════════════════════════════════════════════

    /** 起点（方块坐标） */
    private BlockPos start;

    /** 终点（方块坐标） */
    private BlockPos end;

    /** 控制点（世界坐标，浮点精度） */
    private Vec3 controlPoint;

    /** 曲率 [-1.0, 1.0]，0 = 直线 */
    private float curvature;

    /** 使用的道路模板名称（Phase 2 填充） */
    private String templateName;

    // ══ 构造 ══════════════════════════════════════════════

    /**
     * 创建贝塞尔道路段。
     *
     * @param start     起点方块坐标
     * @param end       终点方块坐标
     * @param curvature 曲率 [-1.0, 1.0]
     */
    public BezierSegment(BlockPos start, BlockPos end, float curvature) {
        this.start = start;
        this.end = end;
        this.curvature = clampCurvature(curvature);
        this.controlPoint = BezierRoad.controlPointFromCurvature(
            Vec3.atCenterOf(start),
            Vec3.atCenterOf(end),
            this.curvature
        );
        this.templateName = "";
    }

    /**
     * 完整构造（含模板名和自定义控制点，用于 NBT 加载）。
     */
    private BezierSegment(BlockPos start, BlockPos end, float curvature, Vec3 controlPoint, String templateName) {
        this.start = start;
        this.end = end;
        this.curvature = curvature;
        this.controlPoint = controlPoint;
        this.templateName = templateName != null ? templateName : "";
    }

    // ══ 路径采样 ══════════════════════════════════════════

    /**
     * 沿曲线等距采样路径点。
     * <p>
     * 每个 PathPoint 包含：
     * <ul>
     *   <li>position — 曲线上的世界坐标</li>
     *   <li>yaw — 切线方向对应的角度（弧度），用于模板旋转</li>
     * </ul>
     * </p>
     *
     * @param samples 采样数量（含首尾，最少 2）
     * @return 路径点列表
     */
    public List<PathPoint> samplePath(int samples) {
        if (samples < 2) samples = 2;

        List<PathPoint> points = new ArrayList<>(samples + 1);
        Vec3 A = Vec3.atCenterOf(start);
        Vec3 B = Vec3.atCenterOf(end);
        Vec3 C = controlPoint;

        for (int i = 0; i <= samples; i++) {
            float t = (float) i / samples;
            Vec3 pos = BezierRoad.sample(A, C, B, t);
            Vec3 tan = BezierRoad.tangent(A, C, B, t);
            float yaw = BezierRoad.yawFromTangent(tan);
            points.add(new PathPoint(pos, yaw));
        }
        return points;
    }

    /**
     * 自动采样（根据起终点距离决定密度）。
     *
     * @return 路径点列表
     */
    public List<PathPoint> samplePath() {
        int count = BezierRoad.autoSampleCount(
            Vec3.atCenterOf(start),
            Vec3.atCenterOf(end)
        );
        return samplePath(count);
    }

    // ══ 曲率更新 ═════════════════════════════════════════

    /**
     * 更新曲率并重新计算控制点。
     *
     * @param newCurvature 新曲率 [-1.0, 1.0]
     */
    public void setCurvature(float newCurvature) {
        this.curvature = clampCurvature(newCurvature);
        this.controlPoint = BezierRoad.controlPointFromCurvature(
            Vec3.atCenterOf(start),
            Vec3.atCenterOf(end),
            this.curvature
        );
    }

    /**
     * 直接设置控制点（用于玩家拖拽交互，Phase 3）。
     *
     * @param cp 新控制点坐标
     */
    public void setControlPoint(Vec3 cp) {
        this.controlPoint = cp;
        // 反算曲率（近似）：将控制点投影回垂直方向
        Vec3 A = Vec3.atCenterOf(start);
        Vec3 B = Vec3.atCenterOf(end);
        Vec3 mid = A.add(B).scale(0.5);
        Vec3 dir = B.subtract(A);
        double dist = dir.length();
        if (dist < 1.0E-6) {
            this.curvature = 0;
            return;
        }
        Vec3 perp = new Vec3(-dir.z, 0, dir.x).normalize();
        Vec3 offset = cp.subtract(mid);
        // 垂直方向上的投影分量
        double perpComponent = offset.dot(perp);
        this.curvature = clampCurvature((float) (perpComponent / (dist * 0.5)));
    }

    // ══ NBT 序列化 ═══════════════════════════════════════

    /**
     * 将道路段保存为 NBT。
     * <pre>
     * {
     *   start: BlockPos,
     *   end: BlockPos,
     *   curvature: float,
     *   controlPoint: {x: double, y: double, z: double},
     *   templateName: string
     * }
     * </pre>
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("start", NbtUtils.writeBlockPos(start));
        tag.put("end", NbtUtils.writeBlockPos(end));
        tag.putFloat("curvature", curvature);

        CompoundTag cpTag = new CompoundTag();
        cpTag.putDouble("x", controlPoint.x);
        cpTag.putDouble("y", controlPoint.y);
        cpTag.putDouble("z", controlPoint.z);
        tag.put("controlPoint", cpTag);

        tag.putString("templateName", templateName != null ? templateName : "");
        return tag;
    }

    /**
     * 从 NBT 加载道路段。
     *
     * @param tag NBT 数据
     * @return 反序列化的 BezierSegment，或 null（数据不合法）
     */
    public static BezierSegment load(CompoundTag tag) {
        if (!tag.contains("start") || !tag.contains("end") || !tag.contains("curvature")) {
            return null;
        }

        BlockPos start = NbtUtils.readBlockPos(tag.getCompound("start"));
        BlockPos end = NbtUtils.readBlockPos(tag.getCompound("end"));
        float curvature = tag.getFloat("curvature");

        Vec3 controlPoint;
        if (tag.contains("controlPoint")) {
            CompoundTag cpTag = tag.getCompound("controlPoint");
            controlPoint = new Vec3(cpTag.getDouble("x"), cpTag.getDouble("y"), cpTag.getDouble("z"));
        } else {
            // 旧存档兼容：从曲率重算
            controlPoint = BezierRoad.controlPointFromCurvature(
                Vec3.atCenterOf(start), Vec3.atCenterOf(end), curvature
            );
        }

        String templateName = tag.contains("templateName") ? tag.getString("templateName") : "";

        return new BezierSegment(start, end, curvature, controlPoint, templateName);
    }

    // ══ Getters ═══════════════════════════════════════════

    public BlockPos getStart() { return start; }
    public BlockPos getEnd() { return end; }
    public Vec3 getControlPoint() { return controlPoint; }
    public float getCurvature() { return curvature; }
    public String getTemplateName() { return templateName; }

    public void setStart(BlockPos start) { this.start = start; }
    public void setEnd(BlockPos end) { this.end = end; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    // ══ 内部工具 ═════════════════════════════════════════

    private static float clampCurvature(float c) {
        return Math.max(-1.0f, Math.min(1.0f, c));
    }

    // ══ 路径点记录 ═══════════════════════════════════════

    /**
     * 路径采样点。
     *
     * @param position 世界坐标（浮点）
     * @param yaw      切线方向角度（弧度）
     */
    public record PathPoint(Vec3 position, float yaw) {

        /**
         * 获取对应的方块坐标（向下取整）。
         */
        public BlockPos blockPos() {
            return new BlockPos(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
            );
        }

        /**
         * 获取水平面上的方块坐标（Y 忽略）。
         */
        public BlockPos blockPosFlat() {
            return new BlockPos(
                (int) Math.floor(position.x),
                0,
                (int) Math.floor(position.z)
            );
        }
    }

    @Override
    public String toString() {
        return "BezierSegment[start=" + start + ", end=" + end
            + ", curvature=" + curvature
            + ", cp=" + controlPoint + "]";
    }
}
