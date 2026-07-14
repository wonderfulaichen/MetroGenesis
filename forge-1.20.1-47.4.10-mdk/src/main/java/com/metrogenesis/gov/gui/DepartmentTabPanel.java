package com.metrogenesis.gov.gui;

import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 部门面板容器 — 包含顶部 5 个 Tab 按钮 + 当前选中部门的面板内容。
 * <p>
 * 渲染尺寸居中，约 440×380px。
 * Tab 按钮横排 5 个，选中黄铜高亮。
 * <p>
 * 生命周期由 {@link com.metrogenesis.gui.MayorBookScreen} 驱动。
 */
public class DepartmentTabPanel {

    private static final String[] DEPT_IDS = {
        "treasury", "commerce", "population", "construction", "planning"
    };

    private static final Component[] TAB_LABELS = {
        Component.literal("财政部"), Component.literal("商务部"),
        Component.literal("人口局"), Component.literal("建设局"),
        Component.literal("规划局")
    };

    /** 5 个子面板实例 */
    private final DepartmentPanel[] panels = new DepartmentPanel[]{
        new TreasuryPanel(),
        new CommerceBureauPanel(),
        new PopulationBureauPanel(),
        new ConstructionBureauPanel(),
        new PlanningBureauPanel()
    };

    private int selectedIndex = 0;
    private boolean visible = false;

    /** 面板宽度比例（相对于屏幕宽度） */
    private static final float PANEL_W_RATIO = 0.55f;
    /** 面板高度比例（相对于屏幕高度） */
    private static final float PANEL_H_RATIO = 0.65f;
    /** 最小面板宽度 */
    private static final int MIN_PANEL_W = 320;
    /** 最小面板高度 */
    private static final int MIN_PANEL_H = 260;

    // ══ 生命周期 ═════════════════════════════════════

    public void toggle() {
        visible = !visible;
        selectedIndex = 0;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }

    // ══ 渲染 ══════════════════════════════════════════

    /**
     * 主渲染入口 — 由 MayorBookScreen.render() 调用。
     *
     * @param g           GuiGraphics
     * @param mx          鼠标 X
     * @param my          鼠标 Y
     * @param partialTick 部分 tick
     * @param alpha       整体透明度
     * @param screen      MayorBookScreen 引用
     */
    public void render(GuiGraphics g, int mx, int my, float partialTick, int alpha, MayorBookScreen screen) {
        if (!visible) return;

        int centerX = screen.width / 2;
        int centerY = screen.height / 2;
        final int panelW = Math.max(MIN_PANEL_W, (int) (screen.width * PANEL_W_RATIO));
        final int panelH = Math.max(MIN_PANEL_H, (int) (screen.height * PANEL_H_RATIO));
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2;

        // 1. 外部磨砂玻璃背景 — 圆角暗色面板
        screen.fillGlassPanel(g, panelX, panelY, panelW, panelH, screen.GLASS_BG, alpha);

        // 2. 内部暖白面板（略缩）
        screen.fillGlassPanel(g, panelX + 4, panelY + 32, panelW - 8, panelH - 36, screen.GLASS_PANEL, alpha);

        // 3. Tab 按钮（横排）
        final int tabW = Math.min(80, (panelW - 16) / 5 - 4);
        final int tabH = 24;
        int tabY = panelY + 4;
        for (int i = 0; i < 5; i++) {
            int tabX = panelX + 8 + i * (tabW + 4);

            boolean hover = mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + tabH;
            int bgColor;
            int textColor;
            if (i == selectedIndex) {
                bgColor = 0xFFC89B3C;       // 选中黄铜
                textColor = 0xFF3D2B1F;      // 深棕文字
            } else if (hover) {
                bgColor = screen.BTN_GLASS_HOVER;
                textColor = screen.TEXT_PRIMARY;
            } else {
                bgColor = screen.BTN_GLASS;
                textColor = screen.TEXT_SECONDARY;
            }
            screen.fillGlassButton(g, tabX, tabY, tabW, tabH, bgColor, alpha);
            String label = TAB_LABELS[i].getString();
            int labelX = tabX + (tabW - screen.getFont().width(label)) / 2;
            screen.drawGlassText(g, label, labelX, tabY + 7, textColor, alpha);
        }

        // 4. 当前部门面板内容
        int contentX = panelX + 8;
        int contentY = panelY + 36;
        int contentW = panelW - 16;
        int contentH = panelH - 44;
        if (selectedIndex >= 0 && selectedIndex < panels.length) {
            panels[selectedIndex].render(g, mx, my, contentX, contentY, contentW, contentH, alpha, screen);
        }

        // 5. 底部关闭提示
        int hintY = panelY + panelH - 12;
        screen.drawGlassText(g, "[Esc] 关闭", panelX + panelW - 70, hintY, screen.TEXT_SECONDARY, alpha);
    }

    // ══ 输入处理 ══════════════════════════════════════

    /**
     * 处理鼠标点击。
     *
     * @param mx     鼠标 X
     * @param my     鼠标 Y
     * @param button 鼠标按钮
     * @param screen MayorBookScreen 引用（用于获取宽高）
     * @return true 表示点击被处理
     */
    public boolean mouseClicked(double mx, double my, int button, MayorBookScreen screen) {
        if (!visible) return false;

        int centerX = screen.width / 2;
        int centerY = screen.height / 2;
        final int panelW = Math.max(MIN_PANEL_W, (int) (screen.width * PANEL_W_RATIO));
        final int panelH = Math.max(MIN_PANEL_H, (int) (screen.height * PANEL_H_RATIO));
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2;

        // 检查 Tab 按钮点击
        final int tabW = Math.min(80, (panelW - 16) / 5 - 4);
        int tabY = panelY + 4;
        for (int i = 0; i < 5; i++) {
            int tabX = panelX + 8 + i * (tabW + 4);
            if (mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + 24) {
                selectedIndex = i;
                return true;
            }
        }

        // 委托给当前子面板
        if (selectedIndex >= 0 && selectedIndex < panels.length) {
            int contentX = panelX + 8;
            int contentY = panelY + 36;
            if (panels[selectedIndex].mouseClicked(mx, my, button, contentX, contentY)) {
                return true;
            }
        }

        // 点击面板外部 → 关闭
        if (mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH) {
            visible = false;
            return true;
        }

        return false;
    }

    /**
     * 处理键盘按键。
     *
     * @param keyCode 按键代码
     * @return true 表示按键被处理
     */
    public boolean keyPressed(int keyCode) {
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            visible = false;
            return true;
        }
        return false;
    }
}
