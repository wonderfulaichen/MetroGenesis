package com.metrogenesis.domumornamentum.block;

import com.metrogenesis.domumornamentum.block.interfaces.IDOBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Creates an instance of the abstract door block.
 * @param <B> the type.
 */
public abstract class AbstractBlockDoor<B extends AbstractBlockDoor<B>> extends DoorBlock implements IDOBlock<B>
{
    /**
     * Constructor of the door block.
     * @param properties the doors properties.
     */
    public AbstractBlockDoor(final Properties properties)
    {
        super(properties, BlockSetType.OAK);
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return getRegistryName(this);
    }
}
