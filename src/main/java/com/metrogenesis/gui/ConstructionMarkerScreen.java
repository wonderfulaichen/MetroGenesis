package com.metrogenesis.gui;

import com.metrogenesis.gui.base.MetroGenesisScreen;
import com.metrogenesis.init.BuildingType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 施工管理 GUI — 继承 MetroGenesisScreen 基类
 * <p>
 * Phase 0e: 改为动态建筑列表，数据源为 BuildingType.ALL（仅 enabled）。
 * <p>
 * 布局：
 * <pre>
 * ┌──────────────────────────────────────┐
 * │ §6§l施工管理                           │
 * ├──────────────────────────────────────┤
 * │ 选择要建造的设施                       │
 * │ ┌──────────────────────────────────┐ │
 * │ │ §l🏛 市政厅                       │ │
 * │ ├──────────────────────────────────┤ │
 * │ │ §l🏗 农场                         │ │
 * │ ├──────────────────────────────────┤ │
 * │ │ §l🏠 住宅                         │ │
 * │ └──────────────────────────────────┘ │
 * │                              进度条    │
 * └──────────────────────────────────────┘
 * </pre>
 */
public class ConstructionMarkerScreen extends MetroGenesisScreen<ConstructionMarkerMenu> {

    private static final int W = 200, H = 180;

    // 列表参数
    private static final int LIST_X = 16;
    private static final int LIST_Y = 34;
    private static final int ITEM_W = 168;
    private static final int ITEM_H = 24;
    private static final int ITEM_GAP = 2;

    // 进度条
    private static final int BAR_X = 20, BAR_Y = 60;
    private static final int BAR_W = 160, BAR_H = 14;

    // 动态建筑列表（仅 enabled 的类型）
    private final List<BuildingType> buildingTypes;

    public ConstructionMarkerScreen(ConstructionMarkerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;

        // 加载可用建筑类型
        this.buildingTypes = BuildingType.ALL.stream()
                .filter(BuildingType::isEnabled)
                .toList();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        super.renderBg(g, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // 标题
        g.drawString(font, "§6§l施工管理", 55, 8, 0xFFFFFF, false);

        int progress = menu.getProgress();

        if (progress > 0 && progress < 100) {
            // 建造中：显示进度条
            drawProgressBar(g, progress);
            g.drawString(font, "§7建造中... §e" + progress + "%", 20, 90, 0xFFFFFF, false);
            g.drawString(font, "§7等待建造师施工...", 20, 110, 0xAAAAAA, false);
        } else if (progress >= 100) {
            // 建造完成
            g.drawString(font, "§a§l✔ 建造完成！", 55, 40, 0xFFFFFF, false);
        } else {
            // 待选择：动态列表
            g.drawString(font, "§7选择要建造的设施", LIST_X, 22, 0xFFFFFF, false);

            for (int i = 0; i < buildingTypes.size(); i++) {
                BuildingType type = buildingTypes.get(i);
                int itemY = LIST_Y + i * (ITEM_H + ITEM_GAP);

                // 背景色（hover 检测在 mouseClicked 中处理）
                int bg = 0xFF1B2838;
                int border = 0xFF4A5568;
                boolean hover = isHoveringItem(mx, my, itemY);

                if (hover) {
                    bg = 0xFF253548;
                    border = 0xFF5B7FFF;
                }

                // 项目背景
                g.fill(LIST_X, itemY, LIST_X + ITEM_W, itemY + ITEM_H, bg);
                // 边框
                g.fill(LIST_X, itemY, LIST_X + ITEM_W, itemY + 1, border);
                g.fill(LIST_X, itemY + ITEM_H - 1, LIST_X + ITEM_W, itemY + ITEM_H, border);
                g.fill(LIST_X, itemY, LIST_X + 1, itemY + ITEM_H, border);
                g.fill(LIST_X + ITEM_W - 1, itemY, LIST_X + ITEM_W, itemY + ITEM_H, border);

                // 图标
                g.drawString(font, type.getIconChar(), LIST_X + 6, itemY + 5, 0xFFAABBCC, false);

                // 名称 + 描述
                g.drawString(font, "§l" + type.getDisplayName(), LIST_X + 28, itemY + 4, 0xFFD0D0D0, false);
                g.drawString(font, "§7" + type.getDescription(), LIST_X + 28, itemY + 14, 0xFF808890, false);
            }
        }
    }

    /**
     * 检测鼠标是否悬停在某个列表项上
     */
    private boolean isHoveringItem(double mx, double my, int itemY) {
        double localX = mx - leftPos;
        double localY = my - topPos;
        return localX >= LIST_X && localX < LIST_X + ITEM_W
            && localY >= itemY && localY < itemY + ITEM_H;
    }

    private void drawProgressBar(GuiGraphics g, int progress) {
        g.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0xFF333333);
        int filled = (BAR_W * progress) / 100;
        if (filled > 0) {
            int color = progress < 50 ? 0xFFE65100 : (progress < 80 ? 0xFFFFD600 : 0xFF00C853);
            g.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_H, color);
        }
        // 边框
        g.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + 1, 0xFF555555);
        g.fill(BAR_X, BAR_Y + BAR_H - 1, BAR_X + BAR_W, BAR_Y + BAR_H, 0xFF555555);
        // 百分比文字
        String pct = progress + "%";
        int tw = font.width(pct);
        g.drawString(font, "§l" + pct, BAR_X + (BAR_W - tw) / 2, BAR_Y + 3, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (menu.getProgress() == 0) {
                // 遍历动态列表，检测点击
                for (int i = 0; i < buildingTypes.size(); i++) {
                    int itemY = LIST_Y + i * (ITEM_H + ITEM_GAP);
                    if (isHoveringItem(mx, my, itemY)) {
                        BuildingType type = buildingTypes.get(i);
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                        showStatus("§e开始建造" + type.getDisplayName() + "..", 40);
                        minecraft.player.closeContainer();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
