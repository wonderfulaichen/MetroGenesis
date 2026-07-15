package com.metrogenesis.gui;

import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.storage.StructurePacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 右侧 3D 体素预览组件 — 渲染建筑模型的等轴测 3D 预览。
 * <p>
 * 支持：旋转/平移/缩放、Ponder 范式注释卡片、等级切换按钮。
 */
public class CatalogPreviewPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesisPreview");

    // ════════════════════════════════════════════════════════
    //  依赖注入
    // ════════════════════════════════════════════════════════

    private final MayorBookScreen owner;
    private final Font font;
    private final Runnable onLevelChanged;

    // ════════════════════════════════════════════════════════
    //  3D 预览状态
    // ════════════════════════════════════════════════════════

    private float previewPanX = 0f;
    private float previewPanY = 0f;
    private float previewZoom = 1f;
    private float previewRotY = 45f;
    private float previewRotX = 35f;
    private long lastPreviewInteraction = 0L;

    /** 当前预览等级（多级建筑） */
    private int previewLevel = 1;

    // 拖拽状态（由外部管理，但在这里定义常量）
    public boolean dragging = false;
    public boolean rotating = false;
    public double dragStartX, dragStartY;

    // ════════════════════════════════════════════════════════

    public CatalogPreviewPanel(MayorBookScreen owner, Font font,
                                Runnable onLevelChanged) {
        this.owner = owner;
        this.font = font;
        this.onLevelChanged = onLevelChanged;
    }

    // ════════════════════════════════════════════════════════
    //  状态访问
    // ════════════════════════════════════════════════════════

    public int getPreviewLevel() { return previewLevel; }
    public void setPreviewLevel(int level) {
        if (previewLevel != level) {
            previewLevel = level;
            onLevelChanged.run();
        }
    }
    public void resetPreviewLevel() { previewLevel = 1; }

    public float getPreviewPanX() { return previewPanX; }
    public void setPreviewPanX(float x) { previewPanX = x; }
    public float getPreviewPanY() { return previewPanY; }
    public void setPreviewPanY(float y) { previewPanY = y; }
    public float getPreviewZoom() { return previewZoom; }
    public void setPreviewZoom(float z) { previewZoom = z; }
    public float getPreviewRotY() { return previewRotY; }
    public void setPreviewRotY(float y) { previewRotY = y; }
    public float getPreviewRotX() { return previewRotX; }
    public void setPreviewRotX(float x) { previewRotX = x; }
    public long getLastInteraction() { return lastPreviewInteraction; }
    public void markInteraction() { lastPreviewInteraction = System.currentTimeMillis(); }

    // ════════════════════════════════════════════════════════
    //  蓝图加载
    // ════════════════════════════════════════════════════════

    @Nullable
    public Blueprint loadBlueprint(final BuildingCatalogEntry entry) {
        return loadBlueprint(entry, previewLevel);
    }

    @Nullable
    public Blueprint loadBlueprint(final BuildingCatalogEntry entry, final int level) {
        try {
            String path = entry.resourcePath();
            if (entry.isMultiLevel()) {
                path = path.replaceAll("\\d+$", String.valueOf(level));
            }
            return StructurePacks.getBlueprint(entry.packName(), path + ".blueprint", true);
        } catch (Exception e) {
            LOGGER.debug("[Preview] Failed to load '{}' lv{}: {}", entry.name(), level, e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  渲染入口
    // ════════════════════════════════════════════════════════

    public void render(GuiGraphics g, int x, int y, int w, int h,
                       @Nullable BuildingCatalogEntry entry, int alpha) {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        if (entry == null) {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.054"),
                x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        final Blueprint bp = loadBlueprint(entry);
        if (bp == null) {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.055"),
                x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 裁剪体素渲染，防止溢出底部操作栏
        enableScissor(x, y, w, h);
        try {
            renderVoxel3D(g, x + 4, y + 4, w - 8, h - 8, bp, entry, alpha);
        } finally {
            disableScissor();
        }
    }

    // ════════════════════════════════════════════════════════
    //  3D 体素渲染
    // ════════════════════════════════════════════════════════

    private void renderVoxel3D(GuiGraphics g, int x, int y, int w, int h,
                                Blueprint bp, BuildingCatalogEntry entry, int alpha) {
        short sx = bp.getSizeX(), sy = bp.getSizeY(), sz = bp.getSizeZ();
        short[][][] struct = bp.getStructure();
        BlockState[] pal = bp.getPalette();
        int maxDim = Math.max(sx, Math.max(sy, sz));
        if (maxDim == 0) return;

        final float baseScale = Math.min((float) w / (maxDim + 2), (float) h / (maxDim + 2)) * 0.7f;
        final float finalScale = baseScale * previewZoom;

        // 自动旋转
        final long now = System.currentTimeMillis();
        if (now - lastPreviewInteraction > 1500L) {
            previewRotY += 0.4f;
            if (previewRotY >= 360f) previewRotY -= 360f;
        }

        final float viewDist = maxDim * 3.5f;
        final float cx = x + w / 2f + previewPanX;
        final float cy = y + h / 2f + previewPanY;

        // 透视投影
        enableScissor(x, y, w, h);
        clearDepth();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

        final Matrix4f persp = new Matrix4f().perspective(
            (float) Math.toRadians(35), (float) w / h, 0.1f, 10000f);

        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(finalScale, -finalScale, 1f);
        pose.translate(-sx / 2f, -sy / 2f, -viewDist);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(previewRotX));
        pose.mulPose(com.mojang.math.Axis.YN.rotationDegrees(previewRotY));

        var mc = Minecraft.getInstance();
        var buffer = mc.renderBuffers().bufferSource();
        var blockRenderer = mc.getBlockRenderer();

        // 包围盒线框
        var wireBox = new AABB(0, 0, 0, sx, sy, sz);
        var lineBuf = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lineBuf, wireBox,
            ((MGStyles.C_BRASS >> 16) & 0xFF) / 255f,
            ((MGStyles.C_BRASS >> 8) & 0xFF) / 255f,
            (MGStyles.C_BRASS & 0xFF) / 255f, 0.6f);

        // 逐方块渲染
        for (short by = 0; by < sy; by++) {
            for (short bz = 0; bz < sz; bz++) {
                for (short bx = 0; bx < sx; bx++) {
                    short val = struct[by][bz][bx];
                    if (val == 0 || val >= pal.length) continue;
                    BlockState state = pal[val];
                    if (state.isAir()) continue;
                    pose.pushPose();
                    pose.translate(bx, by, bz);
                    blockRenderer.renderSingleBlock(state, pose, buffer,
                        0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
        buffer.endBatch();

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        disableScissor();

        // 注解卡片
        renderAnnotations(g, x, y, w, h, entry, (int) cx, (int) cy, alpha);
    }

    // ════════════════════════════════════════════════════════
    //  Ponder 范式注解卡片
    // ════════════════════════════════════════════════════════

    private void renderAnnotations(GuiGraphics g, int x, int y, int w, int h,
                                    BuildingCatalogEntry entry, int mcX, int mcY, int alpha) {
        final int cardW = 94, cardH = 34, pad = 6;
        final int lineAlpha = (alpha * 50) / 100;

        final String cat = entry.category();
        final String lvl = entry.levelsDisplay() + (entry.isMultiLevel()
            ? Language.getInstance().getOrDefault("gui.metrogenesis.catalog.056") : " 级");
        final String fac = entry.isMixedUse()
            ? Language.getInstance().getOrDefault("gui.metrogenesis.catalog.057")
            : (entry.buildingType().isEmpty() ? entry.category() : entry.buildingType());
        final String cost = formatCValue(entry.materialCost()) + " C";

        // 左上：分类
        int ax = x + pad, ay = y + pad;
        drawCard(g, ax, ay, cardW, cardH,
            Language.getInstance().getOrDefault("gui.metrogenesis.catalog.059"), cat,
            MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
        drawLeader(g, ax + cardW, ay + cardH / 2, mcX, mcY, lineAlpha);

        // 右上：设施
        int bx = x + w - cardW - pad, by = y + pad;
        drawCard(g, bx, by, cardW, cardH,
            Language.getInstance().getOrDefault("gui.metrogenesis.catalog.060"), fac,
            MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
        drawLeader(g, bx, by + cardH / 2, mcX, mcY, lineAlpha);

        // 左下：等级（多级显示切换按钮）
        int cx = x + pad, cy = y + h - cardH - pad;
        if (entry.isMultiLevel()) {
            renderLevelButtons(g, cx, cy, cardW, cardH + 8, entry, alpha);
            drawLeader(g, cx + cardW, cy + (cardH + 8) / 2, mcX, mcY, lineAlpha);
        } else {
            drawCard(g, cx, cy, cardW, cardH,
                Language.getInstance().getOrDefault("gui.metrogenesis.catalog.061"), lvl,
                MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
            drawLeader(g, cx + cardW, cy + cardH / 2, mcX, mcY, lineAlpha);
        }

        // 右下：造价
        int dx = x + w - cardW - pad, dy = y + h - cardH - pad;
        drawCard(g, dx, dy, cardW, cardH,
            Language.getInstance().getOrDefault("gui.metrogenesis.catalog.062"), cost,
            MGStyles.C_ACCENT, MGStyles.C_TEXT_BRASS, alpha);
        drawLeader(g, dx, dy + cardH / 2, mcX, mcY, lineAlpha);

        // 底部中央：功能说明
        String descKey = entry.description();
        String desc = (descKey != null && !descKey.isEmpty())
            ? Language.getInstance().getOrDefault(descKey)
            : Language.getInstance().getOrDefault("gui.metrogenesis.catalog.desc.cat.other");
        int gapW = (w - 2 * pad) - 2 * cardW;
        if (gapW >= 90) {
            int ew = gapW - 6, eh = cardH + 8;
            int ex = x + pad + cardW + 6, ey = y + h - eh - pad;
            drawDescCard(g, ex, ey, ew, eh,
                Language.getInstance().getOrDefault(com.metrogenesis.catalog.BuildingDescriptionProvider.TITLE_KEY),
                desc, alpha);
            drawLeader(g, ex + ew / 2, ey, mcX, mcY, lineAlpha);
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h,
                           String title, String value, int tc, int vc, int alpha) {
        MGStyles.drawPanel(g, x, y, w, h, MGStyles.C_BG_CARD, (alpha * 80) / 100);
        g.drawString(font, title, x + 6, y + 5, owner.withAlpha(tc, alpha), false);
        g.drawString(font, value, x + 6, y + 17, owner.withAlpha(vc, alpha), false);
    }

    private void drawDescCard(GuiGraphics g, int x, int y, int w, int h,
                               String title, String text, int alpha) {
        MGStyles.drawPanel(g, x, y, w, h, MGStyles.C_BG_CARD, (alpha * 80) / 100);
        g.drawString(font, title, x + 6, y + 4, owner.withAlpha(MGStyles.C_TEXT_2ND, alpha), false);
        int maxW = w - 12, ty = y + 16, lines = 0;
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length() && lines < 2; i++) {
            char c = text.charAt(i);
            if (font.width(line.toString() + c) > maxW && !line.isEmpty()) {
                g.drawString(font, line.toString(), x + 6, ty,
                    owner.withAlpha(MGStyles.C_TEXT, alpha), false);
                ty += 12; lines++;
                line = new StringBuilder();
            }
            line.append(c);
        }
        if (lines < 2 && !line.isEmpty()) {
            g.drawString(font, line.toString(), x + 6, ty,
                owner.withAlpha(MGStyles.C_TEXT, alpha), false);
        }
    }

    private void renderLevelButtons(GuiGraphics g, int x, int y, int w, int h,
                                     BuildingCatalogEntry entry, int alpha) {
        MGStyles.drawPanel(g, x, y, w, h, MGStyles.C_BG_CARD, (alpha * 80) / 100);
        g.drawString(font, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.061"),
            x + 6, y + 5, owner.withAlpha(MGStyles.C_TEXT_2ND, alpha), false);

        int[] lvls = entry.levels().stream().mapToInt(Integer::intValue).sorted().toArray();
        int bs = 14, bg = 3, tw = lvls.length * bs + (lvls.length - 1) * bg;
        int bx = x + (w - tw) / 2, by = y + h - bs - 4;

        for (int l : lvls) {
            boolean cur = (l == previewLevel);
            MGStyles.drawPanel(g, bx, by, bs, bs,
                cur ? MGStyles.C_BRASS : 0x66484858,
                (alpha * (cur ? 90 : 60)) / 100);
            g.drawString(font, String.valueOf(l),
                bx + (bs - font.width(String.valueOf(l))) / 2, by + 3,
                owner.withAlpha(cur ? 0xFF1A1A1A : MGStyles.C_TEXT, alpha), false);
            bx += bs + bg;
        }
    }

    private void drawLeader(GuiGraphics g, int x1, int y1, int x2, int y2, int alpha) {
        int dx = x2 - x1, dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        // 短线 + 锚点
        double len = Math.min(dist, 28);
        double nx = dx / dist, ny = dy / dist;
        int ex = (int)(x1 + nx * len), ey = (int)(y1 + ny * len);
        // 使用 fill 模拟虚线
        g.fill(x1, y1, ex, ey, owner.withAlpha(MGStyles.C_BRASS, alpha / 2));
    }

    // ════════════════════════════════════════════════════════
    //  等级按钮点击检测
    // ════════════════════════════════════════════════════════

    /**
     * 检测等级按钮点击。返回 true 表示消费。
     * @param previewRect [x, y, w, h] — 预览面板的玻璃面板区域
     */
    public boolean handleLevelClick(double mx, double my, int button,
                                     @Nullable BuildingCatalogEntry entry,
                                     int[] previewRect) {
        if (button != 0 || entry == null || !entry.isMultiLevel() || previewRect == null)
            return false;

        int gx = previewRect[0], gy = previewRect[1], gw = previewRect[2], gh = previewRect[3];
        int pad = 6, cardW = 94, cardH = 34;
        int cx = gx + 10, cy = gy + gh - 44;
        int switchH = cardH + 8;
        int[] lvls = entry.levels().stream().mapToInt(Integer::intValue).sorted().toArray();
        int bs = 14, bg = 3, tw = lvls.length * bs + (lvls.length - 1) * bg;
        int sx = cx + (cardW - tw) / 2, sy = cy + switchH - bs - 4;

        for (int i = 0; i < lvls.length; i++) {
            int bx = sx + i * (bs + bg);
            if (mx >= bx && mx <= bx + bs && my >= sy && my <= sy + bs) {
                setPreviewLevel(lvls[i]);
                return true;
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════
    //  工具
    // ════════════════════════════════════════════════════════

    private static String formatCValue(long cost) {
        if (cost >= 1_000_000) return String.format("%.1fM", cost / 1_000_000.0);
        if (cost >= 1_000) return String.format("%.1fK", cost / 1_000.0);
        return String.valueOf(cost);
    }

    private static void clearDepth() {
        com.mojang.blaze3d.systems.RenderSystem.clear(
            org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, false);
    }

    private static void enableScissor(int x, int y, int w, int h) {
        double s = Minecraft.getInstance().getWindow().getGuiScale();
        int sx = (int)(x * s), sy = (int)((Minecraft.getInstance().getWindow().getScreenHeight() - (y + h)) * s);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(
            sx, sy, Math.max(0, (int)(w * s)), Math.max(0, (int)(h * s)));
    }

    private static void disableScissor() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
}
