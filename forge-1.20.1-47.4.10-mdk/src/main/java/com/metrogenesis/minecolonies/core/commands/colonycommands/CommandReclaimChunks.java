package com.metrogenesis.minecolonies.core.commands.colonycommands;

import com.metrogenesis.minecolonies.api.colony.IChunkmanagerCapability;
import com.metrogenesis.minecolonies.api.colony.IColony;
import com.metrogenesis.minecolonies.api.util.Log;
import com.metrogenesis.minecolonies.api.util.MessageUtils;
import com.metrogenesis.minecolonies.api.util.constant.translation.CommandTranslationConstants;
import com.metrogenesis.minecolonies.core.commands.arguments.ColonyIdArgument;
import com.metrogenesis.minecolonies.core.commands.commandTypes.IMCCommand;
import com.metrogenesis.minecolonies.core.commands.commandTypes.IMCOPCommand;
import com.metrogenesis.minecolonies.core.util.BackUpHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import static com.metrogenesis.minecolonies.api.util.constant.ColonyManagerConstants.UNABLE_TO_FIND_WORLD_CAP_TEXT;
import static com.metrogenesis.minecolonies.api.util.constant.Constants.CHUNKS_TO_CLAIM_THRESHOLD;
import static com.metrogenesis.minecolonies.core.MineColonies.CHUNK_STORAGE_UPDATE_CAP;
import static com.metrogenesis.minecolonies.core.commands.CommandArgumentNames.COLONYID_ARG;

public class CommandReclaimChunks implements IMCOPCommand
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

        if (!(sender instanceof Player))
        {
            return 0;
        }

        final IColony colony = ColonyIdArgument.getColony(context, COLONYID_ARG);

        final IChunkmanagerCapability chunkManager = sender.level.getCapability(CHUNK_STORAGE_UPDATE_CAP, null).resolve().orElse(null);
        if (chunkManager == null)
        {
            Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT, new Exception());
            return 0;
        }

        if (chunkManager.getAllChunkStorages().size() > CHUNKS_TO_CLAIM_THRESHOLD)
        {
            MessageUtils.format(CommandTranslationConstants.COMMAND_CLAIM_MAX_CHUNKS).sendTo((Player) sender);
            return 0;
        }

        BackUpHelper.reclaimChunks(colony);
        MessageUtils.format(CommandTranslationConstants.COMMAND_CLAIM_SUCCESS).sendTo((Player) sender);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "reclaimchunks";
    }

    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
          .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id()).executes(this::checkPreConditionAndExecute));
    }
}
