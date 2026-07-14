package com.metrogenesis.gov.gui;

import com.metrogenesis.gov.DepartmentManager;
import com.metrogenesis.gov.data.PopulationBureauData;
import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 人口局面板 — 人口概览 + 移民政策 + 生育补贴。
 */
public class PopulationBureauPanel implements DepartmentPanel {

    private boolean togglingImmigration = false;
    private boolean togglingSkillFilter = false;
    /** 吸引力滑块拖拽状态 */
    private boolean draggingAttract = false;

    private PopulationBureauData getData() {
        DepartmentManager mgr = DepartmentManager.getInstance();
        if (mgr == null) return null;
        var dept = mgr.getDept("population");
        return dept != null ? (PopulationBureauData) dept.getData() : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen) {
        PopulationBureauData data = getData();
        if (data == null) {
            screen.drawGlassText(g, "§7部门数据不可用", x + 8, y + 8, screen.TEXT_SECONDARY, alpha);
            return;
        }

        int gy = y + 8;

        // 人口概览
        screen.drawGlassText(g, "§e人口概览", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;
        screen.drawGlassText(g, "总人口:", x + 12, gy, screen.TEXT_SECONDARY, alpha);
        screen.drawGlassText(g, "-", x + 80, gy, screen.TEXT_GREEN, alpha);
        gy += 16;

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy, w - 8, 1, 0x33303028, alpha);
        gy += 8;

        // 移民开关
        boolean immigEnabled = data.isImmigrationEnabled();
        int cbX = x + 8;
        int cbSize = 12;
        screen.fillGlassButton(g, cbX, gy, cbSize, cbSize,
            immigEnabled ? 0xFFC89B3C : screen.BTN_GLASS, alpha);
        if (immigEnabled) {
            screen.drawGlassText(g, "✓", cbX + 2, gy, 0xFF3D2B1F, alpha);
        }
        togglingImmigration = inBounds(mx, my, cbX, gy, cbSize, cbSize);
        screen.drawGlassText(g, "开放移民", cbX + cbSize + 6, gy + 1, screen.TEXT_PRIMARY, alpha);
        gy += 18;

        // 吸引力滑块
        screen.drawGlassText(g, "城市吸引力:", x + 12, gy, screen.TEXT_PRIMARY, alpha);
        int attractVal = (int) Math.round(data.getImmigrationAttractiveness() * 100);
        screen.drawGlassText(g, attractVal + "%", x + w - 50, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 12;
        int sliderX = x + 12;
        int sliderW = w - 24;
        int sliderH = 6;
        screen.fillRoundRect(g, sliderX, gy, sliderW, sliderH, screen.withAlpha(0x332A2A40, alpha));
        double ratio = data.getImmigrationAttractiveness() / 3.0;
        int filledW = (int) (sliderW * Math.min(ratio, 1.0));
        if (filledW > 0) {
            screen.fillRoundRect(g, sliderX, gy, Math.min(filledW, sliderW), sliderH, screen.withAlpha(0xFFC89B3C, alpha));
        }
        int handleX = sliderX + Math.min(filledW, sliderW - 4);
        screen.fillRoundRect(g, handleX - 3, gy - 2, 8, sliderH + 4, screen.withAlpha(0xCCF5E6C8, alpha));
        gy += 18;

        // 生育奖金
        screen.drawGlassText(g, "生育奖金:", x + 12, gy, screen.TEXT_PRIMARY, alpha);
        screen.drawGlassText(g, data.getBirthBonus() + " C", x + w - 50, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;

        // 技能筛选
        boolean skillEnabled = data.isSkillFilterEnabled();
        screen.fillGlassButton(g, cbX, gy, cbSize, cbSize,
            skillEnabled ? 0xFFC89B3C : screen.BTN_GLASS, alpha);
        if (skillEnabled) {
            screen.drawGlassText(g, "✓", cbX + 2, gy, 0xFF3D2B1F, alpha);
        }
        togglingSkillFilter = inBounds(mx, my, cbX, gy, cbSize, cbSize);
        screen.drawGlassText(g, "技能筛选（仅高技能移民）", cbX + cbSize + 6, gy + 1, screen.TEXT_PRIMARY, alpha);
        gy += 18;

        // 提示
        screen.drawGlassText(g, "§7人口系统深度集成将在 Phase 5 完成", x + 8, y + h - 14, screen.TEXT_SECONDARY, alpha);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int x, int y) {
        if (button != 0) return false;
        PopulationBureauData data = getData();
        if (data == null) return false;

        if (togglingImmigration) {
            data.setImmigrationEnabled(!data.isImmigrationEnabled());
            togglingImmigration = false;
            return true;
        }
        if (togglingSkillFilter) {
            data.setSkillFilterEnabled(!data.isSkillFilterEnabled());
            togglingSkillFilter = false;
            return true;
        }
        return false;
    }

    private boolean inBounds(double mx, double my, int bx, int by, int w, int h) {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }
}
