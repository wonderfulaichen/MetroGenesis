package com.metrogenesis.gui;

import com.metrogenesis.blockui.Pane;
import com.metrogenesis.blockui.controls.Button;
import com.metrogenesis.blockui.controls.Text;
import com.metrogenesis.blockui.views.ScrollingList;
import com.metrogenesis.init.BuildingType;
import com.metrogenesis.network.BuildToolPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import com.metrogenesis.structurize.client.gui.AbstractWindowSkeleton;
import com.metrogenesis.structurize.util.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 建筑选择窗口 — BlockUI 实现。
 */
public class BuildToolWindow extends AbstractWindowSkeleton
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BlockPos targetPos;
    private final List<BuildingType> buildingTypes = new ArrayList<>();
    private int selectedIndex = -1;
    private RotationMirror currentRotMir = RotationMirror.NONE;

    private ScrollingList buildingList;

    // ════════════════════════════════════════════════════════
    //  构造器
    // ════════════════════════════════════════════════════════

    public BuildToolWindow(final BlockPos targetPos)
    {
        super(new ResourceLocation("metrogenesis", "gui/buildtool.xml"));
        this.targetPos = targetPos;

        // 加载可用建筑
        for (BuildingType type : BuildingType.ALL)
        {
            if (type.isEnabled()) buildingTypes.add(type);
        }

        buildingList = findPaneOfTypeByID("buildingList", ScrollingList.class);

        // 注册按钮
        registerButton("select", (Button b) -> onItemSelected(b));
        registerButton("rotate", () -> cycleRotation());
        registerButton("mirror", () -> toggleMirror());
        registerButton("cancel", this::close);
        registerButton("confirm", this::confirmPlacement);

        setupBuildingList();
    }

    // ════════════════════════════════════════════════════════
    //  生命周期
    // ════════════════════════════════════════════════════════

    @Override
    public void onOpened()
    {
        super.onOpened();
        updateUI();
        LOGGER.debug("BuildToolWindow opened");
    }

    // ════════════════════════════════════════════════════════
    //  建筑列表
    // ════════════════════════════════════════════════════════

    private void setupBuildingList()
    {
        if (buildingList == null) return;

        buildingList.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount() { return buildingTypes.size(); }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final BuildingType type = buildingTypes.get(index);

                // 更新文字
                Text name = rowPane.findPaneOfTypeByID("name", Text.class);
                Text desc = rowPane.findPaneOfTypeByID("desc", Text.class);
                Text icon = rowPane.findPaneOfTypeByID("icon", Text.class);

                if (name != null)
                    name.setText(Component.literal(type.getDisplayName()));
                if (desc != null)
                    desc.setText(Component.literal(type.getDescription()));
                if (icon != null)
                {
                    String c = type.getIconChar();
                    icon.setText(Component.literal(
                        (c != null && !c.isEmpty()) ? c : "\u25A0"));
                }
            }
        });
    }

    /**
     * 处理列表项点击 — 使用 ScrollingList 的内置方法反推行索引。
     */
    private void onItemSelected(@NotNull final Button button)
    {
        if (buildingList == null) return;
        final int index = buildingList.getListElementIndexByPane(button);
        if (index >= 0 && index < buildingTypes.size())
        {
            selectedIndex = index;
            updateUI();
        }
    }

    // ════════════════════════════════════════════════════════
    //  按钮回调
    // ════════════════════════════════════════════════════════

    /**
     * 旋转 — 使用 RotationMirror.rotate() 枚举方法。
     */
    private void cycleRotation()
    {
        currentRotMir = currentRotMir.rotate(Rotation.CLOCKWISE_90);
        updateUI();
    }

    /**
     * 镜像 — 使用 RotationMirror.mirrorate() 枚举方法。
     */
    private void toggleMirror()
    {
        currentRotMir = currentRotMir.mirrorate();
        updateUI();
    }

    /**
     * 确认放置 — 发送网络包到服务端。
     */
    private void confirmPlacement()
    {
        if (selectedIndex < 0 || selectedIndex >= buildingTypes.size()) return;

        BuildingType selected = buildingTypes.get(selectedIndex);
        LOGGER.debug("Place {} at {} with {}", selected.getId(), targetPos, currentRotMir);

        NetworkHandler.CHANNEL.sendToServer(
            new BuildToolPlacementMessage(targetPos, selected.getId(), currentRotMir)
        );
        close();
    }

    // ════════════════════════════════════════════════════════
    //  UI 状态更新
    // ════════════════════════════════════════════════════════

    private void updateUI()
    {
        // 旋转按钮标签
        Button rb = findPaneOfTypeByID("rotate", Button.class);
        if (rb != null)
        {
            rb.setText(Component.literal("\u21BB " + angleLabel(currentRotMir.rotation())));
        }

        // 镜像按钮标签
        Button mb = findPaneOfTypeByID("mirror", Button.class);
        if (mb != null)
        {
            mb.setText(Component.literal("\u2194 " + (currentRotMir.isMirrored() ? "\u5F00" : "\u5173")));
        }

        // 刷新列表
        if (buildingList != null) buildingList.refreshElementPanes();
    }

    private static String angleLabel(final Rotation r)
    {
        return switch (r)
        {
            case NONE -> "0\u00B0";
            case CLOCKWISE_90 -> "90\u00B0";
            case CLOCKWISE_180 -> "180\u00B0";
            case COUNTERCLOCKWISE_90 -> "270\u00B0";
        };
    }

    // ════════════════════════════════════════════════════════
    //  键盘
    // ════════════════════════════════════════════════════════

    @Override
    public boolean onKeyTyped(final char ch, final int keyCode)
    {
        if (keyCode == 82) { cycleRotation();   return true; }  // R
        if (keyCode == 77) { toggleMirror();    return true; }  // M
        if (keyCode == 256) { close();          return true; }  // ESC
        return super.onKeyTyped(ch, keyCode);
    }

    // ════════════════════════════════════════════════════════
    //  工厂
    // ════════════════════════════════════════════════════════

    public static void openAt(final BlockPos targetPos)
    {
        new BuildToolWindow(targetPos).open();
    }
}
