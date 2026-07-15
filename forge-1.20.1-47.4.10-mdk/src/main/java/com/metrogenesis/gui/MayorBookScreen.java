package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.ZoneData;
import com.metrogenesis.client.ZoneRenderer;
import com.metrogenesis.construction.ZoneRect;
import com.metrogenesis.entity.RtsCameraEntity;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.blueprint.v2.BlueprintLibraryData;
import com.metrogenesis.blueprint.v2.StandardBlueprintData;
import com.metrogenesis.layergrid.LayerGrid;
import com.metrogenesis.layergrid.LayerGridSavedData;
import com.metrogenesis.layergrid.ZoneLayerBridge;
import com.metrogenesis.gov.gui.DepartmentTabPanel;
import com.metrogenesis.network.BlueprintPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.util.RotationMirror;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import com.metrogenesis.road.BezierRoad;
import com.metrogenesis.road.BezierSegment;
import com.metrogenesis.road.RoadBuilder;
import com.metrogenesis.road.RoadPlacer;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.locale.Language;

/**
 * 市长管理书 GUI — 玻璃态（Glassmorphism）城市管理模式
 *
 * 风格参考：
 * - 磨砂玻璃面板：深色半透明底 + 暖灰边缘光晕
 * - 圆角 UI 组件（模拟 fillRoundRect 实现圆角)
 * - 低饱和度、不抢眼的配色，让玩家注意力集中在 3D 画面上
 * - 柔和金高亮替代黄铜色，灰色调替代暖棕/铜色
 *
 * 核心特征：
 * 1. 游戏世界始终可见（透明背景，不暂停游戏）
 * 2. 俯视视角（虚拟相机，玩家实体飞到高空）
 * 3. WASD 移动 + 右键旋转 + 滚轮缩放
 * 4. 玻璃态 HUD：深灰半透明面板 + 圆角 + 柔和光晕
 * 5. 开启/关闭淡入淡出动画
 *
 * 布局：
 * ┌─────────────────────────────────────────────────────┐
 * │ [顶部状态栏] 城邦名 | 资金 | 人口 | 满意度 | ×      │
 * ├─────────────────────────────────────────────────────┤
 * │                                              ┌─────┐│
 * │          3D 城市鸟瞰视角                      │ 城  ││
 * │          （游戏世界可见）                      │ 市  ││
 * │                                              │ 信  ││
 * │                                              │ 息  ││
 * │                                              └─────┘│
 * ├─────────────────────────────────────────────────────┤
 * │ [底部工具栏] 住宅 | 工业 | 商业 | 农业 | 公共        │
 * └─────────────────────────────────────────────────────┘
 */
public class MayorBookScreen extends Screen {

    // ═══════════════════════════════════════════════════
    //  玻璃态调色板（灰调磨砂玻璃风格，低调不抢眼）
    // ═══════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════
    // 配色：统一引用 MGStyles（v1.2 Create 工业风 + Playlist 玻璃拟态）
    // 单一来源，便于全局改色；旧 GLASS_*/TEXT_* 名称保留以兼容现有调用。
    // ═══════════════════════════════════════════════════

    // 玻璃面板背景（半透明深色）
    public static final int GLASS_BG     = MGStyles.C_BG;
    public static final int GLASS_PANEL  = MGStyles.C_BG_PANEL;
    public static final int GLASS_HEADER = MGStyles.C_BG_PANEL;

    // 内分隔线（§4：1px #333333）
    public static final int EDGE_INNER   = 0xFF333333;

    // 按钮态
    public static final int BTN_GLASS       = MGStyles.C_BG_CARD;  // 常态（深灰卡片）
    public static final int BTN_GLASS_HOVER = MGStyles.C_BG_HOVER; // 悬浮
    public static final int BTN_GLASS_SEL   = MGStyles.C_ACCENT;   // 选中（工业橙）

    // 文字色
    public static final int TEXT_PRIMARY   = MGStyles.C_TEXT;        // 暖白主文字
    public static final int TEXT_SECONDARY = MGStyles.C_TEXT_2ND;    // 暖灰次要
    public static final int TEXT_SOFT_GOLD = MGStyles.C_TEXT_BRASS;  // 黄铜高亮
    public static final int TEXT_GREEN     = MGStyles.C_GREEN_SUCCESS;

    // 功能区色板（§3.6）
    private static final int ZONE_RESIDENTIAL = MGStyles.ZONE_RESIDENTIAL;
    private static final int ZONE_INDUSTRIAL  = MGStyles.ZONE_INDUSTRIAL;
    private static final int ZONE_COMMERCIAL  = MGStyles.ZONE_COMMERCIAL;
    private static final int ZONE_AGRICULTURE = MGStyles.ZONE_AGRICULTURE;
    private static final int ZONE_PUBLIC      = MGStyles.ZONE_PUBLIC;

    // ═══════════════════════════════════════════════════
    //  Camera State
    // ═══════════════════════════════════════════════════

    private double camX, camY, camZ;
    private float camYaw = 0f;
    private float camPitch = 65f;
    private double camHeight = 80.0;

    // 帧时间移动（参考 RTSbuilding 的 smooth camera 模式）
    private long lastFrameNanos;
    private RtsCameraEntity cameraEntity;

    // ═══════════════════════════════════════════════════
    //  Input State
    // ═══════════════════════════════════════════════════

    private boolean keyForward, keyBack, keyLeft, keyRight, keyUp, keyDown;
    private boolean keyRotateL, keyRotateR;   // Q/E 旋转视角
    private boolean rightDragging;
    private double lastDragX, lastDragY;
    private double mouseX, mouseY; // 当前鼠标位置（用于 hover 判断）

    // 操作模式状态提示（用于 ESC 分段处理）
    private boolean showingTip = false;       // 正在显示操作模式提示（用于抑制下一帧的 ESC）

    // 蓝图图鉴面板（从单体中抽取的独立组件）
    private MayorBookCatalogPanel catalogPanel;

    /** 城市状态（从服务端获取，用于持久化数据） */
    private com.metrogenesis.colony.ColonyState colonyState;

    /** 是否需要先选风格包（首次打开时判断） */
    private boolean needsStylePackSelection = false;

    // ═══ 部门系统 GUI 面板 ══════════════════════════════

    /** 部门面板容器（G 键切换） */
    private DepartmentTabPanel deptPanel = new DepartmentTabPanel();

    // ═══════════════════════════════════════════════════
    //  需求面板状态
    // ═══════════════════════════════════════════════════

    /** 是否显示独立的需求面板（从图鉴中独立出来的） */
    private boolean showingDemands = false;

    // ═══════════════════════════════════════════════════
    //  Player State Backup
    // ═══════════════════════════════════════════════════

    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;
    private boolean savedFlying, savedMayfly;
    private boolean savedInvisible;
    private float savedFallDistance;
    private net.minecraft.world.entity.Entity savedCameraEntity; // 原来的相机实体

    // ═══════════════════════════════════════════════════
    //  Zone Selection
    // ═══════════════════════════════════════════════════

    private boolean zoneMode = false;        // Z 键/按钮切换区域绘制模式
    private int selectedZone = -1;
    private boolean zoneDrawing = false;     // 正在拖拽绘制矩形
    private int drawStartX, drawStartZ;      // 绘制起点（世界坐标）
    private int drawEndX, drawEndZ;          // 绘制终点（世界坐标，实时更新）
    /** 区域类型选择器 UI 状态 */
    private int zoneBtnTop = 0;              // 第一个类型按钮的 y 坐标
    private int hoveredZoneBtn = -1;         // 鼠标悬停的类型按钮索引（-1=无）
    private final int[][] zoneBtnBounds = new int[5][4]; // [x, y, w, h] 每个类型按钮的矩形

    /** 区域参数 popup 状态：区域按钮在 zoneMode 时点击 → 向上弹出纵向列表（含密度 3 行 + 分割线 + 所有权 2 行） */
    private enum ParamPopup { NONE, ZONE_PANEL }
    private ParamPopup paramPopup = ParamPopup.NONE;
    private int paramPopupHoverRow = -1;     // popup 内鼠标悬停行（-1=无）
    /** paramPopupRowBounds 结构：[title x,y,w,h, row0 x,y,w,h, row1 x,y,w,h, ...]
     *  密度 3 行 + 分割行 1（不可点）+ 所有权 2 行 = 共 6 行（最多 6*4+4 = 28 个 int，扩到 32 安全） */
    private final int[] paramPopupRowBounds = new int[32];

    // 城市命名编辑状态（① 市长书内开城/重命名）
    private boolean editingName = false;
    private String nameBuffer = "";
    private final int[] nameEditBtnBounds = new int[]{0, 0, 0, 0};
    private final int[] nameInputBounds   = new int[]{0, 0, 0, 0};
    private final int[] nameConfirmBounds = new int[]{0, 0, 0, 0};
    private final int[] nameCancelBounds  = new int[]{0, 0, 0, 0};

    /** 当前选中的区域密度（在底栏显示切换按钮） */
    private int zoneDensity = ZoneRect.DENSITY_MEDIUM;    // 默认中密度
    /** 当前选中的区域所有权（在底栏显示切换按钮） */
    private int zoneOwnership = ZoneRect.OWNERSHIP_PRIVATE; // 默认私有

    private static final int ZONE_GRID = 1; // 方块级对齐

    private static final String[] ZONE_TYPES = {
        "residential", "industrial", "commercial", "agriculture", "public"
    };

    private static final int[] ZONE_COLORS = {
        ZONE_RESIDENTIAL, ZONE_INDUSTRIAL, ZONE_COMMERCIAL, ZONE_AGRICULTURE, ZONE_PUBLIC
    };

    private static final String[] ZONE_ICONS = {
        "\u2302", // ⌂
        "\u2692", // ⚒
        "\u2696", // ⚖
        "\u2698", // ⚘
        "\u2691", // ⚑
    };

    // ═══════════════════════════════════════════════════
    //  Zone Areas（矩形功能区区域，参考 SimCity 分区机制）
    // ═══════════════════════════════════════════════════

    /** 区划建造阶段常量（委派至 ZoneRect） */
    public static final int STAGE_PLANNING  = ZoneRect.STAGE_PLANNING;
    public static final int STAGE_PENDING   = ZoneRect.STAGE_PENDING;
    public static final int STAGE_BUILDING  = ZoneRect.STAGE_BUILDING;
    public static final int STAGE_COMPLETED = ZoneRect.STAGE_COMPLETED;

    private final List<ZoneRect> zoneRects = new ArrayList<>();
    private static final int MAX_ZONES = 200;

    // ═══════════════════════════════════════════════════
    //  Road Drawing
    // ═══════════════════════════════════════════════════

    private boolean roadMode = false;            // R 键切换道路绘制模式
    private boolean roadDrawing = false;         // 正在拖拽画路
    private boolean roadPending = false;         // 松手后待确认（投影状态）
    private int roadStartBX, roadStartBZ;        // 起点方块坐标
    private int roadEndBX, roadEndBZ;            // 终点方块坐标（实时更新）
    private int pendingFromBX, pendingFromBZ;    // 待确认道路起点
    private int pendingToBX, pendingToBZ;        // 待确认道路终点

    private float roadCurvature = 0f;            // 当前曲率 [-1, 1]
    private static final float CURVATURE_STEP = 0.05f;  // 每次滚轮步进
    private boolean curvedRoad = false;          // false=直道模式，true=弯道模式
    private int[] roadSnapTarget = null;         // 当前吸附目标节点坐标（渲染高亮用）

    // ═══════════════════════════════════════════════════
    //  Blueprint Catalog — 由 MayorBookCatalogPanel 托管
    // ═══════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════
    //  Animation State（Modern UI 启发的平滑动画）
    // ═══════════════════════════════════════════════════

    private long openTime;          // 打开时间戳
    private float fadeInAlpha = 0f; // 淡入进度 0→1
    private static final long FADE_DURATION_MS = 200; // 淡入持续时间（毫秒）

    // ═══════════════════════════════════════════════════
    //  Constants
    // ═══════════════════════════════════════════════════

    private static final double MOVE_SPEED = 0.5;
    private static final double MOUSE_SENSITIVITY = 0.35;
    private static final double SCROLL_SPEED = 5.0;
    private static final double MIN_HEIGHT = 15.0;
    private static final double MAX_HEIGHT = 250.0;

    // HUD 布局（Create 风格，偶数尺寸）
    private static final int TOP_BAR_H     = 24;
    private static final int BOTTOM_BAR_H  = 44;
    private static final int RIGHT_PANEL_W = 160;
    private static final int PIXEL_BORDER  = 2; // 边框厚度

    // ═══════════════════════════════════════════════════
    //  Constructor
    // ═══════════════════════════════════════════════════

    public MayorBookScreen() {
        super(Component.translatable("gui.metrogenesis.mayor_book.title"));
    }

    // ═══════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        Player player = minecraft.player;
        if (player == null) return;

        // U1/U2 修复：打开市长书时主动拉取最新城市数据（避免顶栏/右侧显示陈旧或硬编码值）
        com.metrogenesis.network.SyncCityDataMessage.requestRefresh();

        openTime = System.currentTimeMillis();
        fadeInAlpha = 0f;
        lastFrameNanos = System.nanoTime();

        // 保存玩家状态
        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedYaw = player.getYRot();
        savedPitch = player.getXRot();
        savedFlying = player.getAbilities().flying;
        savedMayfly = player.getAbilities().mayfly;
        savedInvisible = player.isInvisible();
        savedFallDistance = player.fallDistance;
        savedCameraEntity = minecraft.getCameraEntity();

        // 初始化相机位置
        camX = savedX;
        camY = savedY + camHeight;
        camZ = savedZ;
        camYaw = 0f;
        camPitch = 65f;

        // 创建独立 RTS 相机实体
        if (minecraft.level != null) {
            cameraEntity = new RtsCameraEntity(MetroGenesis.RTS_CAMERA.get(), minecraft.level);
            cameraEntity.snapTo(camX, camY, camZ, camYaw, camPitch);
            // 切换到独立相机视角
            minecraft.setCameraEntity(cameraEntity);
        }

        // 玩家隐藏、飞行
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.setInvisible(true);
        player.onUpdateAbilities();

        // 创建蓝图图鉴面板
        catalogPanel = new MayorBookCatalogPanel(width, height, font, this);

        // 加载已保存的分区数据
        loadZonesFromWorld();

        // ══ 初始化城市风格包状态 ═══════════════
        initStylePackState();
    }

    /**
     * 检查城市是否已选择风格包，如未选择则在打开时强制选包。
     */
    private void initStylePackState() {
        try {
            if (minecraft.getSingleplayerServer() != null && minecraft.level != null) {
                var serverLevel = minecraft.getSingleplayerServer()
                    .getLevel(minecraft.level.dimension());
                if (serverLevel != null) {
                    colonyState = com.metrogenesis.colony.ColonyState.get(serverLevel);
                    if (colonyState.getActiveStylePack() == null) {
                        needsStylePackSelection = true;
                        MetroGenesis.LOGGER.info("[StylePack] No active style pack, forcing selection on first open");
                    } else {
                        MetroGenesis.LOGGER.info("[StylePack] Active style pack: {}", colonyState.getActiveStylePack());
                    }
                }
            }
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("[StylePack] Failed to init style pack state", e);
        }
    }

    /**
     * 保存选中的风格包到城市状态。
     * <p>
     * 通过 {@link FoundCityMessage} 发往服务端，确保 {@link ColonyState#setDirty}
     * 在服务端线程调用，避免客户端直接修改 SavedData 的线程安全问题导致持久化失败。
     */
    public void setActiveStylePack(String packName) {
        if (colonyState != null) {
            colonyState.setActiveStylePack(packName); // 客户端立即生效（UI 反馈）
            needsStylePackSelection = false;
            MetroGenesis.LOGGER.info("[StylePack] Set active style pack: {}", packName);

            // 发往服务端确保持久化（FoundCityMessage 内部调用 state.setActiveStylePack + setDirty）
            String currentName = nameBuffer != null ? nameBuffer.toString().trim() : "";
            if (currentName.isEmpty()) {
                String cn = colonyState.getCityName();
                if (!cn.equals("未命名") && !cn.equals("新城邦")) currentName = cn;
            }
            com.metrogenesis.network.NetworkHandler.CHANNEL.sendToServer(
                new com.metrogenesis.network.FoundCityMessage(currentName, packName));

            // 选择风格包后关闭图鉴面板，返回城市管理模式
            if (catalogPanel != null) {
                catalogPanel.close();
            }
        }
    }

    /**
     * 获取当前选择的风格包名称。
     * @return 风格包名称，如果未选择则返回 null
     */
    public String getActiveStylePack() {
        return colonyState != null ? colonyState.getActiveStylePack() : null;
    }

    /**
     * 从世界存档加载分区数据（通过 ZoneData，保留矩形边界）。
     * 加载后同步到 LayerGrid 供运行时冲突检测使用。
     */
    private void loadZonesFromWorld() {
        try {
            if (minecraft.getSingleplayerServer() != null && minecraft.level != null) {
                var serverLevel = minecraft.getSingleplayerServer()
                    .getLevel(minecraft.level.dimension());
                if (serverLevel != null) {
                    // 从 ZoneData 加载（矩形边界无损
                    com.metrogenesis.ZoneData zoneData = com.metrogenesis.ZoneData.getOrCreate(serverLevel);
                    zoneRects.clear();
                    for (int[] z : zoneData.getZones()) {
                        int dir = z.length >= 6 ? z[5] : 0; // 兼容旧存档无 direction
                        int stage = z.length >= 7 ? z[6] : 0;
                        int density = z.length >= 8 ? z[7] : ZoneRect.DENSITY_MEDIUM;
                        int ownership = z.length >= 9 ? z[8] : ZoneRect.OWNERSHIP_PRIVATE;
                        zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], dir, stage, density, ownership));
                    }
                    MetroGenesis.LOGGER.info("Loaded {} zones from ZoneData", zoneRects.size());
                    // 同步到 LayerGrid（供运行时查询）
                    syncZoneRectsToLayerGrid(serverLevel);
                }
            }
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("Failed to load zones", e);
        }
    }

    @Override
    public void removed() {
        // 清除 3D 渲染器状态
        ZoneRenderer.setHoverBlock(Integer.MIN_VALUE, Integer.MIN_VALUE);
        ZoneRenderer.setPreviewZone(null);
        ZoneRenderer.setActiveZones(List.of());
        ZoneRenderer.setRoadCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, 0);
        ZoneRenderer.clearRoadPreview();  // Phase 2: 关闭 BlueprintRenderer 释放 GL 资源
        com.metrogenesis.client.RoadRenderer.clearPreview();

        Player player = minecraft.player;
        if (player == null) return;

        // 保存分区数据到世界存档
        saveZonesToWorld();

        // 恢复相机到玩家
        if (savedCameraEntity != null && savedCameraEntity.isAlive()) {
            minecraft.setCameraEntity(savedCameraEntity);
        } else {
            minecraft.setCameraEntity(player);
        }

        // 清理 RTS 相机实体
        if (cameraEntity != null && cameraEntity.isAlive()) {
            cameraEntity.discard();
        }

        // 恢复玩家状态
        player.setPos(savedX, savedY, savedZ);
        player.setYRot(savedYaw);
        player.setXRot(savedPitch);
        player.getAbilities().flying = savedFlying;
        player.getAbilities().mayfly = savedMayfly;
        player.setInvisible(savedInvisible);
        player.fallDistance = savedFallDistance;
        player.onUpdateAbilities();
    }

    /**
     * 保存分区数据到世界存档。
     * <p>
     * ZoneData 是主存储（保留矩形边界），LayerGrid 作为运行时查询同步更新。
     * 不从 LayerGrid -> syncToZoneData 读取（会丢失矩形边界）。
     * </p>
     */
    private void saveZonesToWorld() {
        try {
            if (minecraft.getSingleplayerServer() != null && minecraft.level != null) {
                var serverLevel = minecraft.getSingleplayerServer()
                    .getLevel(minecraft.level.dimension());
                if (serverLevel != null) {
                    // 构建保存列表
                    List<int[]> saveList = new ArrayList<>();
                    for (ZoneRect r : zoneRects) {
                        saveList.add(new int[]{r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), r.direction(), r.stage(), r.density(), r.ownership()});
                    }

                    // 写入 ZoneData（主存储，矩形边界无损）
                    ZoneData zoneData = ZoneData.getOrCreate(serverLevel);
                    zoneData.setZones(saveList);
                    MetroGenesis.LOGGER.info("Saved {} zones to ZoneData", saveList.size());

                    // 同步到 LayerGrid（运行时查询用）
                    syncZoneRectsToLayerGrid(serverLevel);
                }
            }
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("Failed to save zones", e);
        }
    }

    /**
     * G 键触发：提交所有待建区划到服务端启动生长。
     * <p>
     * 遍历 {@link #zoneRects}，将处于 {@code PLANNING} / {@code PENDING} 的区划
     * 前置到 {@code PENDING} 阶段，通过 {@link com.metrogenesis.network.RequestZoneGrowthMessage}
     * 逐一发往服务端 {@link com.metrogenesis.construction.ZoneBuilder#startGrowth}。
     */
    private void sendPendingZonesForGrowth() {
        if (minecraft.getSingleplayerServer() == null && minecraft.getConnection() == null) return;

        int sentCount = 0;
        for (ZoneRect zone : zoneRects) {
            int stage = zone.stage();
            if (stage >= ZoneRect.STAGE_BUILDING) continue; // 已在建或已完成

            // 前置到 PENDING
            if (stage < ZoneRect.STAGE_PENDING) {
                zone.setStage(ZoneRect.STAGE_PENDING);
            }

            com.metrogenesis.network.NetworkHandler.CHANNEL.sendToServer(
                new com.metrogenesis.network.RequestZoneGrowthMessage(zone));
            sentCount++;
        }

        if (sentCount > 0) {
            saveZonesToWorld();
            MetroGenesis.LOGGER.info("[MayorBook] Sent {} zone(s) to server for growth", sentCount);
        }
    }

    /**
     * 将 zoneRects 全量同步到 LayerGrid。
     * <p>
     * 先清空所有桥接层，再逐矩形 claim。
     * </p>
     */
    private void syncZoneRectsToLayerGrid(net.minecraft.server.level.ServerLevel serverLevel) {
        try {
            com.metrogenesis.layergrid.LayerGridSavedData gridData =
                com.metrogenesis.layergrid.LayerGridSavedData.getOrCreate(serverLevel);
            com.metrogenesis.layergrid.LayerGrid layerGrid = gridData.getLayerGrid();

            // 清空桥接层
            for (int t = 0; t < 5; t++) {
                String ln = com.metrogenesis.layergrid.ZoneLayerBridge.getLayerNameForZoneType(t);
                if (ln != null) {
                    com.metrogenesis.layergrid.Layer existing = layerGrid.getLayer(ln);
                    if (existing != null) {
                        // 复制列表避免并发修改
                        java.util.List<net.minecraft.core.BlockPos> positions =
                            new java.util.ArrayList<>(existing.getPositions());
                        for (net.minecraft.core.BlockPos p : positions) {
                            layerGrid.release(p.getX(), p.getZ(), ln);
                        }
                    }
                }
            }

            // 全量写入
            for (ZoneRect r : zoneRects) {
                String layerName = com.metrogenesis.layergrid.ZoneLayerBridge.getLayerNameForZoneType(r.zoneType());
                if (layerName != null) {
                    if (layerGrid.getLayer(layerName) == null) {
                        layerGrid.createLayer(layerName, "__bridge__");
                    }
                    for (int x = r.minX(); x <= r.maxX(); x++) {
                        for (int z = r.minZ(); z <= r.maxZ(); z++) {
                            layerGrid.claim(x, z, layerName);
                        }
                    }
                }
            }
            gridData.setDirty();
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("Failed to sync zones to LayerGrid", e);
        }
    }

    @Override
    public void tick() {
        // 更新淡入动画
        long elapsed = System.currentTimeMillis() - openTime;
        fadeInAlpha = Math.min(1f, (float) elapsed / FADE_DURATION_MS);

        // 不在此处处理相机移动 — 由 render() 按帧时间处理
        // 保持玩家在原位（不可见）
        Player player = minecraft.player;
        if (player != null) {
            player.setPos(savedX, savedY, savedZ); // 玩家不动
        }
    }

    // ═══════════════════════════════════════════════════
    //  Rendering
    // ═══════════════════════════════════════════════════

    @Override
    public void renderBackground(GuiGraphics g) {
        // 透明背景 — 游戏世界可见
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.mouseX = mx;
        this.mouseY = my;

        // 参数 popup hover 实时更新（仅在 popup 开着时计算，避免无效 hit-test）
        if (paramPopup != ParamPopup.NONE) {
            paramPopupHoverRow = paramPopupRowAt(mx, my);
        }

        // ═════════════════════════════════════════════════
        // 帧时间移动（参考 RTSbuilding smooth mode）
        // 按帧时间计算移动量，而非按 tick — 无论帧率如何都丝滑
        // 图鉴/放置模式下 — 根据 NUDGE 模式切换是否允许相机移动
        // ═════════════════════════════════════════════════
        long now = System.nanoTime();
        float tickDelta = (now - lastFrameNanos) / 50_000_000.0f; // 50ms = 1 tick
        tickDelta = Math.min(tickDelta, 2.0f); // 上限防止跳帧
        lastFrameNanos = now;

        final boolean canMoveCam = catalogPanel == null
            || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode())  // 非图鉴非放置
            || (catalogPanel.isPlacementMode() && !catalogPanel.isPlacementNudgeMode()); // 放置+CAM模式
        if (canMoveCam) {
            // 计算本帧移动量
            double speed = MOVE_SPEED * tickDelta;
            double radYaw = Math.toRadians(camYaw);
            double fwdX = -Math.sin(radYaw) * speed;
            double fwdZ =  Math.cos(radYaw) * speed;
            double rgtX = -Math.cos(radYaw) * speed;
            double rgtZ = -Math.sin(radYaw) * speed;

            if (keyForward)  { camX += fwdX; camZ += fwdZ; }
            if (keyBack)     { camX -= fwdX; camZ -= fwdZ; }
            if (keyLeft)     { camX -= rgtX; camZ -= rgtZ; }
            if (keyRight)    { camX += rgtX; camZ += rgtZ; }
            if (keyUp)       { camY += speed; }
            if (keyDown)     { camY -= speed; }

            // Q/E 键盘旋转
            float ROTATE_SPEED = 3.0f * tickDelta;
            if (keyRotateL)  camYaw -= ROTATE_SPEED;
            if (keyRotateR)  camYaw += ROTATE_SPEED;
        }

        camY = Math.max(savedY + MIN_HEIGHT, Math.min(savedY + MAX_HEIGHT, camY));

        // snapTo() 同时设置当前位置和旧位置，禁用 MC 内置插值
        if (cameraEntity != null) {
            cameraEntity.snapTo(camX, camY, camZ, camYaw, camPitch);
        }

        // 同步分区数据到 3D 世界渲染器（ZoneRenderer）
        List<int[]> zoneList = new ArrayList<>();
        for (ZoneRect r : zoneRects) {
            zoneList.add(new int[]{r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), r.direction()});
        }
        ZoneRenderer.setActiveZones(zoneList);

        // 同步拖拽预览区域到 ZoneRenderer
        if (zoneDrawing && selectedZone >= 0) {
            int minX = snapToGrid(Math.min(drawStartX, drawEndX));
            int minZ = snapToGrid(Math.min(drawStartZ, drawEndZ));
            int maxX = snapToGrid(Math.max(drawStartX, drawEndX)) + 1;
            int maxZ = snapToGrid(Math.max(drawStartZ, drawEndZ)) + 1;
            ZoneRenderer.setPreviewZone(new int[]{minX, minZ, maxX, maxZ, selectedZone});
        } else {
            ZoneRenderer.setPreviewZone(null);
        }

        // 计算鼠标悬停的 chunk（用于高亮），仅当鼠标在 3D 视区内
        boolean in3DView = !inTopBar(mouseX, mouseY) && !inBottomBar(mouseX, mouseY) && !inRightPanel(mouseX, mouseY);
        if (in3DView) {
            BlockPos pos = screenToWorld(mouseX, mouseY);
            if (pos != null) {
                ZoneRenderer.setHoverBlock(snapToGrid(pos.getX()), snapToGrid(pos.getZ()));
            } else {
                ZoneRenderer.setHoverBlock(Integer.MIN_VALUE, Integer.MIN_VALUE);
            }
        } else {
            ZoneRenderer.setHoverBlock(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        // 道路模式下更新光标十字线
        BlockPos cursorPos = screenToWorld(mouseX, mouseY);
        if (roadMode && cursorPos != null) {
            int w = com.metrogenesis.road.RoadTemplateManager.getInstance().getActiveTemplate().getWidth();
            ZoneRenderer.setRoadCursor(snapToGrid(cursorPos.getX()), snapToGrid(cursorPos.getZ()), w);
        } else {
            ZoneRenderer.setRoadCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, 0);
        }

        // 同步道路绘制预览到 RoadRenderer
        // 直道模式用 setPreview()，弯道模式用 setBezierPreview()
        Level clientLevel = minecraft != null ? minecraft.level : null;
        if (roadMode && roadDrawing) {
            if (curvedRoad) {
                // 弯道模式：贝塞尔曲线预览（使用当前曲率，实时跟随滚轮调整）
                com.metrogenesis.client.RoadRenderer.setBezierPreview(clientLevel,
                    roadStartBX, roadStartBZ, roadEndBX, roadEndBZ, roadCurvature, false);
            } else {
                // 直道模式：强制对齐到主轴（X 或 Z 取较大差值方向）
                int dx = Math.abs(roadEndBX - roadStartBX);
                int dz = Math.abs(roadEndBZ - roadStartBZ);
                int alignedEndBX = dx >= dz ? roadEndBX : roadStartBX;
                int alignedEndBZ = dx >= dz ? roadStartBZ : roadEndBZ;
                com.metrogenesis.client.RoadRenderer.setPreview(
                    roadStartBX, roadStartBZ, alignedEndBX, alignedEndBZ, false);
            }
        } else if (roadMode && roadPending) {
            if (curvedRoad) {
                // 弯道模式：贝塞尔曲线投影（使用当前曲率）
                com.metrogenesis.client.RoadRenderer.setBezierPreview(clientLevel,
                    pendingFromBX, pendingFromBZ, pendingToBX, pendingToBZ, roadCurvature, true);
            } else {
                // 直道模式：直线投影
                com.metrogenesis.client.RoadRenderer.setPreview(
                    pendingFromBX, pendingFromBZ, pendingToBX, pendingToBZ, true);
            }
        } else {
            com.metrogenesis.client.RoadRenderer.clearPreview();
        }

        // 吸附节点高亮 — 渲染一个金色半透明方块在吸附目标位置
        if (roadSnapTarget != null && roadDrawing) {
            ZoneRenderer.setSnapHighlight(roadSnapTarget[0], roadSnapTarget[1]);
        } else {
            ZoneRenderer.setSnapHighlight(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        // ═══ 道路节点空间地图预览：当进入 R 道路模式时，高亮所有已有节点 ═══
        if (roadMode && minecraft.getSingleplayerServer() != null && minecraft.level != null) {
            var sr = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
            if (sr != null) {
                var roadData = com.metrogenesis.RoadData.getOrCreate(sr);
                var allNodes = roadData.getAllNodes();
                int[] coords = new int[allNodes.size() * 2];
                int idx = 0;
                for (var node : allNodes) {
                    coords[idx++] = node.blockX();
                    coords[idx++] = node.blockZ();
                }
                ZoneRenderer.setRoadNodes(coords);
            }
        } else {
            ZoneRenderer.setRoadNodes(null);
        }
        // HUD 曲率显示（道路模式下显示当前模式和曲率）
        if (roadMode) {
            String modeText = curvedRoad ? Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.001") : Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.002");
            String curvText = curvedRoad
                ? String.format(Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.003"), modeText, roadCurvature)
                : String.format(Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.004"), modeText);
            drawGlassText(g, curvText, 8, height - BOTTOM_BAR_H - 44, TEXT_SOFT_GOLD, 255);
        }

        // 应用淡入动画（整体透明度）
        int alpha = (int) (fadeInAlpha * 255) & 0xFF;

        // 如果城市未选风格包，强制打开图鉴选包
        if (needsStylePackSelection && catalogPanel != null) {
            if (!catalogPanel.isCatalogMode()) {
                catalogPanel.open();
            }
            // 仍在选包模式，渲染图鉴首页后返回（不渲染城市视图）
            catalogPanel.render(g, alpha);
            return;
        }

        // 图鉴面板模式（含放置确认 + 拖拽定位）
        if (catalogPanel != null) {
            catalogPanel.setCameraYaw(camYaw);
            catalogPanel.render(g, alpha);
            if (catalogPanel.isCatalogMode()) {
                if (catalogPanel.isPlacementMode() || catalogPanel.isPlacementConfirm()) {
                    renderCrosshair(g, mx, my);
                }
                return;
            }
        }

        // 需求面板（独立于图鉴的覆盖层）
        if (showingDemands) {
            renderDemandsOverlay(g, alpha);
            return;
        }

        // 部门面板（覆盖层，跳过所有下层 GUI 元素）
        if (deptPanel != null && deptPanel.isVisible()) {
            deptPanel.render(g, mx, my, partialTick, alpha, this);
            return;
        }

        // ══ 扫描中：在城市场景上覆盖半透明遮罩，提示用户等待 ══
        final boolean scanning = catalogPanel != null && catalogPanel.isScanning();
        if (scanning) {
            fillGlassPanel(g, 0, 0, width, height - BOTTOM_BAR_H, BTN_GLASS, (int)(alpha * 0.55f));
            String loadingMsg = Language.getInstance().getOrDefault("gui.metrogenesis.catalog.003");
            drawGlassText(g, loadingMsg, 10, height - BOTTOM_BAR_H - 20, TEXT_SOFT_GOLD, alpha);
        }

        // 正常城市管理模式
        // 顶部状态栏
        renderTopBar(g, alpha);

        // 底部工具栏
        renderBottomToolbar(g, alpha);

        // 计算右侧面板区域类型按钮的悬停索引
        updateZoneBtnHover(mx, my);

        // 右侧信息面板
        renderRightPanel(g, alpha);

        // 左下角坐标
        renderCoordinates(g, alpha);

        // Create 风格十字准星
        renderCrosshair(g, mx, my);

        // 部门面板（覆盖层，始终在最上层）
        deptPanel.render(g, mx, my, partialTick, alpha, this);
    }

    /**
     * 顶部状态栏 — Create 风格黄铜边框
     */
    private void renderTopBar(GuiGraphics g, int alpha) {
        int h = TOP_BAR_H;

        // 背景
        fillGlassPanel(g, 0, 0, width, h, GLASS_HEADER, alpha);
        // v1.2 金色粒子点缀（低 alpha，不遮挡文字）
        MGStyles.drawParticles(g, 0, 0, width, h, System.currentTimeMillis(), alpha / 2);

        // 城市名称
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.005"), 8, 7, TEXT_SOFT_GOLD, alpha);

        // 分隔线
        g.fill(85, 3, 86, h - 3, withAlpha(EDGE_INNER, alpha));

        // 指标（U1 修复：读真实城市数据，不再硬编码 0）
        int cx = width / 2;
        final var city = com.metrogenesis.network.SyncCityDataMessage.getCachedData();
        String fundsStr = city != null ? (city.funds + " C-Value") : "0 C-Value";
        String popStr = city != null ? (city.population + "/" + city.maxPopulation) : "0/0";
        String satStr = "—"; // G1: 满意度城市级聚合尚未实现，暂显示占位
        drawStatGlass(g, cx - 130, 7, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.006"), fundsStr, TEXT_SOFT_GOLD, alpha);
        drawStatGlass(g, cx - 20, 7, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.007"), popStr, TEXT_GREEN, alpha);
        drawStatGlass(g, cx + 70, 7, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.008"), satStr, TEXT_PRIMARY, alpha);

        // 关闭按钮
        int closeX = width - 22;
        int closeY = 3;
        boolean hover = inBounds(closeX, closeY, 18, 18);
        int closeBg = hover ? BTN_GLASS_HOVER : BTN_GLASS;
        fillGlassButton(g, closeX, closeY, 18, 18, closeBg, alpha);
        drawGlassText(g, "\u00d7", closeX + 5, closeY + 5, TEXT_PRIMARY, alpha);
    }

    /**
     * 底部工具栏 — 玻璃态功能按钮
     * 6 个功能：道路、图鉴、区域、需求、地图、部门
     */
    private void renderBottomToolbar(GuiGraphics g, int alpha) {
        final int barY = height - BOTTOM_BAR_H;

        // 玻璃背景
        fillGlassPanel(g, 0, barY, width, BOTTOM_BAR_H, GLASS_HEADER, alpha);

        int btnW = 64;
        int btnH = 28;
        int gap = 4;
        int btnY = barY + 2;

        // 6 按钮固定居中布局（不再因 zoneMode 漂移）— 参数 popup 从区域按钮向上弹
        int availableWidth = width - RIGHT_PANEL_W;
        int startX = (availableWidth - 6 * (btnW + gap)) / 2;

        // ═══ 道路按钮（R 键）═══
        int rx = startX;
        boolean roadHover = inBounds(rx, btnY, btnW, btnH);
        int roadBg = roadMode ? BTN_GLASS_SEL : (roadHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, rx, btnY, btnW, btnH, roadBg, alpha);
        if (roadMode) {
            g.fill(rx + 4, btnY + btnH - 2, rx + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "R", rx + 3, btnY + 2, roadMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.009"), rx + 8, btnY + btnH / 2 + 2, roadMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // ═══ 图鉴按钮（T 键）═══
        int catX = rx + btnW + gap;
        boolean catHover = inBounds(catX, btnY, btnW, btnH);
        boolean catMode = catalogPanel != null && catalogPanel.isCatalogMode();
        int catBg = catMode ? BTN_GLASS_SEL : (catHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, catX, btnY, btnW, btnH, catBg, alpha);
        if (catMode) {
            g.fill(catX + 4, btnY + btnH - 2, catX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "T", catX + 3, btnY + 2, catMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.010"), catX + 8, btnY + btnH / 2 + 2, catMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // ═══ 区域按钮（Z 键）— 区域模式下追加 ▼ 提示，点击可弹出参数 popup ═══
        int zoneX = catX + btnW + gap;
        boolean zoneHover = inBounds(zoneX, btnY, btnW, btnH);
        boolean zoneBtnMode = zoneMode;
        boolean zonePopupOpen = paramPopup == ParamPopup.ZONE_PANEL;
        int zoneBg = zonePopupOpen ? BTN_GLASS_SEL : (zoneBtnMode ? BTN_GLASS_SEL : (zoneHover ? BTN_GLASS_HOVER : BTN_GLASS));
        fillGlassButton(g, zoneX, btnY, btnW, btnH, zoneBg, alpha);
        if (zoneBtnMode) {
            g.fill(zoneX + 4, btnY + btnH - 2, zoneX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "Z", zoneX + 3, btnY + 2, zoneBtnMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        String zoneLabel = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.011")
            + (zoneBtnMode ? (zonePopupOpen ? " \u25b2" : " \u25be") : "");
        drawGlassText(g, zoneLabel, zoneX + 8, btnY + btnH / 2 + 2, zoneBtnMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // 6 按钮布局恢复：不再因 zoneMode 漂移（参数 popup 改为从区域按钮向上弹）
        int demX = zoneX + btnW + gap;
        int mapX, govX;

        // ═══ 区域参数 popup 渲染（向上弹出纵向玻璃列表：密度 3 行 + 分割 + 所有权 2 行）═══
        if (zoneMode && paramPopup == ParamPopup.ZONE_PANEL) {
            renderParamPopup(g, alpha, zoneX, btnW, btnY, btnH);
        }

        // ═══ 需求面板按钮（B 键）═══
        boolean demHover = inBounds(demX, btnY, btnW, btnH);
        boolean demMode = showingDemands;
        int demBg = demMode ? BTN_GLASS_SEL : (demHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, demX, btnY, btnW, btnH, demBg, alpha);
        if (demMode) {
            g.fill(demX + 4, btnY + btnH - 2, demX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "B", demX + 3, btnY + 2, demMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.017"), demX + 8, btnY + btnH / 2 + 2, demMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // ═══ 地图按钮（M 键）═══
        mapX = demX + btnW + gap;
        boolean mapHover = inBounds(mapX, btnY, btnW, btnH);
        int mapBg = mapHover ? BTN_GLASS_HOVER : BTN_GLASS;
        fillGlassButton(g, mapX, btnY, btnW, btnH, mapBg, alpha);
        drawGlassText(g, "M", mapX + 3, btnY + 2, TEXT_SECONDARY, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.018"), mapX + 8, btnY + btnH / 2 + 2, TEXT_SECONDARY, alpha);

        // ═══ 部门按钮（G 键）═══
        govX = mapX + btnW + gap;
        boolean govHover = inBounds(govX, btnY, btnW, btnH);
        boolean govMode = deptPanel != null && deptPanel.isVisible();
        int govBg = govMode ? BTN_GLASS_SEL : (govHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, govX, btnY, btnW, btnH, govBg, alpha);
        if (govMode) {
            g.fill(govX + 4, btnY + btnH - 2, govX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "G", govX + 3, btnY + 2, govMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.019"), govX + 8, btnY + btnH / 2 + 2, govMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // ═══ 底部操作提示 ═══
        String modeHint;
        if (zoneMode && selectedZone >= 0 && zoneDrawing) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.020");
        } else if (zoneMode && selectedZone >= 0) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.021");
        } else if (zoneMode) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.022");
        } else if (roadMode && roadPending) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.023");
        } else if (roadMode) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.024");
        } else if (showingDemands) {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.025");
        } else {
            modeHint = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.026");
        }
        drawGlassText(g, modeHint, 4, height - 14, TEXT_SECONDARY, alpha);
    }

    /**
     * 渲染城市需求面板（独立覆盖层）。
     * 从图鉴中独立出来的，按 D 键或点击需求按钮打开。
     */
    private void renderDemandsOverlay(GuiGraphics g, int alpha) {
        final int pw = 340;
        final int ph = Math.min(height - 40, 400);
        final int px = (width - pw) / 2;
        final int py = (height - ph) / 2;

        // 背景
        fillGlassPanel(g, px, py, pw, ph, GLASS_BG, alpha);

        // 标题栏
        fillGlassPanel(g, px, py, pw, 22, GLASS_HEADER, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.027"), px + 8, py + 6, TEXT_SOFT_GOLD, alpha);

        // 关闭按钮
        final boolean closeHover = inBounds(px + pw - 18, py + 3, 14, 14);
        fillGlassButton(g, px + pw - 18, py + 3, 14, 14, closeHover ? BTN_GLASS_HOVER : BTN_GLASS, alpha);
        drawGlassText(g, "\u00d7", px + pw - 15, py + 5, TEXT_PRIMARY, alpha);

        // 刷新按钮
        final boolean refHover = inBounds(px + pw - 36, py + 3, 14, 14);
        fillGlassButton(g, px + pw - 36, py + 3, 14, 14, refHover ? BTN_GLASS_HOVER : BTN_GLASS, alpha);
        drawGlassText(g, "\u21BB", px + pw - 34, py + 5, TEXT_PRIMARY, alpha);

        // 内容区
        final var data = com.metrogenesis.network.SyncCityDataMessage.getCachedData();
        if (data == null)
        {
            final String msg = Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.028");
            drawGlassText(g, msg, px + pw / 2 - font.width(msg) / 2, py + ph / 2 - 4, TEXT_SECONDARY, alpha);
            return;
        }

        int contentX = px + 12;
        int contentY = py + 30;
        int contentW = pw - 24;
        int colW = contentW / 3 - 8;
        int topY = contentY;

        // ══ 三栏指标卡片 ══
        renderDemandCard(g, contentX, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.029"),
            data.population + " / " + data.maxPopulation,
            data.population > 0 ? TEXT_GREEN : 0xFFCC8888, alpha);

        renderDemandCard(g, contentX + colW + 4, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.030"),
            data.funds + " C",
            data.funds > 0 ? 0xFFCCCC88 : 0xFFCC8888, alpha);

        renderDemandCard(g, contentX + (colW + 4) * 2, topY, colW, 40, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.031"),
            data.activeConstructionSites + Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.032") + data.unclaimedSites + Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.033"),
            0xFF88AACC, alpha);

        topY += 48;

        // ══ 城市概况 ══
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.034"), contentX, topY, TEXT_SOFT_GOLD, alpha);
        topY += 14;
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.035") + (data.hasTownHall ? Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.036") : Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.037")),
            contentX, topY, TEXT_PRIMARY, alpha);
        topY += 12;
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.038") + data.placedBuildingCount + " \u5EA7",
            contentX, topY, TEXT_PRIMARY, alpha);
        topY += 14;

        g.fill(contentX, topY, contentX + contentW, topY + 1, withAlpha(EDGE_INNER, alpha));
        topY += 6;

        // ══ 经济市场 ══
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.039"), contentX, topY, TEXT_SOFT_GOLD, alpha);
        topY += 14;

        if (data.topItems.isEmpty())
        {
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.040"), contentX, topY, TEXT_SECONDARY, alpha);
        }
        else
        {
            int c1 = contentX, c2 = contentX + 120, c3 = contentX + 200;
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.041"), c1, topY, TEXT_SECONDARY, alpha);
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.042"), c2, topY, TEXT_SECONDARY, alpha);
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.043"), c3, topY, TEXT_SECONDARY, alpha);
            topY += 12;
            for (int i = 0; i < Math.min(data.topItems.size(), 8); i++)
            {
                var item = data.topItems.get(i);
                boolean deficit = item.consumption > item.production;
                int col = deficit ? 0xFFCC8888 : (item.production > 0 ? TEXT_GREEN : TEXT_PRIMARY);
                drawGlassText(g, item.displayName, c1, topY, col, alpha);
                drawGlassText(g, String.valueOf(item.consumption), c2, topY, col, alpha);
                drawGlassText(g, String.valueOf(item.production), c3, topY, col, alpha);
                topY += 11;
            }
        }

        topY += 6;
        g.fill(contentX, topY, contentX + contentW, topY + 1, withAlpha(EDGE_INNER, alpha));
        topY += 6;

        // ══ 资源短缺 ══
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.044"), contentX, topY, TEXT_SOFT_GOLD, alpha);
        topY += 14;
        if (data.deficitItems.isEmpty())
        {
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.045"), contentX, topY, TEXT_GREEN, alpha);
        }
        else
        {
            int d1 = contentX, d2 = contentX + 120;
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.041"), d1, topY, TEXT_SECONDARY, alpha);
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.046"), d2, topY, TEXT_SECONDARY, alpha);
            topY += 12;
            for (int i = 0; i < Math.min(data.deficitItems.size(), 8); i++)
            {
                var item = data.deficitItems.get(i);
                long gap = item.consumption - item.production;
                drawGlassText(g, item.displayName, d1, topY, 0xFFCC8888, alpha);
                drawGlassText(g, "-\u00a7c" + gap, d2, topY, 0xFFCC6666, alpha);
                topY += 11;
            }
        }
    }

    private void renderDemandCard(GuiGraphics g, int x, int y, int w, int h,
                                   String label, String value, int valueColor, int alpha) {
        fillGlassPanel(g, x, y, w, h, GLASS_PANEL, alpha);
        drawGlassText(g, label, x + 6, y + 4, TEXT_SECONDARY, alpha);
        drawGlassText(g, value, x + 6, y + 18, valueColor, alpha);
    }

    /**
     * 渲染右侧信息面板 — 玻璃态风格
     */
    private void renderRightPanel(GuiGraphics g, int alpha) {
        int panelX = width - RIGHT_PANEL_W;
        int panelY = TOP_BAR_H + 4;
        int panelH = height - TOP_BAR_H - BOTTOM_BAR_H - 8;

        // 玻璃面板
        fillGlassPanel(g, panelX, panelY, RIGHT_PANEL_W, panelH, GLASS_PANEL, alpha);
        // v1.2 金色粒子点缀
        MGStyles.drawParticles(g, panelX, panelY, RIGHT_PANEL_W, panelH, System.currentTimeMillis(), alpha / 2);

        // 标题
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.047"), panelX + 8, panelY + 8, TEXT_SOFT_GOLD, alpha);
        g.fill(panelX + 8, panelY + 20, panelX + RIGHT_PANEL_W - 8, panelY + 21, withAlpha(EDGE_INNER, alpha));

        int row = panelY + 28;

        if (selectedZone >= 0) {
            // 功能区详情
            String zoneName = Component.translatable("gui.metrogenesis.mayor_book.zone." + ZONE_TYPES[selectedZone]).getString();
            drawGlassText(g, ZONE_ICONS[selectedZone] + " " + zoneName, panelX + 8, row, ZONE_COLORS[selectedZone], alpha);
            row += 16;
            g.fill(panelX + 8, row, panelX + RIGHT_PANEL_W - 8, row + 1, withAlpha(EDGE_INNER, alpha));
            row += 6;

            int zoneCount = getZoneCount(selectedZone);
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.048"), zoneCount + Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.049"), alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.050"), Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.051"), alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.052"), Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.051"), alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.053"), Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.051"), alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.054"), Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.051"), alpha);
        } else if (zoneMode) {
            // 区域类型选择器
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.055"), panelX + 8, row, TEXT_SOFT_GOLD, alpha);
            row += 6;
            g.fill(panelX + 8, row, panelX + RIGHT_PANEL_W - 8, row + 1, withAlpha(EDGE_INNER, alpha));
            row += 10;

            int btnH = 24;
            int btnW = RIGHT_PANEL_W - 16;
            zoneBtnTop = row; // 记录第一个按钮的 y 坐标，用于 mouseClicked 判断
            for (int i = 0; i < ZONE_TYPES.length; i++) {
                String label = Component.translatable("gui.metrogenesis.mayor_book.zone." + ZONE_TYPES[i]).getString();
                int btnBg = (i == selectedZone) ? BTN_GLASS_SEL : (hoveredZoneBtn == i ? BTN_GLASS_HOVER : GLASS_PANEL);
                fillGlassButton(g, panelX + 8, row, btnW, btnH, withAlpha(ZONE_COLORS[i], alpha / 4), alpha);
                fillGlassButton(g, panelX + 8, row, btnW, btnH, btnBg, alpha);
                drawGlassText(g, ZONE_ICONS[i] + " " + label, panelX + 14, row + 6, TEXT_PRIMARY, alpha);
                zoneBtnBounds[i] = new int[]{panelX + 8, row, btnW, btnH};
                row += btnH + 4;
            }

            row += 4;
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.056"), panelX + 8, row, TEXT_SECONDARY, alpha);
            row += 12;
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.057"), panelX + 8, row, TEXT_SECONDARY, alpha);
        } else {
            // 城市概览（U2 修复：读真实城市数据，不再硬编码）
            final var city = com.metrogenesis.network.SyncCityDataMessage.getCachedData();
            String nameStr = city != null ? city.cityName : Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.058");
            String fundsStr = city != null ? (city.funds + " C-Value") : "0 C-Value";
            String popStr = city != null ? (city.population + "/" + city.maxPopulation) : "0/0";

            // 名称行 + 编辑按钮（✎）
            int nameRow = row;
            drawInfoGlass(g, panelX + 8, nameRow, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.059"), nameStr, alpha);
            int editBtnX = panelX + RIGHT_PANEL_W - 26;
            int editBtnY = nameRow - 1;
            if (!editingName) {
                boolean editHover = inBounds(editBtnX, editBtnY, 18, 18);
                fillGlassButton(g, editBtnX, editBtnY, 18, 18, editHover ? BTN_GLASS_HOVER : BTN_GLASS, alpha);
                drawGlassText(g, "\u270E", editBtnX + 4, editBtnY + 5, TEXT_SECONDARY, alpha);
                nameEditBtnBounds[0] = editBtnX; nameEditBtnBounds[1] = editBtnY;
                nameEditBtnBounds[2] = 18; nameEditBtnBounds[3] = 18;
            } else {
                nameEditBtnBounds[0] = 0; nameEditBtnBounds[1] = 0; nameEditBtnBounds[2] = 0; nameEditBtnBounds[3] = 0;
            }
            row = nameRow + 14;

            // 命名编辑模式：输入框 + 确认/取消
            if (editingName) {
                int inX = panelX + 8, inY = row, inW = RIGHT_PANEL_W - 16, inH = 18;
                MGStyles.drawPanel(g, inX, inY, inW, inH, MGStyles.C_BG_CARD, alpha);
                g.fill(inX, inY, inX + inW, inY + 1, withAlpha(MGStyles.C_BRASS, alpha));
                g.fill(inX, inY + inH - 1, inX + inW, inY + inH, withAlpha(MGStyles.C_BRASS, alpha));
                boolean cursorOn = (System.currentTimeMillis() / 500) % 2 == 0;
                String shown = nameBuffer + (cursorOn ? "_" : "");
                drawGlassText(g, shown.isEmpty() ? Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.071") : shown,
                        inX + 4, inY + 4, shown.isEmpty() ? TEXT_SECONDARY : TEXT_PRIMARY, alpha);
                nameInputBounds[0] = inX; nameInputBounds[1] = inY; nameInputBounds[2] = inW; nameInputBounds[3] = inH;
                row = inY + inH + 4;

                int bw = (inW - 6) / 2;
                int confX = inX, cancX = inX + bw + 6;
                fillGlassButton(g, confX, row, bw, 18, MGStyles.C_ACCENT, alpha);
                drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.069"), confX + 6, row + 5, TEXT_PRIMARY, alpha);
                fillGlassButton(g, cancX, row, bw, 18, BTN_GLASS, alpha);
                drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.070"), cancX + 6, row + 5, TEXT_SECONDARY, alpha);
                nameConfirmBounds[0] = confX; nameConfirmBounds[1] = row; nameConfirmBounds[2] = bw; nameConfirmBounds[3] = 18;
                nameCancelBounds[0] = cancX; nameCancelBounds[1] = row; nameCancelBounds[2] = bw; nameCancelBounds[3] = 18;
                row += 22;
            }

            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.060"), fundsStr, alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.061"), popStr, alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.062"), "—", alpha); row += 14;
            // 当前风格包
            String stylePackStr = getActiveStylePack() != null ? getActiveStylePack() : Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.063");
            drawInfoGlass(g, panelX + 8, row, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.064"), stylePackStr, alpha); row += 20;
            g.fill(panelX + 8, row, panelX + RIGHT_PANEL_W - 8, row + 1, withAlpha(EDGE_INNER, alpha));
            row += 6;

            // 功能区统计
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.065"), panelX + 8, row, TEXT_SOFT_GOLD, alpha);
            row += 14;

            int totalZones = 0;
            for (int i = 0; i < ZONE_TYPES.length; i++) {
                int count = getZoneCount(i);
                totalZones += count;
                if (count > 0) {
                    String label = Component.translatable("gui.metrogenesis.mayor_book.zone." + ZONE_TYPES[i]).getString();
                    drawInfoGlass(g, panelX + 8, row, ZONE_ICONS[i] + " " + label + ":", count + "", alpha);
                    row += 14;
                }
            }

            if (totalZones == 0) {
                drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.066"), panelX + 8, row, TEXT_SECONDARY, alpha);
                row += 14;
            }

            row += 6;
            drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.067"), panelX + 8, row, TEXT_SECONDARY, alpha);
        }
    }

    /**
     * 更新区域类型按钮的悬停状态（鼠标位置检测）
     */
    private void updateZoneBtnHover(int mx, int my) {
        hoveredZoneBtn = -1;
        if (zoneMode && selectedZone < 0) {
            int panelX = width - RIGHT_PANEL_W;
            for (int i = 0; i < ZONE_TYPES.length; i++) {
                int[] b = zoneBtnBounds[i];
                if (b[2] > 0 && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                    hoveredZoneBtn = i;
                    break;
                }
            }
        }
    }

    /**
     * 左下角坐标信息
     */
    private void renderCoordinates(GuiGraphics g, int alpha) {
        int y = height - BOTTOM_BAR_H - 30;
        drawGlassText(g, String.format("X:%.0f Z:%.0f", camX, camZ), 8, y, TEXT_SECONDARY, alpha);
        drawGlassText(g, String.format("Y:%.0f", camY), 8, y + 10, TEXT_SECONDARY, alpha);
    }

    /**
     * 柔和十字准星
     */
    private void renderCrosshair(GuiGraphics g, int mx, int my) {
        int len = 6;
        int color = 0x66706860;
        // 水平线
        g.fill(mx - len, my, mx + len + 1, my + 1, color);
        // 垂直线
        g.fill(mx, my - len, mx + 1, my + len + 1, color);
        // 中心点
        g.fill(mx, my, mx + 1, my + 1, 0x88B8A88A);
    }

    // ═══════════════════════════════════════════════════
    //  玻璃态绘制工具（圆角毛玻璃 + 柔和光晕）
    // ═══════════════════════════════════════════════════

    /**
     * 圆角矩形填充（用四条平移的 fill 模拟圆角）
     * 因为 MC 的 fill() 只支持轴对齐矩形，这里用四段平移在视觉上呈现圆角。
     * 圆角半径 ≈ 2px
     */
    /** 圆角矩形绘制（伪圆角，4px 内缩） */
    public void fillRoundRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) { g.fill(x, y, x + w, y + h, color); return; }
        // 中央主体
        g.fill(x, y + 2, x + w, y + h - 2, color);
        // 上下横条（内缩2px呈现圆角）
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        // 过渡竖条
        g.fill(x, y + 1, x + w, y + 2, color);
        g.fill(x, y + h - 2, x + w, y + h - 1, color);
    }

    /**
     * 玻璃面板：深色圆角半透明背景 + 柔和边缘光晕
     */
    /**
     * 面板：半透明深色玻璃底 + 1px 黄铜细框 + 四角 2x2 像素角（v1.2）。
     * 委托 MGStyles.drawPanel 实现统一外观。
     */
    public void fillGlassPanel(GuiGraphics g, int x, int y, int w, int h, int bgColor, int alpha) {
        if (w <= 0 || h <= 0) { g.fill(x, y, x + w, y + h, withAlpha(bgColor, alpha)); return; }
        MGStyles.drawPanel(g, x, y, w, h, bgColor, alpha);
    }

    /**
     * 玻璃态按钮（深灰底 + 黄铜边框 + 顶部高光），委托 MGStyles.drawButton
     */
    public void fillGlassButton(GuiGraphics g, int x, int y, int w, int h, int bgColor, int alpha) {
        MGStyles.drawButton(g, x, y, w, h, bgColor, alpha);
    }

    /**
     * 悬浮微动效插值进度
     */
    private float animateHover(long startTime, int durationMs) {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1f, (float) elapsed / durationMs);
    }

    /**
     * 玻璃态文字（暖白，带 shadow）
     */
    public void drawGlassText(GuiGraphics g, String text, int x, int y, int color, int alpha) {
        g.drawString(font, text, x, y, withAlpha(color, alpha), true);
    }

    /**
     * 玻璃态统计项
     */
    private void drawStatGlass(GuiGraphics g, int x, int y, String label, String value, int valueColor, int alpha) {
        g.drawString(font, label, x, y, withAlpha(TEXT_SECONDARY, alpha), true);
        g.drawString(font, value, x + font.width(label) + 3, y, withAlpha(valueColor, alpha), true);
    }

    /**
     * 玻璃态信息行
     */
    private void drawInfoGlass(GuiGraphics g, int x, int y, String label, String value, int alpha) {
        g.drawString(font, label, x, y, withAlpha(TEXT_SECONDARY, alpha), true);
        g.drawString(font, value, x + font.width(label) + 3, y, withAlpha(TEXT_PRIMARY, alpha), true);
    }

    public Font getFont() { return font; }

    /**
     * 带 alpha 的颜色
     */
    public int withAlpha(int color, int alpha) {
        if (alpha >= 255) return color;
        int a = (color >>> 24) & 0xFF;
        a = (a * alpha) / 255;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * 边界检测
     */
    public boolean inBounds(int bx, int by, int w, int h) {
        return mouseX >= bx && mouseX < bx + w && mouseY >= by && mouseY < by + h;
    }

    private boolean inBounds(double mx, double my, int bx, int by, int w, int h) {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }

    // ═══════════════════════════════════════════════════
    //  参数 popup（区域设置纵向展开列表：密度 3 行 + 分割 + 所有权 2 行）
    //  参考 Win11 开始菜单"已固定→全部"风格 — 从区域按钮向上弹出
    // ═══════════════════════════════════════════════════

    /** popup 整体宽（与触发器等宽或稍宽），标题高，行高，分割行高。 */
    private static final int POPUP_TITLE_H = 18;
    private static final int POPUP_ROW_H   = 20;
    private static final int POPUP_DIV_H   = 6;  // 分割行（视觉上仅 1px 横线，6px 留余）
    private static final int POPUP_GAP     = 4;

    /**
     * 渲染参数 popup：纵向玻璃列表，悬浮在区域按钮正上方。
     * 内容：标题 + 密度小节标题 + 密度 3 行 + 分割线 + 所有权小节标题 + 所有权 2 行。
     * 行点击命中范围同步写入 paramPopupRowBounds[0..3+row*4..+3] 供 mouseClicked 使用。
     */
    private void renderParamPopup(GuiGraphics g, int alpha, int zoneX, int btnW, int btnY, int btnH) {
        // 总行数：密度 3 + 分割 1（不可点，跳过） + 所有权 2 = 6
        final int totalRows = 6;     // 含分割行
        final int densityRows = 3;
        final int ownershipRows = 2;
        int popupW = btnW + 24;       // 略宽于区域按钮（容纳"密度:中等"等长标签）
        int popupH = POPUP_TITLE_H + densityRows * POPUP_ROW_H + POPUP_DIV_H + ownershipRows * POPUP_ROW_H + 4;
        int popupX = zoneX - 12;      // 居中于区域按钮
        int popupY = btnY - popupH - POPUP_GAP;

        // 边界保护：左右超出屏幕时回拉
        if (popupX < 4) popupX = 4;
        if (popupX + popupW > width - 4) popupX = width - 4 - popupW;
        if (popupY < TOP_BAR_H + 4) popupY = btnY + btnH + 4; // 空间不足时回退到按钮下方

        // 背景：玻璃态深色面板
        fillGlassPanel(g, popupX, popupY, popupW, popupH, GLASS_BG, alpha);
        // 1px 金色描边（v1.2 Create 风）
        int border = withAlpha(TEXT_SOFT_GOLD, alpha);
        g.fill(popupX, popupY, popupX + popupW, popupY + 1, border);
        g.fill(popupX, popupY + popupH - 1, popupX + popupW, popupY + popupH, border);
        g.fill(popupX, popupY, popupX + 1, popupY + popupH, border);
        g.fill(popupX + popupW - 1, popupY, popupX + popupW, popupY + popupH, border);

        // 标题栏
        fillGlassPanel(g, popupX, popupY, popupW, POPUP_TITLE_H, GLASS_HEADER, alpha);
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.074"),
                      popupX + 8, popupY + 5, TEXT_SOFT_GOLD, alpha);

        // === 密度小节标题（不开热点）===
        int sectionY = popupY + POPUP_TITLE_H + 1;
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.072"),
                      popupX + 10, sectionY + 1, TEXT_SOFT_GOLD, alpha);

        // === 密度 3 行 ===
        int densityY0 = sectionY + 12;
        String[] densityLabels = {
            Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.012"),
            Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.013"),
            Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.014") };
        int[] densityColors = { TEXT_SECONDARY, TEXT_SOFT_GOLD, 0xFFFFD866 };
        for (int i = 0; i < densityRows; i++) {
            int rowY = densityY0 + i * POPUP_ROW_H;
            int rowX = popupX + 1;
            int rowW = popupW - 2;
            int rowH = POPUP_ROW_H - 1;
            int rowIdx = i;
            drawParamPopupRow(g, alpha, popupX, rowX, rowY, rowW, rowH,
                              rowIdx, rowIdx == zoneDensity, densityLabels[i], densityColors[i]);
        }

        // === 分割线（不可点，跳过命中）===
        int divY = densityY0 + densityRows * POPUP_ROW_H + 2;
        int divColor = withAlpha(TEXT_SECONDARY, alpha / 2);
        g.fill(popupX + 8, divY, popupX + popupW - 8, divY + 1, divColor);

        // === 所有权小节标题 ===
        int ownSectionY = divY + POPUP_DIV_H;
        drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.073"),
                      popupX + 10, ownSectionY + 1, TEXT_SOFT_GOLD, alpha);

        // === 所有权 2 行 ===
        int ownY0 = ownSectionY + 12;
        String[] ownLabels = {
            Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.015"),
            Language.getInstance().getOrDefault("gui.metrogenesis.mayorbook.016") };
        int[] ownColors = { 0xCCB08060, TEXT_SOFT_GOLD };
        for (int i = 0; i < ownershipRows; i++) {
            int rowY = ownY0 + i * POPUP_ROW_H;
            int rowX = popupX + 1;
            int rowW = popupW - 2;
            int rowH = POPUP_ROW_H - 1;
            // rowIdx 必须连续：密度 3 行后接着 0/1，所以所有权 row i 的 index = densityRows + i = 3/4
            int rowIdx = densityRows + i;
            drawParamPopupRow(g, alpha, popupX, rowX, rowY, rowW, rowH,
                              rowIdx, rowIdx - densityRows == zoneOwnership, ownLabels[i], ownColors[i]);
        }

        // 记录行命中矩形（mouseClicked 使用）：[popupX, popupY, popupW, popupH, row0_x, row0_y, row0_w, row0_h, ...]
        // 密度 3 行 (index 0..2) 写到 bounds[4..15]
        for (int i = 0; i < densityRows; i++) {
            int base = 4 + i * 4;
            paramPopupRowBounds[base    ] = popupX + 1;
            paramPopupRowBounds[base + 1] = densityY0 + i * POPUP_ROW_H;
            paramPopupRowBounds[base + 2] = popupW - 2;
            paramPopupRowBounds[base + 3] = POPUP_ROW_H - 1;
        }
        // 所有权 2 行 (index 3..4) 写到 bounds[16..23]
        for (int i = 0; i < ownershipRows; i++) {
            int base = 4 + (densityRows + i) * 4;
            paramPopupRowBounds[base    ] = popupX + 1;
            paramPopupRowBounds[base + 1] = ownY0 + i * POPUP_ROW_H;
            paramPopupRowBounds[base + 2] = popupW - 2;
            paramPopupRowBounds[base + 3] = POPUP_ROW_H - 1;
        }
        // 整体矩形
        paramPopupRowBounds[0] = popupX;
        paramPopupRowBounds[1] = popupY;
        paramPopupRowBounds[2] = popupW;
        paramPopupRowBounds[3] = popupH;
    }

    /**
     * 绘制 popup 内单行：hover 高亮 + 选中行左侧 2px 金条 + 行文字。
     * @param rowIdx 该行在 popup 中的全局索引（0..5），用于 paramPopupRowBounds 写入与 hover 判断
     */
    private void drawParamPopupRow(GuiGraphics g, int alpha, int popupX, int rowX, int rowY, int rowW, int rowH,
                                   int rowIdx, boolean isSelected, String label, int labelColor) {
        boolean isHover = (rowIdx == paramPopupHoverRow);
        if (isHover) {
            fillGlassPanel(g, rowX, rowY, rowW, rowH, BTN_GLASS_HOVER, alpha);
        }
        if (isSelected) {
            g.fill(rowX, rowY, rowX + 2, rowY + rowH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        int textColor = isSelected ? TEXT_SOFT_GOLD : labelColor;
        // 缩进：选中条(2) + 内边距(8) = 10
        drawGlassText(g, label, rowX + 10, rowY + 5, textColor, alpha);
    }

    /**
     * popup 是否在指定坐标处（用于点击命中与 hover 检测）。
     * @return 命中的行索引（0..5，0..2=密度 / 3=分割 / 4..5=所有权），-1=未命中
     */
    private int paramPopupRowAt(double mx, double my) {
        if (paramPopup == ParamPopup.NONE) return -1;
        // 检查 5 个可点击行（密度 3 + 所有权 2，跳过分割行 index=3）
        for (int i = 0; i < 5; i++) {
            if (i == 3) continue; // 分割行
            int base = 4 + i * 4;
            if (mx >= paramPopupRowBounds[base] && mx < paramPopupRowBounds[base] + paramPopupRowBounds[base + 2]
             && my >= paramPopupRowBounds[base + 1] && my < paramPopupRowBounds[base + 1] + paramPopupRowBounds[base + 3]) {
                return i;
            }
        }
        return -1;
    }

    /** popup 整体矩形是否在指定坐标处。 */
    private boolean paramPopupContains(double mx, double my) {
        if (paramPopup == ParamPopup.NONE) return false;
        return mx >= paramPopupRowBounds[0] && mx < paramPopupRowBounds[0] + paramPopupRowBounds[2]
            && my >= paramPopupRowBounds[1] && my < paramPopupRowBounds[1] + paramPopupRowBounds[3];
    }

    // ══ 城市命名编辑（① 开城/重命名）════════════════════════

    /** 进入命名编辑态：预填当前城市名（未命名/默认名则留空）。 */
    private void startNameEdit() {
        final var city = com.metrogenesis.network.SyncCityDataMessage.getCachedData();
        String cur = city != null ? city.cityName : null;
        nameBuffer = (cur != null && !cur.equals("未命名") && !cur.equals("新城邦")) ? cur : "";
        editingName = true;
    }

    /** 提交命名：发往服务端 foundCity（未开城则开城，已开城则重命名）。 */
    private void commitName() {
        String name = nameBuffer.trim();
        if (name.isEmpty()) { editingName = false; return; }
        String style = getActiveStylePack();
        com.metrogenesis.network.NetworkHandler.CHANNEL.sendToServer(
            new com.metrogenesis.network.FoundCityMessage(name, style));
        com.metrogenesis.network.SyncCityDataMessage.requestRefresh();
        editingName = false;
    }

    /**
     * 判断鼠标是否在顶部状态栏
     */
    private boolean inTopBar(double mx, double my) {
        return my < TOP_BAR_H;
    }

    /**
     * 判断鼠标是否在底部工具栏
     */
    private boolean inBottomBar(double mx, double my) {
        return my >= height - BOTTOM_BAR_H;
    }

    /**
     * 判断鼠标是否在右侧面板
     */
    private boolean inRightPanel(double mx, double my) {
        return mx >= width - RIGHT_PANEL_W && my >= TOP_BAR_H && my < height - BOTTOM_BAR_H;
    }

    /**
     * 屏幕坐标到世界坐标：通过 MC 实际投影矩阵反投影获取射线方向，
     * 然后与地形高度进行迭代求交，得到精确的世界坐标。
     *
     * 迭代法：先用 savedY 平面求交获得初始 (x,z)，然后采样地形高度，
     * 再以该高度重新求交，迭代收敛到精确位置。
     * 这消除了固定平面投影在地形起伏时的偏移问题。
     */
    private BlockPos screenToWorld(double screenX, double screenY) {
        // 归一化到 NDC [-1, 1]
        float ndcX = (float) ((2.0 * screenX / width) - 1.0);
        float ndcY = (float) (1.0 - (2.0 * screenY / height));

        // MC 的投影矩阵
        Matrix4f projMatrix = minecraft.gameRenderer.getProjectionMatrix(
                minecraft.options.fov().get());

        // 视图矩阵（从 Camera 获取）
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 lookVec = new Vec3(camera.getLookVector());
        Vec3 upVec = new Vec3(camera.getUpVector());

        Matrix4f viewMatrix = new Matrix4f().lookAt(
                (float) camPos.x, (float) camPos.y, (float) camPos.z,
                (float) (camPos.x + lookVec.x), (float) (camPos.y + lookVec.y), (float) (camPos.z + lookVec.z),
                (float) upVec.x, (float) upVec.y, (float) upVec.z);

        // 反投影矩阵
        Matrix4f invPV = new Matrix4f(projMatrix).mul(viewMatrix).invert();

        Vector4f nearP = new Vector4f(ndcX, ndcY, -1.0f, 1.0f).mul(invPV);
        Vector4f farP  = new Vector4f(ndcX, ndcY,  1.0f, 1.0f).mul(invPV);

        if (nearP.w == 0 || farP.w == 0) return null;
        nearP.div(nearP.w);
        farP.div(farP.w);

        // 射线方向
        Vec3 dir = new Vec3(
                farP.x - nearP.x,
                farP.y - nearP.y,
                farP.z - nearP.z).normalize();

        Vec3 origin = new Vec3(nearP.x, nearP.y, nearP.z);

        // ══ 迭代地形求交 ═══════════════════════════════════
        // 先用 savedY 作为初始投影平面获得初值
        double targetY = savedY;
        if (Math.abs(dir.y) < 1e-5) {
            // 射线平行于地面 — 退化情况，回退到 savedY
            double t0 = (targetY - origin.y) / 1e-5;
            if (t0 <= 0) return null;
            double wx0 = origin.x + t0 * dir.x;
            double wz0 = origin.z + t0 * dir.z;
            return new BlockPos((int) Math.floor(wx0), (int) targetY, (int) Math.floor(wz0));
        }

        // 最多迭代 3 次，收敛到实际地形高度
        double wx = origin.x, wz = origin.z;
        for (int iter = 0; iter < 3; iter++) {
            double t = (targetY - origin.y) / dir.y;
            if (t <= 0) return null;
            wx = origin.x + t * dir.x;
            wz = origin.z + t * dir.z;

            // 采样该位置的地形高度（使用 WORLD_SURFACE）
            Level level = minecraft.level;
            if (level != null) {
                int newY = level.getHeight(Heightmap.Types.WORLD_SURFACE,
                        (int) Math.floor(wx), (int) Math.floor(wz));
                if (Math.abs(newY - targetY) < 0.5) {
                    // 收敛
                    targetY = newY;
                    break;
                }
                targetY = newY;
            } else {
                break;
            }
        }

        return new BlockPos((int) Math.floor(wx), (int) targetY, (int) Math.floor(wz));
    }

    /**
     * 获取 RTS 摄像机画面中心对准的世界坐标。
     * 用于确定放置建筑的初始位置。
     */
    public BlockPos getCameraTarget() {
        return screenToWorld(width / 2.0, height / 2.0);
    }

    /**
     * 对齐到网格
     */
    private int snapToGrid(int coord) {
        return Math.floorDiv(coord, ZONE_GRID) * ZONE_GRID;
    }

    /**
     * 在半径 radius 格内查找最近的道路节点。
     * 找到则返回该节点坐标，否则返回原坐标。
     */
    private int[] snapToNearestNode(int blockX, int blockZ, int radius) {
        // 只在有 serverLevel 时检查
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null) return new int[]{blockX, blockZ};

        ServerLevel serverLevel = mc.getSingleplayerServer().overworld();
        var roadData = com.metrogenesis.RoadData.getOrCreate(serverLevel);
        com.metrogenesis.RoadData.NodePos nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (var node : roadData.getAllNodes()) {
            double dist = Math.sqrt(Math.pow(node.blockX() - blockX, 2) + Math.pow(node.blockZ() - blockZ, 2));
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                nearest = node;
            }
        }

        if (nearest != null) {
            return new int[]{nearest.blockX(), nearest.blockZ()};
        }
        return new int[]{blockX, blockZ};
    }

    /**
     * 绘制矩形功能区（替换单格标记为矩形区域）
     * 参考 SimCity 分区工具：点击 + 拖拽 = 矩形区域
     */
    private boolean addZoneRect(int x1, int z1, int x2, int z2, int zoneType) {
        saveUndoState(); // 操作前保存撤销状态
        int minX = snapToGrid(Math.min(x1, x2));
        int minZ = snapToGrid(Math.min(z1, z2));
        int maxX = snapToGrid(Math.max(x1, x2)) + 1; // 包含末端
        int maxZ = snapToGrid(Math.max(z1, z2)) + 1;

        // 最小尺寸校验（至少 1 格）
        if (maxX - minX < 1 || maxZ - minZ < 1) return false;

        // 检查与已有区域是否重叠（同类型合并，不同类型跳过）
        List<ZoneRect> toRemove = new ArrayList<>();
        for (ZoneRect existing : zoneRects) {
            if (existing.zoneType() == zoneType && rectsOverlap(existing, minX, minZ, maxX, maxZ)) {
                toRemove.add(existing);
            }
        }
        zoneRects.removeAll(toRemove);

        // 检查上限
        if (zoneRects.size() >= MAX_ZONES) return false;

        // 优先识别最近道路方向，否则用拖拽方向
        int dir = detectRoadDirection(minX, minZ, maxX, maxZ);
        if (dir < 0) dir = calcDirection(x1, z1, x2, z2);

        zoneRects.add(new ZoneRect(minX, minZ, maxX, maxZ, zoneType, dir, STAGE_PLANNING, zoneDensity, zoneOwnership));
        zonePendingRotation = true; // 进入朝向待确认状态
        return true;
    }

    /**
     * 检测区域四条边中哪条边有道路相邻。
     * 读取 RoadData 中的所有道路节点，每个节点"投票"给最近的边，
     * 得票最多的边即为正面方向。
     * <p>参考 citybound: lots 有 road_boundaries，通过道路数据确定朝向。</p>
     *
     * @return 方向 0=东 1=南 2=西 3=北，无道路返回 -1
     */
    private int detectRoadDirection(int minX, int minZ, int maxX, int maxZ) {
        // 读取 RoadData（权威来源）
        if (minecraft.getSingleplayerServer() == null) return -1;
        var serverLevel = minecraft.getSingleplayerServer()
            .getLevel(minecraft.level.dimension());
        if (serverLevel == null) return -1;

        var roadData = com.metrogenesis.RoadData.getOrCreate(serverLevel);
        var nodes = roadData.getAllNodes();

        if (nodes.isEmpty()) return -1;

        // 每个节点给四条边按距离独立加权：距离越近权重越大
        // 避免平局和远距离节点误判
        double[] weights = new double[4];
        int maxDist = 12;

        for (var node : nodes) {
            int nx = node.blockX();
            int nz = node.blockZ();

            int dEast  = Math.abs(nx - maxX) + Math.abs(nz - clamp(nz, minZ, maxZ));
            int dSouth = Math.abs(nz - maxZ) + Math.abs(nx - clamp(nx, minX, maxX));
            int dWest  = Math.abs(nx - minX) + Math.abs(nz - clamp(nz, minZ, maxZ));
            int dNorth = Math.abs(nz - minZ) + Math.abs(nx - clamp(nx, minX, maxX));

            // 每条边独立加分（距离越近分越高）
            if (dEast  < maxDist) weights[0] += maxDist - dEast;
            if (dSouth < maxDist) weights[1] += maxDist - dSouth;
            if (dWest  < maxDist) weights[2] += maxDist - dWest;
            if (dNorth < maxDist) weights[3] += maxDist - dNorth;
        }

        int bestDir = -1;
        double bestWeight = 0;
        for (int d = 0; d < 4; d++) {
            if (weights[d] > bestWeight) {
                bestWeight = weights[d];
                bestDir = d;
            }
        }

        return bestWeight > 0 ? bestDir : -1;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int min4(int a, int b, int c, int d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * 检查 (x,z) 位置地面是否放置了道路方块。
     * 扫描地面表面 3 格深度，识别 SMOOTH_STONE / STONE_BRICKS。
     */
    private static boolean isRoadBlock(Level level, int x, int z) {
        int h = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        // 向下查 3 格（道路可能在地面或略高/略低）
        for (int dy = 0; dy < 3; dy++) {
            int y = h - dy;
            if (y < level.getMinBuildHeight()) break;
            var state = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
            if (state.getBlock() == net.minecraft.world.level.block.Blocks.SMOOTH_STONE
                || state.getBlock() == net.minecraft.world.level.block.Blocks.STONE_BRICKS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据拖拽方向计算区域朝向（建筑正面方向）。
     * <pre>
     *   起点→终点指向正东/西 → 朝东/西（建筑面向右/左边缘）
     *   起点→终点指向正南/北 → 朝南/北（建筑面向下/上边缘）
     * </pre>
     * 方向值：0=东, 1=南, 2=西, 3=北
     */
    private static int calcDirection(int x1, int z1, int x2, int z2) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? 0 : 2; // EAST or WEST
        } else {
            return dz >= 0 ? 1 : 3; // SOUTH or NORTH
        }
    }

    /**
     * 检测两个矩形是否重叠
     */
    private boolean rectsOverlap(ZoneRect r, int minX, int minZ, int maxX, int maxZ) {
        return r.minX() < maxX && r.maxX() > minX && r.minZ() < maxZ && r.maxZ() > minZ;
    }

    /**
     * 确认道路施工 — 从投影状态转为实际生成（贝塞尔曲线模式）
     */
    private void confirmRoadConstruction() {
        if (!roadPending) return;

        if (minecraft.getSingleplayerServer() != null && minecraft.level != null) {
            var serverLevel = minecraft.getSingleplayerServer()
                .getLevel(minecraft.level.dimension());
            if (serverLevel != null) {
                var from = new com.metrogenesis.RoadData.NodePos(pendingFromBX, pendingFromBZ);
                var to = new com.metrogenesis.RoadData.NodePos(pendingToBX, pendingToBZ);
                var roadData = com.metrogenesis.RoadData.getOrCreate(serverLevel);

                // 创建 ChangeStorage 记录方块变更，支持撤销
                var changeStorage = new com.metrogenesis.structurize.util.ChangeStorage(
                    net.minecraft.network.chat.Component.literal("Road construction"),
                    minecraft.player.getUUID());

                if (curvedRoad && Math.abs(roadCurvature) > 0.01f) {
                    // 弯道模式：使用 BezierSegment + RoadPlacer
                    int startY = RoadBuilder.getTrueTerrainHeight(serverLevel, pendingFromBX, pendingFromBZ);
                    int endY   = RoadBuilder.getTrueTerrainHeight(serverLevel, pendingToBX, pendingToBZ);
                    BlockPos start = new BlockPos(pendingFromBX, startY, pendingFromBZ);
                    BlockPos end = new BlockPos(pendingToBX, endY, pendingToBZ);
                    BezierSegment segment = new BezierSegment(start, end, roadCurvature);

                    var template = com.metrogenesis.road.RoadTemplate.createDefaultStraight();
                    var result = RoadPlacer.placeRoad(
                        serverLevel, segment, template, true, 2, changeStorage);
                    // 保存到 RoadData（用于路网连接分析），曲率用于跨段重建
                    roadData.addSegment(new com.metrogenesis.RoadData.RoadSegment(from, to, 0, roadCurvature));
                    // 重建节点路口模式，确保与相邻道路正确连接
                    RoadBuilder.rebuildNode(serverLevel, from, roadData, changeStorage);
                    RoadBuilder.rebuildNode(serverLevel, to, roadData, changeStorage);
                    MetroGenesis.LOGGER.info("Bezier road placed: {} -> {} curvature={}, blocks={}",
                        from, to, roadCurvature, result.totalBlocks());
                } else {
                    // 直道模式：使用 RoadBuilder.commitSegment（原生直线道路）
                    com.metrogenesis.road.RoadBuilder.commitSegment(
                        serverLevel, roadData, from, to, 0, changeStorage);
                    MetroGenesis.LOGGER.info("Straight road placed: {} -> {}", from, to);
                }

                // 记录 ChangeStorage 到 undo 栈（精确记录该段所有方块变更）
                roadUndoStack.push(changeStorage);
                roadRedoStack.clear();
            }
        }

        // 放置完成后清除状态
        roadPending = false;
        roadDrawing = false;
        roadCurvature = 0f;
        com.metrogenesis.client.RoadRenderer.clearPreview();
    }

    /**
     * 通过世界坐标查找区域并删除
     */
    private boolean deleteZoneAt(int worldX, int worldZ) {
        return zoneRects.removeIf(r -> worldX >= r.minX() && worldX < r.maxX()
                                    && worldZ >= r.minZ() && worldZ < r.maxZ());
    }

    /**
     * 获取指定类型的功能区总面积（格数）
     */
    private int getZoneArea(int zoneType) {
        int total = 0;
        for (ZoneRect r : zoneRects) {
            if (r.zoneType() == zoneType) total += r.area();
        }
        return total;
    }

    /**
     * 获取指定类型的区域数量
     */
    private int getZoneCount(int zoneType) {
        int count = 0;
        for (ZoneRect r : zoneRects) {
            if (r.zoneType() == zoneType) count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════
    //  Input Handling
    // ═══════════════════════════════════════════════════

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 命名编辑模式：拦截键盘
        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitName(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingName = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
                return true;
            }
            return true; // 其余键（方向键等）吞掉，由 charTyped 处理输入
        }

        // 图鉴编辑模式 — 优先处理文本输入
        if (catalogPanel != null && catalogPanel.isCatalogMode() && catalogPanel.isEditActive()) {
            if (catalogPanel.keyPressedEdit(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        // Ctrl+Z：撤销（区域 + 道路）
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_Z) {
            doUndo();
            return true;
        }

        // Ctrl+Y：重做
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_Y) {
            doRedo();
            return true;
        }

        // ESC 分段处理
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // 参数 popup → 关闭（最高优先级）
            if (paramPopup != ParamPopup.NONE) {
                paramPopup = ParamPopup.NONE;
                paramPopupHoverRow = -1;
                return true;
            }
            // 部门面板 → 关闭
            if (deptPanel != null && deptPanel.isVisible()) {
                deptPanel.setVisible(false);
                return true;
            }
            if (showingDemands) {
                showingDemands = false;
                return true;
            }
            if (catalogPanel != null && catalogPanel.isCatalogMode()) {
                if (catalogPanel.isPlacementConfirm()) {
                    catalogPanel.cancelPlacement();
                    return true;
                }
                if (catalogPanel.isPlacementMode()) {
                    catalogPanel.cancelPlacement();
                    return true;
                }
                catalogPanel.close();
                return true;
            }
            if (zoneDrawing) {
                zoneDrawing = false;
                selectedZone = -1;
                return true;
            }
            if (zoneMode) {
                zoneMode = false;
                selectedZone = -1;
                zoneDrawing = false;
                return true;
            }
            if (roadDrawing) {
                roadDrawing = false;
                return true;
            }
            if (roadPending) {
                roadPending = false;
                return true;
            }
            if (roadMode) {
                roadMode = false;
                return true;
            }
            // 关闭界面
            minecraft.setScreen(null);
            return true;
        }

        // B 键：切换需求面板（WASD 中 D 已被用于右移，改用 B 避免冲突）
        if (keyCode == GLFW.GLFW_KEY_B && !roadMode
            && (catalogPanel == null || !catalogPanel.isCatalogMode())
            && !showingDemands) {
            showingDemands = true;
            com.metrogenesis.network.SyncCityDataMessage.requestRefresh();
            return true;
        }

        // T 键：切换图鉴模式（放置/道路/需求模式时禁用）
        if (keyCode == GLFW.GLFW_KEY_T
            && !roadMode && !showingDemands
            && (catalogPanel == null || (!catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            if (catalogPanel != null) {
                if (catalogPanel.isCatalogMode()) {
                    catalogPanel.close();
                } else {
                    catalogPanel.open();
                }
            }
            return true;
        }

        // M 键：打开城市地图视图（放置/图鉴/需求/道路模式禁用）
        if (keyCode == GLFW.GLFW_KEY_M
            && !roadMode && !showingDemands
            && (catalogPanel == null || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            CityMapScreen.open();
            return true;
        }

        // G 键（区域模式下）：提交待建区划启动生长生长
        if (keyCode == GLFW.GLFW_KEY_G && zoneMode) {
            sendPendingZonesForGrowth();
            return true;
        }

        // G 键：切换部门面板（放置/图鉴/需求/道路/区域模式禁用）
        if (keyCode == GLFW.GLFW_KEY_G
            && !roadMode && !zoneMode && !showingDemands
            && (catalogPanel == null || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            if (deptPanel != null) {
                deptPanel.toggle();
            }
            return true;
        }

        // Z 键：切换区域绘制模式（其他模式下禁用）
        if (keyCode == GLFW.GLFW_KEY_Z && !roadMode && !showingDemands
            && (catalogPanel == null || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            zoneMode = !zoneMode;
            if (!zoneMode) {
                selectedZone = -1;
                zoneDrawing = false;
                // 退出区域模式时同步清掉 popup 状态
                paramPopup = ParamPopup.NONE;
                paramPopupHoverRow = -1;
            }
            return true;
        }

        // 放置模式下 Enter 确认
        if (keyCode == GLFW.GLFW_KEY_ENTER && catalogPanel != null
            && catalogPanel.isPlacementMode() && !catalogPanel.isPlacementDragging()
            && catalogPanel.getPlacementTargetPos() != null) {
            catalogPanel.confirmPlacementPosition(catalogPanel.getPlacementTargetPos());
            return true;
        }

        // 放置模式下 Tab 切换 CAM/NUDGE 模式
        if (keyCode == GLFW.GLFW_KEY_TAB && catalogPanel != null
            && catalogPanel.isPlacementMode()) {
            catalogPanel.togglePlacementNudgeMode();
            return true;
        }

        // 放置模式下 NUDGE 模式 — WASD/QE/RF 用于建筑微调
        if (catalogPanel != null && catalogPanel.isPlacementMode() && catalogPanel.isPlacementNudgeMode()
            && !catalogPanel.isPlacementDragging()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_W -> catalogPanel.nudgeForward();
                case GLFW.GLFW_KEY_S -> catalogPanel.nudgeBack();
                case GLFW.GLFW_KEY_A -> catalogPanel.nudgeLeft();
                case GLFW.GLFW_KEY_D -> catalogPanel.nudgeRight();
                case GLFW.GLFW_KEY_Q -> catalogPanel.rotate(-1);
                case GLFW.GLFW_KEY_E -> catalogPanel.rotate(+1);
                case GLFW.GLFW_KEY_R -> { catalogPanel.nudgeY(+1); return true; }  // 上移
                case GLFW.GLFW_KEY_F -> { catalogPanel.nudgeY(-1); return true; }  // 下移
            }
            // WASD 还要让摄像机移动操作不触发，但保留状态更新
            // 这里不 return true 以便 WASD 的 keyForward/keyBack 等状态被设置，
            // 但 render() 中 camera movement 在 placementMode 时已跳过
        }

        // WASD + 空格/Shift 移动
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> keyForward = true;
            case GLFW.GLFW_KEY_S -> keyBack = true;
            case GLFW.GLFW_KEY_A -> keyLeft = true;
            case GLFW.GLFW_KEY_D -> keyRight = true;
            case GLFW.GLFW_KEY_SPACE -> keyUp = true;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> keyDown = true;
            case GLFW.GLFW_KEY_Q -> keyRotateL = true;
            case GLFW.GLFW_KEY_E -> keyRotateR = true;
        }

        // 数字键快捷选择：无（旧版区域划分功能已暂停）

        // R 键：切换道路绘制模式（图鉴/放置/需求模式禁用）
        if (keyCode == GLFW.GLFW_KEY_R && !showingDemands
            && (catalogPanel == null || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            roadMode = !roadMode;
            if (roadMode) {
                roadCurvature = 0f;
                curvedRoad = false;  // 默认直道模式
            }
            return true;
        }

        // T 键：道路模式下切换直道/弯道模式（非图鉴/放置模式时）
        if (keyCode == GLFW.GLFW_KEY_T && roadMode
            && (catalogPanel == null || (!catalogPanel.isPlacementMode() && !catalogPanel.isPlacementConfirm()))) {
            curvedRoad = !curvedRoad;
            if (!curvedRoad) roadCurvature = 0f;  // 切回直道时重置曲率
            return true;
        }

        // F3、F5 等功能键透传给 MC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> keyForward = false;
            case GLFW.GLFW_KEY_S -> keyBack = false;
            case GLFW.GLFW_KEY_A -> keyLeft = false;
            case GLFW.GLFW_KEY_D -> keyRight = false;
            case GLFW.GLFW_KEY_SPACE -> keyUp = false;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> keyDown = false;
            case GLFW.GLFW_KEY_Q -> keyRotateL = false;
            case GLFW.GLFW_KEY_E -> keyRotateR = false;
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 命名编辑模式：接受文本输入
        if (editingName) {
            if (codePoint >= 32 && codePoint != 127) {
                if (nameBuffer.length() < 32) nameBuffer += codePoint;
            }
            return true;
        }

        // 图鉴编辑模式 — 处理文本输入
        if (catalogPanel != null && catalogPanel.isCatalogMode() && catalogPanel.isEditActive()) {
            return catalogPanel.charTypedEdit(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 命名编辑模式：优先拦截点击（确认/取消）
        if (editingName && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (inBounds(nameConfirmBounds[0], nameConfirmBounds[1], nameConfirmBounds[2], nameConfirmBounds[3])) { commitName(); return true; }
            if (inBounds(nameCancelBounds[0], nameCancelBounds[1], nameCancelBounds[2], nameCancelBounds[3])) { editingName = false; return true; }
            return true; // 点击输入框或其他区域：保持在编辑态
        }

        // ══ 扫描中保护：图鉴正在加载蓝图时，只允许图鉴面板自身的点击 ══
        if (catalogPanel != null && catalogPanel.isScanning()) {
            if (catalogPanel.mouseClicked(mx, my, button)) {
                return true; // 图鉴面板内的点击（选风格包/刷新）正常处理
            }
            return true; // 扫描期间阻止所有其他 UI 交互（地图、需求、道路、区域等）
        }

        // 部门面板点击（优先处理，防止穿透）
        if (deptPanel != null && deptPanel.mouseClicked(mx, my, button, this)) {
            return true;
        }

        // 委托给图鉴面板（含放置确认 + 拖拽定位）
        if (catalogPanel != null && catalogPanel.mouseClicked(mx, my, button)) {
            return true;
        }

        // 参数 popup（区域设置纵向列表：密度 + 所有权）— 优先级高于其他面板，命中即独占事件
        if (paramPopup == ParamPopup.ZONE_PANEL && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // popup 内行点击
            int row = paramPopupRowAt(mx, my);
            if (row >= 0 && row != 3) { // row 3 是分割行（不可点）
                if (row < 3) {
                    zoneDensity = row;          // 0..2 = 密度
                } else {
                    zoneOwnership = row - 4;    // row 4..5 → ownership 0..1
                }
                paramPopup = ParamPopup.NONE;
                paramPopupHoverRow = -1;
                return true;
            }
            // 落在 popup 矩形内（标题栏/小节标题/分割行） → 关闭，不穿透
            if (paramPopupContains(mx, my)) {
                paramPopup = ParamPopup.NONE;
                paramPopupHoverRow = -1;
                return true;
            }
            // 完全在 popup 外 → 关闭 popup，让事件继续走底栏/部门/需求等逻辑
            paramPopup = ParamPopup.NONE;
            paramPopupHoverRow = -1;
        }

        // 需求面板区域点击
        if (showingDemands) {
            handleDemandsClick(mx, my, button);
            return true;
        }

        // 任何鼠标点击 → 锁定区域朝向
        zonePendingRotation = false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // 关闭按钮
            if (inBounds(width - 22, 3, 18, 18)) {
                minecraft.setScreen(null);
                return true;
            }

            // 底部工具栏按钮（与 renderBottomToolbar 布局一致：6 按钮固定）
            int barY = height - BOTTOM_BAR_H;
            int btnW = 64;
            int btnH = 28;
            int gap = 4;
            int btnY = barY + 2;

            int availableWidth = width - RIGHT_PANEL_W;
            int startX = (availableWidth - 6 * (btnW + gap)) / 2;

            // 道路按钮
            int rx = startX;
            if (inBounds(rx, btnY, btnW, btnH)) {
                roadMode = !roadMode;
                if (roadMode) {
                    roadCurvature = 0f;
                    curvedRoad = false;
                }
                return true;
            }

            // 图鉴按钮
            int catX = rx + btnW + gap;
            if (inBounds(catX, btnY, btnW, btnH)) {
                if (catalogPanel != null) {
                    if (catalogPanel.isCatalogMode()) {
                        catalogPanel.close();
                    } else {
                        catalogPanel.open();
                        roadMode = false;
                    }
                }
                return true;
            }

            // 区域按钮
            int zoneX = catX + btnW + gap;
            if (inBounds(zoneX, btnY, btnW, btnH)) {
                if (!zoneMode) {
                    // 区域模式未开 → 打开区域模式（Z 键的等效路径）
                    zoneMode = true;
                } else {
                    // 区域模式已开 → 切换参数 popup（向上弹出"密度+所有权"纵向列表）
                    paramPopup = (paramPopup == ParamPopup.ZONE_PANEL) ? ParamPopup.NONE : ParamPopup.ZONE_PANEL;
                    paramPopupHoverRow = -1;
                }
                return true;
            }

            // 需求按钮（6 按钮固定布局，不受 zoneMode 漂移影响）
            int demX = zoneX + btnW + gap;
            if (inBounds(demX, btnY, btnW, btnH)) {
                showingDemands = !showingDemands;
                if (showingDemands) {
                    com.metrogenesis.network.SyncCityDataMessage.requestRefresh();
                }
                return true;
            }

            // 地图按钮
            int mapX = demX + btnW + gap;
            if (inBounds(mapX, btnY, btnW, btnH)) {
                CityMapScreen.open();
                return true;
            }

            // 部门按钮
            int govX = mapX + btnW + gap;
            if (inBounds(govX, btnY, btnW, btnH)) {
                if (deptPanel != null) {
                    deptPanel.toggle();
                }
                return true;
            }

            // 区域类型选择按钮（右侧面板中）
            if (zoneMode && selectedZone < 0) {
                int panelX = width - RIGHT_PANEL_W;
                for (int i = 0; i < ZONE_TYPES.length; i++) {
                    int[] b = zoneBtnBounds[i];
                    if (b[2] > 0 && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                        selectedZone = i;
                        return true;
                    }
                }
            }

            // 命名/编辑按钮（右侧面板城市概览）
            if (!editingName && nameEditBtnBounds[2] > 0
                && inBounds(nameEditBtnBounds[0], nameEditBtnBounds[1], nameEditBtnBounds[2], nameEditBtnBounds[3])) {
                startNameEdit();
                return true;
            }

            // 左键点击 3D 视图 — 开始拖拽
            if (!inTopBar(mx, my) && !inBottomBar(mx, my) && !inRightPanel(mx, my)) {
                // 图鉴放置模式
                if (catalogPanel != null && catalogPanel.isPlacementMode()) {
                    catalogPanel.setPlacementDragging(true);
                    BlockPos pos = screenToWorld(mx, my);
                    if (pos != null) {
                        catalogPanel.setPlacementTargetPos(pos);
                    }
                    return true;
                }
                // 区域模式
                if (zoneMode && selectedZone >= 0) {
                    BlockPos pos = screenToWorld(mx, my);
                    if (pos != null) {
                        zoneDrawing = true;
                        drawStartX = snapToGrid(pos.getX());
                        drawStartZ = snapToGrid(pos.getZ());
                        drawEndX = drawStartX;
                        drawEndZ = drawStartZ;
                    }
                    return true;
                }
                // 道路模式
                if (roadMode) {
                    BlockPos pos = screenToWorld(mx, my);
                    if (pos != null) {
                        roadPending = false;
                        roadDrawing = true;
                        roadStartBX = snapToGrid(pos.getX());
                        roadStartBZ = snapToGrid(pos.getZ());
                        int[] snappedStart = snapToNearestNode(roadStartBX, roadStartBZ, 2);
                        roadStartBX = snappedStart[0];
                        roadStartBZ = snappedStart[1];
                        roadEndBX = roadStartBX;
                        roadEndBZ = roadStartBZ;
                    }
                    return true;
                }
            }

            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // 右键 = 确认并结束当前操作

            // 正在画区域 → 确认（退出绘制状态，但留在区域模式）
            if (zoneDrawing) {
                zoneDrawing = false;
                selectedZone = -1;
                return true;
            }

            // 正在画道路 → 确认施工
            if (roadDrawing && roadPending) {
                confirmRoadConstruction();
                return true;
            }
            if (roadDrawing) {
                roadDrawing = false;
                return true;
            }

            // 道路模式有预览 → 确认施工
            if (roadMode && roadPending) {
                confirmRoadConstruction();
                return true;
            }
            if (roadMode) {
                roadMode = false;
                return true;
            }

            // 区域模式：右键删除区域（点在区域内即移除）
            if (zoneMode && selectedZone >= 0 && !zoneRects.isEmpty()) {
                BlockPos wp = screenToWorld((int)mx, (int)my);
                if (wp != null) {
                    int wx = wp.getX(), wz = wp.getZ();
                    for (int i = zoneRects.size() - 1; i >= 0; i--) {
                        ZoneRect r = zoneRects.get(i);
                        if (wx >= r.minX() && wx < r.maxX() && wz >= r.minZ() && wz < r.maxZ()) {
                            saveUndoState();
                            zoneRects.remove(i);
                            return true;
                        }
                    }
                }
            }

            // 有选中的分区类型但没在绘制 → 清除选择（回到空闲）
            if (selectedZone >= 0) {
                selectedZone = -1;
                return true;
            }

            // 放置模式下右键 → 开始自由视角旋转
            if (catalogPanel != null && catalogPanel.isPlacementMode()) {
                rightDragging = true;
                return true;
            }

            // 无操作中 → 退出管理书
            minecraft.setScreen(null);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        // 委托给图鉴面板（3D 预览拖拽平移）
        if (catalogPanel != null && catalogPanel.mouseDragged(mx, my, button, dx, dy)) {
            return true;
        }

        // 放置模式拖拽：实时更新放置目标位置
        if (catalogPanel != null && catalogPanel.isPlacementMode()
            && catalogPanel.isPlacementDragging() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            BlockPos pos = screenToWorld(mx, my);
            if (pos != null) {
                catalogPanel.setPlacementTargetPos(pos);
            }
            return true;
        }

        // 道路线段拖拽
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && roadDrawing) {
            BlockPos pos = screenToWorld(mx, my);
            if (pos != null) {
                int rawEndX = snapToGrid(pos.getX());
                int rawEndZ = snapToGrid(pos.getZ());

                // Shift 角度约束：锁定到 45° 倍数
                if (Screen.hasShiftDown()) {
                    int deltaX = rawEndX - roadStartBX;
                    int deltaZ = rawEndZ - roadStartBZ;
                    double angle = Math.atan2(deltaZ, deltaX);
                    double snappedAngle = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
                    double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                    rawEndX = roadStartBX + (int) Math.round(Math.cos(snappedAngle) * dist);
                    rawEndZ = roadStartBZ + (int) Math.round(Math.sin(snappedAngle) * dist);
                }

                roadEndBX = rawEndX;
                roadEndBZ = rawEndZ;

                // 端点吸附（在角度约束之后）
                int[] snapResult = snapToNearestNode(roadEndBX, roadEndBZ, 2);
                roadEndBX = snapResult[0];
                roadEndBZ = snapResult[1];
                roadSnapTarget = (snapResult[0] != rawEndX || snapResult[1] != rawEndZ)
                    ? new int[]{snapResult[0], snapResult[1]} : null;
                // 自环由 RoadBuilder.commitSegment 处理
            }
            return true;
        }

        // 功能区矩形拖拽绘制：实时更新预览
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && zoneDrawing && selectedZone >= 0) {
            BlockPos pos = screenToWorld(mx, my);
            if (pos != null) {
                drawEndX = pos.getX();
                drawEndZ = pos.getZ();
            }
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && rightDragging) {
            // 右键水平拖拽 = 旋转视角
            camYaw += dx * MOUSE_SENSITIVITY;
            // 右键垂直拖拽 = 调整俯仰角（独立于 SHIFT 键）
            camPitch = (float) Math.max(20, Math.min(85, camPitch - dy * MOUSE_SENSITIVITY * 0.5));
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        // 委托给图鉴面板（3D 预览拖拽释放）
        if (catalogPanel != null && catalogPanel.mouseReleased(mx, my, button)) {
            return true;
        }

        // 放置模式拖拽结束 → 锁定位置，进入微调阶段（不再立即弹出确认弹窗）
        if (catalogPanel != null && catalogPanel.isPlacementMode()
            && catalogPanel.isPlacementDragging() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            BlockPos pos = catalogPanel.getPlacementTargetPos();
            catalogPanel.setPlacementDragging(false);
            // 位置已锁定，停留在 placementMode 等待微调或确认
            // 用户可按 Enter 进入确认弹窗，或用 WASD/QE/RF 微调
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // 完成道路段绘制 → 进入投影/待确认状态
            if (roadDrawing && roadMode) {
                roadDrawing = false;
                roadSnapTarget = null; // 清除吸附高亮
                // 保存待确认的道路坐标
                pendingFromBX = roadStartBX;
                pendingFromBZ = roadStartBZ;
                if (!curvedRoad) {
                    // 直道模式：强制对齐到主轴
                    int dx = Math.abs(roadEndBX - roadStartBX);
                    int dz = Math.abs(roadEndBZ - roadStartBZ);
                    pendingToBX = dx >= dz ? roadEndBX : roadStartBX;
                    pendingToBZ = dx >= dz ? roadStartBZ : roadEndBZ;
                } else {
                    pendingToBX = roadEndBX;
                    pendingToBZ = roadEndBZ;
                }
                roadPending = true;
                MetroGenesis.LOGGER.info("Road projection: ({},{}) -> ({},{}) curved={}",
                    pendingFromBX, pendingFromBZ, pendingToBX, pendingToBZ, curvedRoad);
                return true;
            }

            // 完成矩形绘制
            if (zoneDrawing && selectedZone >= 0) {
                BlockPos pos = screenToWorld(mx, my);
                if (pos != null) {
                    drawEndX = pos.getX();
                    drawEndZ = pos.getZ();
                }
                addZoneRect(drawStartX, drawStartZ, drawEndX, drawEndZ, selectedZone);
                zoneDrawing = false;
                return true;
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightDragging = false;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // 图鉴面板滚动
        if (catalogPanel != null && catalogPanel.mouseScrolled(mx, my, delta)) {
            return true;
        }
        // 道路模式：滚轮调整曲率（仅在弯道模式下生效）
        if (roadMode && curvedRoad) {
            roadCurvature += (float) (delta * CURVATURE_STEP);
            roadCurvature = Math.max(-1f, Math.min(1f, roadCurvature));
            // 最小距离守卫：起终点距离 < 3 格时强制直线
            if (roadPending) {
                double dist = Math.sqrt(
                    Math.pow(pendingToBX - pendingFromBX, 2) +
                    Math.pow(pendingToBZ - pendingFromBZ, 2));
                if (dist < 3) roadCurvature = 0f;
            }
            // 刷新贝塞尔预览，使曲率变化实时反映到画面
            if (roadPending) {
                Level clientLevel = minecraft != null ? minecraft.level : null;
                com.metrogenesis.client.RoadRenderer.setBezierPreview(clientLevel,
                    pendingFromBX, pendingFromBZ, pendingToBX, pendingToBZ, roadCurvature, true);
            }
            return true;
        }

        // 区域模式下：滚轮可旋转鼠标悬停的区域朝向（不再依赖 zonePendingRotation）
        if (zoneMode && selectedZone >= 0 && !zoneRects.isEmpty()) {
            BlockPos wp = screenToWorld((int)mx, (int)my);
            if (wp != null) {
                int wx = wp.getX(), wz = wp.getZ();
                for (int i = zoneRects.size() - 1; i >= 0; i--) {
                    ZoneRect r = zoneRects.get(i);
                    if (wx >= r.minX() && wx < r.maxX() && wz >= r.minZ() && wz < r.maxZ()) {
                        int newDir = delta > 0
                            ? (r.direction() + 1) % 4
                            : (r.direction() + 3) % 4;
                        zoneRects.set(i, new ZoneRect(r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), newDir, r.stage(), r.density(), r.ownership()));
                        return true;
                    }
                }
            }
        }

        // 正常相机缩放
        camHeight -= delta * SCROLL_SPEED;
        camHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, camHeight));
        camY = savedY + camHeight;
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        this.mouseX = mx;
        this.mouseY = my;
    }

    // ════════════════════════════════════════════════════════
    //  需求面板点击
    // ════════════════════════════════════════════════════════

    private void handleDemandsClick(double mx, double my, int button) {
        if (button != 0) return;
        final int pw = 340;
        final int ph = Math.min(height - 40, 400);
        final int px = (width - pw) / 2;
        final int py = (height - ph) / 2;
        if (inBounds(px + pw - 18, py + 3, 14, 14)) { showingDemands = false; return; }
        if (inBounds(px + pw - 36, py + 3, 14, 14)) { com.metrogenesis.network.SyncCityDataMessage.requestRefresh(); return; }
        if (mx < px || mx > px + pw || my < py || my > py + ph) { showingDemands = false; }
    }

    /**
     * 屏幕尺寸更新（由 Screen.resize() 触发时同步更新面板尺寸）
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (catalogPanel != null) {
            catalogPanel.resize(width, height);
        }
    }

    // ══ Undo/Redo ═══════════════════════════════════

    private final java.util.Stack<java.util.List<int[]>> undoStack = new java.util.Stack<>();
    private final java.util.Stack<java.util.List<int[]>> redoStack = new java.util.Stack<>();

    /** 道路撤销栈：每个 ChangeStorage 记录该段放的所有方块 */
    private final java.util.Stack<com.metrogenesis.structurize.util.ChangeStorage> roadUndoStack = new java.util.Stack<>();
    private final java.util.Stack<com.metrogenesis.structurize.util.ChangeStorage> roadRedoStack = new java.util.Stack<>();

    /** 区域创建后进入"朝向待确认"状态，滚轮可旋转，直到下一个操作才锁定 */
    private boolean zonePendingRotation = false;

    private void saveUndoState() {
        redoStack.clear();
        undoStack.push(takeCurrentState());
    }

    private void doUndo() {
        // 区域撤销
        if (!undoStack.isEmpty()) {
            redoStack.push(takeCurrentState());
            zoneRects.clear();
            for (int[] z : undoStack.pop()) {
                int dir = z.length >= 6 ? z[5] : 0;
                int stage = z.length >= 7 ? z[6] : 0;
                int density = z.length >= 8 ? z[7] : ZoneRect.DENSITY_MEDIUM;
                int ownership = z.length >= 9 ? z[8] : ZoneRect.OWNERSHIP_PRIVATE;
                zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], dir, stage, density, ownership));
            }
        }
        // 道路撤销：用 ChangeStorage 精确还原（只影响该段记录的方块，不影响其他路）
        if (!roadUndoStack.isEmpty() && minecraft.getSingleplayerServer() != null && minecraft.level != null) {
            var storage = roadUndoStack.pop();
            var serverLevel = minecraft.getSingleplayerServer()
                .getLevel(minecraft.level.dimension());
            if (serverLevel != null) {
                storage.undo(serverLevel, null);
                MetroGenesis.LOGGER.info("Undo road: reverted {} blocks via ChangeStorage", storage.getBlockCount());
            }
            // 保存到 redo 栈
            roadRedoStack.push(storage);
        }
    }

    private void doRedo() {
        // 区域重做
        if (redoStack.isEmpty() && roadRedoStack.isEmpty()) return;
        if (!redoStack.isEmpty()) {
            undoStack.push(takeCurrentState());
            zoneRects.clear();
            for (int[] z : redoStack.pop()) {
                int dir = z.length >= 6 ? z[5] : 0;
                int stage = z.length >= 7 ? z[6] : 0;
                int density = z.length >= 8 ? z[7] : ZoneRect.DENSITY_MEDIUM;
                int ownership = z.length >= 9 ? z[8] : ZoneRect.OWNERSHIP_PRIVATE;
                zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], dir, stage, density, ownership));
            }
        }
        // 道路重做：用 ChangeStorage 精确恢复（redo() 还原到 postState）
        if (!roadRedoStack.isEmpty() && minecraft.getSingleplayerServer() != null && minecraft.level != null) {
            var storage = roadRedoStack.pop();
            var serverLevel = minecraft.getSingleplayerServer()
                .getLevel(minecraft.level.dimension());
            if (serverLevel != null) {
                storage.redo(serverLevel);
                MetroGenesis.LOGGER.info("Redo road: restored {} blocks via ChangeStorage", storage.getBlockCount());
            }
            roadUndoStack.push(storage);
        }
    }

    private java.util.List<int[]> takeCurrentState() {
        java.util.List<int[]> state = new java.util.ArrayList<>();
        for (ZoneRect r : zoneRects) {
            state.add(new int[]{r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), r.direction(), r.stage(), r.density(), r.ownership()});
        }
        return state;
    }

    /**
     * 在指定区域的边界放置施工围栏（ConstructionTape）。
     * 围栏沿区域外周放置，形成矩形边界。
     */
    private void placeZoneFencing(ZoneRect zone) {
        if (minecraft.getSingleplayerServer() == null || minecraft.level == null) return;
        var serverLevel = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
        if (serverLevel == null) return;

        try {
            final var tapeBlock = com.metrogenesis.minecolonies.api.blocks.ModBlocks.blockConstructionTape;
            if (tapeBlock == null) return;

            // 向外扩1格，沿区域边界放置
            int x1 = zone.minX() - 1;
            int z1 = zone.minZ() - 1;
            int x2 = zone.maxX() + 1;
            int z2 = zone.maxZ() + 1;

            // 放置四边
            for (int x = x1; x <= x2; x++) {
                placeTapeAt(serverLevel, x, z1, tapeBlock);
                placeTapeAt(serverLevel, x, z2, tapeBlock);
            }
            for (int z = z1; z <= z2; z++) {
                placeTapeAt(serverLevel, x1, z, tapeBlock);
                placeTapeAt(serverLevel, x2, z, tapeBlock);
            }

            MetroGenesis.LOGGER.info("[Zone] Placed construction tape around zone ({},{})-({},{})",
                zone.minX(), zone.minZ(), zone.maxX(), zone.maxZ());
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("[Zone] Failed to place construction tape", e);
        }
    }

    /**
     * 在地面找到合适的 Y 高度放置围栏方块。
     */
    private void placeTapeAt(ServerLevel level, int x, int z, net.minecraft.world.level.block.Block tapeBlock) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        if (y <= level.getMinBuildHeight()) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, tapeBlock.defaultBlockState(), 3);
        }
    }
}
