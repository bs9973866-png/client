package dev.nightfall.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public final class NightfallMainMenu extends Screen {
    private static final int BG_TOP = 0xFF0D1217;
    private static final int BG_BOTTOM = 0xFF151D24;
    private static final int PANEL = 0xE519232C;
    private static final int BORDER = 0xFF303C45;
    private static final int TEXT = 0xFFE1E8EA;
    private static final int MUTED = 0xFF8A9AA2;
    private static final int ACCENT = 0xFF79BDB5;
    private static final int ACCENT_DARK = 0xFF294641;

    public NightfallMainMenu() { super(Text.literal("NIGHTFALL")); }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int w = width, h = height, cx = w / 2;
        ctx.fillGradient(0, 0, w, h, BG_TOP, BG_BOTTOM);
        for (int i = 0; i < 5; i++) {
            int yy = (int)((System.currentTimeMillis() / 130.0 + i * 97) % Math.max(1, h));
            ctx.fill(0, yy, w, yy + 1, 0x0900B7AC);
        }
        ctx.fill(0, 0, w, 2, BORDER);
        ctx.fill(0, 0, 3, h, ACCENT_DARK);

        int cardW = Math.min(430, w - 32), cardH = 300, x = cx - cardW / 2, y = h / 2 - cardH / 2;
        ctx.fill(x + 3, y + 4, x + cardW + 3, y + cardH + 4, 0x44000000);
        ctx.fill(x, y, x + cardW, y + cardH, PANEL);
        ctx.fill(x, y, x + cardW, y + 1, BORDER);
        ctx.fill(x, y, x + 2, y + cardH, ACCENT);
        ctx.fill(x + 22, y + 24, x + 56, y + 58, ACCENT_DARK);
        centered(ctx, "N", x + 39, y + 36, ACCENT);
        draw(ctx, "NIGHTFALL", x + 68, y + 27, TEXT);
        draw(ctx, "CLIENT  /  MINECRAFT 1.21.4", x + 68, y + 44, MUTED);
        ctx.fill(x + 22, y + 72, x + cardW - 22, y + 73, BORDER);
        centered(ctx, "A QUIET, CLEAN CLIENT UI", cx, y + 88, MUTED);

        int bw = Math.min(330, cardW - 44), bh = 31, gap = 8, bx = cx - bw / 2, by = y + 116;
        button(ctx, bx, by, bw, bh, "ОДИНОЧНАЯ ИГРА", mx, my, true);
        button(ctx, bx, by + bh + gap, bw, bh, "СЕТЕВАЯ ИГРА", mx, my, false);
        int smallW = (bw - gap) / 2, sy = by + (bh + gap) * 2;
        button(ctx, bx, sy, smallW, 27, "НАСТРОЙКИ", mx, my, false);
        button(ctx, bx + smallW + gap, sy, smallW, 27, "ВЫХОД", mx, my, false);

        centered(ctx, "RIGHT SHIFT  ·  CLICKGUI", cx, y + 260, MUTED);
        draw(ctx, "NIGHTFALL", 12, h - 18, 0xFF687982);
        String build = "EXO 2 UI  /  FABRIC";
        draw(ctx, build, w - textRenderer.getWidth(UiText.of(build)) - 12, h - 18, 0xFF687982);
    }

    private void draw(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(value), x, y, color);
    }
    private void centered(DrawContext ctx, String value, int cx, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(value), cx - textRenderer.getWidth(UiText.of(value)) / 2, y, color);
    }
    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean primary) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = primary ? (hover ? 0xFFB0D5CF : 0xFF9CC9C1) : (hover ? 0xFF263641 : 0xFF1C2730);
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, primary ? 0xFFE5F6F2 : (hover ? ACCENT : BORDER));
        int color = primary ? 0xFF15211F : (hover ? TEXT : 0xFFB4C0C4);
        centered(ctx, label, x + w / 2, y + (h - 8) / 2, color);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cardW = Math.min(430, width - 32), cardH = 300, x = width / 2 - cardW / 2, y = height / 2 - cardH / 2;
            int bw = Math.min(330, cardW - 44), bh = 31, gap = 8, bx = width / 2 - bw / 2, by = y + 116;
            if (inside(mx, my, bx, by, bw, bh)) { client.setScreen(new SelectWorldScreen(this)); return true; }
            if (inside(mx, my, bx, by + bh + gap, bw, bh)) { client.setScreen(new MultiplayerScreen(this)); return true; }
            int smallW = (bw - gap) / 2, sy = by + (bh + gap) * 2;
            if (inside(mx, my, bx, sy, smallW, 27)) { client.setScreen(new OptionsScreen(this, client.options)); return true; }
            if (inside(mx, my, bx + smallW + gap, sy, smallW, 27)) { client.scheduleStop(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
}