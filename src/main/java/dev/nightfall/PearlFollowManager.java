package dev.nightfall;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * When an opponent launches an ender pearl nearby, throws the player's own pearl
 * toward that opponent's predicted position. Rotation is visible to the player.
 */
public final class PearlFollowManager {
    private static final Set<Integer> handledPearls = new HashSet<>();
    private static int tickCount;
    private static int nextAllowedTick;
    private static int restoreSlot = -1;
    private static int restoreAtTick;

    private PearlFollowManager() {}

    public static void tick(MinecraftClient client) {
        tickCount++;
        if (client.player == null || client.interactionManager == null || client.player.networkHandler == null) {
            handledPearls.clear();
            restoreSlot = -1;
            return;
        }

        var player = client.player;
        if (restoreSlot >= 0 && tickCount >= restoreAtTick) {
            player.getInventory().selectedSlot = restoreSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(restoreSlot));
            restoreSlot = -1;
        }

        if (!NightfallClient.enabled("pearlfollow") || client.world == null || client.currentScreen != null) return;
        double range = FeatureSettings.number("pearlfollow", "range");
        var pearls = client.world.getEntitiesByClass(EnderPearlEntity.class,
                player.getBoundingBox().expand(range), pearl -> !pearl.isRemoved());

        for (EnderPearlEntity pearl : pearls) {
            if (handledPearls.contains(pearl.getId())) continue;
            Entity ownerEntity = pearl.getOwner();
            if (!(ownerEntity instanceof PlayerEntity owner) || owner == player || owner.isSpectator()) continue;
            if (player.squaredDistanceTo(owner) > range * range) continue;
            if (tickCount < nextAllowedTick || !hasPearlInReach(player)) continue;

            Vec3d predicted = owner.getPos().add(owner.getVelocity().multiply(4.0))
                    .add(0.0, owner.getHeight() * 0.45, 0.0);
            aimAt(player, predicted);

            if (player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
                client.interactionManager.interactItem(player, Hand.OFF_HAND);
            } else if (player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            } else {
                int oldSlot = player.getInventory().selectedSlot;
                int pearlSlot = findHotbarPearl(player);
                if (pearlSlot < 0) return;
                player.getInventory().selectedSlot = pearlSlot;
                player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                restoreSlot = oldSlot;
                restoreAtTick = tickCount + 3;
            }

            handledPearls.add(pearl.getId());
            if (handledPearls.size() > 160) handledPearls.clear();
            nextAllowedTick = tickCount + Math.max(5, (int)FeatureSettings.number("pearlfollow", "cooldown"));
            NightfallClient.LOGGER.debug("Pearl Follow triggered by {}", owner.getName().getString());
            return;
        }
    }

    private static boolean hasPearlInReach(PlayerEntity player) {
        return player.getOffHandStack().isOf(Items.ENDER_PEARL)
                || player.getMainHandStack().isOf(Items.ENDER_PEARL)
                || findHotbarPearl(player) >= 0;
    }

    private static int findHotbarPearl(PlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) return slot;
        }
        return -1;
    }

    private static void aimAt(PlayerEntity player, Vec3d target) {
        Vec3d delta = target.subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.max(0.001, horizontal)));
        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch - Math.min(12.0f, (float)horizontal * 0.16f), -80.0f, 80.0f));
    }
}