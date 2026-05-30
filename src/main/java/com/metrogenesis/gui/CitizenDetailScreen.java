package com.metrogenesis.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 甯傛皯璇︽儏鐣岄潰 鈥?鍦ㄥ競鏀垮巺鐐瑰嚮甯傛皯鍚嶅瓧鏃舵墦寮€
 * <p>
 * 鏄剧ず甯傛皯璇︾粏淇℃伅 + 鍙垎閰嶈亴涓氭寜閽? */
public class CitizenDetailScreen extends Screen {

    private static final int GUI_W = 220;
    private static final int GUI_H = 180;
    private static final int BG_COLOR = 0xD0081018;
    private static final int BORDER_COLOR = 0xFF4A5568;

    private final String citizenName;
    private final String citizenJob;
    private final int citizenSatisfaction;
    private final int containerId;
    private final int citizenId; // 绋冲畾鐨勫競姘?ID锛岀敤浜庢寜閽紪鐮?
    private final Runnable onClose;

    private static final String[][] JOBS = {
        {"unemployed", "寰呬笟"},
        {"farmer", "鍐滃か"},
        {"builder", "寤洪€犲笀"},
        {"merchant", "鍟嗕汉"},
    };

    public CitizenDetailScreen(String name, String job, int satisfaction,
                                int containerId, int citizenId, Runnable onClose) {
        super(Component.literal("甯傛皯璇︽儏"));
        this.citizenName = name;
        this.citizenJob = job;
        this.citizenSatisfaction = satisfaction;
        this.containerId = containerId;
        this.citizenId = citizenId;
        this.onClose = onClose;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int cx = (width - GUI_W) / 2;
        int cy = (height - GUI_H) / 2;

        g.fill(cx, cy, cx + GUI_W, cy + GUI_H, BG_COLOR);
        g.fill(cx, cy, cx + GUI_W, cy + 1, BORDER_COLOR);
        g.fill(cx, cy + GUI_H - 1, cx + GUI_W, cy + GUI_H, BORDER_COLOR);
        g.fill(cx, cy, cx + 1, cy + GUI_H, BORDER_COLOR);
        g.fill(cx + GUI_W - 1, cy, cx + GUI_W, cy + GUI_H, BORDER_COLOR);

        int x = cx + 16;
        int y = cy + 16;

        g.drawString(font, "搂l" + citizenName, x, y, 0xFFFFFF);
        y += 14;
        g.fill(x, y, x + GUI_W - 32, y + 1, 0x334A5568);
        y += 10;

        String jobCN = switch (citizenJob) {
            case "farmer" -> "鍐滃か";
            case "builder" -> "寤洪€犲笀";
            case "merchant" -> "鍟嗕汉";
            default -> "寰呬笟";
        };
        g.drawString(font, "搂7褰撳墠鑱屼笟锛毬" + jobCN, x, y, 0xFFFFFF);
        y += 14;

        String satColor = citizenSatisfaction >= 80 ? "搂a" :
                          citizenSatisfaction >= 50 ? "搂e" : "搂c";
        g.drawString(font, "搂7婊℃剰搴︼細搂f" + satColor + citizenSatisfaction + "%", x, y, 0xFFFFFF);
        y += 20;

        g.drawString(font, "搂6搂l鍒嗛厤鑱屼笟", x, y, 0xFFFFFF);
        y += 14;

        for (int i = 0; i < JOBS.length; i++) {
            String id = JOBS[i][0];
            String name = JOBS[i][1];
            boolean isCurrent = id.equals(citizenJob);
            int bx = x + (i % 2) * 95;
            int by = y + (i / 2) * 22;
            int color = isCurrent ? 0xFF555555 : 0xFF2E7D32;
            int hover = 0xFF4CAF50;
            boolean hov = mx >= bx && mx < bx + 90 && my >= by && my < by + 20;
            g.fill(bx, by, bx + 90, by + 20, hov ? hover : color);
            g.drawString(font, (isCurrent ? "搂7" : "搂f") + name, bx + 8, by + 5, isCurrent ? 0x888888 : 0xFFFFFF);
        }

        g.drawString(font, "搂7鎸?搂eESC 搂7杩斿洖", x, cy + GUI_H - 28, 0x888888);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int cx = (width - GUI_W) / 2;
        int cy = (height - GUI_H) / 2;
        int x = cx + 16;
        int y = cy + 16 + 14 + 10 + 14 + 14 + 20 + 14;

        for (int i = 0; i < JOBS.length; i++) {
            String id = JOBS[i][0];
            int bx = x + (i % 2) * 95;
            int by = y + (i / 2) * 22;
            if (mx >= bx && mx < bx + 90 && my >= by && my < by + 20) {
                if (!id.equals(citizenJob) && minecraft != null && minecraft.gameMode != null) {
                    int encodedId = 1000 + citizenId * 10 + i;
                    minecraft.gameMode.handleInventoryButtonClick(containerId, encodedId);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        if (onClose != null) onClose.run();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
