package com.metrogenesis.minecolonies.core.colony.jobs;

import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Stub: NetherWorker job kept for registration compatibility.
 * Full implementation depends on BuildingNetherWorker and expedition modules not yet ported.
 */
public class JobNetherWorker extends AbstractJobCrafter<EntityAIWorkNether, JobNetherWorker>
{
    private boolean citizenInNether = false;
    private Queue<ItemStack> craftedResults = new LinkedList<>();
    private Queue<ItemStack> processedResults = new LinkedList<>();

    private static final String TAG_IN_NETHER = "inNether";
    private static final String TAG_CRAFTED = "craftedResults";
    private static final String TAG_PROCESSED = "processedResults";

    public JobNetherWorker(ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag compound = super.serializeNBT();
        compound.putBoolean(TAG_IN_NETHER, citizenInNether);
        return compound;
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);
        citizenInNether = compound.getBoolean(TAG_IN_NETHER);
    }

    @Override
    public EntityAIWorkNether generateAI()
    {
        return new EntityAIWorkNether(this);
    }

    @Override
    public ResourceLocation getModel()
    {
        return new ResourceLocation("MetroGenesis", "nether_worker");
    }

    public void setInNether(boolean away) { citizenInNether = away; }
    public boolean isInNether() { return citizenInNether; }
    public Queue<ItemStack> getCraftedResults() { return craftedResults; }
    public Queue<ItemStack> getProcessedResults() { return processedResults; }

    public boolean addCraftedResultsList(Collection<ItemStack> newResults) { return craftedResults.addAll(newResults); }
    public boolean addProcessedResultsList(Collection<ItemStack> newResults) { return processedResults.addAll(newResults); }

    public boolean ignoresDamage(@NotNull final DamageSource damageSource) { return false; }

    @Override
    public double getDiseaseModifier() { return 1.0; }

    @Override
    public int getIdleSeverity(boolean isDemand) { return 0; }
}
