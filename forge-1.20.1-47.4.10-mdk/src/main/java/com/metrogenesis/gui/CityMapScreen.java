package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.RoadData;
import com.metrogenesis.RoadData.NodePos;
import com.metrogenesis.RoadData.RoadSegment;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.locale.Language;

import java.util.Set;

/**
 * 城市地图 — 真实地形渲染 + 区域/道路/玩家叠加层。
 * <p>
 * 参考 Xaero's Minimap 风格，从世界读取方块 MapColor 生成像素级地形底图，
 * 在其上叠加功能区着色、道路网络、节点标记和玩家位置。
 * </p>
 *
 * <h3>渲染层顺序</h3>
 * <ol>
 *   <li>地形底图（世界方块 MapColor + 亮度）</li>
 *   <li>功能区着色（半透明覆盖）</li>
 *   <li>道路网络（线段 + 节点点）</li>
 *   <li>玩家位置（十字准星）</li>
 *   <li>UI 边框 + 图例</li>
 * </ol>
 */
public class CityMapScreen extends Screen {

    // ════════════════════════════════════════════════════════
    //  颜色常量
    // ════════════════════════════════════════════════════════

    private static final int BG_COLOR = MGStyles.C_BG;
    private static final int BORDER_COLOR = MGStyles.C_BRASS;
    private static final int TITLE_BG = MGStyles.C_BG_PANEL;

    private static final int[] ZONE_COLORS = {
        0x996B9E6B, // 0 住宅 橄榄绿
        0x99B87333, // 1 工业 铜色
        0x996B8EAA, // 2 商业 雾蓝
        0x99B8A044, // 3 农业 麦穗金
        0x99A898B8, // 4 公共 淡紫
    };

    private static final int ROAD_LINE_COLOR = 0xFFA0A0A0;
    private static final int NODE_COLOR = MGStyles.C_TEXT_BRASS;
    private static final int PLAYER_COLOR = 0xFFFFFFFF;
    private static final int TEXT_WHITE = 0xFFEEE8E0;
    private static final int TEXT_GOLD = MGStyles.C_TEXT_BRASS;

    // ════════════════════════════════════════════════════════
    //  渲染参数
    // ════════════════════════════════════════════════════════

    /** 地形像素网格分辨率（N×N），控制纹理生成质量/性能 */
    private static final int TERRAIN_RES = 256;
    /** 单帧最大扫描方块数（防止卡顿） */
    private static final int MAX_SCAN_BLOCKS = 60_000;

    // ════════════════════════════════════════════════════════
    //  状态
    // ════════════════════════════════════════════════════════

    private double centerX = 0;
    private double centerZ = 0;
    private double zoom = 1.0;

    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.25;

    private boolean dragging = false;
    private double lastMouseX, lastMouseY;

    // ══ 地形纹理缓存 ═══════════════════════════════════
    /** 最近一次地形扫描的世界坐标范围，用于判断是否需要重新扫描 */
    private int lastScanMinX, lastScanMinZ, lastScanStride;
    private DynamicTexture terrainTexture;
    private ResourceLocation terrainTextureId;
    /** 地图区域尺寸（在 render() 中计算，地形重建时使用） */
    private int cachedMapW, cachedMapH;

    // ════════════════════════════════════════════════════════
    //  构造
    // ════════════════════════════════════════════════════════

    public CityMapScreen() {
        super(Component.translatable("gui.metrogenesis.citymap.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.player != null) {
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
        }
    }

    @Override
    public void removed() {
        super.removed();
        // 释放纹理资源
        if (terrainTexture != null) {
            terrainTexture.close();
            terrainTexture = null;
            terrainTextureId = null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  主渲染
    // ════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (minecraft == null || minecraft.level == null) {
            g.fill(0, 0, width, height, 0xFF000000);
            return;
        }

        // 背景
        g.fill(0, 0, width, height, BG_COLOR);

        // 地图绘制区域
        int mapX = 8, mapY = 30;
        int mapW = width - 16, mapH = height - 38;

        // 缓存地图尺寸，供地形重建判断
        cachedMapW = mapW;
        cachedMapH = mapH;

        // 裁剪区域
        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);

        // 1. 地形底图
        renderTerrain(g, mapX, mapY, mapW, mapH);

        // 2. 功能区着色
        renderZones(g, mapX, mapY, mapW, mapH);

        // 3. 道路网络
        renderRoads(g, mapX, mapY, mapW, mapH);

        // 4. 玩家位置
        renderPlayerPos(g, mapX, mapY, mapW, mapH);

        g.disableScissor();

        // 边框
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY, BORDER_COLOR);
        g.fill(mapX - 1, mapY + mapH, mapX + mapW + 1, mapY + mapH + 1, BORDER_COLOR);
        g.fill(mapX - 1, mapY - 1, mapX, mapY + mapH + 1, BORDER_COLOR);
        g.fill(mapX + mapW, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, BORDER_COLOR);

        // 标题栏
        g.fill(mapX, mapY - 26, mapX + mapW, mapY - 1, TITLE_BG);
        g.drawString(minecraft.font, "gui.metrogenesis.citymap.hint",
            mapX + 8, mapY - 20, TEXT_GOLD);

        // 图例
        renderLegend(g, mapX, mapY, mapW, mapH);
    }

    // ════════════════════════════════════════════════════════
    //  1. 地形渲染（世界方块 → 像素地图）
    // ════════════════════════════════════════════════════════

    /**
     * 渲染世界地形底图。
     * <p>
     * 将可见范围内的世界方块 MapColor 采样到 {@value #TERRAIN_RES}×{@value #TERRAIN_RES}
     * 纹理上，通过 {@link DynamicTexture} 一次性绘制，避免每帧数万次 fill 调用。
     * 纹理在偏移/缩放变化时自动重建。
     * </p>
     */
    private void renderTerrain(GuiGraphics g, int mapX, int mapY, int mapW, int mapH) {
        if (minecraft.level == null) return;

        Level level = minecraft.level;

        // 计算世界可见范围（方块坐标）
        int halfBlocksX = (int) Math.ceil(mapW / (2.0 * zoom));
        int halfBlocksZ = (int) Math.ceil(mapH / (2.0 * zoom));
        int minBX = (int) Math.floor(centerX) - halfBlocksX;
        int minBZ = (int) Math.floor(centerZ) - halfBlocksZ;
        int blocksX = halfBlocksX * 2;
        int blocksZ = halfBlocksZ * 2;

        // 采样步长：确保总扫描数不超过 MAX_SCAN_BLOCKS
        int stride = (int) Math.ceil(Math.sqrt((double) (blocksX * blocksZ) / MAX_SCAN_BLOCKS));
        stride = Math.max(1, stride);

        // 最终扫描网格尺寸（用于纹理生成）
        int scanW = Math.min(TERRAIN_RES, (blocksX + stride - 1) / stride);
        int scanH = Math.min(TERRAIN_RES, (blocksZ + stride - 1) / stride);
        // 确保至少 1x1
        scanW = Math.max(1, scanW);
        scanH = Math.max(1, scanH);

        // 检查是否需要重建纹理
        boolean needsRebuild = terrainTexture == null
            || terrainTextureId == null
            || minBX != lastScanMinX
            || minBZ != lastScanMinZ
            || stride != lastScanStride;

        if (needsRebuild) {
            rebuildTerrain(level, minBX, minBZ, stride, scanW, scanH);
            lastScanMinX = minBX;
            lastScanMinZ = minBZ;
            lastScanStride = stride;
        }

        // 绘制地形纹理
        if (terrainTextureId != null) {
            // 纹理覆盖从 minBX 到 minBX+stride*scanW-1 的世界区域
            int worldTexW = stride * scanW;
            int worldTexH = stride * scanH;
            int sx = worldToScreenX(minBX, mapX, mapW);
            int sy = worldToScreenY(minBZ, mapY, mapH);
            int ex = worldToScreenX(minBX + worldTexW, mapX, mapW);
            int ey = worldToScreenY(minBZ + worldTexH, mapY, mapH);
            g.blit(terrainTextureId, sx, sy, ex - sx, ey - sy, 0, 0, scanW, scanH, scanW, scanH);
        }
    }

    /**
     * 重建地形纹理：扫描世界方块 → 写入 NativeImage → 更新 DynamicTexture。
     */
    private void rebuildTerrain(Level level, int minBX, int minBZ, int stride, int scanW, int scanH) {
        // 释放旧纹理
        if (terrainTexture != null) {
            terrainTexture.close();
            terrainTexture = null;
            terrainTextureId = null;
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, scanW, scanH, false);

        for (int py = 0; py < scanH; py++) {
            for (int px = 0; px < scanW; px++) {
                int wx = minBX + px * stride;
                int wz = minBZ + py * stride;

                int color = sampleBlockColor(level, wx, wz);
                image.setPixelRGBA(px, py, color);
            }
        }

        terrainTexture = new DynamicTexture(image);
        terrainTextureId = minecraft.getTextureManager().register("citymap_terrain", terrainTexture);
    }

    /**
     * 获取指定 (x,z) 位置地表方块的地图颜色，返回 RGBA 格式像素值。
     * <p>
     * 使用 {@link Heightmap#WORLD_SURFACE} 找到地表最高方块，
     * 读取 {@link MapColor#col} 并使用原色（不额外调亮）。
     * 若方块为空气或不可见，则返回黑色不透明。
     * </p>
     *
     * <h3>设计说明</h3>
     * 与 Minecraft 地图物品一样，使用 MapColor 原色渲染地表，
     * 不额外做亮度/饱和度调整，保证地图颜色与游戏内一致。
     */
    private int sampleBlockColor(Level level, int wx, int wz) {
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
        if (topY < level.getMinBuildHeight()) {
            return packRGBA(0, 0, 0, 255);
        }

        BlockState state;
        int y = topY;
        while (y >= level.getMinBuildHeight()) {
            state = level.getBlockState(new net.minecraft.core.BlockPos(wx, y, wz));
            if (!state.isAir()) {
                MapColor mapColor = state.getMapColor(level, new net.minecraft.core.BlockPos(wx, y, wz));
                if (mapColor != null) {
                    // col = 0xRRGGBB，直接使用原色
                    int col = mapColor.col;
                    int r = (col >> 16) & 0xFF;
                    int g = (col >> 8) & 0xFF;
                    int b = col & 0xFF;
                    return packRGBA(r, g, b, 255);
                }
                break;
            }
            y--;
        }

        return packRGBA(0, 44, 16, 255); // 默认深绿色（森林/草地）
    }

    /**
     * 将 RGBA 分量打包为 NativeImage 的 RGBA 像素格式（ABGR 小端序）。
     */
    private int packRGBA(int r, int g, int b, int a) {
        // NativeImage.RGBA 使用 A→B→G→R 字节序（小端 = int ABGR）
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    // ════════════════════════════════════════════════════════
    //  2. 功能区着色
    // ════════════════════════════════════════════════════════

    private void renderZones(GuiGraphics g, int mapX, int mapY, int mapW, int mapH) {
        if (minecraft == null || minecraft.level == null) return;
        try {
            if (minecraft.getSingleplayerServer() == null) return;
            var serverLevel = minecraft.getSingleplayerServer()
                .getLevel(minecraft.level.dimension());
            if (serverLevel == null) return;

            var zoneData = com.metrogenesis.ZoneData.getOrCreate(serverLevel);
            for (int[] zone : zoneData.getZones()) {
                if (zone.length < 5) continue;
                int minX = zone[0], minZ = zone[1], maxX = zone[2], maxZ = zone[3], type = zone[4];
                if (type < 0 || type >= ZONE_COLORS.length) continue;

                int color = ZONE_COLORS[type];
                int sx = worldToScreenX(minX, mapX, mapW);
                int sz = worldToScreenY(minZ, mapY, mapH);
                int ex = worldToScreenX(maxX + 1, mapX, mapW);
                int ez = worldToScreenY(maxZ + 1, mapY, mapH);

                int w = Math.max(ex - sx, 2);
                int h = Math.max(ez - sz, 2);

                g.fill(sx, sz, sx + w, sz + h, color);
            }
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════
    //  3. 道路网络
    // ════════════════════════════════════════════════════════

    private void renderRoads(GuiGraphics g, int mapX, int mapY, int mapW, int mapH) {
        if (minecraft == null || minecraft.level == null) return;
        try {
            if (minecraft.getSingleplayerServer() == null) return;
            var serverLevel = minecraft.getSingleplayerServer()
                .getLevel(minecraft.level.dimension());
            if (serverLevel == null) return;

            var roadData = RoadData.getOrCreate(serverLevel);
            Set<RoadSegment> segments = roadData.getSegments();

            // 画边
            for (RoadSegment seg : segments) {
                int sx = worldToScreenX(seg.from().blockX(), mapX, mapW);
                int sz = worldToScreenY(seg.from().blockZ(), mapY, mapH);
                int ex = worldToScreenX(seg.to().blockX(), mapX, mapW);
                int ez = worldToScreenY(seg.to().blockZ(), mapY, mapH);
                drawLine(g, sx, sz, ex, ez, ROAD_LINE_COLOR, 2);
            }

            // 画节点
            Set<NodePos> nodes = roadData.getAllNodes();
            int dotSize = Math.max(3, (int)(zoom * 2));
            for (NodePos node : nodes) {
                int nx = worldToScreenX(node.blockX(), mapX, mapW);
                int nz = worldToScreenY(node.blockZ(), mapY, mapH);
                g.fill(nx - dotSize / 2, nz - dotSize / 2,
                       nx - dotSize / 2 + dotSize, nz - dotSize / 2 + dotSize, NODE_COLOR);
            }
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════
    //  4. 玩家位置
    // ════════════════════════════════════════════════════════

    private void renderPlayerPos(GuiGraphics g, int mapX, int mapY, int mapW, int mapH) {
        if (minecraft == null || minecraft.player == null) return;
        int px = worldToScreenX((int) minecraft.player.getX(), mapX, mapW);
        int pz = worldToScreenY((int) minecraft.player.getZ(), mapY, mapH);

        // 十字准星（带外发光）
        g.fill(px - 4, pz, px + 5, pz + 1, PLAYER_COLOR);
        g.fill(px, pz - 4, px + 1, pz + 5, PLAYER_COLOR);
        // 中心点
        g.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFFFF4444);
    }

    // ════════════════════════════════════════════════════════
    //  图例
    // ════════════════════════════════════════════════════════

    private void renderLegend(GuiGraphics g, int mapX, int mapY, int mapW, int mapH) {
        int lx = mapX + mapW - 120;
        int ly = mapY + mapH - 140;

        g.fill(lx, ly, lx + 116, ly + 136, MGStyles.C_BG_PANEL);

        g.drawString(minecraft.font, "gui.metrogenesis.citymap.legend", lx + 4, ly + 2, TEXT_GOLD);

        // 区域
        for (int i = 0; i < ZONE_COLORS.length; i++) {
            int y = ly + 16 + i * 14;
            g.fill(lx + 4, y, lx + 16, y + 10, ZONE_COLORS[i]);
            g.drawString(minecraft.font, Language.getInstance().getOrDefault("gui.metrogenesis.citymap.zone" + i), lx + 20, y, TEXT_WHITE);
        }

        // 道路
        int ry = ly + 16 + ZONE_COLORS.length * 14;
        g.fill(lx + 4, ry, lx + 20, ry + 2, ROAD_LINE_COLOR);
        g.drawString(minecraft.font, "gui.metrogenesis.citymap.road", lx + 24, ry - 2, TEXT_WHITE);

        // 节点
        int ny = ry + 14;
        g.fill(lx + 6, ny + 2, lx + 14, ny + 10, NODE_COLOR);
        g.drawString(minecraft.font, "gui.metrogenesis.citymap.node", lx + 20, ny, TEXT_WHITE);

        // 玩家
        int py = ny + 14;
        g.fill(lx + 9, py, lx + 11, py + 10, PLAYER_COLOR);
        g.fill(lx + 4, py + 4, lx + 16, py + 6, PLAYER_COLOR);
        g.drawString(minecraft.font, "gui.metrogenesis.citymap.player", lx + 20, py, TEXT_WHITE);
    }

    // ════════════════════════════════════════════════════════
    //  坐标转换
    // ════════════════════════════════════════════════════════

    private int worldToScreenX(int wx, int mapX, int mapW) {
        return (int)(mapX + mapW / 2.0 + (wx - centerX) * zoom);
    }

    private int worldToScreenY(int wz, int mapY, int mapH) {
        return (int)(mapY + mapH / 2.0 + (wz - centerZ) * zoom);
    }

    // ════════════════════════════════════════════════════════
    //  绘制工具
    // ════════════════════════════════════════════════════════

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x1 - thickness / 2, y1 - thickness / 2,
                   x1 - thickness / 2 + thickness, y1 - thickness / 2 + thickness, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    // ════════════════════════════════════════════════════════
    //  输入处理
    // ════════════════════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double oldZoom = zoom;
        zoom += delta > 0 ? ZOOM_STEP : -ZOOM_STEP;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        if (zoom != oldZoom) {
            double scale = zoom / oldZoom;
            int mapX = 8; int mapY = 30; int mapW = width - 16; int mapH = height - 38;
            double dx = mx - (mapX + mapW / 2.0);
            double dy = my - (mapY + mapH / 2.0);
            centerX -= dx * (1.0 / scale - 1.0 / oldZoom);
            centerZ -= dy * (1.0 / scale - 1.0 / oldZoom);
            // 缩放手动触发纹理重建
            terrainTextureId = null;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            dragging = true;
            lastMouseX = mx;
            lastMouseY = my;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            centerX -= (mx - lastMouseX) / zoom;
            centerZ -= (my - lastMouseY) / zoom;
            lastMouseX = mx;
            lastMouseY = my;
            // 拖拽触发纹理重建
            terrainTextureId = null;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ════════════════════════════════════════════════════════
    //  工厂方法
    // ════════════════════════════════════════════════════════

    public static void open() {
        Minecraft.getInstance().setScreen(new CityMapScreen());
    }
}