package nms.atmosphericfauna.particle;

import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NorthernCardinalParticle extends BaseBirdParticle {

    private static final List<BaseBirdParticle> NORTHERN_CARDINALS = Collections.synchronizedList(new ArrayList<>());
    static {
        BaseBirdParticle.SPECIES_REGISTRY.add(NORTHERN_CARDINALS);
    }

    // --- CONSTRUCTOR ---

    protected NorthernCardinalParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
            double velocityX, double velocityY, double velocityZ) {
        super(level, x, y, z, getSprite("northern_cardinal_flying_1"));
        if (this.removed)
            return;
        this.baseSpriteName = "northern_cardinal";
        this.formattedName = "Northern Cardinal";
        this.spriteName = "northern_cardinal_flying_1";

        this.lifetime = 3500;
        this.quadSize = 0.35f;

        this.flySpeed = 0.12f + BaseBirdParticle.getNextFlockSpeedOffset(); 
        this.wingFlapSpeed = 2;
        this.wingFlapOffset = random.nextInt(wingFlapSpeed);
        this.steerStrength = 0.009;
        this.minFlightHeight = 1.0;
        this.preferredFlightHeight = 2.0 + (this.random.nextDouble() * 6.0);
        if (this.random.nextFloat() < 0.05f) {
            this.preferredFlightHeight += 3.0 + this.random.nextDouble() * 5.0; 
        }
        this.heightTolerance = 1.5;
        this.heightAdherence = 0.008;
        this.maxVerticalSpeed = 0.25;
        this.verticalSteerFactor = 1.1;
        this.takeoffClimb = 1.8;
        this.flockRadius = 8.0; 
        this.cohesionStrength = 0.003;
        this.alignmentStrength = 0.002;
        this.separationDistance = 1.5;
        this.separationStrength = 0.04;
        this.flockGoalBias = 0.35; 
        this.maxFlockSize = maxFlockSizeNorthernCardinal <= 0 ? Integer.MAX_VALUE : maxFlockSizeNorthernCardinal;
        this.fliesOverOcean = false;

        this.scareRadius = 10.0;
        this.scareTakeoffSpeed = 0.35;

        this.perchingChance = 0.008;
        this.perchingTime = 1200;
        this.perchingDistance = 8; 

        this.goalRadius = 30.0;
        this.goalDurationMin = 50;
        this.goalDurationMax = 110; 
        this.lookAheadMultiplier = 4.0; 

        this.xd = velocityX + (this.random.nextFloat() - 0.5f) * 0.1;
        this.zd = velocityZ + (this.random.nextFloat() - 0.5f) * 0.1;
        this.yd = velocityY + 0.05;
    }

    // --- HELPER METHODS ---

    @Override
    protected List<BaseBirdParticle> getSpeciesList() {
        return NORTHERN_CARDINALS;
    }

    @Override
    public int getSpeciesMaxCount() {
        return maxActiveNorthernCardinals;
    }

    public static int getCount() {
        return NORTHERN_CARDINALS.size();
    }

    public static int getMaxActiveBirds() {
        return maxActiveNorthernCardinals;
    }

    // --- FACTORY ---

    public static final class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new NorthernCardinalParticle(level, x, y, z, this.sprite, velocityX, velocityY, velocityZ);
        }
    }
}
