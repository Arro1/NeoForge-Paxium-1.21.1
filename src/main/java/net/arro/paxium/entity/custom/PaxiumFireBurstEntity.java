package net.arro.paxium.entity.custom;

import net.arro.paxium.entity.ModEntities;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// A one-shot explosive bolt fired by the Paxium Bow: flies straight (no gravity, no slowdown -
// see getInertia/getLiquidInertia) and explodes for heavy damage on the first thing it hits.
public class PaxiumFireBurstEntity extends AbstractHurtingProjectile {
    private static final float BASE_DIRECT_DAMAGE = 50.0F;
    public static final float BASE_EXPLOSION_POWER = 3.0F;
    // Per Blast level on the bow that fired it: +10 direct damage, +25% blast radius.
    public static final float DAMAGE_PER_LEVEL = 10.0F;
    public static final float POWER_PER_LEVEL = 0.75F;

    private int blastLevel = 0;

    public PaxiumFireBurstEntity(EntityType<? extends PaxiumFireBurstEntity> type, Level level) {
        super(type, level);
        this.accelerationPower = 0.0;
    }

    public PaxiumFireBurstEntity(LivingEntity owner, Level level, double x, double y, double z, int blastLevel) {
        super(ModEntities.PAXIUM_FIRE_BURST.get(), x, y, z, level);
        this.setOwner(owner);
        this.accelerationPower = 0.0;
        this.blastLevel = blastLevel;
    }

    private float directDamage() {
        return BASE_DIRECT_DAMAGE + DAMAGE_PER_LEVEL * blastLevel;
    }

    private float explosionPower() {
        return BASE_EXPLOSION_POWER + POWER_PER_LEVEL * blastLevel;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("blast_level", blastLevel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        blastLevel = tag.getInt("blast_level");
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected float getLiquidInertia() {
        return 1.0F;
    }

    @Nullable
    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMALL_FLAME;
    }

    @Override
    public void tick() {
        super.tick();
        // Extra sword-themed flame flecks layered on top of the base trail particle.
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }

        Entity target = result.getEntity();
        if (target instanceof LivingEntity living) {
            living.hurt(level().damageSources().explosion(this, getOwner()), directDamage());
            living.igniteForSeconds(PaxiumSwordItem.IGNITE_SECONDS * 2.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();
        Vec3 pos = position();

        // MOB interaction: breaks blocks like a creeper/wither-skull blast, but still respects the
        // mobGriefing gamerule rather than always destroying terrain.
        level().explode(this, pos.x, pos.y, pos.z, explosionPower(), true, Level.ExplosionInteraction.MOB);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.0F);
        spawnImpactParticles(serverLevel, pos, explosionPower() / BASE_EXPLOSION_POWER);
        discard();
    }

    // scale: 1.0 unupgraded, growing with the blast radius.
    private static void spawnImpactParticles(ServerLevel level, Vec3 pos, float scale) {
        double spread = 0.4 * scale;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, Math.round(8 * scale), spread, spread, spread, 0.02);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, Math.round(20 * scale), spread, spread, spread, 0.05);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
