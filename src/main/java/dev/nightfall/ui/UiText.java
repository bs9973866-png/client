package dev.nightfall.ui;

import net.minecraft.text.Text;

/**
 * Reliable UI text factory. Vanilla's font is intentionally used for UI strings
 * so Russian/Cyrillic glyphs are always available. Exo 2 remains bundled as an
 * optional resource, but must not replace the game's fallback font globally.
 */
public final class UiText {
    private UiText() {}

    public static Text of(String text) {
        return Text.literal(text);
    }
}
