package com.metrogenesis.domumornamentum.block;

import com.metrogenesis.domumornamentum.block.interfaces.IDOBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public abstract class AbstractBlockDirectional<B extends AbstractBlockDirectional<B>> extends HorizontalDirectionalBlock implements IDOBlock<B>
{
    public AbstractBlockDirectional(final Properties properties)
    {
        super(properties);
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return getRegistryName(this);
    }
}
