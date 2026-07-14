package com.metrogenesis.gui;

import com.metrogenesis.gui.base.MetroGenesisScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 甯傛斂鍘?GUI 鈥?灞曠ず鍥藉簱/浜哄彛/甯傛皯鍒楄〃 + 鎷涘嫙鎸夐挳
 */
public class TownHallScreen extends MetroGenesisScreen<TownHallMenu> {

    private static final int GUI_W = 260;
    private static final int GUI_H = 220;

    private static final int BTN_X = 14;
    private static final int BTN0_Y = 150;
    private static final int BTN1_Y = 170;
    private static final int BTN2_Y = 190;

    public TownHallScreen(TownHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 1000; // 闅愯棌榛樿鏍囬
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        super.renderBg(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos, topPos + 16, leftPos + imageWidth, topPos + 17, 0x8033497E);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // 鈹€鈹€ 椤堕儴鏍囬 鈹€鈹€
        g.drawString(font, "搂6搂l鍩庨偊绠＄悊", 12, 6, 0xFFFFFF);

        // 鈹€鈹€ 缁熻淇℃伅 鈹€鈹€
        int funds = menu.getFunds();
        int pop = menu.getPopulation();
        int maxPop = menu.getMaxPopulation();
        g.drawString(font, "搂7鍥藉簱锛毬" + funds + " 搂7C-Value", 16, 24, 0xFFFFFF);
        g.drawString(font, "搂7甯傛皯锛毬" + pop + " 搂7/ 搂b" + maxPop, 16, 38, 0xFFFFFF);

        // 鈹€鈹€ 鍒嗛殧绾?鈹€鈹€
        g.fill(14, 54, GUI_W - 14, 55, 0x334A5568);

        // 鈹€鈹€ 甯傛皯鍒楄〃 鈹€鈹€
        int y = 58;
        g.drawString(font, "搂6搂l甯傛皯鍚嶅唽", 16, y, 0xFFFFFF);
        y += 14;

        int citizenCount = Math.min(menu.citizenNames.length, 8);
        if (citizenCount == 0) {
            g.drawString(font, "搂7鏆傛棤甯傛皯", 20, y, 0x888888);
        } else {
            for (int i = 0; i < citizenCount; i++) {
                String name = menu.citizenNames[i];
                String job = menu.citizenJobs[i];
                int sat = menu.citizenSatisfactions[i];

                // 婊℃剰搴﹂鑹?
                String satColor = sat >= 80 ? "§a" : (sat >= 50 ? "§e" : "§c");
                // 鑱屼笟涓枃
                String jobCN = switch (job) {
                    case "farmer" -> "鍐滃か";
                    case "builder" -> "寤洪€犲笀";
                    case "merchant" -> "鍟嗕汉";
                    default -> job;
                };

                g.drawString(font, "搂7- 搂f" + name + " 搂8(" + jobCN + ")", 20, y, 0xFFFFFF);

                // 婊℃剰搴﹁繘搴︽潯
                int barX = 160;
                int barW = 60;
                int barH = 5;
                int barY = y + 2;
                g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
                int fill = barW * sat / 100;
                int color = sat >= 80 ? 0xFF4CAF50 : (sat >= 50 ? 0xFFFFC107 : 0xFFF44336);
                g.fill(barX, barY, barX + fill, barY + barH, color);
                g.drawString(font, satColor + sat + "%", barX + barW + 4, barY - 1, 0xFFFFFF);

                y += 12;
            }
        }

        // 鈹€鈹€ 鎷涘嫙鎸夐挳 鈹€鈹€
        drawBtn(g, BTN_X, BTN0_Y, mx, my,
                0xFF1B5E20, 0xFF2E7D32, 0xFF388E3C, 0xFF4CAF50,
                "搂l鎷涘嫙鍐滃か 搂7100 C");

        drawBtn(g, BTN_X, BTN1_Y, mx, my,
                0xFF7F3300, 0xFFA04000, 0xFFBF360C, 0xFFE64A19,
                "搂l鎷涘嫙寤洪€犲笀 搂7100 C");

        drawBtn(g, BTN_X, BTN2_Y, mx, my,
                0xFF1A237E, 0xFF283593, 0xFF3949AB, 0xFF5C6BC0,
                "搂l鎷涘嫙鍟嗕汉 搂7100 C");

        drawStatus(g, BTN_X, BTN2_Y + DEFAULT_BTN_H + 4);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            // 鐐瑰嚮甯傛皯鍚?鈫?鎵撳紑璇︽儏
            int listY = 58 + 14; // 鏍囬鍋忕Щ
            int clickedIdx = -1;
            int citizenCount = Math.min(menu.citizenNames.length, 8);
            for (int i = 0; i < citizenCount; i++) {
                int cy = listY + i * 12;
                if (my >= topPos + cy && my < topPos + cy + 11
                        && mx >= leftPos + 20 && mx < leftPos + 220) {
                    clickedIdx = i;
                    break;
                }
            }
            if (clickedIdx >= 0) {
                Minecraft.getInstance().setScreen(new CitizenDetailScreen(
                        menu.citizenNames[clickedIdx],
                        menu.citizenJobs[clickedIdx],
                        menu.citizenSatisfactions[clickedIdx],
                        menu.containerId,
                        menu.citizenIds[clickedIdx],
                        () -> Minecraft.getInstance().setScreen(this)));
                return true;
            }

            // 鎷涘嫙鎸夐挳
            if (inBounds(mx, my, BTN_X, BTN0_Y)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                showStatus("搂e鍐滃か鎷涘嫙涓?..", 60);
                return true;
            }
            if (inBounds(mx, my, BTN_X, BTN1_Y)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                showStatus("搂e寤洪€犲笀鎷涘嫙涓?..", 60);
                return true;
            }
            if (inBounds(mx, my, BTN_X, BTN2_Y)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                showStatus("搂e鍟嗕汉鎷涘嫙涓?..", 60);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
