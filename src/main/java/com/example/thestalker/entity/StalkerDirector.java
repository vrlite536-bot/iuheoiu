package com.example.thestalker.entity;

import com.example.thestalker.registry.ModEntities;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/**
 * Keeps a Hunter on every survival player's trail from the very first minute of a world, instead of
 * waiting for the vanilla mob spawner to roll one. Tune the constants below to taste.
 */
public final class StalkerDirector {
    /** How often each player is checked, in ticks (20 ticks = 1 second). */
    private static final int CHECK_INTERVAL_TICKS = 200;
    /** Chance per check that a Hunter is sent after a player who has none nearby. */
    private static final double SPAWN_CHANCE = 0.5;
    /** Chance per check that an idle Hunter already nearby is pointed at the player again. */
    private static final double REFOCUS_CHANCE = 0.6;
    /** New Hunters appear out of the player's sight, this far away (blocks). */
    private static final double MIN_SPAWN_DISTANCE = 22.0;
    private static final double MAX_SPAWN_DISTANCE = 38.0;
    /** A Hunter within this many blocks counts as "already has one nearby". */
    private static final double PRESENCE_RADIUS = 64.0;

    private StalkerDirector() {}

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % CHECK_INTERVAL_TICKS != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            ServerWorld world = player.getServerWorld();
            if (world.getRegistryKey() != World.OVERWORLD || world.getDifficulty() == Difficulty.PEACEFUL) continue;

            List<StalkerEntity> nearby = world.getEntitiesByClass(StalkerEntity.class,
                    player.getBoundingBox().expand(PRESENCE_RADIUS), s -> s.isAlive() && !s.isJumpscarePose());
            if (!nearby.isEmpty()) {
                if (world.random.nextDouble() < REFOCUS_CHANCE) {
                    for (StalkerEntity stalker : nearby) {
                        if (stalker.getTarget() == null) stalker.beginStalking(player);
                    }
                }
                continue;
            }

            if (world.random.nextDouble() >= SPAWN_CHANCE) continue;
            BlockPos spawnPos = findSpawnPosition(world, player);
            if (spawnPos == null) continue;
            StalkerEntity stalker = ModEntities.STALKER.spawn(world, spawnPos, SpawnReason.EVENT);
            if (stalker != null) stalker.beginStalking(player);
        }
    }

    private static BlockPos findSpawnPosition(ServerWorld world, ServerPlayerEntity player) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = MIN_SPAWN_DISTANCE + world.random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
            int x = MathHelper.floor(player.getX() + Math.cos(angle) * distance);
            int z = MathHelper.floor(player.getZ() + Math.sin(angle) * distance);
            if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) continue;

            int top = Math.min(player.getBlockY() + 8, world.getTopY() - 3);
            int bottom = Math.max(player.getBlockY() - 12, world.getBottomY() + 1);
            for (int y = top; y >= bottom; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!isStandable(world, pos)) continue;
                if (world.getLightLevel(pos) >= StalkerEntity.BURN_LIGHT_LEVEL) continue;
                if (playerCanSee(world, player, pos)) continue;
                return pos;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerWorld world, BlockPos pos) {
        BlockPos floor = pos.down();
        BlockPos head = pos.up();
        return world.getBlockState(floor).isSolidBlock(world, floor)
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(head).getCollisionShape(world, head).isEmpty()
                && world.getFluidState(pos).isEmpty()
                && world.getFluidState(head).isEmpty();
    }

    private static boolean playerCanSee(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos).add(0, 1.0, 0);
        return world.raycast(new RaycastContext(player.getEyePos(), target,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player))
                .getType() == HitResult.Type.MISS;
    }
}
