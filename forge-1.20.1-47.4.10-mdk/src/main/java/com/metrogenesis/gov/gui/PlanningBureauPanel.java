package com.metrogenesis.gov.gui;

import com.metrogenesis.gov.DepartmentManager;
import com.metrogenesis.gov.data.PlanningBureauData;
import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 规划局面板 — 研究进度条 + 已解锁科技 + 现行政策。
 */
public class PlanningBureauPanel implements DepartmentPanel {

    private PlanningBureauData getData() {
        DepartmentManager mgr = DepartmentManager.getInstance();
        if (mgr == null) return null;
        var dept = mgr.getDept("planning");
        return dept != null ? (PlanningBureauData) dept.getData() : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen) {
        PlanningBureauData data = getData();
        if (data == null) {
            screen.drawGlassText(g, "§7部门数据不可用", x + 8, y + 8, screen.TEXT_SECONDARY, alpha);
            return;
        }

        int gy = y + 8;

        // 当前研究
        screen.drawGlassText(g, "§e当前研究", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;

        String current = data.getCurrentResearch();
        if (current == null || current.isEmpty()) {
            screen.drawGlassText(g, "§7未进行研究项目", x + 12, gy, screen.TEXT_SECONDARY, alpha);
            gy += 16;
        } else {
            screen.drawGlassText(g, current, x + 12, gy, screen.TEXT_PRIMARY, alpha);
            gy += 14;

            // 进度条
            int barX = x + 12;
            int barW = w - 24;
            int barH = 10;
            // 背景轨道
            screen.fillRoundRect(g, barX, gy, barW, barH, screen.withAlpha(0x332A2A40, alpha));
            // 填充
            int progress = data.getResearchProgress();
            int filledW = barW * progress / 100;
            if (filledW > 0) {
                screen.fillRoundRect(g, barX, gy, Math.min(filledW, barW), barH, screen.withAlpha(0xCC88B880, alpha));
            }
            // 进度文本
            String pctText = progress + "%";
            screen.drawGlassText(g, pctText, barX + barW / 2 - 8, gy + 1, screen.TEXT_PRIMARY, alpha);
            gy += 18;
        }

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy, w - 8, 1, 0x33303028, alpha);
        gy += 8;

        // 已解锁科技
        screen.drawGlassText(g, "§e已解锁科技", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;

        var unlocked = data.getUnlockedTechs();
        if (unlocked.isEmpty()) {
            screen.drawGlassText(g, "§7暂无", x + 12, gy, screen.TEXT_SECONDARY, alpha);
            gy += 14;
        } else {
            for (String tech : unlocked) {
                screen.drawGlassText(g, "✓ " + tech, x + 12, gy, screen.TEXT_GREEN, alpha);
                gy += 12;
            }
        }
        gy += 6;

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy, w - 8, 1, 0x33303028, alpha);
        gy += 8;

        // 现行政策
        screen.drawGlassText(g, "§e现行政策", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;

        var policies = data.getActivePolicies();
        if (policies.isEmpty()) {
            screen.drawGlassText(g, "§7暂无生效政策", x + 12, gy, screen.TEXT_SECONDARY, alpha);
        } else {
            for (String policy : policies) {
                screen.drawGlassText(g, "▸ " + policy, x + 12, gy, screen.TEXT_PRIMARY, alpha);
                gy += 12;
            }
        }

        // 底部提示
        screen.drawGlassText(g, "§7研究每 3 天自动推进 5%（有研究方向时）", x + 8, y + h - 14, screen.TEXT_SECONDARY, alpha);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int x, int y) {
        return false;
    }
}
