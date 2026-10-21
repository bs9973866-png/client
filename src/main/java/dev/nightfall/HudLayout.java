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

public final class HudLayout {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, int[]>>() {}.getType();
    private static final Map<String, int[]> POSITIONS = new LinkedHashMap<>();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config/nightfall/hud-layout.json");

    private HudLayout() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Map<String, int[]> loaded = GSON.fromJson(Files.readString(FILE), TYPE);
                if (loaded != null) POSITIONS.putAll(loaded);
            }
        } catch (Exception e) {
            NightfallClient.LOGGER.warn("Could not load NIGHTFALL HUD layout", e);
        }
    }

    public static int[] get(String id, int defaultX, int defaultY) {
        int[] p = POSITIONS.get(id);
        return p != null && p.length >= 2 ? new int[]{p[0], p[1]} : new int[]{defaultX, defaultY};
    }

    public static void set(String id, int x, int y) {
        POSITIONS.put(id, new int[]{Math.max(0, x), Math.max(0, y)});
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(POSITIONS, TYPE));
        } catch (IOException e) {
            NightfallClient.LOGGER.warn("Could not save NIGHTFALL HUD layout", e);
        }
    }
}
