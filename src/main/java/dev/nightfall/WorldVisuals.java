package dev.nightfall;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** World-space visuals implemented through Fabric's render event API. */
public final class WorldVisuals {
    private static List<BlockPos> containers = List.of();
    private static List<BlockPos> ores = List.of();
    private static int scanTicks;

    private WorldVisuals() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldVisuals::render);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            containers = List.of();
            ores = List.of();
            scanTicks = 0;
            return;
        }
        if (++scanTicks % 8 != 0) return;
        if (NightfallClient.enabled("storageesp")) scanContainers(client);
        else containers = List.of();
        if (NightfallClient.enabled("oresp")) scanOres(client);
        else ores = List.of();
    }

    private static void scanContainers(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterateOutwards(client.player.getBlockPos(), 8, 5, 8)) {
            var block = client.world.getBlockState(pos).getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL
                    || block == Blocks.ENDER_CHEST || block == Blocks.SHULKER_BOX
                    || block == Blocks.WHITE_SHULKER_BOX || block == Blocks.ORANGE_SHULKER_BOX
                    || block == Blocks.MAGENTA_SHULKER_BOX || block == Blocks.LIGHT_BLUE_SHULKER_BOX
                    || block == Blocks.YELLOW_SHULKER_BOX || block == Blocks.LIME_SHULKER_BOX
                    || block == Blocks.PINK_SHULKER_BOX || block == Blocks.GRAY_SHULKER_BOX
                    || block == Blocks.LIGHT_GRAY_SHULKER_BOX || block == Blocks.CYAN_SHULKER_BOX
                    || block == Blocks.PURPLE_SHULKER_BOX || block == Blocks.BLUE_SHULKER_BOX
                    || block == Blocks.BROWN_SHULKER_BOX || block == Blocks.GREEN_SHULKER_BOX
                    || block == Blocks.RED_SHULKER_BOX || block == Blocks.BLACK_SHULKER_BOX) {
                found.add(pos.toImmutable());
            }
        }
        containers = List.copyOf(found);
    }

    private static void scanOres(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterateOutwards(client.player.getBlockPos(), 8, 5, 8)) {
            BlockState state = client.world.getBlockState(pos);
            if (isOre(state.getBlock())) found.add(pos.toImmutable());
        }
        ores = List.copyOf(found);
    }

    private static boolean isOre(net.minecraft.block.Block block) {
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || context.world() == null) return;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        if (NightfallClient.enabled("playeresp") || NightfallClient.enabled("mobesp")
                || NightfallClient.enabled("tracers") || NightfallClient.enabled("targetring")
                || NightfallClient.enabled("entityglow") || NightfallClient.enabled("itemesp")
                || NightfallClient.enabled("projectileesp")) {
            Box renderArea = client.player.getBoundingBox().expand(64.0);
            for (Entity entity : context.world().getOtherEntities(client.player, renderArea, e -> !e.isRemoved() && !e.isSpectator())) {
                if (entity instanceof PlayerEntity && NightfallClient.enabled("playeresp")) {
                    drawEntityBox(matrices, lines, entity, 0.47f, 0.72f, 0.69f, 0.94f);
                } else if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && NightfallClient.enabled("mobesp")) {
                    drawEntityBox(matrices, lines, entity, 0.76f, 0.67f, 0.49f, 0.85f);
                } else if (entity instanceof ItemEntity && NightfallClient.enabled("itemesp")) {
                    drawEntityBox(matrices, lines, entity, 0.81f, 0.74f, 0.48f, 0.9f);
                } else if (entity instanceof ProjectileEntity && NightfallClient.enabled("projectileesp")) {
                    drawEntityBox(matrices, lines, entity, 0.49f, 0.69f, 0.86f, 0.9f);
                }

                if (NightfallClient.enabled("tracers") && isVisibleTargetType(entity)) {
                    Vec3d to = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.56, entity.getZ());
                    drawLine(matrices.peek(), lines, camera.x, camera.y, camera.z, to.x, to.y, to.z,
                            entity instanceof PlayerEntity ? 0.47f : 0.76f,
                            entity instanceof PlayerEntity ? 0.72f : 0.67f,
                            entity instanceof PlayerEntity ? 0.69f : 0.49f, 0.75f);
                }

                if (NightfallClient.enabled("targetring") && client.crosshairTarget instanceof EntityHitResult hit
                        && hit.getEntity() == entity) {
                    drawRing(matrices.peek(), lines, entity.getX(), entity.getY() + 0.06, entity.getZ(),
                            Math.max(0.48, entity.getWidth() * 0.72), 40,
                            0.47f, 0.72f, 0.69f, 0.95f);
                }
            }
        }

        if (NightfallClient.enabled("storageesp")) {
            for (BlockPos pos : containers) {
                if (pos.getSquaredDistance(client.player.getBlockPos()) > 100) continue;
                VertexRendering.drawBox(matrices, lines, new Box(pos).expand(0.025), 0.78f, 0.69f, 0.49f, 0.88f);
            }
        }
        if (NightfallClient.enabled("oresp")) {
            for (BlockPos pos : ores) {
                if (pos.getSquaredDistance(client.player.getBlockPos()) > 100) continue;
                BlockState state = context.world().getBlockState(pos);
                float r = 0.45f, g = 0.72f, b = 0.66f;
                if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) { r = 0.35f; g = 0.78f; b = 0.95f; }
                else if (state.isOf(Blocks.ANCIENT_DEBRIS)) { r = 0.78f; g = 0.48f; b = 0.38f; }
                else if (state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)) { r = 0.40f; g = 0.78f; b = 0.49f; }
                VertexRendering.drawBox(matrices, lines, new Box(pos).expand(0.018), r, g, b, 0.92f);
            }
        }

        if (NightfallClient.enabled("worldlyrics")) {
            String lyric = MusicManager.currentLyric();
            if (lyric.isBlank() && !MusicManager.state().title().isBlank()) {
                lyric = MusicManager.lyricError().isBlank() ? "СИНХРОНИЗАЦИЯ ТЕКСТА..." : MusicManager.lyricError();
            }
            if (!lyric.isBlank()) {
                if (lyric.length() > 84) lyric = lyric.substring(0, 81) + "…";
                matrices.push();
                matrices.translate(client.player.getX(), client.player.getY() + client.player.getHeight() + 0.55, client.player.getZ());
                matrices.multiply(client.getEntityRenderDispatcher().getRotation());
                matrices.scale(-0.025f, -0.025f, 0.025f);
                float tx = -client.textRenderer.getWidth(lyric) / 2.0f;
                client.textRenderer.draw(lyric, tx, 0, 0xFFE1E9E9, true, matrices.peek().getPositionMatrix(),
                        consumers, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                        0xA010171E, net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE);
                matrices.pop();
            }
        }
        matrices.pop();
    }

    private static boolean isVisibleTargetType(Entity entity) {
        return (entity instanceof PlayerEntity && NightfallClient.enabled("targetplayers"))
                || (entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && NightfallClient.enabled("targetmobs"));
    }

    private static void drawEntityBox(MatrixStack matrices, VertexConsumer lines, Entity entity,
                                      float r, float g, float b, float alpha) {
        Box box = entity.getBoundingBox().expand(0.035);
        if (NightfallClient.enabled("entityglow")) {
            VertexRendering.drawBox(matrices, lines, box.expand(0.07), r, g, b, 0.22f);
        }
        VertexRendering.drawBox(matrices, lines, box, r, g, b, alpha);
        if (entity instanceof LivingEntity living && living.getMaxHealth() > 0) {
            float hp = Math.max(0, Math.min(1, living.getHealth() / living.getMaxHealth()));
            double x = entity.getX(), y = entity.getY() - 0.04, z = entity.getZ();
            drawLine(matrices.peek(), lines, x - 0.32, y, z, x - 0.32 + 0.64 * hp, y, z,
                    0.36f + (1 - hp) * 0.5f, 0.72f * hp + 0.1f, 0.48f, 0.9f);
        }
    }

    private static void drawRing(MatrixStack.Entry entry, VertexConsumer lines, double x, double y, double z,
                                 double radius, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments;
            double a1 = (i + 1) * Math.PI * 2 / segments;
            drawLine(entry, lines, x + Math.cos(a0) * radius, y, z + Math.sin(a0) * radius,
                    x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius, r, g, b, a);
        }
    }

    private static void drawLine(MatrixStack.Entry entry, VertexConsumer lines,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        int ri = (int)(Math.max(0, Math.min(1, r)) * 255);
        int gi = (int)(Math.max(0, Math.min(1, g)) * 255);
        int bi = (int)(Math.max(0, Math.min(1, b)) * 255);
        int ai = (int)(Math.max(0, Math.min(1, a)) * 255);
        lines.vertex(entry, (float)x1, (float)y1, (float)z1).color(ri, gi, bi, ai).normal(entry, 0, 1, 0);
        lines.vertex(entry, (float)x2, (float)y2, (float)z2).color(ri, gi, bi, ai).normal(entry, 0, 1, 0);
    }
}
