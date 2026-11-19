package com.example.thestalker.entity;

import com.example.thestalker.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A "wave" of darkness: a ring of cold air expands outward from a point and snuffs up to a handful of
 * lights (torches, lanterns, candles, campfires) as it reaches them, nearest first.
 */
public final class LightWaveManager {
    /** The most lights a single wave can put out. */
    public static final int MAX_SOURCES = 5;
    /** How fast the ring expands, in blocks per tick. */
    private static final double WAVE_SPEED = 0.8;

    private static final List<Wave> WAVES = new ArrayList<>();

    private LightWaveManager() {}

    /** Starts a wave. Returns false (and does nothing) if there is no light within range to snuff. */
    public static boolean start(ServerWorld world, Vec3d origin, int radius, int maxSources) {
        List<Target> found = new ArrayList<>();
        double radiusSq = (double) radius * radius;
        for (BlockPos pos : BlockPos.iterateOutwards(BlockPos.ofFloored(origin), radius, Math.max(3, radius / 2), radius)) {
            if (!isSnuffable(world.getBlockState(pos))) continue;
            double distanceSq = Vec3d.ofCenter(pos).squaredDistanceTo(origin);
            if (distanceSq > radiusSq) continue;
            found.add(new Target(pos.toImmutable(), Math.sqrt(distanceSq)));
        }
        if (found.isEmpty()) return false;

        found.sort(Comparator.comparingDouble(Target::distance));
        List<Target> chosen = new ArrayList<>(found.subList(0, Math.min(maxSources, found.size())));
        WAVES.add(new Wave(world.getRegistryKey(), origin, radius, chosen));
        world.playSound(null, BlockPos.ofFloored(origin), SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 1.0f, 0.5f);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (WAVES.isEmpty()) return;
        Iterator<Wave> it = WAVES.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            ServerWorld world = server.getWorld(wave.dimension);
            if (world == null) { it.remove(); continue; }

            wave.age++;
            double ring = wave.age * WAVE_SPEED;
            spawnRing(world, wave.origin, ring);
            wave.targets.removeIf(target -> {
                if (target.distance() > ring) return false;
                snuff(world, target.pos(), wave.origin);
                return true;
            });
            if (ring >= wave.radius + 1.0) it.remove();
        }
    }

    /** Drops any waves still in flight, for example when the server stops. */
    public static void clear() { WAVES.clear(); }

    private static void spawnRing(ServerWorld world, Vec3d origin, double radius) {
        int points = Math.max(8, Math.min(36, (int) (radius * 2.2)));
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points + world.random.nextDouble() * 0.12;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            // With a count of 0 the "offset" is the particle's direction, so the puffs drift outward.
            world.spawnParticles(ParticleTypes.CLOUD,
                    origin.x + dx * radius, origin.y + 0.4 + world.random.nextDouble() * 0.9, origin.z + dz * radius,
                    0, dx, 0.0, dz, 0.12);
        }
    }

    private static boolean isSnuffable(BlockState state) {
        if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)
                || state.isOf(Blocks.LANTERN) || state.isOf(Blocks.SOUL_LANTERN)) return true;
        if (state.getBlock() instanceof CandleBlock && state.contains(CandleBlock.LIT) && state.get(CandleBlock.LIT)) return true;
        return state.getBlock() instanceof CampfireBlock && state.contains(CampfireBlock.LIT) && state.get(CampfireBlock.LIT);
    }

    private static void snuff(ServerWorld world, BlockPos pos, Vec3d origin) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.TORCH)) {
            world.setBlockState(pos, ModBlocks.UNLIT_TORCH.getDefaultState(), 3);
        } else if (state.isOf(Blocks.WALL_TORCH)) {
            world.setBlockState(pos, ModBlocks.UNLIT_WALL_TORCH.getDefaultState()
                    .with(WallTorchBlock.FACING, state.get(WallTorchBlock.FACING)), 3);
        } else if (state.isOf(Blocks.LANTERN)) {
            world.setBlockState(pos, ModBlocks.UNLIT_LANTERN.getDefaultState()
                    .with(LanternBlock.HANGING, state.get(LanternBlock.HANGING))
                    .with(LanternBlock.WATERLOGGED, state.get(LanternBlock.WATERLOGGED)), 3);
        } else if (state.isOf(Blocks.SOUL_LANTERN)) {
            world.setBlockState(pos, ModBlocks.UNLIT_SOUL_LANTERN.getDefaultState()
                    .with(LanternBlock.HANGING, state.get(LanternBlock.HANGING))
                    .with(LanternBlock.WATERLOGGED, state.get(LanternBlock.WATERLOGGED)), 3);
        } else if (state.getBlock() instanceof CandleBlock && state.contains(CandleBlock.LIT) && state.get(CandleBlock.LIT)) {
            CandleBlock.extinguish(null, state, world, pos);
        } else if (state.getBlock() instanceof CampfireBlock && state.contains(CampfireBlock.LIT) && state.get(CampfireBlock.LIT)) {
            CampfireBlock.extinguish(null, world, pos, state);
        } else {
            return; // changed since the wave was cast
        }

        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.16, 0.22, 0.16, 0.025);
        // A short gust blown away from the source of the wave.
        Vec3d away = new Vec3d(x - origin.x, 0.0, z - origin.z);
        if (away.lengthSquared() > 1.0E-4) away = away.normalize();
        for (int i = 0; i < 6; i++) {
            world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 0, away.x, 0.04, away.z, 0.14 + world.random.nextDouble() * 0.08);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 0.9f, 0.6f + world.random.nextFloat() * 0.2f);
    }

    private record Target(BlockPos pos, double distance) {}

    private static final class Wave {
        final RegistryKey<World> dimension;
        final Vec3d origin;
        final int radius;
        final List<Target> targets;
        int age;

        Wave(RegistryKey<World> dimension, Vec3d origin, int radius, List<Target> targets) {
            this.dimension = dimension; this.origin = origin; this.radius = radius; this.targets = targets;
        }
    }
}
