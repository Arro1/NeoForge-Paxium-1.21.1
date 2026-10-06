package net.arro.paxium.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

/**
 * A flame that lives only a few ticks, so a stream of them stays crisp instead of smearing into
 * a cloud. Fullbright, shrinks while it ages and cools from yellow-white through orange to red.
 */
public class FlightFlameParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = 15728880;

    private final SpriteSet sprites;

    protected FlightFlameParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = 6 + random.nextInt(4);
        this.gravity = 0.0F;
        this.friction = 0.9F;
        this.hasPhysics = false;
        this.quadSize = 0.14F + random.nextFloat() * 0.06F;
        setSpriteFromAge(sprites);
        updateColor();
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        updateColor();
    }

    private void updateColor() {
        float t = (float) age / lifetime;
        this.rCol = 1.0F;
        this.gCol = 0.9F - 0.65F * t;
        this.bCol = Math.max(0.0F, 0.35F - 0.5F * t);
        this.alpha = 1.0F - 0.6F * t * t;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = (age + partialTick) / lifetime;
        return quadSize * (1.0F - 0.65F * Math.min(1.0F, t));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public FlightFlameParticle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                                double x, double y, double z, double xd, double yd, double zd) {
            return new FlightFlameParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
