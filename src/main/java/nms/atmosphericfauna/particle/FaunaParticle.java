package nms.atmosphericfauna.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import org.jetbrains.annotations.Nullable;

public abstract class FaunaParticle extends SingleQuadParticle {

    protected FaunaParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.hasPhysics = false;
    }

    @SuppressWarnings("null")
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    public abstract static class Factory implements ParticleProvider<SimpleParticleType> {
        @SuppressWarnings("unused")
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public abstract Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                double z, double velocityX, double velocityY, double velocityZ);

        @SuppressWarnings("null")
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                double z, double velocityX, double velocityY, double velocityZ, RandomSource randomSource) {
            return createParticle(type, level, x, y, z, velocityX, velocityY, velocityZ);
        }

        // Child classes (like CrowParticle) will implement this
        public abstract FaunaParticle createFauna(ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ);
    }
}