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
        if (enabled("autototem") && client.currentScreen == null && player.getHealth() <= 12.0f
                && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && ++totemDelay % 8 == 0
                && client.interactionManager != null) {
            for (int i = 0; i < 36; i++) {
                if (player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    int screenSlot = i < 9 ? i + 36 : i;
                    client.interactionManager.clickSlot(player.currentScreenHandler.syncId, screenSlot, 40,
                            SlotActionType.SWAP, player);
                    break;
                }
            }
        }

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
            auraTicks++; triggerTicks++;
            int delay = enabled("killaura") ? 6 : 0;
            if (enabled("killaura") && auraTicks >= delay) {
                LivingEntity target = findTarget(client, enabled("reach") ? 6.0 : 3.8, 0.965);
                if (target != null && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                    attack(client, target); auraTicks = 0;
                }
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
            ctx.fill(9, 9, 126, 33, dark); ctx.fill(9, 9, 11, 33, accent);
            drawText(ctx, c, "NIGHTFALL", 18, 12, white);
            drawText(ctx, c, "1.21.4  /  FABRIC", 18, 22, muted);
        }
        if (enabled("fps")) {
            String s = c.getCurrentFps() + " FPS"; int x = w - textWidth(c, s) - 12;
            ctx.fill(x - 6, 9, w - 8, 25, dark); drawText(ctx, c, s, x, 13, accent);
        }
        if (enabled("coordinates")) {
            String s = String.format(Locale.ROOT, "X %.1f   Y %.1f   Z %.1f", c.player.getX(), c.player.getY(), c.player.getZ());
            ctx.fill(9, h - 25, 9 + textWidth(c, s) + 13, h - 9, dark);
            drawText(ctx, c, s, 15, h - 21, white);
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
            ctx.fill(w - tw - 18, h - 26, w - 8, h - 10, dark);
            drawText(ctx, c, s, w - tw - 12, h - 21, accent);
        }
        if (enabled("targethud") && c.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && living != c.player && living.isAlive()) {
            String name = living.getName().getString(), hp = String.format(Locale.ROOT, "HP %.1f / %.1f", living.getHealth(), living.getMaxHealth());
            int bw = Math.max(textWidth(c, name), textWidth(c, hp)) + 20, x = (w - bw) / 2, y = h / 2 + 34;
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
            drawText(ctx, c, s, 12, h - 39, accent);
        }
        if (enabled("totemcount")) {
            int count = c.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1 : 0;
            for (int i = 0; i < c.player.getInventory().size(); i++) {
                ItemStack stack = c.player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
            }
            drawText(ctx, c, "TOTEMS  " + count, 12, 37, count > 0 ? accent : muted);
        }
        if (enabled("armorhud")) {
            int x = w / 2 - 72, y = h - 58, index = 0;
            for (ItemStack stack : c.player.getArmorItems()) {
                if (stack.isEmpty()) continue;
                int durability = stack.isDamageable() ? (int)(100.0 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) : 100;
                ctx.fill(x + index * 38, y, x + index * 32 + 28, y + 14, dark);
                drawText(ctx, c, durability + "%", x + index * 38 + 3, y + 3, durability < 25 ? 0xFFD58B82 : accent);
                index++;
            }
        }
        if (enabled("potionhud")) {
            int y = 42;
            for (StatusEffectInstance effect : c.player.getStatusEffects()) {
                String name = effect.getEffectType().value().getName().getString();
                int seconds = effect.getDuration() / 20;
                drawText(ctx, c, name + "  " + seconds + "s", w - textWidth(c, name + "  " + seconds + "s") - 10, y, muted);
                y += 11;
                if (y > 120) break;
            }
        }
        if (enabled("radar")) {
            int rx = w - 108, ry = 38, size = 90, centerX = rx + size / 2, centerY = ry + size / 2;
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
    }
}
