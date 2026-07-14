package com.metrogenesis.gov.data;

import com.metrogenesis.gov.DepartmentData;
import net.minecraft.nbt.CompoundTag;

/**
 * 人口局数据 — 移民/生育政策配置。
 */
public class PopulationBureauData extends DepartmentData {

    private boolean immigrationEnabled = true;
    private double immigrationAttractiveness = 1.0;  // 城市吸引力系数
    private int birthBonus = 0;                       // 生育奖金（C-Value/孩）
    private boolean skillFilterEnabled = false;

    // ══ NBT 键 ═════════════════════════════════════
    private static final String KEY_IMMIGRATION = "immigrationEnabled";
    private static final String KEY_ATTRACTIVENESS = "immigrationAttractiveness";
    private static final String KEY_BIRTH_BONUS = "birthBonus";
    private static final String KEY_SKILL_FILTER = "skillFilterEnabled";

    @Override
    public void save(CompoundTag tag) {
        tag.putBoolean(KEY_IMMIGRATION, immigrationEnabled);
        tag.putDouble(KEY_ATTRACTIVENESS, immigrationAttractiveness);
        tag.putInt(KEY_BIRTH_BONUS, birthBonus);
        tag.putBoolean(KEY_SKILL_FILTER, skillFilterEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        this.immigrationEnabled = tag.getBoolean(KEY_IMMIGRATION);
        this.immigrationAttractiveness = tag.getDouble(KEY_ATTRACTIVENESS);
        this.birthBonus = tag.getInt(KEY_BIRTH_BONUS);
        this.skillFilterEnabled = tag.getBoolean(KEY_SKILL_FILTER);
    }

    // ══ Getters / Setters ═══════════════════════════

    public boolean isImmigrationEnabled() { return immigrationEnabled; }
    public void setImmigrationEnabled(boolean enabled) { this.immigrationEnabled = enabled; }

    public double getImmigrationAttractiveness() { return immigrationAttractiveness; }
    public void setImmigrationAttractiveness(double v) { this.immigrationAttractiveness = v; }

    public int getBirthBonus() { return birthBonus; }
    public void setBirthBonus(int bonus) { this.birthBonus = bonus; }

    public boolean isSkillFilterEnabled() { return skillFilterEnabled; }
    public void setSkillFilterEnabled(boolean enabled) { this.skillFilterEnabled = enabled; }
}
