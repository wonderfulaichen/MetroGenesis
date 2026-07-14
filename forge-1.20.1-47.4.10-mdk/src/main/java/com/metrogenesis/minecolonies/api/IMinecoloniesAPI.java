package com.metrogenesis.minecolonies.api;

import com.metrogenesis.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.metrogenesis.minecolonies.api.colony.ICitizenDataManager;
import com.metrogenesis.minecolonies.api.colony.IColonyManager;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.metrogenesis.minecolonies.api.colony.buildings.registry.IBuildingDataManager;
import com.metrogenesis.minecolonies.api.colony.colonyEvents.registry.ColonyEventDescriptionTypeRegistryEntry;
import com.metrogenesis.minecolonies.api.colony.colonyEvents.registry.ColonyEventTypeRegistryEntry;
import com.metrogenesis.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries.BuildingExtensionEntry;
import com.metrogenesis.minecolonies.api.colony.guardtype.GuardType;
import com.metrogenesis.minecolonies.api.colony.guardtype.registry.IGuardTypeDataManager;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.registry.IInteractionResponseHandlerDataManager;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.registry.InteractionResponseHandlerEntry;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.IJobDataManager;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.JobEntry;
import com.metrogenesis.minecolonies.api.compatibility.IFurnaceRecipes;
import com.metrogenesis.minecolonies.api.configuration.Configuration;
import com.metrogenesis.minecolonies.api.crafting.registry.CraftingType;
import com.metrogenesis.minecolonies.api.crafting.registry.RecipeTypeEntry;
import com.metrogenesis.minecolonies.api.entity.mobs.registry.IMobAIRegistry;
import com.metrogenesis.minecolonies.api.entity.citizen.happiness.HappinessRegistry;
import com.metrogenesis.minecolonies.api.entity.pathfinding.registry.IPathNavigateRegistry;
import com.metrogenesis.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.metrogenesis.minecolonies.api.eventbus.EventBus;
import com.metrogenesis.minecolonies.api.quests.registries.QuestRegistries;
import com.metrogenesis.minecolonies.api.research.IGlobalResearchTree;
import com.metrogenesis.minecolonies.api.research.ModResearchCosts.ResearchCostEntry;
import com.metrogenesis.minecolonies.api.research.ModResearchEffects;
import com.metrogenesis.minecolonies.api.research.ModResearchRequirements;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;

public interface IMinecoloniesAPI
{

    static IMinecoloniesAPI getInstance()
    {
        return MinecoloniesAPIProxy.getInstance();
    }

    IColonyManager getColonyManager();

    ICitizenDataManager getCitizenDataManager();

    IMobAIRegistry getMobAIRegistry();

    IPathNavigateRegistry getPathNavigateRegistry();

    IBuildingDataManager getBuildingDataManager();

    IForgeRegistry<BuildingEntry> getBuildingRegistry();

    IForgeRegistry<BuildingExtensionEntry> getBuildingExtensionRegistry();

    IJobDataManager getJobDataManager();

    IForgeRegistry<JobEntry> getJobRegistry();

    IForgeRegistry<InteractionResponseHandlerEntry> getInteractionResponseHandlerRegistry();

    IGuardTypeDataManager getGuardTypeDataManager();

    IForgeRegistry<GuardType> getGuardTypeRegistry();

    IModelTypeRegistry getModelTypeRegistry();

    Configuration getConfig();

    IFurnaceRecipes getFurnaceRecipes();

    IInteractionResponseHandlerDataManager getInteractionResponseHandlerDataManager();

    IGlobalResearchTree getGlobalResearchTree();

    IForgeRegistry<ModResearchRequirements.ResearchRequirementEntry> getResearchRequirementRegistry();

    IForgeRegistry<ModResearchEffects.ResearchEffectEntry> getResearchEffectRegistry();

    IForgeRegistry<ResearchCostEntry> getResearchCostRegistry();

    IForgeRegistry<ColonyEventTypeRegistryEntry> getColonyEventRegistry();

    IForgeRegistry<ColonyEventDescriptionTypeRegistryEntry> getColonyEventDescriptionRegistry();

    IForgeRegistry<RecipeTypeEntry> getRecipeTypeRegistry();

    IForgeRegistry<CraftingType> getCraftingTypeRegistry();

    IForgeRegistry<QuestRegistries.RewardEntry> getQuestRewardRegistry();

    IForgeRegistry<QuestRegistries.ObjectiveEntry> getQuestObjectiveRegistry();

    IForgeRegistry<QuestRegistries.TriggerEntry> getQuestTriggerRegistry();

    IForgeRegistry<QuestRegistries.DialogueAnswerEntry> getQuestDialogueAnswerRegistry();

    IForgeRegistry<HappinessRegistry.HappinessFactorTypeEntry> getHappinessTypeRegistry();

    IForgeRegistry<HappinessRegistry.HappinessFunctionEntry> getHappinessFunctionRegistry();

    void onRegistryNewRegistry(NewRegistryEvent event);

    IForgeRegistry<EquipmentTypeEntry> getEquipmentTypeRegistry();

    EventBus getEventBus();
}
