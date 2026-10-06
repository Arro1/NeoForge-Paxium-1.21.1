package net.arro.paxium.entity.custom;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.arro.paxium.client.PaxiumClientHelper;
import net.arro.paxium.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// The Paxium Bomb's detonation. An invisible controller that runs the whole event over time:
//   IMPACT     - sound, entity damage/knockback, screen shake (tick 0)
//   PRECOMPUTE - builds the noise-shaped crater geometry over a few ticks (no single-tick hitch)
//   CARVING    - removes the crater outward from the center, ~2 s, edge fading into debris; walls and
//                floor turn to magma/blackstone/basalt/netherrack (lava, fire) the moment they're exposed
//   TRANSFORM  - scorches and ignites the rim around the crater
//   VOLCANO    - ~60 s of eruptions: lava/smoke columns and molten "volcano bombs" from the lake
// Visuals (fireball, shockwave, fire column) live in PaxiumBlastRenderer and clientTick().
public class PaxiumBlastEntity extends Entity {
    public static final int RADIUS = 50;
    // Below the center the crater is squashed into a bowl (depth ~30); above it stays full height.
    private static final double DOWN_SCALE = 0.6;
    private static final double NOISE_BASE = 0.82;
    private static final double NOISE_AMPLITUDE = 0.22;
    private static final double NOISE_FREQUENCY = 1.8;
    // Between this ratio and 1.0 of the local radius, blocks survive with rising probability.
    private static final double EDGE_FADE_START = 0.85;
    private static final int MAX_EXTENT = (int) Math.ceil(RADIUS * (NOISE_BASE + NOISE_AMPLITUDE)) + 2;

    private static final int PRECOMPUTE_TICKS = 10;
    private static final int CARVE_TICKS = 40;
    private static final int BUCKETS = 80;
    private static final int MAX_BLOCKS_PER_TICK = 10000;
    private static final int VOLCANO_TICKS = 1200;
    private static final int LAKE_RADIUS = 7;

    private static final int CARVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private enum Phase { IMPACT, PRECOMPUTE, CARVING, TRANSFORM, VOLCANO }

    private Phase phase = Phase.IMPACT;
    private int phaseAge = 0;
    @Nullable
    private LivingEntity owner;

    // Server-side working state; rebuilt (not saved) if the world reloads mid-blast.
    private ImprovedNoise noise;
    private LongArrayList[] buckets;
    private int bucketCursor = 0;
    private int bucketIndex = 0;
    private Long2IntOpenHashMap columnFloor;   // packed (x, 0, z) -> lowest carved y
    private LongOpenHashSet wallSamples;       // edge positions whose neighbours get scorched when carved
    private BlockPos lakeCenter;

    // Game time of detonation, synced so every client times the fireball/column from the real
    // moment of impact - a player arriving later doesn't see a fresh explosion.
    private static final EntityDataAccessor<Long> DATA_BORN_AT =
            SynchedEntityData.defineId(PaxiumBlastEntity.class, EntityDataSerializers.LONG);

    // Client-only: drives the fire column and screen shake.
    private boolean clientImpactHandled = false;

    public PaxiumBlastEntity(EntityType<? extends PaxiumBlastEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public PaxiumBlastEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(ModEntities.PAXIUM_BLAST.get(), level);
        this.setPos(x, y, z);
        this.owner = owner;
        this.entityData.set(DATA_BORN_AT, level.getGameTime());
    }

    // Ticks since detonation, consistent on server and every client.
    public float getVisualAge(float partialTick) {
        return level().getGameTime() - entityData.get(DATA_BORN_AT) + partialTick;
    }

    // Same on both sides (the UUID is synced on spawn), so the client's fireball wobble matches.
    public long getBlastSeed() {
        return getUUID().getLeastSignificantBits();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BORN_AT, 0L);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            clientTick();
            return;
        }

        ServerLevel level = (ServerLevel) level();
        switch (phase) {
            case IMPACT -> {
                impact(level);
                advance(Phase.PRECOMPUTE);
            }
            case PRECOMPUTE -> {
                precomputeSlice(phaseAge);
                if (phaseAge + 1 >= PRECOMPUTE_TICKS) {
                    advance(Phase.CARVING);
                    return;
                }
            }
            case CARVING -> {
                carveTick(level);
                if (bucketIndex >= BUCKETS) {
                    advance(Phase.TRANSFORM);
                    return;
                }
            }
            case TRANSFORM -> {
                if (transformTick(level)) {
                    advance(Phase.VOLCANO);
                    return;
                }
            }
            case VOLCANO -> {
                volcanoTick(level);
                if (phaseAge >= VOLCANO_TICKS) {
                    discard();
                    return;
                }
            }
        }
        phaseAge++;
    }

    private void advance(Phase next) {
        phase = next;
        phaseAge = 0;
    }

    // ---------------------------------------------------------------------------------------------
    // IMPACT
    // ---------------------------------------------------------------------------------------------

    private void impact(ServerLevel level) {
        playFar(level, SoundEvents.GENERIC_EXPLODE.value(), 16.0F, 0.5F);
        playFar(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 20.0F, 0.6F);
        playFar(level, SoundEvents.WARDEN_SONIC_BOOM, 10.0F, 0.6F);
        playFar(level, SoundEvents.DRAGON_FIREBALL_EXPLODE, 12.0F, 0.5F);

        Vec3 center = position();
        double reach = RADIUS * 1.2;
        for (Entity entity : level.getEntities(this, new AABB(center, center).inflate(reach))) {
            double distance = entity.position().distanceTo(center);
            if (distance > reach) {
                continue;
            }

            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                if (distance <= RADIUS) {
                    entity.discard();
                }
                continue;
            }

            if (entity instanceof LivingEntity living) {
                float falloff = (float) Math.max(0.0, 1.0 - distance / RADIUS);
                living.hurt(level.damageSources().explosion(this, owner), 30.0F + 170.0F * falloff);
                living.igniteForSeconds(10.0F);
            }

            Vec3 push = entity.position().subtract(center);
            double strength = 4.0 * Math.max(0.0, 1.0 - distance / reach);
            if (push.lengthSqr() > 1.0E-4 && strength > 0.0) {
                Vec3 velocity = push.normalize().scale(strength).add(0.0, strength * 0.4, 0.0);
                entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
                entity.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // CRATER GEOMETRY
    // ---------------------------------------------------------------------------------------------

    private void ensureGeometryState() {
        if (noise == null) {
            noise = new ImprovedNoise(RandomSource.create(getBlastSeed()));
        }
        if (buckets == null) {
            buckets = new LongArrayList[BUCKETS];
            for (int i = 0; i < BUCKETS; i++) {
                buckets[i] = new LongArrayList();
            }
            columnFloor = new Long2IntOpenHashMap();
            columnFloor.defaultReturnValue(Integer.MAX_VALUE);
            wallSamples = new LongOpenHashSet();
            bucketIndex = 0;
            bucketCursor = 0;
        }
    }

    // Each call handles one x-slice band of the bounding box; PRECOMPUTE_TICKS calls cover it all.
    private void precomputeSlice(int slice) {
        ensureGeometryState();
        BlockPos origin = blockPosition();
        int width = MAX_EXTENT * 2 + 1;
        int fromX = -MAX_EXTENT + slice * width / PRECOMPUTE_TICKS;
        int toX = -MAX_EXTENT + (slice + 1) * width / PRECOMPUTE_TICKS;
        int minDy = (int) -Math.ceil(MAX_EXTENT * DOWN_SCALE);
        long seed = getBlastSeed();

        for (int dx = fromX; dx < toX; dx++) {
            for (int dz = -MAX_EXTENT; dz <= MAX_EXTENT; dz++) {
                for (int dy = minDy; dy <= MAX_EXTENT; dy++) {
                    double sy = dy < 0 ? dy / DOWN_SCALE : dy;
                    double distance = Math.sqrt(dx * dx + sy * sy + dz * dz);
                    double ratio = distance < 1.0E-3 ? 0.0 : distance / localRadius(dx / distance, sy / distance, dz / distance);
                    if (ratio > 1.0) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int y = origin.getY() + dy;
                    int z = origin.getZ() + dz;
                    if (ratio > EDGE_FADE_START) {
                        double keepChance = (ratio - EDGE_FADE_START) / (1.0 - EDGE_FADE_START);
                        if (hash01(x, y, z, seed) < keepChance) {
                            continue;
                        }
                    }

                    long packed = BlockPos.asLong(x, y, z);
                    buckets[Math.min(BUCKETS - 1, (int) (ratio * BUCKETS))].add(packed);

                    long column = BlockPos.asLong(x, 0, z);
                    if (y < columnFloor.get(column)) {
                        columnFloor.put(column, y);
                    }
                    if (ratio > 0.75 && hash01(x, y, z, seed ^ 0x5DEECE66DL) < 0.35) {
                        wallSamples.add(packed);
                    }
                }
            }
        }
    }

    // Lumpy per-direction radius from 3D noise sampled on the unit sphere.
    private double localRadius(double nx, double ny, double nz) {
        double n = noise.noise(nx * NOISE_FREQUENCY, ny * NOISE_FREQUENCY, nz * NOISE_FREQUENCY);
        return RADIUS * (NOISE_BASE + NOISE_AMPLITUDE * n);
    }

    private static double hash01(int x, int y, int z, long seed) {
        long h = Mth.getSeed(x, y, z) ^ seed;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return ((h >>> 11) & 0xFFFFFFL) / (double) 0x1000000;
    }

    // ---------------------------------------------------------------------------------------------
    // CARVING
    // ---------------------------------------------------------------------------------------------

    // Removes every bucket up to the current front, capped per tick so a slow server just stretches
    // the carve instead of freezing. The front follows the cube root of time: the carved volume (and
    // so the work per tick) grows evenly, instead of the dense outer shell all landing at the end.
    private void carveTick(ServerLevel level) {
        if (buckets == null) {
            return;
        }
        if (phaseAge == 0) {
            BlockPos origin = blockPosition();
            int lakeY = columnFloor.get(BlockPos.asLong(origin.getX(), 0, origin.getZ()));
            lakeCenter = new BlockPos(origin.getX(), lakeY == Integer.MAX_VALUE ? origin.getY() : lakeY, origin.getZ());
        }
        double t = Math.min(1.0, (phaseAge + 1) / (double) CARVE_TICKS);
        int frontBucket = (int) Math.ceil(Math.cbrt(t) * BUCKETS);

        RandomSource random = level.random;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int budget = MAX_BLOCKS_PER_TICK;
        while (bucketIndex < Math.min(frontBucket, BUCKETS) && budget > 0) {
            LongArrayList bucket = buckets[bucketIndex];
            while (bucketCursor < bucket.size() && budget > 0) {
                pos.set(bucket.getLong(bucketCursor++));
                if (!level.isLoaded(pos)) {
                    continue;
                }
                if (removeBlock(level, pos)) {
                    budget--;
                }
                onCarved(level, pos, random);
            }
            if (bucketCursor >= bucket.size()) {
                buckets[bucketIndex] = null;
                bucketIndex++;
                bucketCursor = 0;
            }
        }

        // Embers raining out of the expanding crater, and the hiss of rock turning molten.
        if (phaseAge % 6 == 0) {
            playFar(level, SoundEvents.LAVA_EXTINGUISH, 6.0F, 0.5F + random.nextFloat() * 0.3F);
        }
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = random.nextDouble() * RADIUS * t;
            sendFar(level, ParticleTypes.LAVA, getX() + Math.cos(angle) * r, getY() + random.nextDouble() * 6.0,
                    getZ() + Math.sin(angle) * r, 3, 1.0, 0.5, 1.0, 0.0);
        }
    }

    // Returns true if a block was actually removed (so air doesn't eat the per-tick budget).
    private static boolean removeBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isUnbreakable(level, pos, state)) {
            return false;
        }
        // No drops: empty containers first, since their removal hook would otherwise spill items.
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            container.clearContent();
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), CARVE_FLAGS);
        return true;
    }

    private static boolean isUnbreakable(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getDestroySpeed(level, pos) < 0.0F || state.getBlock().getExplosionResistance() >= 3_600_000.0F;
    }

    // ---------------------------------------------------------------------------------------------
    // MOLTEN CRATER (applied during carving)
    // ---------------------------------------------------------------------------------------------

    // Runs right as each crater position is cleared, so the volcano forms with the blast instead of after it:
    //  - edge positions scorch their solid neighbours (the crater walls)
    //  - the lowest position of each column scorches the block under it and may get lava or fire
    private void onCarved(ServerLevel level, BlockPos pos, RandomSource random) {
        long packed = pos.asLong();
        if (wallSamples.contains(packed)) {
            BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                if (random.nextFloat() < 0.6F) {
                    scorch(level, neighbor.setWithOffset(pos, direction), random);
                }
            }
        }

        if (pos.getY() != columnFloor.get(BlockPos.asLong(pos.getX(), 0, pos.getZ()))
                || !level.getBlockState(pos).isAir()) {
            return;
        }
        BlockState floorState = scorch(level, pos.below(), random);
        if (floorState == null) {
            return;
        }
        int dx = pos.getX() - getBlockX();
        int dz = pos.getZ() - getBlockZ();
        boolean inLake = dx * dx + dz * dz <= LAKE_RADIUS * LAKE_RADIUS;
        if (inLake || random.nextFloat() < 0.015F) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        } else if (floorState.is(Blocks.NETHERRACK) && random.nextFloat() < 0.4F) {
            level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // TRANSFORM (burning rim around the crater)
    // ---------------------------------------------------------------------------------------------

    // Crater walls and floor are already done during carving; this just scorches the surroundings.
    private boolean transformTick(ServerLevel level) {
        transformRim(level, level.random);
        return true;
    }

    // Turns a solid, breakable block into volcanic rock. Returns the new state, or null if skipped.
    @Nullable
    private static BlockState scorch(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || isUnbreakable(level, pos, state)) {
            return null;
        }
        float roll = random.nextFloat();
        BlockState scorched = roll < 0.35F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                : roll < 0.60F ? Blocks.BLACKSTONE.defaultBlockState()
                : roll < 0.80F ? Blocks.BASALT.defaultBlockState()
                : Blocks.NETHERRACK.defaultBlockState();
        level.setBlock(pos, scorched, Block.UPDATE_CLIENTS);
        return scorched;
    }

    // R..1.25R around the crater: scorched ground and spreading fires, thinning out with distance.
    private void transformRim(ServerLevel level, RandomSource random) {
        BlockPos origin = blockPosition();
        int outer = (int) (RADIUS * 1.25);
        BlockPos.MutableBlockPos surface = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos top = new BlockPos.MutableBlockPos();
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < RADIUS * 0.85 || distance > outer) {
                    continue;
                }
                double intensity = 1.0 - (distance - RADIUS * 0.85) / (outer - RADIUS * 0.85);
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!level.isLoaded(surface.set(x, origin.getY(), z))) {
                    continue;
                }

                // Ground: scorch soil into netherrack / coarse dirt / blackstone.
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                surface.set(x, groundY, z);
                BlockState ground = level.getBlockState(surface);
                if (random.nextDouble() < intensity * 0.8 && (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT)
                        || ground.is(Blocks.PODZOL) || ground.is(Blocks.MYCELIUM) || ground.is(Blocks.SAND)
                        || ground.is(Blocks.RED_SAND) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.SNOW_BLOCK))) {
                    float roll = random.nextFloat();
                    level.setBlock(surface, roll < 0.5F ? Blocks.NETHERRACK.defaultBlockState()
                            : roll < 0.8F ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }

                // Fire on whatever is highest - ground, or the canopy so trees go up too.
                if (random.nextDouble() < intensity * 0.15) {
                    top.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                    if (level.getBlockState(top).isAir()) {
                        BlockState fire = BaseFireBlock.getState(level, top);
                        if (fire.canSurvive(level, top)) {
                            level.setBlock(top, fire, Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // VOLCANO
    // ---------------------------------------------------------------------------------------------

    private int ticksUntilEruption = 0;

    private void volcanoTick(ServerLevel level) {
        if (lakeCenter == null) {
            lakeCenter = blockPosition();
        }
        RandomSource random = level.random;
        double x = lakeCenter.getX() + 0.5;
        double y = lakeCenter.getY() + 1.0;
        double z = lakeCenter.getZ() + 0.5;
        float waning = 1.0F - (float) phaseAge / VOLCANO_TICKS;

        // Constant simmer.
        sendFar(level, ParticleTypes.LAVA, x, y, z, 2, LAKE_RADIUS * 0.6, 0.5, LAKE_RADIUS * 0.6, 0.0);
        if (phaseAge % 3 == 0) {
            sendFar(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 2.0, z, 1, 3.0, 1.0, 3.0, 0.02);
        }

        if (--ticksUntilEruption > 0) {
            return;
        }
        ticksUntilEruption = 10 + random.nextInt(21);

        // Eruption: a fountain of lava and smoke, a rumble, and a few molten bombs flung out.
        sendFar(level, ParticleTypes.LAVA, x, y + 1.0, z, Math.round(40 * waning) + 10, 2.0, 2.0, 2.0, 0.0);
        sendFar(level, ParticleTypes.LARGE_SMOKE, x, y + 4.0, z, 30, 3.0, 4.0, 3.0, 0.1);
        sendFar(level, ParticleTypes.FLAME, x, y + 2.0, z, 40, 1.5, 3.0, 1.5, 0.15);
        level.playSound(null, x, y, z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 6.0F, 0.5F + random.nextFloat() * 0.3F);
        if (random.nextFloat() < 0.5F) {
            level.playSound(null, x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 4.0F, 0.5F);
        }

        int bombs = random.nextFloat() < waning ? 1 + random.nextInt(3) : 0;
        for (int i = 0; i < bombs; i++) {
            launchVolcanoBomb(level, random, x, y + 2.0, z);
        }
    }

    // A falling magma block flung out of the lake - arcs under gravity and lands as magma.
    private static void launchVolcanoBomb(ServerLevel level, RandomSource random, double x, double y, double z) {
        BlockPos spawn = BlockPos.containing(x, y, z);
        if (!level.getBlockState(spawn).isAir()) {
            return;
        }
        FallingBlockEntity bomb = FallingBlockEntity.fall(level, spawn, Blocks.MAGMA_BLOCK.defaultBlockState());
        double angle = random.nextDouble() * Math.PI * 2.0;
        double speed = 0.6 + random.nextDouble() * 0.9;
        bomb.setDeltaMovement(Math.cos(angle) * speed, 1.2 + random.nextDouble() * 0.6, Math.sin(angle) * speed);
        bomb.dropItem = false;
        bomb.setHurtsEntities(2.0F, 20);
    }

    // ---------------------------------------------------------------------------------------------
    // CLIENT
    // ---------------------------------------------------------------------------------------------

    // Fire burst column with a mushroom cap of smoke, plus the screen shake on the first tick.
    private void clientTick() {
        Level level = level();
        int age = (int) getVisualAge(0.0F);
        if (!clientImpactHandled) {
            clientImpactHandled = true;
            if (age > 5) {
                return;
            }
            PaxiumClientHelper.startBlastShake(position(), RADIUS * 3.0);
            for (int i = 0; i < 6; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION_EMITTER,
                        getX() + (random.nextDouble() - 0.5) * 12.0, getY() + random.nextDouble() * 6.0,
                        getZ() + (random.nextDouble() - 0.5) * 12.0, 0.0, 0.0, 0.0);
            }
        }

        if (age > 100) {
            return;
        }
        float strength = 1.0F - age / 100.0F;

        // The column: fire and lava shooting straight up out of the crater.
        int count = Math.round(40 * strength) + 4;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = random.nextDouble() * 4.0;
            double up = 1.0 + random.nextDouble() * 1.5 * strength;
            ParticleOptions type = random.nextFloat() < 0.75F ? ParticleTypes.FLAME : ParticleTypes.LAVA;
            level.addAlwaysVisibleParticle(type, true, getX() + Math.cos(angle) * r, getY(), getZ() + Math.sin(angle) * r,
                    Math.cos(angle) * 0.08, up, Math.sin(angle) * 0.08);
        }

        // The cap: a ring of smoke rolling outward at the top of the column.
        if (age > 15) {
            double capY = getY() + 30.0 + age * 0.15;
            double capRadius = 4.0 + age * 0.25;
            for (int i = 0; i < 10; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                        getX() + Math.cos(angle) * capRadius, capY + random.nextDouble() * 4.0, getZ() + Math.sin(angle) * capRadius,
                        Math.cos(angle) * 0.15, 0.02, Math.sin(angle) * 0.15);
            }
        }

        if (age < 30) {
            for (int i = 0; i < 8; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true,
                        getX() + (random.nextDouble() - 0.5) * 20.0, getY() + random.nextDouble() * 10.0,
                        getZ() + (random.nextDouble() - 0.5) * 20.0, 0.0, 0.3, 0.0);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // HELPERS / BOILERPLATE
    // ---------------------------------------------------------------------------------------------

    private void playFar(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    // Long-distance particles so the crater effects are visible from far away, not just within 32 blocks.
    private static void sendFar(ServerLevel level, ParticleOptions type, double x, double y, double z,
                                int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, type, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("phase", phase.name());
        tag.putLong("born_at", entityData.get(DATA_BORN_AT));
        tag.putInt("phase_age", phaseAge);
        if (lakeCenter != null) {
            tag.putLong("lake_center", lakeCenter.asLong());
        }
    }

    // The crater geometry isn't saved: a reload mid-carve recomputes it, and already-cleared
    // blocks are simply skipped. Impact never repeats.
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        try {
            phase = Phase.valueOf(tag.getString("phase"));
        } catch (IllegalArgumentException e) {
            phase = Phase.PRECOMPUTE;
        }
        phaseAge = tag.getInt("phase_age");
        entityData.set(DATA_BORN_AT, tag.getLong("born_at"));
        if (tag.contains("lake_center")) {
            lakeCenter = BlockPos.of(tag.getLong("lake_center"));
        }
        if (phase == Phase.IMPACT || phase == Phase.CARVING || phase == Phase.TRANSFORM) {
            phase = Phase.PRECOMPUTE;
            phaseAge = 0;
        }
        clientImpactHandled = true;
    }
}
