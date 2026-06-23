package nms.atmosphericfauna.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class BlueJayParticle extends BaseBirdParticle {

    // --- CONFIG STUFF ---

    public static int maxActiveBlueJays = 10;

    // --- CONSTRUCTOR ---

    protected BlueJayParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
            double velocityX, double velocityY, double velocityZ) {
        super(level, x, y, z, getSprite("blue_jay_flying_1"));
        if (this.removed)
            return;
        this.baseSpriteName = "blue_jay";
        this.spriteName = "blue_jay_flying_1";

        this.lifetime = 3200;
        this.quadSize = 0.5f;

        this.flySpeed = 0.25f;
        this.steerStrength = 0.01;
        this.minFlightHeight = 2; // how many blocks above ground
        this.maxFlightHeight = 85.0; // how many blocks from void
        this.maxVerticalSpeed = 0.30;
        this.verticalSteerFactor = 1.30;
        this.takeoffClimb = 2.5;
        this.flockRadius = 10.0;
        this.cohesionStrength = 0.001;
        this.alignmentStrength = 0.005;
        this.separationDistance = 3.0;
        this.separationStrength = 0.05;
        this.flockGoalBias = 0.25;

        this.scareRadius = 12.5; // horizontal distance that startles perched crows
        this.scareTakeoffSpeed = 0.40; // horizontal speed applied when scared

        this.perchingChance = 0.0025;
        this.perchingTime = 800; // base time spent perched
        this.perchingDistance = 10; // how many blocks down to scan for landing spots

        this.goalRadius = 50.0;
        this.goalDurationMin = 80;
        this.goalDurationMax = 160;
        this.lookAheadMultiplier = 5.0;

        this.xd = velocityX + (this.random.nextFloat() - 0.5f) * 0.1;
        this.zd = velocityZ + (this.random.nextFloat() - 0.5f) * 0.1;
        this.yd = velocityY + 0.05;
    }

    // --- HELPER METHODS ---

    public static int getCount() {
        int count = 0;
        for (BaseBirdParticle bird : getAllBirds()) {
            if (bird instanceof BlueJayParticle) {
                count++;
            }
        }
        return count;
    }

    // --- FACTORY ---

    public static final class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new BlueJayParticle(level, x, y, z, this.sprite, velocityX, velocityY, velocityZ);
        }
    }
}
