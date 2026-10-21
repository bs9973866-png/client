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
    private static final int PANEL = 0xF0130D20;
    private static final int CARD = 0xF5251933;
    private static final int CARD_HOVER = 0xFF382044;
    private static final int BORDER = 0xFF65416C;
    private static final int TEXT = 0xFFFFF5FE;
    private static final int MUTED = 0xFFBDA7C4;
    private static final int ACCENT = 0xFFFF78D7;
    private static final int ACCENT2 = 0xFFB8A1FF;
    private static final int MINT = 0xFF9CE8DA;
    private static final Identifier PORTRAIT = Identifier.of("nightfall", "textures/gui/anime_portrait.jpg");

    private record Layout(int x, int y, int w, int h, int leftW, int rightX, int rightW,
                          int buttonX, int buttonW, int mainY, int mainH, int gap,
                          int smallW, int secondY, int thirdY, int featureY, int hintY) {}

    public NightfallMainMenu() {
        super(Text.literal("NIGHTFALL"));
    }

    private Layout layout() {
        int w = Math.min(960, Math.max(1, width - 28));
        int h = Math.min(540, Math.max(1, height - 20));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int leftW = Math.min(286, Math.max(218, w / 3));
        int rightX = x + leftW + 24;
        int rightW = Math.max(1, x + w - 22 - rightX);
        int buttonX = rightX;
        int buttonW = rightW;
        int mainY = y + 120;
        int mainH = 39;
        int gap = 9;
        int smallW = Math.max(1, (buttonW - gap) / 2);
        int secondY = mainY + mainH * 2 + gap + 3;
        int thirdY = secondY + 30 + 8;
        int featureY = thirdY + 30 + 14;
        int hintY = y + h - 58;
        return new Layout(x, y, w, h, leftW, rightX, rightW, buttonX, buttonW,
                mainY, mainH, gap, smallW, secondY, thirdY, featureY, hintY);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        ctx.fillGradient(0, 0, width, height, 0xFF0A0714, 0xFF28132F);
        drawBackdrop(ctx, now);

        Layout l = layout();
        if (l.w() < 640 || l.h() < 390) {
            centered(ctx, "Увеличь окно, чтобы открыть NIGHTFALL", width / 2, height / 2 - 4, TEXT);
            centered(ctx, "ESC — вернуться в меню Minecraft", width / 2, height / 2 + 14, MUTED);
            return;
        }

        // Soft shadow, rounded shell and a candy-colour highlight.
        roundedFill(ctx, l.x() + 6, l.y() + 8, l.w(), l.h(), 0x65000000, 26);
        roundedFill(ctx, l.x(), l.y(), l.w(), l.h(), PANEL, 25);
        roundedFill(ctx, l.x() + 18, l.y() + 1, l.w() - 36, 3, ACCENT, 2);
        roundedFill(ctx, l.x() + 22, l.y() + 5, l.w() - 44, 1, 0x775CE2D5, 1);
        roundedFill(ctx, l.x() + l.leftW(), l.y() + 20, 2, l.h() - 40, 0xAA55345D, 1);
        roundedFill(ctx, l.x() + l.leftW() + 1, l.y() + 32, 1, l.h() - 64, 0x5548D9D0, 1);

        drawBrandAndMascot(ctx, l, now);

        // Main heading and a living status pill.
        int pulse = (int)(150 + 70 * (0.5 + 0.5 * Math.sin(now * 0.003)));
        draw(ctx, "ТВОЙ МИР. ТВОЙ СТИЛЬ.", l.rightX(), l.y() + 26, mix(ACCENT, ACCENT2, (float)(0.5 + 0.5 * Math.sin(now * 0.0018))));
        draw(ctx, "Мягкий неон, мультяшные визуалы и музыка в игре.", l.rightX(), l.y() + 45, MUTED);

        roundedFill(ctx, l.rightX(), l.y() + 65, l.rightW(), 37, 0xE9231730, 14);
        roundedFill(ctx, l.rightX() + 9, l.y() + 74, 19, 19, 0xFF49304F, 9);
        drawSparkle(ctx, l.rightX() + 14, l.y() + 79, 9, ACCENT);
        draw(ctx, "CARTOON VISUALS", l.rightX() + 37, l.y() + 72, TEXT);
        draw(ctx, "LIVE HUD  /  PEARL FOLLOW  /  MUSIC", l.rightX() + 37, l.y() + 85, MUTED);
        roundedFill(ctx, l.rightX() + l.rightW() - 67, l.y() + 75, 54, 17, 0xFF23483F, 9);
        roundedFill(ctx, l.rightX() + l.rightW() - 60, l.y() + 81, 5, 5, MINT, 3);
        draw(ctx, "READY", l.rightX() + l.rightW() - 49, l.y() + 79, MINT);

        menuButton(ctx, l.buttonX(), l.mainY(), l.buttonW(), l.mainH(),
                "ОДИНОЧНАЯ ИГРА", mx, my, true, now, 0);
        menuButton(ctx, l.buttonX(), l.mainY() + l.mainH() + l.gap(), l.buttonW(), l.mainH(),
                "СЕТЕВАЯ ИГРА", mx, my, false, now, 1);

        menuButton(ctx, l.buttonX(), l.secondY(), l.smallW(), 30,
                "CLICKGUI", mx, my, false, now, 2);
        menuButton(ctx, l.buttonX() + l.smallW() + l.gap(), l.secondY(), l.smallW(), 30,
                "HUD STUDIO", mx, my, false, now, 3);
        menuButton(ctx, l.buttonX(), l.thirdY(), l.smallW(), 30,
                "НАСТРОЙКИ", mx, my, false, now, 4);
        menuButton(ctx, l.buttonX() + l.smallW() + l.gap(), l.thirdY(), l.smallW(), 30,
                "МУЗЫКА", mx, my, false, now, 5);

        // Three rounded mood cards make the feature set visible at a glance.
        int cardGap = 8;
        int featureW = Math.max(1, (l.rightW() - cardGap * 2) / 3);
        int featureH = Math.max(48, l.hintY() - l.featureY() - 12);
        moodCard(ctx, l.rightX(), l.featureY(), featureW, featureH, "CARTOON", "SOFT GLOW", ACCENT, now, 0);
        moodCard(ctx, l.rightX() + featureW + cardGap, l.featureY(), featureW, featureH, "PEARL FOLLOW", "SMART AIM", ACCENT2, now, 1);
        moodCard(ctx, l.rightX() + (featureW + cardGap) * 2, l.featureY(), featureW, featureH, "MUSIC HUD", "SYNC LYRICS", MINT, now, 2);

        roundedFill(ctx, l.rightX(), l.hintY(), l.rightW(), 29, 0xB51B1327, 12);
        draw(ctx, "RIGHT SHIFT  /  CLICKGUI", l.rightX() + 11, l.hintY() + 10, MUTED);
        String hudTip = "RIGHT CTRL  /  HUD EDITOR";
        int hudW = textRenderer.getWidth(UiText.of(hudTip));
        draw(ctx, hudTip, l.rightX() + l.rightW() - hudW - 11, l.hintY() + 10, ACCENT2);

        String build = "NIGHTFALL  •  FABRIC 1.21.4";
        draw(ctx, build, l.x() + l.w() - textRenderer.getWidth(UiText.of(build)) - 20,
                l.y() + l.h() - 20, 0xFF9C80AE);
    }

    private void drawBackdrop(DrawContext ctx, long now) {
        for (int i = 0; i < 12; i++) {
            int size = 15 + (i % 4) * 7;
            int x = (int)Math.floorMod(i * 127L + now / (25L + i % 5), Math.max(1, width));
            int y = (int)Math.floorMod(i * 83L + now / (38L + i % 7), Math.max(1, height));
            int alpha = 8 + (int)(10 * (0.5 + 0.5 * Math.sin(now * 0.0017 + i)));
            int colour = (alpha << 24) | (i % 2 == 0 ? 0x00D77ED8 : 0x008B83FF);
            roundedFill(ctx, x, y, size, size, colour, size / 2);
            if (i % 3 == 0) drawSparkle(ctx, x + size / 2, y + size / 2, 4 + i % 3, 0x558FECF7);
        }
        for (int i = 0; i < 24; i++) {
            int x = (int)Math.floorMod(i * 139L + now / (18L + i % 5), Math.max(1, width));
            int y = (int)Math.floorMod(i * 73L + now / (30L + i % 7), Math.max(1, height));
            int alpha = 25 + (int)(55 * (0.5 + 0.5 * Math.sin(now * 0.002 + i * 1.37)));
            int size = i % 6 == 0 ? 4 : 2;
            roundedFill(ctx, x, y, size, size, (alpha << 24) | 0x00FF96DE, size / 2);
        }
    }

    private void drawBrandAndMascot(DrawContext ctx, Layout l, long now) {
        roundedFill(ctx, l.x() + 18, l.y() + 18, 53, 53, 0xFF512448, 19);
        roundedFill(ctx, l.x() + 24, l.y() + 21, 41, 3, ACCENT, 2);
        drawSparkle(ctx, l.x() + 44, l.y() + 40, 12, TEXT);
        centered(ctx, "N", l.x() + 44, l.y() + 45, TEXT);
        draw(ctx, "NIGHTFALL", l.x() + 82, l.y() + 25, TEXT);
        draw(ctx, "CUTE CHAOS EDITION", l.x() + 82, l.y() + 43, ACCENT2);
        roundedFill(ctx, l.x() + 20, l.y() + 79, l.leftW() - 40, 23, 0x992D203D, 11);
        draw(ctx, "MOONLIGHT  /  1.21.4", l.x() + 31, l.y() + 87, MUTED);

        int portraitSize = Math.min(l.leftW() - 48, l.h() - 177);
        int portraitX = l.x() + (l.leftW() - portraitSize) / 2;
        int portraitY = l.y() + 111;
        roundedFill(ctx, portraitX - 6, portraitY - 6, portraitSize + 12, portraitSize + 12, 0xFF75456F, 19);
        roundedFill(ctx, portraitX - 2, portraitY - 2, portraitSize + 4, portraitSize + 4, 0xFF33213E, 16);
        roundedFill(ctx, portraitX, portraitY, portraitSize, portraitSize, CARD, 14);
        ctx.drawTexture(RenderLayer::getGuiTextured, PORTRAIT, portraitX, portraitY,
                0.0f, 0.0f, portraitSize, portraitSize, 128, 128);
        maskTextureCorners(ctx, portraitX, portraitY, portraitSize, portraitSize, 14, CARD);
        roundedFill(ctx, portraitX + 9, portraitY + portraitSize - 34, portraitSize - 18, 25, 0xE5160D21, 10);
        drawSparkle(ctx, portraitX + 18, portraitY + portraitSize - 26, 6, ACCENT);
        draw(ctx, "STAY IN THE NIGHT", portraitX + 30, portraitY + portraitSize - 25, TEXT);

        int stickerY = portraitY + portraitSize + 12;
        roundedFill(ctx, l.x() + 23, stickerY, l.leftW() - 46, 30, 0xB52A1D39, 14);
        drawHeart(ctx, l.x() + 33, stickerY + 9, ACCENT);
        draw(ctx, "VISUALS WITH PERSONALITY", l.x() + 51, stickerY + 11, TEXT);
        int floatX = portraitX + portraitSize - 1 + (int)(Math.sin(now * 0.002) * 3);
        drawSparkle(ctx, floatX, portraitY + 18 + (int)(Math.sin(now * 0.0025) * 5), 11, ACCENT2);
        drawHeart(ctx, portraitX + 8, portraitY + portraitSize / 2 + (int)(Math.sin(now * 0.0018) * 4), MINT);
        draw(ctx, "made for the night owls", l.x() + 24, l.y() + l.h() - 22, MUTED);
    }

    private void moodCard(DrawContext ctx, int x, int y, int w, int h,
                          String title, String sub, int accent, long now, int index) {
        roundedFill(ctx, x + 2, y + 3, w, h, 0x44000000, 13);
        roundedFill(ctx, x, y, w, h, 0xE622172F, 13);
        roundedFill(ctx, x + 10, y + 8, 25, 25, 0xFF3A2948, 10);
        drawSparkle(ctx, x + 22, y + 20, 7 + index, accent);
        roundedFill(ctx, x + 11, y + h - 5, w - 22, 2, 0x665D456B, 1);
        double wave = 0.5 + 0.5 * Math.sin(now * 0.003 + index * 1.4);
        roundedFill(ctx, x + 11, y + h - 5, Math.max(2, (int)((w - 22) * wave)), 2, accent, 1);
        draw(ctx, title, x + 10, y + 39, TEXT);
        draw(ctx, sub, x + 10, y + 52, accent);
    }

    private void menuButton(DrawContext ctx, int x, int y, int w, int h, String label,
                            int mx, int my, boolean primary, long now, int index) {
        boolean hover = inside(mx, my, x, y, w, h);
        int lift = hover ? 2 : 0;
        roundedFill(ctx, x + 2, y + 4 - lift, w, h, 0x55000000, 16);
        int bg = primary
                ? (hover ? 0xFFFF86DE : 0xFFCB58BA)
                : (hover ? CARD_HOVER : CARD);
        roundedFill(ctx, x, y - lift, w, h, bg, 15);
        roundedFill(ctx, x + 9, y + 2 - lift, w - 18, 2,
                primary || hover ? ACCENT : BORDER, 1);
        if (primary || hover) {
            int glow = 30 + (int)(22 * (0.5 + 0.5 * Math.sin(now * 0.004 + index)));
            roundedFill(ctx, x + 7, y + h - 5 - lift, w - 14, 2, (glow << 24) | 0x00FFB2E8, 1);
        }
        if (index == 0) drawPlayIcon(ctx, x + 17, y + h / 2 - 5 - lift, primary ? TEXT : ACCENT2);
        else if (index == 1) drawNetworkIcon(ctx, x + 16, y + h / 2 - 6 - lift, hover ? TEXT : ACCENT2);
        else drawSparkle(ctx, x + 18, y + h / 2 - 1 - lift, 5, hover ? ACCENT : ACCENT2);

        int labelWidth = textRenderer.getWidth(UiText.of(label));
        centered(ctx, label, x + w / 2 + (index < 2 ? 5 : 0), y + (h - 8) / 2 - lift, primary || hover ? TEXT : MUTED);
        if (w > 220 && index < 2) {
            String hint = index == 0 ? "LOCAL WORLD" : "JOIN A SERVER";
            draw(ctx, hint, x + w - textRenderer.getWidth(UiText.of(hint)) - 14, y + (h - 8) / 2 - lift, 0xBFFFF5FE);
        }
    }

    private void drawPlayIcon(DrawContext ctx, int x, int y, int colour) {
        roundedFill(ctx, x - 5, y - 6, 19, 19, 0x55341245, 9);
        ctx.fill(x, y - 4, x + 3, y + 7, colour);
        ctx.fill(x + 3, y - 2, x + 6, y + 5, colour);
        ctx.fill(x + 6, y, x + 9, y + 3, colour);
    }

    private void drawNetworkIcon(DrawContext ctx, int x, int y, int colour) {
        roundedFill(ctx, x - 5, y - 5, 20, 20, 0x55341245, 9);
        roundedFill(ctx, x + 2, y - 3, 5, 5, colour, 3);
        roundedFill(ctx, x - 3, y + 4, 5, 5, colour, 3);
        roundedFill(ctx, x + 7, y + 4, 5, 5, colour, 3);
        ctx.fill(x - 1, y + 1, x + 4, y + 3, colour);
        ctx.fill(x + 5, y + 1, x + 9, y + 3, colour);
    }

    private void drawSparkle(DrawContext ctx, int cx, int cy, int size, int colour) {
        roundedFill(ctx, cx - Math.max(1, size / 5), cy - size, Math.max(2, size / 2), size * 2, colour, 2);
        roundedFill(ctx, cx - size, cy - Math.max(1, size / 5), size * 2, Math.max(2, size / 2), colour, 2);
        roundedFill(ctx, cx - size / 3, cy - size / 3, Math.max(2, size * 2 / 3), Math.max(2, size * 2 / 3), TEXT, 2);
    }

    private void drawHeart(DrawContext ctx, int x, int y, int colour) {
        roundedFill(ctx, x, y, 5, 5, colour, 2);
        roundedFill(ctx, x + 5, y, 5, 5, colour, 2);
        roundedFill(ctx, x + 1, y + 3, 8, 4, colour, 2);
        roundedFill(ctx, x + 3, y + 6, 4, 3, colour, 2);
    }

    private void maskTextureCorners(DrawContext ctx, int x, int y, int w, int h, int radius, int mask) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            if (edge >= r) continue;
            double dy = r - edge - 0.5;
            int inset = Math.max(0, (int)Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy))));
            if (inset > 0) {
                ctx.fill(x, y + row, x + inset, y + row + 1, mask);
                ctx.fill(x + w - inset, y + row, x + w, y + row + 1, mask);
            }
        }
    }

    private void draw(DrawContext ctx, String s, int x, int y, int colour) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(s), x, y, colour);
    }

    private void centered(DrawContext ctx, String s, int cx, int y, int colour) {
        draw(ctx, s, cx - textRenderer.getWidth(UiText.of(s)) / 2, y, colour);
    }

    private int mix(int from, int to, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int r = Math.round(((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = Math.round(((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = Math.round((from & 255) * (1 - t) + (to & 255) * t);
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private void roundedFill(DrawContext ctx, int x, int y, int w, int h, int colour, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            int inset = 0;
            if (edge < r) {
                double dy = r - edge - 0.5;
                inset = Math.max(0, (int)Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy))));
            }
            ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, colour);
        }
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        Layout l = layout();
        if (l.w() < 640 || l.h() < 390) return true;
        if (inside(mx, my, l.buttonX(), l.mainY(), l.buttonW(), l.mainH())) {
            client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (inside(mx, my, l.buttonX(), l.mainY() + l.mainH() + l.gap(), l.buttonW(), l.mainH())) {
            client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (inside(mx, my, l.buttonX(), l.secondY(), l.smallW(), 30)) {
            client.setScreen(new ClickGuiScreen());
            return true;
        }
        if (inside(mx, my, l.buttonX() + l.smallW() + l.gap(), l.secondY(), l.smallW(), 30)) {
            client.setScreen(new HudEditorScreen());
            return true;
        }
        if (inside(mx, my, l.buttonX(), l.thirdY(), l.smallW(), 30)) {
            client.setScreen(new OptionsScreen(this, client.options));
            return true;
        }
        if (inside(mx, my, l.buttonX() + l.smallW() + l.gap(), l.thirdY(), l.smallW(), 30)) {
            client.setScreen(new MediaPlayerScreen());
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
