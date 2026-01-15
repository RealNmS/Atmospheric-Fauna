package nms.atmosphericfauna.particle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class CrowParticle extends BaseBirdParticle {

    private static final List<CrowParticle> ALL_CROWS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    private boolean counted = false; // whether this instance is counted toward ACTIVE_COUNT

    // --- CONFIG STUFF ---

    public static int maxActiveCrows = 50;

    // --- CONSTRUCTOR ---

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, getSprite("crow_flying_1"));
        this.baseSpriteName = "crow";
        this.spriteName = "crow_flying_1";

        this.lifetime = 2400;
        this.quadSize = 0.5f;

        this.flySpeed = 0.20f;
        this.steerStrength = 0.0075;
        this.minFlightHeight = 2.5; // how many blocks above ground
        this.maxFlightHeight = 85.0; // how many blocks from void
        this.maxVerticalSpeed = 0.30;
        this.verticalSteerFactor = 1.25;
        this.takeoffClimb = 2.5;
        this.flockRadius = 12.0;
        this.cohesionStrength = 0.02;
        this.alignmentStrength = 0.015;
        this.separationDistance = 1.5;
        this.separationStrength = 0.06;
        this.flockGoalBias = 0.40;

        this.scareRadius = 10.0; // horizontal distance that startles perched crows
        this.scareTakeoffSpeed = 0.35; // horizontal speed applied when scared

        this.perchingChance = 0.005;
        this.perchingTime = 600; // base time spent perched
        this.perchingDistance = 10; // how many blocks down to scan for landing spots

        this.goalRadius = 50.0;
        this.goalDurationMin = 80;
        this.goalDurationMax = 160;
        this.lookAheadMultiplier = 5.0;

        this.xd = (Math.random() - 0.5) * flySpeed;
        this.zd = (Math.random() - 0.5) * flySpeed;
        this.yd = 0.05;

        ALL_CROWS.add(this);
        if (ACTIVE_COUNT.get() >= maxActiveCrows) {
            this.remove();
            return;
        }
        ACTIVE_COUNT.incrementAndGet();
        this.counted = true;
    }

    // --- HELPER METHODS ---

    public static int getCount() {
        return ACTIVE_COUNT.get();
    }

    public static void reset() {
        ALL_CROWS.clear();
        ACTIVE_COUNT.set(0);
    }

    @Override
    public void remove() {
        ALL_CROWS.remove(this);
        if (this.counted) {
            int val = ACTIVE_COUNT.decrementAndGet();
            if (val < 0)
                ACTIVE_COUNT.set(0);
            this.counted = false;
        }
        super.remove();
    }

    // --- FACTORY ---

    public static class Factory extends FaunaFactory {
        public Factory(SpriteSet spriteSet) {
            super(spriteSet);
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            if (ACTIVE_COUNT.get() >= maxActiveCrows)
                return null;
            return new CrowParticle(level, x, y, z, this.sprite);
        }
    }
}
