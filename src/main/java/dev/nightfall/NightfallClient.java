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
import net.minecraft.client.render.RenderLayer;
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
import net.minecraft.util.Identifier;
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
    public static final Identifier ANIME_PORTRAIT = Identifier.of("nightfall", "textures/gui/anime_portrait.png");
    private static KeyBinding clickGuiKey, zoomKey, hudEditorKey, mediaKey;
    private static double originalGamma = 1.0;
    private static int originalFov = 70, auraTicks, triggerTicks, trailTicks, airJumpTicks, totemDelay;
    private static long lastHitTime;
    private static boolean zoomWasActive, jumpWasPressed;

    @Override public void onInitializeClient() {
        FeatureRegistry.init();
        FeatureSettings.load();
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
                client.options.getGamma().setValue(FeatureSettings.number("fullbright", "gamma"));
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
            client.options.getGamma().setValue(FeatureSettings.number("fullbright", "gamma"));
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
            client.options.getFov().setValue((int) FeatureSettings.number("zoom", "fov"));
        } else if (zoom && client.options.getFov().getValue() != (int) FeatureSettings.number("zoom", "fov")) {
            client.options.getFov().setValue((int) FeatureSettings.number("zoom", "fov"));
        } else if (!zoom && zoomWasActive) client.options.getFov().setValue(originalFov);
        zoomWasActive = zoom;

        // These helpers now select both player entities and mobs according to their module toggles.
        // The local singleplayer-only guard has intentionally been removed: multiplayer servers may
        // reject client-side automation or enforce their own reach/attack validation.
        if (client.currentScreen == null) {
            if (enabled("aimassist") && !enabled("killaura")) {
                double range = enabled("reach") ? 6.0 : 4.5;
                LivingEntity target = findTarget(client, range, 0.88);
                if (target != null) gentlyAim(player, target);
            }

            triggerTicks++;
            if (enabled("killaura")) {
                auraTicks++;
                double configuredRange = FeatureSettings.number("killaura", "range");
                double auraRange = enabled("reach") ? Math.max(5.0, configuredRange) : configuredRange;
                LivingEntity target = findAuraTarget(client, auraRange);
                if (target != null) {
                    float rotationSpeed = (float) FeatureSettings.number("killaura", "rotationSpeed");
                    boolean snap = FeatureSettings.mode("killaura", "rotationMode").equals("Snap");
                    boolean aligned = rotateToTarget(player, target, snap ? 180.0f : rotationSpeed,
                            snap ? 180.0f : Math.max(3.0f, rotationSpeed * 0.7f));
                    int attackDelay = (int) FeatureSettings.number("killaura", "attackDelay");
                    boolean criticalAllowed = !FeatureSettings.mode("killaura", "criticalMode").equals("Natural Crits")
                            || isNaturalCriticalFall(player);
                    if (auraTicks >= attackDelay && aligned && criticalAllowed
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
        int configuredSwapDelay = Math.max(1, (int) FeatureSettings.number("autototem", "swapDelay"));
        if (++totemDelay < configuredSwapDelay) return;
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

    private static boolean isNaturalCriticalFall(net.minecraft.client.network.ClientPlayerEntity player) {
        return !player.isOnGround() && player.fallDistance > 0.0f && player.getVelocity().y < -0.05
                && !player.isClimbing() && !player.isTouchingWater() && !player.isInLava()
                && !player.isSprinting() && !player.isGliding();
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

    private static void hudPanel(net.minecraft.client.gui.DrawContext ctx, int x, int y,
                                  int width, int height, int accent) {
        ctx.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x55000000);
        ctx.fill(x, y, x + width, y + height, 0xD9150D20);
        ctx.fill(x, y, x + width, y + 1, 0xFF573452);
        ctx.fill(x, y + 1, x + 2, y + height - 1, accent);
    }

    private static void drawKey(net.minecraft.client.gui.DrawContext ctx, MinecraftClient client,
                                String label, int x, int y, int width, boolean active) {
        ctx.fill(x, y, x + width, y + 16, active ? 0xFF6A285A : 0xFF2A1B35);
        ctx.fill(x, y, x + width, y + 1, active ? 0xFFF05CCF : 0xFF4A3150);
        int labelWidth = textWidth(client, label);
        drawText(ctx, client, label, x + Math.max(2, (width - labelWidth) / 2), y + 4,
                active ? 0xFFFFF0FE : 0xFFB39AB8);
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden) return;
        int w = c.getWindow().getScaledWidth(), h = c.getWindow().getScaledHeight();
        int white = 0xFFFFF0FE, muted = 0xFFB39AB8, accent = 0xFFF05CCF;
        int accent2 = 0xFFBE8BFF, dark = 0xD9150D20;

        if (enabled("hitpulse") && c.player.hurtTime > 0)
            ctx.fill(0, 0, w, h, (Math.min(20, c.player.hurtTime * 2) << 24) | 0x006A1C8A);

        if (enabled("watermark")) {
            int[] p = HudLayout.get("watermark", 9, 9);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, 142, 32, accent);
            ctx.fill(x + 8, y + 7, x + 29, y + 28, 0xFF45213F);
            drawText(ctx, c, "N", x + 15, y + 13, accent2);
            drawText(ctx, c, "NIGHTFALL", x + 37, y + 6, white);
            drawText(ctx, c, "EXO UI  /  1.21.4", x + 37, y + 19, muted);
        }

        if (enabled("fps")) {
            int latency = -1;
            if (c.getNetworkHandler() != null) {
                var entry = c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
                if (entry != null) latency = entry.getLatency();
            }
            String s = c.getCurrentFps() + " FPS" + (latency >= 0 ? "  /  " + latency + " MS" : "");
            int width = textWidth(c, s) + 18;
            int[] p = HudLayout.get("fps", w - width - 10, 10);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, width, 23, accent2);
            drawText(ctx, c, s, x + 9, y + 7, accent2);
        }

        if (enabled("coordinates")) {
            String s = String.format(Locale.ROOT, "X %.1f   Y %.1f   Z %.1f",
                    c.player.getX(), c.player.getY(), c.player.getZ());
            int width = textWidth(c, s) + 18;
            int[] p = HudLayout.get("coordinates", 9, h - 30);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, width, 23, accent);
            drawText(ctx, c, s, x + 9, y + 7, white);
        }

        if (enabled("customcrosshair")) {
            int x = w / 2, y = h / 2;
            ctx.fill(x - 7, y - 1, x - 3, y + 1, accent);
            ctx.fill(x + 3, y - 1, x + 7, y + 1, accent);
            ctx.fill(x - 1, y - 7, x + 1, y - 3, accent);
            ctx.fill(x - 1, y + 3, x + 1, y + 7, accent);
            ctx.fill(x - 1, y - 1, x + 1, y + 1, white);
        }

        if (enabled("keystrokes")) {
            int[] p = HudLayout.get("keystrokes", w - 150, h - 57);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, 137, 43, accent);
            drawKey(ctx, c, "W", x + 29, y + 5, 22, c.options.forwardKey.isPressed());
            drawKey(ctx, c, "A", x + 5, y + 23, 22, c.options.leftKey.isPressed());
            drawKey(ctx, c, "S", x + 29, y + 23, 22, c.options.backKey.isPressed());
            drawKey(ctx, c, "D", x + 53, y + 23, 22, c.options.rightKey.isPressed());
            drawKey(ctx, c, "LMB", x + 82, y + 5, 48, c.options.attackKey.isPressed());
            drawKey(ctx, c, "RMB", x + 82, y + 23, 48, c.options.useKey.isPressed());
        }

        if (enabled("targethud") && c.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && living != c.player && living.isAlive()) {
            String name = living.getName().getString();
            String hp = String.format(Locale.ROOT, "%.1f / %.1f HP", living.getHealth(), living.getMaxHealth());
            int bw = Math.max(textWidth(c, name), textWidth(c, hp)) + 22;
            int[] p = HudLayout.get("targethud", (w - bw) / 2, h / 2 + 34);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, bw, 42, accent);
            drawText(ctx, c, name, x + 10, y + 7, white);
            drawText(ctx, c, hp, x + 10, y + 20, muted);
            int barX = x + 10, barY = y + 34, barW = bw - 20;
            ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF46324D);
            int filled = (int)(barW * MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0, 1));
            ctx.fill(barX, barY, barX + filled, barY + 3, accent);
        }

        if (enabled("hitmarker") && System.currentTimeMillis() - lastHitTime < 280) {
            int x = w / 2, y = h / 2;
            ctx.fill(x - 8, y - 8, x - 5, y - 5, white);
            ctx.fill(x + 5, y - 8, x + 8, y - 5, white);
            ctx.fill(x - 8, y + 5, x - 5, y + 8, white);
            ctx.fill(x + 5, y + 5, x + 8, y + 8, white);
        }

        if (enabled("speedometer")) {
            Vec3d v = c.player.getVelocity();
            double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
            String s = String.format(Locale.ROOT, "%.2f M/S", speed);
            int[] p = HudLayout.get("speedometer", 12, h - 39);
            int x = p[0], y = p[1], width = textWidth(c, s) + 18;
            hudPanel(ctx, x, y, width, 23, accent2);
            drawText(ctx, c, s, x + 9, y + 7, accent2);
        }

        if (enabled("totemcount")) {
            int count = c.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1 : 0;
            for (int i = 0; i < c.player.getInventory().size(); i++) {
                ItemStack stack = c.player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
            }
            String s = "TOTEMS  " + count;
            int[] p = HudLayout.get("totemcount", 12, 37);
            int x = p[0], y = p[1];
            hudPanel(ctx, x, y, 105, 23, count > 0 ? accent : 0xFF796A82);
            drawText(ctx, c, s, x + 9, y + 7, count > 0 ? white : muted);
        }

        if (enabled("armorhud")) {
            int[] p = HudLayout.get("armorhud", w / 2 - 72, h - 58);
            int count = 0;
            for (ItemStack stack : c.player.getArmorItems()) if (!stack.isEmpty()) count++;
            int totalWidth = Math.max(1, count) * 38 + 4;
            hudPanel(ctx, p[0], p[1], totalWidth, 23, accent2);
            int index = 0;
            for (ItemStack stack : c.player.getArmorItems()) {
                if (stack.isEmpty()) continue;
                int durability = stack.isDamageable()
                        ? (int)(100.0 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) : 100;
                int sx = p[0] + 5 + index * 38;
                ctx.fill(sx, p[1] + 5, sx + 32, p[1] + 18, 0xFF38263F);
                int fillW = (int)(28 * Math.max(0, Math.min(1, durability / 100.0)));
                ctx.fill(sx + 2, p[1] + 15, sx + 2 + fillW, p[1] + 17, durability < 25 ? 0xFFFF797F : accent2);
                drawText(ctx, c, durability + "%", sx + 3, p[1] + 7, durability < 25 ? 0xFFFFB2B5 : white);
                index++;
            }
        }

        if (enabled("potionhud")) {
            int[] p = HudLayout.get("potionhud", w - 160, 43);
            int x = p[0], y = p[1];
            int count = Math.min(5, c.player.getStatusEffects().size());
            int panelH = Math.max(29, 20 + count * 14);
            hudPanel(ctx, x, y, 148, panelH, accent2);
            drawText(ctx, c, "ACTIVE EFFECTS", x + 8, y + 6, accent2);
            int row = 0;
            for (StatusEffectInstance effect : c.player.getStatusEffects()) {
                if (row >= 5) break;
                String name = effect.getEffectType().value().getName().getString();
                int seconds = effect.getDuration() / 20;
                drawText(ctx, c, name + "  " + seconds + "S", x + 8, y + 20 + row * 14, white);
                row++;
            }
            if (row == 0) drawText(ctx, c, "NO EFFECTS", x + 8, y + 20, muted);
        }

        if (enabled("radar")) {
            int[] p = HudLayout.get("radar", w - 108, 38);
            int rx = p[0], ry = p[1], size = 92, centerX = rx + size / 2, centerY = ry + size / 2;
            hudPanel(ctx, rx, ry, size, size, accent);
            ctx.fill(centerX, ry + 18, centerX + 1, ry + size - 10, 0xFF49344E);
            ctx.fill(rx + 10, centerY, rx + size - 10, centerY + 1, 0xFF49344E);
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
                ctx.fill(px - 2, py - 2, px + 3, py + 3, accent);
            }
            drawText(ctx, c, "RADAR", rx + 7, ry + 6, muted);
        }

        if (enabled("mediaplayer")) {
            MusicManager.MediaState media = MusicManager.state();
            int[] p = HudLayout.get("mediaplayer", w - 290, 40);
            int x = p[0], y = p[1], bw = 278, bh = 62;
            hudPanel(ctx, x, y, bw, bh, accent);
            ctx.fill(x + 7, y + 9, x + 47, y + 53, 0xFF3B2444);
            ctx.drawTexture(RenderLayer::getGuiTextured, ANIME_PORTRAIT,
                    x + 8, y + 10, 0.0f, 0.0f, 38, 42, 48, 60);
            drawText(ctx, c, media.playing() ? "NOW PLAYING" : "MEDIA PLAYER", x + 54, y + 5, accent2);
            String title = media.title().isBlank() ? "Открой Spotify / Яндекс Музыку" : media.title();
            String artist = media.artist().isBlank() ? media.status() : media.artist();
            while (textWidth(c, title) > bw - 70 && title.length() > 3) title = title.substring(0, title.length() - 2) + "…";
            while (textWidth(c, artist) > bw - 70 && artist.length() > 3) artist = artist.substring(0, artist.length() - 2) + "…";
            drawText(ctx, c, title, x + 54, y + 18, white);
            drawText(ctx, c, artist, x + 54, y + 31, muted);
            int buttonY = y + 39, buttonW = 22, buttonH = 13;
            int buttonX = x + bw - 76;
            ctx.fill(buttonX, buttonY, buttonX + buttonW, buttonY + buttonH, 0xFF3B2444);
            ctx.fill(buttonX + 25, buttonY, buttonX + 25 + buttonW, buttonY + buttonH, 0xFF3B2444);
            ctx.fill(buttonX + 50, buttonY, buttonX + 50 + buttonW, buttonY + buttonH, 0xFF3B2444);
            drawText(ctx, c, "|<", buttonX + 6, buttonY + 3, accent2);
            drawText(ctx, c, "||", buttonX + 7 + 25, buttonY + 3, accent2);
            drawText(ctx, c, ">|", buttonX + 6 + 50, buttonY + 3, accent2);
            int progressX = x + 54, progressY = y + 51, progressW = bw - 138;
            ctx.fill(progressX, progressY, progressX + progressW, progressY + 3, 0xFF49344E);
            int progressWidth = media.durationMs() > 0
                    ? (int)(progressW * Math.min(1.0, media.positionNow() / (double)media.durationMs())) : 0;
            if (progressWidth > 0) ctx.fill(progressX, progressY, progressX + progressWidth, progressY + 3, accent);
            drawText(ctx, c, media.status().toUpperCase(Locale.ROOT), x + bw - 77, y + 53, muted);
        }

        if (enabled("worldlyrics")) {
            String lyric = MusicManager.currentLyric();
            if (lyric.isBlank() && !MusicManager.state().title().isBlank()) {
                lyric = MusicManager.lyricError().isBlank() ? "СИНХРОНИЗАЦИЯ ТЕКСТА..." : MusicManager.lyricError();
            }
            if (!lyric.isBlank()) {
                int[] p = HudLayout.get("worldlyrics", w / 2 - 175, h - 57);
                int x = p[0], y = p[1], bw = 350;
                String line = lyric;
                while (textWidth(c, line) > bw - 38 && line.length() > 3) line = line.substring(0, line.length() - 2) + "…";
                hudPanel(ctx, x, y, bw, 29, accent2);
                drawText(ctx, c, "LYRICS", x + 9, y + 4, accent2);
                drawText(ctx, c, line, x + 55, y + 10, white);
            }
        }
    }
}
