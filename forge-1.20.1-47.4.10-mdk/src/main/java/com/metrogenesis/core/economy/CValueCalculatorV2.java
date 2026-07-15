package com.metrogenesis.core.economy;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * C-Value 配方推算器 v2 — 仿 ProjectE SimpleGraphMapper 架构。
 * <p>
 * 核心变化 vs v1：
 * <ul>
 *   <li>Conversion 数据结构：output + outnumber + 原料列表（支持多数量）</li>
 *   <li>双向索引：conversionsFor(output→配方) + usedIn(原料→配方)</li>
 *   <li>BigDecimal 分数运算：精确保持分数值，最后一起取整</li>
 *   <li>漏洞检测：配方价值 < 原料价值时报警</li>
 *   <li>setValueBefore → setValueAfter 两阶段</li>
 * </ul>
 */
public final class CValueCalculatorV2 {

    private static final int MAX_ITERATIONS = 30;

    /** 配方：原料/输出对照 */
    private record Conversion(String outputId, int outNumber, Map<String, Integer> ingredients) {}

    // 双向索引
    private static final Map<String, Set<Conversion>> conversionsFor = new HashMap<>();
    private static final Map<String, Set<Conversion>> usedIn = new HashMap<>();

    // 固定值
    private static final Map<String, Long> fixedBefore = new HashMap<>();
    private static final Map<String, Long> fixedAfter = new HashMap<>();

    private CValueCalculatorV2() {}

    public static void calculate(RecipeManager recipeManager, RegistryAccess registryAccess) {
        MetroGenesis.LOGGER.info("[CValueV2] Starting recipe graph propagation...");

        // 1. 清空状态
        conversionsFor.clear();
        usedIn.clear();
        fixedBefore.clear();
        fixedAfter.clear();

        // 2. 加载 CValueRegistry 中的基准值作为 fixedBefore
        for (var entry : CValueRegistry.allEntries()) {
            fixedBefore.put(entry.getKey().toString(), entry.getValue());
        }
        MetroGenesis.LOGGER.info("[CValueV2] {} base values loaded", fixedBefore.size());

        // 3. 扫描配方构建 Conversions
        scanRecipes(recipeManager, registryAccess);

        MetroGenesis.LOGGER.info("[CValueV2] {} conversions built ({} unique outputs)",
            conversionsFor.values().stream().mapToInt(Set::size).sum(), conversionsFor.size());

        // 4. 图传播迭代（BigDecimal 精确运算）
        Map<String, BigDecimal> results = propagate();

        // 5. 写入 CValueRegistry（新建的才写入，已有值不覆盖）
        int registered = 0;
        for (var entry : results.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) continue;
            long val = entry.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
            if (val <= 0) continue;
            if (CValueRegistry.getBaseValue(id).isEmpty()) {
                CValueRegistry.register(id, val);
                registered++;
            }
        }

        // 诊断：打印几个样本值
        for (String sample : new String[]{
            "minecraft:netherite_sword", "minecraft:diamond_sword",
            "minecraft:diamond", "minecraft:netherite_ingot",
            "minecraft:iron_sword", "minecraft:stick"
        }) {
            ResourceLocation sid = ResourceLocation.tryParse(sample);
            if (sid != null) {
                long v = CValueRegistry.getBaseValueOrZero(sid);
                MetroGenesis.LOGGER.info("[CValueV2]  {} = {}", sample, v);
            }
        }
        MetroGenesis.LOGGER.info("[CValueV2] Done. {} seeded + {} calculated = {} total",
            fixedBefore.size(), registered, CValueRegistry.size());
    }

    // ════════════════════════════════════════════════════════════
    //  配方扫描（仿 ProjectE CraftingMapper + BaseRecipeTypeMapper）
    // ════════════════════════════════════════════════════════════

    private static void scanRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        Collection<Recipe<?>> allRecipes = recipeManager.getRecipes();
        int skipped = 0, added = 0;

        for (Recipe<?> recipe : allRecipes) {
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty()) { skipped++; continue; }

            String outputId = getItemId(result.getItem());
            int outCount = Math.max(1, result.getCount());

            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) { skipped++; continue; }

            // 提取原料（跳过 smithing_template 等无价值物品）
            Map<String, Integer> ingMap = new LinkedHashMap<>();
            boolean hasValid = false;
            for (Ingredient ing : ingredients) {
                if (ing.isEmpty()) continue;
                ItemStack[] matches = ing.getItems();
                if (matches.length == 0) continue;

                // 多匹配原料：取第一个（ProjectE 用 fake group 处理，这里简化）
                ItemStack match = null;
                for (ItemStack m : matches) {
                    if (!m.isEmpty()) { match = m; break; }
                }
                if (match == null) continue;

                String id = getItemId(match.getItem());
                if (id == null || id.contains("smithing_template")) continue;
                if (id.equals(outputId)) continue; // 跳过自引用配方

                ingMap.merge(id, 1, Integer::sum);
                hasValid = true;
            }
            if (!hasValid || ingMap.isEmpty()) { skipped++; continue; }

            // 注册 Conversion
            addConversion(outputId, outCount, ingMap);
            added++;
        }

        MetroGenesis.LOGGER.info("[CValueV2] Recipes: {} added, {} skipped", added, skipped);
    }

    private static void addConversion(String output, int outNumber, Map<String, Integer> ingredients) {
        Conversion conv = new Conversion(output, outNumber, new HashMap<>(ingredients));

        conversionsFor.computeIfAbsent(output, k -> new LinkedHashSet<>()).add(conv);
        for (String ing : ingredients.keySet()) {
            usedIn.computeIfAbsent(ing, k -> new LinkedHashSet<>()).add(conv);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  图传播（仿 ProjectE SimpleGraphMapper.generateValues）
    // ════════════════════════════════════════════════════════════

    private static Map<String, BigDecimal> propagate() {
        // 初始值：fixedBefore 转 BigDecimal
        Map<String, BigDecimal> values = new HashMap<>();
        for (var e : fixedBefore.entrySet()) {
            values.put(e.getKey(), BigDecimal.valueOf(e.getValue()));
        }

        // 变更队列
        Map<String, BigDecimal> changed = new HashMap<>(values);
        int iter = 0;

        // Phase 1: 正向传播
        while (!changed.isEmpty() && iter < MAX_ITERATIONS) {
            iter++;
            Map<String, BigDecimal> nextChanged = new HashMap<>();

            for (var entry : changed.entrySet()) {
                String itemId = entry.getKey();
                BigDecimal itemVal = entry.getValue();
                if (itemVal.signum() <= 0) continue;

                // 这个物品被用在哪些配方里？
                Set<Conversion> recipes = usedIn.get(itemId);
                if (recipes == null) continue;

                for (Conversion conv : recipes) {
                    // 已有值且更优则跳过
                    BigDecimal current = values.get(conv.outputId);
                    if (current != null && current.compareTo(BigDecimal.ZERO) > 0) continue;

                    // 检查所有原料是否都有值
                    BigDecimal totalInput = BigDecimal.ZERO;
                    boolean allReady = true;
                    for (var ing : conv.ingredients.entrySet()) {
                        BigDecimal ingVal = values.get(ing.getKey());
                        if (ingVal == null || ingVal.signum() <= 0) {
                            allReady = false;
                            break;
                        }
                        totalInput = totalInput.add(ingVal.multiply(BigDecimal.valueOf(ing.getValue())));
                    }
                    if (!allReady) continue;

                    // 计算输出 = totalInput / outNumber
                    BigDecimal computed = totalInput.divide(
                        BigDecimal.valueOf(conv.outNumber), 10, RoundingMode.HALF_UP);

                    // 如果已有值且更小，跳过
                    if (current != null && current.compareTo(computed) <= 0) continue;

                    values.put(conv.outputId, computed);
                    nextChanged.put(conv.outputId, computed);
                }
            }

            changed = nextChanged;
        }
        MetroGenesis.LOGGER.info("[CValueV2] Phase 1 converged after {} iterations", iter);

        // Phase 2: 漏洞检测 & 多配方取最小值
        for (var entry : conversionsFor.entrySet()) {
            String output = entry.getKey();
            BigDecimal best = null;

            for (Conversion conv : entry.getValue()) {
                BigDecimal totalInput = BigDecimal.ZERO;
                boolean allReady = true;
                for (var ing : conv.ingredients.entrySet()) {
                    BigDecimal ingVal = values.get(ing.getKey());
                    if (ingVal == null || ingVal.signum() <= 0) { allReady = false; break; }
                    totalInput = totalInput.add(ingVal.multiply(BigDecimal.valueOf(ing.getValue())));
                }
                if (!allReady) continue;

                BigDecimal computed = totalInput.divide(
                    BigDecimal.valueOf(conv.outNumber), 10, RoundingMode.HALF_UP);

                if (best == null || computed.compareTo(best) < 0) {
                    best = computed;
                }

                // 漏洞检测：产物价值 > 原料价值
                BigDecimal currentOut = values.getOrDefault(output, BigDecimal.ZERO);
                if (currentOut.signum() > 0 && computed.compareTo(currentOut) < 0) {
                    MetroGenesis.LOGGER.warn("[CValueV2] Potential exploit: {} cost {} → value {}",
                        output, totalInput, computed);
                }
            }

            if (best != null) {
                BigDecimal existing = values.get(output);
                if (existing == null || best.compareTo(existing) < 0) {
                    values.put(output, best);
                }
            }
        }

        // 应用 fixedAfter（最终覆盖）
        for (var e : fixedAfter.entrySet()) {
            values.put(e.getKey(), BigDecimal.valueOf(e.getValue()));
        }

        // 移除 <= 0 的值
        values.entrySet().removeIf(e -> e.getValue().signum() <= 0);

        return values;
    }

    private static String getItemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.toString() : null;
    }
}
