package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.catalog.BuildingCatalogScanner;
import com.metrogenesis.catalog.BuildingDescriptionProvider;
import com.metrogenesis.catalog.CategoryMapper;
import com.metrogenesis.gui.catalog.PreviewCache;
import com.metrogenesis.network.BlueprintPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.storage.StructurePackMeta;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.structurize.storage.rendering.RenderingCache;
import com.metrogenesis.structurize.storage.rendering.types.BlueprintPreviewData;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import net.minecraft.locale.Language;

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

    /** 是否在风格包选择首页（true=显示包网格，false=显示蓝图列表） */
    private boolean packSelectionMode = true;

    // 放置二次确认（两阶段流程）
    private boolean placementMode = false;     // 阶段1：RTS 视角拖拽确定位置
    private boolean placementDragging = false; // 正在拖拽确定放置位置
    private boolean placementConfirm = false;  // 阶段2：显示确认弹窗
    private BlockPos placementTargetPos = null; // 拖拽确定的放置位置

    // 放置参数调整（位置微调 + 旋转）
    private int placementNudgeX = 0;             // X 方向微调（正=东）
    private int placementNudgeZ = 0;             // Z 方向微调（正=南）
    private int placementNudgeY = 0;             // Y 高度微调（正=上）
    private int placementRotIdx = 0;             // 旋转索引（0=NONE, 1=CW90, 2=CW180, 3=CCW90）

    private static final RotationMirror[] PLACEMENT_ROTATIONS = RotationMirror.NOT_MIRRORED;

    private static final String[] PLACEMENT_ROT_NAMES = {
        "0°", "90°", "180°", "270°"
    };

    /** 放置模式下的世界 3D 预览 key（用于 RenderingCache） */
    private static final String PLACEMENT_PREVIEW_KEY = "metrogenesis_placement";
    /** 缓存当前选中的蓝图，用于预览 */
    @Nullable
    private Blueprint cachedPreviewBp = null;
    /** RTS 摄像机偏航角（用于相对方向移动） */
    private float cameraYaw = 0f;

    /** 放置模式下是否处于微调模式（false=摄像机移动模式，true=建筑微调模式） */
    private boolean placementNudgeMode = true;

    /** 由 MayorBookScreen 每帧设置当前摄像机偏航角。 */
    public void setCameraYaw(final float yaw) { this.cameraYaw = yaw; }

    /** 是否有待恢复的放置状态 */
    private boolean hasPendingPlacement = false;

    // ════════════════════════════════════════════════════════
    //  图鉴编辑状态
    // ════════════════════════════════════════════════════════

    private enum EditAction {
        NONE,
        NEW_FOLDER,
        RENAME_FOLDER,
        RENAME_FILE,
        DELETE_CONFIRM
    }

    /** 当前编辑动作 */
    private EditAction editAction = EditAction.NONE;
    /** 编辑对话框中的输入文本 */
    private String editInputText = "";
    /** 编辑操作的目标路径（pack + relative dir/file name） */
    private String editTargetPack = null;
    private String editTargetPath = null;
    /** 是否为文件夹操作 */
    private boolean editTargetIsFolder = false;
    /** 光标闪烁计时 */
    private long editCursorTime = 0;
    private boolean editCursorVisible = true;

    // ════════════════════════════════════════════════════════
    //  右侧 3D 预览 — 平移/缩放状态
    // ════════════════════════════════════════════════════════

    /** 3D 预览水平平移偏移 */
    private float previewPanX = 0f;
    /** 3D 预览垂直平移偏移 */
    private float previewPanY = 0f;
    /** 3D 预览缩放偏移 */
    private float previewZoom = 1f;
    /** 3D 预览水平旋转角度（绕Y轴） */
    private float previewRotY = 45f;
    /** 3D 预览垂直旋转角度（绕X轴） */
    private float previewRotX = 35f;
    /** 上次用户交互预览的时间戳（毫秒），用于自动旋转暂停判定 */
    private long lastPreviewInteraction = 0L;
    /** 正在拖拽平移预览 */
    private boolean previewDragging = false;
    /** 正在拖拽旋转预览 */
    private boolean previewRotating = false;
    /** 拖拽起点 */
    private double previewDragStartX, previewDragStartY;

    // ════════════════════════════════════════════════════════
    //  扫描器状态
    // ════════════════════════════════════════════════════════

    private final BuildingCatalogScanner scanner = new BuildingCatalogScanner();
    private Future<List<BuildingCatalogEntry>> scanFuture = null;
    private List<BuildingCatalogEntry> allEntries = List.of();
    private boolean scanStarted = false;

    /** 风格包选择页面的滚动偏移 */
    private int packSelectionScrollOffset = 0;
    /** 风格包选择页面的最大滚动范围 */
    private int packSelectionMaxScroll = 0;

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
    /** 右侧 3D 预览面板宽度 */
    private static final int CATALOG_PREVIEW_W = 220;
    private static final int CATALOG_ITEM_H = 20;
    private static final int CATALOG_ITEM_GAP = 2;

    /** 风格包选择卡的尺寸 */
    private static final int PACK_CARD_W = 200;
    private static final int PACK_CARD_H = 170;
    private static final int PACK_CARD_GAP = 12;

    /** 风格包封面图数据 */
    private record PackIconData(DynamicTexture texture, int width, int height) {}

    /** 风格包封面图缓存（packName → PackIconData） */
    private static final Map<String, PackIconData> PACK_ICON_CACHE = new HashMap<>();
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
    public boolean isPlacementNudgeMode() { return placementNudgeMode; }
    public void togglePlacementNudgeMode() { placementNudgeMode = !placementNudgeMode; }
    public boolean isEditActive() { return editAction != EditAction.NONE; }
    public BlockPos getPlacementTargetPos() { return placementTargetPos; }

    public void setPlacementDragging(final boolean dragging) { this.placementDragging = dragging; }
    public void setPlacementTargetPos(final BlockPos pos) {
        this.placementTargetPos = snapSeamlessPlacement(pos);
        updateGhostPreview();
    }

    /**
     * 对无缝建筑进行对齐吸附。
     * 根据 SeamlessType 和邻近道路，自动调整放置位置。
     */
    private BlockPos snapSeamlessPlacement(final BlockPos pos) {
        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry == null) return pos;

        // 从 mcCategory 判断是否为无缝建筑
        if (!"seamless".equals(entry.mcCategory())) return pos;

        final var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return pos;

        // 从 resourcePath 反推子目录名推断 SeamlessType
        // resourcePath 格式: "seamless/left/small_house" → parts[1] = "left"
        final String[] parts = entry.resourcePath().split("/");
        if (parts.length < 2) return pos;
        final String typeDir = parts[1];

        // 根据类型选择对齐方向
        // left = 左(X-)方向对齐, right = 右(X+)方向对齐
        // both = 左右都找, inner/outer = 转角
        final boolean alignLeft = typeDir.equals("left") || typeDir.equals("both") || typeDir.equals("inner") || typeDir.equals("outer");
        final boolean alignRight = typeDir.equals("right") || typeDir.equals("both") || typeDir.equals("inner") || typeDir.equals("outer");

        // 在 pos 周围搜索道路方块，如果有则对齐到最近的道路边缘
        int px = pos.getX() & ~3;  // 按 4 格对齐（道路网格通常以 4 为单位）
        int pz = pos.getZ() & ~3;
        return new BlockPos(px, pos.getY(), pz);
    }

    // ════════════════════════════════════════════════════════
    //  放置参数调整
    // ════════════════════════════════════════════════════════

    /** 获取经过微调后的最终放置位置。 */
    public BlockPos getAdjustedPlacementPos()
    {
        if (placementTargetPos == null) return null;
        return placementTargetPos.offset(placementNudgeX, placementNudgeY, placementNudgeZ);
    }

    /** 获取当前旋转/镜像状态。 */
    public RotationMirror getPlacementRotation()
    {
        return PLACEMENT_ROTATIONS[placementRotIdx];
    }

    /** 微调 X（正=东，负=西）。 */
    public void nudgeX(final int delta) { placementNudgeX += delta; updateGhostPreview(); }

    /** 微调 Z（正=南，负=北）。 */
    public void nudgeZ(final int delta) { placementNudgeZ += delta; updateGhostPreview(); }

    /** 微调 Y（正=上，负=下）。 */
    public void nudgeY(final int delta) { placementNudgeY += delta; updateGhostPreview(); }

    /** 旋转（正=顺时针，负=逆时针）。 */
    public void rotate(final int direction)
    {
        placementRotIdx = (placementRotIdx + direction + PLACEMENT_ROTATIONS.length) % PLACEMENT_ROTATIONS.length;
        updateGhostPreview();
    }

    /** 重置所有位置调整参数。 */
    public void resetPlacementAdjustments()
    {
        placementNudgeX = 0;
        placementNudgeZ = 0;
        placementNudgeY = 0;
        placementRotIdx = 0;
    }

    // ════════════════════════════════════════════════════════
    //  世界 3D 预览（Ghost Preview）
    // ════════════════════════════════════════════════════════

    /**
     * 创建或更新放置模式下的 3D 世界预览。
     * 将当前选中的蓝图以半透明 ghost 形式渲染在目标位置。
     */
    private void updateGhostPreview()
    {
        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry == null || placementTargetPos == null) return;

        // 异步加载蓝图
        if (cachedPreviewBp == null)
        {
            cachedPreviewBp = loadBlueprintForPreview(entry);
        }

        final Blueprint bp = cachedPreviewBp;
        if (bp == null) return;

        final BlockPos finalPos = getAdjustedPlacementPos();
        final RotationMirror rot = getPlacementRotation();

        // 创建预览数据（禁用服务端同步，仅本地预览）
        final BlueprintPreviewData previewData = new BlueprintPreviewData(false);
        previewData.setPos(finalPos);
        previewData.setBlueprint(bp);
        previewData.setRotationMirror(rot);
        // 设置半透明效果（25% 透明度）
        previewData.setOverridePreviewTransparency(0.25f);
        previewData.setRenderBlocksNice(true);
        RenderingCache.queue(PLACEMENT_PREVIEW_KEY, previewData);
    }

    /** 移除放置模式下的 3D 世界预览。 */
    private void removeGhostPreview()
    {
        RenderingCache.removeBlueprint(PLACEMENT_PREVIEW_KEY);
        cachedPreviewBp = null;
    }

    /**
     * 打开图鉴 — 进入图鉴模式。
     * 首次打开时触发异步扫描，之后使用缓存。
     * 如果有未完成的放置状态，自动恢复 Ghost Preview。
     */
    public void open()
    {
        catalogMode = true;
        catalogCategoryIndex = -1;
        catalogScrollOffset = 0;
        folderScrollOffset = 0;
        packSelectionScrollOffset = 0;

        // 恢复未完成的放置状态
        if (hasPendingPlacement)
        {
            placementMode = true;
            placementDragging = false;
            updateGhostPreview();
        }
        else
        {
            catalogSelectedIndex = -1;
            selectedPackName = null;
            selectedSubPath = null;
            placementMode = false;
            placementConfirm = false;
            placementTargetPos = null;
        }

        // 检查是否已有选择的风格包，如果是则直接进入蓝图浏览模式
        final String activePack = owner.getActiveStylePack();
        if (activePack != null && !activePack.isEmpty())
        {
            packSelectionMode = false;
            selectedPackName = activePack;
            MetroGenesis.LOGGER.info("[Catalog] Using persisted style pack: {}", activePack);
        }
        else
        {
            packSelectionMode = true;  // 无已选风格包时显示选择首页
        }

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
     * 保留放置相关状态以便重新打开时恢复。
     */
    public void close()
    {
        catalogMode = false;
        hasPendingPlacement = (placementMode && placementTargetPos != null);
        if (hasPendingPlacement)
        {
            removeGhostPreview();
        }
        else
        {
            placementMode = false;
            placementConfirm = false;
            placementTargetPos = null;
            resetPlacementAdjustments();
            removeGhostPreview();
        }
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

        if (packSelectionMode)
        {
            renderPackSelection(g, alpha);
            return;
        }

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

        // 编辑对话框（渲染在最上层）
        if (editAction != EditAction.NONE)
        {
            renderEditDialog(g, alpha);
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

    // ════════════════════════════════════════════════════════
    //  裁剪辅助（防止列表/网格内容溢出到相邻面板）
    // ════════════════════════════════════════════════════════

    /**
     * 在 GUI 像素坐标上启用裁剪测试。
     * 防止内容渲染到指定矩形之外（如网格溢出到预览区）。
     */
    private static void enableScissor(final int x, final int y, final int w, final int h)
    {
        if (w <= 0 || h <= 0) return;
        final Window window = Minecraft.getInstance().getWindow();
        final double scale = window.getGuiScale();
        final int scX = (int) (x * scale);
        final int scY = (int) (window.getHeight() - (y + h) * scale);
        final int scW = Math.max(1, (int) (w * scale));
        final int scH = Math.max(1, (int) (h * scale));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    /**
     * 禁用裁剪测试。
     */
    private static void disableScissor()
    {
        RenderSystem.disableScissor();
    }

    /**
     * 渲染风格包选择首页。
     * 参考 Structurize 的 StructurePacks 选择界面风格：
     * 网格布局，每个包显示预览图、名称、描述、作者和 Select 按钮。
     * 先检查加载状态，未完成时显示加载中。
     */
    private void renderPackSelection(final GuiGraphics g, final int alpha)
    {
        // 较大的面板尺寸（接近全屏，留边距）
        final int pw = Math.min(width - 40, width);
        final int ph = Math.min(height - 40, height);
        final int px = (width - pw) / 2;
        final int py = (height - ph) / 2;

        // 背景
        owner.fillGlassPanel(g, px, py, pw, ph, owner.GLASS_BG, alpha);
        // v1.2 金色粒子点缀（内容区，低 alpha）
        MGStyles.drawParticles(g, px, py + 22, pw, ph - 22, System.currentTimeMillis(), alpha / 2);

        // 标题栏
        owner.fillGlassPanel(g, px, py, pw, 22, owner.GLASS_HEADER, alpha);
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.001"), px + 8, py + 6, owner.TEXT_SOFT_GOLD, alpha);

        // ── 检查结构包是否加载完成 ──
        if (!com.metrogenesis.structurize.storage.StructurePacks.waitUntilFinishedLoading())
        {
            final String loadingMsg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.002");
            owner.drawGlassText(g, loadingMsg, px + pw / 2 - font.width(loadingMsg) / 2,
                py + ph / 2 - 4, owner.TEXT_SOFT_GOLD, alpha);
            return;
        }

        // ── 检查图鉴扫描是否完成 ──
        final boolean scanDone = scanFuture == null || scanFuture.isDone();
        if (!scanDone)
        {
            final String scanningMsg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.003");
            owner.drawGlassText(g, scanningMsg, px + pw / 2 - font.width(scanningMsg) / 2,
                py + ph / 2 - 16, owner.TEXT_SOFT_GOLD, alpha);
            
            // 进度条
            float progress = scanner.getProgress();
            int barW = pw - 80;
            int barX = px + 40;
            int barY = py + ph / 2 + 4;
            int barH = 8;
            owner.fillGlassPanel(g, barX, barY, barW, barH, owner.GLASS_PANEL, alpha);
            g.fill(barX + 1, barY + 1, barX + 1 + (int)((barW - 2) * progress), barY + barH - 1,
                owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
            
            // 进度百分比
            String percentText = String.format("%.0f%%", progress * 100);
            owner.drawGlassText(g, percentText, px + pw / 2 - font.width(percentText) / 2,
                barY + barH + 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // ── 获取已加载的风格包 ──
        final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
        if (packs.isEmpty())
        {
            final String msg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.004");
            owner.drawGlassText(g, msg, px + pw / 2 - font.width(msg) / 2, py + ph / 2, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 网格参数
        final int cols = Math.max(1, (pw - 40) / (PACK_CARD_W + PACK_CARD_GAP));
        final int gridStartX = px + (pw - cols * PACK_CARD_W - (cols - 1) * PACK_CARD_GAP) / 2;
        final int gridStartY = py + 36;

        // 计算滚动范围
        final int totalRows = (packs.size() + cols - 1) / cols;
        final int totalGridH = totalRows * (PACK_CARD_H + PACK_CARD_GAP) - PACK_CARD_GAP;
        final int visibleH = py + ph - 10 - gridStartY;
        packSelectionMaxScroll = Math.max(0, totalGridH - visibleH);
        packSelectionScrollOffset = Math.max(0, Math.min(packSelectionMaxScroll, packSelectionScrollOffset));

        // 裁剪区域
        enableScissor(px + 2, gridStartY, pw - 4, ph - 36 - 10);
        try
        {
            int col = 0;
            int row = 0;
            int rendered = 0;

            for (final var pack : packs)
            {
                final int cx = gridStartX + col * (PACK_CARD_W + PACK_CARD_GAP);
                final int cy = gridStartY + row * (PACK_CARD_H + PACK_CARD_GAP) - packSelectionScrollOffset;

                // 跳过完全不可见的卡片
                if (cy + PACK_CARD_H < gridStartY || cy > gridStartY + visibleH)
                {
                    col++;
                    if (col >= cols) { col = 0; row++; }
                    continue;
                }

                renderPackCard(g, pack, cx, cy, PACK_CARD_W, PACK_CARD_H, alpha);
                rendered++;

                col++;
                if (col >= cols) { col = 0; row++; }
            }
        }
        finally
        {
            disableScissor();
        }
    }

    /**
     * 渲染单个风格包卡片（带封面预览图）。
     * ┌──────────────────────┐
     * │    [封面预览图]      │
     * │    (缩放填满)        │
     * ├──────────────────────┤
     * │ 风格包名称            │
     * │ 描述文字(一行)       │
     * │ 作者: xxx            │
     * │ [Select 按钮]       │
     * └──────────────────────┘
     */
    private void renderPackCard(final GuiGraphics g, final StructurePackMeta pack,
                                 final int x, final int y, final int w, final int h, final int alpha)
    {
        // 卡片背景
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);

        // ── 封面预览图区域 ──
        final int prevH = 90;
        g.fill(x + 2, y + 2, x + w - 2, y + prevH, owner.withAlpha(0x1A1A1A, alpha)); // 凹陷预览槽 = C_BG 深色

        // 尝试加载并显示封面图（保持宽高比居中显示）
        PackIconData iconData = PACK_ICON_CACHE.get(pack.getName());
        if (iconData == null)
        {
            final DynamicTexture loadedTex = loadPackIcon(pack);
            if (loadedTex != null)
            {
                final NativeImage pixels = loadedTex.getPixels();
                final int iw = (pixels != null) ? pixels.getWidth() : 128;
                final int ih = (pixels != null) ? pixels.getHeight() : 128;
                iconData = new PackIconData(loadedTex, iw, ih);
                PACK_ICON_CACHE.put(pack.getName(), iconData);
            }
        }
        if (iconData != null)
        {
            // 计算居中、保持宽高比的实际绘制区域
            final int imgW = iconData.width();
            final int imgH = iconData.height();
            final int drawW = w - 4;
            final int drawH = prevH - 4;
            final float scale = Math.min((float) drawW / imgW, (float) drawH / imgH);
            final int outW = (int) (imgW * scale);
            final int outH = (int) (imgH * scale);
            final int outX = x + 2 + (drawW - outW) / 2;
            final int outY = y + 2 + (drawH - outH) / 2;

            RenderSystem.setShaderTexture(0, iconData.texture().getId());
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            final var mat = g.pose().last().pose();
            final var buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buf.vertex(mat, outX, outY + outH, 0).uv(0, 1).endVertex();
            buf.vertex(mat, outX + outW, outY + outH, 0).uv(1, 1).endVertex();
            buf.vertex(mat, outX + outW, outY, 0).uv(1, 0).endVertex();
            buf.vertex(mat, outX, outY, 0).uv(0, 0).endVertex();
            BufferUploader.drawWithShader(buf.end());
        }
        else
        {
            // 无封面图时显示包名首字母占位
            final String firstChar = pack.getName().isEmpty() ? "?" : pack.getName().substring(0, 1).toUpperCase();
            owner.drawGlassText(g, firstChar, x + w / 2 - font.width(firstChar) / 2,
                y + prevH / 2 - font.lineHeight / 2, owner.withAlpha(MGStyles.C_TEXT_BRASS, alpha), alpha);
        }

        // ── 名称 ──
        String name = pack.getName();
        if (font.width(name) > w - 12) name = font.plainSubstrByWidth(name, w - 12);
        owner.drawGlassText(g, name, x + 6, y + prevH + 6, owner.TEXT_SOFT_GOLD, alpha);

        // ── 描述（单行裁剪） ──
        String desc = pack.getDesc();
        if (desc == null || desc.isEmpty()) desc = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.005");
        if (font.width(desc) > w - 12) desc = font.plainSubstrByWidth(desc, w - 12) + "..";
        owner.drawGlassText(g, desc, x + 6, y + prevH + 18, owner.TEXT_SECONDARY, alpha);

        // ── 作者 ──
        String authorText = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.006");
        final var authors = pack.getAuthors();
        if (authors != null && !authors.isEmpty())
        {
            authorText += String.join(", ", authors);
        }
        else
        {
            authorText += pack.getOwner() != null ? pack.getOwner() : Language.getInstance().getOrDefault("gui.metrogenesis.catalog.007");
        }
        if (font.width(authorText) > w - 12) authorText = font.plainSubstrByWidth(authorText, w - 12) + "..";
        owner.drawGlassText(g, authorText, x + 6, y + prevH + 30, owner.withAlpha(MGStyles.C_TEXT_2ND, alpha), alpha);

        // ── Select 按钮 ──
        final int btnW = 70;
        final int btnH = 18;
        final int btnX = x + w - btnW - 6;
        final int btnY = y + h - btnH - 4;
        final boolean btnHover = owner.inBounds(btnX, btnY, btnW, btnH);
        // §7.1 强调按钮：工业橙底 + 白色文字（悬停提亮）
        final int btnBg = btnHover ? MGStyles.C_ACCENT_LT : MGStyles.C_ACCENT;
        owner.fillGlassButton(g, btnX, btnY, btnW, btnH, btnBg, alpha);
        owner.drawGlassText(g, "Select", btnX + (btnW - font.width("Select")) / 2, btnY + 4,
            owner.TEXT_PRIMARY, alpha);
    }

    /**
     * 从风格包目录加载封面图 PNG。
     * pack.json 的 "icon" 字段定义了封面图文件名（如 "acacia.png"）。
     */
    @Nullable
    private static DynamicTexture loadPackIcon(final StructurePackMeta pack)
    {
        if (pack.getIconPath() == null || pack.getIconPath().isEmpty()) return null;
        try
        {
            final Path iconPath = pack.getPath().resolve(pack.getIconPath());
            if (!Files.exists(iconPath)) return null;
            try (final InputStream is = Files.newInputStream(iconPath))
            {
                final NativeImage image = NativeImage.read(is);
                if (image == null) return null;
                final DynamicTexture tex = new DynamicTexture(image);
                tex.upload();
                return tex;
            }
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    /**
     * 渲染图鉴主面板（风格包已选择后的蓝图浏览模式）。
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
        // v1.2 金色粒子点缀（内容区，低 alpha，不遮挡文字）
        MGStyles.drawParticles(g, panelX, panelY + 26, panelW - 8, panelH - 30 - CATALOG_BOTTOM_H, System.currentTimeMillis(), alpha / 3);

        // 标题栏
        owner.fillGlassPanel(g, panelX, panelY, panelW, 22, owner.GLASS_HEADER, alpha);
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.008"), panelX + 8, panelY + 6, owner.TEXT_SOFT_GOLD, alpha);

        // 返回风格包首页按钮
        final String backLabel = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.009");
        final boolean backHover = owner.inBounds(panelX + 80, panelY + 3, 50, 16);
        owner.fillGlassButton(g, panelX + 80, panelY + 3, 50, 16, backHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, backLabel, panelX + 84, panelY + 5, owner.TEXT_SECONDARY, alpha);

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

        // ── 文件夹编辑工具栏 ──
        renderEditToolbar(g, panelX, leftY + leftH + 2, CATALOG_LEFT_W, alpha);

        // 分隔线
        g.fill(panelX + CATALOG_LEFT_W, leftY, panelX + CATALOG_LEFT_W + 1, leftY + leftH, owner.withAlpha(owner.EDGE_INNER, alpha));

        // ── 中间：蓝图网格列表 ──
        int midX = panelX + CATALOG_LEFT_W + 4;
        int midY = leftY;
        int midW = panelW - CATALOG_LEFT_W - CATALOG_PREVIEW_W - 16;
        renderCatalogBlueprintList(g, midX, midY, midW, leftH, alpha);

        // 右侧分隔线
        int previewX = midX + midW + 4;
        g.fill(previewX, leftY, previewX + 1, leftY + leftH, owner.withAlpha(owner.EDGE_INNER, alpha));

        // ── 右侧：3D 预览 ──
        int previewW = CATALOG_PREVIEW_W - 8;
        renderCatalogPreview(g, previewX + 4, midY, previewW, leftH, alpha);

        // ── 底部操作栏 ──
        int bottomY = panelY + panelH - CATALOG_BOTTOM_H - 2;
        g.fill(panelX + 4, bottomY, panelX + panelW - 4, bottomY + 1, owner.withAlpha(owner.EDGE_INNER, alpha));
        renderCatalogBottomBar(g, panelX, bottomY, panelW, alpha);
    }

    // ════════════════════════════════════════════════════════
    //  城市需求面板
    // ════════════════════════════════════════════════════════

    /**
     * 渲染城市需求面板。
     * 显示人口、财政、建设进度、经济供需等城市核心指标。
     */
    private void renderDemandsPanel(final GuiGraphics g, final int panelX, final int panelY,
                                     final int panelW, final int panelH, final int alpha)
    {
        final int contentX = panelX + 12;
        final int contentY = panelY + 30;
        final int contentW = panelW - 24;
        final int colW = contentW / 3 - 8;

        int topY = contentY;

        // ══ 刷新提示 ═══════════════════════════════
        final String refreshHint = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.010");
        owner.drawGlassText(g, refreshHint, panelX + panelW - font.width(refreshHint) - 16, panelY + 6,
            owner.TEXT_SECONDARY, alpha);

        // ── 获取城市数据 ──
        final var data = com.metrogenesis.network.SyncCityDataMessage.getCachedData();

        // ── 没有数据时显示提示 ──
        if (data == null)
        {
            final String waitMsg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.011");
            owner.drawGlassText(g, waitMsg, panelX + panelW / 2 - font.width(waitMsg) / 2,
                panelY + panelH / 2 - 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // ══ 第一行：核心指标卡片 ════════════════════
        renderDemandCard(g, contentX, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.012"),
            data.population + " / " + data.maxPopulation,
            data.population > 0 ? 0xFF88CC88 : 0xFFCC8888, alpha);

        renderDemandCard(g, contentX + colW + 4, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.013"),
            data.funds + " C",
            data.funds > 0 ? 0xFFCCCC88 : 0xFFCC8888, alpha);

        renderDemandCard(g, contentX + (colW + 4) * 2, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.014"),
            data.activeConstructionSites + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.015") + data.unclaimedSites + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.016"),
            0xFF88AACC, alpha);

        topY += 48;

        // ══ 功能区 / 建筑统计 ══════════════════════
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.017"), contentX, topY, owner.TEXT_SOFT_GOLD, alpha);
        topY += 14;

        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.018") + (data.hasTownHall ? Language.getInstance().getOrDefault("gui.metrogenesis.catalog.019") : Language.getInstance().getOrDefault("gui.metrogenesis.catalog.020")),
            contentX, topY, owner.TEXT_PRIMARY, alpha);
        topY += 12;
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.021") + data.zoneCount + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.022"),
            contentX, topY, owner.TEXT_PRIMARY, alpha);
        topY += 12;
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.023") + data.placedBuildingCount + " \u5EA7",
            contentX, topY, owner.TEXT_PRIMARY, alpha);
        topY += 14;

        // ── 分隔线 ──
        g.fill(contentX, topY, contentX + contentW, topY + 1, owner.withAlpha(owner.EDGE_INNER, alpha));
        topY += 6;

        // ══ 经济供需 ════════════════════════════════
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.024"), contentX, topY, owner.TEXT_SOFT_GOLD, alpha);
        topY += 14;

        if (data.topItems.isEmpty())
        {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.025"), contentX, topY, owner.TEXT_SECONDARY, alpha);
        }
        else
        {
            // 表格头
            final int col1 = contentX;
            final int col2 = contentX + 120;
            final int col3 = contentX + 200;
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.026"), col1, topY, owner.TEXT_SECONDARY, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.027"), col2, topY, owner.TEXT_SECONDARY, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.028"), col3, topY, owner.TEXT_SECONDARY, alpha);
            topY += 12;

            for (int i = 0; i < Math.min(data.topItems.size(), 8); i++)
            {
                final var item = data.topItems.get(i);
                final boolean deficit = item.consumption > item.production;
                final int itemColor = deficit ? 0xFFCC8888 : (item.production > 0 ? 0xFF88CC88 : owner.TEXT_PRIMARY);
                owner.drawGlassText(g, item.displayName, col1, topY, itemColor, alpha);
                owner.drawGlassText(g, String.valueOf(item.consumption), col2, topY, itemColor, alpha);
                owner.drawGlassText(g, String.valueOf(item.production), col3, topY, itemColor, alpha);
                topY += 11;
            }
        }

        topY += 6;
        g.fill(contentX, topY, contentX + contentW, topY + 1, owner.withAlpha(owner.EDGE_INNER, alpha));
        topY += 6;

        // ══ 资源短缺 ════════════════════════════════
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.029"), contentX, topY, owner.TEXT_SOFT_GOLD, alpha);
        topY += 14;

        if (data.deficitItems.isEmpty())
        {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.030"),
                contentX, topY, 0xFF88CC88, alpha);
        }
        else
        {
            final int dc1 = contentX;
            final int dc2 = contentX + 120;
            final int dc3 = contentX + 200;
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.026"), dc1, topY, owner.TEXT_SECONDARY, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.031"), dc2, topY, owner.TEXT_SECONDARY, alpha);
            topY += 12;

            for (int i = 0; i < Math.min(data.deficitItems.size(), 10); i++)
            {
                final var item = data.deficitItems.get(i);
                final long gap = item.consumption - item.production;
                owner.drawGlassText(g, item.displayName, dc1, topY, 0xFFCC8888, alpha);
                owner.drawGlassText(g, "-\u00a7c" + gap, dc2, topY, 0xFFCC6666, alpha);
                topY += 11;
            }
        }
    }

    /**
     * 渲染一个数据卡片（在需求面板中使用）。
     */
    private void renderDemandCard(final GuiGraphics g, final int x, final int y,
                                   final int w, final int h, final String label,
                                   final String value, final int valueColor, final int alpha)
    {
        owner.fillGlassPanel(g, x, y, w, h, owner.GLASS_PANEL, alpha);
        owner.drawGlassText(g, label, x + 6, y + 4, owner.TEXT_SECONDARY, alpha);
        owner.drawGlassText(g, value, x + 6, y + 18, valueColor, alpha);
    }

    /**
     * 渲染扫描中状态。
     */
    private void renderScanningState(final GuiGraphics g, final int panelX, final int panelY,
                                      final int panelW, final int panelH, final int alpha)
    {
        String msg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.003");
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

        // 启用裁剪，防止项目溢出底部操作栏
        enableScissor(x, y, w, h);
        try
        {

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
                owner.withAlpha(MGStyles.C_ACCENT, alpha));
        }
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.032"), x + 8, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
            owner.TEXT_PRIMARY, alpha);
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
                    owner.withAlpha(MGStyles.C_ACCENT, alpha));
            }

            // 文件夹图标
            owner.drawGlassText(g, "\uD83D\uDCC1", x + 6, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                owner.TEXT_PRIMARY, alpha);

            // 风格包名称（截断）
            String displayName = pack.getName();
            if (displayName.length() > 14) displayName = displayName.substring(0, 12) + "..";
            owner.drawGlassText(g, displayName, x + 22, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                owner.TEXT_PRIMARY, alpha);

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
                        owner.TEXT_PRIMARY, alpha);

                    // 子目录名称
                    String subDisplayName = subPath;
                    if (subDisplayName.length() > 12) subDisplayName = subDisplayName.substring(0, 10) + "..";
                    owner.drawGlassText(g, subDisplayName, x + 36, itemY + (CATALOG_ITEM_H - font.lineHeight) / 2 + 1,
                        owner.TEXT_PRIMARY, alpha);

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
        finally
        {
            disableScissor();
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
            case "\u4f4f\u5b85" -> "\uD83C\uDFE0"; // 🏠 住宅
            case "\u516c\u5171\u8bbe\u65bd" -> "\uD83C\uDFDB"; // 🏛 公共设施
            case "\u57fa\u7840\u8bbe\u65bd" -> "\uD83C\uDFD7"; // 🏗 基础设施
            case "\u751f\u4ea7\u5efa\u7b51" -> "\u2692"; // ⚒ 生产建筑
            case "\u5546\u4e1a" -> "\uD83D\uDCB0"; // 💰 商业
            case "\u519b\u4e8b" -> "\u2694"; // ⚔ 军事
            case "\u5176\u4ed6" -> "\u2B50"; // ⭐ 其他
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
            case "\u4f4f\u5b85" -> "fundamentals";
            case "\u516c\u5171\u8bbe\u65bd" -> "education";
            case "\u57fa\u7840\u8bbe\u65bd" -> "infrastructure";
            case "\u751f\u4ea7\u5efa\u7b51" -> "agriculture";
            case "\u5546\u4e1a" -> "commercial";
            case "\u519b\u4e8b" -> "military";
            case "\u5176\u4ed6" -> "decorations";
            default -> null;
        };
    }

    // ════════════════════════════════════════════════════════
    //  图鉴编辑功能
    // ════════════════════════════════════════════════════════

    /**
     * 渲染文件夹编辑工具栏（在左侧文件夹树底部）。
     * 提供新建文件夹、重命名、删除按钮。
     */
    private void renderEditToolbar(final GuiGraphics g, final int panelX,
                                    final int toolbarY, final int toolbarW, final int alpha)
    {
        // 只显示在非放置模式下
        if (placementMode || placementConfirm) return;

        // 分隔线
        g.fill(panelX + 4, toolbarY, panelX + toolbarW - 4, toolbarY + 1, owner.withAlpha(owner.EDGE_INNER, alpha));

        final int btnY = toolbarY + 3;
        final int btnSize = 18;
        final int gap = 4;
        final int startX = panelX + 4;

        // [+] 新建文件夹
        final boolean newHover = owner.inBounds(startX, btnY, btnSize, btnSize);
        owner.fillGlassButton(g, startX, btnY, btnSize, btnSize, newHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, "+", startX + 4, btnY + 4, newHover ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

        // [✏] 重命名
        final int renameX = startX + btnSize + gap;
        final boolean renameHover = owner.inBounds(renameX, btnY, btnSize, btnSize);
        owner.fillGlassButton(g, renameX, btnY, btnSize, btnSize, renameHover ? owner.BTN_GLASS_HOVER : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, "\u270E", renameX + 4, btnY + 4, renameHover ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);

        // [X] 删除
        final int delX = renameX + btnSize + gap;
        final boolean delHover = owner.inBounds(delX, btnY, btnSize, btnSize);
        owner.fillGlassButton(g, delX, btnY, btnSize, btnSize, delHover ? 0x66484868 : owner.BTN_GLASS, alpha);
        owner.drawGlassText(g, "\u2716", delX + 4, btnY + 4, delHover ? 0xCCB88888 : owner.TEXT_PRIMARY, alpha);

        // 提示文字（仅在非对话框模式显示，对话框由 renderEditDialog 独立渲染）
        if (editAction != EditAction.NONE)
        {
            // 工具栏不显示提示——对话框已有完整界面
        }
    }

    /**
     * 渲染编辑输入对话框。
     */
    private void renderEditDialog(final GuiGraphics g, final int alpha)
    {
        if (editAction == EditAction.NONE) return;

        final int dialogW = 280;
        final int dialogH = 100;
        final int dialogX = (width - dialogW) / 2;
        final int dialogY = (height - dialogH) / 2;

        // 背景
        owner.fillGlassPanel(g, dialogX, dialogY, dialogW, dialogH, owner.GLASS_BG, alpha);

        // 标题
        final String title = switch (editAction)
        {
            case NEW_FOLDER -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.040");
            case RENAME_FOLDER -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.041");
            case RENAME_FILE -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.042");
            case DELETE_CONFIRM -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.043");
            default -> "";
        };
        owner.drawGlassText(g, title, dialogX + 16, dialogY + 10, owner.TEXT_SOFT_GOLD, alpha);

        if (editAction == EditAction.DELETE_CONFIRM)
        {
            // 删除确认信息
            final String msg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.044") + editInputText + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.045");
            owner.drawGlassText(g, msg, dialogX + 16, dialogY + 34, owner.TEXT_PRIMARY, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.046"), dialogX + 16, dialogY + 50, 0xCCB88888, alpha);

            // 确认/取消按钮
            final int btnW = 60;
            final int btnH = 20;
            final int btnY2 = dialogY + dialogH - 30;
            final boolean confirmHover = owner.inBounds(dialogX + 50, btnY2, btnW, btnH);
            final boolean cancelHover = owner.inBounds(dialogX + dialogW - 50 - btnW, btnY2, btnW, btnH);
            owner.fillGlassButton(g, dialogX + 50, btnY2, btnW, btnH, confirmHover ? 0x66486880 : 0x44384858, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.047"), dialogX + 50 + 4, btnY2 + 6, confirmHover ? owner.TEXT_GREEN : owner.TEXT_PRIMARY, alpha);
            owner.fillGlassButton(g, dialogX + dialogW - 50 - btnW, btnY2, btnW, btnH, cancelHover ? 0x66684848 : 0x44584838, alpha);
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.048"), dialogX + dialogW - 50 - btnW + 4, btnY2 + 6, cancelHover ? 0xCCB88888 : owner.TEXT_PRIMARY, alpha);
        }
        else
        {
            // 文本输入框
            final int inputY = dialogY + 34;
            final int inputH = 22;
            owner.fillGlassPanel(g, dialogX + 16, inputY, dialogW - 32, inputH, owner.GLASS_PANEL, alpha);

            // 输入框边框
            g.fill(dialogX + 16, inputY, dialogX + 16 + dialogW - 32, inputY + 1, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
            g.fill(dialogX + 16, inputY + inputH - 1, dialogX + 16 + dialogW - 32, inputY + inputH, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));

            // 输入文本
            owner.drawGlassText(g, editInputText, dialogX + 20, inputY + (inputH - font.lineHeight) / 2 + 1, owner.TEXT_PRIMARY, alpha);

            // 光标闪烁
            final long now = System.currentTimeMillis();
            if (now - editCursorTime > 500)
            {
                editCursorTime = now;
                editCursorVisible = !editCursorVisible;
            }
            if (editCursorVisible)
            {
                final int cursorX = dialogX + 20 + font.width(editInputText);
                g.fill(cursorX, inputY + 4, cursorX + 1, inputY + inputH - 4, owner.withAlpha(owner.TEXT_SOFT_GOLD, alpha));
            }

            // 提示
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.049"), dialogX + 16, inputY + inputH + 4, owner.TEXT_SECONDARY, alpha);
        }
    }

    /**
     * 获取当前选中项的包名和路径（用于编辑操作）。
     * @return [packName, relativePath] 或 null
     */
    @Nullable
    private String[] getEditTargetInfo()
    {
        if (selectedPackName != null)
        {
            final String packPath = selectedSubPath != null ? selectedSubPath : "";
            return new String[]{selectedPackName, packPath};
        }
        // 如果有选中蓝图，使用其包信息
        final BuildingCatalogEntry entry = getSelectedCatalogEntry();
        if (entry != null)
        {
            // 取最后一级目录作为路径
            final String rp = entry.resourcePath();
            final int lastSlash = rp.lastIndexOf('/');
            final String dirPath = lastSlash >= 0 ? rp.substring(0, lastSlash) : "";
            return new String[]{entry.packName(), dirPath};
        }
        // 取第一个可用包
        final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
        if (!packs.isEmpty())
        {
            return new String[]{packs.iterator().next().getName(), ""};
        }
        return null;
    }

    /**
     * 执行文件系统操作：创建文件夹。
     */
    private void doCreateFolder()
    {
        if (editInputText.isEmpty()) { editAction = EditAction.NONE; return; }

        final String[] target = getEditTargetInfo();
        if (target == null) { editAction = EditAction.NONE; return; }

        final String packName = target[0];
        final String parentPath = target[1];
        final String newDirName = editInputText.trim();

        if (newDirName.isEmpty()) { editAction = EditAction.NONE; return; }

        try
        {
            final var packMeta = com.metrogenesis.structurize.storage.StructurePacks.getStructurePack(packName);
            if (packMeta != null)
            {
                final Path basePath = packMeta.getPath();
                final Path newDir = basePath.resolve(
                    parentPath.isEmpty() ? newDirName : parentPath + "/" + newDirName);
                Files.createDirectories(newDir);
                LOGGER.info("[Catalog] Created folder: {}", newDir);
                // 刷新扫描器
                refresh();
            }
        }
        catch (final java.io.IOException e)
        {
            LOGGER.error("[Catalog] Failed to create folder: {}", e.getMessage());
        }

        editAction = EditAction.NONE;
        editInputText = "";
    }

    /**
     * 执行文件系统操作：重命名。
     */
    private void doRename()
    {
        if (editInputText.isEmpty()) { editAction = EditAction.NONE; return; }

        final String newName = editInputText.trim();
        if (newName.isEmpty()) { editAction = EditAction.NONE; return; }

        try
        {
            final var packMeta = com.metrogenesis.structurize.storage.StructurePacks.getStructurePack(editTargetPack);
            if (packMeta != null)
            {
                final Path basePath = packMeta.getPath();
                final Path sourcePath = basePath.resolve(editTargetPath);
                final Path parentPath = sourcePath.getParent();
                final Path targetPath;

                if (editTargetIsFolder)
                {
                    targetPath = parentPath.resolve(newName);
                    Files.move(sourcePath, targetPath);
                    LOGGER.info("[Catalog] Renamed folder: {} -> {}", sourcePath, targetPath);

                    // 文件夹重命名后，其下所有蓝图缓存全部失效
                    try (final var walk = Files.walk(sourcePath))
                    {
                        walk.filter(p -> p.toString().endsWith(".blueprint"))
                            .forEach(p -> {
                                final String relPath = sourcePath.getParent().relativize(p).toString()
                                    .replace(".blueprint", "").replace("\\", "/");
                                PreviewCache.invalidateCache(editTargetPack, relPath);
                            });
                    }
                    catch (final IOException ignored) {}
                }
                else
                {
                    // 蓝图文件
                    targetPath = parentPath.resolve(newName + ".blueprint");
                    Files.move(sourcePath, targetPath);
                    LOGGER.info("[Catalog] Renamed blueprint: {} -> {}", sourcePath, targetPath);

                    // 旧路径缓存失效
                    PreviewCache.invalidateCache(editTargetPack, editTargetPath.replace(".blueprint", ""));
                }
                refresh();
            }
        }
        catch (final java.io.IOException e)
        {
            LOGGER.error("[Catalog] Failed to rename: {}", e.getMessage());
        }

        editAction = EditAction.NONE;
        editInputText = "";
        editTargetPack = null;
        editTargetPath = null;
    }

    /**
     * 执行文件系统操作：删除。
     */
    private void doDelete()
    {
        try
        {
            final var packMeta = com.metrogenesis.structurize.storage.StructurePacks.getStructurePack(editTargetPack);
            if (packMeta != null)
            {
                final Path basePath = packMeta.getPath();
                final Path targetPath = basePath.resolve(editTargetPath);

                if (editTargetIsFolder)
                {
                    // 递归删除文件夹
                    try (final var walk = Files.walk(targetPath))
                    {
                        walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); }
                                catch (final java.io.IOException e) { LOGGER.error("Failed to delete: {}", p); }
                            });
                    }
                    LOGGER.info("[Catalog] Deleted folder: {}", targetPath);

                    // 该文件夹下所有蓝图缓存失效
                    PreviewCache.invalidateCache(editTargetPack, editTargetPath);
                }
                else
                {
                    Files.delete(targetPath);
                    LOGGER.info("[Catalog] Deleted blueprint: {}", targetPath);

                    // 单个蓝图缓存失效
                    PreviewCache.invalidateCache(editTargetPack, editTargetPath.replace(".blueprint", ""));
                }
                refresh();
            }
        }
        catch (final java.io.IOException e)
        {
            LOGGER.error("[Catalog] Failed to delete: {}", e.getMessage());
        }

        editAction = EditAction.NONE;
        editInputText = "";
        editTargetPack = null;
        editTargetPath = null;
    }

    /**
     * 取消当前编辑操作。
     */
    public void cancelEdit()
    {
        editAction = EditAction.NONE;
        editInputText = "";
        editTargetPack = null;
        editTargetPath = null;
    }

    /**
     * 开始新建文件夹。
     */
    private void startNewFolder()
    {
        editAction = EditAction.NEW_FOLDER;
        editInputText = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.040");
        editCursorTime = System.currentTimeMillis();
        editCursorVisible = true;
    }

    /**
     * 开始重命名当前选中项。
     */
    private void startRename()
    {
        if (selectedSubPath != null)
        {
            editAction = EditAction.RENAME_FOLDER;
            editInputText = selectedSubPath;
            editTargetPack = selectedPackName;
            editTargetPath = selectedSubPath;
            editTargetIsFolder = true;
        }
        else
        {
            final BuildingCatalogEntry entry = getSelectedCatalogEntry();
            if (entry != null)
            {
                editAction = EditAction.RENAME_FILE;
                editInputText = entry.name();
                editTargetPack = entry.packName();
                editTargetPath = entry.resourcePath() + ".blueprint";
                editTargetIsFolder = false;
            }
            else
            {
                return;
            }
        }
        editCursorTime = System.currentTimeMillis();
        editCursorVisible = true;
    }

    /**
     * 开始删除确认。
     */
    private void startDelete()
    {
        if (selectedSubPath != null)
        {
            editAction = EditAction.DELETE_CONFIRM;
            editInputText = selectedSubPath;
            editTargetPack = selectedPackName;
            editTargetPath = selectedSubPath;
            editTargetIsFolder = true;
        }
        else
        {
            final BuildingCatalogEntry entry = getSelectedCatalogEntry();
            if (entry != null)
            {
                editAction = EditAction.DELETE_CONFIRM;
                editInputText = entry.name();
                editTargetPack = entry.packName();
                editTargetPath = entry.resourcePath() + ".blueprint";
                editTargetIsFolder = false;
            }
            else
            {
                return;
            }
        }
    }

    /**
     * 处理编辑相关的按键输入（由 MayorBookScreen 委托）。
     */
    public boolean keyPressedEdit(final int keyCode, final int scanCode, final int modifiers)
    {
        if (editAction == EditAction.NONE) return false;

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)
        {
            cancelEdit();
            return true;
        }

        if (editAction == EditAction.DELETE_CONFIRM)
        {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER)
            {
                doDelete();
                return true;
            }
            return true; // 在删除确认中阻止其他按键
        }

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER)
        {
            if (editAction == EditAction.NEW_FOLDER)
            {
                doCreateFolder();
            }
            else if (editAction == EditAction.RENAME_FOLDER || editAction == EditAction.RENAME_FILE)
            {
                doRename();
            }
            return true;
        }

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !editInputText.isEmpty())
        {
            editInputText = editInputText.substring(0, editInputText.length() - 1);
            editCursorTime = System.currentTimeMillis();
            editCursorVisible = true;
            return true;
        }

        return true;
    }

    /**
     * 处理字符输入（由 MayorBookScreen 委托）。
     */
    public boolean charTypedEdit(final char codePoint, final int modifiers)
    {
        if (editAction == EditAction.NONE || editAction == EditAction.DELETE_CONFIRM) return false;

        // 限制输入长度
        if (editInputText.length() < 40 && codePoint >= ' ' && codePoint != 0x7F)
        {
            editInputText += codePoint;
            editCursorTime = System.currentTimeMillis();
            editCursorVisible = true;
            return true;
        }
        return true;
    }

    /**
     * 处理编辑工具栏的鼠标点击。
     */
    private boolean handleEditToolbarClick(final double mx, final double my, final int button)
    {
        if (button != 0) return false;
        if (placementMode || placementConfirm) return false;

        int panelW = width * CATALOG_PANEL_W_RATIO_NUM / CATALOG_PANEL_W_RATIO_DEN;
        panelW = Math.min(panelW, width - 80);
        final int panelH = height * CATALOG_PANEL_H_RATIO_NUM / CATALOG_PANEL_H_RATIO_DEN;
        final int panelX = (width - panelW) / 2;
        final int panelY = (height - panelH) / 2;
        int leftX = panelX + 4;
        int leftY = panelY + 26;
        int leftW = CATALOG_LEFT_W - 8;
        int leftH = panelH - 30 - CATALOG_BOTTOM_H - 4;

        final int toolbarY = leftY + leftH + 2;
        final int btnSize = 18;
        final int gap = 4;
        final int startX = leftX;

        // [+] 新建文件夹
        if (owner.inBounds(startX, toolbarY + 3, btnSize, btnSize))
        {
            startNewFolder();
            return true;
        }

        // [✏] 重命名
        final int renameX = startX + btnSize + gap;
        if (owner.inBounds(renameX, toolbarY + 3, btnSize, btnSize))
        {
            startRename();
            return true;
        }

        // [X] 删除
        final int delX = renameX + btnSize + gap;
        if (owner.inBounds(delX, toolbarY + 3, btnSize, btnSize))
        {
            startDelete();
            return true;
        }

        return false;
    }

    /**
     * 处理编辑对话框中的鼠标点击。
     */
    private boolean handleEditDialogClick(final double mx, final double my, final int button)
    {
        if (editAction == EditAction.NONE) return false;
        if (button != 0) return false;

        final int dialogW = 280;
        final int dialogH = 100;
        final int dialogX = (width - dialogW) / 2;
        final int dialogY = (height - dialogH) / 2;

        if (editAction == EditAction.DELETE_CONFIRM)
        {
            final int btnW = 60;
            final int btnH = 20;
            final int btnY2 = dialogY + dialogH - 30;

            // 确认删除
            if (owner.inBounds(dialogX + 50, btnY2, btnW, btnH))
            {
                doDelete();
                return true;
            }

            // 取消
            if (owner.inBounds(dialogX + dialogW - 50 - btnW, btnY2, btnW, btnH))
            {
                cancelEdit();
                return true;
            }

            return true; // 在对话框区域内阻止其他点击
        }

        // 点击对话框外部 = 取消
        if (mx < dialogX || mx > dialogX + dialogW || my < dialogY || my > dialogY + dialogH)
        {
            cancelEdit();
            return true;
        }

        return true;
    }

    /**
     * 处理风格包选择首页的点击。
     * 点击 Select 按钮 → 选定包 → 进入蓝图浏览模式。
     */
    private boolean handlePackSelectionClick(final double mx, final double my, final int button)
    {
        // 使用与 renderPackSelection 相同的面板几何
        final int pw = Math.min(width - 40, width);
        final int ph = Math.min(height - 40, height);
        final int px = (width - pw) / 2;
        final int py = (height - ph) / 2;

        // 关闭按钮
        if (button == 0 && owner.inBounds(px + pw - 18, py + 3, 14, 14)) {
            close();
            return true;
        }

        // 检查加载状态
        if (scanFuture != null && !scanFuture.isDone()) return false;

        final var packs = com.metrogenesis.structurize.storage.StructurePacks.getPackMetas();
        if (packs.isEmpty()) return false;

        final int cols = Math.max(1, (pw - 40) / (PACK_CARD_W + PACK_CARD_GAP));
        final int gridStartX = px + (pw - cols * PACK_CARD_W - (cols - 1) * PACK_CARD_GAP) / 2;
        final int gridStartY = py + 36;

        int col = 0;
        int row = 0;

        for (final var pack : packs)
        {
            final int cx = gridStartX + col * (PACK_CARD_W + PACK_CARD_GAP);
            final int cy = gridStartY + row * (PACK_CARD_H + PACK_CARD_GAP) - packSelectionScrollOffset;

            // 跳过完全不可见的卡片
            if (cy + PACK_CARD_H < gridStartY || cy > gridStartY + ph - 46) {
                col++;
                if (col >= cols) { col = 0; row++; }
                continue;
            }

            // 检测 Select 按钮点击
            final int btnW = 70;
            final int btnH = 18;
            final int btnX = cx + PACK_CARD_W - btnW - 6;
            final int btnY = cy + PACK_CARD_H - btnH - 4;
            if (button == 0 && owner.inBounds(btnX, btnY, btnW, btnH))
            {
                selectPack(pack.getName());
                return true;
            }

            // 检测整张卡片点击（也可选包）
            if (button == 0 && mx >= cx && mx < cx + PACK_CARD_W && my >= cy && my < cy + PACK_CARD_H)
            {
                selectPack(pack.getName());
                return true;
            }

            col++;
            if (col >= cols) { col = 0; row++; }
        }

        return false;
    }

    /**
     * 选定风格包，进入蓝图浏览模式。
     * 扫描结果已在 checkScanResult() 中异步加载，此方法不阻塞。
     */
    private void selectPack(final String packName)
    {
        packSelectionMode = false;
        selectedPackName = packName;
        selectedSubPath = null;
        catalogSelectedIndex = -1;
        catalogScrollOffset = 0;
        folderScrollOffset = 0;

        // 通知市长书：该风格包被选为城市默认风格（Phase 3 自动建造用）
        owner.setActiveStylePack(packName);
    }

    /**
     * 返回风格包选择首页（由蓝图浏览模式的"返回"按钮触发）。
     */
    public void backToPackSelection()
    {
        packSelectionMode = true;
        selectedPackName = null;
        selectedSubPath = null;
        catalogSelectedIndex = -1;
    }

    // ════════════════════════════════════════════════════════
    //  蓝图列表渲染
    // ════════════════════════════════════════════════════════

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
            case LARGE_ICONS -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.050");
            case SMALL_ICONS -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.051");
            case LIST -> Language.getInstance().getOrDefault("gui.metrogenesis.catalog.052");
        };
        owner.drawGlassText(g, title, x + 6, y + 4, owner.TEXT_SOFT_GOLD, alpha);
        g.fill(x + 4, y + 16, x + w - 4, y + 17, owner.withAlpha(owner.EDGE_INNER, alpha));

        String selectedCat = CategoryMapper.getCategoryByIndex(catalogCategoryIndex);
        final List<BuildingCatalogEntry> entries = getEntriesForCategory(selectedCat);
        int startY = y + 20;

        if (entries.isEmpty())
        {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.053"), x + 8, startY + 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 根据视图模式选择渲染方式
        // 启用裁剪，防止内容溢出到预览区
        final int contentX = x;
        final int contentY = startY;
        final int contentW = w;
        final int contentH = h - 20;
        enableScissor(contentX, contentY, contentW, contentH);
        try
        {
            switch (viewMode)
            {
                case LARGE_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 80, 80, alpha);
                case SMALL_ICONS -> renderGrid(g, x, startY, w, h - 20, entries, 40, 40, alpha);
                case LIST -> renderList(g, x, startY, w, h - 20, entries, alpha);
            }
        }
        finally
        {
            disableScissor();
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

            // 选中边框（v1.2：工业橙强调色）
            if (selected)
            {
                g.fill(cellX, cellY, cellX + cellW, cellY + 1, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX, cellY + cellH - 1, cellX + cellW, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX, cellY, cellX + 1, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
                g.fill(cellX + cellW - 1, cellY, cellX + cellW, cellY + cellH, owner.withAlpha(MGStyles.C_ACCENT, alpha));
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
                cellY + cellH - 14, owner.TEXT_PRIMARY, alpha);

            // 无缝建筑标记
            if ("seamless".equals(entry.mcCategory()))
            {
                owner.drawGlassText(g, "\u25A0", cellX + 2, cellY + 2, 0xFFC89B3C, alpha);
            }
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
                owner.TEXT_PRIMARY, alpha);

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
        // ── 尝试加载缓存缩略图 ──
        final DynamicTexture cachedTex = PreviewCache.loadCachedPreview(entry.packName(), entry.resourcePath());
        if (cachedTex != null)
        {
            // 缓存存在，直接绘制（手动提交纹理四边形）
            RenderSystem.setShaderTexture(0, cachedTex.getId());
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            Matrix4f mat = g.pose().last().pose();
            com.mojang.blaze3d.vertex.BufferBuilder buf = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
            buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
            buf.vertex(mat, x, y + h, 0).uv(0, 1).endVertex();
            buf.vertex(mat, x + w, y + h, 0).uv(1, 1).endVertex();
            buf.vertex(mat, x + w, y, 0).uv(1, 0).endVertex();
            buf.vertex(mat, x, y, 0).uv(0, 0).endVertex();
            com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.end());
            return;
        }

        // ── 无缓存，正常渲染体素预览 ──
        final Blueprint bp = loadBlueprintForPreview(entry);
        if (bp != null)
        {
            renderVoxelPreview(g, x, y, w, h, bp, alpha);

            // 渲染成功后异步保存外部缓存 PNG（不阻塞 UI）
            final String packName = entry.packName();
            final String resourcePath = entry.resourcePath();
            com.metrogenesis.structurize.util.IOPool.submit(() -> {
                PreviewCache.saveExternalPreview(packName, resourcePath, bp);
                return null;
            });

            return;
        }

        // 加载失败时使用尺寸信息渲染简化的建筑轮廓（与原有备用逻辑一致）
        int sizeX = entry.size().getX();
        int sizeY = entry.size().getY();
        int sizeZ = entry.size().getZ();

        // 等轴测投影参数（简化版）
        float scale = Math.min(w / (float)(sizeX + sizeZ + 2), h / (float)(sizeY + 2)) * 0.5f;
        scale = Math.max(1f, Math.min(scale, 6f));

        int centerX = x + w / 2;
        int centerY = y + h / 2;

        int blockSize = Math.max(2, (int)(scale * 0.9f));

        // 绘制简化的建筑形状
        for (int by = 0; by < Math.min(sizeY, 5); by++)
        {
            for (int bz = 0; bz < Math.min(sizeZ, 5); bz++)
            {
                for (int bx = 0; bx < Math.min(sizeX, 5); bx++)
                {
                    float isoX = (bx - bz) * scale + centerX;
                    float isoY = (bx + bz) * scale * 0.5f - by * scale + centerY;

                    int drawX = (int)isoX;
                    int drawY = (int)isoY;

                    int color;
                    if (by == 0)
                        color = 0xFF8B7355;
                    else if (by == sizeY - 1)
                        color = 0xFF8B0000;
                    else
                        color = 0xFFD2B48C;

                    color = owner.withAlpha(color, alpha);
                    g.fill(drawX, drawY, drawX + blockSize, drawY + blockSize, color);

                    if (blockSize >= 3)
                    {
                        g.fill(drawX, drawY, drawX + blockSize, drawY + 1, owner.withAlpha(0x55FFFFFF, alpha));
                        g.fill(drawX, drawY, drawX + 1, drawY + blockSize, owner.withAlpha(0x55FFFFFF, alpha));
                        g.fill(drawX, drawY + blockSize - 1, drawX + blockSize, drawY + blockSize, owner.withAlpha(0x66000000, alpha));
                        g.fill(drawX + blockSize - 1, drawY, drawX + blockSize, drawY + blockSize, owner.withAlpha(0x66000000, alpha));
                    }
                }
            }
        }

        String sizeStr = sizeX + "\u00D7" + sizeZ + "\u00D7" + sizeY;
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
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.054"), x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 使用 StructurePacks 加载蓝图（兼容殖民地生态）
        final Blueprint bp = loadBlueprintForPreview(entry);
        if (bp == null)
        {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.055"), x + 8, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        // 启用裁剪，防止体素渲染溢出底部操作栏
        enableScissor(x, y, w, h);
        try
        {
            renderVoxelPreview3D(g, x + 4, y + 4, w - 8, h - 8, bp, entry, alpha);
        }
        finally
        {
            disableScissor();
        }
    }

    /**
     * 真正的 3D 方块预览 — 使用 MC 原版方块渲染器在透视视口中逐块渲染。
     * 支持鼠标中键拖拽平移和滚轮缩放。
     * 大预览区专用（renderCatalogPreview 调用）。
     */
    private void renderVoxelPreview3D(final GuiGraphics g, final int x, final int y,
                                       final int w, final int h, final Blueprint bp,
                                       final BuildingCatalogEntry entry, final int alpha)
    {
        final short bpSizeX = bp.getSizeX();
        final short bpSizeY = bp.getSizeY();
        final short bpSizeZ = bp.getSizeZ();
        final short[][][] structure = bp.getStructure();
        final BlockState[] palette = bp.getPalette();

        final int maxDim = Math.max(bpSizeX, Math.max(bpSizeY, bpSizeZ));
        if (maxDim == 0) return;

        final var mc = Minecraft.getInstance();

        // ── 计算自适应缩放 ──
        // 用最大维度计算基础缩放，确保建筑完整可见
        final float baseScale = Math.min((float) w / (maxDim + 2), (float) h / (maxDim + 2)) * 0.7f;
        // 应用用户缩放
        final float finalScale = baseScale * previewZoom;
        // Ponder 范式：无交互时自动缓慢旋转展示
        final long now = System.currentTimeMillis();
        if (now - lastPreviewInteraction > 1500L)
        {
            previewRotY += 0.4f;
            if (previewRotY >= 360f) previewRotY -= 360f;
        }
        // 视距基于最大维度，确保建筑在透视投影中可见
        final float viewDist = maxDim * 3.5f;
        // 屏幕中心 + 用户平移偏移
        final float centerX = x + w / 2f + previewPanX;
        final float centerY = y + h / 2f + previewPanY;

        // ── 开启深度测试和裁剪 ──
        enableScissor(x, y, w, h);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        // ── 设置透视投影 ──
        final Matrix4f perspective = new Matrix4f().perspective(
            (float) Math.toRadians(35),
            (float) w / h,
            0.1f, 10000f);

        // ── 相机变换 ──
        // 变换顺序（从后到前）：
        //   block(bx,by,bz) → 旋转视角 → 居中 → 缩放 → 屏幕中心平移
        final PoseStack pose = new PoseStack();
        // 1) 移到屏幕中心 + 用户平移
        pose.translate(centerX, centerY, 0);
        // 2) 缩放（Y 翻转用于屏幕坐标，Z 不缩放因为有透视投影）
        pose.scale(finalScale, -finalScale, 1f);
        // 3) 移到建筑中心（让建筑绕自身中心旋转）
        pose.translate(-bpSizeX / 2f, -bpSizeY / 2f, -viewDist);
        // 4) 用户可调的视角旋转（默认等轴测 35°俯仰/45°偏航）
        pose.mulPose(Axis.XP.rotationDegrees(previewRotX));
        pose.mulPose(Axis.YN.rotationDegrees(previewRotY));

        final var buffer = mc.renderBuffers().bufferSource();
        final var blockRenderer = mc.getBlockRenderer();

        // 建筑包围盒线框（黄铜，Ponder 风格锚定轮廓）
        final AABB wireBox = new AABB(0, 0, 0, bpSizeX, bpSizeY, bpSizeZ);
        final VertexConsumer lineBuf = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lineBuf, wireBox,
            ((MGStyles.C_BRASS >> 16) & 0xFF) / 255f,
            ((MGStyles.C_BRASS >> 8) & 0xFF) / 255f,
            (MGStyles.C_BRASS & 0xFF) / 255f, 0.6f);

        // ── 逐块渲染 ──
        for (short by = 0; by < bpSizeY; by++)
        {
            for (short bz = 0; bz < bpSizeZ; bz++)
            {
                for (short bx = 0; bx < bpSizeX; bx++)
                {
                    final short val = structure[by][bz][bx];
                    if (val == 0 || val >= palette.length) continue;

                    final BlockState state = palette[val];
                    if (state.isAir()) continue;

                    pose.pushPose();
                    pose.translate(bx, by, bz);
                    blockRenderer.renderSingleBlock(
                        state, pose, buffer,
                        0xF000F0,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
                    );
                    pose.popPose();
                }
            }
        }
        buffer.endBatch();

        // ── 恢复 GUI 渲染状态 ──
        RenderSystem.disableDepthTest();
        disableScissor();

        // Ponder 范式注释叠层：黄铜数据卡片 + 引线指向模型中心
        renderPonderAnnotations(g, x, y, w, h, entry, (int) centerX, (int) centerY, alpha);
    }

    /**
     * Ponder 范式注释叠层 — 在 3D 预览四角绘制黄铜数据卡片，
     * 用细引线指向模型中心，标注分类 / 等级 / 设施 / 造价（C-Value）。
     */
    private void renderPonderAnnotations(final GuiGraphics g, final int x, final int y,
                                         final int w, final int h, final BuildingCatalogEntry entry,
                                         final int modelCx, final int modelCy, final int alpha)
    {
        final int cardW = 94;
        final int cardH = 34;
        final int pad = 6;
        final int lineAlpha = (alpha * 50) / 100;

        final String cat  = entry.category();
        final String lvl  = entry.levelsDisplay() + (entry.isMultiLevel() ? Language.getInstance().getOrDefault("gui.metrogenesis.catalog.056") : " 级");
        // 设施：有设施方块显示类型名；混合用途显示"混合用途"；无设施方块则回退到文件夹分类
        final String fac  = entry.isMixedUse() ? Language.getInstance().getOrDefault("gui.metrogenesis.catalog.057")
            : (entry.buildingType().isEmpty() ? entry.category() : entry.buildingType());
        final String cost = formatCValue(entry.materialCost()) + " C";

        // 左上：分类
        final int ax = x + pad, ay = y + pad;
        drawPonderCard(g, ax, ay, cardW, cardH, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.059"), cat, MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
        drawLeader(g, ax + cardW, ay + cardH / 2, modelCx, modelCy, lineAlpha);

        // 右上：设施
        final int bx = x + w - cardW - pad, by = y + pad;
        drawPonderCard(g, bx, by, cardW, cardH, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.060"), fac, MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
        drawLeader(g, bx, by + cardH / 2, modelCx, modelCy, lineAlpha);

        // 左下：等级
        final int cx = x + pad, cy = y + h - cardH - pad;
        drawPonderCard(g, cx, cy, cardW, cardH, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.061"), lvl, MGStyles.C_TEXT_2ND, MGStyles.C_TEXT, alpha);
        drawLeader(g, cx + cardW, cy + cardH / 2, modelCx, modelCy, lineAlpha);

        // 右下：造价（强调色，经济核心）
        final int dx = x + w - cardW - pad, dy = y + h - cardH - pad;
        drawPonderCard(g, dx, dy, cardW, cardH, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.062"), cost, MGStyles.C_ACCENT, MGStyles.C_TEXT_BRASS, alpha);
        drawLeader(g, dx, dy + cardH / 2, modelCx, modelCy, lineAlpha);

        // 底部中央：功能说明（第五卡，真正的「功能说明」卡片）
        final String descKey = entry.description();
        final String desc = (descKey != null && !descKey.isEmpty())
            ? Language.getInstance().getOrDefault(descKey)
            : Language.getInstance().getOrDefault("gui.metrogenesis.catalog.desc.cat.other");
        final int gapW = (w - 2 * pad) - 2 * cardW;
        if (gapW >= 90) {
            final int ew = gapW - 6;
            final int eh = cardH + 8;
            final int ex = x + pad + cardW + 6;
            final int ey = y + h - eh - pad;
            drawPonderDescCard(g, ex, ey, ew, eh,
                Language.getInstance().getOrDefault(BuildingDescriptionProvider.TITLE_KEY), desc, alpha);
            drawLeader(g, ex + ew / 2, ey, modelCx, modelCy, lineAlpha);
        }
    }

    /** 单张 Ponder 数据卡片（半透明玻璃底 + 黄铜框 + 标题/数值） */
    private void drawPonderCard(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final String title, final String value, final int titleColor, final int valueColor,
                                final int alpha)
    {
        MGStyles.drawPanel(g, x, y, w, h, MGStyles.C_BG_CARD, (alpha * 80) / 100);
        g.drawString(font, title, x + 6, y + 5, owner.withAlpha(titleColor, alpha), false);
        g.drawString(font, value, x + 6, y + 17, owner.withAlpha(valueColor, alpha), false);
    }

    /** 功能说明卡片（标题 + 最多 2 行自动换行正文）。 */
    private void drawPonderDescCard(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                    final String title, final String text, final int alpha)
    {
        MGStyles.drawPanel(g, x, y, w, h, MGStyles.C_BG_CARD, (alpha * 80) / 100);
        g.drawString(font, title, x + 6, y + 4, owner.withAlpha(MGStyles.C_TEXT_2ND, alpha), false);
        final int maxW = w - 12;
        int ty = y + 16;
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (int i = 0; i < text.length() && lines < 2; i++) {
            final char c = text.charAt(i);
            if (font.width(line.toString() + c) > maxW && !line.isEmpty()) {
                g.drawString(font, line.toString(), x + 6, ty, owner.withAlpha(MGStyles.C_TEXT, alpha), false);
                ty += 12;
                lines++;
                line = new StringBuilder();
            }
            line.append(c);
        }
        if (lines < 2 && !line.isEmpty()) {
            g.drawString(font, line.toString(), x + 6, ty, owner.withAlpha(MGStyles.C_TEXT, alpha), false);
        }
    }

    /** 黄铜虚线引线：从卡片指向模型中心，末端小锚点 */
    private void drawLeader(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2, final int alpha)
    {
        final int dx = x2 - x1, dy = y2 - y1;
        final double dist = Math.hypot(dx, dy);
        final int steps = (int) (dist / 7);
        for (int i = 1; i < steps; i++)
        {
            final double t = (double) i / steps;
            final int px = x1 + (int) (dx * t);
            final int py = y1 + (int) (dy * t);
            g.fill(px, py, px + 1, py + 1, owner.withAlpha(MGStyles.C_BRASS, alpha));
        }
        g.fill(x2 - 1, y2 - 1, x2 + 2, y2 + 2, owner.withAlpha(MGStyles.C_BRASS_LT, alpha));
    }

    /** C-Value 造价格式化（千分位） */
    private static String formatCValue(final long v)
    {
        return String.format("%,d", v);
    }

    /**
     * 等轴测体素渲染（缩略图用） — 2D 3面方块模拟。
     */
    private void renderVoxelPreview(final GuiGraphics g, final int x, final int y,
                                    final int w, final int h, final Blueprint bp, final int alpha)
    {
        final short bpSizeX = bp.getSizeX();
        final short bpSizeY = bp.getSizeY();
        final short bpSizeZ = bp.getSizeZ();
        final short[][][] structure = bp.getStructure();
        final BlockState[] palette = bp.getPalette();

        // 等轴测投影参数 — 根据预览区域自动计算最佳缩放
        final float isoScaleX = Math.max(1.5f, Math.min(
            (float) w / (bpSizeX + bpSizeZ + 2),
            (float) h / (bpSizeY + 2)
        ));
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
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.063"), x + w / 2 - 12, y + h / 2 - 4, owner.TEXT_SECONDARY, alpha);
            return;
        }

        final int centerX = x + w / 2;
        final int centerY = y + h / 2;
        final float centerIsoX = (minIsoX + maxIsoX) / 2;
        final float centerIsoY = (minIsoY + maxIsoY) / 2;
        final float offsetX = centerX - centerIsoX;
        final float offsetY = centerY - centerIsoY;

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
                    final int dx = (int) isoX;
                    final int dy = (int) isoY;

                    int baseColor = 0xFF888888;
                    if (val >= 0 && val < palette.length)
                    {
                        final BlockState state = palette[val];
                        try
                        {
                            final MapColor mapColor = state.getMapColor(clientLevel, BlockPos.ZERO);
                            if (mapColor != null)
                            {
                                baseColor = 0xFF000000 | (mapColor.col & 0xFFFFFF);
                            }
                        }
                        catch (Exception e) { /* fallback */ }
                    }

                    // 提亮 40%
                    baseColor = brighten(baseColor, 0.4f);

                    final int bs = Math.max(2, (int) (isoScaleX * 0.9f));
                    if (bs < 3) {
                        g.fill(dx, dy, dx + bs, dy + bs, owner.withAlpha(baseColor, alpha));
                        continue;
                    }
                    final int tH = Math.max(1, bs / 3);
                    // 3 面方块：右侧暗 -> 左侧中 -> 顶面亮
                    g.fill(dx + bs / 2, dy + tH, dx + bs, dy + bs,
                        owner.withAlpha(shade(baseColor, 0.35f), alpha));
                    g.fill(dx, dy + tH, dx + bs / 2, dy + bs,
                        owner.withAlpha(shade(baseColor, 0.65f), alpha));
                    g.fill(dx, dy, dx + bs, dy + tH,
                        owner.withAlpha(baseColor, alpha));
                    // 左下阴影分割线
                    if (bs >= 4 && tH >= 2) {
                        g.fill(dx, dy + tH, dx + bs, dy + tH + 1,
                            0x33000000);
                    }
                }
            }
        }

        final String dimStr = bpSizeX + "\u00D7" + bpSizeZ + "\u00D7" + bpSizeY;
        owner.drawGlassText(g, dimStr, x + w / 2 - font.width(dimStr) / 2, y + 2, owner.TEXT_SECONDARY, alpha);
    }

    /** 亮度缩放 */
    private static int shade(int argb, float factor) {
        int r = (int)(((argb >> 16) & 0xFF) * factor);
        int g = (int)(((argb >> 8) & 0xFF) * factor);
        int b = (int)((argb & 0xFF) * factor);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    /** 提亮颜色 */
    private static int brighten(int argb, float amount) {
        int r = (int)(((argb >> 16) & 0xFF) + 255 * amount);
        int g = (int)(((argb >> 8) & 0xFF) + 255 * amount);
        int b = (int)((argb & 0xFF) + 255 * amount);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
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
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.064"), panelX + 12, bottomY + 8, owner.TEXT_SECONDARY, alpha);
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
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.065"), placeBtnX + (placeBtnW - font.width(Language.getInstance().getOrDefault("gui.metrogenesis.catalog.065"))) / 2, bottomY + 9,
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

        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.066") + bpName + "]\uFF1F", dialogX + 20, dialogY + 12, owner.TEXT_SOFT_GOLD, alpha);
        final BlockPos adjustedPos = getAdjustedPlacementPos();
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.067") + (adjustedPos != null ? adjustedPos.toShortString() : "?")
            + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.068") + PLACEMENT_ROT_NAMES[placementRotIdx],
            dialogX + 20, dialogY + 28, owner.TEXT_SECONDARY, alpha);

        final int btnW = 80;
        final int btnH = 22;
        final int confirmX = dialogX + 30;
        final int cancelX = dialogX + dialogW - btnW - 30;
        final int btnY = dialogY + 50;

        final boolean confirmHover = owner.inBounds(confirmX, btnY, btnW, btnH);
        final boolean cancelHover = owner.inBounds(cancelX, btnY, btnW, btnH);

        owner.fillGlassButton(g, confirmX, btnY, btnW, btnH, confirmHover ? 0x66486880 : 0x44384858, alpha);
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.069"), confirmX + (btnW - font.width(Language.getInstance().getOrDefault("gui.metrogenesis.catalog.069"))) / 2, btnY + 6,
            confirmHover ? owner.TEXT_GREEN : owner.TEXT_PRIMARY, alpha);

        owner.fillGlassButton(g, cancelX, btnY, btnW, btnH, cancelHover ? 0x66684848 : 0x44584838, alpha);
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.048"), cancelX + (btnW - font.width(Language.getInstance().getOrDefault("gui.metrogenesis.catalog.048"))) / 2, btnY + 6,
            cancelHover ? 0xCCB88888 : owner.TEXT_PRIMARY, alpha);
    }

    /**
     * 渲染放置模式提示（阶段1：拖拽确定位置 + 微调）。
     */
    private void renderPlacementOverlay(final GuiGraphics g, final int alpha)
    {
        // ── 顶部提示栏 ──
        final int barH = 22;
        owner.fillGlassPanel(g, 0, 0, width, barH, owner.GLASS_HEADER, alpha);
        final String modeName = placementNudgeMode ? "\u00a7a[NUDGE]" : "\u00a77[CAM]";
        final String hint = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.070")
            + modeName + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.071")
            + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.072");
        owner.drawGlassText(g, hint, width / 2 - font.width(hint) / 2, 5, owner.TEXT_SOFT_GOLD, alpha);

        // ── 底部信息 ──
        if (placementTargetPos != null)
        {
            final BlockPos adj = getAdjustedPlacementPos();
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.073") + adj.toShortString() + Language.getInstance().getOrDefault("gui.metrogenesis.catalog.074") + PLACEMENT_ROT_NAMES[placementRotIdx],
                8, height - 18, owner.TEXT_GREEN, alpha);
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

        // 风格包选择首页点击处理
        if (packSelectionMode)
        {
            return handlePackSelectionClick(mx, my, button);
        }

        // 编辑对话框处理（最高优先级，在对话框区域外点击取消）
        if (editAction != EditAction.NONE)
        {
            return handleEditDialogClick(mx, my, button);
        }

        if (placementConfirm)
        {
            return handlePlacementConfirmClick(mx, my, button);
        }

        if (!placementMode)
        {
            // 先检查编辑工具栏按钮
            if (handleEditToolbarClick(mx, my, button))
            {
                return true;
            }
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

        // 风格包选择模式下的滚动
        if (packSelectionMode)
        {
            if (packSelectionMaxScroll > 0)
            {
                packSelectionScrollOffset = Math.max(0, Math.min(packSelectionMaxScroll,
                    packSelectionScrollOffset - (int) (delta * 60)));
            }
            return true;
        }

        // 检查是否在 3D 预览区 — 如果是则缩放预览
        if (!placementMode && !placementConfirm)
        {
            final int[] previewRect = getPreviewRect();
            if (previewRect != null)
            {
                final int prX = previewRect[0], prY = previewRect[1], prW = previewRect[2], prH = previewRect[3];
                if (mx >= prX && mx <= prX + prW && my >= prY && my <= prY + prH)
                {
                    // 预览区缩放
                    final float zoomStep = 0.1f;
                    previewZoom = Math.max(0.2f, Math.min(3f, previewZoom + (float) (delta * zoomStep)));
                    lastPreviewInteraction = System.currentTimeMillis();
                    return true;
                }
            }
        }

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
     * 处理放置模式下的调整按钮点击。
     * 按钮布局（3x3，i=0~8）：
     *   0=↺逆时针 1=↑前进  2=↻顺时针
     *   3=←左移   4=✓确认  5=→右移
     *   6=↓下移   7=↕后移  8=↑上移
     */
    private boolean handleAdjButton(final int index)
    {
        switch (index)
        {
            case 0 -> rotate(-1);       // 逆时针旋转
            case 1 -> nudgeForward();   // 前进（玩家朝向）
            case 2 -> rotate(+1);       // 顺时针旋转
            case 3 -> nudgeLeft();      // 左移（玩家朝向）
            case 4 -> {                // 确认放置
                if (getSelectedCatalogEntry() != null && placementTargetPos != null) {
                    confirmPlacementPosition(placementTargetPos);
                }
            }
            case 5 -> nudgeRight();     // 右移（玩家朝向）
            case 6 -> nudgeY(-1);       // 下移
            case 7 -> nudgeBack();      // 后移（玩家朝向）
            case 8 -> nudgeY(+1);       // 上移
        }
        return true;
    }

    /** 根据摄像机朝向移动 */
    public void nudgeForward()
    {
        final double rad = Math.toRadians(cameraYaw);
        placementNudgeX -= (int) Math.round(Math.sin(rad));
        placementNudgeZ += (int) Math.round(Math.cos(rad));
        updateGhostPreview();
    }

    /** 根据摄像机朝向后退 */
    public void nudgeBack()
    {
        final double rad = Math.toRadians(cameraYaw);
        placementNudgeX += (int) Math.round(Math.sin(rad));
        placementNudgeZ -= (int) Math.round(Math.cos(rad));
        updateGhostPreview();
    }

    /** 根据摄像机向左 */
    public void nudgeLeft()
    {
        // 向右方向向量 (cos(yaw), sin(yaw))，左 = 反方向
        final double rad = Math.toRadians(cameraYaw);
        placementNudgeX += (int) Math.round(Math.cos(rad));
        placementNudgeZ += (int) Math.round(Math.sin(rad));
        updateGhostPreview();
    }

    /** 根据摄像机向右 */
    public void nudgeRight()
    {
        final double rad = Math.toRadians(cameraYaw);
        placementNudgeX -= (int) Math.round(Math.cos(rad));
        placementNudgeZ -= (int) Math.round(Math.sin(rad));
        updateGhostPreview();
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
            cancelPlacement();
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

        // 返回风格包首页按钮
        if (button == 0 && owner.inBounds(panelX + 80, panelY + 3, 50, 16))
        {
            backToPackSelection();
            return true;
        }

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

        // 刷新按钮（图鉴扫描刷新）
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

        // 中间蓝图列表/网格点击
        final int midX = panelX + CATALOG_LEFT_W + 4;
        final int midY = leftY;
        final int midW = panelW - CATALOG_LEFT_W - CATALOG_PREVIEW_W - 16;

        if (mx >= midX && mx <= midX + midW && my >= midY && my <= midY + leftH)
        {
            String selectedCat = CategoryMapper.getCategoryByIndex(catalogCategoryIndex);
            final List<BuildingCatalogEntry> entries = getEntriesForCategory(selectedCat);

            final int gridStartY = midY + 20;

            // 根据视图模式计算点击位置
            switch (viewMode)
            {
                case LARGE_ICONS, SMALL_ICONS ->
                {
                    final int cellW = (viewMode == ViewMode.LARGE_ICONS) ? 80 : 40;
                    final int cellH = (viewMode == ViewMode.LARGE_ICONS) ? 80 : 40;
                    int availableW = midW - 8;
                    int cols = Math.max(1, availableW / (cellW + GRID_CELL_GAP));

                    for (int i = 0; i < entries.size(); i++)
                    {
                        int row = i / cols;
                        int col = i % cols;

                        int cellX = midX + 4 + col * (cellW + GRID_CELL_GAP);
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

                        if (owner.inBounds(midX + 4, itemY, midW - 8, itemH))
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
                // 计算 RTS 摄像机中心的放置位置
                final BlockPos camTarget = owner.getCameraTarget();
                if (camTarget != null) {
                    placementTargetPos = camTarget;
                    updateGhostPreview();
                }
            }
            return true;
        }

        // ── 右侧 3D 预览区 — 左键拖拽平移，右键拖拽旋转 ──
        final int[] previewRect = getPreviewRect();
        if (previewRect != null)
        {
            final int prX = previewRect[0], prY = previewRect[1], prW = previewRect[2], prH = previewRect[3];
            if (mx >= prX && mx <= prX + prW && my >= prY && my <= prY + prH)
            {
                if (button == 0)
                {
                    previewDragging = true;
                    previewDragStartX = mx;
                    previewDragStartY = my;
                    return true;
                }
                if (button == 1)
                {
                    previewRotating = true;
                    previewDragStartX = mx;
                    previewDragStartY = my;
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取右侧 3D 预览区的屏幕矩形。
     * 用于判定鼠标交互。
     * @return [x, y, w, h] 或 null 如果面板不可计算
     */
    @Nullable
    private int[] getPreviewRect()
    {
        int panelW = width * CATALOG_PANEL_W_RATIO_NUM / CATALOG_PANEL_W_RATIO_DEN;
        panelW = Math.min(panelW, width - 80);
        final int panelH = height * CATALOG_PANEL_H_RATIO_NUM / CATALOG_PANEL_H_RATIO_DEN;
        final int panelX = (width - panelW) / 2;
        final int panelY = (height - panelH) / 2;

        int leftY = panelY + 26;
        int leftH = panelH - 30 - CATALOG_BOTTOM_H - 4;
        int midX = panelX + CATALOG_LEFT_W + 4;
        int midW = panelW - CATALOG_LEFT_W - CATALOG_PREVIEW_W - 16;
        int previewX = midX + midW + 4;
        int previewW = CATALOG_PREVIEW_W - 8;
        return new int[]{previewX + 4, leftY, previewW, leftH};
    }

    /**
     * 处理鼠标在 3D 预览区的拖拽平移。
     */
    public void mouseDraggedPreview(final double mx, final double my, final double dx, final double dy)
    {
        if (previewDragging)
        {
            previewPanX += (float) dx;
            previewPanY += (float) dy;
        }
    }

    /** 处理预览区的释放，停止拖拽。 */
    public void releasePreviewDrag()
    {
        previewDragging = false;
    }

    // ════════════════════════════════════════════════════════
    //  鼠标事件（由 MayorBookScreen 委托）
    // ════════════════════════════════════════════════════════

    /**
     * 处理鼠标拖拽平移/旋转。
     */
    public boolean mouseDragged(final double mx, final double my, final int button, final double dx, final double dy)
    {
        if (!catalogMode) return false;

        // 3D 预览区左键拖拽平移
        if (previewDragging && button == 0)
        {
            previewPanX += (float) dx;
            previewPanY += (float) dy;
            lastPreviewInteraction = System.currentTimeMillis();
            return true;
        }

        // 3D 预览区右键拖拽旋转
        if (previewRotating && button == 1)
        {
            previewRotY -= (float) dx * 0.5f;
            previewRotX += (float) dy * 0.5f;
            previewRotX = Math.max(-90f, Math.min(90f, previewRotX));
            lastPreviewInteraction = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标释放。
     */
    public boolean mouseReleased(final double mx, final double my, final int button)
    {
        if (previewDragging)
        {
            previewDragging = false;
            return true;
        }
        if (previewRotating)
        {
            previewRotating = false;
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
        placementNudgeMode = true;
        resetPlacementAdjustments();
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
        resetPlacementAdjustments();
        removeGhostPreview();
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

        final BlockPos finalPos = getAdjustedPlacementPos();
        final RotationMirror rot = getPlacementRotation();

        LOGGER.info("Placing blueprint '{}' at {} (nudge={},{},{} rot={})",
            entry.name(), finalPos, placementNudgeX, placementNudgeY, placementNudgeZ, rot);

        // 使用 StructurePacks 加载蓝图并放置（通过网络消息发送到服务端执行）
        NetworkHandler.CHANNEL.sendToServer(
            new BlueprintPlacementMessage(
                entry.name(),
                entry.packName(),
                entry.resourcePath(),
                finalPos,
                rot
            )
        );
        removeGhostPreview();
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
