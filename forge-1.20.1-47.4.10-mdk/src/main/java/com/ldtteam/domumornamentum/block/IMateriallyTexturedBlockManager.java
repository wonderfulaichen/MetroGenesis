package com.metrogenesis.domumornamentum.block;

import com.metrogenesis.domumornamentum.IDomumOrnamentumApi;
import net.minecraft.world.item.ItemStack;

public interface IMateriallyTexturedBlockManager
{
    static IMateriallyTexturedBlockManager getInstance() {
        return IDomumOrnamentumApi.getInstance().getMateriallyTexturedBlockManager();
    }

    int getMaxTexturableComponentCount();

    boolean doesItemStackContainsMaterialForSlot(int slotIndex, ItemStack stack);

    boolean doesItemStackContainsMaterialForSlot(int slotIndex, ItemStack stack, ItemStack type);
}
