package com.metrogenesis.minecolonies.core.quests.objectives;

import com.metrogenesis.minecolonies.api.quests.IQuestInstance;
import com.metrogenesis.minecolonies.api.quests.IObjectiveInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Specific objective for entity killing.
 */
public interface IKillEntityObjectiveTemplate
{
    /**
     * Callback for entity kill event
     *
     * @param progressData the objective data.
     * @param player the involved player.
     */
    void onEntityKill(IObjectiveInstance progressData, final IQuestInstance colonyQuest, final Player player);
}
