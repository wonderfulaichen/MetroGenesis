package com.metrogenesis.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;

/**
 * 宸插畬鎴愬唴瀹规€昏鐣岄潰 鈥?鍦?GUI 涓柊澧炵殑鏍囩椤? * <p>
 * 鎸夌被鍒睍绀?MetroGenesis 鍚勬ā鍧楃殑瀹屾垚鐘舵€侊紝
 * 璁╃帺瀹?寮€鍙戣€呬竴鐪肩湅娓呭摢浜涘姛鑳藉凡绋冲畾銆佸摢浜涜繕鏈夊凡鐭ラ棶棰樸€? */
public class FeaturesOverviewScreen extends Screen
{
    // ══ 甯冨眬甯搁噺 ═════════════════════════════════
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private static final int TITLE_HEIGHT = 20;
    private static final int CATEGORY_GAP = 4;
    private static final int ITEM_HEIGHT = 16;
    private static final int BOTTOM_BAR = 30;

    // ══ 棰滆壊 ═════════════════════════════════════
    private static final int C_BG = MGStyles.C_BG;
    private static final int C_BORDER = MGStyles.C_BRASS;
    private static final int C_TITLE = MGStyles.C_TEXT_BRASS;
    private static final int C_TEXT = MGStyles.C_TEXT;
    private static final int C_DESC = MGStyles.C_TEXT_2ND;
    private static final int C_LINE = 0xFF333333;

    // ══ 鍒楄〃鏁版嵁 ═════════════════════════════════
    private final List<FeatureEntry> flatEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private int totalContentHeight = 0;

    // ══ 鏋勯€?═════════════════════════════════════

    public FeaturesOverviewScreen()
    {
        super(Component.translatable("MetroGenesis.gui.overview.title"));
        buildFlatList();
    }

    // ══ 鏁版嵁鏋勫缓 ═════════════════════════════════

    /**
     * 灏嗗垎绫荤壒寰佹爲灞曞紑涓烘墎骞冲垪琛紙鍚?category 鍒嗛殧琛岋級
     */
    private void buildFlatList()
    {
        flatEntries.clear();
        totalContentHeight = 0;

        for (FeatureCategory cat : FeatureRegistry.CATEGORIES)
        {
            // 鍒嗙被鏍囬琛?鈥?鏄剧ず "已完成x / 鍏?y"
            String countStr = "\u5DF2\u5B8C\u6210" + cat.count + " / " + cat.items.size();
            flatEntries.add(new FeatureEntry(cat.name, true, cat.count, MGStyles.C_TEXT_BRASS, countStr));
            totalContentHeight += ITEM_HEIGHT + 2;

            for (FeatureItem item : cat.items)
            {
                flatEntries.add(new FeatureEntry("  " + item.label, false, item.status, item.statusColor(), ""));
                totalContentHeight += ITEM_HEIGHT + 1;
            }

            totalContentHeight += CATEGORY_GAP;
        }
    }

    private int listAreaTop()
    {
        int guiLeft = (width - GUI_WIDTH) / 2;
        int guiTop = (height - GUI_HEIGHT) / 2;
        return guiTop + TITLE_HEIGHT + 4;
    }

    private int listAreaBottom()
    {
        int guiTop = (height - GUI_HEIGHT) / 2;
        return guiTop + GUI_HEIGHT - BOTTOM_BAR;
    }

    private int listHeight()
    {
        return listAreaBottom() - listAreaTop();
    }

    // ══ 娓叉煋 ═════════════════════════════════════

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

        // 鍒嗛殧绾?
        int lineY = guiTop + TITLE_HEIGHT;
        g.fill(guiLeft + 8, lineY, guiLeft + GUI_WIDTH - 8, lineY + 1, C_BORDER);

        // 鍒楄〃鍖哄煙
        int listTop = listAreaTop();
        int listBottom = listAreaBottom();
        int listH = listHeight();

        enableScissor(guiLeft, listTop, GUI_WIDTH, listH);

        int itemY = listTop - scrollOffset;
        for (FeatureEntry entry : flatEntries)
        {
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom)
            {
                itemY += entry.isCategory ? (ITEM_HEIGHT + 2) : (ITEM_HEIGHT + 1);
                continue;
            }

            if (entry.isCategory)
            {
                // 鍒嗙被鏍囬琛?                g.drawString(font, entry.text, guiLeft + 10, itemY + 2, entry.color, false);
                // 瀹屾垚璁℃暟锛堝凡瀹屾垚鏁?鎬绘暟锛?
                String countStr = entry.countStr;
                int cw = font.width(countStr);
                g.drawString(font, countStr, guiLeft + GUI_WIDTH - 10 - cw, itemY + 2, C_DESC, false);
                itemY += ITEM_HEIGHT + 2;
            }
            else
            {
                // 鏅€氭潯鐩?
                int statusColor = entry.color;
                String statusIcon = statusIcon(entry.status);
                String statusLabel = statusLabel(entry.status);

                // 鐘舵€佸浘鏍?                g.drawString(font, statusIcon, guiLeft + 10, itemY, statusColor, false);

                // 鎻忚堪鏂囨湰
                g.drawString(font, entry.text, guiLeft + 24, itemY, C_TEXT, false);

                // 鐘舵€佹爣绛撅紙鍙充晶锛?
                String sl = statusLabel;
                int sw = font.width(sl);
                g.drawString(font, sl, guiLeft + GUI_WIDTH - 10 - sw, itemY, statusColor, false);

                // 琛屽垎闅旂嚎
                g.fill(guiLeft + 10, itemY + ITEM_HEIGHT, guiLeft + GUI_WIDTH - 10, itemY + ITEM_HEIGHT + 1, C_LINE);

                itemY += ITEM_HEIGHT + 1;
            }
        }

        disableScissor();

        // 婊氬姩鏉?
        if (totalContentHeight > listH)
        {
            int sbY = listTop + (listH * scrollOffset / (totalContentHeight - listH + 1));
            int sbH = Math.max(8, listH * listH / (totalContentHeight + 1));
            g.fill(guiLeft + GUI_WIDTH - 4, listTop, guiLeft + GUI_WIDTH - 2, listBottom, 0x66C89B3C);
            g.fill(guiLeft + GUI_WIDTH - 4, sbY, guiLeft + GUI_WIDTH - 2, sbY + sbH, 0xAAC89B3C);
        }

        // 搴曢儴鎻愮ず
        String hint = Component.translatable("MetroGenesis.gui.overview.hint").getString();
        int hintY = guiTop + GUI_HEIGHT - BOTTOM_BAR + 10;
        int hw = font.width(hint);
        g.drawString(font, hint, guiLeft + (GUI_WIDTH - hw) / 2, hintY, C_DESC, false);

        super.render(g, mx, my, partialTick);
    }

    // ══ 榧犳爣婊氬姩 ═════════════════════════════════

    @Override
    public boolean mouseScrolled(double mx, double my, double delta)
    {
        int listH = listHeight();
        if (totalContentHeight > listH)
        {
            scrollOffset = Math.max(0, Math.min(totalContentHeight - listH,
                scrollOffset - (int) (delta * ITEM_HEIGHT * 2)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        // 浠讳綍鐐瑰嚮鍏抽棴锛堣交瑙﹂€€鍑猴級
        onClose();
        return true;
    }

    // ══ 杈呭姪 ═════════════════════════════════════

    private static String statusIcon(int status)
    {
        return switch (status)
        {
            case 0 -> "[\u2714]";  // 鉁?瀹屾垚
            case 1 -> "[\u26A0]";  // 鈿?閮ㄥ垎瀹屾垚
            case 2 -> "[\u2718]";  // 鉂?鏈慨澶?
            default -> "[?]";
        };
    }

    private static String statusLabel(int status)
    {
        return switch (status)
        {
            case 0 -> Language.getInstance().getOrDefault("gui.metrogenesis.overview.status.done");
            case 1 -> Language.getInstance().getOrDefault("gui.metrogenesis.overview.status.progress");
            case 2 -> Language.getInstance().getOrDefault("gui.metrogenesis.overview.status.unfixed");
            default -> Language.getInstance().getOrDefault("gui.metrogenesis.overview.status.unknown");
        };
    }

    private void renderBorder(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 1, y + h, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER);
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

    // ══ 鍐呴儴鏁版嵁缁撴瀯 ═════════════════════════════

    private static final class FeatureEntry
    {
        final String text;
        final boolean isCategory;
        final int status;
        final int color;
        final String countStr;  // 鍒嗙被琛屾樉绀?"x/y 已完成

        FeatureEntry(String text, boolean isCategory, int status, int color, String countStr)
        {
            this.text = text;
            this.isCategory = isCategory;
            this.status = status;
            this.color = color;
            this.countStr = countStr;
        }
    }

    // ══ 鐗瑰緛娉ㄥ唽琛?═════════════════════════════

    /**
     * 鎸夊垎绫荤粍缁囩壒寰佹潯鐩?     * <p>
     * status 鍚箟锛?     *   0 = 宸插畬鎴愶紙绋冲畾鍙敤锛?     *   1 = 閮ㄥ垎瀹屾垚 / 寮€鍙戜腑
     *   2 = 鏈夊凡鐭ラ棶棰?/ 鏈慨澶?     */
    public static final class FeatureItem
    {
        public final String label;
        public final int status;

        public FeatureItem(String label, int status)
        {
            this.label = label;
            this.status = status;
        }

        public int statusColor()
        {
            return switch (status)
            {
                case 0 -> 0xFF6AB06A;               // 已完成 绿
                case 1 -> MGStyles.C_ACCENT;        // 进行中 工业橙
                case 2 -> MGStyles.C_DANGER;        // 未修复 砖红
                default -> MGStyles.C_TEXT_2ND;
            };
        }
    }

    public static final class FeatureCategory
    {
        public final String name;
        public final List<FeatureItem> items;
        public final int count;

        public FeatureCategory(String name, List<FeatureItem> items)
        {
            this.name = name;
            this.items = items;
            this.count = (int) items.stream().filter(i -> i.status == 0).count();
        }
    }

    /**
     * \u6240\u6709\u5206\u7C7B\u53CA\u5176\u7279\u5F81\u6761\u76EE
     * <p>
     * \u6DFB\u52A0\u65B0\u529F\u80FD\u540E\u53EA\u9700\u5728\u6B64\u8FFD\u52A0\u6761\u76EE\uFF0CUI \u81EA\u52A8\u66F4\u65B0\u3002
     */
    public static final class FeatureRegistry
    {
        public static final List<FeatureCategory> CATEGORIES = List.of(
            new FeatureCategory("\u57FA\u7840\u6846\u67B6", List.of(
                new FeatureItem("Mod \u6CE8\u518C\u7CFB\u7EDF\uFF08\u65B9\u5757/\u7269\u54C1/\u83DC\u5355/\u5B9E\u4F53\uFF09", 0),
                new FeatureItem("\u7F51\u7EDC\u901A\u4FE1 (NetworkHandler + \u6D88\u606F\u5305)", 0),
                new FeatureItem("\u521B\u9020\u6A21\u5F0F\u6807\u7B7E\u9875", 0),
                new FeatureItem("\u5168\u606F\u56FE\u6A21\u5757 (MetroGenesisHologramMod)", 0)
            )),
            new FeatureCategory("GUI \u754C\u9762\u7CFB\u7EDF", List.of(
                new FeatureItem("BuildToolScreen — \u5EFA\u7B51\u9009\u62E9", 0),
                new FeatureItem("TownHallScreen — \u5E02\u653F\u5385\u7BA1\u7406", 0),
                new FeatureItem("CitizenInteractionScreen — \u5E02\u6C11\u4EA4\u4E92\u5F39\u7A97", 0),
                new FeatureItem("CitizenDetailScreen — \u5E02\u6C11\u8BE6\u60C5", 0),
                new FeatureItem("ConstructionMarkerScreen — \u65BD\u5DE5\u6807\u8BB0", 0),
                new FeatureItem("FeaturesOverviewScreen — \u5F53\u524D\u9875 (\u2714)", 0)
            )),
            new FeatureCategory("\u5EFA\u7B51\u7CFB\u7EDF", List.of(
                new FeatureItem("BuildingType \u6CE8\u518C (5 \u79CD\u5EFA\u7B51)", 0),
                new FeatureItem("TownHallBlock — \u5E02\u653F\u5385", 0),
                new FeatureItem("FarmFacilityBlock — \u519C\u573A", 0),
                new FeatureItem("House / Workshop / Warehouse", 0),
                new FeatureItem("ConstructionMarkerBlock + \u65B9\u5757\u5B9E\u4F53", 0),
                new FeatureItem("BlockConstructionTape — \u65BD\u5DE5\u56F4\u680F", 0),
                new FeatureItem("\u5EFA\u7B51\u653E\u7F6E\u903B\u8F91 (BuildToolPlacementMessage)", 0)
            )),
            new FeatureCategory("\u5E02\u6C11\u7CFB\u7EDF", List.of(
                new FeatureItem("MetroGenesisCitizen \u5B9E\u4F53 + \u5C5E\u6027", 0),
                new FeatureItem("\u5DE5\u4F5C\u6CE8\u518C (ModJobs: Farmer/Builder/Merchant)", 0),
                new FeatureItem("\u6EE1\u610F\u5EA6\u7CFB\u7EDF (SatisfactionSystem)", 0),
                new FeatureItem("\u7ECF\u6D4E\u7CFB\u7EDF (C-Value / \u94B1\u5305)", 0),
                new FeatureItem("\u5E02\u6C11\u547D\u540D (CitizenNameListener)", 0),
                new FeatureItem("\u5E02\u6C11\u6E32\u67D3 (CitizenRenderer)", 0)
            )),
            new FeatureCategory("\u5E02\u653F\u5385\u7BA1\u7406", List.of(
                new FeatureItem("\u62DB\u52DF\u7CFB\u7EDF (\u8017\u8D39 C-Value)", 0),
                new FeatureItem("\u804C\u4F4D\u53D8\u66F4 (TownHallMenu)", 0),
                new FeatureItem("\u56FD\u5E93\u663E\u793A", 0)
            )),
            new FeatureCategory("\u5DF2\u77E5\u95EE\u9898 (\u9700\u914D\u5408 MineColonies \u5E95\u5EA7)", List.of(
                new FeatureItem("Structurize BlockUI NPE (updateRotationState)", 2),
                new FeatureItem("framework-forge \u6620\u5C04\u9519 (NoSuchMethodError)", 2),
                new FeatureItem("BlockUI Pane \u5DE5\u5382\u6CE8\u518C (layout/ToggleButton)", 1),
                new FeatureItem("\u5F85\u63A5\u5165 MineColonies \u6B96\u6C11\u5730\u6838\u5FC3", 1)
            ))
        );
    }
}
