package dev.nightfall;

import dev.nightfall.ui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;

public final class NightfallClient implements ClientModInitializer {
    public static final String MOD_ID = "nightfall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyBinding clickGuiKey, zoomKey;
    private static double originalGamma = 1.0;
    private static int originalFov = 70, auraTicks, triggerTicks, trailTicks;
    private static long lastHitTime;
    private static boolean zoomWasActive;

    @Override public void onInitializeClient() {
        FeatureRegistry.init();
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nightfall"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.nightfall"));
        ConfigManager.load();
        ClientTickEvents.END_CLIENT_TICK.register(NightfallClient::tick);
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
        LOGGER.info("NIGHTFALL for Minecraft 1.21.4 loaded");
    }

    public static boolean enabled(String id) { return FeatureRegistry.enabled(id); }
    public static long lastHitTime() { return lastHitTime; }

    public static void setFeatureEnabled(String id, boolean enabled) {
        FeatureRegistry.find(id).ifPresent(feature -> {
            if (feature.enabled() == enabled) return;
            feature.setEnabledRaw(enabled);
            applyFeatureState(id, enabled);
            ConfigManager.save();
        });
    }

    static void applyFeatureState(String id, boolean active) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        if (id.equals("fullbright")) {
            if (active) {
                originalGamma = client.options.getGamma().getValue();
                client.options.getGamma().setValue(100.0);
            } else client.options.getGamma().setValue(originalGamma);
        }
    }

    private static void tick(MinecraftClient client) {
        while (clickGuiKey.wasPressed()) {
            client.setScreen(client.currentScreen instanceof ClickGuiScreen ? null : new ClickGuiScreen());
        }
        if (client.player == null || client.world == null) {
            restoreZoom(client);
            return;
        }
        var player = client.player;
        if (enabled("fullbright") && client.options.getGamma().getValue() < 90.0)
            client.options.getGamma().setValue(100.0);
        if (enabled("autosprint") && player.input.movementForward > 0.1f && !player.horizontalCollision)
            player.setSprinting(true);
        if (enabled("nofall")) player.fallDistance = 0.0f;
        if (enabled("speed") && client.isInSingleplayer() && player.isOnGround()
                && player.input.movementForward != 0.0f) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x * 1.06, v.y, v.z * 1.06);
        }
        boolean zoom = enabled("zoom") && zoomKey.isPressed();
        if (zoom && !zoomWasActive) {
            originalFov = client.options.getFov().getValue();
            client.options.getFov().setValue(25);
        } else if (zoom && client.options.getFov().getValue() != 25) {
            client.options.getFov().setValue(25);
        } else if (!zoom && zoomWasActive) client.options.getFov().setValue(originalFov);
        zoomWasActive = zoom;

        if (client.isInSingleplayer() && client.currentScreen == null) {
            if (enabled("aimassist")) {
                LivingEntity target = findTarget(client, 5.0, 0.90);
                if (target != null) gentlyAim(player, target);
            }
            auraTicks++; triggerTicks++;
            if (enabled("killaura") && auraTicks >= 10) {
                LivingEntity target = findTarget(client, 4.2, 0.965);
                if (target != null && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                    attack(client, target); auraTicks = 0;
                }
            }
            if (enabled("triggerbot") && triggerTicks >= 2 && client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity target && target != player && target.isAlive()
                    && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                attack(client, target); triggerTicks = 0;
            }
        }
        trailTicks++;
        if (enabled("trail") && trailTicks % 3 == 0 && player.getVelocity().horizontalLengthSquared() > 0.015)
            client.world.addParticle(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.12, player.getZ(), 0, 0.025, 0);
    }

    private static void restoreZoom(MinecraftClient client) {
        if (zoomWasActive && client.options != null) client.options.getFov().setValue(originalFov);
        zoomWasActive = false;
    }
    private static LivingEntity findTarget(MinecraftClient client, double range, double minDot) {
        var player = client.player;
        if (player == null || client.world == null) return null;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        return client.world.getOtherEntities(player, player.getBoundingBox().expand(range), entity ->
                entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                    && player.squaredDistanceTo(entity) <= range * range
                    && entity.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize().dotProduct(look) >= minDot)
            .stream().filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
            .min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
    }
    private static void gentlyAim(net.minecraft.client.network.ClientPlayerEntity player, LivingEntity target) {
        Vec3d d = target.getBoundingBox().getCenter().subtract(player.getEyePos());
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(d.y, horizontal));
        player.setYaw(player.getYaw() + MathHelper.wrapDegrees(yaw - player.getYaw()) * 0.12f);
        player.setPitch(player.getPitch() + MathHelper.wrapDegrees(pitch - player.getPitch()) * 0.08f);
    }
    private static void attack(MinecraftClient client, LivingEntity target) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        lastHitTime = System.currentTimeMillis();
        if (enabled("hitspark")) {
            Vec3d p = target.getBoundingBox().getCenter();
            for (int i = 0; i < 8; i++) {
                double vx = (Math.random() - 0.5) * 0.16, vy = Math.random() * 0.16, vz = (Math.random() - 0.5) * 0.16;
                client.world.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, vx, vy, vz);
            }
        }
    }
    private static void renderHud(net.minecraft.client.gui.DrawContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden) return;
        int w = c.getWindow().getScaledWidth(), h = c.getWindow().getScaledHeight();
        int white = 0xFFF2F4FF, muted = 0xFF9DA3B8, cyan = 0xFF5CE1E6, purple = 0xFFB75CFF;
        if (enabled("hitpulse") && c.player.hurtTime > 0)
            ctx.fill(0, 0, w, h, (Math.min(36, c.player.hurtTime * 3) << 24) | 0x00FF354F);
        if (enabled("watermark")) {
            ctx.fill(9, 9, 112, 31, 0xB90C0E18); ctx.fill(9, 9, 12, 31, purple);
            ctx.drawTextWithShadow(c.textRenderer, "NIGHTFALL", 18, 12, white);
            ctx.drawTextWithShadow(c.textRenderer, "1.21.4 / FABRIC", 18, 22, muted);
        }
        if (enabled("fps")) {
            String s = c.getCurrentFps() + " FPS"; int x = w - c.textRenderer.getWidth(s) - 12;
            ctx.fill(x - 6, 9, w - 8, 25, 0xB90C0E18); ctx.drawTextWithShadow(c.textRenderer, s, x, 13, cyan);
        }
        if (enabled("coordinates")) {
            String s = String.format(java.util.Locale.ROOT, "X %.1f  Y %.1f  Z %.1f", c.player.getX(), c.player.getY(), c.player.getZ());
            ctx.fill(9, h - 25, 9 + c.textRenderer.getWidth(s) + 13, h - 9, 0xB90C0E18);
            ctx.drawTextWithShadow(c.textRenderer, s, 15, h - 21, white);
        }
        if (enabled("customcrosshair")) {
            int x = w / 2, y = h / 2;
            ctx.fill(x - 8, y - 1, x - 3, y + 1, cyan); ctx.fill(x + 3, y - 1, x + 8, y + 1, cyan);
            ctx.fill(x - 1, y - 8, x + 1, y - 3, cyan); ctx.fill(x - 1, y + 3, x + 1, y + 8, cyan);
            ctx.fill(x - 1, y - 1, x + 1, y + 1, white);
        }
        if (enabled("keystrokes")) {
            String s = "W " + (c.options.forwardKey.isPressed() ? "■" : "□") + "  A " + (c.options.leftKey.isPressed() ? "■" : "□")
                    + "  S " + (c.options.backKey.isPressed() ? "■" : "□") + "  D " + (c.options.rightKey.isPressed() ? "■" : "□");
            ctx.fill(w - c.textRenderer.getWidth(s) - 14, h - 26, w - 8, h - 10, 0xB90C0E18);
            ctx.drawTextWithShadow(c.textRenderer, s, w - c.textRenderer.getWidth(s) - 11, h - 21, cyan);
        }
        if (enabled("targethud") && c.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && living != c.player && living.isAlive()) {
            String name = living.getName().getString(), hp = String.format(java.util.Locale.ROOT, "HP %.1f / %.1f", living.getHealth(), living.getMaxHealth());
            int bw = Math.max(c.textRenderer.getWidth(name), c.textRenderer.getWidth(hp)) + 20, x = (w - bw) / 2, y = h / 2 + 34;
            ctx.fill(x, y, x + bw, y + 31, 0xD50C0E18); ctx.fill(x, y, x + 2, y + 31, purple);
            ctx.drawTextWithShadow(c.textRenderer, name, x + 8, y + 5, white);
            ctx.drawTextWithShadow(c.textRenderer, hp, x + 8, y + 17, muted);
            ctx.fill(x + 8, y + 27, x + bw - 8, y + 29, 0xFF383A4D);
            ctx.fill(x + 8, y + 27, x + 8 + (int)((bw - 16) * MathHelper.clamp(living.getHealth()/living.getMaxHealth(),0,1)), y + 29, cyan);
        }
        if (enabled("hitmarker") && System.currentTimeMillis() - lastHitTime < 280) {
            int x = w/2, y = h/2; ctx.fill(x-8,y-8,x-5,y-5,white); ctx.fill(x+5,y-8,x+8,y-5,white);
            ctx.fill(x-8,y+5,x-5,y+8,white); ctx.fill(x+5,y+5,x+8,y+8,white);
        }
    }
}
