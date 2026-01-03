package nms.atmosphericfauna.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import nms.atmosphericfauna.Util;

public class CrowParticle extends FaunaParticle {

    private enum State {
        FLYING,
        LANDING,
        PERCHED,
        TAKING_OFF,
        DYING
    }

    private State state = State.FLYING;
    private int perchTimer = 0;
    private final float flySpeed = 0.15f;

    // Landing target: y-coordinate to land on (top of the block) and the block pos
    private Double landingTargetY = Double.NaN;
    private BlockPos landingBlockPos = null;

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, Util.getSprite("crow_fly"));

        this.lifetime = 1200;
        this.gravity = 0;
        this.quadSize = 0.5f;
        this.hasPhysics = false;

        this.xd = (Math.random() - 0.5) * flySpeed;
        this.zd = (Math.random() - 0.5) * flySpeed;
        this.yd = 0.05;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.state = State.DYING;
        }

        switch (state) {
            case FLYING -> tickFlying();
            case LANDING -> tickLanding();
            case PERCHED -> tickPerched();
            case TAKING_OFF -> tickTakingOff();
            case DYING -> tickDying();
        }

        // ONLY FOR TESTING: Prints status to console every 1 second (20 ticks)
        if (this.age % 10 == 0) {
            System.out.println("Crow #" + this.hashCode() + " | State: " + this.state + " | Height: "
                    + String.format("%.2f", this.y));
            System.out.println("   Crow #" + this.hashCode() + " | xd: " + String.format("%.3f", this.xd) + " | yd: "
                    + String.format("%.3f", this.yd) + " | zd: " + String.format("%.3f", this.zd));
        }

        this.move(this.xd, this.yd, this.zd);
    }

    // --- BEHAVIORS ---

    private void tickFlying() {
        // Steer randomly
        this.xd += (Math.random() - 0.5) * 0.02;
        this.zd += (Math.random() - 0.5) * 0.02;
        this.yd += (Math.random() - 0.5) * 0.01;

        // Clamp speed
        double speed = Math.sqrt(xd * xd + yd * yd + zd * zd);
        if (speed > flySpeed) {
            this.xd = (this.xd / speed) * flySpeed;
            this.yd = (this.yd / speed) * flySpeed;
            this.zd = (this.zd / speed) * flySpeed;
        }

        // Check EVERY tick with a small random chance (5%) and look up to 10 blocks
        // below
        if (Math.random() < 0.05) {
            for (int i = 1; i <= 10; i++) {
                BlockPos below = BlockPos.containing(x, y - i, z);
                // Only consider blocks that have an air space just above them and a collision
                // shape
                if (!level.getBlockState(below).isAir()
                        && level.getBlockState(below.above()).isAir()
                        && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                    // Set a landing target (the top surface of the found block) so we don't perch
                    // mid-air
                    this.state = State.LANDING;
                    this.landingBlockPos = below;
                    this.landingTargetY = below.getY() + 1.0;
                    break;
                }
            }
        }
    }

    private void tickLanding() {
        // Glide down
        this.yd = -0.1;
        this.xd *= 0.95;
        this.zd *= 0.95;

        if (!Double.isNaN(this.landingTargetY)) {
            // If we've reached (or slightly passed) the target landing Y, attempt to perch
            if (this.y <= this.landingTargetY + 0.2) {
                // verify the landing block still exists and is not air
                if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos).isAir()) {
                    // Snap to the top of the block and stop movement
                    this.setPos(this.x, this.landingTargetY, this.z);
                    this.xd = 0;
                    this.zd = 0;
                    this.yd = 0;
                    this.state = State.PERCHED;
                    this.perchTimer = 100 + (int) (Math.random() * 100);
                } else {
                    // Landing block disappeared — abort landing
                    this.state = State.FLYING;
                }
                this.landingTargetY = Double.NaN;
                this.landingBlockPos = null;
            }
        } else {
            // No valid target — resume flying
            this.state = State.FLYING;
        }
    }

    private void tickPerched() {
        this.xd = 0;
        this.zd = 0;
        this.yd = 0;

        if (perchTimer-- <= 0) {
            this.state = State.TAKING_OFF;
            this.perchTimer = 80;
        }
    }

    private void tickTakingOff() {
        // Clear any previous landing target
        this.landingTargetY = Double.NaN;
        this.landingBlockPos = null;

        // Break collision with perch
        this.setPos(this.x, this.y + 0.05, this.z);

        // Consistent upward speed
        this.yd = 0.05;
        this.xd += (Math.random() - 0.5) * 0.05;
        this.zd += (Math.random() - 0.5) * 0.05;

        // Wait for the timer to finish before switching to normal flight
        if (perchTimer-- <= 0) {
            this.state = State.FLYING;
        }
    }

    private void tickDying() {
        this.yd -= 0.02; // Accelerate down

        // Remove if we hit the void or have fallen far enough
        if (this.y < -64) {
            System.out
                    .println("Crow #" + this.hashCode() + " has died at age " + this.age + " ticks.");
            this.remove();
        }

        // Hard limit to prevent memory leaks if it falls forever
        if (this.age > this.lifetime + 200) {
            System.out
                    .println("Crow #" + this.hashCode() + " forcibly removed after exceeding death time limit.");
            this.remove();
        }
    }

    // --- FACTORY ---

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