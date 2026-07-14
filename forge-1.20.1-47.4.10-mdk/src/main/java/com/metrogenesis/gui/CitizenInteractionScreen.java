package com.metrogenesis.gui;

import com.metrogenesis.entity.MetroGenesisCitizen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 甯傛皯浜や簰淇℃伅绐?鈥?鍙抽敭甯傛皯鏃跺脊鍑? * <p>
 * 绾俊鎭睍绀虹晫闈紝涓嶄緷璧?Container/MenuType銆? * 鏁版嵁鏉ヨ嚜瀹炰綋鐨?SynchedEntityData锛堝凡鑷姩鍚屾鍒板鎴风锛夈€? */
public class CitizenInteractionScreen extends Screen {

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 180;

    private static final int BG_COLOR = 0xD0081018;
    private static final int BORDER_COLOR = 0xFF4A5568;

    private final MetroGenesisCitizen citizen;

    public CitizenInteractionScreen(MetroGenesisCitizen citizen) {
        super(Component.literal("Citizen Info"));
        this.citizen = citizen;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        drawFrame(g);
        drawInfo(g);
        drawCloseButton(g, mx, my);
        super.render(g, mx, my, pt);
    }

    // ══ 甯冨眬 ═════════════════════════════════════════

    private void drawFrame(GuiGraphics g) {
        int cx = (width - GUI_WIDTH) / 2;
        int cy = (height - GUI_HEIGHT) / 2;

        // 鑳屾櫙
        g.fill(cx, cy, cx + GUI_WIDTH, cy + GUI_HEIGHT, BG_COLOR);
        // 杈规
        g.fill(cx, cy, cx + GUI_WIDTH, cy + 1, BORDER_COLOR);
        g.fill(cx, cy + GUI_HEIGHT - 1, cx + GUI_WIDTH, cy + GUI_HEIGHT, BORDER_COLOR);
        g.fill(cx, cy, cx + 1, cy + GUI_HEIGHT, BORDER_COLOR);
        g.fill(cx + GUI_WIDTH - 1, cy, cx + GUI_WIDTH, cy + GUI_HEIGHT, BORDER_COLOR);
    }

    private void drawCloseButton(GuiGraphics g, int mx, int my) {
        int cx = (width - GUI_WIDTH) / 2;
        int cy = (height - GUI_HEIGHT) / 2;
        int bx = cx + GUI_WIDTH - 22;
        int by = cy + 6;
        boolean hov = mx >= bx && mx < bx + 16 && my >= by && my < by + 16;
        g.fill(bx, by, bx + 16, by + 16, hov ? 0xFFFF5555 : 0xAA333333);
        g.drawString(font, "脳", bx + 5, by + 4, 0xFFFFFF);
    }

    // ══ 淇℃伅缁樺埗 ═════════════════════════════════════

    private void drawInfo(GuiGraphics g) {
        int cx = (width - GUI_WIDTH) / 2;
        int cy = (height - GUI_HEIGHT) / 2;
        int pad = 16;
        int x = cx + pad;
        int y = cy + pad;

        // 1. 绾圭悊澶村儚鍖哄煙锛?6x16 绀烘剰锛?
        ResourceLocation tex = getCitizenTexture();
        g.blit(tex, x, y, 24, 24, 8, 8, 16, 16, 64, 64);
        g.fill(x, y, x + 24, y + 24, 0x44FFFFFF); // 鍗婇€忔槑搴曡壊

        // 2. 鍚嶅瓧锛堝乏渚у亸绉伙級
        String name = citizen.getCitizenName();
        g.drawString(font, "§l" + name, x + 32, y + 4, 0xFFFFFF);

        // 3. 鑱屼笟
        y += 34;
        String job = citizen.getCitizenJob();
        String jobDisplay = switch (job) {
            case "farmer" -> "农夫";
            case "merchant" -> "商人";
            case "builder" -> "建造师";
            case "miner" -> "矿工";
            case "lumberjack" -> "伐木工";
            case "crafter" -> "工匠";
            case "unemployed" -> "待业";
            default -> job;
        };
        g.drawString(font, "§7鑱屼笟锛毬" + jobDisplay, x, y, 0xFFFFFF);

        // 4. 閽卞寘
        y += 12;
        int wallet = citizen.getWallet();
        g.drawString(font, "§7閽卞寘锛毬" + wallet + " §7C-Value", x, y, 0xFFFFFF);

        // 5. 鎬у埆
        y += 12;
        String gender = citizen.isFemale() ? "\u2640 \u5973\u6027" : "\u2642 \u7537\u6027";
        g.drawString(font, "§7鎬у埆锛毬" + gender, x, y, 0xFFFFFF);

        // 6. 婊℃剰搴﹁繘搴︽潯
        y += 18;
        int sat = citizen.getSatisfaction();
        int barWidth = GUI_WIDTH - pad * 2;
        int barHeight = 8;
        // 鑳屾櫙鏉?        g.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);
        // 濉厖鏉★紙棰滆壊闅忔弧鎰忓害鍙樺寲锛?
        int fill = barWidth * sat / 100;
        int color;
        if (sat >= 80)      color = 0xFF4CAF50; // 缁?        else
        if (sat >= 60) color = 0xFF8BC34A; // 娴呯豢
        else if (sat >= 30) color = 0xFFFFC107; // 榛?        else                color = 0xFFF44336; // 绾?        g.fill(x, y, x + fill, y + barHeight, color);
        // 杈规
        g.fill(x, y, x + barWidth, y + 1, 0xAAFFFFFF);
        g.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, 0xAAFFFFFF);
        g.fill(x, y, x + 1, y + barHeight, 0xAAFFFFFF);
        g.fill(x + barWidth - 1, y, x + barWidth, y + barHeight, 0xAAFFFFFF);
        // 鐧惧垎姣旀暟瀛?        g.drawString(font, "§7婊℃剰搴︼細§f" + sat + "%", x, y + barHeight + 2, 0xFFFFFF);

        // 7. 鍒嗛殧绾?        y += 20;
        g.fill(x, y, x + GUI_WIDTH - pad * 2, y + 1, 0x334A5568);

        // 8. 宸ヤ綔鏃舵
        y += 10;
        g.drawString(font, "§7宸ヤ綔鏃堕棿锛毬06:00 鈥?18:00", x, y, 0xFFFFFF);

        // 8. 鍏抽棴鎻愮ず
        g.drawString(font, "§7鎸?§eESC §7鍏抽棴", x, cy + GUI_HEIGHT - 30, 0x888888);
    }

    /**
     * 浠?textureId 鑾峰彇绾圭悊璺緞锛堜笌 CitizenRenderer 淇濇寔涓€鑷达級
     */
    private ResourceLocation getCitizenTexture() {
        int id = citizen.getTextureId() % 16;
        String[] names = {
                "villager", "farmer", "fisherman", "fletcher",
                "butcher", "shepherd", "leatherworker", "armorer",
                "weaponsmith", "toolsmith", "librarian", "cartographer",
                "cleric", "mason", "nitwit", "unemployed"
        };
        return new ResourceLocation("minecraft", "textures/entity/villager/" + names[id] + ".png");
    }

    // ══ 榧犳爣鐐瑰嚮 ═════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cx = (width - GUI_WIDTH) / 2;
            int cy = (height - GUI_HEIGHT) / 2;
            int bx = cx + GUI_WIDTH - 22;
            int by = cy + 6;
            if (mx >= bx && mx < bx + 16 && my >= by && my < by + 16) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
