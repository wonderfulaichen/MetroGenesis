package com.metrogenesis.gui;

import com.metrogenesis.gui.base.MetroGenesisScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 鏂藉伐鏋?GUI 鈥?缁ф壙 MetroGenesisScreen 鍩虹被
 * <p>
 * 閫夋嫨寤虹瓚绫诲瀷 鎴?鏌ョ湅鏂藉伐杩涘害
 */
public class ConstructionMarkerScreen extends MetroGenesisScreen<ConstructionMarkerMenu> {

    private static final int W = 200, H = 150;

    // 閫夋嫨鎸夐挳
    private static final int SEL_X = 20;
    private static final int SEL_FARM_Y = 50;
    private static final int SEL_HALL_Y = 80;

    // 杩涘害鏉?
    private static final int BAR_X = 20, BAR_Y = 60;
    private static final int BAR_W = 160, BAR_H = 14;

    public ConstructionMarkerScreen(ConstructionMarkerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        super.renderBg(g, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "§6§l施工管理", 55, 8, 0xFFFFFF, false);

        int progress = menu.getProgress();

        if (progress > 0 && progress < 100) {
            drawProgressBar(g, progress);
            g.drawString(font, "§7建造中... §e" + progress + "%", 20, 90, 0xFFFFFF, false);
            g.drawString(font, "§7等待建造师施工...", 20, 110, 0xAAAAAA, false);
        } else if (progress >= 100) {
            g.drawString(font, "§a§l✔ 建造完成！", 55, 40, 0xFFFFFF, false);
        } else {
            g.drawString(font, "§7选择要建造的设施", 20, 30, 0xFFFFFF, false);
            drawBtn(g, SEL_X, SEL_FARM_Y, mx, my, 0xFF1B5E20, 0xFF2E7D32, 0xFF388E3C, 0xFF4CAF50,
                    "§l农场 🍵", DEFAULT_BTN_W, 24);
            drawBtn(g, SEL_X, SEL_HALL_Y, mx, my, 0xFF1A237E, 0xFF283593, 0xFF3949AB, 0xFF5C6BC0,
                    "§l市政厅 🏛", DEFAULT_BTN_W, 24);
        }
    }

    private void drawProgressBar(GuiGraphics g, int progress) {
        g.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0xFF333333);
        int filled = (BAR_W * progress) / 100;
        if (filled > 0) {
            int color = progress < 50 ? 0xFFE65100 : (progress < 80 ? 0xFFFFD600 : 0xFF00C853);
            g.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_H, color);
        }
        // 杈规
        g.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + 1, 0xFF555555);
        g.fill(BAR_X, BAR_Y + BAR_H - 1, BAR_X + BAR_W, BAR_Y + BAR_H, 0xFF555555);
        // 鐧惧垎姣旀枃瀛?
        String pct = progress + "%";
        int tw = font.width(pct);
        g.drawString(font, "§l" + pct, BAR_X + (BAR_W - tw) / 2, BAR_Y + 3, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (menu.getProgress() == 0) {
                if (inBounds(mx, my, SEL_X, SEL_FARM_Y, DEFAULT_BTN_W, 24)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    showStatus("§e开始建造农场..", 40);
                    minecraft.player.closeContainer();
                    return true;
                }
                if (inBounds(mx, my, SEL_X, SEL_HALL_Y, DEFAULT_BTN_W, 24)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    showStatus("§e开始建造市政厅...", 40);
                    minecraft.player.closeContainer();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
