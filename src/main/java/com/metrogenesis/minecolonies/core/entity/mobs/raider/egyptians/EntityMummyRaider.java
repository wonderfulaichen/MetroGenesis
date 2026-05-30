package com.metrogenesis.minecolonies.core.entity.mobs.raider.egyptians;

import com.metrogenesis.minecolonies.api.entity.mobs.egyptians.AbstractEntityEgyptianRaider;
import com.metrogenesis.minecolonies.api.entity.mobs.egyptians.IMeleeMummyEntity;
import com.metrogenesis.minecolonies.core.entity.pathfinding.navigation.MovementHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Mummy entity.
 */
public class EntityMummyRaider extends AbstractEntityEgyptianRaider implements IMeleeMummyEntity
{

    /**
     * Constructor of the entity.
     *
     * @param type    the entity type.
     * @param worldIn world to construct it in.
     */
    public EntityMummyRaider(final EntityType<? extends EntityMummyRaider> type, final Level worldIn)
    {
        super(type, worldIn);
        this.moveControl = new MovementHandler(this);
    }
}
