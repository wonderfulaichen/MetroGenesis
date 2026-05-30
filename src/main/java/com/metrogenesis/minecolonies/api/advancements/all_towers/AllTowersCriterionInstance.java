package com.metrogenesis.minecolonies.api.advancements.all_towers;

import com.metrogenesis.minecolonies.api.util.constant.Constants;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.resources.ResourceLocation;

/**
 * All towers criterion instance.
 */
public class AllTowersCriterionInstance extends AbstractCriterionTriggerInstance
{
    public AllTowersCriterionInstance()
    {
        super(new ResourceLocation(Constants.MOD_ID, Constants.CRITERION_ALL_TOWERS), ContextAwarePredicate.ANY);
    }
}
