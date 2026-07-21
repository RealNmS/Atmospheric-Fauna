package nms.atmosphericfauna.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class CrowParticle extends BaseBirdParticle {

    // --- CONFIG STUFF ---

    public static int maxActiveCrows = 50;

    private static final List<BaseBirdParticle> CROWS = Collections.synchronizedList(new ArrayList<>());
    static {
        BaseBirdParticle.SPECIES_REGISTRY.add(CROWS);
    }

    // --- CONSTRUCTOR ---

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
            double velocityX, double velocityY, double velocityZ) {
        super(level, x, y, z, getSprite("crow_flying_1"));
        if (this.removed)
            return;
        this.baseSpriteName = "crow";
        this.spriteName = "crow_flying_1";

        this.lifetime = 3200;
        this.quadSize = 0.5f;

        this.flySpeed = 0.20f;
        this.wingFlapSpeed = 4;
        this.wingFlapOffset = random.nextInt(wingFlapSpeed);
        this.steerStrength = 0.0075;
        this.minFlightHeight = 2.5;
        this.preferredFlightHeight = 25.0 + (this.random.nextDouble() * 25.0 - 5.0);
        if (this.random.nextFloat() < 0.15f) {
            this.preferredFlightHeight += 25.0 + this.random.nextDouble() * 25.0;
        }
        this.heightTolerance = 8.0;
        this.heightAdherence = 0.0025;
        this.maxVerticalSpeed = 0.30;
        this.verticalSteerFactor = 1.25;
        this.takeoffClimb = 2.5;
        this.flockRadius = 12.0;
        this.cohesionStrength = 0.002;
        this.alignmentStrength = 0.01;
        this.separationDistance = 2.5;
        this.separationStrength = 0.05;
        this.flockGoalBias = 0.25;
        this.maxFlockSize = Integer.MAX_VALUE;
        this.fliesOverOcean = true;

        this.scareRadius = 10.0;
        this.scareTakeoffSpeed = 0.35;

        this.perchingChance = 0.005;
        this.perchingTime = 600;
        this.perchingDistance = 10;

        this.goalRadius = 50.0;
        this.goalDurationMin = 80;
        this.goalDurationMax = 160;
        this.lookAheadMultiplier = 5.0;

        this.xd = velocityX + (this.random.nextFloat() - 0.5f) * 0.1;
        this.zd = velocityZ + (this.random.nextFloat() - 0.5f) * 0.1;
        this.yd = velocityY + 0.05;
    }

    // --- HELPER METHODS ---

    @Override
    protected List<BaseBirdParticle> getSpeciesList() {
        return CROWS;
    }

    @Override
    public int getSpeciesMaxCount() {
        return maxActiveCrows;
    }

    public static int getCount() {
        return CROWS.size();
    }

    public static int getMaxActiveBirds() {
        return maxActiveCrows;
    }

    // --- FACTORY ---

    public static final class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new CrowParticle(level, x, y, z, this.sprite, velocityX, velocityY, velocityZ);
        }
    }
}
