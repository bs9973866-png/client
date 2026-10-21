package dev.nightfall.mixin;

import dev.nightfall.FeatureSettings;
import dev.nightfall.NightfallClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lightweight built-in sky colour profiles; no external shader pack is required. */
@Mixin(ClientWorld.class)
public abstract class NebulaSkyMixin {
    private static boolean active() {
        return NightfallClient.enabled("shaders") || NightfallClient.enabled("nebulasky");
    }

    private static int profile() {
        if (!NightfallClient.enabled("shaders")) return 0;
        return switch (FeatureSettings.mode("shaders", "preset")) {
            case "Sakura" -> 1;
            case "Sunset" -> 2;
            case "Aqua" -> 3;
            case "Cinema" -> 4;
            default -> 0;
        };
    }

    private static float strength() {
        return NightfallClient.enabled("shaders")
                ? (float)Math.max(0.2, Math.min(1.0, FeatureSettings.number("shaders", "intensity") / 100.0))
                : 1.0f;
    }

    private static int tint(int profile, boolean clouds) {
        if (clouds) {
            return switch (profile) {
                case 1 -> 0xFFC28BB9;
                case 2 -> 0xFFD38B6B;
                case 3 -> 0xFF77BCCB;
                case 4 -> 0xFF635276;
                default -> 0xFF9070B0;
            };
        }
        return switch (profile) {
            case 1 -> 0xFF765079;
            case 2 -> 0xFF96565C;
            case 3 -> 0xFF2B6378;
            case 4 -> 0xFF2D243D;
            default -> 0xFF493064;
        };
    }

    private static int blend(int original, int target, float amount) {
        float a = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round(((original >> 16) & 255) * (1.0f - a) + ((target >> 16) & 255) * a);
        int g = Math.round(((original >> 8) & 255) * (1.0f - a) + ((target >> 8) & 255) * a);
        int b = Math.round((original & 255) * (1.0f - a) + (target & 255) * a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void nightfall$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (active()) cir.setReturnValue(blend(cir.getReturnValue(), tint(profile(), false), strength()));
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void nightfall$clouds(float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (active()) cir.setReturnValue(blend(cir.getReturnValue(), tint(profile(), true), strength()));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void nightfall$stars(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (active()) cir.setReturnValue(Math.max(cir.getReturnValue(), 0.45f + 0.45f * strength()));
    }
}