package dev.nightfall.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class NightfallMainMenu extends Screen {
    private static final int PANEL = 0xE9140C20;
    private static final int CARD = 0xEF21152F;
    private static final int BORDER = 0xFF59335D;
    private static final int TEXT = 0xFFFFF0FE;
    private static final int MUTED = 0xFFB39AB8;
    private static final int ACCENT = 0xFFF05CCF;
    private static final int ACCENT2 = 0xFFBE8BFF;
    private static final Identifier PORTRAIT = Identifier.of("nightfall", "textures/gui/anime_portrait.jpg");

    private record Layout(int x, int y, int w, int h, int leftW, int rightX, int rightW,
                          int buttonX, int buttonW, int buttonY, int buttonH, int gap,
                          int smallW, int secondY, int thirdY) {}

    public NightfallMainMenu() { super(Text.literal("NIGHTFALL")); }

    private Layout layout() {
        int w = Math.min(850, width - 28), h = Math.min(500, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        int leftW = Math.min(264, Math.max(208, w / 3));
        int rightX = x + leftW + 24, rightW = x + w - 20 - rightX;
        int buttonX = rightX + 4, buttonW = rightW - 8;
        int buttonY = y + 136, buttonH = 40, gap = 10;
        int smallW = (buttonW - gap) / 2;
        int secondY = buttonY + (buttonH + gap) * 2;
        int thirdY = secondY + 27 + gap;
        return new Layout(x, y, w, h, leftW, rightX, rightW, buttonX, buttonW,
                buttonY, buttonH, gap, smallW, secondY, thirdY);
    }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        ctx.fillGradient(0, 0, width, height, 0xFF0B0616, 0xFF21102D);
        for (int i = 0; i < 46; i++) {
            int sx = (int)Math.floorMod(i * 139L + now / (20L + i % 9), Math.max(1, width));
            int sy = (int)Math.floorMod(i * 73L + now / (34L + i % 11), Math.max(1, height));
            int alpha = (int)(24 + 45 * (0.5 + 0.5 * Math.sin(now * 0.0018 + i * 1.9)));
            roundedFill(ctx, sx, sy, i % 8 == 0 ? 4 : 2, i % 8 == 0 ? 4 : 2,
                    (alpha << 24) | 0x00E86ED8, 2);
        }
        int pulse = (int)(0.5 + 0.5 * Math.sin(now * 0.0025) * 0.5);
        ctx.fill(0, 0, width, 2, BORDER);
        ctx.fill(0, 0, 3, height, ACCENT);

        Layout l = layout();
        roundedFill(ctx, l.x() + 5, l.y() + 7, l.w(), l.h(), 0x55000000, 18);
        roundedFill(ctx, l.x(), l.y(), l.w(), l.h(), PANEL, 18);
        roundedFill(ctx, l.x() + 2, l.y() + 1, l.w() - 4, 3, ACCENT, 2);
        roundedFill(ctx, l.x() + l.leftW(), l.y() + 20, 1, l.h() - 40, BORDER, 1);

        // Left brand / anime illustration.
        roundedFill(ctx, l.x() + 20, l.y() + 23, 42, 42, 0xFF512048, 12);
        roundedFill(ctx, l.x() + 20, l.y() + 23, 42, 3, ACCENT, 2);
        centered(ctx, "N", l.x() + 41, l.y() + 40, TEXT);
        draw(ctx, "NIGHTFALL", l.x() + 74, l.y() + 28, TEXT);
        draw(ctx, "MOONLIGHT EDITION", l.x() + 74, l.y() + 46, ACCENT2);
        draw(ctx, "MINECRAFT 1.21.4 / FABRIC", l.x() + 20, l.y() + 76, MUTED);

        int portraitSize = Math.min(l.leftW() - 42, l.h() - 154);
        int portraitX = l.x() + (l.leftW() - portraitSize) / 2;
        int portraitY = l.y() + 110;
        roundedFill(ctx, portraitX - 4, portraitY - 4, portraitSize + 8, portraitSize + 8, BORDER, 14);
        roundedFill(ctx, portraitX, portraitY, portraitSize, portraitSize, CARD, 12);
        ctx.drawTexture(RenderLayer::getGuiTextured, PORTRAIT, portraitX, portraitY,
                0.0f, 0.0f, portraitSize, portraitSize, 128, 128);
        roundedFill(ctx, portraitX + 7, portraitY + portraitSize - 32, portraitSize - 14, 25, 0xD9100718, 8);
        draw(ctx, "STAY IN THE NIGHT", portraitX + 17, portraitY + portraitSize - 24, TEXT);
        draw(ctx, "✦  made for the night owls  ✦", l.x() + 20, l.y() + l.h() - 22, MUTED);

        // Right-hand menu.
        draw(ctx, "YOUR WORLD, YOUR STYLE", l.rightX(), l.y() + 29, ACCENT);
        draw(ctx, "Всё нужное — в одном мягком неоновом меню.", l.rightX(), l.y() + 48, MUTED);
        roundedFill(ctx, l.rightX(), l.y() + 68, l.rightW() - 4, 40, CARD, 9);
        roundedFill(ctx, l.rightX(), l.y() + 68, 3, 40, ACCENT, 2);
        draw(ctx, "✦   Визуальный клиент", l.rightX() + 14, l.y() + 81, TEXT);
        draw(ctx, "Анимации  •  мультяшные контуры  •  темы", l.rightX() + 14, l.y() + 94, MUTED);

        button(ctx, l.buttonX(), l.buttonY(), l.buttonW(), l.buttonH(),
                "ОДИНОЧНАЯ ИГРА", mx, my, true);
        button(ctx, l.buttonX(), l.buttonY() + l.buttonH() + l.gap(), l.buttonW(), l.buttonH(),
                "СЕТЕВАЯ ИГРА", mx, my, false);
        button(ctx, l.buttonX(), l.secondY(), l.smallW(), 29, "CLICKGUI", mx, my, false);
        button(ctx, l.buttonX() + l.smallW() + l.gap(), l.secondY(), l.smallW(), 29, "HUD STUDIO", mx, my, false);
        button(ctx, l.buttonX(), l.thirdY(), l.smallW(), 29, "НАСТРОЙКИ", mx, my, false);
        button(ctx, l.buttonX() + l.smallW() + l.gap(), l.thirdY(), l.smallW(), 29, "МУЗЫКА", mx, my, false);
        draw(ctx, "RIGHT SHIFT  /  CLICKGUI", l.rightX(), l.y() + l.h() - 23, MUTED);
        String build = "NIGHTFALL  •  1.21.4";
        draw(ctx, build, l.x() + l.w() - textRenderer.getWidth(UiText.of(build)) - 18,
                l.y() + l.h() - 23, 0xFF9776A6);
    }

    private void draw(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(s), x, y, color);
    }

    private void centered(DrawContext ctx, String s, int cx, int y, int color) {
        draw(ctx, s, cx - textRenderer.getWidth(UiText.of(s)) / 2, y, color);
    }

    private void roundedFill(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            int inset = 0;
            if (edge < r) {
                double dy = r - edge - 0.5;
                inset = Math.max(0, (int)Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy))));
            }
            ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label,
                        int mx, int my, boolean primary) {
        boolean hover = inside(mx, my, x, y, w, h);
        int bg = primary ? (hover ? 0xFFF56CD7 : 0xFFB945A8) : (hover ? 0xFF4A2855 : CARD);
        roundedFill(ctx, x, y, w, h, bg, 10);
        roundedFill(ctx, x + 8, y, w - 16, 2, hover || primary ? ACCENT : BORDER, 1);
        int col = primary ? TEXT : (hover ? TEXT : MUTED);
        centered(ctx, label, x + w / 2, y + (h - 8) / 2, col);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        Layout l = layout();
        if (inside(mx, my, l.buttonX(), l.buttonY(), l.buttonW(), l.buttonH())) {
            client.setScreen(new SelectWorldScreen(this)); return true;
        }
        if (inside(mx, my, l.buttonX(), l.buttonY() + l.buttonH() + l.gap(), l.buttonW(), l.buttonH())) {
            client.setScreen(new MultiplayerScreen(this)); return true;
        }
        if (inside(mx, my, l.buttonX(), l.secondY(), l.smallW(), 29)) {
            client.setScreen(new ClickGuiScreen()); return true;
        }
        if (inside(mx, my, l.buttonX() + l.smallW() + l.gap(), l.secondY(), l.smallW(), 29)) {
            client.setScreen(new HudEditorScreen()); return true;
        }
        if (inside(mx, my, l.buttonX(), l.thirdY(), l.smallW(), 29)) {
            client.setScreen(new OptionsScreen(this, client.options)); return true;
        }
        if (inside(mx, my, l.buttonX() + l.smallW() + l.gap(), l.thirdY(), l.smallW(), 29)) {
            client.setScreen(new MediaPlayerScreen()); return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}