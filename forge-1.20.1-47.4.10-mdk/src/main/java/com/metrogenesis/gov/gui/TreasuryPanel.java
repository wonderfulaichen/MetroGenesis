package com.metrogenesis.gov.gui;

import com.metrogenesis.gov.DepartmentManager;
import com.metrogenesis.gov.data.TreasuryData;
import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 财政部面板 — 税率滑块 + 国库概览 + 日收支。
 */
public class TreasuryPanel implements DepartmentPanel {

    /** 是否正在拖拽某个滑块 */
    private int draggingSlider = -1; // -1=无, 0~5 对应 6 个参数
    private static final String[] LABELS = {
        "个人所得税", "企业所得税", "消费税", "地价税",
        "基准利率", "最低工资系数"
    };
    private static final String[] SUFFIXES = {
        "%", "%", "%", "%", "%", "x"
    };
    private static final double[] SCALES = {
        100.0, 100.0, 100.0, 100.0, 100.0, 10.0
    };

    /** 获取当前 TreasuryData（可能为 null） */
    private TreasuryData getData() {
        DepartmentManager mgr = DepartmentManager.getInstance();
        if (mgr == null) return null;
        var dept = mgr.getDept("treasury");
        return dept != null ? (TreasuryData) dept.getData() : null;
    }

    /// 辅助：从屏幕坐标获取百分比的 double 值
    private double readValue(int index) {
        TreasuryData d = getData();
        if (d == null) return 0;
        return switch (index) {
            case 0 -> d.getIncomeTaxRate();
            case 1 -> d.getCorporateTaxRate();
            case 2 -> d.getConsumptionTaxRate();
            case 3 -> d.getLandTaxRate();
            case 4 -> d.getBaseInterestRate();
            case 5 -> d.getMinWageCoefficient();
            default -> 0;
        };
    }

    private void writeValue(int index, double val) {
        TreasuryData d = getData();
        if (d == null) return;
        switch (index) {
            case 0 -> d.setIncomeTaxRate(val);
            case 1 -> d.setCorporateTaxRate(val);
            case 2 -> d.setConsumptionTaxRate(val);
            case 3 -> d.setLandTaxRate(val);
            case 4 -> d.setBaseInterestRate(val);
            case 5 -> d.setMinWageCoefficient(val);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen) {
        TreasuryData data = getData();
        if (data == null) {
            screen.drawGlassText(g, "§7部门数据不可用", x + 8, y + 8, screen.TEXT_SECONDARY, alpha);
            return;
        }

        // 国库概览（U5 修复：读已同步的真实国库，不再写死 "---"）
        int gy = y;
        final var city = com.metrogenesis.network.SyncCityDataMessage.getCachedData();
        String balStr = city != null ? (city.funds + " C") : "—";
        screen.drawGlassText(g, "§e国库余额: " + balStr, x + 8, gy, screen.TEXT_SOFT_GOLD, alpha);
        gy += 14;
        screen.drawGlassText(g, "§7今日收入: " + data.getDailyIncome() + " C", x + 8, gy, screen.TEXT_GREEN, alpha);
        screen.drawGlassText(g, "§7今日支出: " + data.getDailyExpense() + " C", x + 128, gy, screen.TEXT_PRIMARY, alpha);
        gy += 20;

        // 分隔线
        screen.fillGlassPanel(g, x + 4, gy - 2, w - 8, 1, 0x33303028, alpha);
        gy += 6;

        // 6 个滑块
        int sliderStartY = gy;
        for (int i = 0; i < LABELS.length; i++) {
            int sy = sliderStartY + i * 22;
            screen.drawGlassText(g, LABELS[i], x + 8, sy, screen.TEXT_PRIMARY, alpha);

            // 值文本
            double rawVal = readValue(i);
            int percentVal = (int) Math.round(rawVal * SCALES[i]);
            String valStr = percentVal + SUFFIXES[i];
            screen.drawGlassText(g, valStr, x + w - 50, sy, screen.TEXT_SOFT_GOLD, alpha);

            // 滑块条
            int sliderX = x + 8;
            int sliderY = sy + 12;
            int sliderW = w - 16;
            int sliderH = 6;
            int filledW = (int) (sliderW * rawVal / (i == 5 ? 3.0 : 1.0));

            // 背景轨道
            screen.fillRoundRect(g, sliderX, sliderY, sliderW, sliderH, screen.withAlpha(0x332A2A40, alpha));
            // 填充部分
            if (filledW > 0) {
                screen.fillRoundRect(g, sliderX, sliderY, Math.min(filledW, sliderW), sliderH, screen.withAlpha(0xFFC89B3C, alpha));
            }
            // 滑块手柄
            int handleX = sliderX + Math.min(filledW, sliderW - 4);
            screen.fillRoundRect(g, handleX - 3, sliderY - 2, 8, sliderH + 4, screen.withAlpha(0xCCF5E6C8, alpha));
        }

        // 底部提示
        screen.drawGlassText(g, "§7税率变更在下次每日结算生效", x + 8, sliderStartY + LABELS.length * 22 + 6, screen.TEXT_SECONDARY, alpha);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button, int x, int y) {
        return false; // 当前为只读展示，滑块操作暂不实现
    }
}
