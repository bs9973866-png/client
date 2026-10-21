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

        // Combat — client-side automation. Servers may still validate/reject interactions.
        add("killaura", "KillAura", "Автоатака ближайшего игрока или моба.", COMBAT, false);
        add("triggerbot", "TriggerBot", "Автоатака цели под прицелом.", COMBAT, false);
        add("aimassist", "Aim Assist", "Плавная доводка к выбранной цели.", COMBAT, false);
        add("reach", "Reach", "Увеличенная дистанция выбора цели на клиенте.", COMBAT, false);
        add("velocity", "Velocity", "Игнорировать входящий пакет отбрасывания.", COMBAT, false);
        add("targetplayers", "Target Players", "Разрешить выбор игроков как целей.", COMBAT, true);
        add("targetmobs", "Target Mobs", "Разрешить выбор мобов как целей.", COMBAT, false);
        add("hitspark", "Hit Particles", "Бирюзовые частицы при атаке.", COMBAT, false);

        // Movement
        add("autosprint", "Auto Sprint", "Автоматический спринт при движении вперёд.", MOVEMENT, false);
        add("speed", "Speed", "Небольшое ускорение наземного движения.", MOVEMENT, false);
        add("nofall", "No Fall", "Сбрасывает локальный счётчик падения.", MOVEMENT, false);
        add("spider", "Spider", "Подъём по стенам при контакте.", MOVEMENT, false);
        add("airjump", "Air Jump", "Повторный прыжок в воздухе по нажатию Space.", MOVEMENT, false);
        add("antifire", "Anti Fire", "Гасит огонь на локальном игроке.", MOVEMENT, false);
        add("zoom", "Zoom", "Удерживай C для плавного приближения.", MOVEMENT, false);

        // Visuals
        add("playeresp", "Player ESP", "Контуры игроков через рендер мира.", VISUALS, false);
        add("mobesp", "Mob ESP", "Контуры мобов.", VISUALS, false);
        add("tracers", "Tracers", "Линии от камеры к игрокам и мобам.", VISUALS, false);
        add("targetring", "Target Ring", "Кольцо под целью под прицелом.", VISUALS, false);
        add("entityglow", "Entity Glow", "Дополнительный мягкий контур сущностей.", VISUALS, false);
        add("itemesp", "Item ESP", "Контуры выпавших предметов.", VISUALS, false);
        add("projectileesp", "Projectile ESP", "Контуры стрел и других снарядов.", VISUALS, false);
        add("storageesp", "Storage ESP", "Контуры сундуков, бочек и шалкеров поблизости.", VISUALS, false);
        add("oresp", "Ore ESP", "Подсветка руд в пределах загруженной области.", VISUALS, false);
        add("fullbright", "FullBright", "Повышенная яркость с восстановлением настройки.", VISUALS, false);
        add("nebulasky", "Nebula Sky", "Фиолетовое небо, тонированные облака и звёзды NIGHTFALL.", VISUALS, true);
        add("customcrosshair", "Custom Crosshair", "Тонкий бирюзовый прицел.", VISUALS, true);
        add("hitpulse", "Damage Tint", "Ненавязчивый оттенок при получении урона.", VISUALS, false);
        add("trail", "Player Trail", "Мягкие частицы за игроком.", VISUALS, false);
        add("radar", "Player Radar", "Мини-карта игроков в радиусе 32 блоков.", VISUALS, false);
        add("worldlyrics", "World Lyrics", "Синхронный текст песни над игроком в мире.", VISUALS, false);

        // HUD
        add("watermark", "Watermark", "Логотип и версия клиента.", HUD, true);
        add("fps", "FPS Counter", "Счётчик кадров.", HUD, true);
        add("coordinates", "Coordinates", "Координаты игрока.", HUD, true);
        add("keystrokes", "Keystrokes", "Индикатор WASD и прыжка.", HUD, true);
        add("targethud", "Target HUD", "Имя и здоровье цели под прицелом.", HUD, true);
        add("hitmarker", "Hit Marker", "Небольшой маркер после удара.", HUD, true);
        add("armorhud", "Armor HUD", "Показатель прочности брони.", HUD, false);
        add("totemcount", "Totem Count", "Количество тотемов в инвентаре.", HUD, false);
        add("speedometer", "Speedometer", "Текущая горизонтальная скорость.", HUD, false);
        add("potionhud", "Effects HUD", "Активные эффекты и оставшееся время.", HUD, false);
        add("mediaplayer", "Media Player", "Spotify / Яндекс Музыка: трек, прогресс и управление.", HUD, false);
        add("mascot", "Anime Mascot", "Аниме-иллюстрация в HUD в стиле NIGHTFALL.", HUD, true);

        // Player / world utility
        add("autototem", "Auto Totem", "Перекладывает тотем в левую руку при низком HP.", PLAYER, false);
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
