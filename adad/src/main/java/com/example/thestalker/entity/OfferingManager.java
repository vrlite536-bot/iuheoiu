package com.example.thestalker.entity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles soul-sand offerings and the temporary truce they buy. */
public final class OfferingManager {
    public static final long TRUCE_TICKS = 24_000L; // one Minecraft day
    private static final Map<UUID, Long> TRUCES = new HashMap<>();
    private static final Map<UUID, OfferingAnimation> ANIMATIONS = new HashMap<>();
    private OfferingManager() {}

    public static boolean isProtected(PlayerEntity player) {
        Long expiry = TRUCES.get(player.getUuid());
        if (expiry == null) return false;
        if (getServerClock(player) >= expiry) {
            TRUCES.remove(player.getUuid());
            return false;
        }
        return true;
    }

    /** Drops a half-finished offering animation when its player disconnects. The truce itself is kept. */
    public static void clearAnimation(ServerPlayerEntity player) { ANIMATIONS.remove(player.getUuid()); }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            Long expiry = TRUCES.get(player.getUuid());
            long serverTime = world.getServer().getOverworld().getTime();
            if (expiry != null && serverTime >= expiry) TRUCES.remove(player.getUuid());

            OfferingAnimation animation = ANIMATIONS.get(player.getUuid());
            if (animation != null) {
                tickHandAnimation(player, world, animation);
                if (++animation.age >= 28) {
                    ANIMATIONS.remove(player.getUuid());
                    TRUCES.put(player.getUuid(), world.getServer().getOverworld().getTime() + TRUCE_TICKS);
                    world.playSound(null, animation.pos, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 0.9f, 0.55f);
                    world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SOUL_SAND_STEP, SoundCategory.BLOCKS, 0.8f, 0.6f);
                }
                continue;
            }

            if (isProtected(player)) continue;

            Box search = player.getBoundingBox().expand(2.5, 1.5, 2.5);
            for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, search,
                    e -> e.isAlive() && e.getStack().isOf(Items.SOUL_SAND))) {
                BlockPos offeringPos = item.getBlockPos();
                if (world.getLightLevel(LightType.BLOCK, offeringPos) > 3 || !world.getBlockState(offeringPos.down()).isSolidBlock(world, offeringPos.down())) continue;
                // Only accept an offering made close to the player who will receive the truce.
                if (item.squaredDistanceTo(player) > 6.25) continue;
                item.getStack().decrement(1);
                if (item.getStack().isEmpty()) item.discard();
                ANIMATIONS.put(player.getUuid(), new OfferingAnimation(offeringPos.toImmutable()));
                world.playSound(null, offeringPos, SoundEvents.BLOCK_SOUL_SAND_BREAK, SoundCategory.BLOCKS, 1.0f, 0.45f);
                world.spawnParticles(ParticleTypes.SOUL, offeringPos.getX() + 0.5, offeringPos.getY() + 0.15, offeringPos.getZ() + 0.5,
                        14, 0.22, 0.12, 0.22, 0.015);
                break;
            }
        }
    }

    private static long getServerClock(PlayerEntity player) {
        // Use the Overworld's persistent world clock so dimension changes do not
        // shorten or extend the offering's 24,000-tick duration.
        return player.getWorld().getServer().getOverworld().getTime();
    }

    private static void tickHandAnimation(ServerPlayerEntity player, ServerWorld world, OfferingAnimation animation) {
        BlockPos pos = animation.pos;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.12 + Math.min(0.85, animation.age * 0.035);
        double z = pos.getZ() + 0.5;
        // A rising palm and five curling fingers, built from soul wisps and soul-fire motes.
        world.spawnParticles(ParticleTypes.SOUL, x, y, z, 3, 0.10, 0.04, 0.10, 0.005);
        if (animation.age > 5) {
            for (int finger = -2; finger <= 2; finger++) {
                double fx = x + finger * 0.105;
                double fy = y + (finger == 0 ? 0.27 : 0.19);
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, fx, fy, z, 1, 0.012, 0.025, 0.012, 0.001);
            }
        }
        if (animation.age > 17) {
            world.spawnParticles(ParticleTypes.SMOKE, x, y - 0.12, z, 5, 0.16, 0.08, 0.16, 0.012);
        }
    }

    private static final class OfferingAnimation {
        private final BlockPos pos;
        private int age;
        private OfferingAnimation(BlockPos pos) { this.pos = pos; }
    }
}
