package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.network.BlueprintPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.storage.rendering.RenderingCache;
import com.metrogenesis.structurize.storage.rendering.types.BlueprintPreviewData;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.locale.Language;

/**
 * 图鉴放置窗口 — 关闭 MayorBookScreen 后弹出，玩家可自由走动查看 3D Ghost Preview。
 * <p>
 * 键盘快捷键：
 * <ul>
 *   <li>W/A/S/D — 微调位置（北/西/南/东）</li>
 *   <li>Q/E — 旋转（逆时针/顺时针 90°）</li>
 *   <li>R/F — 高度（升/降 1 格）</li>
 *   <li>Enter — 确认放置</li>
 *   <li>ESC — 取消</li>
 * </ul>
 */
public class CatalogPlacementScreen extends Screen
{
    private static final String PREVIEW_KEY = "metrogenesis_placement";

    private final BuildingCatalogEntry entry;
    @Nullable
    private final Blueprint blueprint;

    // 放置参数
    private BlockPos basePos;
    private int nudgeX = 0, nudgeZ = 0, nudgeY = 0;
    private int rotIdx = 0;

    private static final RotationMirror[] ROTATIONS = RotationMirror.NOT_MIRRORED;
    private static final String[] ROT_NAMES = {"0\u00B0", "90\u00B0", "180\u00B0", "270\u00B0"};

    protected CatalogPlacementScreen(final BuildingCatalogEntry entry, @Nullable final Blueprint blueprint, final BlockPos basePos)
    {
        super(Component.translatable("gui.metrogenesis.placement.title"));
        this.entry = entry;
        this.blueprint = blueprint;
        this.basePos = basePos;
    }

    @Override
    protected void init()
    {
        super.init();
        updateGhostPreview();
    }

    @Override
    public void onClose()
    {
        removeGhostPreview();
        super.onClose();
    }

    // ════════════════════════════════════════════════════════
    //  键盘输入
    // ════════════════════════════════════════════════════════

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        switch (keyCode)
        {
            case 87 -> { nudgeZ--; updateGhostPreview(); }   // W → 北
            case 83 -> { nudgeZ++; updateGhostPreview(); }   // S → 南
            case 65 -> { nudgeX--; updateGhostPreview(); }   // A → 西
            case 68 -> { nudgeX++; updateGhostPreview(); }   // D → 东
            case 82 -> { nudgeY++; updateGhostPreview(); }   // R → 上
            case 70 -> { nudgeY--; updateGhostPreview(); }   // F → 下
            case 81 -> { rotIdx = (rotIdx - 1 + 4) % 4; updateGhostPreview(); } // Q → 逆时针
            case 69 -> { rotIdx = (rotIdx + 1) % 4; updateGhostPreview(); }     // E → 顺时针
            case 257, 335 -> { doPlace(); return true; }     // Enter → 确认
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
        return true;
    }

    // ════════════════════════════════════════════════════════
    //  鼠标输入
    // ════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        // 确认按钮
        if (button == 0 && isConfirmButton(mx, my)) { doPlace(); return true; }
        // 取消按钮
        if (button == 0 && isCancelButton(mx, my)) { onClose(); return true; }
        // 重置按钮
        if (button == 0 && isResetButton(mx, my)) { resetAdjustments(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    // ════════════════════════════════════════════════════════
    //  渲染
    // ════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick)
    {
        // 不渲染背景 — 玩家可以看到世界和 Ghost Preview
        renderBackground(g);

        final int cw = this.width;
        final int panelW = 220;
        final int panelH = 160;
        final int px = cw - panelW - 10;
        final int py = 10;

        // 半透明背景
        g.fill(px, py, px + panelW, py + panelH, MGStyles.C_BG_PANEL);

        // 标题
        final String title = entry.name();
        g.drawString(font, Component.literal(title), px + 10, py + 8, MGStyles.C_TEXT_BRASS);

        // 风格包
        g.drawString(font, Component.literal("\u00a77" + entry.packName()), px + 10, py + 22, 0xAAAAAA);

        // 位置
        final BlockPos pos = getPos();
        final String posStr = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        g.drawString(font, Component.literal("\u00a7f" + Language.getInstance().getOrDefault("gui.metrogenesis.placement.pos") + ": \u00a7a" + posStr), px + 10, py + 40, 0xFFFFFF);

        // 旋转
        g.drawString(font, Component.literal("\u00a7f" + Language.getInstance().getOrDefault("gui.metrogenesis.placement.rot") + ": \u00a7a" + ROT_NAMES[rotIdx]), px + 10, py + 54, 0xFFFFFF);

        // 微调值
        if (nudgeX != 0 || nudgeZ != 0 || nudgeY != 0)
        {
            final String nudgeStr = "X" + (nudgeX >= 0 ? "+" : "") + nudgeX
                + " Z" + (nudgeZ >= 0 ? "+" : "") + nudgeZ
                + " Y" + (nudgeY >= 0 ? "+" : "") + nudgeY;
            g.drawString(font, Component.literal("\u00a77" + nudgeStr), px + 10, py + 68, 0xAAAAAA);
        }

        // 分隔线
        g.fill(px + 8, py + 78, px + panelW - 8, py + 79, 0xFF333333);

        // 快捷键提示
        g.drawString(font, Component.literal(Language.getInstance().getOrDefault("gui.metrogenesis.placement.hint1")), px + 10, py + 86, 0xAAAAAA);
        g.drawString(font, Component.literal(Language.getInstance().getOrDefault("gui.metrogenesis.placement.hint2")), px + 10, py + 100, 0xAAAAAA);

        // 按钮区域
        final int btnY = py + 118;
        final int btnH = 22;

        // 确认按钮（强调橙）
        final boolean confirmHover = isConfirmButton(mx, my);
        fillButton(g, px + 10, btnY, 80, btnH, confirmHover ? MGStyles.C_ACCENT_LT : MGStyles.C_ACCENT);
        g.drawString(font, Component.literal(Language.getInstance().getOrDefault("gui.metrogenesis.placement.confirm")), px + 16, btnY + 6, 0xFFFFFF);

        // 取消按钮（砖红）
        final boolean cancelHover = isCancelButton(mx, my);
        fillButton(g, px + 100, btnY, 80, btnH, cancelHover ? 0xBF5C40 : MGStyles.C_DANGER);
        g.drawString(font, Component.literal(Language.getInstance().getOrDefault("gui.metrogenesis.placement.cancel")), px + 112, btnY + 6, 0xFFFFFF);

        // 重置按钮（深灰）
        final boolean resetHover = isResetButton(mx, my);
        fillButton(g, px + 10, btnY + 28, 60, 20, resetHover ? MGStyles.C_BG_HOVER : MGStyles.C_BG_CARD);
        g.drawString(font, Component.literal(Language.getInstance().getOrDefault("gui.metrogenesis.placement.reset")), px + 20, btnY + 32, MGStyles.C_TEXT_2ND);
    }

    private void fillButton(GuiGraphics g, int x, int y, int w, int h, int color)
    {
        g.fill(x, y, x + w, y + h, color);
        g.fill(x, y, x + w, y + 1, 0x55FFFFFF); // 上高光
        g.fill(x, y + h - 1, x + w, y + h, 0x66000000); // 下阴影
    }

    private boolean isConfirmButton(double mx, double my)
    {
        return mx >= width - 220 + 10 && mx <= width - 220 + 90
            && my >= 128 && my <= 150;
    }

    private boolean isCancelButton(double mx, double my)
    {
        return mx >= width - 220 + 100 && mx <= width - 220 + 180
            && my >= 128 && my <= 150;
    }

    private boolean isResetButton(double mx, double my)
    {
        return mx >= width - 220 + 10 && mx <= width - 220 + 70
            && my >= 156 && my <= 176;
    }

    // ════════════════════════════════════════════════════════
    //  Ghost Preview 管理
    // ════════════════════════════════════════════════════════

    private BlockPos getPos()
    {
        return basePos.offset(nudgeX, nudgeY, nudgeZ);
    }

    private RotationMirror getRotation()
    {
        return ROTATIONS[rotIdx];
    }

    private void updateGhostPreview()
    {
        if (blueprint == null) return;

        final BlueprintPreviewData data = new BlueprintPreviewData(false);
        data.setPos(getPos());
        data.setBlueprint(blueprint);
        data.setRotationMirror(getRotation());
        data.setOverridePreviewTransparency(0.25f);
        data.setRenderBlocksNice(true);
        RenderingCache.queue(PREVIEW_KEY, data);
    }

    private void removeGhostPreview()
    {
        RenderingCache.removeBlueprint(PREVIEW_KEY);
    }

    private void resetAdjustments()
    {
        nudgeX = 0;
        nudgeZ = 0;
        nudgeY = 0;
        rotIdx = 0;
        updateGhostPreview();
    }

    // ════════════════════════════════════════════════════════
    //  放置执行
    // ════════════════════════════════════════════════════════

    private void doPlace()
    {
        final BlockPos finalPos = getPos();
        final RotationMirror rot = getRotation();

        MetroGenesis.LOGGER.info("[CatalogPlacement] Placing '{}' at {} rot={}", entry.name(), finalPos, rot);

        NetworkHandler.CHANNEL.sendToServer(
            new BlueprintPlacementMessage(
                entry.name(),
                entry.packName(),
                entry.resourcePath(),
                finalPos,
                rot
            )
        );
        Minecraft.getInstance().setScreen(null);
    }

    // ════════════════════════════════════════════════════════
    //  静态入口
    // ════════════════════════════════════════════════════════

    /**
     * 从图鉴面板打开放置窗口。
     * 关闭 MayorBookScreen 后弹出独立窗口，玩家可自由走动。
     *
     * @param entry   选中的蓝图条目
     * @param worldPos 初始放置位置（世界坐标，null 则由玩家前方自动计算）
     */
    public static void open(final BuildingCatalogEntry entry, @Nullable final BlockPos worldPos)
    {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null) return;

        // 计算初始位置
        final BlockPos pos;
        if (worldPos != null)
        {
            pos = worldPos;
        }
        else
        {
            // 玩家前方 15 格
            final Vec3 eye = player.getEyePosition();
            final Vec3 look = player.getLookAngle();
            final Vec3 target = eye.add(look.x * 15, 0, look.z * 15);
            pos = BlockPos.containing(target);
        }

        // 加载蓝图
        Blueprint bp = null;
        try
        {
            bp = com.metrogenesis.structurize.storage.StructurePacks.getBlueprint(
                entry.packName(), entry.resourcePath() + ".blueprint", true);
        }
        catch (Exception e)
        {
            MetroGenesis.LOGGER.warn("[CatalogPlacement] Failed to load blueprint: {}", e.getMessage());
        }

        mc.setScreen(new CatalogPlacementScreen(entry, bp, pos));
    }
}
