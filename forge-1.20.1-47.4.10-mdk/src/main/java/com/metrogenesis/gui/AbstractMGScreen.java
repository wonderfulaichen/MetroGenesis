package com.metrogenesis.gui;

import com.metrogenesis.blockui.Loader;
import com.metrogenesis.blockui.controls.Button;
import com.metrogenesis.blockui.controls.ButtonHandler;
import com.metrogenesis.blockui.views.BOWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MetroGenesis GUI 统一基类 — 基于 BlockUI 的窗口基座。
 * <p>
 * 封装了常用 GUI 模式：按钮注册、Pane 查找、窗口打开。
 * 对标 MineColonies {@code AbstractWindowSkeleton} 但不复制其具体实现，
 * 仅提取通用模式。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * // 方式一：继承 AbstractMGScreen 创建全屏 GUI
 * public class MyScreen extends AbstractMGScreen {
 *     public MyScreen() {
 *         super("metrogenesis", "gui/mywindow.xml");
 *         registerButton("ok", () -> doSomething());
 *     }
 * }
 * new MyScreen().open();
 *
 * // 方式二：用 BOWindow 子类（MineColonies 兼容模式）
 * public class MyWindow extends BOWindow {
 *     public MyWindow() {
 *         super(new ResourceLocation("metrogenesis", "gui/mywindow.xml"));
 *     }
 * }
 * new MyWindow().open();
 * </pre>
 */
public class AbstractMGScreen extends com.metrogenesis.blockui.BOScreen implements ButtonHandler
{
    /** 按钮 ID → 回调映射 */
    @NotNull
    private final Map<String, Consumer<Button>> buttons = new HashMap<>();

    /** 窗口对应的 XML 资源路径 */
    @Nullable
    protected final ResourceLocation xmlResource;

    // ════════════════════════════════════════════════════════
    //  构造器
    // ════════════════════════════════════════════════════════

    /**
     * 从 XML 布局文件创建屏幕。
     *
     * @param modId    mod ID（如 "metrogenesis"）
     * @param xmlPath  XML 路径（如 "gui/buildtool.xml"）
     */
    public AbstractMGScreen(final String modId, final String xmlPath)
    {
        this(new ResourceLocation(modId, xmlPath));
    }

    /**
     * 从 ResourceLocation 加载 XML 布局。
     *
     * @param resource XML 资源位置
     */
    public AbstractMGScreen(final ResourceLocation resource)
    {
        super(new BOWindow(resource));
        this.xmlResource = resource;
    }

    /**
     * 使用已有的 BOWindow 实例创建屏幕。
     *
     * @param window 已经初始化好的 BOWindow
     */
    public AbstractMGScreen(final BOWindow window)
    {
        super(window);
        this.xmlResource = null;
    }

    // ════════════════════════════════════════════════════════
    //  按钮注册
    // ════════════════════════════════════════════════════════

    /**
     * 注册一个按钮的点击回调（不关心按钮实例）。
     *
     * @param id     按钮 ID（匹配 XML 中 button 的 id 属性）
     * @param action 回调操作
     */
    public final void registerButton(final String id, final Runnable action)
    {
        registerButton(id, button -> action.run());
    }

    /**
     * 注册一个按钮的点击回调（可获取按钮实例）。
     *
     * @param id     按钮 ID
     * @param action 回调操作（接收按钮实例）
     */
    public final void registerButton(final String id, final Consumer<Button> action)
    {
        buttons.put(id, action);
    }

    // ════════════════════════════════════════════════════════
    //  ButtonHandler 接口 — 事件路由
    // ════════════════════════════════════════════════════════

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        final Consumer<Button> action = buttons.get(button.getID());
        if (action != null)
        {
            action.accept(button);
        }
    }

    // ════════════════════════════════════════════════════════
    //  Pane 查找工具方法
    // ════════════════════════════════════════════════════════

    /**
     * 按 ID 查找并转型 Pane。
     *
     * @param id    Pane ID
     * @param type  目标类型
     * @param <T>   类型参数
     * @return 找到的 Pane
     * @throws NullPointerException 如果未找到
     */
    public <T extends com.metrogenesis.blockui.Pane> T findPane(final String id, final Class<T> type)
    {
        return window.findPaneOfTypeByID(id, type);
    }

    // ════════════════════════════════════════════════════════
    //  打开/关闭
    // ════════════════════════════════════════════════════════

    /**
     * 打开此 GUI 屏幕。
     */
    public void open()
    {
        Minecraft.getInstance().setScreen(this);
    }

    /**
     * 获取此屏幕的 BOWindow 实例。
     */
    public BOWindow getWindow()
    {
        return window;
    }
}
