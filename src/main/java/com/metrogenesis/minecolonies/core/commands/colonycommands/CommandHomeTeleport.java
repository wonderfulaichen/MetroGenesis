package com.metrogenesis.minecolonies.core.commands.colonycommands;

import com.metrogenesis.minecolonies.api.util.MessageUtils;
import com.metrogenesis.minecolonies.core.MineColonies;
import com.metrogenesis.minecolonies.core.commands.commandTypes.IMCCommand;
import com.metrogenesis.minecolonies.core.util.TeleportHelper;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import static com.metrogenesis.minecolonies.api.util.constant.translation.CommandTranslationConstants.COMMAND_DISABLED_IN_CONFIG;

public class CommandHomeTeleport implements IMCCommand
{
    /**
     * What happens when the command is executed after preConditions are successful.
     *
     * @param context the context of the command execution
     */
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        final Entity sender = context.getSource().getEntity();

        if (!MineColonies.getConfig().getServer().canPlayerUseHomeTPCommand.get())
        {
            MessageUtils.format(COMMAND_DISABLED_IN_CONFIG).sendTo((Player) sender);
            return 0;
        }

        TeleportHelper.homeTeleport((ServerPlayer) sender);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "home";
    }
}
