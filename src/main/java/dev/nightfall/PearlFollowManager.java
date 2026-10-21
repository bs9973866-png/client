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
 * Responds to a nearby opponent throwing an ender pearl. It first turns the
 * visible camera and syncs the selected slot; the pearl is thrown on the next
 * client tick so the server has received the updated rotation.
 */
public final class PearlFollowManager {
    private static final Set<Integer> handledPearls = new HashSet<>();
    private static int tickCount;
    private static int nextAllowedTick;
    private static int restoreSlot = -1;
    private static int restoreAtTick;

    private static int pendingThrowAtTick = -1;
    private static int pendingPearlSlot = -1;
    private static int pendingPreviousSlot = -1;
    private static int pendingOwnerId = -1;
    private static Hand pendingHand = Hand.MAIN_HAND;
    private static Vec3d pendingTarget;

    private PearlFollowManager() {}

    public static void tick(MinecraftClient client) {
        tickCount++;
        if (client.player == null || client.interactionManager == null
                || client.player.networkHandler == null || client.world == null) {
            handledPearls.clear();
            restoreSlot = -1;
            cancelPending(null);
            return;
        }

        PlayerEntity player = client.player;
        restoreSlotIfDue(player);

        if (pendingThrowAtTick >= 0) {
            if (!NightfallClient.enabled("pearlfollow") || client.currentScreen != null) {
                cancelPending(player);
                return;
            }
            if (tickCount >= pendingThrowAtTick) {
                // Prefer the opponent's latest position if it is still available.
                Entity ownerEntity = client.world.getEntityById(pendingOwnerId);
                Vec3d target = pendingTarget;
                if (ownerEntity instanceof PlayerEntity owner && owner != player) {
                    target = predictedPlayerTarget(owner);
                }
                if (target != null) {
                    aimAt(player, target);
                    if (pendingPearlSlot >= 0) {
                        if (player.getInventory().selectedSlot != pendingPearlSlot) {
                            player.getInventory().selectedSlot = pendingPearlSlot;
                            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pendingPearlSlot));
                        }
                        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                        restoreSlot = pendingPreviousSlot;
                        restoreAtTick = tickCount + 3;
                    } else {
                        client.interactionManager.interactItem(player, pendingHand);
                    }
                    nextAllowedTick = tickCount + Math.max(5,
                            (int)FeatureSettings.number("pearlfollow", "cooldown"));
                }
                clearPending();
            }
            return;
        }

        if (!NightfallClient.enabled("pearlfollow") || client.currentScreen != null
                || tickCount < nextAllowedTick) return;

        double range = FeatureSettings.number("pearlfollow", "range");
        var pearls = client.world.getEntitiesByClass(EnderPearlEntity.class,
                player.getBoundingBox().expand(range), pearl -> !pearl.isRemoved());

        for (EnderPearlEntity pearl : pearls) {
            if (handledPearls.contains(pearl.getId())) continue;
            Entity ownerEntity = pearl.getOwner();
            if (!(ownerEntity instanceof PlayerEntity owner) || owner == player || owner.isSpectator()) continue;
            if (player.squaredDistanceTo(owner) > range * range || !hasPearlInReach(player)) continue;

            pendingTarget = predictedPlayerTarget(owner);
            pendingOwnerId = owner.getId();
            pendingHand = Hand.MAIN_HAND;
            pendingPearlSlot = -1;
            pendingPreviousSlot = -1;

            if (player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
                pendingHand = Hand.OFF_HAND;
            } else if (player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                pendingHand = Hand.MAIN_HAND;
            } else {
                int pearlSlot = findHotbarPearl(player);
                if (pearlSlot < 0) continue;
                pendingPreviousSlot = player.getInventory().selectedSlot;
                pendingPearlSlot = pearlSlot;
                player.getInventory().selectedSlot = pearlSlot;
                player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
            }

            aimAt(player, pendingTarget);
            pendingThrowAtTick = tickCount + 1;
            handledPearls.add(pearl.getId());
            if (handledPearls.size() > 160) handledPearls.clear();
            NightfallClient.LOGGER.debug("Pearl Follow queued for pearl thrown by {}", owner.getName().getString());
            return;
        }
    }

    private static void restoreSlotIfDue(PlayerEntity player) {
        if (restoreSlot >= 0 && tickCount >= restoreAtTick) {
            player.getInventory().selectedSlot = restoreSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(restoreSlot));
            restoreSlot = -1;
        }
    }

    private static void cancelPending(PlayerEntity player) {
        if (player != null && pendingPearlSlot >= 0 && pendingPreviousSlot >= 0) {
            player.getInventory().selectedSlot = pendingPreviousSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pendingPreviousSlot));
        }
        clearPending();
    }

    private static void clearPending() {
        pendingThrowAtTick = -1;
        pendingPearlSlot = -1;
        pendingPreviousSlot = -1;
        pendingOwnerId = -1;
        pendingTarget = null;
        pendingHand = Hand.MAIN_HAND;
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

    private static Vec3d predictedPlayerTarget(PlayerEntity player) {
        return player.getPos().add(player.getVelocity().multiply(3.0))
                .add(0.0, player.getHeight() * 0.45, 0.0);
    }

    private static void aimAt(PlayerEntity player, Vec3d target) {
        Vec3d delta = target.subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float directPitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.max(0.001, horizontal)));
        // Ender pearls arc down; bias the aim slightly upward at longer ranges.
        float arcCompensation = Math.min(18.0f, (float)horizontal * 0.22f);
        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(directPitch - arcCompensation, -80.0f, 80.0f));
    }
}