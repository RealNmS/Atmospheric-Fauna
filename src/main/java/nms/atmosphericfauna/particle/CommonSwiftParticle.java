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

public class CommonSwiftParticle extends BaseBirdParticle {

    private static final List<BaseBirdParticle> COMMON_SWIFTS = Collections.synchronizedList(new ArrayList<>());
    static {
        BaseBirdParticle.SPECIES_REGISTRY.add(COMMON_SWIFTS);
    }

    // --- CONSTRUCTOR ---

    protected CommonSwiftParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
            double velocityX, double velocityY, double velocityZ) {
        super(level, x, y, z, getSprite("common_swift_flying_1"));
        if (this.removed)
            return;
        this.baseSpriteName = "common_swift";
        this.spriteName = "common_swift_flying_1";

        this.lifetime = 4200;
        this.quadSize = 0.35f;

        this.flySpeed = 0.34f + BaseBirdParticle.getNextFlockSpeedOffset();
        this.wingFlapSpeed = 2;
        this.wingFlapOffset = random.nextInt(wingFlapSpeed);
        this.steerStrength = 0.022;
        this.minFlightHeight = 10.0;
        this.preferredFlightHeight = 15.0 + (this.random.nextDouble() * 20.0);
        if (this.random.nextFloat() < 0.15f) {
            this.preferredFlightHeight += 10.0 + this.random.nextDouble() * 15.0;
        }
        this.heightTolerance = 4.0;
        this.heightAdherence = 0.003;
        this.maxVerticalSpeed = 0.65;
        this.verticalSteerFactor = 1.9;
        this.takeoffClimb = 3.5;
        this.flockRadius = 15.0;
        this.cohesionStrength = 0.002;
        this.alignmentStrength = 0.012;
        this.separationDistance = 2.5;
        this.separationStrength = 0.06;
        this.flockGoalBias = 0.40;
        this.maxFlockSize = maxFlockSizeCommonSwift <= 0 ? Integer.MAX_VALUE : maxFlockSizeCommonSwift;
        this.fliesOverOcean = true;
        // Continuous weave for a wavier, more fluid flight path than the other species
        this.weaveAmplitude = 0.045;
        this.weaveFrequency = 0.5;

        this.scareRadius = 15.0;
        this.scareTakeoffSpeed = 0.50;

        this.perchingChance = 0.0002; 
        this.perchingTime = 200; 
        this.perchingDistance = 15;

        this.goalRadius = 60.0;
        this.goalDurationMin = 60;
        this.goalDurationMax = 120;
        this.lookAheadMultiplier = 6.0;

        this.xd = velocityX + (this.random.nextFloat() - 0.5f) * 0.1;
        this.zd = velocityZ + (this.random.nextFloat() - 0.5f) * 0.1;
        this.yd = velocityY + 0.05;
    }

    // --- HELPER METHODS ---

    @Override
    protected List<BaseBirdParticle> getSpeciesList() {
        return COMMON_SWIFTS;
    }

    @Override
    public int getSpeciesMaxCount() {
        return maxActiveCommonSwifts;
    }

    public static int getCount() {
        return COMMON_SWIFTS.size();
    }

    public static int getMaxActiveBirds() {
        return maxActiveCommonSwifts;
    }

    // --- FACTORY ---

    public static final class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new CommonSwiftParticle(level, x, y, z, this.sprite, velocityX, velocityY, velocityZ);
        }
    }
}
