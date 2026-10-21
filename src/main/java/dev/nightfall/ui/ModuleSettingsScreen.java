package dev.nightfall.ui;

import dev.nightfall.Feature;
import dev.nightfall.FeatureBinds;
import dev.nightfall.FeatureRegistry;
import dev.nightfall.FeatureSettings;
import dev.nightfall.NightfallClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Per-module options screen. Opened using right-click on a module card. */
public final class ModuleSettingsScreen extends Screen {
    private static final int PANEL = 0xF10A0712, CARD = 0xFF24182E, BORDER = 0xFF4B3154;
    private static final int TEXT = 0xFFFFF0FE, MUTED = 0xFFB39AB8, ACCENT = 0xFFF05CCF, ACCENT_BG = 0xFF572447;
    private final String featureId;
    private final Screen parent;
    private boolean waitingForBind;
    private FeatureSettings.Spec draggingSlider;

    public ModuleSettingsScreen(String featureId, Screen parent) {
        super(Text.literal("NIGHTFALL Module Settings"));
        this.featureId = featureId;
        this.parent = parent;
    }

    private Feature feature() { return FeatureRegistry.find(featureId).orElse(null); }
    private List<FeatureSettings.Spec> specs() { return FeatureSettings.forModule(featureId); }
    private int panelW() { return Math.min(520, width - 28); }
    private int panelH() { return Math.min(height - 20, 135 + specs().size() * 43); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2; }
    private int settingsY() { return panelY() + 99; }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB807060B);
        Feature feature = feature();
        if (feature == null) { draw(ctx, "Модуль не найден", 16, 16, TEXT); return; }
        int x = panelX(), y = panelY(), pw = panelW(), ph = panelH();
        ctx.fill(x + 3, y + 4, x + pw + 3, y + ph + 4, 0x52000000);
        ctx.fill(x, y, x + pw, y + ph, PANEL);
        ctx.fill(x, y, x + pw, y + 1, BORDER);
        ctx.fill(x, y, x + 3, y + ph, ACCENT);
        draw(ctx, feature.name().toUpperCase(Locale.ROOT), x + 18, y + 15, TEXT);
        draw(ctx, feature.description(), x + 18, y + 31, MUTED);
        button(ctx, x + pw - 88, y + 12, 70, 22, "← НАЗАД", mx, my, false);

        int bindY = y + 61;
        ctx.fill(x + 18, bindY, x + pw - 18, bindY + 29, CARD);
        draw(ctx, "БИНД МОДУЛЯ", x + 27, bindY + 10, MUTED);
        button(ctx, x + 160, bindY + 4, 150, 21,
                waitingForBind ? "НАЖМИ КЛАВИШУ / MOUSE 3" : FeatureBinds.label(featureId), mx, my, waitingForBind);
        button(ctx, x + pw - 120, bindY + 4, 98, 21,
                feature.enabled() ? "ВКЛЮЧЁН" : "ВЫКЛЮЧЕН", mx, my, feature.enabled());

        List<FeatureSettings.Spec> settings = specs();
        if (settings.isEmpty()) draw(ctx, "У этого модуля пока нет дополнительных настроек.", x + 20, y + 114, MUTED);
        for (int i = 0; i < settings.size(); i++) {
            FeatureSettings.Spec spec = settings.get(i);
            int sx = x + 18, sy = settingsY() + i * 43, sw = pw - 36;
            ctx.fill(sx, sy, sx + sw, sy + 39, CARD);
            ctx.fill(sx, sy, sx + 2, sy + 39, draggingSlider == spec ? ACCENT : BORDER);
            draw(ctx, spec.label(), sx + 10, sy + 7, TEXT);
            switch (spec.type()) {
                case TOGGLE -> {
                    String label = FeatureSettings.bool(spec) ? "ON" : "OFF";
                    button(ctx, sx + sw - 68, sy + 7, 56, 22, label, mx, my, FeatureSettings.bool(spec));
                }
                case MODE -> {
                    String label = FeatureSettings.displayValue(spec);
                    int bw = Math.min(145, Math.max(82, textRenderer.getWidth(UiText.of(label)) + 22));
                    button(ctx, sx + sw - bw - 9, sy + 7, bw, 22, label, mx, my, true);
                }
                case SLIDER -> {
                    String label = FeatureSettings.displayValue(spec);
                    draw(ctx, label, sx + sw - textRenderer.getWidth(UiText.of(label)) - 10, sy + 7, ACCENT);
                    int barX = sx + 165, barW = Math.max(75, sw - 185), barY = sy + 28;
                    ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF40384A);
                    double v = FeatureSettings.number(featureId, nameOf(spec));
                    double fraction = (v - spec.min()) / Math.max(0.00001, spec.max() - spec.min());
                    int fillW = (int)Math.round(barW * Math.max(0, Math.min(1, fraction)));
                    ctx.fill(barX, barY, barX + fillW, barY + 3, ACCENT);
                    ctx.fill(barX + fillW - 2, barY - 2, barX + fillW + 3, barY + 5, ACCENT);
                }
            }
        }
        draw(ctx, waitingForBind
                ? "Нажми клавишу или колесо мыши; Backspace — снять бинд; Esc — отмена"
                : "ЛКМ меняет настройку  •  ESC — назад  •  автосохранение",
                x + 18, y + ph - 17, MUTED);
    }

    private String nameOf(FeatureSettings.Spec spec) {
        int dot = spec.key().indexOf('.');
        return dot < 0 ? spec.key() : spec.key().substring(dot + 1);
    }

    private void draw(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(value), x, y, color);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean active) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        ctx.fill(x, y, x + w, y + h, active ? ACCENT_BG : CARD);
        ctx.fill(x, y, x + w, y + 1, hover || active ? ACCENT : BORDER);
        int tw = textRenderer.getWidth(UiText.of(label));
        draw(ctx, label, x + Math.max(4, (w - tw) / 2), y + (h - 8) / 2, hover || active ? TEXT : MUTED);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (waitingForBind) {
            FeatureBinds.setMouseButton(featureId, button);
            waitingForBind = false;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            return super.mouseClicked(mx, my, button);
        Feature feature = feature();
        if (feature == null) return true;
        int x = panelX(), y = panelY(), pw = panelW();
        if (inside(mx, my, x + pw - 88, y + 12, 70, 22) || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            client.setScreen(parent);
            return true;
        }
        int bindY = y + 61;
        if (inside(mx, my, x + 160, bindY + 4, 150, 21)) { waitingForBind = true; return true; }
        if (inside(mx, my, x + pw - 120, bindY + 4, 98, 21)) {
            NightfallClient.setFeatureEnabled(featureId, !feature.enabled());
            return true;
        }
        List<FeatureSettings.Spec> all = specs();
        for (int i = 0; i < all.size(); i++) {
            FeatureSettings.Spec spec = all.get(i);
            int sx = x + 18, sy = settingsY() + i * 43, sw = pw - 36;
            if (!inside(mx, my, sx, sy, sw, 39)) continue;
            switch (spec.type()) {
                case TOGGLE -> FeatureSettings.toggle(spec);
                case MODE -> FeatureSettings.nextMode(spec);
                case SLIDER -> {
                    draggingSlider = spec;
                    updateSlider(spec, mx, sx, sw);
                }
            }
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateSlider(draggingSlider, mx, panelX() + 18, panelW() - 36);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (draggingSlider != null) { draggingSlider = null; return true; }
        return super.mouseReleased(mx, my, button);
    }

    private void updateSlider(FeatureSettings.Spec spec, double mx, int sx, int sw) {
        int barX = sx + 165, barW = Math.max(75, sw - 185);
        double fraction = Math.max(0, Math.min(1, (mx - barX) / barW));
        FeatureSettings.setNumber(spec, spec.min() + fraction * (spec.max() - spec.min()));
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForBind) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { waitingForBind = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                FeatureBinds.set(featureId, GLFW.GLFW_KEY_UNKNOWN);
                waitingForBind = false;
                return true;
            }
            FeatureBinds.set(featureId, keyCode);
            waitingForBind = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}
