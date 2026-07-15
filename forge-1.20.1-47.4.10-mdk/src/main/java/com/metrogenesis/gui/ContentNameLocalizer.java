package com.metrogenesis.gui;

import net.minecraft.locale.Language;

/**
 * 内容名称本地化工具 — 将 MineColonies 风格包/目录/建筑的英文名通过 lang 文件翻译。
 * <p>
 * 这些名称来自文件系统，无法自动翻译。通过 lang 文件查询：
 * <ul>
 *   <li>风格包名：{@code pack.metrogenesis.<packname>} → 如 {@code pack.metrogenesis.desertoasis} = "沙漠绿洲"</li>
 *   <li>目录名：{@code dir.metrogenesis.<dirname>} → 如 {@code dir.metrogenesis.agriculture} = "农业"</li>
 *   <li>建筑名：{@code building.metrogenesis.<buildingname>} → 如 {@code building.metrogenesis.camp} = "营地"</li>
 * </ul>
 * 查找策略：lang 键 → 原文返回（无匹配时）。
 */
public final class ContentNameLocalizer {

    private ContentNameLocalizer() {}

    /**
     * 本地化风格包名。
     * 查找键：{@code pack.metrogenesis.<packname小写>}
     */
    public static String localizePackName(String name) {
        if (name == null || name.isEmpty()) return name;
        String key = "pack.metrogenesis." + name.toLowerCase().trim();
        String translated = Language.getInstance().getOrDefault(key);
        // 如果找不到翻译，getOrDefault 返回 key 本身，此时返回原文
        return translated.equals(key) ? name : translated;
    }

    /**
     * 本地化目录/分类名。
     * 查找键：{@code dir.metrogenesis.<dirname小写>}
     */
    public static String localizeDirectoryName(String name) {
        if (name == null || name.isEmpty()) return name;
        String key = "dir.metrogenesis." + name.toLowerCase().trim();
        String translated = Language.getInstance().getOrDefault(key);
        return translated.equals(key) ? name : translated;
    }

    /**
     * 本地化建筑名。
     * 查找键：{@code building.metrogenesis.<buildingname小写>}
     */
    public static String localizeBuildingName(String name) {
        if (name == null || name.isEmpty()) return name;
        String key = "building.metrogenesis." + name.toLowerCase().trim();
        String translated = Language.getInstance().getOrDefault(key);
        return translated.equals(key) ? name : translated;
    }

    /**
     * 通用本地化（按优先级依次尝试风格包 → 目录 → 建筑 → 原文）。
     */
    public static String localize(String name) {
        if (name == null || name.isEmpty()) return name;
        String lower = name.toLowerCase().trim();

        // 尝试风格包名
        String key = "pack.metrogenesis." + lower;
        String translated = Language.getInstance().getOrDefault(key);
        if (!translated.equals(key)) return translated;

        // 尝试目录名
        key = "dir.metrogenesis." + lower;
        translated = Language.getInstance().getOrDefault(key);
        if (!translated.equals(key)) return translated;

        // 尝试建筑名
        key = "building.metrogenesis." + lower;
        translated = Language.getInstance().getOrDefault(key);
        if (!translated.equals(key)) return translated;

        // 无翻译，返回原文
        return name;
    }
}
