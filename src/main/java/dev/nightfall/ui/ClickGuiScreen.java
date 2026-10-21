package dev.nightfall.ui;

import dev.nightfall.Feature;
import dev.nightfall.FeatureBinds;
import dev.nightfall.FeatureRegistry;
import dev.nightfall.FeatureSettings;
import dev.nightfall.NightfallClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** NIGHTFALL's card-based, rose-violet ClickGUI. */
public final class ClickGuiScreen extends Screen {
    private static final int BG = 0xF10D0918;
    private static final int SIDEBAR = 0xFF171023;
    private static final int PANEL = 0xFF21172E;
    private static final int CARD = 0xFF2A1C36;
    private static final int CARD_HOVER = 0xFF3A2448;
    private static final int CARD_ON = 0xFF372043;
    private static final int BORDER = 0xFF654267;
    private static final int TEXT = 0xFFFFF0FE;
    private static final int MUTED = 0xFFB39AB8;
    private static final int ACCENT = 0xFFFF78D7;
    private static final int ACCENT_2 = 0xFFB8A1FF;
    private static final int ACCENT_DARK = 0xFF71385E;
    private static final Identifier ANIME_PORTRAIT = Identifier.of("nightfall", "textures/gui/anime_portrait.jpg");

    private Feature.Category selected = Feature.Category.COMBAT;
    private String search = "";
    private String selectedModuleId = "killaura";
    private int scroll;

    private record Geometry(int x, int y, int pw, int ph, int sideW, int detailW,
                            int mainX, int mainRight, int listY, int footerY,
                            int cardH, int gapY, int columns, int cardGap,
                            int cardW, int visibleRows) {}

    public ClickGuiScreen() {
        super(Text.literal("NIGHTFALL"));
    }

    private Geometry geometry() {
        int pw = Math.min(960, width - 28);
        int ph = Math.min(560, height - 24);
        int x = (width - pw) / 2, y = (height - ph) / 2;
        int sideW = pw >= 900 ? 170 : 148;
        int detailW = Math.min(238, Math.max(190, pw / 4));
        int mainX = x + sideW + 17;
        int mainRight = x + pw - detailW - 27;
        int listY = y + 92, footerY = y + ph - 31;
        int cardH = 65, gapY = 8, cardGap = 9;
        int contentW = mainRight - mainX;
        int columns = contentW >= 370 ? 2 : 1;
        int cardW = Math.max(130, (contentW - (columns - 1) * cardGap) / columns);
        int visibleRows = Math.max(1, (footerY - listY - 3) / (cardH + gapY));
        return new Geometry(x, y, pw, ph, sideW, detailW, mainX, mainRight, listY,
                footerY, cardH, gapY, columns, cardGap, cardW, visibleRows);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        ctx.fillGradient(0, 0, width, height, 0xD0090615, 0xE9160A24);
        for (int i = 0; i < 28; i++) {
            int sx = (int)Math.floorMod(i * 137L + now / (22L + i % 5), Math.max(1, width));
            int sy = (int)Math.floorMod(i * 79L + now / (31L + i % 7), Math.max(1, height));
            int pulse = (int)(22 + 22 * (0.5 + 0.5 * Math.sin(now * 0.002 + i)));
            roundedFill(ctx, sx, sy, 2 + i % 2, 2 + i % 2, (pulse << 24) | 0x00EF6CDE, 2);
        }
        for (int i = 0; i < 10; i++) {
            int size = 12 + (i % 4) * 5;
            int sx = (int)Math.floorMod(i * 163L + now / (29L + i % 6), Math.max(1, width));
            int sy = (int)Math.floorMod(i * 97L + now / (37L + i % 7), Math.max(1, height));
            int alpha = 8 + (int)(12 * (0.5 + 0.5 * Math.sin(now * 0.0016 + i * 0.9)));
            roundedFill(ctx, sx, sy, size, size, (alpha << 24) | 0x00B88DFF, size / 2);
        }

        Geometry g = geometry();
        if (g.pw < 590 || g.ph < 350) {
            drawText(ctx, "Увеличь разрешение экрана для NIGHTFALL ClickGUI", 14, 14, TEXT);
            return;
        }

        int x = g.x, y = g.y, right = x + g.pw, bottom = y + g.ph;
        roundedFill(ctx, x + 4, y + 5, g.pw, g.ph, 0x65000000, 12);
        roundedFill(ctx, x, y, g.pw, g.ph, BG, 22);
        ctx.fill(x, y, right, y + 1, BORDER);
        ctx.fill(x, y, x + 3, bottom, ACCENT);
        roundedFill(ctx, x + 3, y + 1, g.sideW - 3, g.ph - 2, SIDEBAR, 18);
        ctx.fill(x + g.sideW, y + 1, x + g.sideW + 1, bottom - 1, BORDER);

        // Brand block.
        roundedFill(ctx, x + 18, y + 18, 28, 28, ACCENT_DARK, 8);
        ctx.fill(x + 18, y + 18, x + 46, y + 20, ACCENT);
        drawText(ctx, "N", x + 27, y + 26, TEXT);
        drawText(ctx, "NIGHTFALL", x + 55, y + 20, TEXT);
        drawText(ctx, "CARTOON UI  /  1.21.4", x + 55, y + 36, MUTED);
        ctx.fill(x + 17, y + 58, x + g.sideW - 16, y + 59, BORDER);

        Feature.Category[] categories = Feature.Category.values();
        for (int i = 0; i < categories.length; i++) {
            Feature.Category category = categories[i];
            int cy = y + 76 + i * 43;
            boolean active = category == selected;
            if (active) {
                roundedFill(ctx, x + 10, cy, g.sideW - 20, 32, ACCENT_DARK, 10);
                ctx.fill(x + 10, cy, x + 13, cy + 32, ACCENT);
                ctx.fill(x + 13, cy + 1, x + g.sideW - 11, cy + 2, 0xFF79405F);
            }
            int chip = active ? 0xFF6B2B5C : 0xFF261A30;
            ctx.fill(x + 18, cy + 7, x + 39, cy + 27, chip);
            drawCategoryGlyph(ctx, category, x + 20, cy + 8, active ? ACCENT : MUTED);
            drawText(ctx, category.title(), x + 48, cy + 12, active ? TEXT : MUTED);
            long on = FeatureRegistry.in(category).stream().filter(Feature::enabled).count();
            if (on > 0) {
                String count = Long.toString(on);
                ctx.fill(x + g.sideW - 29, cy + 9, x + g.sideW - 17, cy + 23, ACCENT_DARK);
                drawText(ctx, count, x + g.sideW - 26, cy + 12, ACCENT_2);
            }
        }

        int profileY = bottom - 69;
        int artX = x + 8, artY = y + 337, artW = g.sideW - 16, artH = Math.min(145, profileY - artY - 8);
        if (artH > 60) {
            ctx.fill(artX - 2, artY - 2, artX + artW + 2, artY + artH + 2, BORDER);
            ctx.fill(artX, artY, artX + artW, artY + artH, PANEL);
            ctx.drawTexture(RenderLayer::getGuiTextured, ANIME_PORTRAIT, artX, artY,
                    0.0f, 0.0f, artW, artH, 128, 128);
            ctx.fill(artX, artY + artH - 25, artX + artW, artY + artH, 0xD90A0712);
            ctx.fill(artX, artY + artH - 25, artX + artW, artY + artH - 23, ACCENT);
            drawText(ctx, "STAY IN THE NIGHT", artX + 7, artY + artH - 16, TEXT);
        }
        ctx.fill(x + 16, profileY, x + g.sideW - 16, profileY + 1, BORDER);
        ctx.fill(x + 18, profileY + 12, x + 43, profileY + 37, 0xFF38213F);
        drawText(ctx, "NF", x + 21, profileY + 20, ACCENT_2);
        drawText(ctx, "PREMIUM UI", x + 50, profileY + 13, TEXT);
        drawText(ctx, "CONFIG AUTO-SAVED", x + 50, profileY + 27, MUTED);
        drawText(ctx, "RIGHT SHIFT  /  CLOSE", x + 18, bottom - 17, 0xFF866A8D);

        // Main heading and search.
        int mainX = g.mainX;
        drawText(ctx, selected.title().toUpperCase(Locale.ROOT), mainX, y + 17, ACCENT);
        drawText(ctx, "Выбери модуль. ПКМ открывает его настройки.", mainX, y + 34, MUTED);
        int searchY = y + 52, searchW = Math.min(280, g.mainRight - mainX - 95);
        roundedFill(ctx, mainX, searchY, searchW, 25, PANEL, 5);
        ctx.fill(mainX, searchY, mainX + searchW, searchY + 1, BORDER);
        drawText(ctx, search.isBlank() ? "ПОИСК МОДУЛЕЙ..." : "SEARCH  " + search,
                mainX + 9, searchY + 9, search.isBlank() ? 0xFF8F7798 : TEXT);
        String activeCount = FeatureRegistry.all().stream().filter(Feature::enabled).count() + " ON";
        drawText(ctx, activeCount, g.mainRight - textRenderer.getWidth(UiText.of(activeCount)), searchY + 9, ACCENT_2);

        List<Feature> modules = filteredModules();
        if (modules.stream().noneMatch(f -> f.id().equals(selectedModuleId))) {
            selectedModuleId = modules.isEmpty() ? "" : modules.get(0).id();
        }

        int visibleCount = g.columns * g.visibleRows;
        int maxScroll = Math.max(0, (int)Math.ceil(modules.size() / (double)g.columns) - g.visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        ctx.enableScissor(g.mainX, g.listY, g.mainRight, g.footerY - 3);
        for (int i = 0; i < modules.size(); i++) {
            int col = i % g.columns;
            int row = i / g.columns - scroll;
            if (row < 0 || row >= g.visibleRows) continue;
            Feature feature = modules.get(i);
            int bx = g.mainX + col * (g.cardW + g.cardGap);
            int by = g.listY + row * (g.cardH + g.gapY);
            boolean hovered = inside(mx, my, bx, by, g.cardW, g.cardH);
            if (hovered) selectedModuleId = feature.id();
            boolean selectedCard = feature.id().equals(selectedModuleId);
            int cardColor = feature.enabled() ? CARD_ON : (hovered ? CARD_HOVER : CARD);
            roundedFill(ctx, bx, by, g.cardW, g.cardH, cardColor, 14);
            float glowPulse = (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.004 + i * 0.65));
            int edge = feature.enabled()
                    ? mixColor(ACCENT, ACCENT_2, glowPulse * 0.55f)
                    : (selectedCard ? ACCENT_2 : BORDER);
            roundedFill(ctx, bx + 4, by, g.cardW - 8, 2, feature.enabled() || selectedCard ? edge : BORDER, 1);
            roundedFill(ctx, bx, by + 5, 3, g.cardH - 10,
                    feature.enabled() ? edge : (selectedCard ? ACCENT_2 : BORDER), 1);

            roundedFill(ctx, bx + 9, by + 9, 24, 24, 0xFF3B2444, 11);
            roundedFill(ctx, bx + 12, by + 9, 18, 2, feature.enabled() ? edge : BORDER, 1);
            drawModuleGlyph(ctx, feature.id(), bx + 11, by + 11, feature.enabled() ? ACCENT : ACCENT_2);
            drawText(ctx, feature.name(), bx + 40, by + 8, TEXT);
            String desc = feature.description();
            int maxChars = Math.max(10, (g.cardW - 61) / 5);
            if (desc.length() > maxChars) desc = desc.substring(0, maxChars - 1) + "…";
            drawText(ctx, desc, bx + 40, by + 27, MUTED);
            drawText(ctx, "BIND  " + FeatureBinds.label(feature.id()), bx + 10, by + 46,
                    FeatureBinds.isBound(feature.id()) ? ACCENT_2 : 0xFF886F90);

            int tx = bx + g.cardW - 42, ty = by + 8;
            roundedFill(ctx, tx, ty, 31, 15, feature.enabled() ? ACCENT_DARK : 0xFF3A2A41, 8);
            roundedFill(ctx, tx + (feature.enabled() ? 17 : 3), ty + 3, 10, 9,
                    feature.enabled() ? ACCENT : 0xFF9A83A4, 4);
            drawText(ctx, feature.enabled() ? "ON" : "OFF", bx + g.cardW - 41, by + 49,
                    feature.enabled() ? ACCENT : MUTED);
        }
        ctx.disableScissor();

        if (modules.isEmpty()) drawText(ctx, "Ничего не найдено", mainX + 3, g.listY + 9, MUTED);
        String footer = modules.size() + " MODULES   /   LMB: TOGGLE   /   RMB: SETTINGS";
        drawText(ctx, footer, mainX, g.footerY + 8, MUTED);

        // Live module detail card on the right.
        int dx = g.mainRight + 13, dy = g.listY, dw = x + g.pw - 15 - dx, dh = g.footerY - g.listY - 3;
        roundedFill(ctx, dx, dy, dw, dh, PANEL, 14);
        ctx.fill(dx, dy, dx + dw, dy + 1, BORDER);
        ctx.fill(dx, dy, dx + 2, dy + dh, ACCENT);
        drawText(ctx, "MODULE DETAILS", dx + 12, dy + 12, ACCENT_2);

        Feature chosen = FeatureRegistry.find(selectedModuleId).orElse(null);
        if (chosen != null) {
            drawText(ctx, chosen.name(), dx + 12, dy + 34, TEXT);
            drawText(ctx, chosen.enabled() ? "ENABLED" : "DISABLED", dx + 12, dy + 51,
                    chosen.enabled() ? ACCENT : MUTED);
            String desc = chosen.description();
            int charW = Math.max(13, (dw - 24) / 5);
            int textY = dy + 73;
            for (String line : wrap(desc, charW, 3)) {
                drawText(ctx, line, dx + 12, textY, MUTED);
                textY += 13;
            }
            ctx.fill(dx + 12, textY + 3, dx + dw - 12, textY + 4, BORDER);
            textY += 16;
            drawText(ctx, "SETTINGS", dx + 12, textY, ACCENT);
            textY += 15;
            List<FeatureSettings.Spec> specs = FeatureSettings.forModule(chosen.id());
            if (specs.isEmpty()) {
                drawText(ctx, "Нет дополнительных", dx + 12, textY, MUTED);
                drawText(ctx, "параметров.", dx + 12, textY + 13, MUTED);
            } else {
                int shown = Math.min(specs.size(), 4);
                for (int i = 0; i < shown && textY < dy + dh - 64; i++) {
                    FeatureSettings.Spec spec = specs.get(i);
                    String value = FeatureSettings.displayValue(spec);
                    drawText(ctx, spec.label(), dx + 12, textY, MUTED);
                    int valWidth = textRenderer.getWidth(UiText.of(value));
                    drawText(ctx, value, dx + dw - valWidth - 12, textY, TEXT);
                    if (spec.type() == FeatureSettings.Type.SLIDER) {
                        int barX = dx + 12, barY = textY + 11, barW = dw - 24;
                        ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF49334F);
                        double range = Math.max(0.0001, spec.max() - spec.min());
                        double fraction = (FeatureSettings.number(chosen.id(), settingName(spec)) - spec.min()) / range;
                        int filled = (int)(barW * Math.max(0, Math.min(1, fraction)));
                        ctx.fill(barX, barY, barX + filled, barY + 3, ACCENT);
                        textY += 7;
                    }
                    textY += 22;
                }
            }
            int bindY = dy + dh - 47;
            ctx.fill(dx + 10, bindY, dx + dw - 10, bindY + 34, ACCENT_DARK);
            drawText(ctx, "BIND  " + FeatureBinds.label(chosen.id()), dx + 17, bindY + 7, TEXT);
            drawText(ctx, "ПКМ ПО КАРТОЧКЕ", dx + 17, bindY + 20, ACCENT_2);
        } else {
            drawText(ctx, "Выбери модуль", dx + 12, dy + 40, MUTED);
        }

        String version = "NIGHTFALL  /  1.21.4";
        drawText(ctx, version, right - textRenderer.getWidth(UiText.of(version)) - 14, bottom - 18, 0xFF886E91);
    }

    private List<Feature> filteredModules() {
        return FeatureRegistry.in(selected).stream()
                .filter(f -> search.isBlank()
                        || f.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.id().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || f.description().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private void roundedFill(DrawContext ctx, int x, int y, int width, int height, int color, int radius) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < height; row++) {
            int edgeY = Math.min(row, height - 1 - row);
            int inset = 0;
            if (edgeY < r) {
                double dy = r - edgeY - 0.5;
                inset = Math.max(0, (int)Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy))));
            }
            ctx.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    private int mixColor(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = Math.round(((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = Math.round((from & 255) * (1 - t) + (to & 255) * t);
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private void drawCategoryGlyph(DrawContext ctx, Feature.Category category, int x, int y, int color) {
        switch (category) {
            case COMBAT -> {
                ctx.fill(x + 2, y + 12, x + 5, y + 15, color);
                ctx.fill(x + 5, y + 9, x + 8, y + 12, color);
                ctx.fill(x + 8, y + 6, x + 11, y + 9, color);
                ctx.fill(x + 11, y + 3, x + 15, y + 7, color);
                ctx.fill(x + 4, y + 8, x + 9, y + 10, color);
            }
            case MOVEMENT -> {
                ctx.fill(x + 8, y + 2, x + 10, y + 17, color);
                ctx.fill(x + 4, y + 4, x + 8, y + 8, color);
                ctx.fill(x + 10, y + 4, x + 14, y + 8, color);
                ctx.fill(x + 4, y + 11, x + 8, y + 15, color);
                ctx.fill(x + 10, y + 11, x + 14, y + 15, color);
            }
            case VISUALS -> {
                ctx.fill(x + 2, y + 8, x + 6, y + 5, color);
                ctx.fill(x + 6, y + 5, x + 12, y + 5, color);
                ctx.fill(x + 12, y + 5, x + 16, y + 8, color);
                ctx.fill(x + 2, y + 8, x + 6, y + 12, color);
                ctx.fill(x + 6, y + 12, x + 12, y + 12, color);
                ctx.fill(x + 12, y + 12, x + 16, y + 8, color);
                ctx.fill(x + 7, y + 7, x + 11, y + 11, color);
            }
            case HUD -> {
                ctx.fill(x + 2, y + 2, x + 7, y + 7, color);
                ctx.fill(x + 10, y + 2, x + 15, y + 7, color);
                ctx.fill(x + 2, y + 10, x + 7, y + 15, color);
                ctx.fill(x + 10, y + 10, x + 15, y + 15, color);
            }
            case WORLD -> {
                ctx.fill(x + 5, y + 2, x + 12, y + 3, color);
                ctx.fill(x + 2, y + 5, x + 3, y + 12, color);
                ctx.fill(x + 14, y + 5, x + 15, y + 12, color);
                ctx.fill(x + 5, y + 14, x + 12, y + 15, color);
                ctx.fill(x + 5, y + 5, x + 12, y + 12, color);
                ctx.fill(x + 7, y + 7, x + 10, y + 10, 0xFF28182F);
            }
            case PLAYER -> {
                ctx.fill(x + 6, y + 2, x + 11, y + 7, color);
                ctx.fill(x + 4, y + 8, x + 13, y + 11, color);
                ctx.fill(x + 3, y + 11, x + 5, y + 16, color);
                ctx.fill(x + 12, y + 11, x + 14, y + 16, color);
            }
        }
    }

    private void drawModuleGlyph(DrawContext ctx, String id, int x, int y, int color) {
        if (id.equals("mediaplayer") || id.equals("worldlyrics")) {
            ctx.fill(x + 3, y + 3, x + 5, y + 15, color);
            ctx.fill(x + 5, y + 3, x + 12, y + 5, color);
            ctx.fill(x + 10, y + 4, x + 12, y + 13, color);
            ctx.fill(x + 2, y + 12, x + 8, y + 16, color);
            return;
        }
        if (id.equals("killaura") || id.equals("triggerbot") || id.equals("hitspark")) {
            drawCategoryGlyph(ctx, Feature.Category.COMBAT, x, y, color);
            return;
        }
        if (id.equals("aimassist") || id.equals("targethud") || id.equals("customcrosshair")) {
            ctx.fill(x + 10, y + 2, x + 12, y + 8, color);
            ctx.fill(x + 10, y + 14, x + 12, y + 20, color);
            ctx.fill(x + 2, y + 10, x + 8, y + 12, color);
            ctx.fill(x + 14, y + 10, x + 20, y + 12, color);
            ctx.fill(x + 8, y + 8, x + 14, y + 14, color);
            ctx.fill(x + 9, y + 9, x + 13, y + 13, 0xFF21142A);
            return;
        }
        drawCategoryGlyph(ctx, FeatureRegistry.find(id).map(Feature::category).orElse(Feature.Category.VISUALS), x, y, color);
    }

    private String categoryCode(Feature.Category category) {
        return switch (category) {
            case COMBAT -> "CB";
            case MOVEMENT -> "MV";
            case VISUALS -> "VS";
            case HUD -> "UI";
            case WORLD -> "WR";
            case PLAYER -> "PL";
        };
    }

    private List<String> wrap(String value, int widthChars, int maxLines) {
        if (value == null || value.isBlank()) return List.of();
        String[] words = value.split("\\s+");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() > 0 && line.length() + word.length() + 1 > widthChars) {
                lines.add(line.toString());
                line.setLength(0);
                if (lines.size() >= maxLines) break;
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (lines.size() < maxLines && line.length() > 0) lines.add(line.toString());
        if (lines.size() == maxLines && !lines.isEmpty()) {
            int last = lines.size() - 1;
            String s = lines.get(last);
            if (s.length() > 2) lines.set(last, s.substring(0, s.length() - 1) + "…");
        }
        return lines;
    }

    private String settingName(FeatureSettings.Spec spec) {
        int dot = spec.key().indexOf('.');
        return dot < 0 ? spec.key() : spec.key().substring(dot + 1);
    }

    private void drawText(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(value), x, y, color);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            return super.mouseClicked(mx, my, button);
        Geometry g = geometry();
        if (g.pw < 590 || g.ph < 350) return true;
        int x = g.x, y = g.y;

        for (int i = 0; i < Feature.Category.values().length; i++) {
            int cy = y + 76 + i * 43;
            if (inside(mx, my, x + 8, cy, g.sideW - 16, 32)) {
                selected = Feature.Category.values()[i];
                scroll = 0;
                List<Feature> next = filteredModules();
                selectedModuleId = next.isEmpty() ? "" : next.get(0).id();
                return true;
            }
        }

        List<Feature> modules = filteredModules();
        for (int i = 0; i < modules.size(); i++) {
            int col = i % g.columns, row = i / g.columns - scroll;
            if (row < 0 || row >= g.visibleRows) continue;
            int bx = g.mainX + col * (g.cardW + g.cardGap);
            int by = g.listY + row * (g.cardH + g.gapY);
            if (inside(mx, my, bx, by, g.cardW, g.cardH)) {
                Feature f = modules.get(i);
                selectedModuleId = f.id();
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    client.setScreen(new ModuleSettingsScreen(f.id(), this));
                } else {
                    NightfallClient.setFeatureEnabled(f.id(), !f.enabled());
                }
                return true;
            }
        }

        // The detail panel is interactive too, not just a visual preview.
        int dx = g.mainRight + 13, dy = g.listY, dw = x + g.pw - 15 - dx;
        int dh = g.footerY - g.listY - 3;
        Feature chosen = FeatureRegistry.find(selectedModuleId).orElse(null);
        if (chosen != null && inside(mx, my, dx, dy, dw, dh)) {
            int textY = dy + 73 + wrap(chosen.description(), Math.max(13, (dw - 24) / 5), 3).size() * 13 + 16 + 15;
            List<FeatureSettings.Spec> specs = FeatureSettings.forModule(chosen.id());
            for (int i = 0; i < Math.min(specs.size(), 4); i++) {
                FeatureSettings.Spec spec = specs.get(i);
                if (textY > dy + dh - 64) break;
                if (inside(mx, my, dx + 8, textY - 4, dw - 16, 22)) {
                    switch (spec.type()) {
                        case TOGGLE -> FeatureSettings.toggle(spec);
                        case MODE -> FeatureSettings.nextMode(spec);
                        case SLIDER -> {
                            int barX = dx + 12, barW = dw - 24;
                            double fraction = Math.max(0, Math.min(1, (mx - barX) / (double) barW));
                            FeatureSettings.setNumber(spec, spec.min() + fraction * (spec.max() - spec.min()));
                        }
                    }
                    return true;
                }
                if (spec.type() == FeatureSettings.Type.SLIDER) textY += 7;
                textY += 22;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int)Math.signum(vertical));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!Character.isISOControl(chr) && search.length() < 32) {
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}