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

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, Util.getSprite("crow_fly"));

        this.lifetime = 2400;
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
        if (this.age % 20 == 0) {
            System.out.println("Crow #" + this.hashCode() + " | State: " + this.state + " | Height: "
                    + String.format("%.2f", this.y));
        }

        this.move(this.xd, this.yd, this.zd);
    }

    // --- BEHAVIORS ---

    private void tickFlying() {
        this.hasPhysics = false;

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

        // Check EVERY tick with a small random chance (1%)
        if (Math.random() < 0.01) {
            // Scan 10 blocks down to find a spot.
            // If we see solid ground, start landing!
            for (int i = 1; i <= 10; i++) {
                BlockPos below = BlockPos.containing(x, y - i, z);
                if (!level.getBlockState(below).isAir()) {
                    this.state = State.LANDING;
                    break;
                }
            }
        }
    }

    private void tickLanding() {
        this.hasPhysics = true;

        // Glide down
        this.yd = -0.1;
        this.xd *= 0.95;
        this.zd *= 0.95;

        if (this.onGround) {
            this.state = State.PERCHED;
            this.perchTimer = 100 + (int) (Math.random() * 100);
        }
    }

    private void tickPerched() {
        this.xd = 0;
        this.zd = 0;
        this.yd = 0;

        if (perchTimer-- <= 0) {
            this.state = State.TAKING_OFF;
        }
    }

    private void tickTakingOff() {
        this.hasPhysics = false;
        this.yd = 0.2; // Jump up

        // Once high enough, switch back to flying
        if (this.age % 10 == 0) {
            this.state = State.FLYING;
        }
    }

    private void tickDying() {
        this.hasPhysics = false;
        this.yd -= 0.02; // Accelerate down

        // Remove if we hit the void or have fallen far enough
        if (this.y < -64 || (this.onGround && this.yd < -0.5)) {
            this.remove();
        }

        // Hard limit to prevent memory leaks if it falls forever
        if (this.age > this.lifetime + 200) {
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