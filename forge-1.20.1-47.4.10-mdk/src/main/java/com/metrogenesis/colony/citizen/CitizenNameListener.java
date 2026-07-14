package com.metrogenesis.colony.citizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 甯傛皯鍛藉悕 JSON 鐩戝惉鍣?鈥?鍔犺浇 {@code data/<modid>/citizennames/} 涓嬬殑 JSON 鏂囦欢
 * <p>
 * 鍙傝€?MineColonies {@code com.minecolonies.core.datalistener.CitizenNameListener}
 */
public class CitizenNameListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis-Names");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** 宸插姞杞界殑鍛藉悕鏂囦欢鏄犲皠锛宬ey = 鏂囦欢鍚嶏紙涓嶅惈鎵╁睍鍚嶏級 */
    private static Map<String, CitizenNameFile> nameFileMap = new HashMap<>();

    public CitizenNameListener() {
        super(GSON, "citizennames");
    }

    @Override
    protected void apply(
            final Map<ResourceLocation, JsonElement> jsonElementMap,
            final @NotNull ResourceManager resourceManager,
            final @NotNull ProfilerFiller profiler) {
        nameFileMap.clear();
        for (final Map.Entry<ResourceLocation, JsonElement> entry : jsonElementMap.entrySet()) {
            tryParse(entry);
        }
        LOGGER.info("Loaded {} citizen name file(s)", nameFileMap.size());
    }

    private void tryParse(final Map.Entry<ResourceLocation, JsonElement> entry) {
        try {
            final JsonObject data = (JsonObject) entry.getValue();

            final int parts = data.get("parts").getAsInt();
            final CitizenNameFile.NameOrder nameOrder =
                    CitizenNameFile.NameOrder.valueOf(data.get("order").getAsString());

            final List<String> maleFirstName = new ArrayList<>();
            final JsonArray maleArr = data.get("male_firstname").getAsJsonArray();
            for (final JsonElement e : maleArr) {
                maleFirstName.add(e.getAsString());
            }

            final List<String> femaleFirstName = new ArrayList<>();
            final JsonArray femaleArr = data.get("female_firstname").getAsJsonArray();
            for (final JsonElement e : femaleArr) {
                femaleFirstName.add(e.getAsString());
            }

            final List<String> surnames = new ArrayList<>();
            final JsonArray surArr = data.get("surnames").getAsJsonArray();
            for (final JsonElement e : surArr) {
                surnames.add(e.getAsString());
            }

            final String key = entry.getKey().getPath();
            nameFileMap.put(key, new CitizenNameFile(parts, nameOrder, maleFirstName, femaleFirstName, surnames));
            LOGGER.debug("Parsed name file: {}", key);
        } catch (Exception e) {
            LOGGER.warn("Could not parse citizen name file: {}", entry.getKey(), e);
        }
    }

    /** 鑾峰彇鎵€鏈夊凡鍔犺浇鐨勫懡鍚嶆枃浠?*/
    public static Map<String, CitizenNameFile> getNameFileMap() {
        return nameFileMap;
    }

    /** 鑾峰彇鎸囧畾閿殑鍛藉悕鏂囦欢锛屼笉瀛樺湪鏃惰繑鍥為粯璁?*/
    public static CitizenNameFile getByName(final String key) {
        return nameFileMap.get(key);
    }
}
