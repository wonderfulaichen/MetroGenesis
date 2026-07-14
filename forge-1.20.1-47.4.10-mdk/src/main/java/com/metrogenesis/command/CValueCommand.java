package com.metrogenesis.command;

import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.core.economy.EconomyEngine;
import com.metrogenesis.core.economy.test.EconomySimulator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Debug commands for the C-Value economy system.
 *   /cvalue list            - Show top 20 items by C-Value
 *   /cvalue simulate <days> - Run automated economy simulation
 *   /cvalue debug <on/off>  - Toggle debug logging
 *   /cvalue reset           - Reset all economy data
 */
public final class CValueCommand
{
    private static boolean debugEnabled = false;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("cvalue")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("list")
                .executes(CValueCommand::listCValues))
            .then(Commands.literal("simulate")
                .then(Commands.argument("days", IntegerArgumentType.integer(1, 100))
                    .executes(CValueCommand::simulateEconomy)))
            .then(Commands.literal("debug")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(CValueCommand::toggleDebug)))
            .then(Commands.literal("reset")
                .executes(CValueCommand::resetData)));
    }

    private static int listCValues(CommandContext<CommandSourceStack> context)
    {
        ServerLevel level = context.getSource().getLevel();
        EconomyEngine engine = getEngine(context);
        if (engine == null) return 1;

        context.getSource().sendSuccess(msg("=== C-Value List (Top 20) ==="), false);
        context.getSource().sendSuccess(msg("Format: [item_id] prod | cons | price"), false);

        List<ResourceLocation> sorted = engine.getMarketData().getAllItems().stream()
            .sorted(Comparator.comparingDouble(id -> -engine.getMarketData().getSmoothedPrice(id)))
            .limit(20)
            .collect(Collectors.toList());

        if (sorted.isEmpty())
        {
            context.getSource().sendSuccess(msg("(no items tracked yet — start mining!)"), false);
        }
        else
        {
            for (ResourceLocation id : sorted)
            {
                long prod = engine.getMarketData().getCurrentProduction(id);
                long cons = engine.getMarketData().getCurrentConsumption(id);
                double price = engine.getMarketData().getSmoothedPrice(id);
                context.getSource().sendSuccess(msg(String.format("[%s]  prod: %d, cons: %d, price: %.2f",
                    id.toString(), prod, cons, price)), false);
            }
        }

        return 1;
    }

    private static int simulateEconomy(CommandContext<CommandSourceStack> context)
    {
        ServerLevel level = context.getSource().getLevel();
        ColonyState state = ColonyState.get(level);
        if (state == null || state.getEconomyEngine() == null)
        {
            context.getSource().sendFailure(Component.literal("城市尚未创立，请先使用 /foundcity <名称> [风格包] 开城。"));
            return 0;
        }

        int days = IntegerArgumentType.getInteger(context, "days");

        // Reset before simulation
        state.getEconomyEngine().getMarketData().resetAll();
        context.getSource().sendSuccess(msg("Simulating " + days + " days..."), false);

        EconomySimulator sim = new EconomySimulator(state, level);
        String report = sim.simulate(days);

        // Send report line by line
        for (String line : report.split("\n"))
        {
            context.getSource().sendSuccess(msg(line), false);
        }

        return 1;
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        debugEnabled = enabled;
        context.getSource().sendSuccess(msg("C-Value debug mode: " + (enabled ? "ON" : "OFF")), false);
        return 1;
    }

    private static int resetData(CommandContext<CommandSourceStack> context)
    {
        EconomyEngine engine = getEngine(context);
        if (engine == null) return 0;
        engine.getMarketData().resetAll();
        context.getSource().sendSuccess(msg("C-Value data reset"), false);
        return 1;
    }

    /** Helper: get economy engine or send failure. */
    private static EconomyEngine getEngine(CommandContext<CommandSourceStack> context)
    {
        ServerLevel level = context.getSource().getLevel();
        ColonyState state = ColonyState.get(level);
        if (state == null || state.getEconomyEngine() == null)
        {
            context.getSource().sendFailure(Component.literal("No economy data available"));
            return null;
        }
        return state.getEconomyEngine();
    }

    /** Helper: wrap a string in a Component supplier for sendSuccess. */
    private static Supplier<Component> msg(String text)
    {
        return () -> Component.literal(text);
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }
}
