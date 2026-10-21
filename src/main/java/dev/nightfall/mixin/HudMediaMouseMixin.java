package dev.nightfall.mixin;

import dev.nightfall.HudLayout;
import dev.nightfall.MusicManager;
import dev.nightfall.NightfallClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the transport buttons inside the in-game media HUD receive mouse clicks. */
@Mixin(Mouse.class)
public abstract class HudMediaMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void nightfall$handleHudMediaButtons(long windowHandle, int button, int action, int modifiers,
                                                 CallbackInfo ci) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.currentScreen != null
                || client.options == null || client.options.hudHidden
                || !NightfallClient.enabled("mediaplayer")) return;

        Window window = client.getWindow();
        int scaledWidth = window.getScaledWidth();
        int scaledHeight = window.getScaledHeight();
        if (window.getWidth() <= 0 || window.getHeight() <= 0) return;
        int mouseX = (int)(client.mouse.getX() * scaledWidth / (double)window.getWidth());
        int mouseY = (int)(client.mouse.getY() * scaledHeight / (double)window.getHeight());

        int[] position = HudLayout.get("mediaplayer", scaledWidth - 290, 40);
        int x = position[0], y = position[1], panelWidth = 278;
        int buttonY = y + 39, buttonWidth = 22, buttonHeight = 13;
        int firstX = x + panelWidth - 76;

        if (inside(mouseX, mouseY, firstX, buttonY, buttonWidth, buttonHeight)) {
            MusicManager.command("previous");
            ci.cancel();
        } else if (inside(mouseX, mouseY, firstX + 25, buttonY, buttonWidth, buttonHeight)) {
            MusicManager.command("toggle");
            ci.cancel();
        } else if (inside(mouseX, mouseY, firstX + 50, buttonY, buttonWidth, buttonHeight)) {
            MusicManager.command("next");
            ci.cancel();
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}