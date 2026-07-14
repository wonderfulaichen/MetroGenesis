package com.metrogenesis.domumornamentum.block;

import com.metrogenesis.domumornamentum.block.interfaces.IDOBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SlabBlock;

public abstract class AbstractBlockSlab<B extends AbstractBlockSlab<B>> extends SlabBlock implements IDOBlock<B>
{
    /**
     * Constructor of abstract class.
     * @param properties the input properties.
     */
    public AbstractBlockSlab(final Properties properties)
    {
        super(properties);
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return getRegistryName(this);
    }
}