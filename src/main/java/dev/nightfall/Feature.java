package dev.nightfall;

public final class Feature {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), VISUALS("Visuals"),
        HUD("HUD"), WORLD("World"), PLAYER("Player");

        private final String title;
        Category(String title) { this.title = title; }
        public String title() { return title; }
    }

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    public Feature(String id, String name, String description, Category category, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public boolean enabled() { return enabled; }
    void setEnabledRaw(boolean enabled) { this.enabled = enabled; }
}
