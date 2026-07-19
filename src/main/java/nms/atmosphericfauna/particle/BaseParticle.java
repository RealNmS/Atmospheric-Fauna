package nms.atmosphericfauna.particle;

import nms.atmosphericfauna.AtmosphericFauna;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

//? if <=1.21.8 {
//  import net.minecraft.client.particle.TextureSheetParticle;
//  import net.minecraft.client.particle.ParticleRenderType;
//  import net.minecraft.client.renderer.texture.TextureAtlas;
//  import net.minecraft.client.renderer.texture.AbstractTexture;
//  import net.minecraft.resources.ResourceLocation;
//?}
//? if >1.21.8 {
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
//?}

//? if <=1.21.8 {
//  public abstract class BaseParticle extends TextureSheetParticle
//?}
//? if >1.21.8 {
public abstract class BaseParticle extends SingleQuadParticle
//?}
{
    protected BaseParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
//? if <=1.21.8 {
//      super(level, x, y, z);
//      this.setSprite(sprite);
//?}
//? if >1.21.8 {
        super(level, x, y, z, sprite);
//?}
        this.hasPhysics = false;
        this.gravity = 0;
    }

//? if <=1.21.8 {
//  @Override
//  public ParticleRenderType getRenderType() {
//      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
//  }
//?}
//? if >1.21.8 {
    @Override
    protected @NonNull Layer getLayer() {
        return Layer.OPAQUE;
    }
//?}

//? if <=1.21.8 {
//  public static ResourceLocation getId(@NonNull String path) {
//      return ResourceLocation.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, path);
//  }
//?}
//? if >1.21.8 {
    public static Identifier getId(@NonNull String path) {
        return Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, path);
    }
//?}

    public static TextureAtlasSprite getSprite(@NonNull String path) {
//? if <=1.21.8 {
//      AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
//      if (texture instanceof TextureAtlas atlas) {
//          return atlas.getSprite(getId(path));
//      }
//      throw new IllegalStateException("Particle atlas not found!");
//?}
//? if >1.21.8 {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(getId(path));
//?}
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
