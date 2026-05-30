package com.metrogenesis.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.metrogenesis.init.BuildingType;
import com.metrogenesis.network.BuildToolPlacementMessage;
import com.metrogenesis.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import com.metrogenesis.gui.FeaturesOverviewScreen;
import java.util.ArrayList;
import java.util.List;

/**
 * 寤虹瓚閫夋嫨鐣岄潰 鈥?鍙抽敭鍦伴潰鍚庡脊鍑? * <p>
 * 鏄剧ず鎵€鏈夊彲鐢ㄧ殑寤虹瓚绫诲瀷璁╃帺瀹堕€夋嫨锛岀偣鍑荤‘璁ゅ悗鍙戦€佹斁缃姹傚埌鏈嶅姟绔€? */
public class BuildToolScreen extends Screen
{
    // 鈹€鈹€ 甯冨眬甯搁噺 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;
    private static final int TITLE_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 36;
    private static final int ITEM_GAP = 2;
    private static final int BTN_WIDTH = 80;
    private static final int BTN_HEIGHT = 18;
    private static final int BOTTOM_BAR = 30;

    // 鈹€鈹€ 棰滆壊 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
    private static final int C_BG = 0xD0081018;
    private static final int C_BORDER = 0xFF4A5568;
    private static final int C_ACCENT = 0xFF5B7FFF;
    private static final int C_ACCENT_HOVER = 0xFF7B9FFF;
    private static final int C_ITEM_BG = 0x88182030;
    private static final int C_ITEM_HOVER = 0x88203048;
    private static final int C_TITLE = 0xFFFFFFFF;
    private static final int C_TEXT = 0xFFD0D0D0;
    private static final int C_DESC = 0xFF808890;
    private static final int C_SELECTED = 0x99304860;
    private static final int C_BTN_ENABLED = 0xFF2D4A7F;
    private static final int C_BTN_DISABLED = 0xFF333333;
    private static final int C_BTN_HOVER = 0xFF3D6ABF;
    private static final int C_BTN_BORDER = 0xFF4A6A9F;

    private static final String OOverviewText = "[\u89C8]";  // 鎬昏鍏ュ彛

    private final BlockPos targetPos;
    private final List<BuildingType> buildingTypes = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int overviewBtnX, overviewBtnY;

    // 鈹€鈹€ 鏋勯€?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public BuildToolScreen(BlockPos targetPos)
    {
        super(Component.translatable("MetroGenesis.gui.buildtool.title"));
        this.targetPos = targetPos;

        // 鍔犺浇鎵€鏈夊彲鐢ㄥ缓绛戠被鍨?
        for (BuildingType type : BuildingType.ALL)
        {
            if (type.isEnabled()) buildingTypes.add(type);
        }
    }

    // 鈹€鈹€ 鍒濆鍖?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    protected void init()
    {
        super.init();
        selectedIndex = -1;
    }

    // 鈹€鈹€ 娓叉煋 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partialTick)
    {
        renderBackground(g);

        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;

        // 鑳屾櫙 + 杈规
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, C_BG);
        renderBorder(g, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);

        // 鏍囬
        g.drawString(font, title, guiLeft + 10, guiTop + 6, C_TITLE, false);

        // 鎬昏鍏ュ彛鎸夐挳锛堝彸涓婅锛?        overviewBtnX = guiLeft + GUI_WIDTH - 60;
        overviewBtnY = guiTop + 4;
        int obw = font.width(OOverviewText);
        boolean obHover = inBounds(mx, my, overviewBtnX, overviewBtnY, obw, 10);
        g.drawString(font, OOverviewText, overviewBtnX, overviewBtnY, obHover ? C_ACCENT : C_DESC, false);

        // 鍒嗛殧绾?
        int lineY = guiTop + TITLE_HEIGHT;
        g.fill(guiLeft + 8, lineY, guiLeft + GUI_WIDTH - 8, lineY + 1, C_BORDER);

        // 鍒楄〃鍖哄煙
        int listTop = lineY + 4;
        int listBottom = guiTop + GUI_HEIGHT - BOTTOM_BAR;
        int listHeight = listBottom - listTop;

        // 瑁佸壀鍒楄〃鍖哄煙
        enableScissor(guiLeft, listTop, GUI_WIDTH, listHeight);

        // 娓叉煋姣忎釜寤虹瓚鏉＄洰
        int itemY = listTop - scrollOffset;
        for (int i = 0; i < buildingTypes.size(); i++)
        {
            BuildingType type = buildingTypes.get(i);
            boolean hover = isHoveringItem(mx, my, guiLeft, listTop, listHeight, i);
            boolean selected = (i == selectedIndex);

            int itemBg = selected ? C_SELECTED : (hover ? C_ITEM_HOVER : C_ITEM_BG);
            g.fill(guiLeft + 8, itemY, guiLeft + GUI_WIDTH - 8, itemY + ITEM_HEIGHT, itemBg);

            // 閫変腑/鎮仠杈规
            if (selected || hover)
            {
                int borderCol = selected ? C_ACCENT : C_BORDER;
                g.fill(guiLeft + 8, itemY, guiLeft + GUI_WIDTH - 8, itemY + 1, borderCol);
                g.fill(guiLeft + 8, itemY + ITEM_HEIGHT - 1, guiLeft + GUI_WIDTH - 8, itemY + ITEM_HEIGHT, borderCol);
                g.fill(guiLeft + 8, itemY, guiLeft + 9, itemY + ITEM_HEIGHT, borderCol);
                g.fill(guiLeft + GUI_WIDTH - 9, itemY, guiLeft + GUI_WIDTH - 8, itemY + ITEM_HEIGHT, borderCol);
            }

            // 鍥炬爣鍗犱綅锛堟柟鍧楋級
            int iconX = guiLeft + 14;
            int iconY = itemY + 6;
            g.fill(iconX, iconY, iconX + 22, iconY + 22, 0xFF283848);
            g.fill(iconX + 1, iconY + 1, iconX + 21, iconY + 21, 0xFF385868);
            g.drawString(font, type.getIconChar(), iconX + 7, iconY + 6, 0xFFAABBCC, false);

            // 寤虹瓚鍚嶇О
            int textX = iconX + 30;
            g.drawString(font, type.getDisplayName(), textX, itemY + 5, C_TEXT, false);

            // 寤虹瓚鎻忚堪
            g.drawString(font, type.getDescription(), textX, itemY + 18, C_DESC, false);

            itemY += ITEM_HEIGHT + ITEM_GAP;
        }

        disableScissor();

        // 鍒楄〃楂樺害鎸囩ず锛堝鏋滄湁婊氬姩锛?
        int totalHeight = buildingTypes.size() * (ITEM_HEIGHT + ITEM_GAP);
        if (totalHeight > listHeight)
        {
            int scrollBarY = listTop + (listHeight * scrollOffset / (totalHeight - listHeight + 1));
            int scrollBarH = Math.max(8, listHeight * listHeight / (totalHeight + 1));
            g.fill(guiLeft + GUI_WIDTH - 4, listTop, guiLeft + GUI_WIDTH - 2, listBottom, 0x664A5568);
            g.fill(guiLeft + GUI_WIDTH - 4, scrollBarY, guiLeft + GUI_WIDTH - 2, scrollBarY + scrollBarH, 0xAA4A5568);
        }

        // 搴曢儴鎸夐挳
        int btnY = guiTop + GUI_HEIGHT - BOTTOM_BAR + 6;
        int btnCenterX = guiLeft + GUI_WIDTH / 2;

        // 鍙栨秷鎸夐挳
        boolean hasSelection = selectedIndex >= 0;
        drawButton(g, btnCenterX - BTN_WIDTH - 8, btnY, BTN_WIDTH, BTN_HEIGHT,
            hasSelection ? C_BTN_ENABLED : C_BTN_DISABLED,
            hasSelection ? C_BTN_HOVER : C_BTN_DISABLED,
            C_BTN_BORDER, Component.translatable("MetroGenesis.gui.buildtool.cancel").getString(),
            mx, my);

        // 纭鎸夐挳
        drawButton(g, btnCenterX + 8, btnY, BTN_WIDTH, BTN_HEIGHT,
            hasSelection ? C_BTN_ENABLED : C_BTN_DISABLED,
            hasSelection ? C_BTN_HOVER : C_BTN_DISABLED,
            C_BTN_BORDER, Component.translatable("MetroGenesis.gui.buildtool.confirm").getString(),
            mx, my);

        super.render(g, mx, my, partialTick);
    }

    // 鈹€鈹€ 榧犳爣浜嬩欢 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;
        int lineY = guiTop + TITLE_HEIGHT;
        int listTop = lineY + 4;
        int listBottom = guiTop + GUI_HEIGHT - BOTTOM_BAR;
        int listHeight = listBottom - listTop;

        // 鐐瑰嚮寤虹瓚鏉＄洰
        for (int i = 0; i < buildingTypes.size(); i++)
        {
            int itemY = listTop - scrollOffset + i * (ITEM_HEIGHT + ITEM_GAP);
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;

            if (mx >= guiLeft + 8 && mx < guiLeft + GUI_WIDTH - 8 &&
                my >= itemY && my < itemY + ITEM_HEIGHT)
            {
                selectedIndex = i;
                return true;
            }
        }

        // 纭鎸夐挳
        int btnY = guiTop + GUI_HEIGHT - BOTTOM_BAR + 6;
        int btnCenterX = guiLeft + GUI_WIDTH / 2;
        if (inBounds(mx, my, btnCenterX + 8, btnY, BTN_WIDTH, BTN_HEIGHT))
        {
            confirmSelection();
            return true;
        }

        // 鍙栨秷鎸夐挳
        if (inBounds(mx, my, btnCenterX - BTN_WIDTH - 8, btnY, BTN_WIDTH, BTN_HEIGHT))
        {
            onClose();
            return true;
        }

        // 鎬昏鍏ュ彛鎸夐挳
        int obw = font.width(OOverviewText);
        if (inBounds(mx, my, overviewBtnX, overviewBtnY, obw, 10))
        {
            Minecraft.getInstance().setScreen(new FeaturesOverviewScreen());
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta)
    {
        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;
        int listTop = guiTop + TITLE_HEIGHT + 4;
        int listBottom = guiTop + GUI_HEIGHT - BOTTOM_BAR;
        int listHeight = listBottom - listTop;
        int totalHeight = buildingTypes.size() * (ITEM_HEIGHT + ITEM_GAP);

        if (totalHeight > listHeight)
        {
            scrollOffset = Math.max(0, Math.min(totalHeight - listHeight,
                scrollOffset - (int) (delta * ITEM_HEIGHT)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // 鈹€鈹€ 纭 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private void confirmSelection()
    {
        if (selectedIndex < 0 || selectedIndex >= buildingTypes.size()) return;

        BuildingType selected = buildingTypes.get(selectedIndex);

        // 鍙戦€佹斁缃姹傚埌鏈嶅姟绔?
        NetworkHandler.CHANNEL.sendToServer(
            new BuildToolPlacementMessage(targetPos, selected.getId()));

        onClose();
    }

    // 鈹€鈹€ 杈呭姪鏂规硶 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    private void renderBorder(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 1, y + h, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER);
    }

    private void drawButton(GuiGraphics g, int bx, int by, int w, int h,
                             int bg, int bgHover, int border, String text,
                             int mx, int my)
    {
        boolean hover = inBounds(mx, my, bx, by, w, h);
        int col = hover ? bgHover : bg;
        g.fill(bx - 1, by - 1, bx + w + 1, by + h + 1, border);
        g.fill(bx, by, bx + w, by + h, col);
        int tw = font.width(text);
        g.drawString(font, text, bx + (w - tw) / 2, by + (h - 8) / 2, 0xFFFFFF, false);
    }

    private boolean inBounds(double mx, double my, int bx, int by, int w, int h)
    {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }

    private boolean isHoveringItem(double mx, double my, int guiLeft, int listTop, int listHeight, int index)
    {
        int itemY = listTop - scrollOffset + index * (ITEM_HEIGHT + ITEM_GAP);
        if (itemY + ITEM_HEIGHT < listTop || itemY > listTop + listHeight) return false;
        return mx >= guiLeft + 8 && mx < guiLeft + GUI_WIDTH - 8 &&
               my >= itemY && my < itemY + ITEM_HEIGHT;
    }

    private void enableScissor(int x, int y, int w, int h)
    {
        RenderSystem.enableScissor(
            (int) (x * minecraft.getWindow().getGuiScale()),
            (int) (minecraft.getWindow().getHeight() - (y + h) * minecraft.getWindow().getGuiScale()),
            (int) (w * minecraft.getWindow().getGuiScale()),
            (int) (h * minecraft.getWindow().getGuiScale())
        );
    }

    private void disableScissor()
    {
        RenderSystem.disableScissor();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
