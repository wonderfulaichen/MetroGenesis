package com.metrogenesis.minecolonies.core.entity.mobs.raider.amazons;

import com.metrogenesis.minecolonies.api.entity.mobs.amazons.AbstractEntityAmazonRaider;
import com.metrogenesis.minecolonies.api.entity.mobs.amazons.IArcherAmazon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Archer amazon entity.
 */
public class EntityArcherAmazonRaider extends AbstractEntityAmazonRaider implements IArcherAmazon
{
    /**
     * Constructor of the entity.
     *
     * @param worldIn world to construct it in.
     * @param type    the entity type.
     */
    public EntityArcherAmazonRaider(final EntityType<? extends EntityArcherAmazonRaider> type, final Level worldIn)
    {
        super(type, worldIn);
    }

    @Override
    public double getAttackDelayModifier()
    {
        return 2;
    }
}
