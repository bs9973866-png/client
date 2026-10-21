package dev.nightfall.ui;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Shared custom Exo 2 font for NIGHTFALL menus and HUD. */
public final class UiText {
    public static final Identifier FONT = Identifier.of("nightfall", "exo2");
    private UiText() {}

    public static Text of(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(FONT));
    }
}