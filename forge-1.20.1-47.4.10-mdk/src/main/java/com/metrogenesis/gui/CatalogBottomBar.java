package com.metrogenesis.gui;

import com.metrogenesis.catalog.BuildingCatalogEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;

import javax.annotation.Nullable;

/**
 * 底部操作栏组件 — 显示当前选中建筑的名称/尺寸/包名/放置按钮。
 */
public class CatalogBottomBar {

    private final MayorBookScreen owner;
    private final Font font;

    public CatalogBottomBar(MayorBookScreen owner, Font font) {
        this.owner = owner;
        this.font = font;
    }

    public void render(GuiGraphics g, int panelX, int bottomY, int panelW,
                       @Nullable BuildingCatalogEntry entry, int alpha) {
        if (entry == null) {
            owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.064"),
                panelX + 12, bottomY + 8, owner.TEXT_SECONDARY, alpha);
            return;
        }

        final String localizedName = ContentNameLocalizer.localize(entry.name());
        owner.drawGlassText(g, localizedName, panelX + 12, bottomY + 7, owner.TEXT_SOFT_GOLD, alpha);

        final String sizeStr = entry.sizeDisplay();
        owner.drawGlassText(g, sizeStr, panelX + 12 + font.width(localizedName) + 12,
            bottomY + 7, owner.TEXT_SECONDARY, alpha);

        final String packStr = "[" + ContentNameLocalizer.localizePackName(entry.packName()) + "]";
        owner.drawGlassText(g, packStr,
            panelX + 12 + font.width(localizedName) + 12 + font.width(sizeStr) + 12,
            bottomY + 7, owner.TEXT_SECONDARY, alpha);

        // 放置按钮
        final int btnW = 60;
        final int btnX = panelX + panelW - btnW - 16;
        owner.fillGlassButton(g, btnX, bottomY + 3, btnW, 20,
            owner.inBounds(btnX, bottomY + 3, btnW, 20) ? 0x66486880 : 0x44384858, alpha);
        owner.drawGlassText(g, Language.getInstance().getOrDefault("gui.metrogenesis.catalog.065"),
            btnX + (btnW - font.width(Language.getInstance().getOrDefault("gui.metrogenesis.catalog.065"))) / 2,
            bottomY + 9,
            owner.inBounds(btnX, bottomY + 3, btnW, 20) ? owner.TEXT_SOFT_GOLD : owner.TEXT_PRIMARY, alpha);
    }

    /** 检测放置按钮点击 */
    public boolean isPlaceButtonClicked(double mx, double my, int panelX, int bottomY, int panelW) {
        int btnW = 60, btnX = panelX + panelW - btnW - 16;
        return owner.inBounds(btnX, bottomY + 3, btnW, 20);
    }
}
