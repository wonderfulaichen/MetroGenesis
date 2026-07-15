package com.metrogenesis.gui;

import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.catalog.CategoryMapper;
import com.metrogenesis.gui.catalog.PreviewCache;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 中间建筑网格/列表组件 — 渲染图鉴的蓝图选择区。
 * <p>
 * 负责三种视图模式（大图标、小图标、列表）的渲染 + 选中 + 滚动。
 * 拥有 {@code selectedIndex}、{@code scrollOffset}、{@code viewMode} 状态。
 */
public class CatalogBlueprintGrid {

    // ════════════════════════════════════════════════════════
    //  视图模式
    // ════════════════════════════════════════════════════════

    public enum ViewMode {
        LARGE_ICONS(80, 80),
        SMALL_ICONS(40, 40),
        LIST(0, 20);

        public final int cellW;
        public final int cellH;
        ViewMode(int cellW, int cellH) { this.cellW = cellW; this.cellH = cellH; }
    }

    // ════════════════════════════════════════════════════════
    //  依赖注入
    // ════════════════════════════════════════════════════════

    private final MayorBookScreen owner;
    private final Font font;
    private final CatalogDataManager dataManager;
    private final Runnable onSelectionChanged;

    // ════════════════════════════════════════════════════════
    //  状态
    // ════════════════════════════════════════════════════════

    private ViewMode viewMode = ViewMode.LARGE_ICONS;
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    /** 当前筛选的分类索引（由左侧分类列表更新） */
    private int filterCategoryIndex = -1;

    private static final int GRID_CELL_GAP = 4;
    private static final int ITEM_H = 20;
    private static final int ITEM_GAP = 2;

    // ════════════════════════════════════════════════════════

    public CatalogBlueprintGrid(MayorBookScreen owner, Font font,
                                 CatalogDataManager dataManager,
                                 Runnable onSelectionChanged) {
        this.owner = owner;
        this.font = font;
        this.dataManager = dataManager;
        this.onSelectionChanged = onSelectionChanged;
    }

    // ════════════════════════════════════════════════════════
    //  状态查询
    // ════════════════════════════════════════════════════════

    public ViewMode getViewMode() { return viewMode; }
    public void setViewMode(ViewMode vm) { this.viewMode = vm; scrollOffset = 0; }

    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int idx) { this.selectedIndex = idx; }

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int offset) { this.scrollOffset = Math.max(0, offset); }

    public int getFilterCategoryIndex() { return filterCategoryIndex; }
    public void setFilterCategoryIndex(int idx) { this.filterCategoryIndex = idx; }

    /** 当前选中的条目 */
    @Nullable
    public BuildingCatalogEntry getSelectedEntry() {
        if (selectedIndex < 0) return null;
        final List<BuildingCatalogEntry> entries = getFilteredEntries();
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            return entries.get(selectedIndex);
        }
        return null;
    }

    /** 滚轮滚动 */
    public void scroll(double delta) {
        final int step = switch (viewMode) {
            case LARGE_ICONS -> 80 + GRID_CELL_GAP;
            case SMALL_ICONS -> 40 + GRID_CELL_GAP;
            case LIST -> ITEM_H + ITEM_GAP;
        };
        scrollOffset = Math.max(0, scrollOffset - (int)(delta * step));
    }

    // ════════════════════════════════════════════════════════
    //  获取当前筛选的条目列表
    // ════════════════════════════════════════════════════════

    private List<BuildingCatalogEntry> getFilteredEntries() {
        final String cat = filterCategoryIndex >= 0
            ? CategoryMapper.getCategoryByIndex(filterCategoryIndex)
            : null;
        // pass null for pack/sub since CatalogDataManager handles full filtering
        return dataManager.getFilteredEntries(cat, null, null);
    }

    // ════════════════════════════════════════════════════════
    //  渲染
    // ════════════════════════════════════════════════════════

    public void render(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        // 标题
        final String title = switch (viewMode) {
            case LARGE_ICONS -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.050");
            case SMALL_ICONS -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.051");
            case LIST -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.052");
        };
        owner.drawGlassText(g, title, x + 6, y + 4, owner.TEXT_SOFT_GOLD, alpha);
        g.fill(x + 4, y + 16, x + w - 4, y + 17, owner.withAlpha(owner.EDGE_INNER, alpha));

        final List<BuildingCatalogEntry> entries = getFilteredEntries();
        final int startY = y + 20;

        if (entries.isEmpty()) {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.053"),
                x + 8, startY + 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 裁剪
        enableScissor(x, startY, w, h - 20);
        try {
            switch (viewMode) {
                case LARGE_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 80, 80, alpha);
                case SMALL_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 40, 40, alpha);
                case LIST -> renderList(g, x, startY, w, h - 20, entries, alpha);
            }
        } finally {
            disableScissor();
        }
    }

    private void renderGrid(GuiGraphics g, int x, int y, int w, int h,
                            List<BuildingCatalogEntry> entries, int cellW, int cellH, int alpha) {
        int availableW = w - 8;
        int cols = Math.max(1, availableW / (cellW + GRID_CELL_GAP));

        for (int i = 0; i < entries.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int cellX = x + 4 + col * (cellW + GRID_CELL_GAP);
            int cellY = y + row * (cellH + GRID_CELL_GAP) - scrollOffset;

            if (cellY + cellH < y || cellY > y + h) continue;

            BuildingCatalogEntry entry = entries.get(i);
            boolean sel = (selectedIndex == i);
            boolean hover = owner.inBounds(cellX, cellY, cellW, cellH);
            int bg = sel ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
            owner.fillGlassPanel(g, cellX, cellY, cellW, cellH, bg, alpha);

            if (sel) {
                g.fill(cellX, cellY, cellX + cellW, cellY + 1, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX, cellY + cellH - 1, cellX + cellW, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX, cellY, cellX + 1, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX + cellW - 1, cellY, cellX + cellW, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
            }

            int previewX = cellX + 4, previewY = cellY + 4;
            int previewW = cellW - 8, previewH = cellH - 20;
            renderMiniPreview(g, previewX, previewY, previewW, previewH, entry, alpha);

            String name = ContentNameLocalizer.localize(entry.name());
            if (name.length() > 10) name = name.substring(0, 8) + "..";
            owner.drawGlassText(g, name, cellX + (cellW - font.width(name)) / 2,
                cellY + cellH - 14, owner.TEXT_PRIMARY, alpha);

            if ("seamless".equals(entry.mcCategory())) {
                owner.drawGlassText(g, "\u25A0", cellX + 2, cellY + 2, 0xFFC89B3C, alpha);
            }
        }
    }

    private void renderList(GuiGraphics g, int x, int y, int w, int h,
                            List<BuildingCatalogEntry> entries, int alpha) {
        for (int i = 0; i < entries.size(); i++) {
            int itemY = y + i * (ITEM_H + ITEM_GAP) - scrollOffset;
            if (itemY + ITEM_H < y || itemY > y + h) continue;

            BuildingCatalogEntry entry = entries.get(i);
            boolean sel = (selectedIndex == i);
            boolean hover = owner.inBounds(x + 4, itemY, w - 8, ITEM_H);
            int bg = sel ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
            owner.fillGlassButton(g, x + 4, itemY, w - 8, ITEM_H, bg, alpha);

            String name = ContentNameLocalizer.localize(entry.name());
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            owner.drawGlassText(g, name, x + 10, itemY + (ITEM_H - font.lineHeight) / 2 + 1,
                owner.TEXT_PRIMARY, alpha);

            String sizeStr = entry.sizeDisplay();
            owner.drawGlassText(g, sizeStr, x + w - 10 - font.width(sizeStr),
                itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);
        }
    }

    private void renderMiniPreview(GuiGraphics g, int x, int y, int w, int h,
                                   BuildingCatalogEntry entry, int alpha) {
        DynamicTexture cachedTex = PreviewCache.loadCachedPreview(entry.packName(), entry.resourcePath());
        if (cachedTex != null) {
            RenderSystem.setShaderTexture(0, cachedTex.getId());
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            Matrix4f mat = g.pose().last().pose();
            var buf = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
            buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
            buf.vertex(mat, x, y + h, 0).uv(0, 1).endVertex();
            buf.vertex(mat, x + w, y + h, 0).uv(1, 1).endVertex();
            buf.vertex(mat, x + w, y, 0).uv(1, 0).endVertex();
            buf.vertex(mat, x, y, 0).uv(0, 0).endVertex();
            com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.end());
            return;
        }

        final Blueprint bp = loadBlueprintForPreview(entry);
        if (bp != null) {
            renderVoxelPreview(g, x, y, w, h, bp, alpha);
            final String packName = entry.packName();
            final String resourcePath = entry.resourcePath();
            com.metrogenesis.structurize.util.IOPool.submit(() -> {
                PreviewCache.saveExternalPreview(packName, resourcePath, bp);
                return null;
            });
            return;
        }

        // Fallback: 简化建筑轮廓
        int sx = entry.size().getX(), sz = entry.size().getZ(), sy = entry.size().getY();
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);
        int dw = Math.min(w - 4, sx * 3), dh = Math.min(h - 4, sz * 3);
        int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
        owner.fillGlassPanel(g, dx, dy, dw, dh, 0x66484858, alpha);
        String fs = sx + "\u00D7" + sz + "\u00D7" + sy;
        owner.drawGlassText(g, fs, x + (w - font.width(fs)) / 2, y + 1, owner.TEXT_SECONDARY, alpha);
    }

    private void renderVoxelPreview(GuiGraphics g, int x, int y, int w, int h, Blueprint bp, int alpha) {
        short bx = bp.getSizeX(), by = bp.getSizeY(), bz = bp.getSizeZ();
        short[][][] struct = bp.getStructure();
        BlockState[] pal = bp.getPalette();
        int maxDim = Math.max(bx, Math.max(by, bz));
        if (maxDim == 0) return;

        float scale = Math.min((float)w / (maxDim + 2), (float)h / (maxDim + 2)) * 0.6f;

        enableScissor(x, y, w, h);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2f, y + h / 2f, 0);
        pose.scale(scale, -scale, 1);
        pose.translate(-bx / 2f, -by / 2f, 0);

        var mc = Minecraft.getInstance();
        var blockRenderer = mc.getBlockRenderer();
        for (short iy = 0; iy < by; iy++) {
            for (short iz = 0; iz < bz; iz++) {
                for (short ix = 0; ix < bx; ix++) {
                    short val = struct[iy][iz][ix];
                    if (val == 0 || val >= pal.length) continue;
                    BlockState state = pal[val];
                    if (state.isAir()) continue;
                    pose.pushPose();
                    pose.translate(ix, iy, iz);
                    blockRenderer.renderSingleBlock(state, pose, mc.renderBuffers().bufferSource(),
                        0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
        mc.renderBuffers().bufferSource().endBatch();
        pose.popPose();
        disableScissor();
    }

    @Nullable
    private Blueprint loadBlueprintForPreview(BuildingCatalogEntry entry) {
        try {
            String path = entry.resourcePath() + ".blueprint";
            return com.metrogenesis.structurize.storage.StructurePacks.getBlueprint(entry.packName(), path, true);
        } catch (Exception e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  鼠标点击
    // ════════════════════════════════════════════════════════

    /** 处理点击，返回 true 表示消费。选中变更时自动调用 onSelectionChanged。 */
    public boolean mouseClicked(double mx, double my, int button, int x, int y, int w, int h) {
        if (button != 0) return false;

        final List<BuildingCatalogEntry> entries = getFilteredEntries();
        if (entries.isEmpty()) return false;

        final int startY = y + 20;
        if (mx < x || mx > x + w || my < startY || my > startY + h - 20) return false;

        switch (viewMode) {
            case LARGE_ICONS, SMALL_ICONS -> {
                int cellW = viewMode.cellW, cellH = viewMode.cellH;
                int availableW = w - 8;
                int cols = Math.max(1, availableW / (cellW + GRID_CELL_GAP));
                for (int i = 0; i < entries.size(); i++) {
                    int row = i / cols, col = i % cols;
                    int cx = x + 4 + col * (cellW + GRID_CELL_GAP);
                    int cy = startY + row * (cellH + GRID_CELL_GAP) - scrollOffset;
                    if (mx >= cx && mx <= cx + cellW && my >= cy && my <= cy + cellH) {
                        selectedIndex = i;
                        onSelectionChanged.run();
                        return true;
                    }
                }
            }
            case LIST -> {
                for (int i = 0; i < entries.size(); i++) {
                    int iy = startY + i * (ITEM_H + ITEM_GAP) - scrollOffset;
                    if (mx >= x + 4 && mx <= x + w - 4 && my >= iy && my <= iy + ITEM_H) {
                        selectedIndex = i;
                        onSelectionChanged.run();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════
    //  裁剪
    // ════════════════════════════════════════════════════════

    private static void enableScissor(int x, int y, int w, int h) {
        double s = Minecraft.getInstance().getWindow().getGuiScale();
        int scX = (int)(x * s), scY = (int)((Minecraft.getInstance().getWindow().getScreenHeight() - (y + h)) * s);
        int scW = Math.max(0, (int)(w * s)), scH = Math.max(0, (int)(h * s));
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private static void disableScissor() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
}
