package com.metrogenesis.minecolonies.apiimp;

import com.metrogenesis.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.metrogenesis.minecolonies.core.client.render.modeltype.registry.ModelTypeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;

public class ClientMinecoloniesAPIImpl extends CommonMinecoloniesAPIImpl
{
    private final IModelTypeRegistry modelTypeRegistry = new ModelTypeRegistry();

    @Override
    public IModelTypeRegistry getModelTypeRegistry()
    {
        return modelTypeRegistry;
    }

    @Override
    public void onRegistryNewRegistry(final NewRegistryEvent event)
    {
        super.onRegistryNewRegistry(event);
    }
}
