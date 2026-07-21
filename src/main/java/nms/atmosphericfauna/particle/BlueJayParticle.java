package nms.atmosphericfauna.particle;

import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlueJayParticle extends BaseBirdParticle {

    private static final List<BaseBirdParticle> BLUE_JAYS = Collections.synchronizedList(new ArrayList<>());
    static {
        BaseBirdParticle.SPECIES_REGISTRY.add(BLUE_JAYS);
    }

    // --- CONSTRUCTOR ---

    protected BlueJayParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
            double velocityX, double velocityY, double velocityZ) {
        super(level, x, y, z, getSprite("blue_jay_flying_1"));
        if (this.removed)
            return;
        this.baseSpriteName = "blue_jay";
        this.spriteName = "blue_jay_flying_1";

        this.lifetime = 3800;
        this.quadSize = 0.4f;

        this.flySpeed = 0.15f;
        this.wingFlapSpeed = 3;
        this.wingFlapOffset = random.nextInt(wingFlapSpeed);
        this.steerStrength = 0.0075;
        this.minFlightHeight = 2;
        this.preferredFlightHeight = 5.0 + (this.random.nextDouble() * 13.0 - 3.0);
        if (this.random.nextFloat() < 0.10f) {
            this.preferredFlightHeight += 5.0 + this.random.nextDouble() * 10.0;
        }
        this.heightTolerance = 2.0;
        this.heightAdherence = 0.005;
        this.maxVerticalSpeed = 0.35;
        this.verticalSteerFactor = 1.35;
        this.takeoffClimb = 2.5;
        this.flockRadius = 10.0;
        this.cohesionStrength = 0.001;
        this.alignmentStrength = 0.005;
        this.separationDistance = 3.0;
        this.separationStrength = 0.05;
        this.flockGoalBias = 0.25;
        this.maxFlockSize = 3;
        this.fliesOverOcean = false;

        this.scareRadius = 12.5;
        this.scareTakeoffSpeed = 0.40;

        this.perchingChance = 0.0025;
        this.perchingTime = 800;
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
        return BLUE_JAYS;
    }

    @Override
    public int getSpeciesMaxCount() {
        return maxActiveBlueJays;
    }

    public static int getCount() {
        return BLUE_JAYS.size();
    }

    public static int getMaxActiveBirds() {
        return maxActiveBlueJays;
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
