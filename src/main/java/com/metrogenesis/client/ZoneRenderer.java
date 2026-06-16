package com.metrogenesis.client;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.blueprint.v1.BlockInfo;
import com.metrogenesis.road.RoadBlueprintBuilder;
import com.metrogenesis.road.RoadBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界覆盖层渲染器（单一事件处理器，共享 BufferBuilder）
 *
 * 功能：
 * - 已确认区域染色（地面 + 侧面）
 * - 预览区域（拖拽绘制中）
 * - 悬停 chunk 高亮
 * - 道路预览（即时方块模型渲染，实际 MC BakedModel）
 *
 * ── 架构说明 ──
 * 使用 Tesselator.getInstance().getBuilder() 共享 BufferBuilder，
 * 一次 begin() -> 渲染全部 -> 一次 end()，避免两路事件处理器冲突
 * 导致的 Already building! / OOM 崩溃。
 *
 * 道路预览采用即时渲染：每帧根据道路路径构建 Blueprint（复用 RoadBlueprintBuilder），
 * 然后遍历 Blueprint 中的非空气方块，用 BlockRenderDispatcher + ModelBlockRenderer
 * 直接渲染实际 BakedModel。GL 状态使用 CONSTANT_ALPHA blend 实现整体半透明。
 */
@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZoneRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneRenderer.class);

    private static volatile List<int[]> activeZones = List.of();
    private static volatile int hoverBlockX = Integer.MIN_VALUE;
    private static volatile int hoverBlockZ = Integer.MIN_VALUE;
    private static volatile int[] previewZone = null;
    private static volatile int snapHighlightX = Integer.MIN_VALUE;
    private static volatile int snapHighlightZ = Integer.MIN_VALUE;
    /** 所有道路节点坐标（空间地图预览） */
    private static volatile int[] roadNodeCoords = new int[0];
    private static final int ROAD_NODE_COLOR = 0xFFD4A847;
    private static final float ROAD_NODE_ALPHA = 0.20f;
    private static final float SNAP_ALPHA = 0.35f;

    /** 道路模板光标十字线（光标位置 + 模板尺寸） */
    private static volatile int roadCursorX = Integer.MIN_VALUE;
    private static volatile int roadCursorZ = Integer.MIN_VALUE;
    private static volatile int roadCursorWidth = 3; // 默认 3 格宽

    private static final int[] ZONE_COLORS = {
        0xFF6B9E6B, 0xFFB87333, 0xFF6B8EAA, 0xFFB8A044, 0xFF8B6BAA,
    };

    private static final float HOVER_R = 1.0f, HOVER_G = 1.0f, HOVER_B = 1.0f;

    public static void setActiveZones(List<int[]> zones) { activeZones = zones; }
    public static void setHoverBlock(int x, int z) { hoverBlockX = x; hoverBlockZ = z; }
    public static void setPreviewZone(int[] preview) { previewZone = preview; }
    public static void setSnapHighlight(int x, int z) { snapHighlightX = x; snapHighlightZ = z; }
    /** 设置道路模板光标位置和宽度（-1 清除） */
    public static void setRoadCursor(int blockX, int blockZ, int width) {
        roadCursorX = blockX;
        roadCursorZ = blockZ;
        roadCursorWidth = Math.max(1, width);
    }

    /**
     * 设置所有道路节点坐标（空间地图预览）。
     * 参数为交错数组：{x1, z1, x2, z2, ...}，长度必须为偶数。
     */
    public static void setRoadNodes(int[] coords) {
        roadNodeCoords = (coords != null) ? coords : new int[0];
    }

    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        List<int[]> zones = activeZones;
        int[] preview = previewZone;
        int hx = hoverBlockX, hz = hoverBlockZ;
        int snapX = snapHighlightX, snapZ = snapHighlightZ;
        int[] roadPreview = RoadRenderer.getPreview();
        int[] roadNodes = roadNodeCoords;

        boolean hasZones = !zones.isEmpty();
        boolean hasPreview = preview != null;
        boolean hasHover = hx != Integer.MIN_VALUE;
        boolean hasSnap = snapX != Integer.MIN_VALUE;
        boolean hasRoad = roadPreview != null;
        boolean hasNodes = roadNodes.length >= 2;
        boolean hasCursor = roadCursorX != Integer.MIN_VALUE;
        if (!hasZones && !hasPreview && !hasHover && !hasSnap && !hasRoad && !hasNodes && !hasCursor) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // == 道路预览（即时方块模型渲染）==
        if (hasRoad) {
            renderRoadPreview(event, mc.level);
        }

        // == 区域/悬停使用平面四边形渲染（共享 BufferBuilder）==
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        try {
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            // 道路模板光标十字线
            if (hasCursor) {
                drawRoadCursor(poseStack, builder, mc.level);
            }

            // == 已确认区域 ==
            for (int[] zone : zones) {
                int minX = zone[0], minZ = zone[1], maxX = zone[2], maxZ = zone[3], type = zone[4];
                if (type < 0 || type >= ZONE_COLORS.length) continue;
                int abgr = ZONE_COLORS[type];
                float r = ((abgr >> 16) & 0xFF) / 255.0f;
                float g = ((abgr >> 8)  & 0xFF) / 255.0f;
                float b = ( abgr        & 0xFF) / 255.0f;
                colorSurface(poseStack, builder, mc.level, minX, minZ, maxX, maxZ, r, g, b, 0.20f);

                // 方向箭头（正面方向深色三角标记）
                int dir = zone.length >= 6 ? zone[5] : 0;
                drawDirectionArrow(poseStack, builder, mc.level, minX, minZ, maxX, maxZ, dir);
            }

            // == 预览区域 ==
            if (hasPreview) {
                int minX = preview[0], minZ = preview[1], maxX = preview[2], maxZ = preview[3], type = preview[4];
                if (type >= 0 && type < ZONE_COLORS.length) {
                    int abgr = ZONE_COLORS[type];
                    float r = ((abgr >> 16) & 0xFF) / 255.0f;
                    float g = ((abgr >> 8)  & 0xFF) / 255.0f;
                    float b = ( abgr        & 0xFF) / 255.0f;
                    colorSurface(poseStack, builder, mc.level, minX, minZ, maxX, maxZ, r, g, b, 0.35f);
                }
            }

            // == 悬停高亮（方块级）==
            if (hasHover) {
                colorSurface(poseStack, builder, mc.level, hx, hz, hx + 1, hz + 1,
                        HOVER_R, HOVER_G, HOVER_B, 0.12f);
            }

            // == 道路节点预览（空间地图 — 全部节点）==
            if (hasNodes) {
                float nr = ((ROAD_NODE_COLOR >> 16) & 0xFF) / 255.0f;
                float ng = ((ROAD_NODE_COLOR >> 8)  & 0xFF) / 255.0f;
                float nb = ( ROAD_NODE_COLOR        & 0xFF) / 255.0f;
                for (int i = 0; i < roadNodes.length; i += 2) {
                    int nx = roadNodes[i];
                    int nz = roadNodes[i + 1];
                    colorSurface(poseStack, builder, mc.level, nx, nz, nx + 1, nz + 1,
                        nr, ng, nb, ROAD_NODE_ALPHA);
                }
            }

            // == 吸附节点高亮（金色半透明方块，当前吸附目标更亮）==
            if (hasSnap) {
                // 金色：R=1.0, G=0.84, B=0.0
                colorSurface(poseStack, builder, mc.level, snapX, snapZ, snapX + 1, snapZ + 1,
                        1.0f, 0.84f, 0.0f, 0.35f);
            }

            BufferUploader.drawWithShader(builder.end());

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

        } finally {
            poseStack.popPose();
        }
    }

    // == 道路预览（即时方块模型渲染）==

    private static void renderRoadPreview(RenderLevelStageEvent event, Level level) {
        int[] roadPreview = RoadRenderer.getPreview();
        if (roadPreview == null) return;

        boolean bezier = RoadRenderer.isUseBezier();
        List<Vec3> path = bezier ? RoadRenderer.getCurvePath() : null;
        List<Float> yaws = bezier ? RoadRenderer.getCurveYaws() : null;
        int rw = RoadBuilder.getRoadWidth();

        Blueprint bp;
        int[] origin;
        if (bezier && path != null && !path.isEmpty() && yaws != null && !yaws.isEmpty()) {
            bp = RoadBlueprintBuilder.buildFromPath(path, yaws, rw);
            origin = RoadBlueprintBuilder.getBlueprintOrigin(path, yaws, rw);
        } else {
            int fx = roadPreview[0], fz = roadPreview[1];
            int tx = roadPreview[2], tz = roadPreview[3];
            int dx = tx - fx, dz = tz - fz;
            int steps = Math.max(Math.abs(dx), Math.abs(dz));
            if (steps == 0) return;
            float yaw = (float) Math.atan2(dz, dx);
            List<Vec3> straightPath = new ArrayList<>(steps + 1);
            List<Float> straightYaws = new ArrayList<>(steps + 1);
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                int bx = fx + Math.round(dx * t);
                int bz = fz + Math.round(dz * t);
                straightPath.add(new Vec3(bx + 0.5, 0, bz + 0.5));
                straightYaws.add(yaw);
            }
            bp = RoadBlueprintBuilder.buildFromPath(straightPath, straightYaws, rw);
            origin = RoadBlueprintBuilder.getBlueprintOrigin(straightPath, straightYaws, rw);
        }

        if (bp == null || origin == null) return;

        float alpha = RoadRenderer.isPending() ? 0.45f : 0.30f;

        // GL 状态：CONSTANT_ALPHA blend 实现整体半透明
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
        GL14.glBlendColor(0f, 0f, 0f, alpha);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        for (BlockInfo info : bp.getBlockInfoAsList()) {
            if (info.state() == null || info.state().isAir()) continue;

            BlockPos localPos = info.pos();
            int worldX = origin[0] + localPos.getX();
            int worldZ = origin[1] + localPos.getZ();
            int worldY = RoadBuilder.getTrueTerrainHeight(level, worldX, worldZ);

            BakedModel model = dispatcher.getBlockModel(info.state());
            int packedLight = LevelRenderer.getLightColor(level, new BlockPos(worldX, worldY, worldZ));

            poseStack.pushPose();
            poseStack.translate(worldX, worldY, worldZ);
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                builder,
                info.state(),
                model,
                1.0f, 1.0f, 1.0f,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.solid()
            );
            poseStack.popPose();
        }

        BufferUploader.drawWithShader(builder.end());

        poseStack.popPose();

        // 恢复 GL 状态
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    /**
     * 清理道路渲染缓存（Phase 2 遗留，现为空实现兼容 MayorBookScreen）。
     */
    public static void clearRoadPreview() {
        // 即时渲染无需缓存清理
    }

    // 降采样性能优化常量
    private static final int MAX_DIRECT_RENDER = 64 * 64;

    private static void colorSurface(PoseStack poseStack, BufferBuilder builder,
            Level level, int minX, int minZ, int maxX, int maxZ,
            float r, float g, float b, float a) {

        int area = (maxX - minX) * (maxZ - minZ);
        int step = 1;

        if (area > MAX_DIRECT_RENDER) {
            step = Math.max(2, (int) Math.sqrt((double) area / MAX_DIRECT_RENDER));
        }

        Matrix4f mat = poseStack.last().pose();

        for (int x = minX; x < maxX; x += step) {
            for (int z = minZ; z < maxZ; z += step) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                float y = h + 0.01f;

                float x2 = Math.min(x + step, maxX);
                float z2 = Math.min(z + step, maxZ);

                tri(mat, builder, x, y, z,      x2, y, z,     x2, y, z2,    r, g, b, a);
                tri(mat, builder, x, y, z,      x2, y, z2,    x, y, z2,     r, g, b, a);

                int hN = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1);
                if (hN < h) {
                    float yN = hN + 0.01f;
                    tri(mat, builder, x, yN, z, x2, yN, z, x2, y, z, r, g, b, a);
                    tri(mat, builder, x, yN, z, x2, y, z,  x, y, z,  r, g, b, a);
                }

                int hS = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, Math.min(z + step, maxZ - 1));
                if (hS < h) {
                    float yS = hS + 0.01f;
                    tri(mat, builder, x2, yS, z2, x, yS, z2, x, y, z2, r, g, b, a);
                    tri(mat, builder, x2, yS, z2, x, y, z2,  x2, y, z2, r, g, b, a);
                }

                int hE = level.getHeight(Heightmap.Types.WORLD_SURFACE, Math.min(x + step, maxX - 1), z);
                if (hE < h) {
                    float yE = hE + 0.01f;
                    tri(mat, builder, x2, yE, z, x2, yE, z2, x2, y, z2, r, g, b, a);
                    tri(mat, builder, x2, yE, z, x2, y, z2,  x2, y, z,  r, g, b, a);
                }

                int hW = level.getHeight(Heightmap.Types.WORLD_SURFACE, x - 1, z);
                if (hW < h) {
                    float yW = hW + 0.01f;
                    tri(mat, builder, x, yW, z2, x, yW, z, x, y, z, r, g, b, a);
                    tri(mat, builder, x, yW, z2, x, y, z,  x, y, z2, r, g, b, a);
                }
            }
        }
    }

    private static void tri(Matrix4f mat, BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a) {
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * 在区域正面边缘绘制方向三角箭头（深色，半透明）。
     * <p>
     * 方向值：0=东, 1=南, 2=西, 3=北
     * 箭头是一个等腰三角形，底边在区域正面边缘，顶点指向区域外部（道路方向）。
     * </p>
     */
    private static void drawDirectionArrow(PoseStack poseStack, BufferBuilder builder,
            Level level, int minX, int minZ, int maxX, int maxZ, int direction) {
        Matrix4f mat = poseStack.last().pose();
        float h = level.getHeight(Heightmap.Types.WORLD_SURFACE, (minX + maxX) / 2, (minZ + maxZ) / 2) + 0.02f;
        float r = 0.15f, g = 0.15f, b = 0.15f, a = 0.75f;

        float size = Math.min(maxX - minX, maxZ - minZ) * 0.3f;
        if (size < 0.5f) size = 0.5f;

        float cx = (minX + maxX) / 2.0f;
        float cz = (minZ + maxZ) / 2.0f;

        // 三角箭头完全在区域内，底边平行于正面边缘，顶点朝向道路
        switch (direction) {
            case 0: // 东：正面在 maxX 边
                tri(mat, builder, maxX - size, h, cz - size, maxX - size, h, cz + size,
                        maxX, h, cz, r, g, b, a);
                break;
            case 1: // 南：正面在 maxZ 边
                tri(mat, builder, cx - size, h, maxZ - size, cx + size, h, maxZ - size,
                        cx, h, maxZ, r, g, b, a);
                break;
            case 2: // 西：正面在 minX 边
                tri(mat, builder, minX + size, h, cz - size, minX + size, h, cz + size,
                        minX, h, cz, r, g, b, a);
                break;
            case 3: // 北：正面在 minZ 边
                tri(mat, builder, cx - size, h, minZ + size, cx + size, h, minZ + size,
                        cx, h, minZ, r, g, b, a);
                break;
        }
    }

    /**
     * 道路模板井字光标 — 在光标位置绘制井字形标尺。
     * <p>
     * 井字 = 4 条延伸线（模板左右边缘各一条竖线，上下边缘各一条横线），
     * 中间空出模板宽度的正方形区域。线延伸超出模板范围用于对齐辅助。
     * </p>
     */
    private static void drawRoadCursor(PoseStack poseStack, BufferBuilder builder, Level level) {
        Matrix4f mat = poseStack.last().pose();
        int half = roadCursorWidth / 2;
        int minX = roadCursorX - half;
        int minZ = roadCursorZ - half;
        int maxX = roadCursorX + half + (roadCursorWidth % 2 == 0 ? 0 : 1);
        int maxZ = roadCursorZ + half + (roadCursorWidth % 2 == 0 ? 0 : 1);

        float y = level.getHeight(Heightmap.Types.WORLD_SURFACE, roadCursorX, roadCursorZ) + 0.03f;
        float lw = 0.05f;
        float ext = 8f; // 延伸长度（超出模板范围）

        // ══ 井字 4 条线 ═══════════════════════════════
        // 左竖线（minX 位置，上下延伸）
        tri(mat, builder, minX - lw, y, minZ - ext, minX + lw, y, minZ - ext,
                minX + lw, y, maxZ + ext, 1f, 1f, 1f, 0.55f);
        tri(mat, builder, minX - lw, y, minZ - ext, minX + lw, y, maxZ + ext,
                minX - lw, y, maxZ + ext, 1f, 1f, 1f, 0.55f);
        // 右竖线（maxX 位置，上下延伸）
        tri(mat, builder, maxX - lw, y, minZ - ext, maxX + lw, y, minZ - ext,
                maxX + lw, y, maxZ + ext, 1f, 1f, 1f, 0.55f);
        tri(mat, builder, maxX - lw, y, minZ - ext, maxX + lw, y, maxZ + ext,
                maxX - lw, y, maxZ + ext, 1f, 1f, 1f, 0.55f);
        // 上横线（minZ 位置，左右延伸）
        tri(mat, builder, minX - ext, y, minZ - lw, maxX + ext, y, minZ - lw,
                maxX + ext, y, minZ + lw, 1f, 1f, 1f, 0.55f);
        tri(mat, builder, minX - ext, y, minZ - lw, maxX + ext, y, minZ + lw,
                minX - ext, y, minZ + lw, 1f, 1f, 1f, 0.55f);
        // 下横线（maxZ 位置，左右延伸）
        tri(mat, builder, minX - ext, y, maxZ - lw, maxX + ext, y, maxZ - lw,
                maxX + ext, y, maxZ + lw, 1f, 1f, 1f, 0.55f);
        tri(mat, builder, minX - ext, y, maxZ - lw, maxX + ext, y, maxZ + lw,
                minX - ext, y, maxZ + lw, 1f, 1f, 1f, 0.55f);

        // ══ 中心半透明白色填充 ═══════════════════════
        tri(mat, builder, minX, y, minZ, maxX, y, minZ, maxX, y, maxZ, 0.9f, 0.9f, 0.9f, 0.25f);
        tri(mat, builder, minX, y, minZ, maxX, y, maxZ, minX, y, maxZ, 0.9f, 0.9f, 0.9f, 0.25f);
    }
}
