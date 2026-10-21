package dev.nightfall.ui;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** NIGHTFALL's bundled Exo 2 font, with Minecraft's vanilla font as a provider fallback. */
public final class UiText {
    public static final Identifier FONT = Identifier.of("nightfall", "exo2");
    private UiText() {}

    public static Text of(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(FONT));
    }
}
