package com.metrogenesis.minecolonies.core.entity.mobs.raider.norsemen;

import com.metrogenesis.minecolonies.api.entity.mobs.vikings.AbstractEntityNorsemenRaider;
import com.metrogenesis.minecolonies.api.entity.mobs.vikings.IArcherNorsemenEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Archer norsemen entity.
 */
public class EntityNorsemenArcherRaider extends AbstractEntityNorsemenRaider implements IArcherNorsemenEntity
{

    /**
     * Constructor of the entity.
     *
     * @param worldIn world to construct it in.
     * @param type    the entity type.
     */
    public EntityNorsemenArcherRaider(final EntityType<? extends EntityNorsemenArcherRaider> type, final Level worldIn)
    {
        super(type, worldIn);
    }
}
