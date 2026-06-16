package com.metrogenesis.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.metrogenesis.blueprint.v2.BlueprintCaptureState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;


/**
 * 蓝图之眼 — 选区方框渲染器（WorldEdit 风格）
 * <p>
 * 数据来源：BlueprintEyeItem → BlueprintCaptureState 静态状态（起点 + 鼠标指向方块）
 * <p>
 * 渲染方式：半透明面 + 边框线，两遍渲染。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScanToolSelectionRenderer {

    private static final String TAG_FIRST = "firstPos";
    private static final String TAG_SECOND = "secondPos";

    // ── 颜色 ──
    private static final float FACE_R = 0.2f, FACE_G = 0.6f, FACE_B = 1.0f, FACE_A = 0.15f;
    private static final float LINE_R = 0.4f, LINE_G = 0.8f, LINE_B = 1.0f, LINE_A = 0.9f;
    // 入口面高亮 — 橙红色填充 + 红色加粗边框，更醒目
    private static final float ENTRY_FILL_R = 1.0f, ENTRY_FILL_G = 0.4f, ENTRY_FILL_B = 0.2f, ENTRY_FILL_A = 0.5f;
    private static final float ENTRY_EDGE_R = 1.0f, ENTRY_EDGE_G = 0.2f, ENTRY_EDGE_B = 0.0f, ENTRY_EDGE_A = 1.0f;

    /** 缓存上一次有效的终点位置（MISS 时保持选框不消失） */
    private static BlockPos lastFirst = null;
    private static BlockPos lastValidSecond = null;
    private static float lastPlayerYaw = 0f; // 缓存玩家 yaw 供入口面判定

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        BlockPos first = null;
        BlockPos second = null;

        // ── 来源: BlueprintEye 静态状态（自由3D定位） ──
        if (BlueprintCaptureState.isActive()) {
            first = BlueprintCaptureState.getStartPos();
            // 方式 1（优先）: 准星指向的实体方块 — 确保选框各轴独立伸缩
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                second = ((BlockHitResult) mc.hitResult).getBlockPos();
            }
            // 方式 2（MISS 兜底）: 射线-平面交点 — 准星指向空气时仍可选框
            if (second == null && player != null && first != null) {
                second = RayPlaneUtil.getFreeEndPos(player, first);
            }
            // 缓存
            if (second != null) lastValidSecond = second;
            // MISS 时使用缓存的 lastValidSecond 保持选框不消失
            if (second == null) second = lastValidSecond;
            if (first == null) first = lastFirst;
        }

        // scan_tool 来源已移除

        if (first == null || second == null) return;

        // 缓存 first/lastValidSecond 供后续入口面判定使用
        lastFirst = first;
        if (second != lastValidSecond) { lastValidSecond = second; }
        // 入口面方向使用左键时锁定的玩家朝向（不再实时更新）
        lastPlayerYaw = BlueprintCaptureState.getStartYaw();

        // ── 计算包围盒 ──
        Vec3 camera = event.getCamera().getPosition();
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()) + 1;
        int maxY = Math.max(first.getY(), second.getY()) + 1;
        int maxZ = Math.max(first.getZ(), second.getZ()) + 1;

        float x1 = (float) (minX - camera.x);
        float y1 = (float) (minY - camera.y);
        float z1 = (float) (minZ - camera.z);
        float x2 = (float) (maxX - camera.x);
        float y2 = (float) (maxY - camera.y);
        float z2 = (float) (maxZ - camera.z);

        Matrix4f pose = event.getPoseStack().last().pose();

        // ── Pass 1: 半透明面 ──
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);  // 防止半透明面写入深度缓冲区
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addQuad(buffer, pose, x1,y1,z1, x2,y1,z1, x2,y2,z1, x1,y2,z1); // 北
        addQuad(buffer, pose, x1,y1,z2, x1,y2,z2, x2,y2,z2, x2,y1,z2); // 南
        addQuad(buffer, pose, x1,y1,z1, x1,y2,z1, x1,y2,z2, x1,y1,z2); // 西
        addQuad(buffer, pose, x2,y1,z1, x2,y1,z2, x2,y2,z2, x2,y2,z1); // 东
        addQuad(buffer, pose, x1,y1,z1, x1,y1,z2, x2,y1,z2, x2,y1,z1); // 底
        addQuad(buffer, pose, x1,y2,z1, x2,y2,z1, x2,y2,z2, x1,y2,z2); // 顶

        BufferUploader.drawWithShader(buffer.end());

        // ── Pass 2: 边框线 ──
        RenderSystem.lineWidth(2.0f);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        // 底部 4 条
        addLine(buffer, pose, x1,y1,z1, x2,y1,z1);
        addLine(buffer, pose, x2,y1,z1, x2,y1,z2);
        addLine(buffer, pose, x2,y1,z2, x1,y1,z2);
        addLine(buffer, pose, x1,y1,z2, x1,y1,z1);
        // 顶部 4 条
        addLine(buffer, pose, x1,y2,z1, x2,y2,z1);
        addLine(buffer, pose, x2,y2,z1, x2,y2,z2);
        addLine(buffer, pose, x2,y2,z2, x1,y2,z2);
        addLine(buffer, pose, x1,y2,z2, x1,y2,z1);
        // 4 条竖线
        addLine(buffer, pose, x1,y1,z1, x1,y2,z1);
        addLine(buffer, pose, x2,y1,z1, x2,y2,z1);
        addLine(buffer, pose, x2,y1,z2, x2,y2,z2);
        addLine(buffer, pose, x1,y1,z2, x1,y2,z2);

        BufferUploader.drawWithShader(buffer.end());

        // ── Pass 3: 入口面黄色填充 ──
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        renderEntryFaceFill(buffer, pose, x1, y1, z1, x2, y2, z2);
        BufferUploader.drawWithShader(buffer.end());

        // ── Pass 4: 入口面加粗绿色边框 ──
        RenderSystem.lineWidth(3.0f);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        renderEntryEdge(buffer, pose, x1, y1, z1, x2, y2, z2);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.lineWidth(1.0f);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);  // 恢复深度写入
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** 根据缓存的玩家 yaw 判定入口面（MC yaw: 0=南, 90=西, ±180=北, -90=东）
     *  入口面 = 建筑朝向玩家的面（玩家面前的面） */
    private static int getPlayerFacingFace()
    {
        float yaw = lastPlayerYaw % 360;
        if (yaw < -180) yaw += 360;
        if (yaw >= 180) yaw -= 360;
        if (yaw >= -45 && yaw < 45)   return 3; // 面朝南 → 入口在北面(-Z)
        if (yaw >= 45 && yaw < 135)   return 0; // 面朝西 → 入口在东面(+X)
        if (yaw >= -135 && yaw < -45) return 1; // 面朝东 → 入口在西面(-X)
        return 2; // 面朝北 → 入口在南面(+Z)
    }

    /** 渲染入口面黄色填充 */
    private static void renderEntryFaceFill(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        int face = getPlayerFacingFace();
        switch (face)
        {
            case 0: // +X (东)
                entryQuad(buf, mat, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
                break;
            case 1: // -X (西)
                entryQuad(buf, mat, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2);
                break;
            case 2: // +Z (南)
                entryQuad(buf, mat, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
                break;
            case 3: // -Z (北)
                entryQuad(buf, mat, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1);
                break;
        }
    }

    /** 渲染入口面加粗绿色边框 */
    private static void renderEntryEdge(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        int face = getPlayerFacingFace();
        switch (face)
        {
            case 0: // +X (东)
                entryEdgeLine(buf, mat, x2, y1, z1, x2, y1, z2);
                entryEdgeLine(buf, mat, x2, y1, z2, x2, y2, z2);
                entryEdgeLine(buf, mat, x2, y2, z2, x2, y2, z1);
                entryEdgeLine(buf, mat, x2, y2, z1, x2, y1, z1);
                break;
            case 1: // -X (西)
                entryEdgeLine(buf, mat, x1, y1, z1, x1, y1, z2);
                entryEdgeLine(buf, mat, x1, y1, z2, x1, y2, z2);
                entryEdgeLine(buf, mat, x1, y2, z2, x1, y2, z1);
                entryEdgeLine(buf, mat, x1, y2, z1, x1, y1, z1);
                break;
            case 2: // +Z (南)
                entryEdgeLine(buf, mat, x1, y1, z2, x2, y1, z2);
                entryEdgeLine(buf, mat, x2, y1, z2, x2, y2, z2);
                entryEdgeLine(buf, mat, x2, y2, z2, x1, y2, z2);
                entryEdgeLine(buf, mat, x1, y2, z2, x1, y1, z2);
                break;
            case 3: // -Z (北)
                entryEdgeLine(buf, mat, x2, y1, z1, x1, y1, z1);
                entryEdgeLine(buf, mat, x1, y1, z1, x1, y2, z1);
                entryEdgeLine(buf, mat, x1, y2, z1, x2, y2, z1);
                entryEdgeLine(buf, mat, x2, y2, z1, x2, y1, z1);
                break;
        }
    }

    /** 入口面四边形（2 个三角形） */
    private static void entryQuad(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float x4, float y4, float z4)
    {
        buf.vertex(mat, x1, y1, z1).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
        buf.vertex(mat, x2, y2, z2).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
        buf.vertex(mat, x3, y3, z3).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
        buf.vertex(mat, x1, y1, z1).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
        buf.vertex(mat, x3, y3, z3).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
        buf.vertex(mat, x4, y4, z4).color(ENTRY_FILL_R, ENTRY_FILL_G, ENTRY_FILL_B, ENTRY_FILL_A).endVertex();
    }

    /** 入口面绿色边框线 */
    private static void entryEdgeLine(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        buf.vertex(mat, x1, y1, z1).color(ENTRY_EDGE_R, ENTRY_EDGE_G, ENTRY_EDGE_B, ENTRY_EDGE_A).endVertex();
        buf.vertex(mat, x2, y2, z2).color(ENTRY_EDGE_R, ENTRY_EDGE_G, ENTRY_EDGE_B, ENTRY_EDGE_A).endVertex();
    }

    // ── 绘制一个四边形 ──
    private static void addQuad(BufferBuilder buf, Matrix4f pose,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4) {
        buf.vertex(pose, x1, y1, z1).color(FACE_R, FACE_G, FACE_B, FACE_A).endVertex();
        buf.vertex(pose, x2, y2, z2).color(FACE_R, FACE_G, FACE_B, FACE_A).endVertex();
        buf.vertex(pose, x3, y3, z3).color(FACE_R, FACE_G, FACE_B, FACE_A).endVertex();
        buf.vertex(pose, x4, y4, z4).color(FACE_R, FACE_G, FACE_B, FACE_A).endVertex();
    }

    // ── 绘制一条线 ──
    private static void addLine(BufferBuilder buf, Matrix4f pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2) {
        buf.vertex(pose, x1, y1, z1).color(LINE_R, LINE_G, LINE_B, LINE_A).endVertex();
        buf.vertex(pose, x2, y2, z2).color(LINE_R, LINE_G, LINE_B, LINE_A).endVertex();
    }
}
