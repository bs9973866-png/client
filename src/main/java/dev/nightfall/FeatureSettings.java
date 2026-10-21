package dev.nightfall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Typed settings registry and persistent values for user-configurable modules. */
public final class FeatureSettings {
    public enum Type { TOGGLE, SLIDER, MODE }
    public record Spec(String key, String label, Type type, double min, double max, double step,
                       List<String> options, Object defaultValue) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, JsonElement>>() {}.getType();
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();
    private static final Map<String, Object> VALUES = new LinkedHashMap<>();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config/nightfall/settings.json");

    static {
        add("killaura", "range", "Дальность атаки", Type.SLIDER, 2.0, 5.0, 0.1, List.of(), 3.6);
        add("killaura", "rotation", "Ротация", Type.MODE, 0, 0, 0, List.of("Silent", "Visible"), "Silent");
        add("killaura", "rotationSpeed", "Скорость поворота", Type.SLIDER, 5, 90, 1, List.of(), 42.0);
        add("killaura", "criticalMode", "Криты", Type.MODE, 0, 0, 0, List.of("AutoJump", "Only", "Off"), "AutoJump");
        add("autototem", "healthThreshold", "Порог здоровья", Type.SLIDER, 1, 20, 1, List.of(), 12.0);
        add("autototem", "swapDelay", "Задержка замены", Type.SLIDER, 1, 10, 1, List.of(), 5.0);
        add("zoom", "fov", "Поле зрения", Type.SLIDER, 10, 50, 1, List.of(), 28.0);
        add("fullbright", "gamma", "Яркость", Type.SLIDER, 10, 100, 1, List.of(), 100.0);
    }

    private FeatureSettings() {}

    private static void add(String module, String name, String label, Type type, double min, double max,
                            double step, List<String> options, Object defaultValue) {
        String key = module + "." + name;
        SPECS.put(key, new Spec(key, label, type, min, max, step, List.copyOf(options), defaultValue));
    }

    public static List<Spec> forModule(String moduleId) {
        List<Spec> result = new ArrayList<>();
        String prefix = moduleId + ".";
        for (Spec spec : SPECS.values()) if (spec.key().startsWith(prefix)) result.add(spec);
        return List.copyOf(result);
    }

    public static Object get(Spec spec) {
        return VALUES.getOrDefault(spec.key(), spec.defaultValue());
    }

    public static boolean bool(Spec spec) {
        Object value = get(spec);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    public static double number(String moduleId, String settingName) {
        Spec spec = SPECS.get(moduleId + "." + settingName);
        if (spec == null) return 0;
        Object value = get(spec);
        return value instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    public static String mode(String moduleId, String settingName) {
        Spec spec = SPECS.get(moduleId + "." + settingName);
        if (spec == null) return "";
        return String.valueOf(get(spec));
    }

    public static String displayValue(Spec spec) {
        Object value = get(spec);
        if (spec.type() == Type.SLIDER && value instanceof Number n) {
            double d = n.doubleValue();
            return spec.step() < 1 ? String.format(java.util.Locale.ROOT, "%.1f", d)
                    : String.format(java.util.Locale.ROOT, "%.0f", d);
        }
        return String.valueOf(value);
    }

    public static void toggle(Spec spec) {
        VALUES.put(spec.key(), !bool(spec));
        save();
    }

    public static void setNumber(Spec spec, double value) {
        double step = spec.step() <= 0 ? 1 : spec.step();
        double clamped = Math.max(spec.min(), Math.min(spec.max(), value));
        double stepped = spec.min() + Math.round((clamped - spec.min()) / step) * step;
        VALUES.put(spec.key(), Math.max(spec.min(), Math.min(spec.max(), stepped)));
        save();
    }

    public static void nextMode(Spec spec) {
        if (spec.options().isEmpty()) return;
        String current = String.valueOf(get(spec));
        int index = spec.options().indexOf(current);
        VALUES.put(spec.key(), spec.options().get((index + 1 + spec.options().size()) % spec.options().size()));
        save();
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Map<String, JsonElement> loaded = GSON.fromJson(Files.readString(FILE), MAP_TYPE);
            if (loaded == null) return;
            loaded.forEach((key, element) -> {
                Spec spec = SPECS.get(key);
                if (spec == null || element == null || element.isJsonNull()) return;
                try {
                    switch (spec.type()) {
                        case TOGGLE -> VALUES.put(key, element.getAsBoolean());
                        case SLIDER -> setLoadedNumber(spec, element.getAsDouble());
                        case MODE -> {
                            String mode = element.getAsString();
                            if (spec.options().contains(mode)) VALUES.put(key, mode);
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            NightfallClient.LOGGER.warn("Could not load NIGHTFALL settings", e);
        }
    }

    private static void setLoadedNumber(Spec spec, double value) {
        double step = spec.step() <= 0 ? 1 : spec.step();
        double clamped = Math.max(spec.min(), Math.min(spec.max(), value));
        double stepped = spec.min() + Math.round((clamped - spec.min()) / step) * step;
        VALUES.put(spec.key(), Math.max(spec.min(), Math.min(spec.max(), stepped)));
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Map<String, Object> output = new LinkedHashMap<>();
            for (Spec spec : SPECS.values()) output.put(spec.key(), get(spec));
            Files.writeString(FILE, GSON.toJson(output));
        } catch (IOException e) {
            NightfallClient.LOGGER.warn("Could not save NIGHTFALL settings", e);
        }
    }
}
