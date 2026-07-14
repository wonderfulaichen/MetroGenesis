package com.metrogenesis.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * 市民详情界面 — 在市政厅点击市民名字时打开
 * <p>
 * 显示市民详细信息 + 可分配职业按钮。
 */
public class CitizenDetailScreen extends Screen {

    private static final int GUI_W = 220;
    private static final int GUI_H = 180;
    private static final int BG_COLOR = MGStyles.C_BG;
    private static final int BORDER_COLOR = MGStyles.C_BRASS;

    private final String citizenName;
    private final String citizenJob;
    private final int citizenSatisfaction;
    private final int containerId;
    private final int citizenId; // 稳定的市民 ID，用于按钮编码
    private final Runnable onClose;

    private static final String[][] JOBS = {
        {"unemployed", "gui.metrogenesis.citizen.job.unemployed"},
        {"farmer", "gui.metrogenesis.citizen.job.farmer"},
        {"builder", "gui.metrogenesis.citizen.job.builder"},
        {"merchant", "gui.metrogenesis.citizen.job.merchant"},
    };

    public CitizenDetailScreen(String name, String job, int satisfaction,
                                int containerId, int citizenId, Runnable onClose) {
        super(Component.translatable("gui.metrogenesis.citizen.title"));
        this.citizenName = name;
        this.citizenJob = job;
        this.citizenSatisfaction = satisfaction;
        this.containerId = containerId;
        this.citizenId = citizenId;
        this.onClose = onClose;
    }

    private static String lang(String key) {
        return Language.getInstance().getOrDefault(key);
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

        g.drawString(font, "§l" + citizenName, x, y, 0xFFFFFF);
        y += 14;
        g.fill(x, y, x + GUI_W - 32, y + 1, 0xFF333333);
        y += 10;

        String jobCN = lang(switch (citizenJob) {
            case "farmer" -> "gui.metrogenesis.citizen.job.farmer";
            case "builder" -> "gui.metrogenesis.citizen.job.builder";
            case "merchant" -> "gui.metrogenesis.citizen.job.merchant";
            default -> "gui.metrogenesis.citizen.job.unemployed";
        });
        g.drawString(font, lang("gui.metrogenesis.citizen.currentjob") + jobCN, x, y, 0xFFFFFF);
        y += 14;

        String satColor = citizenSatisfaction >= 80 ? "§a" :
                          citizenSatisfaction >= 50 ? "§e" : "§c";
        g.drawString(font, lang("gui.metrogenesis.citizen.satisfaction") + satColor + citizenSatisfaction + "%", x, y, 0xFFFFFF);
        y += 20;

        g.drawString(font, lang("gui.metrogenesis.citizen.assignjob"), x, y, 0xFFFFFF);
        y += 14;

        for (int i = 0; i < JOBS.length; i++) {
            String id = JOBS[i][0];
            String name = lang(JOBS[i][1]);
            boolean isCurrent = id.equals(citizenJob);
            int bx = x + (i % 2) * 95;
            int by = y + (i / 2) * 22;
            int color = isCurrent ? MGStyles.C_ACCENT : MGStyles.C_BG_CARD;
            int hover = MGStyles.C_BG_HOVER;
            boolean hov = mx >= bx && mx < bx + 90 && my >= by && my < by + 20;
            g.fill(bx, by, bx + 90, by + 20, hov ? hover : color);
            g.drawString(font, (isCurrent ? "§7" : "§f") + name, bx + 8, by + 5, isCurrent ? 0xFFFFFF : MGStyles.C_TEXT);
        }

        g.drawString(font, lang("gui.metrogenesis.citizen.escback"), x, cy + GUI_H - 28, 0x888888);
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
