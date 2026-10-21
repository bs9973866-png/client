package dev.nightfall.ui;

import dev.nightfall.HudLayout;
import dev.nightfall.NightfallClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Drag-and-drop visual editor for the NIGHTFALL overlay. */
public final class HudEditorScreen extends Screen {
    private static final int TEXT = 0xFFFFF0FE;
    private static final int MUTED = 0xFFB39AB8;
    private static final int ACCENT = 0xFFF05CCF;
    private static final int ACCENT_2 = 0xFFBE8BFF;
    private static final int PANEL = 0xE0160E20;
    private static final int BORDER = 0xFF513454;

    private record Widget(String id, String label, String module, int defaultX, int defaultY,
                          int width, int height) {}

    private final List<Widget> widgets;
    private Widget dragging;
    private int dragDx, dragDy;

    public HudEditorScreen() {
        super(Text.literal("NIGHTFALL HUD Studio"));
        int w = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight();
        widgets = List.of(
            new Widget("watermark", "NIGHTFALL / EXO UI", "watermark", 9, 9, 142, 32),
            new Widget("fps", "FPS 358 / PING 45 MS", "fps", w - 143, 10, 133, 23),
            new Widget("coordinates", "X 120.0  Y 64.0  Z -20.0", "coordinates", 9, h - 30, 170, 23),
            new Widget("keystrokes", "W A S D / LMB / RMB", "keystrokes", w - 150, h - 57, 137, 43),
            new Widget("mediaplayer", "NOW PLAYING / MEDIA", "mediaplayer", w - 290, 40, 278, 62),
            new Widget("worldlyrics", "LYRICS / current track line", "worldlyrics", w / 2 - 175, h - 57, 350, 29),
            new Widget("radar", "PLAYER RADAR", "radar", w - 108, 110, 92, 92),
            new Widget("targethud", "TARGET / 18.5 HP", "targethud", w / 2 - 80, h / 2 + 34, 160, 42),
            new Widget("speedometer", "SPEED 5.24 M/S", "speedometer", 12, h - 60, 120, 23),
            new Widget("totemcount", "TOTEMS  3", "totemcount", 12, 46, 105, 23),
            new Widget("armorhud", "ARMOR 100%", "armorhud", w / 2 - 75, h - 62, 156, 23),
            new Widget("potionhud", "ACTIVE EFFECTS", "potionhud", w - 160, 43, 148, 90)
        );
        HudLayout.load();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB9090610);
        // Faint drafting grid makes positioning easier without cluttering the preview.
        for (int gx = 0; gx < width; gx += 28) ctx.fill(gx, 48, gx + 1, height - 1, 0x181E1330);
        for (int gy = 48; gy < height; gy += 28) ctx.fill(0, gy, width, gy + 1, 0x181E1330);

        ctx.fill(0, 0, width, 43, PANEL);
        ctx.fill(0, 0, width, 2, ACCENT);
        ctx.fill(14, 10, 39, 34, 0xFF49213F);
        text(ctx, "N", 22, 17, ACCENT_2);
        text(ctx, "NIGHTFALL  /  HUD STUDIO", 49, 10, TEXT);
        text(ctx, "Drag widgets to move them. Right-click a widget to enable or disable it.", 49, 25, MUTED);

        int enabledCount = 0;
        for (Widget widget : widgets) {
            int[] p = HudLayout.get(widget.id(), widget.defaultX(), widget.defaultY());
            int x = clampX(p[0], widget.width());
            int y = clampY(p[1], widget.height());
            boolean hover = inside(mx, my, x, y, widget.width(), widget.height());
            boolean active = NightfallClient.enabled(widget.module());

            ctx.fill(x + 2, y + 3, x + widget.width() + 2, y + widget.height() + 3, 0x55000000);
            ctx.fill(x, y, x + widget.width(), y + widget.height(), active ? 0xE52A1735 : 0xD8171021);
            ctx.fill(x, y, x + widget.width(), y + 1, hover || active ? ACCENT : BORDER);
            ctx.fill(x, y + 1, x + 2, y + widget.height() - 1, active ? ACCENT : 0xFF765780);
            if (hover) {
                ctx.fill(x + widget.width() - 3, y, x + widget.width(), y + widget.height(), ACCENT_2);
            }

            String label = widget.label();
            int maxWidth = widget.width() - 15;
            while (!label.isEmpty() && textRenderer.getWidth(UiText.of(label)) > maxWidth) {
                label = label.substring(0, label.length() - 2) + "…";
            }
            text(ctx, label, x + 8, y + Math.max(5, (widget.height() - 8) / 2), active ? TEXT : MUTED);
            if (active) enabledCount++;
        }

        int footerY = height - 30;
        ctx.fill(12, footerY, width - 12, footerY + 19, PANEL);
        ctx.fill(12, footerY, width - 12, footerY + 1, BORDER);
        text(ctx, "DRAG: MOVE   /   RIGHT CLICK: TOGGLE   /   ESC: CLOSE",
                21, footerY + 6, MUTED);
        String count = enabledCount + " WIDGETS ACTIVE";
        text(ctx, count, width - textRenderer.getWidth(UiText.of(count)) - 22, footerY + 6, ACCENT_2);
    }

    private int clampX(int x, int widgetWidth) {
        return Math.max(0, Math.min(Math.max(0, width - widgetWidth), x));
    }

    private int clampY(int y, int widgetHeight) {
        return Math.max(48, Math.min(Math.max(48, height - 31 - widgetHeight), y));
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, UiText.of(s), x, y, color);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            return super.mouseClicked(mx, my, button);
        for (Widget widget : widgets) {
            int[] p = HudLayout.get(widget.id(), widget.defaultX(), widget.defaultY());
            int x = clampX(p[0], widget.width());
            int y = clampY(p[1], widget.height());
            if (!inside(mx, my, x, y, widget.width(), widget.height())) continue;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                NightfallClient.setFeatureEnabled(widget.module(), !NightfallClient.enabled(widget.module()));
                return true;
            }
            dragging = widget;
            dragDx = (int)mx - x;
            dragDy = (int)my - y;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            HudLayout.set(dragging.id(), clampX((int)mx - dragDx, dragging.width()),
                    clampY((int)my - dragDy, dragging.height()));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            HudLayout.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}