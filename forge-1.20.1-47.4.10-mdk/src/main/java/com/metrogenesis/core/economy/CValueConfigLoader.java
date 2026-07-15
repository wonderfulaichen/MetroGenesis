package com.metrogenesis.core.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.metrogenesis.MetroGenesis;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class CValueConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();

    private CValueConfigLoader() {}

    public static void loadOverrides() {
        final File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "metrogenesis");
        if (!dir.exists() && !dir.mkdirs()) {
            MetroGenesis.LOGGER.warn("[CValueConfig] Cannot create config dir: {}", dir);
            return;
        }

        final File file = new File(dir, "cvalue_overrides.json");

        if (!file.exists()) {
            generateTemplate(file);
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Map<String, Long> overrides = GSON.fromJson(reader, MAP_TYPE);
            if (overrides == null || overrides.isEmpty()) {
                MetroGenesis.LOGGER.info("[CValueConfig] No overrides found");
                return;
            }
            int count = 0;
            for (var entry : overrides.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id != null && entry.getValue() != null && entry.getValue() > 0) {
                    CValueRegistry.register(id, entry.getValue());
                    count++;
                }
            }
            MetroGenesis.LOGGER.info("[CValueConfig] Loaded {} C-Value overrides", count);
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("[CValueConfig] Failed to load {}", file, e);
        }
    }

    private static void generateTemplate(File file) {
        Map<String, Long> template = new HashMap<>();
        template.put("minecraft:rotten_flesh", 3L);
        template.put("minecraft:gunpowder", 15L);
        template.put("minecraft:spider_eye", 5L);
        template.put("minecraft:ender_pearl", 60L);
        template.put("minecraft:ghast_tear", 40L);
        template.put("minecraft:magma_cream", 20L);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(template, writer);
            MetroGenesis.LOGGER.info("[CValueConfig] Generated template at {}", file);
        } catch (Exception e) {
            MetroGenesis.LOGGER.error("[CValueConfig] Failed to write template", e);
        }
    }
}
