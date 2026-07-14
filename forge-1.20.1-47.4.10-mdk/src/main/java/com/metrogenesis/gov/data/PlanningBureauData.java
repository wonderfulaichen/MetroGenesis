package com.metrogenesis.gov.data;

import com.metrogenesis.gov.DepartmentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规划局数据 — 科技树进度 + 已生效政策。
 */
public class PlanningBureauData extends DepartmentData {

    private String currentResearch = "";        // 当前研究方向（空 = 无研究中项目）
    private int researchProgress = 0;            // 研究进度（0-100）
    private final List<String> unlockedTechs = new ArrayList<>();   // 已解锁科技
    private final List<String> activePolicies = new ArrayList<>();  // 已生效政策

    // ══ NBT 键 ═════════════════════════════════════
    private static final String KEY_CURRENT = "currentResearch";
    private static final String KEY_PROGRESS = "researchProgress";
    private static final String KEY_UNLOCKED = "unlockedTechs";
    private static final String KEY_POLICIES = "activePolicies";

    @Override
    public void save(CompoundTag tag) {
        tag.putString(KEY_CURRENT, currentResearch);
        tag.putInt(KEY_PROGRESS, researchProgress);

        ListTag unlockedList = new ListTag();
        for (String tech : unlockedTechs) {
            unlockedList.add(StringTag.valueOf(tech));
        }
        tag.put(KEY_UNLOCKED, unlockedList);

        ListTag policiesList = new ListTag();
        for (String policy : activePolicies) {
            policiesList.add(StringTag.valueOf(policy));
        }
        tag.put(KEY_POLICIES, policiesList);
    }

    @Override
    public void load(CompoundTag tag) {
        this.currentResearch = tag.getString(KEY_CURRENT);
        this.researchProgress = tag.getInt(KEY_PROGRESS);

        this.unlockedTechs.clear();
        ListTag unlockedList = tag.getList(KEY_UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < unlockedList.size(); i++) {
            unlockedTechs.add(unlockedList.getString(i));
        }

        this.activePolicies.clear();
        ListTag policiesList = tag.getList(KEY_POLICIES, Tag.TAG_STRING);
        for (int i = 0; i < policiesList.size(); i++) {
            activePolicies.add(policiesList.getString(i));
        }
    }

    // ══ Getters / Setters ═══════════════════════════

    public String getCurrentResearch() { return currentResearch; }
    public void setCurrentResearch(String id) { this.currentResearch = id; }

    public int getResearchProgress() { return researchProgress; }
    public void setResearchProgress(int progress) {
        this.researchProgress = Math.min(100, Math.max(0, progress));
    }

    public List<String> getUnlockedTechs() { return Collections.unmodifiableList(unlockedTechs); }
    public void unlockTech(String techId) {
        if (!unlockedTechs.contains(techId)) {
            unlockedTechs.add(techId);
        }
    }

    public boolean isTechUnlocked(String techId) { return unlockedTechs.contains(techId); }

    public List<String> getActivePolicies() { return Collections.unmodifiableList(activePolicies); }

    /** 启用一项政策（添加到活跃列表） */
    public void enablePolicy(String policyId) {
        if (!activePolicies.contains(policyId)) {
            activePolicies.add(policyId);
        }
    }

    /** 停用一项政策（从活跃列表移除） */
    public void disablePolicy(String policyId) {
        activePolicies.remove(policyId);
    }

    public boolean isPolicyActive(String policyId) { return activePolicies.contains(policyId); }
}
