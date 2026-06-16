package com.metrogenesis.core.economy;

import net.minecraft.nbt.CompoundTag;

/**
 * Immutable value object representing a C-Value amount.
 * C-Value is the universal value metric in PolisCraft economy.
 * All economic transactions (wages, prices, taxes, trade) use C-Value as unit.
 *
 * Uses long (fixed-point with implicit 2 decimal places) for exact accounting.
 * multiply/divide apply rounding.
 */
public record CValue(long amount)
{
    public static final CValue ZERO = new CValue(0);

    // --- Double convenience — for intermediate price calculations ---

    /** Create a CValue from a double price (e.g., from EconomyEngine). */
    public static CValue of(double value)
    {
        return new CValue(Math.round(value));
    }

    /** Get the amount as double for intermediate calculations. */
    public double toDouble() { return (double) amount; }

    // --- Arithmetic ---

    public CValue add(CValue other)
    {
        return new CValue(this.amount + other.amount);
    }

    public CValue subtract(CValue other)
    {
        return new CValue(this.amount - other.amount);
    }

    public CValue multiply(long factor)
    {
        return new CValue(this.amount * factor);
    }

    public CValue divide(long divisor)
    {
        if (divisor == 0) return ZERO;
        return new CValue(this.amount / divisor);
    }

    public boolean isNegative() { return amount < 0; }
    public boolean isZero()     { return amount == 0; }
    public boolean isPositive() { return amount > 0; }

    // --- NBT persistence ---

    public CompoundTag toNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("value", amount);
        return tag;
    }

    public static CValue fromNBT(CompoundTag tag)
    {
        return new CValue(tag.getLong("value"));
    }

    // --- Formatting ---

    public String toDisplayString()
    {
        if (amount >= 1_000_000)
            return String.format("%.1fM c", amount / 1_000_000.0);
        if (amount >= 1_000)
            return String.format("%.1fK c", amount / 1_000.0);
        return amount + " c";
    }

    @Override
    public String toString()
    {
        return "CValue[" + amount + "]";
    }
}
