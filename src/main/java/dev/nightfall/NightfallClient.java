package dev.nightfall;

import dev.nightfall.ui.ClickGuiScreen;
import dev.nightfall.ui.HudEditorScreen;
import dev.nightfall.ui.MediaPlayerScreen;
import dev.nightfall.ui.UiText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.Locale;

public final class NightfallClient implements ClientModInitializer {
    public static final String MOD_ID = "nightfall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyBinding clickGuiKey, zoomKey, hudEditorKey, mediaKey;
    private static double originalGamma = 1.0;
    private static int originalFov = 70, auraTicks, triggerTicks, trailTicks, airJumpTicks, totemDelay;
    private static long lastHitTime;
    private static boolean zoomWasActive, jumpWasPressed;

    @Override public void onInitializeClient() {
        FeatureRegistry.init();
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nightfall"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.nightfall"));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.hud_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.nightfall"));
        mediaKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nightfall.media_player", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.nightfall"));
        ConfigManager.load();
        FeatureBinds.load();
        HudLayout.load();
        MusicManager.init();
        WorldVisuals.register();
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
        while (hudEditorKey.wasPressed()) {
            client.setScreen(client.currentScreen instanceof HudEditorScreen ? null : new HudEditorScreen());
        }
        while (mediaKey.wasPressed()) {
            client.setScreen(client.currentScreen instanceof MediaPlayerScreen ? null : new MediaPlayerScreen());
        }
        FeatureBinds.tick(client);
        if (client.player == null || client.world == null) {
            restoreZoom(client);
            WorldVisuals.tick(client);
            return;
        }
        var player = client.player;
        WorldVisuals.tick(client);

        if (enabled("fullbright") && client.options.getGamma().getValue() < 90.0)
            client.options.getGamma().setValue(100.0);
        if (enabled("autosprint") && player.input.movementForward > 0.1f && !player.horizontalCollision)
            player.setSprinting(true);
        if (enabled("nofall")) player.fallDistance = 0.0f;
        if (enabled("antifire") && player.isOnFire()) player.extinguish();
        maintainTotem(client, player);

        if (enabled("speed") && player.isOnGround() && player.input.movementForward != 0.0f) {
            Vec3d v = player.getVelocity();
            double multiplier = client.isInSingleplayer() ? 1.085 : 1.02;
            player.setVelocity(v.x * multiplier, v.y, v.z * multiplier);
        }
        if (enabled("spider") && player.horizontalCollision && player.input.movementForward > 0.0f) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x, Math.max(v.y, 0.20), v.z);
        }
        boolean jumpPressed = client.options.jumpKey.isPressed();
        if (enabled("airjump") && jumpPressed && !jumpWasPressed && !player.isOnGround() && airJumpTicks <= 0) {
            player.jump();
            airJumpTicks = 7;
        }
        jumpWasPressed = jumpPressed;
        if (airJumpTicks > 0) airJumpTicks--;

        boolean zoom = enabled("zoom") && zoomKey.isPressed();
        if (zoom && !zoomWasActive) {
            originalFov = client.options.getFov().getValue();
            client.options.getFov().setValue(28);
        } else if (zoom && client.options.getFov().getValue() != 28) {
            client.options.getFov().setValue(28);
        } else if (!zoom && zoomWasActive) client.options.getFov().setValue(originalFov);
        zoomWasActive = zoom;

        // These helpers now select both player entities and mobs according to their module toggles.
        // The local singleplayer-only guard has intentionally been removed: multiplayer servers may
        // reject client-side automation or enforce their own reach/attack validation.
        if (client.currentScreen == null) {
            if (enabled("aimassist")) {
                double range = enabled("reach") ? 6.0 : 4.5;
                LivingEntity target = findTarget(client, range, 0.88);
                if (target != null) gentlyAim(player, target);
            }

            triggerTicks++;
            if (enabled("killaura")) {
                auraTicks++;
                double auraRange = enabled("reach") ? 5.0 : 3.6;
                LivingEntity target = findAuraTarget(client, auraRange);
                if (target != null) {
                    boolean aligned = rotateToTarget(player, target, 42.0f, 30.0f);
                    if (auraTicks >= 2 && aligned
                            && player.squaredDistanceTo(target) <= auraRange * auraRange
                            && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                        attack(client, target);
                        auraTicks = 0;
                    }
                } else {
                    auraTicks = 0;
                }
            } else {
                auraTicks = 0;
            }

            if (enabled("triggerbot") && triggerTicks >= 2 && client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity target && target != player && target.isAlive()
                    && shouldTarget(target) && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
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

    private static boolean shouldTarget(LivingEntity living) {
        if (living instanceof PlayerEntity) return enabled("targetplayers");
        return enabled("targetmobs");
    }

    private static LivingEntity findTarget(MinecraftClient client, double range, double minDot) {
        var player = client.player;
        if (player == null || client.world == null) return null;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        return client.world.getOtherEntities(player, player.getBoundingBox().expand(range), entity ->
                entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                    && shouldTarget(living)
                    && player.squaredDistanceTo(entity) <= range * range
                    && entity.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize().dotProduct(look) >= minDot)
            .stream().filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
            .min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
    }

    private static void maintainTotem(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (!enabled("autototem") || client.currentScreen != null || client.interactionManager == null) {
            totemDelay = 0;
            return;
        }
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            totemDelay = 0;
            return;
        }
        if (++totemDelay < 5) return;
        totemDelay = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int screenSlot = i < 9 ? i + 36 : i;
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, screenSlot, 40,
                        SlotActionType.SWAP, player);
                return;
            }
        }
    }

    /** Selects a visible target independently of the crosshair, so KillAura can acquire it on its own. */
    private static LivingEntity findAuraTarget(MinecraftClient client, double range) {
        var player = client.player;
        if (player == null || client.world == null) return null;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        return client.world.getOtherEntities(player, player.getBoundingBox().expand(range), entity ->
                        entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                                && shouldTarget(living) && player.canSee(living)
                                && player.squaredDistanceTo(living) <= range * range)
                .stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .min(Comparator.comparingDouble(target -> {
                    Vec3d direction = target.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize();
                    double dot = Math.max(-1.0, Math.min(1.0, look.dotProduct(direction)));
                    double angularCost = 1.0 - dot;
                    return angularCost * 2.4 + player.squaredDistanceTo(target) * 0.12;
                }))
                .orElse(null);
    }

    /** Smoothly turns the player's view toward the selected entity and reports when aim is close enough. */
    private static boolean rotateToTarget(net.minecraft.client.network.ClientPlayerEntity player,
                                          LivingEntity target, float maxYawStep, float maxPitchStep) {
        Vec3d delta = target.getBoundingBox().getCenter().subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - player.getPitch());
        player.setYaw(player.getYaw() + MathHelper.clamp(yawDelta, -maxYawStep, maxYawStep));
        player.setPitch(MathHelper.clamp(player.getPitch() + MathHelper.clamp(pitchDelta, -maxPitchStep, maxPitchStep), -90.0f, 90.0f));
        return Math.abs(MathHelper.wrapDegrees(targetYaw - player.getYaw())) <= 8.0f
                && Math.abs(targetPitch - player.getPitch()) <= 8.0f;
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

    private static void drawText(net.minecraft.client.gui.DrawContext ctx, MinecraftClient c, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(c.textRenderer, UiText.of(value), x, y, color);
    }

    private static int textWidth(MinecraftClient c, String value) {
        return c.textRenderer.getWidth(UiText.of(value));
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden) return;
        int w = c.getWindow().getScaledWidth(), h = c.getWindow().getScaledHeight();
        int white = 0xFFE1E9E9, muted = 0xFF8797A0, accent = 0xFF79BDB5, dark = 0xD910171E;
        if (enabled("hitpulse") && c.player.hurtTime > 0)
            ctx.fill(0, 0, w, h, (Math.min(22, c.player.hurtTime * 2) << 24) | 0x0000BBAA);

        if (enabled("watermark")) {
            int[] p = HudLayout.get("watermark", 9, 9); int x = p[0], y = p[1];
            ctx.fill(x, y, x + 117, y + 24, dark); ctx.fill(x, y, x + 2, y + 24, accent);
            drawText(ctx, c, "NIGHTFALL", x + 9, y + 3, white);
            drawText(ctx, c, "1.21.4  /  FABRIC", x + 9, y + 13, muted);
        }
        if (enabled("fps")) {
            String s = c.getCurrentFps() + " FPS";
            int[] p = HudLayout.get("fps", w - textWidth(c, s) - 12, 9); int x = p[0], y = p[1];
            ctx.fill(x - 6, y - 4, x + textWidth(c, s) + 4, y + 12, dark);
            drawText(ctx, c, s, x, y, accent);
        }
        if (enabled("coordinates")) {
            String s = String.format(Locale.ROOT, "X %.1f   Y %.1f   Z %.1f", c.player.getX(), c.player.getY(), c.player.getZ());
            int[] p = HudLayout.get("coordinates", 9, h - 25); int x = p[0], y = p[1];
            ctx.fill(x, y, x + textWidth(c, s) + 13, y + 16, dark);
            drawText(ctx, c, s, x + 6, y + 4, white);
        }
        if (enabled("customcrosshair")) {
            int x = w / 2, y = h / 2;
            ctx.fill(x - 7, y - 1, x - 3, y + 1, accent); ctx.fill(x + 3, y - 1, x + 7, y + 1, accent);
            ctx.fill(x - 1, y - 7, x + 1, y - 3, accent); ctx.fill(x - 1, y + 3, x + 1, y + 7, accent);
            ctx.fill(x - 1, y - 1, x + 1, y + 1, white);
        }
        if (enabled("keystrokes")) {
            String s = "W " + (c.options.forwardKey.isPressed() ? "■" : "□") + "   A " + (c.options.leftKey.isPressed() ? "■" : "□")
                    + "   S " + (c.options.backKey.isPressed() ? "■" : "□") + "   D " + (c.options.rightKey.isPressed() ? "■" : "□");
            int tw = textWidth(c, s);
            int[] p = HudLayout.get("keystrokes", w - tw - 18, h - 26); int x = p[0], y = p[1];
            ctx.fill(x, y, x + tw + 10, y + 16, dark);
            drawText(ctx, c, s, x + 5, y + 5, accent);
        }
        if (enabled("targethud") && c.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && living != c.player && living.isAlive()) {
            String name = living.getName().getString(), hp = String.format(Locale.ROOT, "HP %.1f / %.1f", living.getHealth(), living.getMaxHealth());
            int bw = Math.max(textWidth(c, name), textWidth(c, hp)) + 20;
            int[] p = HudLayout.get("targethud", (w - bw) / 2, h / 2 + 34); int x = p[0], y = p[1];
            ctx.fill(x, y, x + bw, y + 31, dark); ctx.fill(x, y, x + 2, y + 31, accent);
            drawText(ctx, c, name, x + 8, y + 5, white);
            drawText(ctx, c, hp, x + 8, y + 17, muted);
            ctx.fill(x + 8, y + 27, x + bw - 8, y + 29, 0xFF303B43);
            ctx.fill(x + 8, y + 27, x + 8 + (int)((bw - 16) * MathHelper.clamp(living.getHealth()/living.getMaxHealth(),0,1)), y + 29, accent);
        }
        if (enabled("hitmarker") && System.currentTimeMillis() - lastHitTime < 280) {
            int x = w/2, y = h/2;
            ctx.fill(x-8,y-8,x-5,y-5,white); ctx.fill(x+5,y-8,x+8,y-5,white);
            ctx.fill(x-8,y+5,x-5,y+8,white); ctx.fill(x+5,y+5,x+8,y+8,white);
        }
        if (enabled("speedometer")) {
            Vec3d v = c.player.getVelocity();
            double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
            String s = String.format(Locale.ROOT, "%.2f m/s", speed);
            int[] p = HudLayout.get("speedometer", 12, h - 39);
            drawText(ctx, c, s, p[0], p[1], accent);
        }
        if (enabled("totemcount")) {
            int count = c.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1 : 0;
            for (int i = 0; i < c.player.getInventory().size(); i++) {
                ItemStack stack = c.player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
            }
            int[] p = HudLayout.get("totemcount", 12, 37);
            drawText(ctx, c, "TOTEMS  " + count, p[0], p[1], count > 0 ? accent : muted);
        }
        if (enabled("armorhud")) {
            int[] p = HudLayout.get("armorhud", w / 2 - 72, h - 58); int x = p[0], y = p[1], index = 0;
            for (ItemStack stack : c.player.getArmorItems()) {
                if (stack.isEmpty()) continue;
                int durability = stack.isDamageable() ? (int)(100.0 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) : 100;
                ctx.fill(x + index * 38, y, x + index * 32 + 28, y + 14, dark);
                drawText(ctx, c, durability + "%", x + index * 38 + 3, y + 3, durability < 25 ? 0xFFD58B82 : accent);
                index++;
            }
        }
        if (enabled("potionhud")) {
            int[] p = HudLayout.get("potionhud", w - 150, 42); int x = p[0], y = p[1];
            for (StatusEffectInstance effect : c.player.getStatusEffects()) {
                String name = effect.getEffectType().value().getName().getString();
                int seconds = effect.getDuration() / 20;
                drawText(ctx, c, name + "  " + seconds + "s", x, y, muted);
                y += 11;
                if (y > 120) break;
            }
        }
        if (enabled("radar")) {
            int[] p = HudLayout.get("radar", w - 108, 38);
            int rx = p[0], ry = p[1], size = 90, centerX = rx + size / 2, centerY = ry + size / 2;
            ctx.fill(rx, ry, rx + size, ry + size, 0xD910171E);
            ctx.fill(rx, ry, rx + size, ry + 1, 0xFF38454C);
            ctx.fill(rx, ry, rx + 1, ry + size, accent);
            ctx.fill(centerX, ry + 10, centerX + 1, ry + size - 10, 0xFF303C45);
            ctx.fill(rx + 10, centerY, rx + size - 10, centerY + 1, 0xFF303C45);
            ctx.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, white);
            for (PlayerEntity other : c.world.getPlayers()) {
                if (other == c.player || other.isSpectator()) continue;
                double dx = other.getX() - c.player.getX(), dz = other.getZ() - c.player.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 32.0) continue;
                int px = centerX + (int)Math.round(dx * 1.05);
                int py = centerY - (int)Math.round(dz * 1.05);
                px = Math.max(rx + 4, Math.min(rx + size - 4, px));
                py = Math.max(ry + 4, Math.min(ry + size - 4, py));
                ctx.fill(px - 2, py - 2, px + 3, py + 3, 0xFF79BDB5);
            }
            drawText(ctx, c, "RADAR", rx + 6, ry + 5, muted);
        }
        if (enabled("mediaplayer")) {
            MusicManager.MediaState media = MusicManager.state();
            int[] p = HudLayout.get("mediaplayer", w - 260, 39); int x = p[0], y = p[1], bw = 248, bh = 45;
            ctx.fill(x, y, x + bw, y + bh, dark); ctx.fill(x, y, x + 2, y + bh, accent);
            String title = media.title().isBlank() ? "Открой Spotify / Яндекс Музыку" : media.title();
            String artist = media.artist().isBlank() ? media.status() : media.artist();
            while (textWidth(c, title) > bw - 18 && title.length() > 3) title = title.substring(0, title.length()-2) + "…";
            while (textWidth(c, artist) > bw - 18 && artist.length() > 3) artist = artist.substring(0, artist.length()-2) + "…";
            drawText(ctx, c, title, x + 9, y + 5, white);
            drawText(ctx, c, artist, x + 9, y + 17, muted);
            int progressWidth = media.durationMs() > 0 ? (int)((bw - 18) * Math.min(1.0, media.positionNow() / (double)media.durationMs())) : 0;
            ctx.fill(x + 9, y + 35, x + bw - 9, y + 37, 0xFF34434B);
            if (progressWidth > 0) ctx.fill(x + 9, y + 35, x + 9 + progressWidth, y + 37, accent);
        }
        if (enabled("worldlyrics")) {
            String lyric = MusicManager.currentLyric();
            if (!lyric.isBlank()) {
                int[] p = HudLayout.get("worldlyrics", w / 2 - 175, h - 57); int x = p[0], y = p[1], bw = 350;
                String line = lyric;
                while (textWidth(c, line) > bw - 20 && line.length() > 3) line = line.substring(0, line.length()-2) + "…";
                ctx.fill(x, y, x + bw, y + 25, 0xCC10171E);
                ctx.fill(x, y, x + 2, y + 25, accent);
                drawText(ctx, c, line, x + 10, y + 8, white);
            }
        }
    }
}
