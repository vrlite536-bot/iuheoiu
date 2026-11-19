package com.example.thestalker.entity;

import com.example.thestalker.TheStalkerMod;
import com.example.thestalker.registry.ModEntities;
import com.example.thestalker.network.JumpscarePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative delayed ambushes tied to the Observer's original position. */
public final class AmbushManager {
    private static final Map<UUID, Ambush> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveScare> ACTIVE_KILLS = new ConcurrentHashMap<>();
    private static final double TRIGGER_RADIUS = 1.65;
    private AmbushManager() {}

    public static void schedule(ServerPlayerEntity player, ServerWorld world, BlockPos observerPos, long now, int delayTicks) {
        PENDING.put(player.getUuid(), new Ambush(world.getRegistryKey(), observerPos.toImmutable(), now + delayTicks));
    }

    public static void cancel(ServerPlayerEntity player) {
        PENDING.remove(player.getUuid());
        ActiveScare active = ACTIVE_KILLS.remove(player.getUuid());
        if (active != null && active.grabber != null) active.grabber.discard();
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (OfferingManager.isProtected(player)) {
                PENDING.remove(player.getUuid());
                ActiveScare protectedScare = ACTIVE_KILLS.remove(player.getUuid());
                if (protectedScare != null && protectedScare.grabber != null) protectedScare.grabber.discard();
                continue;
            }
            ActiveScare active = ACTIVE_KILLS.get(player.getUuid());
            if (active != null) {
                // A cinematic must never kill a player who died, changed dimension, or
                // entered a protected game mode while the grab animation was running.
                if (!player.isAlive() || player.isCreative() || player.isSpectator()
                        || !player.getWorld().getRegistryKey().equals(active.world.getRegistryKey())) {
                    ACTIVE_KILLS.remove(player.getUuid());
                    if (active.grabber != null) active.grabber.discard();
                    continue;
                }
                if (active.world.getTime() >= active.killAt) {
                    ACTIVE_KILLS.remove(player.getUuid());
                    if (active.grabber != null) active.grabber.discard();
                    player.kill();
                }
                continue;
            }

            Ambush ambush = PENDING.get(player.getUuid());
            if (ambush == null) continue;
            if (!player.isAlive() || !player.getWorld().getRegistryKey().equals(ambush.dimension)) {
                PENDING.remove(player.getUuid());
                continue;
            }
            ServerWorld world = (ServerWorld) player.getWorld();
            double distance = player.getPos().squaredDistanceTo(Vec3d.ofCenter(ambush.position));
            if (distance > TRIGGER_RADIUS * TRIGGER_RADIUS) continue;

            // Returning to the remembered position in light level 3+ permanently breaks this ambush.
            if (world.getLightLevel(player.getBlockPos()) >= 3) {
                PENDING.remove(player.getUuid());
                continue;
            }
            if (world.getTime() < ambush.dueTime) continue;

            PENDING.remove(player.getUuid());
            StalkerEntity grabber = spawnGrabberBehind(player, world);
            // Never schedule the lethal finish if the visual grabber failed to spawn.
            if (grabber == null) continue;
            // Changing a ServerPlayerEntity's yaw alone never reaches the client camera;
            // a teleport request with the new rotation is what actually turns the player around.
            float turnedYaw = player.getYaw() + 180.0f;
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), turnedYaw, player.getPitch());
            player.setHeadYaw(turnedYaw);
            player.setBodyYaw(turnedYaw);
            ServerPlayNetworking.send(player, new JumpscarePayload());
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 2.0f, 0.65f);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 2.0f, 0.55f);
            ACTIVE_KILLS.put(player.getUuid(), new ActiveScare(world, grabber, world.getTime() + 10L));
        }
    }

    private static StalkerEntity spawnGrabberBehind(ServerPlayerEntity player, ServerWorld world) {
        Vec3d behind = player.getPos().subtract(player.getRotationVec(1.0f).multiply(0.78));
        BlockPos spawnBlock = BlockPos.ofFloored(behind.x, behind.y, behind.z);
        StalkerEntity grabber = ModEntities.STALKER.spawn(world, spawnBlock, SpawnReason.EVENT);
        if (grabber == null) return null;
        float yaw = (float) (Math.atan2(player.getZ() - behind.z, player.getX() - behind.x) * 180.0 / Math.PI) - 90.0f;
        grabber.refreshPositionAndAngles(behind.x, behind.y, behind.z, yaw, 0.0f);
        grabber.prepareJumpscare();
        return grabber;
    }

    private record Ambush(net.minecraft.registry.RegistryKey<World> dimension, BlockPos position, long dueTime) {}
    private record ActiveScare(ServerWorld world, StalkerEntity grabber, long killAt) {}
}
