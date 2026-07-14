package com.metrogenesis.minecolonies.apiimp.initializer;

import com.metrogenesis.minecolonies.api.enchants.ModEnchants;
import com.metrogenesis.minecolonies.core.enchants.RaiderDamageEnchant;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

import static com.metrogenesis.minecolonies.api.enchants.ModEnchants.ENCHANTMENTS;

/**
 * Enchants initializer
 */
public class ModEnchantInitializer
{
    static
    {
        ModEnchants.raiderDamage = ENCHANTMENTS.register("raider_damage_enchant", () -> new RaiderDamageEnchant(Enchantment.Rarity.VERY_RARE, new EquipmentSlot[] {EquipmentSlot.MAINHAND}));
    }
    /**
     * Init this.
     */
    public static void init()
    {
        // Class load.
    }
}
