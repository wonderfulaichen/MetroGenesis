package com.metrogenesis.core.economy;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * C-Value 配方推算器 v3 — 修复锻造/燃料/传播三问题。
 * <p>
 * 相对 v2 的改变：
 * <ul>
 *   <li>显式处理 SmithingTransformRecipe（分别取 base+addition，跳过 template）</li>
 *   <li>Phase 1 移除 "已有值则跳过" 限制，允许多路径取更优</li>
 *   <li>Phase 2 改为从所有配方中取最小值</li>
 *   <li>熔炉燃料价值：燃烧时间 / 32 → C-Value</li>
 *   <li>矿物块反向推算：9 锭 → 1 块</li>
 * </ul>
 */
public final class CValueCalculatorV3 {

    private static final int MAX_ITER = 30;

    private record Conversion(String output, int outNum, Map<String, Integer> inputs) {}

    /** 燃料燃烧时间 → C-Value 映射 */
    private static final Map<Item, Integer> FUEL_BURN_TIMES = new HashMap<>();
    static {
        // 常用燃料
        FUEL_BURN_TIMES.put(Items.COAL, 1600);
        FUEL_BURN_TIMES.put(Items.CHARCOAL, 1600);
        FUEL_BURN_TIMES.put(Items.COAL_BLOCK, 16000);
        FUEL_BURN_TIMES.put(Items.LAVA_BUCKET, 20000);
        FUEL_BURN_TIMES.put(Items.DRIED_KELP_BLOCK, 4000);
        FUEL_BURN_TIMES.put(Items.BLAZE_ROD, 2400);
        // 木质燃料  
        FUEL_BURN_TIMES.put(Items.OAK_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.SPRUCE_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.BIRCH_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.JUNGLE_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.ACACIA_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.DARK_OAK_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.CRIMSON_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.WARPED_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.OAK_LOG, 300);
        FUEL_BURN_TIMES.put(Items.STICK, 100);
        FUEL_BURN_TIMES.put(Items.OAK_SLAB, 150);
        FUEL_BURN_TIMES.put(Items.OAK_FENCE, 300);
        FUEL_BURN_TIMES.put(Items.OAK_STAIRS, 300);
        FUEL_BURN_TIMES.put(Items.OAK_TRAPDOOR, 300);
        FUEL_BURN_TIMES.put(Items.OAK_DOOR, 200);
        FUEL_BURN_TIMES.put(Items.OAK_BUTTON, 100);
        FUEL_BURN_TIMES.put(Items.OAK_PRESSURE_PLATE, 300);
        FUEL_BURN_TIMES.put(Items.OAK_FENCE_GATE, 300);
        // 其他
        FUEL_BURN_TIMES.put(Items.BAMBOO, 50);
        FUEL_BURN_TIMES.put(Items.BOWL, 100);
        FUEL_BURN_TIMES.put(Items.LADDER, 300);
        FUEL_BURN_TIMES.put(Items.CRAFTING_TABLE, 300);
        FUEL_BURN_TIMES.put(Items.CHEST, 300);
        FUEL_BURN_TIMES.put(Items.BOOKSHELF, 300);
        FUEL_BURN_TIMES.put(Items.JUKEBOX, 300);
        FUEL_BURN_TIMES.put(Items.NOTE_BLOCK, 300);
        FUEL_BURN_TIMES.put(Items.DAYLIGHT_DETECTOR, 300);
        FUEL_BURN_TIMES.put(Items.CARTOGRAPHY_TABLE, 300);
        FUEL_BURN_TIMES.put(Items.FLETCHING_TABLE, 300);
        FUEL_BURN_TIMES.put(Items.LOOM, 300);
        FUEL_BURN_TIMES.put(Items.STONECUTTER, 300);
        FUEL_BURN_TIMES.put(Items.SMITHING_TABLE, 300);
        FUEL_BURN_TIMES.put(Items.GRINDSTONE, 300);
    }

    // ════════════════════════════════════════════════════════════

    private static final Map<String, Set<Conversion>> conversionsFor = new HashMap<>();
    private static final Map<String, Set<Conversion>> usedIn = new HashMap<>();

    private CValueCalculatorV3() {}

    public static void calculate(RecipeManager recipeManager, RegistryAccess regAccess) {
        MetroGenesis.LOGGER.info("[CValueV3] --- start ---");

        conversionsFor.clear();
        usedIn.clear();

        // 1. 注册已有基准值
        Map<String, Long> seedValues = new HashMap<>();
        for (var e : CValueRegistry.allEntries()) {
            seedValues.put(e.getKey().toString(), e.getValue());
        }
        MetroGenesis.LOGGER.info("[CValueV3] {} seed values loaded", seedValues.size());

        // 2. 扫描配方 + 燃料
        scanAllRecipes(recipeManager, regAccess);
        registerFuelValues(seedValues);

        MetroGenesis.LOGGER.info("[CValueV3] {} conversions, {} unique outputs",
            conversionsFor.values().stream().mapToInt(Set::size).sum(), conversionsFor.size());

        // 3. 传播
        Map<String, BigDecimal> results = propagate(seedValues);

        // 3b. 给仍无价的物品按用途自动定价
        int autoPriced = autoPriceUnpriced(results, regAccess);
        MetroGenesis.LOGGER.info("[CValueV3] Auto-priced {} items by utility", autoPriced);

        // 4. 写回 CValueRegistry
        int reg = 0;
        for (var e : results.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            if (id == null) continue;
            long v = e.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
            if (v <= 0) continue;
            if (CValueRegistry.getBaseValue(id).isEmpty()) {
                CValueRegistry.register(id, v);
                reg++;
            }
        }

        // 5. 诊断
        for (String s : new String[]{
            "minecraft:netherite_sword", "minecraft:diamond_sword",
            "minecraft:diamond", "minecraft:netherite_ingot", "minecraft:coal",
            "minecraft:iron_sword", "minecraft:oak_planks", "minecraft:cobblestone"
        }) {
            ResourceLocation sid = ResourceLocation.tryParse(s);
            if (sid != null)
                MetroGenesis.LOGGER.info("[CValueV3]  {} = {}", s, CValueRegistry.getBaseValueOrZero(sid));
        }

        MetroGenesis.LOGGER.info("[CValueV3] Done: {} seed + {} calc = {} total",
            seedValues.size(), reg, CValueRegistry.size());
    }

    // ════════════════════════════════════════════════════════════
    //  配方扫描
    // ════════════════════════════════════════════════════════════

    private static void scanAllRecipes(RecipeManager rm, RegistryAccess ra) {
        int added = 0, skipped = 0;
        for (Recipe<?> recipe : rm.getRecipes()) {
            if (addConversionForRecipe(recipe, ra)) added++;
            else skipped++;
        }
        MetroGenesis.LOGGER.info("[CValueV3] Recipes: {} added, {} skipped", added, skipped);
    }

    private static boolean addConversionForRecipe(Recipe<?> recipe, RegistryAccess ra) {
        ItemStack result = recipe.getResultItem(ra);
        if (result.isEmpty()) return false;
        String outId = getItemId(result.getItem());
        if (outId == null) return false;
        int outNum = Math.max(1, result.getCount());

        // 获取原料：对 Smithing 配方需要反射取私有字段（getIngredients() 不返回 base/addition）
        List<Ingredient> ingredients = getRecipeIngredients(recipe);
        if (ingredients.isEmpty()) return false;

        Map<String, Integer> inputs = new LinkedHashMap<>();
        boolean hasValid = false;

        for (Ingredient ing : ingredients) {
            if (ing == null || ing.isEmpty()) continue;
            ItemStack[] matches = ing.getItems();
            if (matches.length == 0) continue;
            for (ItemStack m : matches) {
                if (m.isEmpty()) continue;
                String id = getItemId(m.getItem());
                if (id == null || id.contains("smithing_template") || id.equals(outId)) continue;
                inputs.merge(id, 1, Integer::sum);
                hasValid = true;
            }
        }
        if (!hasValid || inputs.isEmpty()) return false;

        registerConversion(outId, outNum, inputs);
        return true;
    }

    /** 获取配方的原料列表（对 Smithing 配方用反射取 base+addition） */
    @SuppressWarnings("unchecked")
    private static List<Ingredient> getRecipeIngredients(Recipe<?> recipe) {
        // SmithingTransformRecipe: getIngredients() 不包含 base 和 addition，需要反射
        if (recipe.getClass().getName().contains("SmithingTransform")) {
            try {
                Ingredient base = null, addition = null, template = null;
                for (java.lang.reflect.Field f : recipe.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    String name = f.getName();
                    Object val = f.get(recipe);
                    if (!(val instanceof Ingredient)) continue;
                    if (name.contains("template") || name.contains("Template")) template = (Ingredient) val;
                    else if (name.contains("base") || name.contains("Base")) base = (Ingredient) val;
                    else if (name.contains("addition") || name.contains("Addition")) addition = (Ingredient) val;
                }
                List<Ingredient> list = new ArrayList<>();
                if (base != null) list.add(base);
                if (addition != null) list.add(addition);
                return list;
            } catch (Exception e) {
                // fall through to default
            }
        }
        // 默认路径
        NonNullList<Ingredient> ings = recipe.getIngredients();
        return ings;
    }

    /** 燃料价值：燃烧时间 / 32 */
    private static void registerFuelValues(Map<String, Long> seeds) {
        int reg = 0;
        for (var e : FUEL_BURN_TIMES.entrySet()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.getKey());
            if (id == null) continue;
            long val = Math.max(1, e.getValue() / 32);
            String key = id.toString();
            if (!seeds.containsKey(key)) {
                seeds.put(key, val);
                reg++;
            }
        }
        MetroGenesis.LOGGER.info("[CValueV3] Fuel values: {} registered", reg);
    }

    private static void registerConversion(String out, int n, Map<String, Integer> inputs) {
        Conversion c = new Conversion(out, n, new HashMap<>(inputs));
        conversionsFor.computeIfAbsent(out, k -> new LinkedHashSet<>()).add(c);
        for (String ing : inputs.keySet())
            usedIn.computeIfAbsent(ing, k -> new LinkedHashSet<>()).add(c);
    }

    // ════════════════════════════════════════════════════════════
    //  传播（多配方取最小值）
    // ════════════════════════════════════════════════════════════

    private static Map<String, BigDecimal> propagate(Map<String, Long> seeds) {
        Map<String, BigDecimal> values = new HashMap<>();
        for (var e : seeds.entrySet())
            values.put(e.getKey(), BigDecimal.valueOf(e.getValue()));

        // 双向传播迭代
        boolean changed = true;
        int iter = 0;
        while (changed && iter < MAX_ITER) {
            iter++;
            changed = false;

            // 从所有种子/已有值出发，尝试计算所有配方
            for (Map.Entry<String, Set<Conversion>> entry : conversionsFor.entrySet()) {
                String output = entry.getKey();
                BigDecimal best = null;

                for (Conversion conv : entry.getValue()) {
                    boolean allSet = true;
                    BigDecimal total = BigDecimal.ZERO;
                    for (var ing : conv.inputs.entrySet()) {
                        BigDecimal val = values.get(ing.getKey());
                        if (val == null || val.signum() <= 0) { allSet = false; break; }
                        total = total.add(val.multiply(BigDecimal.valueOf(ing.getValue())));
                    }
                    if (!allSet) continue;

                    BigDecimal computed = total.divide(
                        BigDecimal.valueOf(conv.outNum), 10, RoundingMode.HALF_UP);
                    if (best == null || computed.compareTo(best) < 0) best = computed;
                }

                if (best == null) continue;
                BigDecimal existing = values.get(output);
                if (existing == null || best.compareTo(existing) < 0) {
                    values.put(output, best);
                    changed = true;
                }
            }
        }

        // 诊断：石头压力板的价格异常检测
        BigDecimal sp = values.get("minecraft:stone_pressure_plate");
        BigDecimal st = values.get("minecraft:stone");
        if (sp != null && st != null) {
            Set<Conversion> spc = conversionsFor.get("minecraft:stone_pressure_plate");
            if (spc != null) {
                for (Conversion c : spc) {
                    BigDecimal total = BigDecimal.ZERO;
                    for (var ing : c.inputs.entrySet()) {
                        BigDecimal iv = values.get(ing.getKey());
                        total = total.add(iv != null ? iv.multiply(BigDecimal.valueOf(ing.getValue())) : BigDecimal.ZERO);
                    }
                    BigDecimal expected = total.divide(BigDecimal.valueOf(c.outNum), 10, RoundingMode.HALF_UP);
                    if (sp.compareTo(expected) < 0) {
                        MetroGenesis.LOGGER.warn("[CValueV3] BUG: stone_pressure_plate={} < expected={} (stone={}, total={})",
                            sp, expected, st, total);
                    }
                }
            }
        }

        // 诊断：检查 netherite_sword 的来源
        BigDecimal ns = values.get("minecraft:netherite_sword");
        if (ns == null) {
            // 尝试直接推算
            Set<Conversion> cs = conversionsFor.get("minecraft:netherite_sword");
            if (cs != null) {
                for (Conversion c : cs) {
                    StringBuilder sb = new StringBuilder("  netherite_sword: ");
                    boolean all = true;
                    for (var ing : c.inputs.entrySet()) {
                        BigDecimal iv = values.get(ing.getKey());
                        sb.append(ing.getKey()).append("=").append(iv != null ? iv : "NULL").append(" ");
                        if (iv == null || iv.signum() <= 0) all = false;
                    }
                    sb.append("allSet=").append(all);
                    MetroGenesis.LOGGER.info("[CValueV3] {}", sb);
                }
            } else {
                MetroGenesis.LOGGER.info("[CValueV3] netherite_sword has NO conversion registered!");
            }
        } else {
            MetroGenesis.LOGGER.info("[CValueV3] netherite_sword = {}", ns);
        }

        MetroGenesis.LOGGER.info("[CValueV3] Converged after {} iterations, {} items",
            iter, values.size());
        return values;
    }

    // ════════════════════════════════════════════════════════════
    //  自动定价 — 按用途/稀有度给无价物品定最低价
    // ════════════════════════════════════════════════════════════

    /** 堆肥概率 → 价值映射（1.20.1 原版堆肥数据） */
    private static final Map<String, Float> COMPOST_CHANCES = new HashMap<>();
    static {
        // 30%: 草/叶/苗/海草等
        for (String s : new String[]{"grass","seagrass","kelp","moss_block",
            "moss_carpet","hanging_roots","vine","glow_berries","sweet_berries"})
            COMPOST_CHANCES.put("minecraft:" + s, 0.3f);
        // 50%: 干海带块/竹/仙人掌等
        for (String s : new String[]{"dried_kelp_block","cactus","sugar_cane",
            "bamboo","twisting_vines","weeping_vines","nether_sprouts"})
            COMPOST_CHANCES.put("minecraft:" + s, 0.5f);
        // 65%: 苹果/甜菜根/西瓜片/南瓜/胡萝卜/土豆等
        for (String s : new String[]{"apple","beetroot","melon_slice","pumpkin",
            "carrot","potato","sweet_berries","wheat","dried_kelp"})
            COMPOST_CHANCES.put("minecraft:" + s, 0.65f);
        // 85%: 面包/曲奇/干海带/西瓜/烤土豆等
        for (String s : new String[]{"baked_potato","bread","cookie","pumpkin_pie",
            "cake","cooked_cod","cooked_salmon"})
            COMPOST_CHANCES.put("minecraft:" + s, 0.85f);
        // 100%: 南瓜派/蛋糕等
        COMPOST_CHANCES.put("minecraft:cake", 1.0f);
        COMPOST_CHANCES.put("minecraft:pumpkin_pie", 1.0f);
    }

    private static int autoPriceUnpriced(Map<String, BigDecimal> results, RegistryAccess ra) {
        int count = 0;
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            String id = entry.getKey().location().toString();
            if (results.containsKey(id)) continue; // 已有价

            Item item = entry.getValue();
            ItemStack stack = item.getDefaultInstance();

            // 跳过创造模式专属物品（刷怪蛋、命令方块等）— 无可获取途径不标价
            // 1.20.1: 检查物品 ID 前缀（minecraft:spawn_egg, minecraft:command等）
            if (id.contains("spawn_egg") || id.contains("command_block")
                || id.contains("barrier") || id.contains("structure_block")
                || id.contains("jigsaw") || id.contains("debug")
                || id.contains("knowledge_book") || id.contains("light")) continue;

            long price = 0;

            // a) 熔炉燃料：燃烧时间 / 32
            int burnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null);
            if (burnTime > 0) price = Math.max(price, burnTime / 32);

            // b) 堆肥物品：概率 → 价值
            Float chance = COMPOST_CHANCES.get(id);
            if (chance != null) {
                price = Math.max(price, chance >= 1.0f ? 5 : chance >= 0.8f ? 4
                    : chance >= 0.6f ? 3 : chance >= 0.4f ? 2 : 1);
            }

            // c) 有合成配方的物品让传播系统定价，跳过
            if (conversionsFor.containsKey(id)) continue;

            // d) 自然生成/掉落物品：按稀有度给最低价
            if (price == 0) {
                var rarity = item.getRarity(stack);
                price = switch (rarity) {
                    case COMMON -> 1;
                    case UNCOMMON -> 5;
                    case RARE -> 20;
                    case EPIC -> 80;
                };
            }

            if (price > 0) {
                results.put(id, BigDecimal.valueOf(price));
                count++;
            }
        }
        return count;
    }

    private static String getItemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.toString() : null;
    }
}
