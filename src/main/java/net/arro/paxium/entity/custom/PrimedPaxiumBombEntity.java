package net.arro.paxium.entity.custom;

import net.arro.paxium.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// The armed Paxium Bomb: rises out of where the block stood, hovers and "charges" for FUSE_TICKS
// with rising chimes, a quickening heartbeat and silent golden lightning, then hands over to a
// PaxiumBlastEntity. Movement and sounds are server-driven; visuals are client-side.
public class PrimedPaxiumBombEntity extends Entity {
    public static final int FUSE_TICKS = 360;
    private static final int RISE_TICKS = 60;
    private static final double RISE_HEIGHT = 2.5;
    // The last second before detonation is silent - the calm before the blast.
    private static final int SILENCE_TICKS = 20;

    // Entity event id for a visual-only lightning strike (mirrored to clients like the Starforge's).
    private static final byte STRIKE_EVENT = 70;
    private static final int STRIKE_DURATION = 6;
    private static final int MAX_BOLT_HEIGHT = 80;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(PrimedPaxiumBombEntity.class, EntityDataSerializers.INT);

    private double baseY;
    private int ticksUntilStrike = 30;
    @Nullable
    private LivingEntity owner;

    // Client-only lightning state.
    private int strikeTicksLeft = 0;
    private long boltSeed = 0L;

    public PrimedPaxiumBombEntity(EntityType<? extends PrimedPaxiumBombEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public PrimedPaxiumBombEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(ModEntities.PRIMED_PAXIUM_BOMB.get(), level);
        this.setPos(x, y, z);
        this.baseY = y;
        this.owner = owner;
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, FUSE_TICKS);
    }

    public int getFuse() {
        return entityData.get(DATA_FUSE);
    }

    // 0 at arming, 1 at detonation.
    public float getProgress(float partialTick) {
        return Mth.clamp((FUSE_TICKS - getFuse() + partialTick) / FUSE_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();

        int fuse = getFuse();
        int age = FUSE_TICKS - fuse;

        // Clients just interpolate the synced position; baseY only exists on the server.
        if (level().isClientSide) {
            clientTick(age);
            return;
        }

        // Ease up out of the block, then hover with a gentle bob.
        double rise = RISE_HEIGHT * easeOutCubic(Math.min(1.0, age / (double) RISE_TICKS));
        double bob = age > RISE_TICKS ? Math.sin((age - RISE_TICKS) * 0.12) * 0.08 : 0.0;
        setPos(getX(), baseY + rise + bob, getZ());

        if (fuse <= 0) {
            detonate();
            return;
        }
        entityData.set(DATA_FUSE, fuse - 1);
        serverEffects(fuse, age);
    }

    private void serverEffects(int fuse, int age) {
        if (fuse <= SILENCE_TICKS) {
            return;
        }
        RandomSource random = level().random;
        float progress = (float) age / FUSE_TICKS;

        // Chimes rising in pitch; they quicken over the last 5 seconds.
        int chimeInterval = fuse > 100 ? 20 : Math.max(5, fuse / 5);
        if (age % chimeInterval == 0) {
            playCharge(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.5F + 1.5F * progress);
        }

        // Heartbeat for the last 10 seconds, speeding up.
        if (fuse <= 200) {
            int beatInterval = Mth.clamp(fuse / 7, 8, 30);
            if (fuse % beatInterval == 0) {
                playCharge(SoundEvents.WARDEN_HEARTBEAT, 3.0F, 0.6F + 0.4F * progress);
            }
        }

        if (age % 80 == 40) {
            playCharge(SoundEvents.BEACON_AMBIENT, 2.0F, 0.7F + 0.6F * progress);
        }

        // Lightning strikes, more frequent as the charge builds.
        if (--ticksUntilStrike <= 0) {
            int min = Math.round(Mth.lerp(progress, 20, 6));
            int max = Math.round(Mth.lerp(progress, 40, 10));
            ticksUntilStrike = min + random.nextInt(max - min + 1);
            level().broadcastEntityEvent(this, STRIKE_EVENT);
            playCharge(SoundEvents.ANVIL_LAND, 0.6F, 0.5F + random.nextFloat() * 0.2F);
        }
    }

    private void detonate() {
        PaxiumBlastEntity blast = new PaxiumBlastEntity(level(), getX(), getY() + 0.5, getZ(), owner);
        level().addFreshEntity(blast);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == STRIKE_EVENT) {
            strikeTicksLeft = STRIKE_DURATION;
            boltSeed = random.nextLong();
            level().addParticle(ParticleTypes.FLASH, getX(), getY() + 0.5, getZ(), 0.0, 0.0, 0.0);
            for (int i = 0; i < 10; i++) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(),
                        (random.nextDouble() - 0.5) * 0.6, random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.6);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    // Glyphs pulled into the bomb, sparks and wisps - all thickening as the charge builds.
    private void clientTick(int age) {
        if (strikeTicksLeft > 0) {
            strikeTicksLeft--;
            boltSeed = random.nextLong();
        }

        float progress = (float) age / FUSE_TICKS;
        double cx = getX();
        double cy = getY() + 0.5;
        double cz = getZ();

        int glyphs = 1 + Math.round(progress * 5);
        for (int i = 0; i < glyphs; i++) {
            level().addParticle(ParticleTypes.ENCHANT, cx, cy, cz,
                    (random.nextDouble() - 0.5) * 5.0, (random.nextDouble() - 0.3) * 3.0, (random.nextDouble() - 0.5) * 5.0);
        }
        if (random.nextFloat() < 0.2F + progress * 0.6F) {
            level().addParticle(ParticleTypes.END_ROD, cx + (random.nextDouble() - 0.5), cy + (random.nextDouble() - 0.5),
                    cz + (random.nextDouble() - 0.5), 0.0, 0.03, 0.0);
        }
        if (random.nextFloat() < progress) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL, cx, cy, cz,
                    (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
        }
    }

    public boolean isStriking() {
        return strikeTicksLeft > 0;
    }

    public long getBoltSeed() {
        return boltSeed;
    }

    public int getBoltHeight() {
        return Math.max(1, Math.min(MAX_BOLT_HEIGHT, level().getMaxBuildHeight() - (int) getY() - 1));
    }

    private void playCharge(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    private static double easeOutCubic(double t) {
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
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
        tag.putInt("fuse", getFuse());
        tag.putDouble("base_y", baseY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_FUSE, tag.getInt("fuse"));
        baseY = tag.getDouble("base_y");
    }
}
