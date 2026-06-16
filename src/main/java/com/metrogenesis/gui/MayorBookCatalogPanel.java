package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.catalog.BuildingCatalogScanner;
import com.metrogenesis.catalog.CategoryMapper;
import com.metrogenesis.network.BlueprintPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 蓝图图鉴面板 — 从 {@link MayorBookScreen} 单体中抽取的独立图鉴组件。
 * <p>
 * 消化吸收改造：
 * <ul>
 *   <li>数据源从 BlueprintLibraryData 改为 BuildingCatalogScanner（殖民地扫描逻辑）</li>
 *   <li>分类使用 CategoryMapper（殖民地目录名 → MetroGenesis 8 大分类）</li>
 *   <li>元数据使用 BuildingCatalogEntry（兼容 StructurePacks 生态）</li>
 * </ul>
 * <p>
 * 封装蓝图分类浏览、列表选择、等轴测体素预览、两阶段放置流程。
 * 被 MayorBookScreen 创建并委托渲染/输入。
 * </p>
 */
public class MayorBookCatalogPanel
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // ════════════════════════════════════════════════════════
    //  依赖引用（由 MayorBookScreen 注入）
    // ════════════════════════════════════════════════════════

    private final Font font;
    private final int width;
    private final int height;
    private final MayorBookScreen owner;

    // ════════════════════════════════════════════════════════
    //  视图模式枚举
    // ════════════════════════════════════════════════════════

    /**
     * 图鉴视图模式（类似 Windows 文件管理器）。
     */
    private enum ViewMode {
        /** 大图标模式 — 显示等轴测缩略图 */
        LARGE_ICONS(80, 80),
        /** 小图标模式 — 显示小型缩略图 */
        SMALL_ICONS(40, 40),
        /** 列表模式 — 纯文字列表 */
        LIST(0, 20);

        final int cellW;
        final int cellH;

        ViewMode(int cellW, int cellH) {
            this.cellW = cellW;
            this.cellH = cellH;
        }
    }

    // ════════════════════════════════════════════════════════
    //  Catalog 状态
    // ════════════════════════════════════════════════════════

    private boolean catalogMode = false;
    private int catalogCategoryIndex = -1;     // 选中的主分类索引（-1=全部）
    private int catalogSelectedIndex = -1;     // 在过滤后的蓝图列表中的选中索引
    private int catalogScrollOffset = 0;       // 列表滚动偏移
    private int folderScrollOffset = 0;        // 文件夹树滚动偏移

    // 文件夹树状态
    private String selectedPackName = null;    // 选中的风格包名（如 "medievaloak"）
    private String selectedSubPath = null;     // 选中的子路径（如 "agriculture"）
    private boolean showFolderTree = true;     // 是否显示文件夹树

    // 视图模式
    private ViewMode viewMode = ViewMode.LARGE_ICONS;  // 当前视图模式

    // 放置二次确认（两阶段流程）
    private boolean placementMode = false;     // 阶段1：RTS 视角拖拽确定位置
    private boolean placementDragging = false; // 正在拖拽确定放置位置
    private boolean placementConfirm = false;  // 阶段2：显示确认弹窗
    private BlockPos placementTargetPos = null; // 拖拽确定的放置位置

    // ════════════════════════════════════════════════════════
    //  扫描器状态
    // ════════════════════════════════════════════════════════

    private final BuildingCatalogScanner scanner = new BuildingCatalogScanner();
    private Future<List<BuildingCatalogEntry>> scanFuture = null;
    private List<BuildingCatalogEntry> allEntries = List.of();
    private boolean scanStarted = false;

    // ════════════════════════════════════════════════════════
    //  布局常量
    // ════════════════════════════════════════════════════════

    private static final int CATALOG_BTN_W = 64;
    private static final int CATALOG_BTN_H = 28;

    private static final int CATALOG_PANEL_X = 40;
    private static final int CATALOG_PANEL_Y = 30;
    private static final int CATALOG_PANEL_W_RATIO_NUM = 3;
    private static final int CATALOG_PANEL_W_RATIO_DEN = 4;
    private static final int CATALOG_PANEL_H_RATIO_NUM = 5;
    private static final int CATALOG_PANEL_H_RATIO_DEN = 6;
    private static final int CATALOG_LEFT_W = 140;
    private static final int CATALOG_ITEM_H = 20;
    private static final int CATALOG_ITEM_GAP = 2;
    private static final int CATALOG_BOTTOM_H = 28;

    // 网格布局常量
    private static final int GRID_CELL_GAP = 4;      // 网格单元间距

    // ════════════════════════════════════════════════════════
    //  构造
    // ════════════════════════════════════════════════════════

    public MayorBookCatalogPanel(final int width, final int height, final Font font, final MayorBookScreen owner)
    {
        this.width = width;
        this.height = height;
        this.font = font;
        this.owner = owner;
    }

    /**
     * 屏幕尺寸发生变化时更新（由 MayorBookScreen 的 resize() 调用）。
     */
    public void resize(final int newWidth, final int newHeight)
    {
        // layout 常量不受尺寸影响，不需要实际更新
    }

    // ════════════════════════════════════════════════════════
    //  状态查询
    // ════════════════════════════════════════════════════════

    public boolean isCatalogMode() { return catalogMode; }
    public boolean isPlacementMode() { return placementMode; }
    public boolean isPlacementConfirm() { return placementConfirm; }
    public boolean isPlacementDragging() { return placementDragging; }
    public BlockPos getPlacementTargetPos() { return placementTargetPos; }

    public void setPlacementDragging(final boolean dragging) { this.placementDragging = dragging; }
    public void setPlacementTargetPos(final BlockPos pos) { this.placementTargetPos = pos; }

    /**
     * 打开图鉴 — 重置状态并进入图鉴模式。
     * 首次打开时触发异步扫描，之后使用缓存。
     */
    public void open()
    {
        catalogMode = true;
        catalogCategoryIndex = -1;
        catalogSelectedIndex = -1;
        catalogScrollOffset = 0;
        folderScrollOffset = 0;
        selectedPackName = null;
        selectedSubPath = null;
        placementMode = false;
        placementConfirm = false;
        placementTargetPos = null;

        // 首次打开触发扫描（之后使用缓存）
        if (!scanStarted && allEntries.isEmpty())
        {
            scanStarted = true;
            scanFuture = scanner.scanAllAsync();
        }
    }

    /**
     * 手动刷新 — 重新扫描所有蓝图。
     */
    public void refresh()
    {
        BuildingCatalogScanner.clearCache();
        scanStarted = true;
        scanFuture = scanner.scanAllAsync();
        allEntries = List.of();
    }

    /**
     * 关闭图鉴模式。
     */
    public void close()
    {
        catalogMode = false;
        placementMode = false;
        placementConfirm = false;
        placementTargetPos = null;
    }

    // ════════════════════════════════════════════════════════
    //  渲染（由 MayorBookScreen.render() 调用）
    // ════════════════════════════════════════════════════════

    /**
     * 主渲染入口 — 在 3D 城市鸟瞰之上叠加图鉴面板。
     *
     * @param g     GuiGraphics
     * @param alpha 全局透明度（用于淡入动画）
     */
    public void render(final GuiGraphics g, final int alpha)
    {
        if (!catalogMode) return;

        // 检查扫描结果
        checkScanResult();

        if (placementConfirm)
        {
            renderPlacementConfirm(g, alpha);
        }
        else if (placementMode)
        {
            renderPlacementOverlay(g, alpha);
        }
        else
        {
            renderCatalogPanel(g, alpha);
        }
    }

    /**
     * 检查异步扫描结果。
     */
    private void checkScanResult()
    {
        if (scanFuture != null && scanFuture.isDone())
        {
            try
            {
                allEntries = scanFuture.get();
                LOGGER.info("[Catalog] Scanned {} building entries", allEntries.size());
            }
            catch (InterruptedException | ExecutionException e)
            {
                LOGGER.error("[Catalog] Scan failed: {}", e.getMessage());
                allEntries = List.of();
            }
            scanFuture = null;
        }
    }

    /**
     * 渲染图鉴主面板。
     */
    private void renderCatalogPanel(final GuiGraphics g, final int alpha)
    {
        int panelW = width * CATALOG_PANEL_W_RATIO_NUM / CATALOG_PANEL_W_RATIO_DEN;
        panelW = Math.min(panelW, width - 80);
        int panelH = height * CATALOG_PANEL_H_RATIO_NUM / CATALOG_PANEL_H_RATIO_DEN;
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // 背景
        owner.fillGlassPanel(g, panelX, panelY, panelW, panelH, owner.GLASS_BG, alpha);

        // 标题栏
        owner.fillGlassPanel(g, panelX, panelY, panelW, 22, owner.GLASS_HEADER, alpha);
        owner.drawGlassText(g, "\uD83D\uDCD6 \u84DD\u56FE\u56FE\u9274", panelX + 8, panelY + 6, owner.TEXT_SOFT_GOLD, alpha);

        // 视图模式切换按钮（大图标/小图标/列表）
        int modeBtnX = panelX + panelW - 90;
        for (final ViewMode mode : ViewMode.values())
        {
            final boolean active = (viewMode == mode);
            final boolean modeHover = owner.inBounds(modeBtnX, panelY + 3, 14, 14);
            final int modeBg = active ? owner.BTN_GLASS_SEL : (modeHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS);
            owner.fillGlassButton(g, modeBtnX, panelY + 3, 14, 14, modeBg, alpha);

            // 图标
            final String icon = switch (mode)
            {
                case LARGE_ICONS -> "\u25A0"; // ■
                case SMALL_ICONS -> "\u25AA"; // ▪
                case LIST -> "\u2630"; // ☰
            };
            owner.drawGlassText(g, icon, modeBtnX + 2, panelY + 5, active ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);
            modeBtnX += 18;
        }

        // 刷新按钮
        int refreshX = panelX + panelW - 36;
        boolean refreshHover = owner.inBounds(refreshX, panelY + 3, 14, 14);
        owner.fillGlassButton(g, refreshX, panelY + 3, 14, 14, refreshHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, "\u21BB", refreshX + 2, panelY + 5, owner.TEXT_PRIMARY, alpha);

        // 关闭按钮
        int closeX = panelX + panelW - 18;
        boolean closeHover = owner.inBounds(closeX, panelY + 3, 14, 14);
        owner.fillGlassButton(g, closeX, panelY + 3, 14, 14, closeHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, "\u00d7", closeX + 3, panelY + 5, owner.TEXT_PRIMARY, alpha);

        // 分隔线
        g.fill(panelX + 4, panelY + 22, panelX + panelW - 4, panelY + 23, owner.withAlpha(owner.EDGE_INNER, alpha));

        // 检查扫描状态
        if (scanFuture != null)
        {
            renderScanningState(g, panelX, panelY, panelW, panelH, alpha);
            return;
        }

        // ── 左侧：分类列表 ──
        int leftX = panelX + 4;
        int leftY = panelY + 26;
        int leftW = CATALOG_LEFT_W - 8;
        int leftH = panelH - 30 - CATALOG_BOTTOM_H - 4;
        renderCatalogCategoryList(g, leftX, leftY, leftW, leftH, alpha);

        // 分隔线
        g.fill(panelX + CATALOG_LEFT_W, leftY, panelX + CATALOG_LEFT_W + 1, leftY + leftH, owner.withAlpha(owner.EDGE_INNER, alpha));

        // ── 右侧上方：蓝图列表 ──
        int rightX = panelX + CATALOG_LEFT_W + 4;
        int rightY = leftY;
        int rightW = panelW - CATALOG_LEFT_W - 12;
        int listH = leftH / 2;
        renderCatalogBlueprintList(g, rightX, rightY, rightW, listH, alpha);

        // 分隔线
        g.fill(rightX, rightY + listH + 2, rightX + rightW, rightY + listH + 3, owner.withAlpha(owner.EDGE_INNER, alpha));

        // ── 右侧下方：3D 预览 ──
        int previewY = rightY + listH + 6;
        int previewH = leftH - listH - 8;
        renderCatalogPreview(g, rightX, previewY, rightW, previewH, alpha);

        // ── 底部操作栏 ──
        int bottomY = panelY + panelH - CATALOG_BOTTOM_H - 2;
        g.fill(panelX + 4, bottomY, panelX + panelW - 4, bottomY + 1, owner.withAlpha(owner.EDGE_INNER, alpha));
        renderCatalogBottomBar(g, panelX, bottomY, panelW, alpha);
    }

    /**
     * 渲染扫描中状态。
     */
    private void renderScanningState(final GuiGraphics g, final int panelX, final int panelY,
                                      final int panelW, final int panelH, final int alpha)
    {
        String msg = "\u6B63\u5728\u626B\u63CF\u5EFA\u7B51\u84DD\u56FE...";
        owner.drawGlassText(g, msg, panelX + panelW / 2 - font.width(msg) / 2,
            panelY + panelH / 2 - 4, owner.TEXT_SOFT_GOLD, alpha);

        // 进度条
        float progress = scanner.getProgress();
        int barW = panelW - 40;
        int barX = panelX + 20;
        int barY = panelY + panelH / 2 + 10;
        int barH = 8;
        owner.fillGlassPanel(g, barX, barY, barW, barH, owner.GLASS_PANEL, alpha);
        g.fill(barX + 1, barY + 1, barX + 1 + (int)((barW - 2) * progress), barY + barH - 1,
            owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
    }

    /**
     * 渲染文件夹树（左侧）。
     * 显示风格包列表，点击展开显示子目录。
     */
    private void renderCatalogCategoryList(final GuiGraphics g, final int x, final int y,
                                           final int w, final int h, final int alpha)
    {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        // 获取所有风格包
        final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
        int itemY = y + 4 - folderScrollOffset;

        // "全部"选项
        final boolean allSelected = (selectedPackName == null);
        final boolean allHover = owner.inBounds(x + 2, itemY, w - 4, CATALOG_ITEM_H);
        final int allBg = allSelected ? owner.BTN_GLASS_SEL : (allHover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
        owner.fillGlassButton(g, x + 2, itemY, w - 4, CATALOG_ITEM_H, allBg, alpha);
        if (allSelected)
        {
            g.fill(x + 4, itemY + CATALOG_ITEM_H - 2, x + w - 6, itemY + CATALOG_ITEM_H,
                owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
        }
        owner.drawGlassText(g, "\u5168\u90E8\u84DD\u56FE", x + 8, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
            allSelected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);
        final long allCount = allEntries.size();
        owner.drawGlassText(g, String.valueOf(allCount), x + w - 10 - font.width(String.valueOf(allCount)),
            itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);
        itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;

        // 风格包列表
        for (final var pack : packs)
        {
            if (itemY + CATALOG_ITEM_H < y || itemY > y + h)
            {
                itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;
                continue;
            }

            final String packName = pack.getName();
            final boolean selected = packName.equals(selectedPackName);
            final boolean hover = owner.inBounds(x + 2, itemY, w - 4, CATALOG_ITEM_H);
            final int bg = selected ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);

            owner.fillGlassButton(g, x + 2, itemY, w - 4, CATALOG_ITEM_H, bg, alpha);
            if (selected)
            {
                g.fill(x + 4, itemY + CATALOG_ITEM_H - 2, x + w - 6, itemY + CATALOG_ITEM_H,
                    owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
            }

            // 文件夹图标
            owner.drawGlassText(g, "\uD83D\uDCC1", x + 6, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                selected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

            // 风格包名称（截断）
            String displayName = pack.getName();
            if (displayName.length() > 14) displayName = displayName.substring(0, 12) + "..";
            owner.drawGlassText(g, displayName, x + 22, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                selected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

            // 显示该包下的蓝图数量
            final long count = allEntries.stream().filter(e -> e.packName().equals(packName)).count();
            final String countStr = String.valueOf(count);
            owner.drawGlassText(g, countStr, x + w - 10 - font.width(countStr),
                itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);

            itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;

            // 如果选中了这个包，显示子目录
            if (selected)
            {
                // 获取该包的子目录
                final var categories = com.metrogenesis.structurize.storage.StructurePacks.getCategories(packName, "");
                for (final var cat : categories)
                {
                    if (itemY + CATALOG_ITEM_H < y || itemY > y + h)
                    {
                        itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;
                        continue;
                    }

                    final String subPath = cat.subPath;
                    final boolean subSelected = subPath.equals(selectedSubPath);
                    final boolean subHover = owner.inBounds(x + 2, itemY, w - 4, CATALOG_ITEM_H);
                    final int subBg = subSelected ? owner.BTN_GLASS_SEL : (subHover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);

                    owner.fillGlassButton(g, x + 2, itemY, w - 4, CATALOG_ITEM_H, subBg, alpha);

                    // 连接线（树状结构）
                    owner.drawGlassText(g, "│", x + 6, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                        owner.withAlpha(owner.TEXT_SECONDARY, alpha), alpha);

                    // 子目录图标
                    owner.drawGlassText(g, "\uD83D\uDCC2", x + 18, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                        subSelected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

                    // 子目录名称
                    String subDisplayName = subPath;
                    if (subDisplayName.length() > 12) subDisplayName = subDisplayName.substring(0, 10) + "..";
                    owner.drawGlassText(g, subDisplayName, x + 36, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                        subSelected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

                    // 显示该子目录下的蓝图数量
                    final long subCount = allEntries.stream()
                        .filter(e -> e.packName().equals(packName) && e.mcCategory().equals(subPath))
                        .count();
                    final String subCountStr = String.valueOf(subCount);
                    owner.drawGlassText(g, subCountStr, x + w - 10 - font.width(subCountStr),
                        itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);

                    itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;
                }
            }
        }
    }

    /**
     * 渲染分类图标。
     * TODO: 后续接入 OutOfJarResourceLocation 加载 icon.png
     * 当前版本暂用分类首字符作为图标占位。
     */
    private void renderCategoryIcon(final GuiGraphics g, final String category,
                                     final int x, final int y, final int w, final int h, final int alpha)
    {
        // 用分类首字符作为简易图标
        final String iconChar = switch (category)
        {
            case "\u4F4F\u5B85" -> "\uD83C\uDFE0"; // 🏠
            case "\u516C\u5171\u8BBE\u65BD" -> "\uD83C\uDFDB"; // 🏛
            case "\u57FA\u7840\u8BBE\u65BD" -> "\uD83C\uDFD7"; // 🏗
            case "\u751F\u4EA7\u5EFA\u7B51" -> "\u2692"; // ⚒
            case "\u5546\u4E1A" -> "\uD83D\uDCB0"; // 💰
            case "\u519B\u4E8B" -> "\u2694"; // ⚔
            case "\u5176\u4ED6" -> "\u2B50"; // ⭐
            default -> "\u25CF"; // ●
        };
        owner.drawGlassText(g, iconChar, x, y, owner.TEXT_SOFT_GOLD, alpha);
    }

    /**
     * 获取 MetroGenesis 分类对应的殖民地目录名。
     */
    private String getMcCategoryForMgCategory(String mgCategory)
    {
        return switch (mgCategory)
        {
            case "住宅" -> "fundamentals";
            case "公共设施" -> "education";
            case "基础设施" -> "infrastructure";
            case "生产建筑" -> "agriculture";
            case "商业" -> "commercial";
            case "军事" -> "military";
            case "其他" -> "decorations";
            default -> null;
        };
    }

    /**
     * 渲染蓝图列表/网格（右上）。
     * 根据 viewMode 选择渲染方式。
     */
    private void renderCatalogBlueprintList(final GuiGraphics g, final int x, final int y,
                                            final int w, final int h, final int alpha)
    {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        // 标题
        final String title = switch (viewMode)
        {
            case LARGE_ICONS -> "\u5927\u56FE\u6807\u89C6\u56FE";
            case SMALL_ICONS -> "\u5C0F\u56FE\u6807\u89C6\u56FE";
            case LIST -> "\u5217\u8868\u89C6\u56FE";
        };
        owner.drawGlassText(g, title, x + 6, y + 4, owner.TEXT_SOFT_GOLD, alpha);
        g.fill(x + 4, y + 16, x + w - 4, y + 17, owner.withAlpha(owner.EDGE_INNER, alpha));

        String selectedCat = CategoryMapper.getCategoryByIndex(catalogCategoryIndex);
        final List<BuildingCatalogEntry> entries = getEntriesForCategory(selectedCat);
        int startY = y + 20;

        if (entries.isEmpty())
        {
            owner.drawGlassText(g, "\u8BE5\u5206\u7C7B\u4E0B\u6CA1\u6709\u84DD\u56FE", x + 8, startY + 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 根据视图模式选择渲染方式
        switch (viewMode)
        {
            case LARGE_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 80, 80, alpha);
            case SMALL_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 40, 40, alpha);
            case LIST -> renderList(g, x, startY, w, h - 20, entries, alpha);
        }
    }

    /**
     * 渲染网格视图（大图标/小图标）。
     */
    private void renderGrid(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final List<BuildingCatalogEntry> entries, final int cellW, final int cellH, final int alpha)
    {
        // 计算每行可以放多少列
        int availableW = w - 8;
        int cols = Math.max(1, availableW / (cellW + GRID_CELL_GAP));

        // 渲染网格
        for (int i = 0; i < entries.size(); i++)
        {
            int row = i / cols;
            int col = i % cols;

            int cellX = x + 4 + col * (cellW + GRID_CELL_GAP);
            int cellY = y + row * (cellH + GRID_CELL_GAP) - catalogScrollOffset;

            // 跳过不在可见区域的单元格
            if (cellY + cellH < y || cellY > y + h)
            {
                continue;
            }

            final BuildingCatalogEntry entry = entries.get(i);
            final boolean selected = (catalogSelectedIndex == i);
            final boolean hover = owner.inBounds(cellX, cellY, cellW, cellH);

            // 背景
            final int bg = selected ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
            owner.fillGlassPanel(g, cellX, cellY, cellW, cellH, bg, alpha);

            // 选中边框
            if (selected)
            {
                g.fill(cellX, cellY, cellX + cellW, cellY + 1, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
                g.fill(cellX, cellY + cellH - 1, cellX + cellW, cellY + cellH, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
                g.fill(cellX, cellY, cellX + 1, cellY + cellH, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
                g.fill(cellX + cellW - 1, cellY, cellX + cellW, cellY + cellH, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
            }

            // 缩略图预览区域
            int previewX = cellX + 4;
            int previewY = cellY + 4;
            int previewW = cellW - 8;
            int previewH = cellH - 20;

            // 渲染小型等轴测预览
            renderMiniPreview(g, previewX, previewY, previewW, previewH, entry, alpha);

            // 名称（底部）
            String displayName = entry.name();
            if (displayName.length() > 10) displayName = displayName.substring(0, 8) + "..";
            owner.drawGlassText(g, displayName, cellX + (cellW - font.width(displayName)) / 2,
                cellY + cellH - 14, selected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);
        }
    }

    /**
     * 渲染列表视图。
     */
    private void renderList(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final List<BuildingCatalogEntry> entries, final int alpha)
    {
        final int itemH = 20;
        final int itemGap = 2;

        for (int i = 0; i < entries.size(); i++)
        {
            int itemY = y + i * (itemH + itemGap) - catalogScrollOffset;

            // 跳过不在可见区域的项目
            if (itemY + itemH < y || itemY > y + h)
            {
                continue;
            }

            final BuildingCatalogEntry entry = entries.get(i);
            final boolean selected = (catalogSelectedIndex == i);
            final boolean hover = owner.inBounds(x + 4, itemY, w - 8, itemH);

            // 背景
            final int bg = selected ? owner.BTN_GLASS_SEL : (hover ? owner.BTN_GLASS_HOVER : owner.GLASS_PANEL);
            owner.fillGlassButton(g, x + 4, itemY, w - 8, itemH, bg, alpha);

            // 名称
            String displayName = entry.name();
            if (displayName.length() > 20) displayName = displayName.substring(0, 18) + "..";
            owner.drawGlassText(g, displayName, x + 10, itemY + (itemH - font.lineHeight) / 2 + 1,
                selected ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

            // 尺寸
            final String sizeStr = entry.sizeDisplay();
            owner.drawGlassText(g, sizeStr, x + w - 10 - font.width(sizeStr),
                itemY + (itemH - font.lineHeight) / 2 + 1, owner.TEXT_SECONDARY, alpha);
        }
    }

    /**
     * 渲染小型等轴测预览。
     * 比大型预览更简化，用于网格缩略图。
     */
    private void renderMiniPreview(final GuiGraphics g, final int x, final int y,
                                    final int w, final int h, final BuildingCatalogEntry entry, final int alpha)
    {
        // 使用尺寸信息渲染简化的建筑轮廓
        int sizeX = entry.size().getX();
        int sizeY = entry.size().getY();
        int sizeZ = entry.size().getZ();

        // 等轴测投影参数（简化版）
        float scale = Math.min(w / (float)(sizeX + sizeZ + 2), h / (float)(sizeY + 2)) * 0.5f;
        scale = Math.max(1f, Math.min(scale, 3f));

        int centerX = x + w / 2;
        int centerY = y + h / 2;

        // 渲染简化的建筑轮廓（几个色块代表建筑）
        int blockSize = Math.max(1, (int)(scale * 0.8f));

        // 绘制简化的建筑形状
        for (int by = 0; by < Math.min(sizeY, 3); by++)
        {
            for (int bz = 0; bz < Math.min(sizeZ, 3); bz++)
            {
                for (int bx = 0; bx < Math.min(sizeX, 3); bx++)
                {
                    // 等轴测坐标
                    float isoX = (bx - bz) * scale;
                    float isoY = (bx + bz) * scale * 0.5f - by * scale;

                    int drawX = centerX + (int)isoX;
                    int drawY = centerY + (int)isoY;

                    // 根据位置选择颜色
                    int color;
                    if (by == 0) // 底层
                    {
                        color = 0xFF8B7355; // 棕色（木头）
                    }
                    else if (by == sizeY - 1) // 顶层
                    {
                        color = 0xFF8B0000; // 深红色（屋顶）
                    }
                    else // 中间层
                    {
                        color = 0xFFD2B48C; // 浅棕色（墙壁）
                    }

                    color = owner.withAlpha(color, alpha);
                    g.fill(drawX, drawY, drawX + blockSize, drawY + blockSize, color);

                    // 添加阴影效果
                    if (blockSize >= 2)
                    {
                        g.fill(drawX, drawY, drawX + blockSize, drawY + 1, owner.withAlpha(0x44000000, alpha));
                        g.fill(drawX, drawY, drawX + 1, drawY + blockSize, owner.withAlpha(0x44000000, alpha));
                    }
                }
            }
        }

        // 显示尺寸
        String sizeStr = sizeX + "×" + sizeZ + "×" + sizeY;
        owner.drawGlassText(g, sizeStr, x + (w - font.width(sizeStr)) / 2, y + 1, owner.TEXT_SECONDARY, alpha);
    }

    /**
     * 渲染 3D 体素预览（右下）。
     * 使用 StructurePacks 加载蓝图进行预览。
     */
    private void renderCatalogPreview(final GuiGraphics g, final int x, final int y,
                                      final int w, final int h, final int alpha)
    {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry == null)
        {
            owner.drawGlassText(g, "\u9009\u62E9\u4E00\u4E2A\u5EFA\u7B51\u9884\u89C8", x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 使用 StructurePacks 加载蓝图（兼容殖民地生态）
        final Blueprint bp = loadBlueprintForPreview(entry);
        if (bp == null)
        {
            owner.drawGlassText(g, "\u65E0\u6CD5\u52A0\u8F7D\u84DD\u56FE\u6570\u636E", x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        renderVoxelPreview(g, x + 4, y + 4, w - 8, h - 8, bp, alpha);
    }

    /**
     * 等轴测体素渲染 — 复用 BlueprintCaptureResultScreen 的体素渲染逻辑。
     */
    private void renderVoxelPreview(final GuiGraphics g, final int x, final int y,
                                    final int w, final int h, final Blueprint bp, final int alpha)
    {
        final short bpSizeX = bp.getSizeX();
        final short bpSizeY = bp.getSizeY();
        final short bpSizeZ = bp.getSizeZ();
        final short[][][] structure = bp.getStructure();
        final BlockState[] palette = bp.getPalette();

        // 等轴测投影参数
        final float isoScaleX = Math.max(1f, Math.min(w / (float) (bpSizeX + bpSizeZ + 2), 4f)) * 0.5f;
        final float isoScaleY = isoScaleX * 0.5f;
        final float isoScaleH = isoScaleX * 1.0f;

        // 计算包围盒用于居中
        float minIsoX = Float.MAX_VALUE, maxIsoX = -Float.MAX_VALUE;
        float minIsoY = Float.MAX_VALUE, maxIsoY = -Float.MAX_VALUE;
        final var clientLevel = Minecraft.getInstance().level;

        for (short by = 0; by < bpSizeY; by++)
        {
            for (short bz = 0; bz < bpSizeZ; bz++)
            {
                for (short bx = 0; bx < bpSizeX; bx++)
                {
                    if (structure[by][bz][bx] == 0) continue;
                    final float ix = (bx - bz) * isoScaleX;
                    final float iy = (bx + bz) * isoScaleY - by * isoScaleH;
                    minIsoX = Math.min(minIsoX, ix);
                    maxIsoX = Math.max(maxIsoX, ix);
                    minIsoY = Math.min(minIsoY, iy);
                    maxIsoY = Math.max(maxIsoY, iy);
                }
            }
        }

        if (minIsoX == Float.MAX_VALUE)
        {
            owner.drawGlassText(g, "\u7A7A\u84DD\u56FE", x + w / 2 - 12, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        final int centerX = x + w / 2;
        final int centerY = y + h / 2;
        final float centerIsoX = (minIsoX + maxIsoX) / 2;
        final float centerIsoY = (minIsoY + maxIsoY) / 2;
        final float offsetX = centerX - centerIsoX;
        final float offsetY = centerY - centerIsoY;

        final int blockSize = Math.max(1, (int) (isoScaleX * 0.8f));

        // 从后到前遍历（等轴测遮挡顺序）
        for (short by = (short) (bpSizeY - 1); by >= 0; by--)
        {
            for (short bz = (short) (bpSizeZ - 1); bz >= 0; bz--)
            {
                for (short bx = 0; bx < bpSizeX; bx++)
                {
                    final short val = structure[by][bz][bx];
                    if (val == 0) continue;

                    final float isoX = (bx - bz) * isoScaleX + offsetX;
                    final float isoY = (bx + bz) * isoScaleY - by * isoScaleH + offsetY;
                    final int drawX = (int) isoX;
                    final int drawY = (int) isoY;

                    int color = 0xFF888888;
                    if (val >= 0 && val < palette.length)
                    {
                        final BlockState state = palette[val];
                        try
                        {
                            final MapColor mapColor = state.getMapColor(clientLevel, BlockPos.ZERO);
                            if (mapColor != null)
                            {
                                color = 0xFF000000 | (mapColor.col & 0xFFFFFF);
                            }
                        }
                        catch (Exception e)
                        {
                            LOGGER.debug("[Catalog] getMapColor failed for palette[{}]: {}", val, e.getMessage());
                        }
                    }

                    color = owner.withAlpha(color, alpha);
                    g.fill(drawX, drawY, drawX + blockSize, drawY + blockSize, color);
                    if (blockSize >= 3)
                    {
                        g.fill(drawX, drawY, drawX + blockSize, drawY + 1, owner.withAlpha(0x44000000, alpha));
                        g.fill(drawX, drawY, drawX + 1, drawY + blockSize, owner.withAlpha(0x44000000, alpha));
                    }
                }
            }
        }

        final String dimStr = bpSizeX + "\u00D7" + bpSizeZ + "\u00D7" + bpSizeY;
        owner.drawGlassText(g, dimStr, x + w / 2 - font.width(dimStr) / 2, y + 2, owner.TEXT_SECONDARY, alpha);
    }

    /**
     * 渲染底部操作栏。
     * 使用 BuildingCatalogEntry 作为数据源。
     */
    private void renderCatalogBottomBar(final GuiGraphics g, final int panelX,
                                        final int bottomY, final int panelW, final int alpha)
    {
        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry == null)
        {
            owner.drawGlassText(g, "\u8BF7\u5148\u9009\u62E9\u5EFA\u7B51", panelX + 12, bottomY + 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        owner.drawGlassText(g, entry.name(), panelX + 12, bottomY + 7, owner.TEXT_SOFT_GOLD, alpha);

        final String sizeStr = entry.sizeDisplay();
        owner.drawGlassText(g, sizeStr, panelX + 12 + font.width(entry.name()) + 12, bottomY + 7, owner.TEXT_SECONDARY, alpha);

        // 显示风格
        final String packStr = "[" + entry.packName() + "]";
        owner.drawGlassText(g, packStr, panelX + 12 + font.width(entry.name()) + 12 + font.width(sizeStr) + 12,
            bottomY + 7, owner.TEXT_SECONDARY, alpha);

        final int placeBtnW = 60;
        final int placeBtnX = panelX + panelW - placeBtnW - 16;
        final boolean placeHover = owner.inBounds(placeBtnX, bottomY + 3, placeBtnW, 20);
        final int placeBg = placeHover ? 0x66486880 : 0x44384858;
        owner.fillGlassButton(g, placeBtnX, bottomY + 3, placeBtnW, 20, placeBg, alpha);
        owner.drawGlassText(g, "\u653E\u7F6E", placeBtnX + (placeBtnW - font.width("\u653E\u7F6E")) / 2, bottomY + 9,
            placeHover ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);
    }

    /**
     * 渲染放置确认弹窗（阶段2）。
     */
    private void renderPlacementConfirm(final GuiGraphics g, final int alpha)
    {
        final int dialogW = 260;
        final int dialogH = 80;
        final int dialogX = (width - dialogW) / 2;
        final int dialogY = (height - dialogH) / 2;

        owner.fillGlassPanel(g, dialogX, dialogY, dialogW, dialogH, owner.GLASS_BG, alpha);

        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        final String bpName = entry != null ? entry.name() : "?";

        owner.drawGlassText(g, "\u786E\u8BA4\u653E\u7F6E [" + bpName + "]\uFF1F", dialogX + 20, dialogY + 15, owner.TEXT_SOFT_GOLD, alpha);
        owner.drawGlassText(g, "\u4F4D\u7F6E: " + (placementTargetPos != null ? placementTargetPos.toShortString() : "?"),
            dialogX + 20, dialogY + 32, owner.TEXT_SECONDARY, alpha);

        final int btnW = 80;
        final int btnH = 22;
        final int confirmX = dialogX + 30;
        final int cancelX = dialogX + dialogW - btnW - 30;
        final int btnY = dialogY + 50;

        final boolean confirmHover = owner.inBounds(confirmX, btnY, btnW, btnH);
        final boolean cancelHover = owner.inBounds(cancelX, btnY, btnW, btnH);

        owner.fillGlassButton(g, confirmX, btnY, btnW, btnH, confirmHover ? 0x66486880 : 0x44384858, alpha);
        owner.drawGlassText(g, "\u786E\u8BA4", confirmX + (btnW - font.width("\u786E\u8BA4")) / 2, btnY + 6,
            confirmHover ? owner.TEXT_GREEN : owner.TEXT_PRIMARY, alpha);

        owner.fillGlassButton(g, cancelX, btnY, btnW, btnH, cancelHover ? 0x66684848 : 0x44584838, alpha);
        owner.drawGlassText(g, "\u53D6\u6D88", cancelX + (btnW - font.width("\u53D6\u6D88")) / 2, btnY + 6,
            cancelHover ? 0xCCB88888 : owner.TEXT_PRIMARY, alpha);
    }

    /**
     * 渲染放置模式提示（阶段1：拖拽确定位置）。
     */
    private void renderPlacementOverlay(final GuiGraphics g, final int alpha)
    {
        final int barH = 24;
        owner.fillGlassPanel(g, 0, 0, width, barH, owner.GLASS_HEADER, alpha);

        final String hint = "\u5DE6\u952E\u62D6\u62FD\u786E\u5B9A\u653E\u7F6E\u4F4D\u7F6E | \u53F3\u952E/Esc \u53D6\u6D88";
        owner.drawGlassText(g, hint, width / 2 - font.width(hint) / 2, 7, owner.TEXT_SOFT_GOLD, alpha);

        if (placementTargetPos != null)
        {
            final String posStr = "\u653E\u7F6E\u4F4D\u7F6E: " + placementTargetPos.toShortString();
            owner.drawGlassText(g, posStr, 8, height - 20, owner.TEXT_GREEN, alpha);
        }
    }

    // ════════════════════════════════════════════════════════
    //  输入事件（由 MayorBookScreen 委托）
    // ════════════════════════════════════════════════════════

    /**
     * 处理鼠标点击。返回 true 表示事件已消费。
     */
    public boolean mouseClicked(final double mx, final double my, final int button)
    {
        if (!catalogMode) return false;

        if (placementConfirm)
        {
            return handlePlacementConfirmClick(mx, my, button);
        }

        if (!placementMode)
        {
            return handleCatalogPanelClick(mx, my, button);
        }

        return false;
    }

    /**
     * 处理鼠标滚轮。返回 true 表示事件已消费。
     */
    public boolean mouseScrolled(final double mx, final double my, final double delta)
    {
        if (!catalogMode) return false;

        // 根据视图模式计算滚动步长
        final int scrollStep = switch (viewMode)
        {
            case LARGE_ICONS -> 80 + GRID_CELL_GAP;
            case SMALL_ICONS -> 40 + GRID_CELL_GAP;
            case LIST -> 20 + 2;
        };
        catalogScrollOffset = Math.max(0, catalogScrollOffset - (int) (delta * scrollStep));
        return true;
    }

    /**
     * 处理放置确认弹窗中的点击。
     */
    private boolean handlePlacementConfirmClick(final double mx, final double my, final int button)
    {
        final int dialogW = 260;
        final int dialogH = 80;
        final int dialogX = (width - dialogW) / 2;
        final int dialogY = (height - dialogH) / 2;

        final int btnW = 80;
        final int btnH = 22;
        final int confirmX = dialogX + 30;
        final int cancelX = dialogX + dialogW - btnW - 30;
        final int btnY = dialogY + 50;

        // 确认
        if (owner.inBounds(confirmX, btnY, btnW, btnH))
        {
            doPlaceBlueprint();
            return true;
        }
        // 取消
        if (owner.inBounds(cancelX, btnY, btnW, btnH))
        {
            placementConfirm = false;
            placementMode = true;
            return true;
        }

        return false;
    }

    /**
     * 处理图鉴面板内的点击。
     */
    private boolean handleCatalogPanelClick(final double mx, final double my, final int button)
    {
        int panelW = width * CATALOG_PANEL_W_RATIO_NUM / CATALOG_PANEL_W_RATIO_DEN;
        panelW = Math.min(panelW, width - 80);
        final int panelH = height * CATALOG_PANEL_H_RATIO_NUM / CATALOG_PANEL_H_RATIO_DEN;
        final int panelX = (width - panelW) / 2;
        final int panelY = (height - panelH) / 2;

        // 视图模式切换按钮
        int modeBtnX = panelX + panelW - 90;
        for (final ViewMode mode : ViewMode.values())
        {
            if (owner.inBounds(modeBtnX, panelY + 3, 14, 14))
            {
                viewMode = mode;
                catalogScrollOffset = 0;
                return true;
            }
            modeBtnX += 18;
        }

        // 刷新按钮
        final int refreshX = panelX + panelW - 36;
        if (owner.inBounds(refreshX, panelY + 3, 14, 14))
        {
            refresh();
            return true;
        }

        // 关闭按钮
        final int closeX = panelX + panelW - 18;
        if (owner.inBounds(closeX, panelY + 3, 14, 14))
        {
            close();
            return true;
        }

        // 左侧文件夹树点击
        final int leftX = panelX + 4;
        final int leftY = panelY + 26;
        final int leftW = CATALOG_LEFT_W - 8;
        final int leftH = panelH - 30 - CATALOG_BOTTOM_H - 4;
        if (mx >= leftX && mx <= leftX + leftW && my >= leftY && my <= leftY + leftH)
        {
            final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
            int itemY = leftY + 4 - folderScrollOffset;

            // 检查"全部"选项
            if (owner.inBounds(leftX + 2, itemY, leftW - 4, CATALOG_ITEM_H))
            {
                selectedPackName = null;
                selectedSubPath = null;
                catalogSelectedIndex = -1;
                catalogScrollOffset = 0;
                return true;
            }
            itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;

            // 检查风格包列表
            for (final var pack : packs)
            {
                if (owner.inBounds(leftX + 2, itemY, leftW - 4, CATALOG_ITEM_H))
                {
                    final String packName = pack.getName();
                    if (packName.equals(selectedPackName))
                    {
                        // 点击已选中的包 → 取消选中
                        selectedPackName = null;
                        selectedSubPath = null;
                    }
                    else
                    {
                        // 点击未选中的包 → 选中
                        selectedPackName = packName;
                        selectedSubPath = null;
                    }
                    catalogSelectedIndex = -1;
                    catalogScrollOffset = 0;
                    return true;
                }
                itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;

                // 如果选中了这个包，检查子目录
                if (pack.getName().equals(selectedPackName))
                {
                    final var categories = com.metrogenesis.structurize.storage.StructurePacks.getCategories(pack.getName(), "");
                    for (final var cat : categories)
                    {
                        if (owner.inBounds(leftX + 2, itemY, leftW - 4, CATALOG_ITEM_H))
                        {
                            selectedSubPath = cat.subPath;
                            catalogSelectedIndex = -1;
                            catalogScrollOffset = 0;
                            return true;
                        }
                        itemY += CATALOG_ITEM_H + CATALOG_ITEM_GAP;
                    }
                }
            }
            return true;
        }

        // 右侧蓝图列表/网格点击
        final int rightX = panelX + CATALOG_LEFT_W + 4;
        final int rightY = leftY;
        final int rightW = panelW - CATALOG_LEFT_W - 12;
        final int gridH = leftH / 2;

        if (mx >= rightX && mx <= rightX + rightW && my >= rightY && my <= rightY + gridH)
        {
            String selectedCat = CategoryMapper.getCategoryByIndex(catalogCategoryIndex);
            final List<BuildingCatalogEntry> entries = getEntriesForCategory(selectedCat);

            final int gridStartY = rightY + 20;

            // 根据视图模式计算点击位置
            switch (viewMode)
            {
                case LARGE_ICONS, SMALL_ICONS ->
                {
                    final int cellW = (viewMode == ViewMode.LARGE_ICONS) ? 80 : 40;
                    final int cellH = (viewMode == ViewMode.LARGE_ICONS) ? 80 : 40;
                    int availableW = rightW - 8;
                    int cols = Math.max(1, availableW / (cellW + GRID_CELL_GAP));

                    for (int i = 0; i < entries.size(); i++)
                    {
                        int row = i / cols;
                        int col = i % cols;

                        int cellX = rightX + 4 + col * (cellW + GRID_CELL_GAP);
                        int cellY = gridStartY + row * (cellH + GRID_CELL_GAP) - catalogScrollOffset;

                        if (owner.inBounds(cellX, cellY, cellW, cellH))
                        {
                            catalogSelectedIndex = i;
                            return true;
                        }
                    }
                }
                case LIST ->
                {
                    final int itemH = 20;
                    final int itemGap = 2;

                    for (int i = 0; i < entries.size(); i++)
                    {
                        int itemY = gridStartY + i * (itemH + itemGap) - catalogScrollOffset;

                        if (owner.inBounds(rightX + 4, itemY, rightW - 8, itemH))
                        {
                            catalogSelectedIndex = i;
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        // 底部操作栏 — 放置按钮
        final int bottomY = panelY + panelH - CATALOG_BOTTOM_H - 2;
        final int placeBtnW = 60;
        final int placeBtnX = panelX + panelW - placeBtnW - 16;
        if (owner.inBounds(placeBtnX, bottomY + 3, placeBtnW, 20))
        {
            if (getSelectedCatalogEntry() != null)
            {
                startPlacement();
            }
            return true;
        }

        return false;
    }

    // ════════════════════════════════════════════════════════
    //  放置流程
    // ════════════════════════════════════════════════════════

    /**
     * 进入放置模式（阶段1：拖拽确定位置）。
     */
    public void startPlacement()
    {
        placementMode = true;
        placementConfirm = false;
        placementTargetPos = null;
        placementDragging = false;
    }

    /**
     * 从放置模式回到图鉴面板。
     */
    public void cancelPlacement()
    {
        placementMode = false;
        placementConfirm = false;
        placementTargetPos = null;
        placementDragging = false;
    }

    /**
     * 进入放置确认（阶段2）。
     */
    public void confirmPlacementPosition(final BlockPos pos)
    {
        placementTargetPos = pos;
        placementConfirm = true;
    }

    /**
     * 执行蓝图放置。
     * 使用 BuildingCatalogEntry 的 packName + resourcePath 通过 StructurePacks 加载蓝图。
     */
    private void doPlaceBlueprint()
    {
        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry == null || placementTargetPos == null)
        {
            LOGGER.warn("Cannot place: no entry or no target position");
            close();
            return;
        }

        LOGGER.info("Placing blueprint '{}' at {}", entry.name(), placementTargetPos);

        // 使用 StructurePacks 加载蓝图并放置
        // 注意：这里需要通过网络消息发送到服务端执行
        NetworkHandler.CHANNEL.sendToServer(
            new BlueprintPlacementMessage(entry.name(), placementTargetPos, RotationMirror.NONE)
        );
        close();
    }

    // ════════════════════════════════════════════════════════
    //  数据查询（使用 BuildingCatalogScanner）
    // ════════════════════════════════════════════════════════

    /**
     * 获取当前选中文件夹下的图鉴条目。
     * 根据 selectedPackName 和 selectedSubPath 过滤。
     */
    private List<BuildingCatalogEntry> getEntriesForCategory(final String category)
    {
        // 如果选中了特定文件夹，按文件夹过滤
        if (selectedPackName != null)
        {
            if (selectedSubPath != null)
            {
                // 选中了子目录：按 pack + subPath 过滤
                return allEntries.stream()
                    .filter(e -> e.packName().equals(selectedPackName) && e.mcCategory().equals(selectedSubPath))
                    .collect(Collectors.toList());
            }
            else
            {
                // 选中了风格包：按 pack 过滤
                return allEntries.stream()
                    .filter(e -> e.packName().equals(selectedPackName))
                    .collect(Collectors.toList());
            }
        }

        // 否则使用分类过滤（兼容旧逻辑）
        if (CategoryMapper.isAllCategory(category))
        {
            return allEntries;
        }
        return allEntries.stream()
            .filter(e -> e.category().equals(category))
            .collect(Collectors.toList());
    }

    /**
     * 加载蓝图用于预览（按需加载，使用 StructurePacks）。
     * resourcePath 格式: "agriculture/horticulture/farmer"（不含 .blueprint 后缀）
     */
    @Nullable
    private Blueprint loadBlueprintForPreview(final BuildingCatalogEntry entry)
    {
        try
        {
            // resourcePath 是相对于包根目录的路径（不含 .blueprint 后缀）
            // StructurePacks.getBlueprint(packName, relativePath) 需要完整的相对路径
            final String relativePath = entry.resourcePath() + ".blueprint";
            return StructurePacks.getBlueprint(entry.packName(), relativePath, true);
        }
        catch (Exception e)
        {
            LOGGER.debug("[Catalog] Failed to load blueprint '{}': {}", entry.name(), e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前选中的图鉴条目。
     */
    @Nullable
    private BuildingCatalogEntry getSelectedCatalogEntry()
    {
        if (catalogSelectedIndex < 0) return null;
        String selectedCat = CategoryMapper.getCategoryByIndex(catalogCategoryIndex);
        final List<BuildingCatalogEntry> entries = getEntriesForCategory(selectedCat);
        if (catalogSelectedIndex >= 0 && catalogSelectedIndex < entries.size())
        {
            return entries.get(catalogSelectedIndex);
        }
        return null;
    }
}
