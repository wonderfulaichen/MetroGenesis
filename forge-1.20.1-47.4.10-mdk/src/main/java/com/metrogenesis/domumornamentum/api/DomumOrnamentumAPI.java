package com.metrogenesis.domumornamentum.api;

import com.metrogenesis.domumornamentum.IDomumOrnamentumApi;
import com.metrogenesis.domumornamentum.block.IMateriallyTexturedBlockManager;
import com.metrogenesis.domumornamentum.block.IModBlocks;
import com.metrogenesis.domumornamentum.block.MateriallyTexturedBlockManager;
import com.metrogenesis.domumornamentum.block.ModBlocks;

public class DomumOrnamentumAPI implements IDomumOrnamentumApi
{
    private static final DomumOrnamentumAPI INSTANCE = new DomumOrnamentumAPI();

    public static DomumOrnamentumAPI getInstance()
    {
        return INSTANCE;
    }

    @Override
    public IModBlocks getBlocks()
    {
        return ModBlocks.getInstance();
    }

    @Override
    public IMateriallyTexturedBlockManager getMateriallyTexturedBlockManager()
    {
        return MateriallyTexturedBlockManager.getInstance();
    }

    private DomumOrnamentumAPI()
    {
    }
}
