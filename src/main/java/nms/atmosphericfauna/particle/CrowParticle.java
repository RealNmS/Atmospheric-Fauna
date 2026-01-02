package nms.atmosphericfauna.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

public class CrowParticle extends FaunaParticle {

    // Config for movement
    private final double initialY;

    protected CrowParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);

        // MOJANG MAPPING: 'lifetime' instead of 'maxAge'
        this.lifetime = 200; // Lives for 10 seconds

        // MOJANG MAPPING: 'gravity' instead of 'gravityStrength'
        this.gravity = 0;

        // MOJANG MAPPING: 'quadSize' instead of 'scale'
        this.quadSize = 0.5f;

        this.initialY = y;

        // Set some initial random velocity so it doesn't just sit there
        this.xd = (Math.random() - 0.5) * 0.1;
        this.zd = (Math.random() - 0.5) * 0.1;
    }

    @Override
    public void tick() {
        // 1. Standard Tick Updates (store previous position)
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove(); // MOJANG MAPPING: 'remove()' instead of 'markDead()'
            return;
        }

        // 2. AI / Movement Logic (Simple Circling)
        // Add a slight curve to the velocity to make it circle
        this.xd += (Math.random() - 0.5) * 0.01;
        this.zd += (Math.random() - 0.5) * 0.01;

        // Keep it near the spawn height (bobbing up and down)
        double targetY = initialY + Math.sin(this.age * 0.1) * 0.5;
        this.yd += (targetY - this.y) * 0.01;

        // 3. Move
        this.move(this.xd, this.yd, this.zd);
    }

    public static class Factory extends FaunaParticle.Factory {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz) {
            // Because your FaunaParticle constructor demands a sprite immediately, we must
            // pick one now.
            TextureAtlasSprite sprite = this.spriteSet.get(level.random);

            return new CrowParticle(level, x, y, z, sprite);
        }

        @Override
        public FaunaParticle createFauna(ClientLevel level, double x, double y, double z, double vx, double vy,
                double vz) {
            // This is required by your abstract parent, even if we used createParticle
            // above.
            return new CrowParticle(level, x, y, z, this.spriteSet.get(level.random));
        }
    }
}