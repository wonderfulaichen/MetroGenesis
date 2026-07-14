package com.metrogenesis.command;

import com.metrogenesis.colony.ColonyState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * ① 开城命令（MVP 触发）：确立城市归属，使市民循环开始运转。
 *   /foundcity <名称> [风格包]
 * 后续将由市长书（MayorBook）UI 调用同一后端 {@link ColonyState#foundCity}。
 */
public final class FoundCityCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("foundcity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> execute(ctx, false))
                .then(Commands.argument("stylepack", StringArgumentType.string())
                    .executes(ctx -> execute(ctx, true)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean withStyle)
    {
        ServerLevel level = context.getSource().getLevel();
        ServerPlayer player = context.getSource().getPlayer();

        String name = StringArgumentType.getString(context, "name");
        String style = withStyle ? StringArgumentType.getString(context, "stylepack") : null;

        BlockPos core = (player != null)
                ? player.blockPosition()
                : level.getSharedSpawnPos();

        ColonyState.get(level).foundCity(name, style, core);

        String styleInfo = (style != null) ? "，风格包=" + style : "";
        context.getSource().sendSuccess(
                msg("§a城市『" + name + "』已创立" + styleInfo + "，市民正在迁入…"), false);
        context.getSource().sendSuccess(
                msg("§7用 /cvalue list 查看经济，观察市民实体在核心附近刷新。"), false);
        return 1;
    }

    /** Helper: wrap a string in a Component supplier for sendSuccess. */
    private static Supplier<Component> msg(String text)
    {
        return () -> Component.literal(text);
    }
}
