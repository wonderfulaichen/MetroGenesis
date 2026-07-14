package com.metrogenesis.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * MGStyles — MetroGenesis UI 统一样式类（Create 像素工业风 v1.2）。
 *
 * 规范来源：docs/UI重建设计规范_v1.md（Create 工业风 + Playlist 玻璃拟态）。
 * 集中管理 §3 全部色值，并提供 §9 要求的程序化绘制工具方法，
 * 供 MayorBookScreen / MayorBookCatalogPanel / TreasuryPanel 等所有界面共用。
 *
 * 设计基调：半透明深色玻璃底 + 1px 黄铜细线为骨 + 2x2 像素角 + 金色粒子点缀。
 */
public final class MGStyles {

    private MGStyles() {}

    // ═══════════════════════════════════════════════════
    // §3.1 基础底色（半透明深色玻璃拟态）
    // ═══════════════════════════════════════════════════
    public static final int C_BG       = 0xCC1A1A1A; // 主背景 #1A1A1A 80%
    public static final int C_BG_PANEL = 0xDD1E1E1E; // 面板 #1E1E1E 87%
    public static final int C_BG_CARD  = 0xE62A2A2A; // 卡片 #2A2A2A 90%
    public static final int C_BG_HOVER = 0xF0333333; // 悬停/选中 #333333 94%
    public static final int C_BG_MODAL = 0xF01A1A1A; // 模态 #1A1A1A 94%

    // ═══════════════════════════════════════════════════
    // §3.2 黄铜（细线条 + 强调主色）
    // ═══════════════════════════════════════════════════
    public static final int C_BRASS    = 0xC89B3C; // 主黄铜边框/标题
    public static final int C_BRASS_LT = 0xE0BE5A; // 黄铜高光（悬停）
    public static final int C_BRASS_DK = 0x9C7728; // 黄铜阴影

    // ═══════════════════════════════════════════════════
    // §3.2.1 强调色（琥珀/橙）
    // ═══════════════════════════════════════════════════
    public static final int C_ACCENT   = 0xD9772B; // 工业橙（主强调）
    public static final int C_ACCENT_DK = 0xB35E1F;
    public static final int C_ACCENT_LT = 0xE8944A;

    // ═══════════════════════════════════════════════════
    // §3.3 绿色（仅成功状态）
    // ═══════════════════════════════════════════════════
    public static final int C_GREEN_SUCCESS = 0x5A8A4A;
    public static final int C_GREEN_DK      = 0x3A6A2A;

    // ═══════════════════════════════════════════════════
    // §3.4 功能/行动色
    // ═══════════════════════════════════════════════════
    public static final int C_ACTION  = 0xD9772B;
    public static final int C_WARNING = 0xE8B53A;
    public static final int C_DANGER  = 0xA64B32;

    // ═══════════════════════════════════════════════════
    // §3.4.1 金色/琥珀粒子
    // ═══════════════════════════════════════════════════
    public static final int C_PARTICLE     = 0xE0BE5A;
    public static final int C_PARTICLE_DK  = 0xC89B3C;
    public static final int C_PARTICLE_LT  = 0xF0D878;

    // ═══════════════════════════════════════════════════
    // §3.5 文字色
    // ═══════════════════════════════════════════════════
    public static final int C_TEXT      = 0xE0D8CC; // 主文字暖白
    public static final int C_TEXT_2ND  = 0x9A9088; // 次要暖灰
    public static final int C_TEXT_3RD  = 0x6A6058; // 三级/禁用
    public static final int C_TEXT_BRASS = 0xC89B3C; // 黄铜色文字（标题/数字）

    // ═══════════════════════════════════════════════════
    // §3.6 功能区色（住宅/工业/商业/农业/公共）
    // ═══════════════════════════════════════════════════
    public static final int ZONE_RESIDENTIAL = 0x4A8A4A;
    public static final int ZONE_INDUSTRIAL  = 0xB5703A;
    public static final int ZONE_COMMERCIAL  = 0x5A7A8A;
    public static final int ZONE_AGRICULTURE = 0x7A8A3A;
    public static final int ZONE_PUBLIC      = 0x7A6A8A;

    // ═══════════════════════════════════════════════════
    // §3.5 旧 GLASS_* 别名（迁移兼容：让现有屏逐步替换，不一次爆破）
    // ═══════════════════════════════════════════════════
    public static final int GLASS_BG     = C_BG;
    public static final int GLASS_PANEL  = C_BG_PANEL;
    public static final int GLASS_HEADER = C_BG_PANEL;
    public static final int TEXT_PRIMARY   = C_TEXT;
    public static final int TEXT_SECONDARY = C_TEXT_2ND;
    public static final int TEXT_SOFT_GOLD = C_TEXT_BRASS;
    public static final int TEXT_GREEN     = C_GREEN_SUCCESS;

    // ═══════════════════════════════════════════════════
    // 绘制工具
    // ═══════════════════════════════════════════════════

    /** 带 alpha 的颜色（保留原色 alpha 通道并按比例混合） */
    public static int withAlpha(int color, int alpha) {
        if (alpha >= 255) return color;
        int a = (color >>> 24) & 0xFF;
        a = (a * alpha) / 255;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * 统一面板：半透明深色玻璃底 + 1px 黄铜细框 + 四角 2x2 像素角。
     * 替代原 fillGlassPanel，是 v1.2 换皮的核心外观。
     */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int bg, int alpha) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, withAlpha(bg, alpha));
        drawBrassFrame(g, x, y, w, h, alpha);
        drawPixelCorner(g, x, y, w, h, alpha);
    }

    /** 1px 黄铜细框（四边） */
    public static void drawBrassFrame(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        int c = withAlpha(C_BRASS, alpha);
        g.fill(x, y, x + w, y + 1, c);            // 上
        g.fill(x, y + h - 1, x + w, y + h, c);    // 下
        g.fill(x, y, x + 1, y + h, c);            // 左
        g.fill(x + w - 1, y, x + w, y + h, c);    // 右
    }

    /** 四角 2x2 像素方块（黄铜色，增强像素感），代替圆角/铆钉 */
    public static void drawPixelCorner(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        int c = withAlpha(C_BRASS_LT, alpha);
        g.fill(x, y, x + 2, y + 2, c);
        g.fill(x + w - 2, y, x + w, y + 2, c);
        g.fill(x, y + h - 2, x + 2, y + h, c);
        g.fill(x + w - 2, y + h - 2, x + w, y + h, c);
    }

    /** 黄铜按钮底（深色 + 1px 黄铜边框 + 顶部极细高光），用于工具栏/普通按钮 */
    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, int bg, int alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(bg, alpha));
        drawBrassFrame(g, x, y, w, h, alpha);
        if (w > 4) g.fill(x + 2, y, x + w - 2, y + 1, withAlpha(C_BRASS_LT, (alpha * 60) / 100));
    }

    /** 文字（暖白/指定色，带 shadow），兼容原 drawGlassText 调用 */
    public static void drawText(GuiGraphics g, Font font, String text, int x, int y, int color, int alpha, boolean shadow) {
        g.drawString(font, text, x, y, withAlpha(color, alpha), shadow);
    }

    /**
     * 金色/琥珀粒子场（§3.4.1 / §4）：在区域内漂浮 1~2px 方块，缓慢移动。
     * timeMs 驱动动画；用确定性伪随机分布，避免每帧抖动。
     */
    public static void drawParticles(GuiGraphics g, int x, int y, int w, int h, long timeMs, int alpha) {
        final int COUNT = Math.max(6, (w * h) / 4000);
        final float t = (timeMs % 6000L) / 6000f; // 0~1 循环
        for (int i = 0; i < COUNT; i++) {
            // 确定性位置
            double r1 = fract(Math.sin(i * 12.9898) * 43758.5453);
            double r2 = fract(Math.sin(i * 78.233) * 12543.123);
            double r3 = fract(Math.sin(i * 39.425) * 9123.777);
            int px = x + (int) (r1 * w);
            int py = y + (int) (((r2 + t * (0.3 + r3 * 0.7)) % 1.0) * h);
            int pc = (i % 3 == 0) ? C_PARTICLE_LT : (i % 3 == 1) ? C_PARTICLE : C_PARTICLE_DK;
            int a = (int) (alpha * (20 + r3 * 30) / 100);
            int s = (r3 > 0.66) ? 2 : 1;
            g.fill(px, py, px + s, py + s, withAlpha(pc, a));
        }
    }

    /** Factorio 风黄黑警示条纹（§7.3），用于进度/建设语义 */
    public static void drawHazardStripe(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        int yellow = withAlpha(C_WARNING, alpha);
        int black = withAlpha(0x1A1A1A, alpha);
        int step = 8;
        for (int i = -h; i < w; i += step) {
            // 简单对角线：用矩形近似
            g.fill(x + i, y, x + i + 4, y + h, yellow);
            g.fill(x + i + 4, y, x + i + 8, y + h, black);
        }
    }

    /** 进度条：深灰凹槽 + 强调色填充（§7.3） */
    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h, float progress, int alpha) {
        int groove = withAlpha(0x1A1A1A, alpha);
        g.fill(x, y, x + w, y + h, groove);
        int fillW = (int) (w * Math.max(0f, Math.min(1f, progress)));
        if (fillW > 0) g.fill(x, y, x + fillW, y + h, withAlpha(C_ACCENT, alpha));
        drawBrassFrame(g, x, y, w, h, alpha);
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }
}
