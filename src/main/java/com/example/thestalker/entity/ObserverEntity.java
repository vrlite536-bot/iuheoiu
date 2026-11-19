package com.example.thestalker.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

import java.util.List;

/** A deliberately passive, mostly-static sighting. It is not the combat creature. */
public class ObserverEntity extends net.minecraft.entity.mob.HostileEntity {
    private static final TrackedData<Integer> DISSOLVE_TICKS = DataTracker.registerData(ObserverEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private PlayerEntity watchedPlayer;
    private int brightTicks;
    private int screechCooldown;
    private boolean exhaledWave;

    /** A player this close makes the Observer exhale a wave that snuffs nearby lights (once). */
    private static final double WAVE_TRIGGER_RANGE = 16.0;
    /** How far from the Observer the wave reaches, in blocks. */
    private static final int WAVE_RADIUS = 16;

    public ObserverEntity(EntityType<? extends ObserverEntity> type, World world) { super(type, world); }

    public static boolean canSpawn(EntityType<ObserverEntity> type, ServerWorldAccess world,
                                   SpawnReason reason, BlockPos pos, net.minecraft.util.math.random.Random random) {
        return world.getLightLevel(pos) <= 3 && world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DISSOLVE_TICKS, -1);
    }

    @Override protected void initGoals() { /* Observer intentionally has no conventional AI goals. */ }

    /** Vanilla stops a pack after 4 mobs per chunk; Observers spawn in packs of up to 10. */
    @Override public int getLimitPerChunk() { return 10; }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        ServerWorld world = (ServerWorld) getWorld();

        int dissolveTicks = dataTracker.get(DISSOLVE_TICKS);
        if (dissolveTicks >= 0) {
            emitDissolve(world, dissolveTicks);
            if (dissolveTicks + 1 >= 12) discard();
            else dataTracker.set(DISSOLVE_TICKS, dissolveTicks + 1);
            return;
        }

        int light = world.getLightLevel(getBlockPos());
        if (light >= 4) {
            setOnFireFor(2);
            if (--screechCooldown <= 0) {
                screechCooldown = 35;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 0.8f, 0.58f);
            }
            if (age % 3 == 0) world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 4, 0.18, 0.28, 0.18, 0.015);
            if (++brightTicks >= 24) { discard(); return; }
        } else {
            brightTicks = 0;
        }
        if (!exhaledWave) {
            boolean playerNear = !world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                    && p.squaredDistanceTo(this) < WAVE_TRIGGER_RANGE * WAVE_TRIGGER_RANGE).isEmpty();
            if (playerNear) {
                exhaledWave = true; // only ever once, even if there was nothing to snuff
                LightWaveManager.start(world, getPos().add(0, 0.8, 0), WAVE_RADIUS, LightWaveManager.MAX_SOURCES);
            }
        }
        List<ServerPlayerEntity> nearby = world.getPlayers(p -> p.isAlive() && !p.isCreative()
                && !p.isSpectator() && p.squaredDistanceTo(this) < 20.25);
        if (!nearby.isEmpty()) {
            // Select the closest eligible player deterministically; getPlayers() ordering
            // is not a reliable way to choose which player the Observer is watching.
            ServerPlayerEntity player = nearby.stream()
                    .min((a, b) -> Double.compare(a.squaredDistanceTo(this), b.squaredDistanceTo(this)))
                    .orElse(null);
            if (player != null) {
                watchedPlayer = player;
                float yaw = (float) (Math.atan2(player.getZ() - getZ(), player.getX() - getX()) * 180.0 / Math.PI) - 90.0f;
                setYaw(yaw); setBodyYaw(yaw); setHeadYaw(yaw);
                beginDissolve(world, player);
            }
        }
    }

    public float getDissolveProgress(float tickDelta) {
        int ticks = dataTracker.get(DISSOLVE_TICKS);
        return ticks < 0 ? 0.0f : Math.min(1.0f, (ticks + tickDelta) / 12.0f);
    }

    private void beginDissolve(ServerWorld world, PlayerEntity player) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) { discard(); return; }
        dataTracker.set(DISSOLVE_TICKS, 0);
        AmbushManager.schedule(serverPlayer, world, getBlockPos(), world.getTime(), 200 + random.nextInt(201));
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.2f, 0.55f);
    }

    private void emitDissolve(ServerWorld world, int dissolveTicks) {
        double scale = 0.25 + dissolveTicks * 0.09;
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * scale;
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    getX() + Math.cos(angle) * radius, getY() + random.nextDouble() * getHeight(),
                    getZ() + Math.sin(angle) * radius, 1, 0.015, 0.035, 0.015, 0.01);
        }
        if (dissolveTicks % 3 == 0) world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 8, 0.25, 0.5, 0.25, 0.02);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean canHit() { return false; }
}
