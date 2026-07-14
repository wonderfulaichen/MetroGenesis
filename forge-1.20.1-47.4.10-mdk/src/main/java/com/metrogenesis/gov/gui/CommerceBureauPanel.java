package com.metrogenesis.gov.gui;

import com.metrogenesis.gov.DepartmentManager;
import com.metrogenesis.gov.data.CommerceBureauData;
import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 商务部面板 — 价格干预开关 + 物品价格上下限列表。
 */
public class CommerceBureauPanel implements DepartmentPanel {

    /** 点击状态 */
    private boolean togglingIntervention = false;

    private CommerceBureauData getData() {
        DepartmentManager mgr = DepartmentManager.getInstance();
        if (mgr == null) return null;
        var dept = mgr.getDept("commerce");
        return dept != null ? (CommerceBureauData) dept.getData() : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen) {
        CommerceBureauData data = getData();
        if (data == null) {
            screen.drawGlassText(g, "§7部门数据不可用", x + 8, y + 8, screen.TEXT_SECONDARY, alpha);
            return;
        }

        // 价格干预开关
        int gy = y + 8;
        boolean enabled = data.isPriceInterventionEnabled();
        // 勾选框
        int cbX = x + 8;
        int cbY = gy;
        int cbSize = 12;
        screen.fillGlassButton(g, cbX, cbY, cbSize, cbSize,
            enabled ? 0xFFC89B3C : screen.BTN_GLASS, alpha);
        if (enabled) {
            screen.drawGlassText(g, "✓", cbX + 2, cbY, 0xFF3D2B1F, alpha);
        }
        togglingIntervention = inBounds(mx, my, cbX, cbY, cbSize, cbSize);

        screen.drawGlassText(g, "启用价格干预", cbX + cbSize + 6, cbY + 1, screen.TEXT_PRIMARY, alpha);
        gy += 22;

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy, w - 8, 1, 0x33303028, alpha);
        gy += 8;

        if (!enabled) {
            screen.drawGlassText(g, "§7价格干预已禁用 — 当前价格为自由市场定价", x + 8, gy, screen.TEXT_SECONDARY, alpha);
            return;
        }

        // 已设置的物品列表
        var ceilings = data.getPriceCeilings();
        var floors = data.getPriceFloors();

        if (ceilings.isEmpty() && floors.isEmpty()) {
            screen.drawGlassText(g, "§7暂无价格限制设置", x + 8, gy, screen.TEXT_SECONDARY, alpha);
            gy += 14;
        } else {
            screen.drawGlassText(g, "§e物品价格限制", x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
            gy += 14;
            for (var entry : ceilings.entrySet()) {
                String item = entry.getKey();
                double ceiling = entry.getValue();
                screen.drawGlassText(g, item, x + 12, gy, screen.TEXT_PRIMARY, alpha);
                screen.drawGlassText(g, "上限: " + String.format("%.1f", ceiling), x + w - 90, gy, screen.TEXT_SOFT_GOLD, alpha);
                gy += 12;
            }
            for (var entry : floors.entrySet()) {
                String item = entry.getKey();
                double floor = entry.getValue();
                screen.drawGlassText(g, item, x + 12, gy, screen.TEXT_PRIMARY, alpha);
                screen.drawGlassText(g, "下限: " + String.format("%.1f", floor), x + w - 90, gy, screen.TEXT_SOFT_GOLD, alpha);
                gy += 12;
            }
        }

        // 底部提示
        screen.drawGlassText(g, "§7物品价格限制通过 /gov 命令或后续 GUI 配置", x + 8, y + h - 14, screen.TEXT_SECONDARY, alpha);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int x, int y) {
        if (button == 0 && togglingIntervention) {
            CommerceBureauData data = getData();
            if (data != null) {
                data.setPriceInterventionEnabled(!data.isPriceInterventionEnabled());
            }
            togglingIntervention = false;
            return true;
        }
        return false;
    }

    private boolean inBounds(double mx, double my, int bx, int by, int w, int h) {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }
}
