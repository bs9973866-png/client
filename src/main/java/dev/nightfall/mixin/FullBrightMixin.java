package dev.nightfall.mixin;

import dev.nightfall.FeatureSettings;
import dev.nightfall.NightfallClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.render.LightmapTextureManager.class)
public abstract class FullBrightMixin {
    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void nightfall$fullBright(float ambientLight, int lightLevel,
                                             CallbackInfoReturnable<Float> cir) {
        if (NightfallClient.enabled("fullbright")) {
            float intensity = (float)Math.max(0.5, Math.min(1.0,
                    FeatureSettings.number("fullbright", "gamma") / 100.0));
            cir.setReturnValue(intensity);
        }
    }
}