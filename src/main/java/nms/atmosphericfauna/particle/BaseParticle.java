package nms.atmosphericfauna.particle;

import nms.atmosphericfauna.AtmosphericFauna;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

public abstract class BaseParticle extends SingleQuadParticle {

    protected BaseParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.hasPhysics = false;
        this.gravity = 0;
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    public static Identifier getId(String path) {
        return Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, path);
    }

    public static TextureAtlasSprite getSprite(String path) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(getId(path));
    }

    public abstract static class FaunaFactory implements ParticleProvider<SimpleParticleType> {
        SpriteSet sprite;

        public FaunaFactory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public abstract Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                double z, double velocityX, double velocityY, double velocityZ);

        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                double z, double velocityX, double velocityY, double velocityZ, RandomSource randomSource) {
            return createParticle(type, level, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
