package dev.nightfall.ui;

import dev.nightfall.MusicManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MediaPlayerScreen extends Screen {
    private static final Identifier PORTRAIT = Identifier.of("nightfall", "textures/gui/anime_portrait.jpg");

    public MediaPlayerScreen() { super(Text.literal("NIGHTFALL Media")); }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int w = width, h = height, cx = w / 2;
        ctx.fill(0, 0, w, h, 0xB8090610);
        int pw = Math.min(420, w - 30), ph = 236, x = cx - pw / 2, y = h / 2 - ph / 2;
        ctx.fill(x + 3, y + 4, x + pw + 3, y + ph + 4, 0x66000000);
        ctx.fill(x, y, x + pw, y + ph, 0xF11A1222);
        ctx.fill(x, y, x + pw, y + 1, 0xFF4B3154);
        ctx.fill(x, y, x + 3, y + ph, 0xFFF05CCF);
        draw(ctx, "NIGHTFALL  /  MEDIA STUDIO", x + 20, y + 17, 0xFFF05CCF);
        draw(ctx, "Spotify / Яндекс Музыка через Windows Media", x + 20, y + 35, 0xFFB39AB8);

        var state = MusicManager.state();
        String title = state.title().isBlank() ? "Открой Spotify / Яндекс Музыку" : state.title();
        String artist = state.artist().isBlank() ? state.status() : state.artist();

        ctx.fill(x + 20, y + 59, x + 99, y + 143, 0xFF3B2444);
        ctx.drawTexture(RenderLayer::getGuiTextured, PORTRAIT, x + 21, y + 60, 0.0f, 0.0f, 77, 82, 128, 128);
        draw(ctx, state.playing() ? "NOW PLAYING" : "MEDIA SESSION", x + 110, y + 61, 0xFFBE8BFF);
        draw(ctx, fit(title, pw - 130), x + 110, y + 81, 0xFFFFF0FE);
        draw(ctx, fit(artist, pw - 130), x + 110, y + 99, 0xFFB39AB8);
        String app = state.app().isBlank() ? "SYSTEM MEDIA" : state.app();
        draw(ctx, fit(app, pw - 130), x + 110, y + 117, 0xFF9C7AA7);

        int barX = x + 20, barY = y + 151, barW = pw - 40;
        ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF49344E);
        if (state.durationMs() > 0) {
            int filled = (int)(barW * Math.max(0, Math.min(1, state.positionNow() / (double) state.durationMs())));
            ctx.fill(barX, barY, barX + filled, barY + 3, 0xFFF05CCF);
        }
        String lyric = MusicManager.currentLyric();
        String lyricLine = lyric.isBlank()
                ? "WORLD LYRICS  /  " + (MusicManager.lyricError().isBlank() ? "ищем синхронный текст…" : MusicManager.lyricError())
                : lyric;
        draw(ctx, fit(lyricLine, pw - 40), x + 20, y + 160, lyric.isBlank() ? 0xFF896E91 : 0xFFBE8BFF);

        int gap = 8, bw = (pw - 40 - gap * 2) / 3, bh = 27, by = y + 183, bx = x + 20;
        button(ctx, bx, by, bw, bh, "PREV", mx, my);
        button(ctx, bx + bw + gap, by, bw, bh, state.playing() ? "PAUSE" : "PLAY", mx, my);
        button(ctx, bx + (bw + gap) * 2, by, bw, bh, "NEXT", mx, my);
        draw(ctx, "K — открыть плеер     ESC — закрыть", x + 20, y + ph - 18, 0xFF9C7AA7);
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isBlank()) return "";
        String s = text;
        while (!s.isEmpty() && textRenderer.getWidth(UiText.of(s)) > maxWidth) s = s.substring(0, s.length() - 1);
        return s.equals(text) ? s : (s.length() > 1 ? s.substring(0, s.length() - 1) + "…" : "…");
    }

    private void draw(DrawContext ctx, String text, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(text), x, y, color);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        ctx.fill(x, y, x + w, y + h, hover ? 0xFF3C234A : 0xFF24182E);
        ctx.fill(x, y, x + w, y + 1, hover ? 0xFFF05CCF : 0xFF4B3154);
        int tw = textRenderer.getWidth(UiText.of(label));
        draw(ctx, label, x + (w - tw) / 2, y + (h - 8) / 2, hover ? 0xFFFFF0FE : 0xFFB39AB8);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int pw = Math.min(420, width - 30), ph = 236, x = width / 2 - pw / 2, y = height / 2 - ph / 2;
            int gap = 8, bw = (pw - 40 - gap * 2) / 3, bh = 27, by = y + 183, bx = x + 20;
            if (inside(mx, my, bx, by, bw, bh)) { MusicManager.command("previous"); return true; }
            if (inside(mx, my, bx + bw + gap, by, bw, bh)) { MusicManager.command("toggle"); return true; }
            if (inside(mx, my, bx + (bw + gap) * 2, by, bw, bh)) { MusicManager.command("next"); return true; }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_K) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}