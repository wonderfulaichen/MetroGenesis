package com.metrogenesis.gov;

import net.minecraft.network.chat.Component;

/**
 * 政策 record — 表示一项可启用的政府政策。
 * <p>
 * 每个部门可定义一组政策，玩家可在 GUI 中启用/禁用。
 * 启用政策每日消耗 C-Value（costPerDay），效果由各部门在 onDailyTick 中实现。
 *
 * @param id          唯一标识，如 "flat_tax_rate"
 * @param displayName 显示名称，如 "统一税率"
 * @param description 描述文本
 * @param enabled     当前是否启用
 * @param costPerDay  每日维护成本（C-Value）
 */
public record Policy(
        String id,
        Component displayName,
        Component description,
        boolean enabled,
        int costPerDay
) {}
