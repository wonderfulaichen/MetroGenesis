package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.ZoneData;
import com.metrogenesis.client.ZoneRenderer;
import com.metrogenesis.entity.RtsCameraEntity;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.blueprint.v2.BlueprintLibraryData;
import com.metrogenesis.blueprint.v2.StandardBlueprintData;
import com.metrogenesis.layergrid.LayerGrid;
import com.metrogenesis.layergrid.LayerGridSavedData;
import com.metrogenesis.layergrid.ZoneLayerBridge;
import com.metrogenesis.network.BlueprintPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.util.RotationMirror;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
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

    // 玻璃面板背景（深灰半透明磨砂）
    public static final int GLASS_BG     = 0xCC18182A;   // 主背景
    public static final int GLASS_PANEL  = 0xCC1E1E30;   // 面板
    public static final int GLASS_HEADER = 0xCC222238;   // 标题栏

    // 玻璃边框（柔和暖灰边缘，无蓝色）
    public static final int EDGE_LIGHT   = 0x44484038;   // 上/左亮边
    public static final int EDGE_DARK    = 0x22181010;   // 下/右阴影
    public static final int EDGE_INNER   = 0x33303028;   // 内分隔线

    // 按钮玻璃态
    public static final int BTN_GLASS       = 0x332A2A40; // 常态
    public static final int BTN_GLASS_HOVER = 0x553A3A60; // 悬浮
    public static final int BTN_GLASS_SEL   = 0x66484870; // 选中

    // 文字色
    public static final int TEXT_PRIMARY   = 0xEEE0D8D0;  // 暖白主文字
    public static final int TEXT_SECONDARY = 0xAA9A9088;  // 暖灰次要
    public static final int TEXT_SOFT_GOLD = 0xDDC8B898;  // 柔和金高亮
    public static final int TEXT_GREEN     = 0xCC88B880;  // 柔和绿

    // 功能区色板（柔和低饱和配色）
    private static final int ZONE_RESIDENTIAL = 0xCC88B888; // 住宅 柔和绿
    private static final int ZONE_INDUSTRIAL  = 0xCCB89878; // 工业 暖铜
    private static final int ZONE_COMMERCIAL  = 0xCC88A8B8; // 商业 雾蓝
    private static final int ZONE_AGRICULTURE = 0xCCB8B080; // 农业 暖黄
    private static final int ZONE_PUBLIC      = 0xCCA898B8; // 公共 淡紫

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

    private int selectedZone = -1;
    private boolean zoneDrawing = false;     // 正在拖拽绘制矩形
    private int drawStartX, drawStartZ;      // 绘制起点（世界坐标）
    private int drawEndX, drawEndZ;          // 绘制终点（世界坐标，实时更新）

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

    /**
     * 区域矩形，带朝向。
     *
     * @param direction 朝向（0=东, 1=南, 2=西, 3=北），建筑正面朝此方向
     */
    private record ZoneRect(int minX, int minZ, int maxX, int maxZ, int zoneType, int direction) {
        public int area() { return (maxX - minX) * (maxZ - minZ); }
        /** 获取正面方向（朝向道路/建筑入口的一侧）对应的边坐标 */
        public int frontX() { return direction == 2 ? minX : (direction == 0 ? maxX + 1 : minX); }
        public int frontZ() { return direction == 3 ? minZ : (direction == 1 ? maxZ + 1 : minZ); }
    }

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
                        zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], dir));
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
                        saveList.add(new int[]{r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), r.direction()});
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

        // ═════════════════════════════════════════════════
        // 帧时间移动（参考 RTSbuilding smooth mode）
        // 按帧时间计算移动量，而非按 tick — 无论帧率如何都丝滑
        // 图鉴/放置模式下跳过相机移动
        // ═════════════════════════════════════════════════
        long now = System.nanoTime();
        float tickDelta = (now - lastFrameNanos) / 50_000_000.0f; // 50ms = 1 tick
        tickDelta = Math.min(tickDelta, 2.0f); // 上限防止跳帧
        lastFrameNanos = now;

        if ((catalogPanel == null || (!catalogPanel.isCatalogMode() && !catalogPanel.isPlacementMode()))) {
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
            String modeText = curvedRoad ? "\u5f2f\u9053\u6a21\u5f0f" : "\u76f4\u9053\u6a21\u5f0f";
            String curvText = curvedRoad
                ? String.format("%s | \u66f2\u7387: %.2f [T\u5207\u6362]", modeText, roadCurvature)
                : String.format("%s [T\u5207\u6362]", modeText);
            drawGlassText(g, curvText, 8, height - BOTTOM_BAR_H - 44, TEXT_SOFT_GOLD, 255);
        }

        // 应用淡入动画（整体透明度）
        int alpha = (int) (fadeInAlpha * 255) & 0xFF;

        // 图鉴面板模式（含放置确认 + 拖拽定位）
        if (catalogPanel != null) {
            catalogPanel.render(g, alpha);
            if (catalogPanel.isCatalogMode()) {
                if (catalogPanel.isPlacementMode() || catalogPanel.isPlacementConfirm()) {
                    renderCrosshair(g, mx, my);
                }
                return;
            }
        }

        // 正常城市管理模式
        // 顶部状态栏
        renderTopBar(g, alpha);

        // 底部工具栏
        renderBottomToolbar(g, alpha);

        // 右侧信息面板
        renderRightPanel(g, alpha);

        // 左下角坐标
        renderCoordinates(g, alpha);

        // Create 风格十字准星
        renderCrosshair(g, mx, my);
    }

    /**
     * 顶部状态栏 — Create 风格黄铜边框
     */
    private void renderTopBar(GuiGraphics g, int alpha) {
        int h = TOP_BAR_H;

        // 背景
        fillGlassPanel(g, 0, 0, width, h, GLASS_HEADER, alpha);

        // 城市名称
        drawGlassText(g, "\u57ce\u90a6\u7ba1\u7406", 8, 7, TEXT_SOFT_GOLD, alpha);

        // 分隔线
        g.fill(85, 3, 86, h - 3, withAlpha(EDGE_INNER, alpha));

        // 指标
        int cx = width / 2;
        drawStatGlass(g, cx - 130, 7, "\u56fd\u5e93:", "0 C-Value", TEXT_SOFT_GOLD, alpha);
        drawStatGlass(g, cx - 20, 7, "\u4eba\u53e3:", "0/0", TEXT_GREEN, alpha);
        drawStatGlass(g, cx + 70, 7, "\u6ee1\u610f\u5ea6:", "0%", TEXT_PRIMARY, alpha);

        // 关闭按钮
        int closeX = width - 22;
        int closeY = 3;
        boolean hover = inBounds(closeX, closeY, 18, 18);
        int closeBg = hover ? BTN_GLASS_HOVER : BTN_GLASS;
        fillGlassButton(g, closeX, closeY, 18, 18, closeBg, alpha);
        drawGlassText(g, "\u00d7", closeX + 5, closeY + 5, TEXT_PRIMARY, alpha);
    }

    /**
     * 底部工具栏 — 玻璃态功能区按钮
     */
    private void renderBottomToolbar(GuiGraphics g, int alpha) {
        int barY = height - BOTTOM_BAR_H;

        // 玻璃背景
        fillGlassPanel(g, 0, barY, width, BOTTOM_BAR_H, GLASS_HEADER, alpha);

        // 功能区按钮
        int btnW = 64;
        int btnH = 28;
        int gap = 4;
        int totalW = ZONE_TYPES.length * btnW + (ZONE_TYPES.length - 1) * gap;
        int startX = (width - totalW) / 2;
        int btnY = barY + 2;

        for (int i = 0; i < ZONE_TYPES.length; i++) {
            int x = startX + i * (btnW + gap);
            boolean selected = (selectedZone == i);
            boolean hover = inBounds(x, btnY, btnW, btnH);

            // 玻璃态按钮
            int bg = selected ? ZONE_COLORS[i] : (hover ? BTN_GLASS_HOVER : BTN_GLASS);
            fillGlassButton(g, x, btnY, btnW, btnH, bg, alpha);

            // 选中时柔和高亮条
            if (selected) {
                g.fill(x + 4, btnY + btnH - 2, x + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
            }

            // 键位提示
            String keyHint = String.valueOf(i + 1);
            drawGlassText(g, keyHint, x + 3, btnY + 2, selected ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

            // 图标 + 文字
            String label = Component.translatable("gui.metrogenesis.mayor_book.zone." + ZONE_TYPES[i]).getString();
            String icon = ZONE_ICONS[i];
            String text = icon + " " + label;
            int textW = font.width(text);
            int textX = x + (btnW - textW) / 2;
            int textY = btnY + (btnH - font.lineHeight) / 2 + 4;
            drawGlassText(g, text, textX, textY, selected ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        }

        // 分隔线 + 道路按钮
        int sepX = startX + ZONE_TYPES.length * (btnW + gap) + 8;
        g.fill(sepX, btnY + 2, sepX + 1, btnY + btnH - 2, withAlpha(EDGE_INNER, alpha));
        int roadBtnX = sepX + 12;
        boolean roadHover = inBounds(roadBtnX, btnY, btnW, btnH);
        int roadBg = roadMode ? BTN_GLASS_SEL : (roadHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, roadBtnX, btnY, btnW, btnH, roadBg, alpha);
        if (roadMode) {
            g.fill(roadBtnX + 4, btnY + btnH - 2, roadBtnX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "R", roadBtnX + 3, btnY + 2, roadMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, "\u26F5 \u9053\u8DEF", roadBtnX + 8, btnY + btnH / 2 + 2, roadMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // 图鉴按钮（T 键）
        int catBtnX = roadBtnX + btnW + gap + 8;
        boolean catHover = inBounds(catBtnX, btnY, btnW, btnH);
        boolean catMode = catalogPanel != null && catalogPanel.isCatalogMode();
        int catBg = catMode ? BTN_GLASS_SEL : (catHover ? BTN_GLASS_HOVER : BTN_GLASS);
        fillGlassButton(g, catBtnX, btnY, btnW, btnH, catBg, alpha);
        if (catMode) {
            g.fill(catBtnX + 4, btnY + btnH - 2, catBtnX + btnW - 4, btnY + btnH, withAlpha(TEXT_SOFT_GOLD, alpha));
        }
        drawGlassText(g, "T", catBtnX + 3, btnY + 2, catMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);
        drawGlassText(g, "\uD83D\uDCD6 \u56FE\u9274", catBtnX + 8, btnY + btnH / 2 + 2, catMode ? TEXT_PRIMARY : TEXT_SECONDARY, alpha);

        // ═══ 地图按钮（M 键）═══
        int mapBtnX = catBtnX + btnW + gap + 8;
        boolean mapHover = inBounds(mapBtnX, btnY, btnW, btnH);
        int mapBg = mapHover ? BTN_GLASS_HOVER : BTN_GLASS;
        fillGlassButton(g, mapBtnX, btnY, btnW, btnH, mapBg, alpha);
        drawGlassText(g, "M", mapBtnX + 3, btnY + 2, TEXT_SECONDARY, alpha);
        drawGlassText(g, "\uD83D\uDDFA \u5730\u56FE", mapBtnX + 8, btnY + btnH / 2 + 2, TEXT_SECONDARY, alpha);

        // 底部操作提示栏
        String modeHint;
        if (roadMode && roadPending) {
            modeHint = "[\u53f3\u952e] \u786e\u8ba4\u65bd\u5de5 [\u5de6\u952e] \u91cd\u65b0\u7ed8\u5236 [Esc] \u53d6\u6d88   \u6295\u5f71\u5f85\u786e\u8ba4";
        } else if (roadMode) {
            modeHint = "[\u5de6\u952e\u62d6\u62fd] \u7ed8\u5236\u9053\u8def [\u53f3\u952e] \u9000\u51fa   \u6a21\u5f0f: \u7ed8\u5236\u9053\u8def";
        } else if (selectedZone >= 0) {
            modeHint = "[WASD] \u79fb\u52a8 [QE] \u65cb\u8f6c [Esc] \u9000\u51fa   \u6a21\u5f0f: \u7ed8\u5236 " + Component.translatable("gui.metrogenesis.mayor_book.zone." + ZONE_TYPES[selectedZone]).getString();
        } else {
            modeHint = "[WASD] \u79fb\u52a8 [QE] \u65cb\u8f6c [1-5] \u9009\u62e9\u533a\u57df [R] \u9053\u8def  \u6a21\u5f0f: \u6d4f\u89c8";
        }
        drawGlassText(g, modeHint, 4, barY + btnY + btnH - barY + 1, TEXT_SECONDARY, alpha);
    }

    /**
     * 右侧信息面板 — 玻璃态风格
     */
    private void renderRightPanel(GuiGraphics g, int alpha) {
        int panelX = width - RIGHT_PANEL_W;
        int panelY = TOP_BAR_H + 4;
        int panelH = height - TOP_BAR_H - BOTTOM_BAR_H - 8;

        // 玻璃面板
        fillGlassPanel(g, panelX, panelY, RIGHT_PANEL_W, panelH, GLASS_PANEL, alpha);

        // 标题
        drawGlassText(g, "\u57ce\u5e02\u4fe1\u606f", panelX + 8, panelY + 8, TEXT_SOFT_GOLD, alpha);
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
            drawInfoGlass(g, panelX + 8, row, "\u5df2\u89c4\u5212:", zoneCount + " \u4e2a\u65b9\u5757", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u5efa\u7b51\u6570\u91cf:", "\u6682\u65e0", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u5e02\u6c11\u5de5\u4f5c:", "\u6682\u65e0", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u4ea7\u91cf\u7edf\u8ba1:", "\u6682\u65e0", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u533a\u57df\u72b6\u6001:", "\u6682\u65e0", alpha);
        } else {
            // 城市概览
            drawInfoGlass(g, panelX + 8, row, "\u57ce\u5e02\u540d\u79f0:", "\u672a\u547d\u540d", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u56fd\u5e93\u8d44\u91d1:", "0 C-Value", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u5e02\u6c11\u4eba\u53e3:", "0/0", alpha); row += 14;
            drawInfoGlass(g, panelX + 8, row, "\u6574\u4f53\u6ee1\u610f\u5ea6:", "0%", alpha); row += 20;
            g.fill(panelX + 8, row, panelX + RIGHT_PANEL_W - 8, row + 1, withAlpha(EDGE_INNER, alpha));
            row += 6;

            // 功能区统计
            drawGlassText(g, "\u529f\u80fd\u533a\u7edf\u8ba1", panelX + 8, row, TEXT_SOFT_GOLD, alpha);
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
                drawGlassText(g, "\u5c1a\u672a\u89c4\u5212\u4efb\u4f55\u533a\u57df", panelX + 8, row, TEXT_SECONDARY, alpha);
                row += 14;
            }

            row += 6;
            drawGlassText(g, "\u5de6\u952e: \u653e\u7f6e | \u53f3\u952e: \u5220\u9664", panelX + 8, row, TEXT_SECONDARY, alpha);
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
    private void fillRoundRect(GuiGraphics g, int x, int y, int w, int h, int color) {
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
    public void fillGlassPanel(GuiGraphics g, int x, int y, int w, int h, int bgColor, int alpha) {
        if (w <= 4 || h <= 4) { g.fill(x, y, x + w, y + h, withAlpha(bgColor, alpha)); return; }
        fillRoundRect(g, x, y, w, h, withAlpha(bgColor, alpha));

        // 边缘光晕（上+左暖灰亮边，下+右半透明阴影）
        g.fill(x + 2, y, x + w - 2, y + 1, withAlpha(EDGE_LIGHT, alpha));
        g.fill(x, y + 2, x + 1, y + h - 2, withAlpha(EDGE_LIGHT, alpha));
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, withAlpha(EDGE_DARK, alpha));
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, withAlpha(EDGE_DARK, alpha));
    }

    /**
     * 玻璃态按钮（圆角半透明背景 + 悬浮微发光）
     */
    public void fillGlassButton(GuiGraphics g, int x, int y, int w, int h, int bgColor, int alpha) {
        fillRoundRect(g, x, y, w, h, withAlpha(bgColor, alpha));
        // 顶部极细亮边（模拟玻璃反光）
        if (w > 4) {
            g.fill(x + 2, y, x + w - 2, y + 1, withAlpha(EDGE_LIGHT, alpha));
        }
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

        zoneRects.add(new ZoneRect(minX, minZ, maxX, maxZ, zoneType, dir));
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

        // 任何按键 → 锁定区域朝向
        zonePendingRotation = false;

        // ESC 分段处理：图鉴(含放置确认/模式) → 拖拽 → 投影 → 道路 → 分区 → 关闭
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
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
            if (selectedZone >= 0) {
                selectedZone = -1;
                return true;
            }
            // 关闭界面
            minecraft.setScreen(null);
            return true;
        }

        // T 键：切换图鉴模式（道路模式下由后面的处理器处理直道/弯道切换）
        if (keyCode == GLFW.GLFW_KEY_T && !roadMode
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

        // M 键：打开城市地图视图
        if (keyCode == GLFW.GLFW_KEY_M && !catalogPanel.isCatalogMode() && !roadMode) {
            CityMapScreen.open();
            return true;
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

        // 数字键快捷选择功能区（1-5 切换，再按取消）
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_5) {
            int idx = keyCode - GLFW.GLFW_KEY_1;
            selectedZone = (selectedZone == idx) ? -1 : idx;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_0) {
            selectedZone = -1;
            return true;
        }

        // R 键：切换道路绘制模式
        if (keyCode == GLFW.GLFW_KEY_R) {
            roadMode = !roadMode;
            if (roadMode) {
                selectedZone = -1;
                zoneDrawing = false;
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
    public boolean mouseClicked(double mx, double my, int button) {
        // 委托给图鉴面板（含放置确认 + 拖拽定位）
        if (catalogPanel != null && catalogPanel.mouseClicked(mx, my, button)) {
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

            // 底部工具栏按钮
            int barY = height - BOTTOM_BAR_H;
            int btnW = 64;
            int btnH = 28;
            int gap = 4;
            int totalW = ZONE_TYPES.length * btnW + (ZONE_TYPES.length - 1) * gap;
            int startX = (width - totalW) / 2;
            int btnY = barY + (BOTTOM_BAR_H - btnH) / 2;

            for (int i = 0; i < ZONE_TYPES.length; i++) {
                int x = startX + i * (btnW + gap);
                if (inBounds(x, btnY, btnW, btnH)) {
                    selectedZone = (selectedZone == i) ? -1 : i;
                    zoneDrawing = false; // 取消进行中的绘制
                    return true;
                }
            }

            // 道路按钮
            int sepX = startX + ZONE_TYPES.length * (btnW + gap) + 8;
            int roadBtnX = sepX + 12;
            if (inBounds(roadBtnX, btnY, btnW, btnH)) {
                roadMode = !roadMode;
                if (roadMode) {
                    selectedZone = -1;
                    zoneDrawing = false;
                    roadCurvature = 0f;
                }
                return true;
            }

            // 图鉴按钮
            int catBtnX = roadBtnX + btnW + gap + 8;
            if (inBounds(catBtnX, btnY, btnW, btnH)) {
                if (catalogPanel != null) {
                    if (catalogPanel.isCatalogMode()) {
                        catalogPanel.close();
                    } else {
                        catalogPanel.open();
                        // 退出其他模式
                        selectedZone = -1;
                        zoneDrawing = false;
                        roadMode = false;
                    }
                }
                return true;
            }

            // 左键点击 3D 视图 — 开始矩形/道路绘制
            if (!inTopBar(mx, my) && !inBottomBar(mx, my) && !inRightPanel(mx, my)) {
                // 道路模式
                if (roadMode) {
                    BlockPos pos = screenToWorld(mx, my);
                    if (pos != null) {
                        roadPending = false; // 清除之前的投影
                        roadDrawing = true;
                        roadStartBX = snapToGrid(pos.getX());
                        roadStartBZ = snapToGrid(pos.getZ());
                        // 端点吸附：检查附近是否有已存在的道路节点
                        int[] snappedStart = snapToNearestNode(roadStartBX, roadStartBZ, 2);
                        roadStartBX = snappedStart[0];
                        roadStartBZ = snappedStart[1];
                        roadEndBX = roadStartBX;
                        roadEndBZ = roadStartBZ;
                    }
                    return true;
                }
                // 分区模式
                if (selectedZone >= 0) {
                    BlockPos pos = screenToWorld(mx, my);
                    if (pos != null) {
                        zoneDrawing = true;
                        drawStartX = pos.getX();
                        drawStartZ = pos.getZ();
                        drawEndX = pos.getX();
                        drawEndZ = pos.getZ();
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

            // 有选中的分区类型但没在绘制 → 清除选择（回到空闲）
            if (selectedZone >= 0) {
                selectedZone = -1;
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
        // 放置模式拖拽结束 → 弹出确认弹窗
        if (catalogPanel != null && catalogPanel.isPlacementMode()
            && catalogPanel.isPlacementDragging() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            BlockPos pos = catalogPanel.getPlacementTargetPos();
            catalogPanel.setPlacementDragging(false);
            if (pos != null) {
                catalogPanel.confirmPlacementPosition(pos);
            }
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

        // 非道路模式下：区域朝向待确认时，滚轮可旋转最新区域
        if (!roadMode && zonePendingRotation && !zoneRects.isEmpty()) {
            BlockPos wp = screenToWorld((int)mx, (int)my);
            if (wp != null) {
                int wx = wp.getX();
                int wz = wp.getZ();
                // 只调整最后一个创建的区域
                ZoneRect last = zoneRects.get(zoneRects.size() - 1);
                if (wx >= last.minX() && wx < last.maxX() && wz >= last.minZ() && wz < last.maxZ()) {
                    int newDir = delta > 0
                        ? (last.direction() + 1) % 4
                        : (last.direction() + 3) % 4;
                    zoneRects.set(zoneRects.size() - 1,
                        new ZoneRect(last.minX(), last.minZ(), last.maxX(), last.maxZ(), last.zoneType(), newDir));
                    return true;
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
                zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], z.length >= 6 ? z[5] : 0));
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
                zoneRects.add(new ZoneRect(z[0], z[1], z[2], z[3], z[4], z.length >= 6 ? z[5] : 0));
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
            state.add(new int[]{r.minX(), r.minZ(), r.maxX(), r.maxZ(), r.zoneType(), r.direction()});
        }
        return state;
    }
}
