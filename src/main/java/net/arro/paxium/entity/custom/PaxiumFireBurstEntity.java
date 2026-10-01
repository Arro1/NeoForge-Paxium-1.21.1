package net.arro.paxium.entity.custom;

import net.arro.paxium.entity.ModEntities;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.minecraft.core.particles.ParticleOptions;
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
    private static final float DIRECT_DAMAGE = 50.0F;
    private static final float EXPLOSION_POWER = 3.0F;

    public PaxiumFireBurstEntity(EntityType<? extends PaxiumFireBurstEntity> type, Level level) {
        super(type, level);
        this.accelerationPower = 0.0;
    }

    public PaxiumFireBurstEntity(LivingEntity owner, Level level, double x, double y, double z) {
        super(ModEntities.PAXIUM_FIRE_BURST.get(), x, y, z, level);
        this.setOwner(owner);
        this.accelerationPower = 0.0;
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
            living.hurt(level().damageSources().explosion(this, getOwner()), DIRECT_DAMAGE);
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
        level().explode(this, pos.x, pos.y, pos.z, EXPLOSION_POWER, true, Level.ExplosionInteraction.MOB);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.0F);
        spawnImpactParticles(serverLevel, pos);
        discard();
    }

    private static void spawnImpactParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 8, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 20, 0.35, 0.35, 0.35, 0.05);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
