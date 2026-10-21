package dev.nightfall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent keyboard toggle binds for each NIGHTFALL feature. */
public final class FeatureBinds {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Map<String, Integer> BINDS = new LinkedHashMap<>();
    private static final Map<String, Boolean> WAS_DOWN = new HashMap<>();

    private FeatureBinds() {}

    private static Path path() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/nightfall/binds.json");
    }

    public static void load() {
        BINDS.clear();
        WAS_DOWN.clear();
        try {
            Path path = path();
            if (!Files.exists(path)) return;
            Map<String, Integer> loaded = GSON.fromJson(Files.readString(path), MAP_TYPE);
            if (loaded != null) {
                loaded.forEach((id, key) -> {
                    if (FeatureRegistry.find(id).isPresent() && key != null && key >= 0) BINDS.put(id, key);
                });
            }
        } catch (Exception e) {
            NightfallClient.LOGGER.warn("Could not load NIGHTFALL module binds", e);
        }
    }

    public static int keyCode(String featureId) {
        return BINDS.getOrDefault(featureId, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static boolean isBound(String featureId) {
        return keyCode(featureId) != GLFW.GLFW_KEY_UNKNOWN;
    }

    public static String label(String featureId) {
        int key = keyCode(featureId);
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        return switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACK";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> {
                String keyName = GLFW.glfwGetKeyName(key, 0);
                if (keyName != null && !keyName.isBlank()) yield keyName.toUpperCase(Locale.ROOT);
                if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25)
                    yield "F" + (key - GLFW.GLFW_KEY_F1 + 1);
                if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9)
                    yield "NUM" + (key - GLFW.GLFW_KEY_KP_0);
                yield "KEY " + key;
            }
        };
    }

    public static void set(String featureId, int keyCode) {
        if (FeatureRegistry.find(featureId).isEmpty()) return;
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) BINDS.remove(featureId);
        else if (keyCode >= 0) BINDS.put(featureId, keyCode);
        WAS_DOWN.put(featureId, false);
        save();
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        long window = client.getWindow().getHandle();
        if (window == 0L) return;
        boolean mayToggle = client.currentScreen == null;
        for (Feature feature : FeatureRegistry.all()) {
            int key = keyCode(feature.id());
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            boolean wasDown = WAS_DOWN.getOrDefault(feature.id(), false);
            if (mayToggle && down && !wasDown) {
                NightfallClient.setFeatureEnabled(feature.id(), !feature.enabled());
            }
            WAS_DOWN.put(feature.id(), down);
        }
    }

    private static void save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(BINDS));
        } catch (IOException e) {
            NightfallClient.LOGGER.warn("Could not save NIGHTFALL module binds", e);
        }
    }
}
