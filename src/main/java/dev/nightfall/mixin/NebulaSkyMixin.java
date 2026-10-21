package dev.nightfall.mixin;

import dev.nightfall.NightfallClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Purple sky tint and visible stars for the NIGHTFALL nebula palette. */
@Mixin(ClientWorld.class)
public abstract class NebulaSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void nightfall$nebulaSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (NightfallClient.enabled("nebulasky")) cir.setReturnValue(0xFF493064);
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void nightfall$nebulaCloudColor(float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (NightfallClient.enabled("nebulasky")) cir.setReturnValue(0xFF725080);
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void nightfall$nebulaStars(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (NightfallClient.enabled("nebulasky")) cir.setReturnValue(0.62f);
    }
}