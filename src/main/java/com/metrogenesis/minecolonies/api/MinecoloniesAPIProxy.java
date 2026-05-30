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

public final class MinecoloniesAPIProxy implements IMinecoloniesAPI
{
    private static final MinecoloniesAPIProxy ourInstance = new MinecoloniesAPIProxy();

    private IMinecoloniesAPI apiInstance;

    public static MinecoloniesAPIProxy getInstance()
    {
        return ourInstance;
    }

    private MinecoloniesAPIProxy()
    {
    }

    public void setApiInstance(final IMinecoloniesAPI apiInstance)
    {
        this.apiInstance = apiInstance;
    }

    @Override
    public IColonyManager getColonyManager()
    {
        return apiInstance.getColonyManager();
    }

    @Override
    public ICitizenDataManager getCitizenDataManager()
    {
        return apiInstance.getCitizenDataManager();
    }

    @Override
    public IMobAIRegistry getMobAIRegistry()
    {
        return apiInstance.getMobAIRegistry();
    }

    @Override
    public IPathNavigateRegistry getPathNavigateRegistry()
    {
        return apiInstance.getPathNavigateRegistry();
    }

    @Override
    public IBuildingDataManager getBuildingDataManager()
    {
        return apiInstance.getBuildingDataManager();
    }

    @Override
    public IForgeRegistry<BuildingEntry> getBuildingRegistry()
    {
        return apiInstance.getBuildingRegistry();
    }

    @Override
    public IForgeRegistry<BuildingExtensionEntry> getBuildingExtensionRegistry()
    {
        return apiInstance.getBuildingExtensionRegistry();
    }

    @Override
    public IJobDataManager getJobDataManager()
    {
        return apiInstance.getJobDataManager();
    }

    @Override
    public IForgeRegistry<JobEntry> getJobRegistry()
    {
        return apiInstance.getJobRegistry();
    }

    @Override
    public IForgeRegistry<InteractionResponseHandlerEntry> getInteractionResponseHandlerRegistry()
    {
        return apiInstance.getInteractionResponseHandlerRegistry();
    }

    @Override
    public IGuardTypeDataManager getGuardTypeDataManager()
    {
        return apiInstance.getGuardTypeDataManager();
    }

    @Override
    public IForgeRegistry<GuardType> getGuardTypeRegistry()
    {
        return apiInstance.getGuardTypeRegistry();
    }

    @Override
    public IModelTypeRegistry getModelTypeRegistry()
    {
        return apiInstance.getModelTypeRegistry();
    }

    @Override
    public Configuration getConfig()
    {
        return apiInstance.getConfig();
    }

    @Override
    public IFurnaceRecipes getFurnaceRecipes()
    {
        return apiInstance.getFurnaceRecipes();
    }

    @Override
    public IInteractionResponseHandlerDataManager getInteractionResponseHandlerDataManager()
    {
        return apiInstance.getInteractionResponseHandlerDataManager();
    }

    @Override
    public IGlobalResearchTree getGlobalResearchTree()
    {
        return apiInstance.getGlobalResearchTree();
    }

    @Override
    public IForgeRegistry<ModResearchRequirements.ResearchRequirementEntry> getResearchRequirementRegistry() {return apiInstance.getResearchRequirementRegistry();}

    @Override
    public IForgeRegistry<ModResearchEffects.ResearchEffectEntry> getResearchEffectRegistry() {return apiInstance.getResearchEffectRegistry();}

    @Override
    public IForgeRegistry<ResearchCostEntry> getResearchCostRegistry()
    {
        return apiInstance.getResearchCostRegistry();
    }

    @Override
    public IForgeRegistry<ColonyEventTypeRegistryEntry> getColonyEventRegistry()
    {
        return apiInstance.getColonyEventRegistry();
    }

    @Override
    public IForgeRegistry<ColonyEventDescriptionTypeRegistryEntry> getColonyEventDescriptionRegistry()
    {
        return apiInstance.getColonyEventDescriptionRegistry();
    }

    @Override
    public IForgeRegistry<RecipeTypeEntry> getRecipeTypeRegistry()
    {
        return apiInstance.getRecipeTypeRegistry();
    }

    @Override
    public IForgeRegistry<CraftingType> getCraftingTypeRegistry()
    {
        return apiInstance.getCraftingTypeRegistry();
    }

    @Override
    public IForgeRegistry<QuestRegistries.RewardEntry> getQuestRewardRegistry()
    {
        return apiInstance.getQuestRewardRegistry();
    }

    @Override
    public IForgeRegistry<QuestRegistries.ObjectiveEntry> getQuestObjectiveRegistry()
    {
        return apiInstance.getQuestObjectiveRegistry();
    }

    @Override
    public IForgeRegistry<QuestRegistries.TriggerEntry> getQuestTriggerRegistry()
    {
        return apiInstance.getQuestTriggerRegistry();
    }

    @Override
    public IForgeRegistry<QuestRegistries.DialogueAnswerEntry> getQuestDialogueAnswerRegistry()
    {
        return apiInstance.getQuestDialogueAnswerRegistry();
    }

    @Override
    public IForgeRegistry<HappinessRegistry.HappinessFactorTypeEntry> getHappinessTypeRegistry()
    {
        return apiInstance.getHappinessTypeRegistry();
    }

    @Override
    public IForgeRegistry<HappinessRegistry.HappinessFunctionEntry> getHappinessFunctionRegistry()
    {
        return apiInstance.getHappinessFunctionRegistry();
    }

    @Override
    public void onRegistryNewRegistry(final NewRegistryEvent event)
    {
        apiInstance.onRegistryNewRegistry(event);
    }

    @Override
    public IForgeRegistry<EquipmentTypeEntry> getEquipmentTypeRegistry()
    {
        return apiInstance.getEquipmentTypeRegistry();
    }

    @Override
    public EventBus getEventBus()
    {
        return apiInstance.getEventBus();
    }
}
