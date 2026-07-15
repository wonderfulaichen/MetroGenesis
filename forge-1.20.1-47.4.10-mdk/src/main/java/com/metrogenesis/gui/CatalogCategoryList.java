package com.metrogenesis.gui;

import com.metrogenesis.catalog.CategoryMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 左侧分类/风格包列表组件 — 渲染文件夹树。
 * <p>
 * 显示 "全部"、风格包列表、选中包的子目录。
 * 拥有 {@code selectedPackName}、{@code selectedSubPath}、{@code scrollOffset} 状态。
 */
public class CatalogCategoryList {

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

    @Nullable private String selectedPackName = null;
    @Nullable private String selectedSubPath = null;
    private int scrollOffset = 0;

    // ════════════════════════════════════════════════════════

    private static final int ITEM_H = 20;
    private static final int ITEM_GAP = 2;

    // ════════════════════════════════════════════════════════

    public CatalogCategoryList(MayorBookScreen owner, Font font,
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

    @Nullable public String getSelectedPackName() { return selectedPackName; }
    public void setSelectedPackName(@Nullable String name) { selectedPackName = name; }

    @Nullable public String getSelectedSubPath() { return selectedSubPath; }
    public void setSelectedSubPath(@Nullable String path) { selectedSubPath = path; }

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int offset) { this.scrollOffset = Math.max(0, offset); }

    /** 滚轮滚动 */
    public void scroll(double delta) {
        scrollOffset = Math.max(0, scrollOffset - (int)(delta * 60));
    }

    // ════════════════════════════════════════════════════════
    //  渲染
    // ════════════════════════════════════════════════════════

    public void render(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        enableScissor(x, y, w, h);
        try {
            final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
            int itemY = y + 4 - scrollOffset;

            // "全部"选项
            final boolean allSelected = (selectedPackName == null);
            final boolean allHover = owner.inBounds(x + 2, itemY, w - 4, ITEM_H);
            final int allBg = allSelected ? owner.BTN_GLASS_SEL : (allHover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
            owner.fillGlassButton(g, x + 2, itemY, w - 4, ITEM_H, allBg, alpha);
            if (allSelected) {
                g.fill(x + 4, itemY + ITEM_H - 2, x + w - 6, itemY + ITEM_H,
                    owner.withAlpha(MGStyles.C_ACCENT, alpha));
            }
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.032"),
                x + 8, itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);
            final long allCount = dataManager.getAllEntries().size();
            final String countStr = String.valueOf(allCount);
            owner.drawGlassText(g, countStr, x + w - 10 - font.width(countStr),
                itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);
            itemY += ITEM_H + ITEM_GAP;

            // 风格包列表
            for (final var pack : packs) {
                if (itemY + ITEM_H < y || itemY > y + h) {
                    itemY += ITEM_H + ITEM_GAP;
                    continue;
                }

                final String packName = pack.getName();
                final boolean selected = packName.equals(selectedPackName);
                final boolean hover = owner.inBounds(x + 2, itemY, w - 4, ITEM_H);
                final int bg = selected ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);

                owner.fillGlassButton(g, x + 2, itemY, w - 4, ITEM_H, bg, alpha);
                if (selected) {
                    g.fill(x + 4, itemY + ITEM_H - 2, x + w - 6, itemY + ITEM_H,
                        owner.withAlpha(MGStyles.C_ACCENT, alpha));
                }

                // 文件夹图标
                owner.drawGlassText(g, "\uD83D\uDCC1", x + 6,
                    itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);

                // 名称（本地化）
                String displayName = ContentNameLocalizer.localizePackName(pack.getName());
                if (displayName.length() > 14) displayName = displayName.substring(0, 12) + "..";
                owner.drawGlassText(g, displayName, x + 22,
                    itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);

                // 数量
                final long count = dataManager.getAllEntries().stream()
                    .filter(e -> e.packName().equals(packName)).count();
                final String cntStr = String.valueOf(count);
                owner.drawGlassText(g, cntStr, x + w - 10 - font.width(cntStr),
                    itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);

                itemY += ITEM_H + ITEM_GAP;

                // 子目录
                if (selected) {
                    final var categories = com.metrogenesis.structurize.storage.StructurePacks
                        .getCategories(packName, "");
                    for (final var cat : categories) {
                        if (itemY + ITEM_H < y || itemY > y + h) {
                            itemY += ITEM_H + ITEM_GAP;
                            continue;
                        }

                        final String sub = cat.subPath;
                        final boolean subSel = sub.equals(selectedSubPath);
                        final boolean subHover = owner.inBounds(x + 2, itemY, w - 4, ITEM_H);
                        final int subBg = subSel ? owner.BTN_GLASS_SEL
                            : (subHover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);

                        owner.fillGlassButton(g, x + 2, itemY, w - 4, ITEM_H, subBg, alpha);
                        owner.drawGlassText(g, "\u2502", x + 6,
                            itemY + (ITEM_H - font.lineHeight) / 2 + 1,
                            owner.withAlpha(owner.TEXT_SECONDARY, alpha), alpha);
                        owner.drawGlassText(g, "\uD83D\uDCC2", x + 18,
                            itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);

                        String subName = ContentNameLocalizer.localizeDirectoryName(sub);
                        if (subName.length() > 12) subName = subName.substring(0, 10) + "..";
                        owner.drawGlassText(g, subName, x + 36,
                            itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);

                        final long sc = dataManager.getAllEntries().stream()
                            .filter(e -> e.packName().equals(packName) && e.mcCategory().equals(sub))
                            .count();
                        final String scStr = String.valueOf(sc);
                        owner.drawGlassText(g, scStr, x + w - 10 - font.width(scStr),
                            itemY + (ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);

                        itemY += ITEM_H + ITEM_GAP;
                    }
                }
            }
        } finally {
            disableScissor();
        }
    }

    // ════════════════════════════════════════════════════════
    //  鼠标点击
    // ════════════════════════════════════════════════════════

    /** 处理点击，返回 true 表示消费。选中变更时自动调用 onSelectionChanged。 */
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h) {
        final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
        int itemY = y + 4 - scrollOffset;

        // "全部"
        if (owner.inBounds(x + 2, itemY, w - 4, ITEM_H)) {
            selectedPackName = null;
            selectedSubPath = null;
            onSelectionChanged.run();
            return true;
        }
        itemY += ITEM_H + ITEM_GAP;

        // 风格包
        for (final var pack : packs) {
            if (owner.inBounds(x + 2, itemY, w - 4, ITEM_H)) {
                final String pn = pack.getName();
                if (pn.equals(selectedPackName)) {
                    selectedPackName = null;
                    selectedSubPath = null;
                } else {
                    selectedPackName = pn;
                    selectedSubPath = null;
                }
                onSelectionChanged.run();
                return true;
            }
            itemY += ITEM_H + ITEM_GAP;

            // 子目录
            if (pack.getName().equals(selectedPackName)) {
                final var cats = com.metrogenesis.structurize.storage.StructurePacks
                    .getCategories(pack.getName(), "");
                for (final var cat : cats) {
                    if (owner.inBounds(x + 2, itemY, w - 4, ITEM_H)) {
                        selectedSubPath = cat.subPath;
                        onSelectionChanged.run();
                        return true;
                    }
                    itemY += ITEM_H + ITEM_GAP;
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
        int scX = (int)(x * s);
        int scY = (int)((Minecraft.getInstance().getWindow().getScreenHeight() - (y + h)) * s);
        int scW = Math.max(0, (int)(w * s));
        int scH = Math.max(0, (int)(h * s));
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private static void disableScissor() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
}
