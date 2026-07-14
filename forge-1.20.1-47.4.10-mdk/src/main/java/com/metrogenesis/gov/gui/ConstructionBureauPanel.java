package com.metrogenesis.gov.gui;

import com.metrogenesis.gov.DepartmentManager;
import com.metrogenesis.gov.data.ConstructionBureauData;
import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 建设局面板 — 施工数概览 + 最大施工数滑块 + 自动填充开关。
 */
public class ConstructionBureauPanel implements DepartmentPanel {

    private boolean togglingAutoFill = false;

    private ConstructionBureauData getData() {
        DepartmentManager mgr = DepartmentManager.getInstance();
        if (mgr == null) return null;
        var dept = mgr.getDept("construction");
        return dept != null ? (ConstructionBureauData) dept.getData() : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen) {
        ConstructionBureauData data = getData();
        if (data == null) {
            screen.drawGlassText(g, "§7部门数据不可用", x + 8, y + 8, screen.TEXT_SECONDARY, alpha);
            return;
        }

        int gy = y + 8;

        // 标题
        screen.drawGlassText(g, "§e施工管理", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 16;

        // 最大施工数
        screen.drawGlassText(g, "最大同时施工数:", x + 12, gy, screen.TEXT_PRIMARY, alpha);
        screen.drawGlassText(g, String.valueOf(data.getMaxConstructionSites()), x + w - 50, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;

        // 滑块 (1-10)
        int sliderX = x + 12;
        int sliderW = w - 24;
        int sliderH = 6;
        screen.fillRoundRect(g, sliderX, gy, sliderW, sliderH, screen.withAlpha(0x332A2A40, alpha));
        double ratio = (data.getMaxConstructionSites() - 1) / 9.0;
        int filledW = (int) (sliderW * ratio);
        if (filledW > 0) {
            screen.fillRoundRect(g, sliderX, gy, Math.min(filledW, sliderW), sliderH, screen.withAlpha(0xFFC89B3C, alpha));
        }
        int handleX = sliderX + Math.min(filledW, sliderW - 4);
        screen.fillRoundRect(g, handleX - 3, gy - 2, 8, sliderH + 4, screen.withAlpha(0xCCF5E6C8, alpha));
        gy += 20;

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy, w - 8, 1, 0x33303028, alpha);
        gy += 8;

        // 自动填充开关（灰色不可用）
        int cbSize = 12;
        int cbX = x + 8;
        boolean autoFill = data.isAutoFillEnabled();
        screen.fillGlassButton(g, cbX, gy, cbSize, cbSize,
            autoFill ? 0x55C89B3C : 0x222A2A40, alpha);
        if (autoFill) {
            screen.drawGlassText(g, "✓", cbX + 2, gy, 0x553D2B1F, alpha);
        }
        togglingAutoFill = inBounds(mx, my, cbX, gy, cbSize, cbSize);
        screen.drawGlassText(g, "§7自动填充", cbX + cbSize + 6, gy + 1, screen.TEXT_SECONDARY, alpha);
        gy += 14;
        screen.drawGlassText(g, "§8（需 Phase 4 研究解锁）", x + 24, gy, screen.TEXT_SECONDARY, alpha);
        gy += 20;

        // 当前概览
        screen.drawGlassText(g, "§8当前施工队列由 ConstructionManager 管理", x + 8, y + h - 14, screen.TEXT_SECONDARY, alpha);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int x, int y) {
        if (button == 0 && togglingAutoFill) {
            ConstructionBureauData data = getData();
            if (data != null) {
                data.setAutoFillEnabled(!data.isAutoFillEnabled());
            }
            togglingAutoFill = false;
            return true;
        }
        return false;
    }

    private boolean inBounds(double mx, double my, int bx, int by, int w, int h) {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }
}
