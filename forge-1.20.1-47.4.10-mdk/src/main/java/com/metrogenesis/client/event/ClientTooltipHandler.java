package com.metrogenesis.client.event;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.core.economy.CValueRegistry;
import com.metrogenesis.core.economy.EconomyEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * C-Value Tooltip — 显示基准价值 + 当前市价。
 * <p>
 * 第一行：基准价值（配方推算/预设值，稳定可预期）
 * 第二行：当前市价（经济引擎供需波动价，仅引擎在线时显示）
 */
@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientTooltipHandler {

    private ClientTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        final ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;

        final long base = CValueRegistry.getBaseValueOrZero(itemId);
        if (base <= 0) return;

        // 第一行：基准 C-Value
        event.getToolTip().add(Component.literal("§e[C-Value] §7" + formatPrice(base) + " §7C"));

        // 第二行：当前市价（引擎在线且价格不同时显示）
        final EconomyEngine engine = EconomyEngine.getInstance();
        if (engine != null) {
            double market = engine.getCurrentPrice(itemId);
            if (market > 0 && Math.abs(market - base) > 0.5) {
                String arrow = market > base ? "↑" : "↓";
                event.getToolTip().add(Component.literal(
                    "  §7市场: §f" + formatPrice(market) + " §7C " + arrow));
            }
        }
    }

    private static String formatPrice(double v) {
        if (v >= 1_000_000) return String.format("%.2fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000);
        if (v == (long) v) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}
