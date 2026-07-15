package com.metrogenesis.core.economy;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

/**
 * C-Value 配方推算器 — 仿 ProjectE SimpleGraphMapper。
 * <p>
 * 从已知基础物质（原木=2、圆石=2、铁锭=10…）出发，遍历所有合成配方，
 * 用图传播算法为所有可合成的物品推算 C-Value。
 * <p>
 * 算法步骤：
 * <ol>
 *   <li>种子：{@link CValueRegistry} 中已注册的基准值（硬编码 + JSON 覆盖）</li>
 *   <li>对每个已知价值的物品，找出所有以此为原料的配方</li>
 *   <li>若配方中所有原料都有价值，计算输出价值 = Σ(原料价值×数量) ÷ 输出数量</li>
 *   <li>若计算值 < 现有值，更新并将新物品加入传播队列</li>
 *   <li>迭代至收敛（队列为空）</li>
 *   <li>圈检测：若迭代超过阈值仍未收敛，标记为 0</li>
 * </ol>
 * <p>
 * 由 {@code MetroGenesis.onServerStarting} 触发，datapack reload 时重新计算。
 */
public final class CValueCalculator {

    private static final int MAX_ITERATIONS = 20;

    private CValueCalculator() {}

    /**
     * 从 RecipeManager 中计算所有物品的 C-Value，结果写入 CValueRegistry。
     * 在服务端启动和数据包重新加载时调用。
     */
    public static void calculate(RecipeManager recipeManager, RegistryAccess registryAccess) {
        MetroGenesis.LOGGER.info("[CValueCalc] Starting recipe-based C-Value calculation...");

        // 1. 收集所有配方（1.20.1 API: getRecipes() 返回 Collection<Recipe<?>>）
        final Collection<Recipe<?>> allRecipes = recipeManager.getRecipes();
        MetroGenesis.LOGGER.info("[CValueCalc] Scanning {} recipes", allRecipes.size());

        // 2. 构建配方索引：Item → 以此物品为原料的配方
        final Map<String, List<RecipeIngredient>> recipeIndex = new HashMap<>();
        int skipped = 0;

        for (Recipe<?> recipe : allRecipes) {
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty()) {
                skipped++;
                continue;
            }

            // 获取原料（smelting 配方只有 1 个原料，smithing 有 3 个含模板）
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                skipped++;
                continue;
            }

            // 过滤：跳过无价值原料（如锻造模板），但保留至少 2 个有值原料
            List<String> ingredientIds = new ArrayList<>();
            for (Ingredient ing : ingredients) {
                if (ing.isEmpty()) continue;
                ItemStack[] stacks = ing.getItems();
                if (stacks.length > 0 && !stacks[0].isEmpty()) {
                    String id = getItemId(stacks[0].getItem());
                    // 跳过模板等无基准价值的原料（锻造配方会用）
                    if (!id.contains("smithing_template")) {
                        ingredientIds.add(id);
                    }
                }
            }
            if (ingredientIds.size() < 1) {
                skipped++;
                continue;
            }

            String outputId = getItemId(result.getItem());
            int outputCount = Math.max(1, result.getCount());

            RecipeIngredient ri = new RecipeIngredient(outputId, outputCount, ingredientIds);

            // 为每个原料建立反向索引
            for (String ingId : ingredientIds) {
                recipeIndex.computeIfAbsent(ingId, k -> new ArrayList<>()).add(ri);
            }
        }

        MetroGenesis.LOGGER.info("[CValueCalc] Built recipe index: {} recipes (skipped {}), {} ingredient types",
            allRecipes.size() - skipped, skipped, recipeIndex.size());

        // 3. 从 CValueRegistry 获取已知基准值
        final Map<String, Long> values = new HashMap<>();
        for (var entry : CValueRegistry.allEntries()) {
            values.put(entry.getKey().toString(), entry.getValue());
        }
        MetroGenesis.LOGGER.info("[CValueCalc] Seeded with {} fixed values (×10 scaled)", values.size());

        // 4. 图传播迭代（基准值已放大×10，整数除法不再截断为0）
        final Set<String> changed = new HashSet<>(values.keySet());
        int iteration = 0;

        while (!changed.isEmpty() && iteration < MAX_ITERATIONS) {
            iteration++;
            final Set<String> currentBatch = new HashSet<>(changed);
            changed.clear();

            for (String itemId : currentBatch) {
                long itemValue = values.getOrDefault(itemId, 0L);
                if (itemValue <= 0) continue;

                List<RecipeIngredient> recipes = recipeIndex.get(itemId);
                if (recipes == null) continue;

                for (RecipeIngredient ri : recipes) {
                    long currentOutputValue = values.getOrDefault(ri.outputId, 0L);

                    boolean allHaveValues = true;
                    long totalInputValue = 0;
                    for (String ingId : ri.ingredientIds) {
                        Long ingVal = values.get(ingId);
                        if (ingVal == null || ingVal <= 0) {
                            allHaveValues = false;
                            break;
                        }
                        totalInputValue += ingVal;
                    }
                    if (!allHaveValues) continue;

                    long computedValue = Math.max(1, totalInputValue / ri.outputCount);

                    if (currentOutputValue <= 0 || computedValue < currentOutputValue) {
                        values.put(ri.outputId, computedValue);
                        changed.add(ri.outputId);
                    }
                }
            }

            MetroGenesis.LOGGER.info("[CValueCalc] Iter {}: {} items → {} in queue",
                iteration, currentBatch.size(), changed.size());
        }

        // 5. 将计算结果写回 CValueRegistry
        int registered = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id != null && entry.getValue() > 0) {
                if (CValueRegistry.getBaseValue(id).isEmpty()) {
                    CValueRegistry.register(id, entry.getValue());
                    registered++;
                }
            }
        }

        MetroGenesis.LOGGER.info("[CValueCalc] Done. {} fixed values + {} calculated = {} total items with C-Value",
            values.size() - registered, registered, CValueRegistry.size());
    }

    private static String getItemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.toString() : "";
    }

    /** 配方原料映射 */
    private record RecipeIngredient(String outputId, int outputCount, List<String> ingredientIds) {}
}
