package com.example.thestalker.entity;

import com.example.thestalker.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;

/** The mobile Hunter. Its compact collision profile lets it navigate one-block-high tunnels. */
public class StalkerEntity extends HostileEntity {
    private static final TrackedData<Boolean> JUMPSCARE_POSE = DataTracker.registerData(StalkerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TRAVEL_ANIMATION = DataTracker.registerData(StalkerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TRAVEL_PROGRESS = DataTracker.registerData(StalkerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private boolean jumpscareDummy;
    private boolean orphanedJumpscare;
    private int torchSearchCooldown;
    private int lightScreechCooldown;
    private int burningTicks;
    private int darkSearchCooldown;
    private int lostSightTicks;
    private int investigateCooldown;
    private BlockPos lastKnownPlayerPos;
    private boolean crawling;
    private int travelTicks;
    private BlockPos travelExit;
    private BlockPos travelEntrance;
    private boolean travelThroughPainting;
    private float travelFacingYaw;
    private PaintingEntity entrancePainting;
    private PaintingEntity exitPainting;
    private int huntRepositionCooldown;
    private int stalkingPauseTicks;
    private boolean climbing;
    private int trappedBlockBreakTicks;

    /** The Hunter burns, screeches and flees at this light level or brighter. Full daylight is 15. */
    public static final int BURN_LIGHT_LEVEL = 10;
    /** After burning continuously for this long (200 ticks = 10 seconds) it retreats into smoke. */
    public static final int MAX_BURN_TICKS = 200;

    public StalkerEntity(EntityType<? extends HostileEntity> type, World world) { super(type, world); }

    public static DefaultAttributeContainer.Builder createStalkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 70.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 56.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(JUMPSCARE_POSE, false);
        builder.add(TRAVEL_ANIMATION, 0);
        builder.add(TRAVEL_PROGRESS, 0);
    }

    public void prepareJumpscare() {
        jumpscareDummy = true;
        dataTracker.set(JUMPSCARE_POSE, true);
        setAiDisabled(true);
        setNoGravity(true);
        setVelocity(Vec3d.ZERO);
        setPersistent();
    }

    public boolean isJumpscarePose() { return dataTracker.get(JUMPSCARE_POSE); }

    /** Sends this Hunter after a player, remembering where they are in case sight is lost on the way. */
    public void beginStalking(PlayerEntity player) {
        setTarget(player);
        lastKnownPlayerPos = player.getBlockPos().toImmutable();
        lostSightTicks = 0;
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 0.725, true));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.525));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static boolean canSpawn(EntityType<StalkerEntity> type, ServerWorldAccess world,
                                   SpawnReason reason, BlockPos pos, net.minecraft.util.math.random.Random random) {
        return world.getLightLevel(pos) < BURN_LIGHT_LEVEL && world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    @Override public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (jumpscareDummy) nbt.putBoolean("JumpscareDummy", true);
    }

    @Override public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        // The scripted kill is memory-only, so a saved grabber has no purpose after a reload.
        if (nbt.getBoolean("JumpscareDummy")) orphanedJumpscare = true;
    }

    @Override public void tick() {
        if (orphanedJumpscare && !getWorld().isClient) { discard(); return; }
        super.tick();
        if (jumpscareDummy) {
            setVelocity(Vec3d.ZERO);
            return;
        }
        updateCrawlPose();
        if (getWorld().isClient) return;
        ServerWorld world = (ServerWorld) getWorld();
        // Travel state is not saved; never leave a Hunter permanently invisible after a reload.
        if (dataTracker.get(TRAVEL_ANIMATION) == 0 && isInvisible()) setInvisible(false);
        if (tickEnvironmentalTravel(world)) return;
        // Burning for too long drives it back into the dark: it dissolves into smoke and is gone.
        if (isOnFire()) {
            if (++burningTicks > MAX_BURN_TICKS) { retreatIntoSmoke(world); return; }
        } else {
            burningTicks = 0;
        }

        int light = world.getLightLevel(getBlockPos());

        if (light >= BURN_LIGHT_LEVEL) {
            setOnFireFor(2);
            getNavigation().stop();
            if (--lightScreechCooldown <= 0) {
                lightScreechCooldown = 35;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.0f, 0.62f + random.nextFloat() * 0.16f);
            }
            if (age % 3 == 0) world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 4, 0.22, 0.35, 0.22, 0.015);
            // Actively seek a dark pocket instead of standing still and burning forever.
            if (--darkSearchCooldown <= 0) {
                darkSearchCooldown = 12;
                seekDarkness(world);
            }
        }

        if (--torchSearchCooldown <= 0) {
            torchSearchCooldown = 25 + random.nextInt(20);
            extinguishNearbyTorch(world);
        }

        // A brief, unnatural pause when a player is looking directly at it from a distance.
        PlayerEntity target = getTarget() instanceof PlayerEntity p ? p : null;
        boolean truce = target != null && OfferingManager.isProtected(target);
        if (truce && target != null && target.isAlive() && age % 12 == 0) {
            getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 0.64);
        }
        if (target != null) {
            if (canSee(target)) {
                lastKnownPlayerPos = target.getBlockPos().toImmutable();
                lostSightTicks = 0;
            } else if (++lostSightTicks > 140) {
                // Stop chasing an unseen player blindly; investigate their last known position.
                setTarget(null);
                lostSightTicks = 0;
            }
            if (target.squaredDistanceTo(this) > 64 && target.canSee(this)
                    && target.getRotationVec(1.0f).dotProduct(getPos().subtract(target.getEyePos()).normalize()) > 0.985) {
                getNavigation().stop();
            }
        } else if (lastKnownPlayerPos != null) {
            if (getBlockPos().getSquaredDistance(lastKnownPlayerPos) <= 4 || age % 240 == 0) {
                lastKnownPlayerPos = null;
            } else if (--investigateCooldown <= 0) {
                investigateCooldown = 12;
                getNavigation().startMovingTo(lastKnownPlayerPos.getX() + 0.5,
                        lastKnownPlayerPos.getY(), lastKnownPlayerPos.getZ() + 0.5, 0.675);
            }
        }

        // It does not always charge. It occasionally breaks line of sight and approaches from another angle.
        if (target != null && target.isAlive() && target.squaredDistanceTo(this) > 100
                && target.canSee(this) && --huntRepositionCooldown <= 0) {
            huntRepositionCooldown = 45 + random.nextInt(45);
            stalkingPauseTicks = 3 + random.nextInt(5);
            getNavigation().stop();
        }
        if (stalkingPauseTicks > 0) {
            stalkingPauseTicks--;
            if (target != null && target.squaredDistanceTo(this) > 64) getNavigation().stop();
        }

        if (target != null && target.isAlive()) {
            // Keep pursuing, but route through nearby darkness instead of blindly crossing lit floors.
            BlockPos darkApproach = findDarkApproach(world, target);
            if (darkApproach != null) {
                getNavigation().startMovingTo(darkApproach.getX() + 0.5, darkApproach.getY(), darkApproach.getZ() + 0.5, 0.75);
            } else {
                getNavigation().startMovingTo(target, 0.75);
            }
            if (tickClimbing(world, target)) return;
            tickSlowBlockBreaking(world, target);
        } else if (climbing) {
            climbing = false;
            setNoGravity(false);
        }

        // Use a dark container or painting as a short-range ambush route, but only after snuffing nearby flames.
        if (target != null && target.isAlive() && target.squaredDistanceTo(this) > 100
                && target.squaredDistanceTo(this) < 30 * 30 && age % (truce ? 12 : 18) == 0) {
            tryBeginEnvironmentalTravel(world, target);
        }
    }

    /** Slowly chips through a single non-tile block when stuck against it while pursuing a target. */

    /** Picks a dark, walkable approach point close to the target to preserve light as a countermeasure. */
    private BlockPos findDarkApproach(ServerWorld world, PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos best = null;
        int bestLight = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(targetPos, 5, 2, 5)) {
            if (!world.isAir(pos) || !world.isAir(pos.up())
                    || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) continue;
            int light = world.getLightLevel(pos);
            // Reject bright destinations outright; if every nearby point is bright, fall back to direct pursuit.
            if (light >= 8) continue;
            double targetDistance = pos.getSquaredDistance(targetPos);
            double stalkerDistance = pos.getSquaredDistance(getBlockPos());
            if (targetDistance > 25) continue;
            // Prefer darkness first, then a point that is still close to the player and reachable in practice.
            double score = light * 100.0 + targetDistance * 2.0 + stalkerDistance * 0.15;
            if (score < bestDistance) {
                bestDistance = score;
                bestLight = light;
                best = pos.toImmutable();
            }
        }
        return best;
    }

    private void tickSlowBlockBreaking(ServerWorld world, PlayerEntity target) {
        if (target == null || target.squaredDistanceTo(this) > 64 || climbing || isOnFire()) {
            trappedBlockBreakTicks = 0;
            return;
        }
        Vec3d toward = new Vec3d(target.getX() - getX(), 0, target.getZ() - getZ());
        if (toward.lengthSquared() < 0.0001) { trappedBlockBreakTicks = 0; return; }
        toward = toward.normalize();
        BlockPos obstruction = getBlockPos().offset(Math.abs(toward.x) > Math.abs(toward.z)
                ? (toward.x > 0 ? net.minecraft.util.math.Direction.EAST : net.minecraft.util.math.Direction.WEST)
                : (toward.z > 0 ? net.minecraft.util.math.Direction.SOUTH : net.minecraft.util.math.Direction.NORTH));
        BlockState state = world.getBlockState(obstruction);
        boolean blocked = !state.isAir() && !state.getCollisionShape(world, obstruction).isEmpty()
                && getNavigation().isIdle() && state.getHardness(world, obstruction) >= 0
                && !state.hasBlockEntity();
        if (!blocked) { trappedBlockBreakTicks = 0; return; }
        trappedBlockBreakTicks++;
        // About 45 seconds per block: deliberately slow and only when movement is obstructed.
        if (trappedBlockBreakTicks >= 900) {
            world.breakBlock(obstruction, true);
            trappedBlockBreakTicks = 0;
            world.playSound(null, obstruction, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.45f, 0.7f);
        }
    }


    private boolean tickClimbing(ServerWorld world, PlayerEntity target) {
        double verticalGap = target.getY() - getY();
        double horizontalGap = Math.sqrt(target.squaredDistanceTo(getX(), target.getY(), getZ()));
        if (verticalGap < 1.6 || horizontalGap > 5.5 || isOnFire()
                || dataTracker.get(TRAVEL_ANIMATION) != 0) {
            if (climbing) {
                climbing = false;
                setNoGravity(false);
            }
            return false;
        }

        Vec3d horizontal = new Vec3d(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();
        BlockPos forward = getBlockPos().offset(
                Math.abs(horizontal.x) > Math.abs(horizontal.z)
                        ? (horizontal.x > 0 ? net.minecraft.util.math.Direction.EAST : net.minecraft.util.math.Direction.WEST)
                        : (horizontal.z > 0 ? net.minecraft.util.math.Direction.SOUTH : net.minecraft.util.math.Direction.NORTH));
        boolean wall = !world.getBlockState(forward).getCollisionShape(world, forward).isEmpty()
                || !world.getBlockState(forward.up()).getCollisionShape(world, forward.up()).isEmpty()
                || !world.getBlockState(forward.up(2)).getCollisionShape(world, forward.up(2)).isEmpty();
        if (!wall) {
            if (climbing) {
                climbing = false;
                setNoGravity(false);
            }
            return false;
        }

        climbing = true;
        setNoGravity(true);
        getNavigation().stop();
        double speed = 0.09;
        setVelocity(horizontal.x * speed, Math.min(0.16, 0.09 + verticalGap * 0.0125), horizontal.z * speed);
        return true;
    }

    private boolean tickEnvironmentalTravel(ServerWorld world) {
        int state = dataTracker.get(TRAVEL_ANIMATION);
        if (state == 0) return false;
        travelTicks++;
        dataTracker.set(TRAVEL_PROGRESS, travelTicks);
        getNavigation().stop();
        setVelocity(Vec3d.ZERO);

        // Phase 1: the Hunter melts into the darkness. It does not need a second container to travel from.
        if (state == 1) {
            if (travelTicks == 1) {
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.55f);
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.45, getZ(), 22, 0.3, 0.45, 0.3, 0.025);
                setInvisible(true);
            }
            if (travelTicks >= 16) {
                if (travelExit != null) {
                    Vec3d emergence = getEmergencePosition(world, travelExit, travelThroughPainting);
                    refreshPositionAndAngles(emergence.x, emergence.y, emergence.z, travelFacingYaw, 0);
                }
                dataTracker.set(TRAVEL_ANIMATION, 2);
                travelTicks = 0;
                dataTracker.set(TRAVEL_PROGRESS, 0);
            }
            return true;
        }

        // Phase 2: a slow, deliberate crawl out of the selected hiding place, hands first.
        if (state == 2) {
            BlockPos effectPos = travelExit == null ? getBlockPos() : travelExit;
            boolean chestExit = !travelThroughPainting && travelExit != null
                    && (world.getBlockState(travelExit).isOf(Blocks.CHEST)
                    || world.getBlockState(travelExit).isOf(Blocks.TRAPPED_CHEST));

            if (travelTicks == 1) {
                setInvisible(false);
                world.playSound(null, effectPos,
                        travelThroughPainting ? SoundEvents.ENTITY_ENDERMAN_STARE : SoundEvents.BLOCK_CHEST_OPEN,
                        SoundCategory.BLOCKS, 1.0f, 0.55f);
                // The first burst is deliberately small: it should frame the hands, not hide the model.
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.25, getZ(), 12, 0.22, 0.22, 0.22, 0.018);
                world.spawnParticles(ParticleTypes.PORTAL, getX(), getY() + 0.35, getZ(), 10, 0.16, 0.22, 0.16, 0.045);
                if (chestExit) world.addSyncedBlockEvent(travelExit, world.getBlockState(travelExit).getBlock(), 1, 1);
            }

            // Follow the animation's beats with small effects at the rim and where the body drags free.
            if (travelTicks == 9) {
                world.spawnParticles(ParticleTypes.SMOKE, effectPos.getX() + 0.5, effectPos.getY() + 0.9,
                        effectPos.getZ() + 0.5, 8, 0.18, 0.12, 0.18, 0.012);
            } else if (travelTicks == 19) {
                world.spawnParticles(ParticleTypes.PORTAL, effectPos.getX() + 0.5, effectPos.getY() + 0.82,
                        effectPos.getZ() + 0.5, 14, 0.24, 0.14, 0.24, 0.035);
                if (chestExit) world.playSound(null, effectPos, SoundEvents.BLOCK_CHEST_OPEN,
                        SoundCategory.BLOCKS, 0.38f, 0.42f);
            } else if (travelTicks == 34) {
                world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.12, getZ(), 9,
                        0.22, 0.05, 0.22, 0.012);
                world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 0.28, getZ(), 7,
                        0.18, 0.16, 0.18, 0.01);
            } else if (travelTicks == 49) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.35, getZ(), 10,
                        0.2, 0.18, 0.2, 0.012);
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.HOSTILE, 0.28f, 0.48f);
            }

            if (travelTicks >= 72) {
                if (chestExit) world.addSyncedBlockEvent(travelExit, world.getBlockState(travelExit).getBlock(), 1, 0);
                dataTracker.set(TRAVEL_ANIMATION, 0);
                travelTicks = 0;
                dataTracker.set(TRAVEL_PROGRESS, 0);
                travelExit = null;
                exitPainting = null;
                travelThroughPainting = false;
                setTarget(null);
                stalkingPauseTicks = 30;
            }
            return true;
        }
        return false;
    }

    private Vec3d getEmergencePosition(ServerWorld world, BlockPos hidingPlace, boolean painting) {
        if (painting && exitPainting != null && exitPainting.isAlive()) {
            net.minecraft.util.math.Direction facing = exitPainting.getHorizontalFacing();
            return new Vec3d(exitPainting.getX() + facing.getOffsetX() * 0.75,
                    exitPainting.getY() + 0.15, exitPainting.getZ() + facing.getOffsetZ() * 0.75);
        }
        BlockState state = world.getBlockState(hidingPlace);
        net.minecraft.util.math.Direction facing = state.contains(net.minecraft.block.HorizontalFacingBlock.FACING)
                ? state.get(net.minecraft.block.HorizontalFacingBlock.FACING) : getHorizontalFacing();
        // Chests/furnaces are solid props: the creature emerges from their player-facing side.
        BlockPos front = hidingPlace.offset(facing);
        if (!world.getBlockState(front).isAir() || !world.getBlockState(front.up()).isAir()) front = hidingPlace.offset(facing.getOpposite());
        return new Vec3d(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
    }

    /** A home ambush is local to the player: the Hunter vanishes, then crawls out of one nearby prop. */
    private void tryBeginEnvironmentalTravel(ServerWorld world, PlayerEntity target) {
        if (dataTracker.get(TRAVEL_ANIMATION) != 0 || stalkingPauseTicks > 0 || random.nextFloat() > 0.42f) return;
        BlockPos hidingPlace = findHomeHidingPlace(world, target.getBlockPos(), 11);
        if (hidingPlace == null) return;

        // The extinguishing is deliberately visible and happens before the creature vanishes.
        extinguishAllNearbyFlames(world, hidingPlace, 8);
        if (hasNearbyFlame(world, hidingPlace, 8)) return;

        exitPainting = findPainting(world, target.getPos(), 11);
        boolean painting = exitPainting != null && exitPainting.getBlockPos().equals(hidingPlace);
        if (painting) {
            // The painting entity itself is the hiding place; its backing block need not be air.
        } else {
            exitPainting = null;
        }
        travelExit = hidingPlace.toImmutable();
        travelThroughPainting = painting;
        Vec3d towardPlayer = target.getPos().subtract(Vec3d.ofCenter(hidingPlace));
        travelFacingYaw = towardPlayer.lengthSquared() > 0.001
                ? (float) (Math.atan2(towardPlayer.z, towardPlayer.x) * 57.29577951308232 - 90.0)
                : getYaw();
        travelTicks = 0;
        dataTracker.set(TRAVEL_ANIMATION, 1);
        setTarget(null);
        getNavigation().stop();
    }

    private BlockPos findHomeHidingPlace(ServerWorld world, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, 4, radius)) {
            BlockState state = world.getBlockState(pos);
            boolean chest = state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
            boolean furnace = state.isOf(Blocks.FURNACE) && (!state.contains(net.minecraft.block.AbstractFurnaceBlock.LIT)
                    || !state.get(net.minecraft.block.AbstractFurnaceBlock.LIT));
            if (!chest && !furnace) continue;
            BlockPos front = pos.offset(state.contains(net.minecraft.block.HorizontalFacingBlock.FACING)
                    ? state.get(net.minecraft.block.HorizontalFacingBlock.FACING) : getHorizontalFacing());
            if (!world.getBlockState(front).isAir() || !world.getBlockState(front.up()).isAir()) continue;
            double d = pos.getSquaredDistance(center);
            if (d < bestDistance) { best = pos.toImmutable(); bestDistance = d; }
        }
        if (best != null) return best;
        PaintingEntity painting = findPainting(world, Vec3d.ofCenter(center), radius);
        return painting == null ? null : painting.getBlockPos().toImmutable();
    }

    private PaintingEntity findPainting(ServerWorld world, Vec3d center, double radius) {
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(center.x-radius, center.y-radius, center.z-radius,
                center.x+radius, center.y+radius, center.z+radius);
        return world.getOtherEntities(this, box, e -> e instanceof PaintingEntity)
                .stream().filter(e -> e instanceof PaintingEntity).map(e -> (PaintingEntity)e)
                .min((a,b) -> Double.compare(a.squaredDistanceTo(center.x, center.y, center.z), b.squaredDistanceTo(center.x, center.y, center.z))).orElse(null);
    }

    private void extinguishAllNearbyFlames(ServerWorld world, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, 4, radius)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                BlockState replacement = state.isOf(Blocks.WALL_TORCH)
                        ? ModBlocks.UNLIT_WALL_TORCH.getDefaultState().with(net.minecraft.block.WallTorchBlock.FACING, state.get(net.minecraft.block.WallTorchBlock.FACING))
                        : ModBlocks.UNLIT_TORCH.getDefaultState();
                world.setBlockState(pos, replacement, 3);
                puffSmoke(world, pos);
            } else if (state.isOf(Blocks.LANTERN)) {
                world.setBlockState(pos, ModBlocks.UNLIT_LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, state.get(net.minecraft.block.LanternBlock.HANGING)), 3);
                puffSmoke(world, pos);
            } else if (state.isOf(Blocks.SOUL_LANTERN)) {
                world.setBlockState(pos, ModBlocks.UNLIT_SOUL_LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, state.get(net.minecraft.block.LanternBlock.HANGING)), 3);
                puffSmoke(world, pos);
            } else if (state.getBlock() instanceof CandleBlock && state.contains(CandleBlock.LIT) && state.get(CandleBlock.LIT)) {
                CandleBlock.extinguish(null, state, world, pos); puffSmoke(world, pos);
            } else if (state.isOf(Blocks.CAMPFIRE) && state.get(CampfireBlock.LIT)) {
                CampfireBlock.extinguish(null, world, pos, state); puffSmoke(world, pos);
            }
        }
    }

    private void puffSmoke(ServerWorld world, BlockPos pos) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX()+0.5, pos.getY()+0.65, pos.getZ()+0.5, 10, 0.16, 0.22, 0.16, 0.025);
        world.playSound(null, pos, SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 0.75f, 0.65f);
    }

    private boolean hasNearbyFlame(ServerWorld world, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, 4, radius)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH) || state.isOf(Blocks.LANTERN) || state.isOf(Blocks.SOUL_LANTERN)) return true;
            if (state.getBlock() instanceof CandleBlock && state.contains(CandleBlock.LIT) && state.get(CandleBlock.LIT)) return true;
            if (state.isOf(Blocks.CAMPFIRE) && state.get(CampfireBlock.LIT)) return true;
        }
        return false;
    }


    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player && OfferingManager.isProtected(player)) {
            // During a paid truce the Hunter may approach and threaten, but cannot hurt the player.
            if (getWorld() instanceof ServerWorld serverWorld)
                serverWorld.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8, target.getZ(), 3, 0.18, 0.22, 0.18, 0.01);
            return false;
        }
        return super.tryAttack(target);
    }

    private void updateCrawlPose() {
        BlockPos overhead = getBlockPos().up();
        boolean next = !getWorld().getBlockState(overhead).isAir();
        if (next != crawling) {
            crawling = next;
            calculateDimensions();
        }
    }

    public boolean isCrawling() { return crawling; }
    public boolean isClimbing() { return climbing; }
    public int getTravelAnimation() { return dataTracker.get(TRAVEL_ANIMATION); }
    public int getTravelAnimationTicks() { return dataTracker.get(TRAVEL_PROGRESS); }

    @Override public net.minecraft.entity.EntityDimensions getBaseDimensions(net.minecraft.entity.EntityPose pose) {
        // The hitbox follows the visible model: tall while standing, compact only while crawling
        // under a low ceiling, with a matching eye height so line-of-sight checks make sense.
        if (crawling) return net.minecraft.entity.EntityDimensions.changing(0.49f, 0.49f).withEyeHeight(0.36f);
        return net.minecraft.entity.EntityDimensions.changing(0.7f, 1.95f).withEyeHeight(1.7f);
    }

    private void retreatIntoSmoke(ServerWorld world) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 60, 0.4, 0.8, 0.4, 0.06);
        world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 40, 0.5, 0.9, 0.5, 0.05);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);
        discard();
    }

    private void seekDarkness(ServerWorld world) {
        BlockPos origin = getBlockPos();
        BlockPos best = null;
        int bestLight = world.getLightLevel(origin);
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, 10, 4, 10)) {
            if (!world.isAir(pos) || !world.isAir(pos.up()) || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) continue;
            int light = world.getLightLevel(pos);
            double distance = pos.getSquaredDistance(origin);
            if (light < bestLight || (light == bestLight && distance < bestDistance)) {
                best = pos.toImmutable(); bestLight = light; bestDistance = distance;
            }
        }
        if (best != null && bestLight < BURN_LIGHT_LEVEL) getNavigation().startMovingTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, 1.15);
    }

    private void extinguishNearbyTorch(ServerWorld world) {
        BlockPos origin = getBlockPos();
        int extinguished = 0;
        // Search the full radius and extinguish several visible torch sources in one pass.
        for (BlockPos pos : BlockPos.iterateOutwards(origin, 8, 4, 8)) {
            if (extinguished >= 5) break;
            BlockState old = world.getBlockState(pos);
            boolean floorTorch = old.isOf(Blocks.TORCH);
            boolean wallTorch = old.isOf(Blocks.WALL_TORCH);
            if (!floorTorch && !wallTorch) continue;
            if (pos.getSquaredDistance(origin) > 64) continue;
            HitResult hit = world.raycast(new RaycastContext(
                    getPos().add(0, crawling ? 0.55 : 1.1, 0),
                    Vec3d.ofCenter(pos), RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, this));
            if (!(hit instanceof net.minecraft.util.hit.BlockHitResult blockHit)
                    || !blockHit.getBlockPos().equals(pos)) continue;
            BlockState replacement = wallTorch
                    ? ModBlocks.UNLIT_WALL_TORCH.getDefaultState().with(
                        net.minecraft.block.WallTorchBlock.FACING,
                        old.get(net.minecraft.block.WallTorchBlock.FACING))
                    : ModBlocks.UNLIT_TORCH.getDefaultState();
            if (world.setBlockState(pos, replacement, 3)) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8,
                        pos.getZ() + 0.5, 5, 0.12, 0.18, 0.12, 0.025);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8,
                        pos.getZ() + 0.5, 12, 0.08, 0.12, 0.08, 0.015);
                world.playSound(null, pos, SoundEvents.BLOCK_CANDLE_EXTINGUISH,
                        SoundCategory.BLOCKS, 0.9f, 0.65f);
                extinguished++;
            }
        }
    }
}
