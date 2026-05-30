package com.metrogenesis.domumornamentum.block;

import com.metrogenesis.domumornamentum.block.interfaces.IDOBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public abstract class AbstractBlock<B extends AbstractBlock<B>> extends Block implements IDOBlock<B>
{
    public AbstractBlock(final Properties properties)
    {
        super(properties);
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return getRegistryName(this);
    }
}
