package nms.atmosphericfauna.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import nms.atmosphericfauna.Util;

public class CrowParticle extends FaunaParticle {

    private final double initialY;

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, Util.getSprite("crow_fly"));

        this.lifetime = 200;
        this.gravity = 0;
        this.quadSize = 0.5f;
        this.initialY = y;

        // MOVEMENT: Random velocity to start
        this.xd = (Math.random() - 0.5) * 0.1;
        this.zd = (Math.random() - 0.5) * 0.1;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Logic: Circle and bob up/down
        this.xd += (Math.random() - 0.5) * 0.01;
        this.zd += (Math.random() - 0.5) * 0.01;
        double targetY = initialY + Math.sin(this.age * 0.1) * 0.5;
        this.yd += (targetY - this.y) * 0.01;

        this.move(this.xd, this.yd, this.zd);
    }

    public static class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new CrowParticle(level, x, y, z, this.sprite);
        }
    }
}