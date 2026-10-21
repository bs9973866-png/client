package dev.nightfall.ui;

import dev.nightfall.Feature;
import dev.nightfall.FeatureRegistry;
import dev.nightfall.NightfallClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Low-glare, compact module dashboard. */
public final class ClickGuiScreen extends Screen {
    private Feature.Category selected = Feature.Category.VISUALS;
    private String search = "";
    private int scroll;
    private final long openedAt = System.currentTimeMillis();

    private static final int BG = 0xF111720D;
    private static final int SIDEBAR = 0xFF151C26;
    private static final int PANEL = 0xFF1A222D;
    private static final int CARD = 0xFF202A35;
    private static final int CARD_HOVER = 0xFF26333E;
    private static final int BORDER = 0xFF303D48;
    private static final int TEXT = 0xFFDCE5E8;
    private static final int MUTED = 0xFF8797A0;
    private static final int ACCENT = 0xFF79BDB5;
    private static final int ACCENT_DARK = 0xFF294641;

    public ClickGuiScreen() { super(Text.literal("NIGHTFALL")); }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int w = width, h = height;
        ctx.fill(0, 0, w, h, 0xA6070A0D);
        int pw = Math.min(740, w - 28), ph = Math.min(430, h - 24);
        if (pw < 360 || ph < 200) {
            drawText(ctx, "Увеличь разрешение экрана для ClickGUI", 14, 14, TEXT);
            return;
        }
        int x = (w - pw) / 2, y = (h - ph) / 2, sw = 158;
        ctx.fill(x + 3, y + 4, x + pw + 3, y + ph + 4, 0x50000000);
        ctx.fill(x, y, x + pw, y + ph, BG);
        ctx.fill(x, y, x + pw, y + 1, BORDER);
        ctx.fill(x, y + 1, x + sw, y + ph, SIDEBAR);
        ctx.fill(x, y, x + 3, y + ph, ACCENT);
        ctx.fill(x + sw, y + 1, x + sw + 1, y + ph - 1, BORDER);

        drawText(ctx, "NIGHTFALL", x + 18, y + 17, TEXT);
        drawText(ctx, "CLIENT  /  1.21.4", x + 18, y + 33, MUTED);
        ctx.fill(x + 16, y + 51, x + sw - 16, y + 52, BORDER);

        Feature.Category[] categories = Feature.Category.values();
        for (int i = 0; i < categories.length; i++) {
            Feature.Category category = categories[i];
            int cy = y + 64 + i * 32;
            boolean active = category == selected;
            if (active) {
                ctx.fill(x + 10, cy - 6, x + sw - 10, cy + 19, ACCENT_DARK);
                ctx.fill(x + 10, cy - 6, x + 12, cy + 19, ACCENT);
            }
            drawText(ctx, category.title(), x + 22, cy + 1, active ? TEXT : MUTED);
            String count = Integer.toString(FeatureRegistry.in(category).size());
            drawText(ctx, count, x + sw - 23 - textRenderer.getWidth(UiText.of(count)), cy + 1, active ? ACCENT : 0xFF596771);
        }

        int fx = x + 18, fy = y + ph - 47;
        ctx.fill(fx, fy, x + sw - 18, fy + 1, BORDER);
        drawText(ctx, "RIGHT SHIFT  ·  CLOSE", fx, fy + 9, MUTED);
        drawText(ctx, "CONFIG AUTO-SAVED", fx, fy + 23, 0xFF607D7D);

        int tx = x + sw + 18, right = x + pw - 18, contentW = right - tx;
        drawText(ctx, selected.title().toUpperCase(Locale.ROOT), tx, y + 17, ACCENT);
        String subtitle = "Локальные функции клиента и визуальные модули";
        drawText(ctx, subtitle, tx, y + 33, MUTED);

        int searchY = y + 51, searchW = Math.min(290, contentW);
        ctx.fill(tx, searchY, tx + searchW, searchY + 24, PANEL);
        ctx.fill(tx, searchY, tx + searchW, searchY + 1, BORDER);
        drawText(ctx, search.isEmpty() ? "ПОИСК МОДУЛЕЙ..." : "> " + search, tx + 9, searchY + 8, search.isEmpty() ? 0xFF61717B : TEXT);
        String state = FeatureRegistry.all().stream().filter(Feature::enabled).count() + " ACTIVE";
        drawText(ctx, state, right - textRenderer.getWidth(UiText.of(state)), searchY + 8, ACCENT);

        List<Feature> modules = FeatureRegistry.in(selected).stream()
                .filter(f -> search.isBlank()
                        || f.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.id().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.description().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        int listY = y + 87, footerY = y + ph - 27, cardH = 61, gapY = 8;
        int columns = contentW >= 390 ? 2 : 1;
        int cardGap = 9, cardW = (contentW - (columns - 1) * cardGap) / columns;
        int visibleRows = Math.max(1, (footerY - listY - 7) / (cardH + gapY));
        int rowCount = (int)Math.ceil(modules.size() / (double)columns);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rowCount - visibleRows)));

        ctx.enableScissor(tx, listY, right, footerY - 3);
        for (int i = 0; i < modules.size(); i++) {
            int col = i % columns, row = i / columns - scroll;
            if (row < 0 || row >= visibleRows) continue;
            Feature feature = modules.get(i);
            int bx = tx + col * (cardW + cardGap), by = listY + row * (cardH + gapY);
            boolean hovered = mx >= bx && mx <= bx + cardW && my >= by && my <= by + cardH;
            int bg = feature.enabled() ? 0xFF202D35 : (hovered ? CARD_HOVER : CARD);
            ctx.fill(bx, by, bx + cardW, by + cardH, bg);
            ctx.fill(bx, by, bx + cardW, by + 1, feature.enabled() ? ACCENT : BORDER);
            if (feature.enabled()) ctx.fill(bx, by + 1, bx + 2, by + cardH - 1, ACCENT);
            drawText(ctx, feature.name(), bx + 10, by + 9, feature.enabled() ? TEXT : 0xFFC3CDD1);

            String stateLabel = feature.enabled() ? "ON" : "OFF";
            int tw = textRenderer.getWidth(UiText.of(stateLabel));
            int pillX = bx + cardW - tw - 20, pillY = by + 7;
            ctx.fill(pillX, pillY, bx + cardW - 8, pillY + 15, feature.enabled() ? ACCENT_DARK : 0xFF2B353E);
            drawText(ctx, stateLabel, pillX + 6, pillY + 4, feature.enabled() ? ACCENT : MUTED);

            String desc = feature.description();
            int maxChars = Math.max(12, (cardW - 18) / 5);
            if (desc.length() > maxChars) desc = desc.substring(0, maxChars - 1) + "…";
            drawText(ctx, desc, bx + 10, by + 31, MUTED);
            drawText(ctx, feature.id().toUpperCase(Locale.ROOT), bx + 10, by + 46, 0xFF5B6B76);
        }
        ctx.disableScissor();

        if (modules.isEmpty()) drawText(ctx, "Ничего не найдено", tx + 2, listY + 8, MUTED);
        String footer = modules.size() + " MODULES  ·  CLICK CARD TO TOGGLE";
        drawText(ctx, footer, tx, footerY + 7, MUTED);
        String version = "EXO 2 UI";
        drawText(ctx, version, right - textRenderer.getWidth(UiText.of(version)), footerY + 7, 0xFF607D7D);
    }

    private void drawText(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(value), x, y, color);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int pw = Math.min(740, width - 28), ph = Math.min(430, height - 24);
        if (pw < 360 || ph < 200) return true;
        int x = (width - pw) / 2, y = (height - ph) / 2, sw = 158;
        for (int i = 0; i < Feature.Category.values().length; i++) {
            int cy = y + 64 + i * 32;
            if (mx >= x + 8 && mx <= x + sw - 8 && my >= cy - 8 && my <= cy + 21) {
                selected = Feature.Category.values()[i];
                scroll = 0;
                return true;
            }
        }
        int tx = x + sw + 18, right = x + pw - 18, contentW = right - tx;
        int listY = y + 87, footerY = y + ph - 27, cardH = 61, gapY = 8;
        int columns = contentW >= 390 ? 2 : 1, cardGap = 9, cardW = (contentW - (columns - 1) * cardGap) / columns;
        int visibleRows = Math.max(1, (footerY - listY - 7) / (cardH + gapY));
        List<Feature> modules = FeatureRegistry.in(selected).stream()
                .filter(f -> search.isBlank()
                        || f.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.id().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.description().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        for (int i = 0; i < modules.size(); i++) {
            int col = i % columns, row = i / columns - scroll;
            if (row < 0 || row >= visibleRows) continue;
            int bx = tx + col * (cardW + cardGap), by = listY + row * (cardH + gapY);
            if (mx >= bx && mx <= bx + cardW && my >= by && my <= by + cardH) {
                Feature f = modules.get(i);
                NightfallClient.setFeatureEnabled(f.id(), !f.enabled());
                return true;
            }
        }
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int)Math.signum(vertical));
        return true;
    }

    @Override public boolean charTyped(char chr, int modifiers) {
        if (!Character.isISOControl(chr) && search.length() < 32) {
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            search = "";
            scroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
}