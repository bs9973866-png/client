package dev.nightfall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static dev.nightfall.Feature.Category.*;

public final class FeatureRegistry {
    private static final List<Feature> FEATURES = new ArrayList<>();
    private FeatureRegistry() {}

    public static void init() {
        FEATURES.clear();
        add("killaura", "KillAura", "Автоатака ближайшей цели (только одиночный мир).", COMBAT, false);
        add("triggerbot", "TriggerBot", "Атака цели под прицелом (только одиночный мир).", COMBAT, false);
        add("aimassist", "Aim Assist", "Мягкая доводка прицела к ближайшей цели.", COMBAT, false);
        add("hitspark", "Hit Particles", "Частицы при срабатывании автоатаки.", COMBAT, true);
        add("autosprint", "Auto Sprint", "Автоматический спринт при движении вперёд.", MOVEMENT, false);
        add("speed", "Speed", "Умеренное ускорение движения в одиночном мире.", MOVEMENT, false);
        add("nofall", "No Fall", "Сбрасывает клиентский счётчик падения.", MOVEMENT, false);
        add("fullbright", "FullBright", "Максимальная яркость с восстановлением исходного значения.", RENDER, false);
        add("customcrosshair", "Custom Crosshair", "Неоновый кастомный прицел.", RENDER, true);
        add("trail", "Player Trail", "Частицы за движущимся игроком.", RENDER, false);
        add("hitpulse", "Hit Pulse", "Лёгкая красная вспышка при получении урона.", RENDER, true);
        add("zoom", "Zoom", "Удерживай C для приближения.", RENDER, false);
        add("watermark", "Watermark", "Логотип NIGHTFALL в HUD.", HUD, true);
        add("fps", "FPS Counter", "Показывает текущий FPS.", HUD, true);
        add("coordinates", "Coordinates", "Показывает координаты игрока.", HUD, true);
        add("keystrokes", "Keystrokes", "Индикатор WASD и прыжка.", HUD, true);
        add("targethud", "Target HUD", "Информация о сущности под прицелом.", HUD, true);
        add("hitmarker", "Hit Marker", "Короткий маркер при успешной атаке.", HUD, true);
        add("menuinfo", "Menu Info", "Показывает состояние мира в меню ClickGUI.", MISC, true);
    }

    private static void add(String id, String name, String description, Feature.Category category, boolean enabled) {
        FEATURES.add(new Feature(id, name, description, category, enabled));
    }

    public static List<Feature> all() { return Collections.unmodifiableList(FEATURES); }
    public static List<Feature> in(Feature.Category category) {
        return FEATURES.stream().filter(f -> f.category() == category).toList();
    }
    public static Optional<Feature> find(String id) {
        return FEATURES.stream().filter(f -> f.id().equals(id)).findFirst();
    }
    public static boolean enabled(String id) {
        return find(id).map(Feature::enabled).orElse(false);
    }
}
