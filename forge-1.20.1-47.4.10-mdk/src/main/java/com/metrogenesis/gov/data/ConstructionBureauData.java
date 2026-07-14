package com.metrogenesis.gov.data;

import com.metrogenesis.gov.DepartmentData;
import net.minecraft.nbt.CompoundTag;

/**
 * 建设局数据 — 施工队列配置。
 * <p>
 * Zone 系统已由其他模块实现，本类仅提供建设局的策略配置层。
 */
public class ConstructionBureauData extends DepartmentData {

    private int maxConstructionSites = 3;   // 最大同时建设数
    private boolean autoFillEnabled = false; // 区内自动填充（待 Phase 4）

    // ══ NBT 键 ═════════════════════════════════════
    private static final String KEY_MAX_SITES = "maxConstructionSites";
    private static final String KEY_AUTO_FILL = "autoFillEnabled";

    @Override
    public void save(CompoundTag tag) {
        tag.putInt(KEY_MAX_SITES, maxConstructionSites);
        tag.putBoolean(KEY_AUTO_FILL, autoFillEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        this.maxConstructionSites = tag.getInt(KEY_MAX_SITES);
        this.autoFillEnabled = tag.getBoolean(KEY_AUTO_FILL);
    }

    // ══ Getters / Setters ═══════════════════════════

    public int getMaxConstructionSites() { return maxConstructionSites; }
    public void setMaxConstructionSites(int n) { this.maxConstructionSites = Math.max(1, n); }

    public boolean isAutoFillEnabled() { return autoFillEnabled; }
    public void setAutoFillEnabled(boolean enabled) { this.autoFillEnabled = enabled; }
}
