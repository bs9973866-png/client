package dev.nightfall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private static final Path PATH = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/nightfall.json");
    private ConfigManager() {}

    public static void load() {
        if (!Files.exists(PATH)) { save(); return; }
        try {
            Map<String, Boolean> values = GSON.fromJson(Files.readString(PATH), MAP_TYPE);
            if (values == null) return;
            values.forEach((id, enabled) -> FeatureRegistry.find(id).ifPresent(f -> {
                f.setEnabledRaw(Boolean.TRUE.equals(enabled));
                NightfallClient.applyFeatureState(f.id(), f.enabled());
            }));
        } catch (Exception exception) {
            NightfallClient.LOGGER.error("Не удалось загрузить конфиг NIGHTFALL", exception);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Map<String, Boolean> values = new LinkedHashMap<>();
            for (Feature feature : FeatureRegistry.all()) values.put(feature.id(), feature.enabled());
            Files.writeString(PATH, GSON.toJson(values));
        } catch (IOException exception) {
            NightfallClient.LOGGER.error("Не удалось сохранить конфиг NIGHTFALL", exception);
        }
    }
}
