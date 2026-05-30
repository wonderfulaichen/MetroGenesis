package com.metrogenesis.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for data-driven language generation.
 * Collects translations from multiple SubProvider instances and writes them
 * into a single language JSON file under assets/&lt;modid&gt;/lang/.
 * <p>
 * This replaces the LDTTeam shared infrastructure class originally at
 * {@code com.ldtteam.data.LanguageProvider}.
 */
public abstract class LanguageProvider implements DataProvider
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path outputFolder;
    private final String modId;
    private final String defaultLang;
    private final List<SubProvider> subProviders;

    protected LanguageProvider(
            final DataGenerator gen,
            final String modId,
            final String defaultLang,
            final List<SubProvider> subProviders
    ) {
        this.outputFolder = gen.getPackOutput().getOutputFolder();
        this.modId = modId;
        this.defaultLang = defaultLang;
        this.subProviders = subProviders;
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(@NotNull final CachedOutput cache)
    {
        final Map<String, String> translations = new HashMap<>();
        final LanguageAcceptor acceptor = translations::put;

        for (final SubProvider provider : subProviders)
        {
            provider.addTranslations(acceptor);
        }

        final JsonObject json = new JsonObject();
        for (final Map.Entry<String, String> entry : translations.entrySet())
        {
            json.addProperty(entry.getKey(), entry.getValue());
        }

        final Path output = outputFolder.resolve(
                "assets/" + modId + "/lang/" + defaultLang + ".json"
        );
        return DataProvider.saveStable(cache, GSON.toJsonTree(json), output);
    }

    @Override
    @NotNull
    public abstract String getName();

    @FunctionalInterface
    public interface SubProvider
    {
        void addTranslations(LanguageAcceptor acceptor);
    }

    @FunctionalInterface
    public interface LanguageAcceptor
    {
        void add(String key, String value);
    }
}
