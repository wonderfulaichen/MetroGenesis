package com.metrogenesis.gui.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * MetroGenesis GUI 鍩虹被 鈥?缁熶竴 UI 鏍峰紡锛屽噺灏戦噸澶嶄唬鐮? * <p>
 * 鎻愪緵锛? * <ul>
 *   <li>{@link #drawBtn(GuiGraphics, int, int, int, int, int, int, int, int, String)} 鈥?鎸夐挳缁樺埗</li>
 *   <li>{@link #inBounds(double, double, int, int)} 鈥?鐐瑰嚮鍖哄煙妫€娴?/li>
 *   <li>{@link #renderBorder(GuiGraphics, int, int, int, int)} 鈥?杈规缁樺埗</li>
 * </ul>
 *
 * @param <T> 鑿滃崟绫诲瀷
 */
public abstract class MetroGenesisScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T>
{
    protected static final int DEFAULT_BTN_W = 160;
    protected static final int DEFAULT_BTN_H = 22;
    protected static final int BG_COLOR = 0xD0081018;
    protected static final int BORDER_COLOR = 0xFF4A5568;

    public MetroGenesisScreen(T menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
    }

    // 鈹€鈹€ 閫氱敤杈规缁樺埗 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 缁樺埗缁熶竴椋庢牸鐨?GUI 杈规锛堝洓杈癸級
     */
    protected void renderBorder(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + 1, BORDER_COLOR);
        g.fill(x, y + h - 1, x + w, y + h, BORDER_COLOR);
        g.fill(x, y, x + 1, y + h, BORDER_COLOR);
        g.fill(x + w - 1, y, x + w, y + h, BORDER_COLOR);
    }

    /**
     * 缁樺埗鑳屾櫙灞?+ 杈规
     */
    protected void renderBg(GuiGraphics g, int x, int y, int w, int h)
    {
        renderBackground(g);
        g.fill(x, y, x + w, y + h, BG_COLOR);
        renderBorder(g, x, y, w, h);
    }

    // 鈹€鈹€ 閫氱敤鎸夐挳 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 缁樺埗涓€涓?MetroGenesis 椋庢牸鎸夐挳
     *
     * @param g    GUI 鐢诲竷
     * @param bx   鎸夐挳 X
     * @param by   鎸夐挳 Y
     * @param mx   榧犳爣 X
     * @param my   榧犳爣 Y
     * @param bg   姝ｅ父鑳屾櫙鑹?     * @param bgH  鎮仠鑳屾櫙鑹?     * @param bdr  姝ｅ父杈规鑹?     * @param bdrH 鎮仠杈规鑹?     * @param text 鎸夐挳鏂囧瓧
     */
    protected void drawBtn(GuiGraphics g, int bx, int by, int mx, int my,
                            int bg, int bgH, int bdr, int bdrH, String text)
    {
        drawBtn(g, bx, by, mx, my, bg, bgH, bdr, bdrH, text, DEFAULT_BTN_W, DEFAULT_BTN_H);
    }

    /**
     * 鑷畾涔夊昂瀵哥殑鎸夐挳
     */
    protected void drawBtn(GuiGraphics g, int bx, int by, int mx, int my,
                            int bg, int bgH, int bdr, int bdrH, String text,
                            int w, int h)
    {
        boolean hov = inBounds(mx, my, bx, by, w, h);
        g.fill(bx - 1, by - 1, bx + w + 1, by + h + 1, hov ? bdrH : bdr);
        g.fill(bx, by, bx + w, by + h, hov ? bgH : bg);
        int tw = font.width(text);
        g.drawString(font, text, bx + (w - tw) / 2, by + (h - 8) / 2, 0xFFFFFF, false);
    }

    // 鈹€鈹€ 鐐瑰嚮鍖哄煙妫€娴?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    /**
     * 妫€鏌ラ紶鏍囨槸鍚﹀湪鎸夐挳鍖哄煙鍐?     */
    protected boolean inBounds(double mx, double my, int bx, int by)
    {
        return inBounds(mx, my, bx, by, DEFAULT_BTN_W, DEFAULT_BTN_H);
    }

    protected boolean inBounds(double mx, double my, int bx, int by, int w, int h)
    {
        double rx = mx - leftPos;
        double ry = my - topPos;
        return rx >= bx && rx < bx + w && ry >= by && ry < by + h;
    }

    // 鈹€鈹€ 鐘舵€佷俊鎭?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    protected String statusMsg = "";
    protected int statusTimer = 0;

    /**
     * 鏄剧ず涓€鏉′复鏃剁姸鎬佷俊鎭?     */
    protected void showStatus(String msg, int ticks)
    {
        this.statusMsg = msg;
        this.statusTimer = ticks;
    }

    /**
     * 缁樺埗鐘舵€佷俊鎭紙鏀惧湪 renderLabels 涓皟鐢級
     */
    protected void drawStatus(GuiGraphics g, int x, int y)
    {
        if (statusTimer > 0) {
            g.drawString(font, statusMsg, x, y, 0xFFAAAAAA, false);
            statusTimer--;
        }
    }
}
