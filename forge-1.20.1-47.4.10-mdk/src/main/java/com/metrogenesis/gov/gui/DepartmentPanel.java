package com.metrogenesis.gov.gui;

import com.metrogenesis.gui.MayorBookScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 部门面板接口 — 每个部门面板实现此接口，提供渲染和点击处理。
 * <p>
 * {@code render} 和 {@code mouseClicked} 由 {@link DepartmentTabPanel} 委托调用。
 * 面板之间不共享状态，各自持有对自身 DepartmentData 的引用。
 */
public interface DepartmentPanel {

    /**
     * 渲染部门面板内容。
     *
     * @param g     GuiGraphics
     * @param mx    鼠标 X（屏幕坐标）
     * @param my    鼠标 Y（屏幕坐标）
     * @param x     面板左上角 X（相对于 DepartmentTabPanel 内部）
     * @param y     面板左上角 Y
     * @param w     面板可用宽度
     * @param h     面板可用高度
     * @param alpha 整体透明度
     * @param screen MayorBookScreen 引用（可调用 fillGlassPanel / drawGlassText 等）
     */
    void render(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int alpha, MayorBookScreen screen);

    /**
     * 处理鼠标点击。
     *
     * @param mx     鼠标 X（绝对屏幕坐标）
     * @param my     鼠标 Y（绝对屏幕坐标）
     * @param button 鼠标按钮（GLFW_MOUSE_BUTTON_LEFT 等）
     * @param x      面板左上角 X（绝对屏幕坐标）
     * @param y      面板左上角 Y
     * @return true 表示点击被处理
     */
    boolean mouseClicked(double mx, double my, int button, int x, int y);
}
