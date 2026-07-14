package com.metrogenesis.blueprint.v2;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.road.RoadBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.metrogenesis.client.renderer.RayPlaneUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 蓝图之眼选框渲染器 — 3D 半透明蓝色线框 + 入口面黄色高亮。
 * <p>
 * 当玩家手持 BlueprintEyeItem 且处于扫描状态时渲染：
 * <ul>
 *   <li>整体线框：12 条边蓝色半透明 (#4488FF 0.5)</li>
 *   <li>入口面：黄色填充 (#FFCC00 0.3) + 绿色加粗边框 (#00FF44 1.0)</li>
 *   <li>尺寸标注：悬浮文字 "W×H×D"</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlueprintSelectionRenderer
{
    // 颜色常量
    private static final float LINE_R = 0.267f;  // #4488FF → 0.267/0.533/1.0
    private static final float LINE_G = 0.533f;
    private static final float LINE_B = 1.0f;
    private static final float LINE_A = 0.5f;

    private static final float ENTRY_FILL_R = 1.0f;  // #FF6633 0.5 — 橙红色，更醒目
    private static final float ENTRY_FILL_G = 0.4f;
    private static final float ENTRY_FILL_B = 0.2f;
    private static final float ENTRY_FILL_A = 0.5f;

    private static final float ENTRY_EDGE_R = 1.0f;  // #FF3300 1.0 — 红色加粗边框
    private static final float ENTRY_EDGE_G = 0.2f;
    private static final float ENTRY_EDGE_B = 0.0f;
    private static final float ENTRY_EDGE_A = 1.0f;

    // 缓存上一帧的选框信息用于文本标注
    private static int lastMinX, lastMinY, lastMinZ;
    private static int lastSizeX, lastSizeY, lastSizeZ;
    private static int lastEntryFace; // 0=+X(东), 1=-X(西), 2=+Z(南), 3=-Z(北)

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        // 只在扫描状态下渲染
        if (!BlueprintCaptureState.isActive()) return;

        // 避免与 ScanToolSelectionRenderer 重影：如果主手/副手没有 BlueprintEyeItem 则跳过
        // （ScanToolSelectionRenderer 会处理所有情况）
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // 检查手持物品
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof com.metrogenesis.item.BlueprintEyeItem))
        {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof com.metrogenesis.item.BlueprintEyeItem)) return;
        }

        BlockPos start = BlueprintCaptureState.getStartPos();
        if (start == null) return;

        // 获取鼠标指向的方块作为终点（实时预览）
        BlockPos end = getMouseTargetBlock(mc, start);
        if (end == null) return;

        // 计算选框边界
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX()) + 1;
        int maxY = Math.max(start.getY(), end.getY()) + 1;
        int maxZ = Math.max(start.getZ(), end.getZ()) + 1;

        // 缓存信息供文本标注（在渲染循环外绘制）
        lastMinX = minX; lastMinY = minY; lastMinZ = minZ;
        lastSizeX = maxX - minX; lastSizeY = maxY - minY; lastSizeZ = maxZ - minZ;
        lastEntryFace = getPlayerFacingFace(player);

        // ══ 渲染线框 + 入口面 ═══════════════════════
        Vec3 cam = event.getCamera().getPosition();
        float ox = (float) (minX - cam.x);
        float oy = (float) (minY - cam.y);
        float oz = (float) (minZ - cam.z);
        float fx = (float) (maxX - cam.x);
        float fy = (float) (maxY - cam.y);
        float fz = (float) (maxZ - cam.z);

        Matrix4f mat = event.getPoseStack().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // ── Step 1: 入口面黄色填充 ──
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        renderEntryFaceFill(buf, mat, ox, oy, oz, fx, fy, fz);
        BufferUploader.drawWithShader(buf.end());

        // ── Step 2: 12 条边蓝色线框 ──
        buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        renderWireframe(buf, mat, ox, oy, oz, fx, fy, fz);
        BufferUploader.drawWithShader(buf.end());

        // ── Step 3: 入口面加粗绿色边框 ──
        RenderSystem.lineWidth(3.0f);
        buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        renderEntryEdge(buf, mat, ox, oy, oz, fx, fy, fz);
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.lineWidth(1.0f);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** 渲染 12 条蓝色线框 */
    private static void renderWireframe(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        // 底部 4 条
        line(buf, mat, x1, y1, z1, x2, y1, z1);
        line(buf, mat, x2, y1, z1, x2, y1, z2);
        line(buf, mat, x2, y1, z2, x1, y1, z2);
        line(buf, mat, x1, y1, z2, x1, y1, z1);
        // 顶部 4 条
        line(buf, mat, x1, y2, z1, x2, y2, z1);
        line(buf, mat, x2, y2, z1, x2, y2, z2);
        line(buf, mat, x2, y2, z2, x1, y2, z2);
        line(buf, mat, x1, y2, z2, x1, y2, z1);
        // 4 条竖线
        line(buf, mat, x1, y1, z1, x1, y2, z1);
        line(buf, mat, x2, y1, z1, x2, y2, z1);
        line(buf, mat, x2, y1, z2, x2, y2, z2);
        line(buf, mat, x1, y1, z2, x1, y2, z2);
    }

    /** 渲染入口面黄色填充 */
    private static void renderEntryFaceFill(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        switch (lastEntryFace)
        {
            case 0: // +X (东)
                quad(buf, mat, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
                break;
            case 1: // -X (西)
                quad(buf, mat, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2);
                break;
            case 2: // +Z (南)
                quad(buf, mat, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
                break;
            case 3: // -Z (北)
                quad(buf, mat, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1);
                break;
        }
    }

    /** 渲染入口面绿色加粗边框 */
    private static void renderEntryEdge(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        switch (lastEntryFace)
        {
            case 0: // +X (东) → 4 条边
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

    // ══ 工具方法 ═════════════════════════════════════

    private static void line(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        buf.vertex(mat, x1, y1, z1).color(LINE_R, LINE_G, LINE_B, LINE_A).endVertex();
        buf.vertex(mat, x2, y2, z2).color(LINE_R, LINE_G, LINE_B, LINE_A).endVertex();
    }

    /** 入口面绿色边框线（使用 ENTRY_EDGE 颜色） */
    private static void entryEdgeLine(BufferBuilder buf, Matrix4f mat,
        float x1, float y1, float z1, float x2, float y2, float z2)
    {
        buf.vertex(mat, x1, y1, z1).color(ENTRY_EDGE_R, ENTRY_EDGE_G, ENTRY_EDGE_B, ENTRY_EDGE_A).endVertex();
        buf.vertex(mat, x2, y2, z2).color(ENTRY_EDGE_R, ENTRY_EDGE_G, ENTRY_EDGE_B, ENTRY_EDGE_A).endVertex();
    }

    /** 四边形 = 2 个三角形 */
    private static void quad(BufferBuilder buf, Matrix4f mat,
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

    /** 入口面上加绿色边框 */

    /** 获取选框终点 — 优先用准星指向的方块，MISS 时用射线-平面交点兜底。 */
    private static BlockPos getMouseTargetBlock(Minecraft mc, BlockPos startPos)
    {
        // 方式 1（优先）: 准星指向的实体方块 — 确保选框各轴独立伸缩
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK)
            return bhr.getBlockPos();
        // 方式 2（MISS 兜底）: 射线-平面交点 — 准星指向空气时仍可选框
        LocalPlayer player = mc.player;
        if (player != null && startPos != null)
        {
            BlockPos freeEnd = RayPlaneUtil.getFreeEndPos(player, startPos);
            if (freeEnd != null) return freeEnd;
        }
        return null;
    }

    /** 根据玩家面向方向判定入口面（MC yaw: 0=南, 90=西, ±180=北, -90=东）
     *  入口面 = 建筑朝向玩家的面（玩家面前的面） */
    private static int getPlayerFacingFace(LocalPlayer player)
    {
        float yaw = player.getYRot() % 360;
        if (yaw < -180) yaw += 360;
        if (yaw >= 180) yaw -= 360;
        if (yaw >= -45 && yaw < 45)   return 3; // 面朝南 → 入口在北面(-Z)
        if (yaw >= 45 && yaw < 135)   return 0; // 面朝西 → 入口在东面(+X)
        if (yaw >= -135 && yaw < -45) return 1; // 面朝东 → 入口在西面(-X)
        return 2; // 面朝北 → 入口在南面(+Z)
    }
}
