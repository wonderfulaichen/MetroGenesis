package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.network.CaptureBlueprintPacket;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.util.RotationMirror;
import com.metrogenesis.util.BlueprintUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import com.mojang.blaze3d.vertex.*;

/**
 * 扫描结果弹窗 — 显示 3D 预览 + 名称输入 + 旋转/镜像 + 保存/丢弃。
 * <p>
 * v2: 新增旋转/镜像按钮，状态存储在 {@link #rotationMirror} 中，
 * 按下后更新预览并记录变换，保存时传递给 {@link CaptureBlueprintPacket}。
 */
public class BlueprintCaptureResultScreen extends Screen
{
    // 暂不使用自定义纹理（文件不存在时粉红占位），改用纯代码绘制背景
    // private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("metrogenesis",
    //     "textures/gui/blueprint_result.png");

    private final BlockPos start;
    private final BlockPos end;
    private final int sizeX, sizeY, sizeZ;
    private final long blockCount;

    private EditBox nameField;
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 248;
    private static final int GUI_HEIGHT = 220; // 增高以容纳旋转按钮行和名称输入框

    /** 当前旋转/镜像状态，保存时传给服务端应用 */
    private RotationMirror rotationMirror = RotationMirror.NONE;

    /** 用于预览的蓝图副本（仅客户端预览，不保存） */
    private Blueprint previewBlueprint;

    /** init() 时扫描的原始副本，refreshPreviewBlueprint() 从这里重建，不重走 getBlockState() */
    private Blueprint rawBlueprint;

    public BlueprintCaptureResultScreen(BlockPos start, BlockPos end)
    {
        super(Component.literal("扫描结果"));
        this.start = start;
        this.end = end;
        this.sizeX = Math.abs(end.getX() - start.getX()) + 1;
        this.sizeY = Math.abs(end.getY() - start.getY()) + 1;
        this.sizeZ = Math.abs(end.getZ() - start.getZ()) + 1;
        this.blockCount = (long) sizeX * sizeY * sizeZ;
    }

    @Override
    protected void init()
    {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        // 客户端扫描：用 Minecraft.getInstance().level 扫描 start~end 区域
        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel != null)
        {
            this.rawBlueprint = BlueprintUtil.scanRegion(clientLevel, start, end, "preview");
            this.previewBlueprint = rawBlueprint; // 初始状态无变换
        }
        else
        {
            this.rawBlueprint = null;
            this.previewBlueprint = null; // 降级为线框
        }

        // 名称输入框（移到按钮行上方，留 10px 间隙）
        this.nameField = new EditBox(this.font, guiLeft + 10, guiTop + 142, 120, 18,
            Component.literal("蓝图名称"));
        this.nameField.setValue("建筑 #" + System.currentTimeMillis() % 10000);
        this.addWidget(this.nameField);

        int btnY = guiTop + 172;

        // ── 旋转/镜像按钮行（18px宽，6px间距，左侧起始避免与保存/丢弃重叠） ──
        int rotBtnX = guiLeft + 10;
        int BW = 18;   // 按钮宽
        int BS = 6;    // 按钮间距

        // 顺时针旋转 90°
        this.addRenderableWidget(Button.builder(
            Component.literal("↻"), b -> applyTransformation(RotationMirror.R90))
            .bounds(rotBtnX, btnY, BW, 20)
            .build());

        // 逆时针旋转 90°
        this.addRenderableWidget(Button.builder(
            Component.literal("↺"), b -> applyTransformation(RotationMirror.R270))
            .bounds(rotBtnX + BW + BS, btnY, BW, 20)
            .build());

        // 水平镜像（LEFT_RIGHT = R180 + FRONT_BACK，几何等价于纯左右翻转）
        this.addRenderableWidget(Button.builder(
            Component.literal("⇔"), b -> applyTransformation(RotationMirror.MIR_R180))
            .bounds(rotBtnX + (BW + BS) * 2, btnY, BW, 20)
            .build());

        // 垂直镜像（FRONT_BACK = 前后翻转，等价于垂直方向镜像）
        this.addRenderableWidget(Button.builder(
            Component.literal("⇕"), b -> applyTransformation(RotationMirror.MIR_NONE))
            .bounds(rotBtnX + (BW + BS) * 3, btnY, BW, 20)
            .build());

        // 保存按钮（与镜像按钮保持充足间距，避免重叠）
        this.addRenderableWidget(Button.builder(
            Component.literal("§a✓"), this::onSave)
            .bounds(guiLeft + GUI_WIDTH - 48, btnY, 18, 20)
            .build());

        // 丢弃按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("§7✗"), this::onDiscard)
            .bounds(guiLeft + GUI_WIDTH - 26, btnY, 18, 20)
            .build());
    }

    /**
     * 应用用户选择的变换并刷新预览显示。
     */
    private void applyTransformation(RotationMirror transform)
    {
        this.rotationMirror = this.rotationMirror.add(transform);
        // 重建预览蓝图
        refreshPreviewBlueprint();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);

        // ══ 绘制背景面板（纯代码，无纹理依赖） ═══════════
        // 深色半透明背景
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xE01A1A2E);
        // 边框线
        graphics.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF4488FF);

        // ══ 标题 ═════════════════════════════════════
        graphics.drawString(this.font,
            Component.literal("§6✦ 扫描结果"), guiLeft + 10, guiTop + 8, 0xFFFFFF);

        // ══ 尺寸信息（显示变换后的尺寸） ═══════════════
        int dispX = isRotated() ? sizeZ : sizeX;
        int dispZ = isRotated() ? sizeX : sizeZ;
        String info = String.format("§7尺寸: §f%d × %d × %d  §7方块: §f%d",
            dispX, sizeY, dispZ, blockCount);
        graphics.drawString(this.font, Component.literal(info),
            guiLeft + 10, guiTop + 24, 0xFFFFFF);

        // ══ 入口方向 + 旋转变换状态 ════════════════════
        String mirrorStr = rotationMirror.isMirrored() ? " §7镜像:§b是" : " §7镜像:§8否";
        graphics.drawString(this.font,
            Component.literal("§7入口方向: §a" + getEntryDirection()
                + "  §7旋转:§e" + getRotationDisplay() + mirrorStr),
            guiLeft + 10, guiTop + 38, 0xFFFFFF);

        // ══ 3D 预览区域 ═══════════════════════════════
        int previewX = guiLeft + 10;
        int previewY = guiTop + 50;
        int previewW = GUI_WIDTH - 20;
        int previewH = 80;

        // 预览背景
        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF1A1A2E);

        // 渲染简化的 3D 方块预览（使用变换后尺寸）
        render3DPreview(graphics, previewX, previewY, previewW, previewH);

        // ══ 操作提示 ═════════════════════════════════
        String hint = "§7↻旋转  ⇔镜像  ✓保存  ✗丢弃  ESC取消";
        graphics.drawString(this.font, Component.literal(hint),
            guiLeft + 10, guiTop + 132, 0x666666);

        // ══ 名称输入框 ═══════════════════════════════
        this.nameField.render(graphics, mouseX, mouseY, partialTick);

        // ══ 底部范围 ═════════════════════════════════
        String rangeStr = String.format("§7范围: %s ~ %s", start.toShortString(), end.toShortString());
        graphics.drawString(this.font, Component.literal(rangeStr),
            guiLeft + 10, guiTop + GUI_HEIGHT - 10, 0x444444);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 预览尺寸是否因旋转发生了 XZ 交换 */
    private boolean isRotated()
    {
        return rotationMirror.rotation() == net.minecraft.world.level.block.Rotation.CLOCKWISE_90
            || rotationMirror.rotation() == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
    }

    /** 返回旋转角度文字（如 "90°"、"180°"、"270°"） */
    private String getRotationDisplay()
    {
        return switch (rotationMirror.rotation())
        {
            case CLOCKWISE_90         -> "90°";
            case CLOCKWISE_180        -> "180°";
            case COUNTERCLOCKWISE_90  -> "270°";
            default                   -> "0°";
        };
    }

    /**
     * 3D 预览 — 如果 previewBlueprint 有数据则渲染体素方块，
     * 否则降级为纯线框模式。
     */
    private void render3DPreview(GuiGraphics graphics, int x, int y, int w, int h)
    {
        int dispX = isRotated() ? sizeZ : sizeX;
        int dispZ = isRotated() ? sizeX : sizeZ;

        int centerX = x + w / 2;
        int centerY = y + h / 2;

        float scale = Math.min(
            (float) w / (dispX * 2 + dispZ),
            (float) h / (sizeY * 3 + dispX)
        );
        scale = Math.min(scale, 2.0f);
        scale = Math.max(scale, 0.5f);

        int blockSize = Math.max(1, (int) (scale * 0.8f));

        // 入口面信息（两种模式共用）
        String entryDir = getEntryDirection();
        boolean mirrored = rotationMirror.isMirrored();
        int entryHighlightColor = mirrored ? 0x88FF88AA : 0x88FFCC00;
        int wireColor = isRotated() ? 0x88CC88FF : 0x884488FF;

        if (previewBlueprint != null)
        {
            // ═══ 体素渲染模式 ═══
            short[][][] structure = previewBlueprint.getStructure();
            var palette = previewBlueprint.getPalette();
            short bpSizeX = previewBlueprint.getSizeX();
            short bpSizeY = previewBlueprint.getSizeY();
            short bpSizeZ = previewBlueprint.getSizeZ();
            Level clientLevel = Minecraft.getInstance().level;

            // 等轴测投影参数
            // 先确定包围盒范围用于居中
            float isoScaleX = blockSize * 0.5f;
            float isoScaleY = blockSize * 0.25f;
            float isoScaleH = blockSize * 1.0f;

            // 计算包围盒用于居中
            float minIsoX = Float.MAX_VALUE, maxIsoX = -Float.MAX_VALUE;
            float minIsoY = Float.MAX_VALUE, maxIsoY = -Float.MAX_VALUE;
            for (short by = 0; by < bpSizeY; by++)
            {
                for (short bz = 0; bz < bpSizeZ; bz++)
                {
                    for (short bx = 0; bx < bpSizeX; bx++)
                    {
                        if (structure[by][bz][bx] == 0) continue;
                        float ix = (bx - bz) * isoScaleX;
                        float iy = (bx + bz) * isoScaleY - by * isoScaleH;
                        minIsoX = Math.min(minIsoX, ix);
                        maxIsoX = Math.max(maxIsoX, ix);
                        minIsoY = Math.min(minIsoY, iy);
                        maxIsoY = Math.max(maxIsoY, iy);
                    }
                }
            }

            // 无非空气方块时降级
            if (minIsoX == Float.MAX_VALUE)
            {
                renderWireframeFallback(graphics, x, y, w, h, dispX, dispZ,
                    centerX, centerY, scale, blockSize, wireColor, entryHighlightColor, entryDir, mirrored);
                return;
            }

            float centerIsoX = (minIsoX + maxIsoX) / 2;
            float centerIsoY = (minIsoY + maxIsoY) / 2;
            float viewOffsetX = centerX - centerIsoX;
            float viewOffsetY = centerY - centerIsoY;

            // 绘制入口面高亮（底层）
            int halfW = dispX * blockSize;
            int halfD = dispZ * blockSize;
            int cx = centerX - halfW;
            int cy = centerY - halfD / 2;
            int cw = halfW * 2;
            int ch = halfD;
            int heightScale = Math.min(sizeY, 8) * blockSize;

            int entryX1 = cx, entryZ1 = cy, entryX2 = cx + cw, entryZ2 = cy + ch;
            if (mirrored && entryDir.contains("东")) entryX1 = cx + cw - blockSize;
            else if (entryDir.contains("东")) entryX2 = cx + blockSize;
            else if (mirrored && entryDir.contains("西")) entryX2 = cx + blockSize;
            else if (entryDir.contains("西")) entryX1 = cx + cw - blockSize;
            else if (mirrored && entryDir.contains("南")) entryZ1 = cy + ch - blockSize;
            else if (entryDir.contains("南")) entryZ2 = cy + blockSize;
            else if (mirrored && entryDir.contains("北")) entryZ2 = cy + blockSize;
            else entryZ1 = cy + ch - blockSize;
            graphics.fill(entryX1, entryZ1, entryX2, entryZ2, entryHighlightColor);

            // 遍历体素，从后到前绘制（等轴测遮挡顺序）
            for (short by = (short) (bpSizeY - 1); by >= 0; by--)
            {
                for (short bz = (short) (bpSizeZ - 1); bz >= 0; bz--)
                {
                    for (short bx = 0; bx < bpSizeX; bx++)
                    {
                        short val = structure[by][bz][bx];
                        if (val == 0) continue; // 空气跳过

                        // 等轴测投影坐标
                        float isoX = (bx - bz) * isoScaleX + viewOffsetX;
                        float isoY = (bx + bz) * isoScaleY - by * isoScaleH + viewOffsetY;
                        int drawX = (int) isoX;
                        int drawY = (int) isoY;

                        // 取方块颜色
                        int color = 0xFF888888; // fallback 灰色
                        if (val < palette.size())
                        {
                            BlockState state = palette.get(val);
                            try
                            {
                                MapColor mapColor = state.getMapColor(clientLevel, BlockPos.ZERO);
                                if (mapColor != null)
                                {
                                    color = 0xFF000000 | (mapColor.col & 0xFFFFFF);
                                }
                            }
                            catch (Exception e)
                            {
                                // 降级到灰色
                            }
                        }

                        // 绘制小方块
                        graphics.fill(drawX, drawY, drawX + blockSize, drawY + blockSize, color);
                        // 可选：加边框增加可读性
                        if (blockSize >= 3)
                        {
                            graphics.fill(drawX, drawY, drawX + blockSize, drawY + 1, 0x44000000);
                            graphics.fill(drawX, drawY, drawX + 1, drawY + blockSize, 0x44000000);
                        }
                    }
                }
            }

            // 叠加线框（半透明）
            graphics.renderOutline(cx, cy - heightScale, cw, ch, wireColor);
            graphics.fill(cx, cy - heightScale, cx + 1, cy + ch, wireColor);
            graphics.fill(cx + cw, cy - heightScale, cx + cw + 1, cy + ch, wireColor);

            // 尺寸标注
            String dimStr = bpSizeX + "×" + bpSizeY + "×" + bpSizeZ;
            graphics.drawString(this.font, Component.literal("§7" + dimStr),
                cx + cw / 2 - font.width(dimStr) / 2, cy - heightScale - 10, 0xFFFFFF);

            // 镜像标注
            if (mirrored)
            {
                graphics.drawString(this.font, Component.literal("§d[M]"),
                    cx + cw / 2 - 8, cy + ch + 2, 0xFFFFFF);
            }
        }
        else
        {
            // ═══ 降级：纯线框模式 ═══
            renderWireframeFallback(graphics, x, y, w, h, dispX, dispZ,
                centerX, centerY, scale, blockSize, wireColor, entryHighlightColor, entryDir, mirrored);
        }
    }

    /**
     * 降级线框渲染（previewBlueprint 为 null 时使用）。
     */
    private void renderWireframeFallback(GuiGraphics graphics, int x, int y, int w, int h,
        int dispX, int dispZ, int centerX, int centerY, float scale, int blockSize,
        int wireColor, int entryHighlightColor, String entryDir, boolean mirrored)
    {
        int halfW = dispX * blockSize;
        int halfD = dispZ * blockSize;

        int cx = centerX - halfW;
        int cy = centerY - halfD / 2;
        int cw = halfW * 2;
        int ch = halfD;
        int heightScale = Math.min(sizeY, 8) * blockSize;

        // 入口面高亮
        int entryX1 = cx, entryZ1 = cy, entryX2 = cx + cw, entryZ2 = cy + ch;
        if (mirrored && entryDir.contains("东")) entryX1 = cx + cw - blockSize;
        else if (entryDir.contains("东")) entryX2 = cx + blockSize;
        else if (mirrored && entryDir.contains("西")) entryX2 = cx + blockSize;
        else if (entryDir.contains("西")) entryX1 = cx + cw - blockSize;
        else if (mirrored && entryDir.contains("南")) entryZ1 = cy + ch - blockSize;
        else if (entryDir.contains("南")) entryZ2 = cy + blockSize;
        else if (mirrored && entryDir.contains("北")) entryZ2 = cy + blockSize;
        else entryZ1 = cy + ch - blockSize;
        graphics.fill(entryX1, entryZ1, entryX2, entryZ2, entryHighlightColor);

        // 线框
        graphics.renderOutline(cx, cy, cw, ch, wireColor);
        graphics.renderOutline(cx, cy - heightScale, cw, ch, wireColor);
        graphics.fill(cx, cy - heightScale, cx + 1, cy + ch, wireColor);
        graphics.fill(cx + cw, cy - heightScale, cx + cw + 1, cy + ch, wireColor);
        graphics.fill(cx, cy, cx + cw, cy + 1, wireColor);

        // 尺寸标注
        String dimStr = dispX + "×" + sizeY + "×" + dispZ;
        graphics.drawString(this.font, Component.literal("§7" + dimStr),
            cx + cw / 2 - font.width(dimStr) / 2, cy - heightScale - 10, 0xFFFFFF);

        // 镜像标注
        if (mirrored)
        {
            graphics.drawString(this.font, Component.literal("§d[M]"),
                cx + cw / 2 - 8, cy + ch + 2, 0xFFFFFF);
        }
    }

    /**
     * 根据拖拽方向计算入口方向。
     * 入口在拖拽方向的最外侧竖直面。
     */
    private String getEntryDirection()
    {
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();

        if (Math.abs(dx) >= Math.abs(dz))
        {
            return dx >= 0 ? "东 (East)" : "西 (West)";
        }
        else
        {
            return dz >= 0 ? "南 (South)" : "北 (North)";
        }
    }

    /**
     * 刷新客户端预览蓝图 — 按区域大小决定策略。
     * 小区域 (<50³) 重新扫描保证精度；大区域原地变换避免冻帧。
     */
    private void refreshPreviewBlueprint()
    {
        if (rawBlueprint == null) return;

        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        // 小区域重新扫描（<50³ 瞬间完成）
        if (sizeX <= 50 && sizeY <= 50 && sizeZ <= 50)
        {
            Blueprint fresh = BlueprintUtil.scanRegion(clientLevel, start, end, "preview");
            if (rotationMirror != RotationMirror.NONE)
            {
                fresh.setRotationMirror(rotationMirror);
            }
            this.previewBlueprint = fresh;
        }
        else
        {
            // 大区域：跳过重新扫描，只变换已有数据（接受一次性的原地修改）
            if (rotationMirror != RotationMirror.NONE)
            {
                this.previewBlueprint.setRotationMirror(rotationMirror);
            }
        }

        MetroGenesis.LOGGER.debug("[Preview] refreshed with {} ({}×{}×{})",
            rotationMirror, sizeX, sizeY, sizeZ);
    }

    /** 保存按钮 — 发送带旋转状态的扫描请求到服务端 */
    private void onSave(Button btn)
    {
        String name = nameField.getValue();
        if (name == null || name.trim().isEmpty())
        {
            name = "建筑 #" + System.currentTimeMillis() % 10000;
        }

        if (Minecraft.getInstance().getConnection() != null)
        {
            NetworkHandler.CHANNEL.sendToServer(
                new CaptureBlueprintPacket(start, end, name.trim(), rotationMirror));
        }

        this.onClose();
    }

    /** 丢弃按钮 */
    private void onDiscard(Button btn)
    {
        this.onClose();
    }
}
