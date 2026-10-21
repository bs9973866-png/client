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

/** Persistent keyboard and mouse-button toggle binds for NIGHTFALL features. */
public final class FeatureBinds {
    private static final int MOUSE_BIND_BASE = -100;
    private static final int MAX_MOUSE_BUTTON = 7;
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
                    if (FeatureRegistry.find(id).isPresent() && key != null
                            && (key >= 0 || isMouseBind(key))) BINDS.put(id, key);
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
        return BINDS.containsKey(featureId);
    }

    public static boolean isMouseBind(int code) {
        return code <= MOUSE_BIND_BASE && code >= MOUSE_BIND_BASE - MAX_MOUSE_BUTTON;
    }

    private static int encodeMouseButton(int button) {
        return MOUSE_BIND_BASE - button;
    }

    private static int decodeMouseButton(int code) {
        return MOUSE_BIND_BASE - code;
    }

    public static String label(String featureId) {
        int key = keyCode(featureId);
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        if (isMouseBind(key)) {
            return switch (decodeMouseButton(key)) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "MOUSE 1";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "MOUSE 2";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE 3";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "MOUSE 4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "MOUSE 5";
                case GLFW.GLFW_MOUSE_BUTTON_6 -> "MOUSE 6";
                case GLFW.GLFW_MOUSE_BUTTON_7 -> "MOUSE 7";
                case GLFW.GLFW_MOUSE_BUTTON_8 -> "MOUSE 8";
                default -> "MOUSE";
            };
        }
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
        else if (keyCode >= 0 || isMouseBind(keyCode)) BINDS.put(featureId, keyCode);
        WAS_DOWN.put(featureId, false);
        save();
    }

    public static void setMouseButton(String featureId, int button) {
        if (button >= GLFW.GLFW_MOUSE_BUTTON_1 && button <= MAX_MOUSE_BUTTON)
            set(featureId, encodeMouseButton(button));
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        long window = client.getWindow().getHandle();
        if (window == 0L) return;
        boolean mayToggle = client.currentScreen == null;
        for (Feature feature : FeatureRegistry.all()) {
            int code = keyCode(feature.id());
            if (code == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean down = isMouseBind(code)
                    ? GLFW.glfwGetMouseButton(window, decodeMouseButton(code)) == GLFW.GLFW_PRESS
                    : GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
            boolean wasDown = WAS_DOWN.getOrDefault(feature.id(), false);
            if (mayToggle && down && !wasDown)
                NightfallClient.setFeatureEnabled(feature.id(), !feature.enabled());
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
