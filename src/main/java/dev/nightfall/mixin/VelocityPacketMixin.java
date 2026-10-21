package dev.nightfall.mixin;

import dev.nightfall.NightfallClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-side velocity packet filter. Server-side movement validation is unchanged. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class VelocityPacketMixin {
    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void nightfall$ignoreLocalKnockback(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (NightfallClient.enabled("velocity") && client.player != null
                && packet.getEntityId() == client.player.getId()) {
            ci.cancel();
        }
    }
}
