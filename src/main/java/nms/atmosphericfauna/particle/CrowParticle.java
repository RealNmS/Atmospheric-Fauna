package nms.atmosphericfauna.particle;

import nms.atmosphericfauna.Util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
    private double goalX = Double.NaN;
    private double goalY = Double.NaN;
    private double goalZ = Double.NaN;
    private int goalTimer = 0;
    private int landingCooldown = 0;
    private Double landingTargetY = Double.NaN;
    private BlockPos landingBlockPos = null;
    private BlockPos perchBlockPos = null; // stores actual perch while perched

    // FLOCKING: global registry of active crow particles (thread-safe iteration)
    private static final List<CrowParticle> ALL_CROWS = new CopyOnWriteArrayList<>();

    // Active count cap
    private static final int MAX_ACTIVE_CROWS = 120; // configurable max active crows
    private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    private boolean counted = false; // whether this instance is counted toward ACTIVE_COUNT

    // --- CONSTANTS ---

    private final float flySpeed = 0.20f;
    private final double steerStrength = 0.0075;
    private final double minFlightHeight = 2.5; // how many blocks above ground
    private final double maxFlightHeight = 85.0; // how many blocks from void
    private final double maxVerticalSpeed = 0.30;
    private final double verticalSteerFactor = 1.25;
    private final double takeoffClimb = 2.5;

    private final double flockRadius = 12.0;
    private final double cohesionStrength = 0.02;
    private final double alignmentStrength = 0.015;
    private final double separationDistance = 1.5;
    private final double separationStrength = 0.06;
    private final double flockGoalBias = 0.40;

    private final double scareRadius = 10.0; // horizontal distance that startles perched crows
    private final double scareTakeoffSpeed = 0.35; // horizontal speed applied when scared

    private final double perchingChance = 0.005;
    private final int perchingTime = 600; // base time spent perched
    private final int perchingDistance = 10; // how many blocks down to scan for landing spots

    private final double goalRadius = 50.0;
    private final int goalDurationMin = 80;
    private final int goalDurationMax = 160;
    private final double lookAheadMultiplier = 5.0;

    // --- CONSTRUCTOR & TICK ---

    protected CrowParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, Util.getSprite("crow_fly_1"));

        this.lifetime = 2400;
        this.gravity = 0;
        this.quadSize = 0.5f;
        this.hasPhysics = false;

        this.xd = (Math.random() - 0.5) * flySpeed;
        this.zd = (Math.random() - 0.5) * flySpeed;
        this.yd = 0.05;
        ALL_CROWS.add(this);

        // Respect active cap: if we're already at maximum, remove this instance
        // immediately
        if (ACTIVE_COUNT.get() >= MAX_ACTIVE_CROWS) {
            // not counted, just remove and don't increment the active counter
            this.remove();
            return;
        }
        ACTIVE_COUNT.incrementAndGet();
        this.counted = true;
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

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (landingCooldown > 0)
            landingCooldown--;

        if (this.age++ >= this.lifetime) {
            if (this.state != State.DYING) {
                this.state = State.DYING;
                if (this.counted) {
                    int val = ACTIVE_COUNT.decrementAndGet();
                    if (val < 0)
                        ACTIVE_COUNT.set(0);
                    this.counted = false;
                }
            }
        }

        switch (state) {
            case FLYING -> tickFlying();
            case LANDING -> tickLanding();
            case PERCHED -> tickPerched();
            case TAKING_OFF -> tickTakingOff();
            case DYING -> tickDying();
        }

        if (state != State.DYING && state != State.PERCHED) {
            if (this.age % 5 == 0) {
                // flap wings
                if (this.sprite.contents().name().getPath().startsWith("crow_fly_")) {
                    int frame = Integer
                            .parseInt(this.sprite.contents().name().getPath().substring("crow_fly_".length()));
                    frame = (frame % 2) + 1;
                    this.setSprite(Util.getSprite("crow_fly_" + frame));
                }
            }
        }

        // ONLY FOR TESTING
        /*
         * if (this.age % 10 == 0) {
         * System.out.println("Crow #" + this.hashCode() + " | State: " + this.state +
         * " | Height: "
         * + String.format("%.2f", this.y));
         * System.out.println("   Crow #" + this.hashCode() + " | xd: " +
         * String.format("%.3f", this.xd) + " | yd: "
         * + String.format("%.3f", this.yd) + " | zd: " + String.format("%.3f",
         * this.zd));
         * }
         */

        this.move(this.xd, this.yd, this.zd);
    }

    // --- BEHAVIORS ---

    // Returns other crow particles within radius (in the same level)
    private List<CrowParticle> getNeighbors(double radius) {
        double rsq = radius * radius;
        List<CrowParticle> out = new ArrayList<>();
        for (CrowParticle other : ALL_CROWS) {
            if (other == this)
                continue;
            if (other.level != this.level)
                continue;
            double dx = other.x - this.x;
            double dy = other.y - this.y;
            double dz = other.z - this.z;
            if (dx * dx + dy * dy + dz * dz <= rsq) {
                out.add(other);
            }
        }
        return out;
    }

    // Ask nearby flockmates to go land on the given perch (same BlockPos)
    private void groupPerch(BlockPos target) {
        if (target == null)
            return;
        for (CrowParticle nb : getNeighbors(flockRadius)) {
            if (nb == this)
                continue;
            if (nb.state == State.FLYING && nb.landingCooldown == 0) {
                nb.state = State.LANDING;
                nb.landingBlockPos = target;
                nb.landingTargetY = target.getY() + 1.0 + nb.quadSize;
                nb.goalTimer = 10;
                nb.goalX = target.getX() + 0.5;
                nb.goalY = nb.landingTargetY + 0.5;
                nb.goalZ = target.getZ() + 0.5;
            }
        }
    }

    // Ask nearby perched flockmates to take off with this crow
    private void groupTakeoff() {
        for (CrowParticle nb : getNeighbors(flockRadius)) {
            if (nb == this)
                continue;
            if (nb.state == State.PERCHED) {
                nb.state = State.TAKING_OFF;
                nb.setSprite(Util.getSprite("crow_fly_1"));
                nb.perchTimer = 5; // short window so they start their takeoff sequence quickly
                nb.landingCooldown = 100;
                nb.perchBlockPos = null;
            }
        }
    }

    // Scare nearby perched crows into a quick, directional takeoff away from the
    // source player
    private void scareTakeoff(Player source) {
        // vector away from player
        double dx = this.x - source.getX();
        double dz = this.z - source.getZ();
        double mag = Math.sqrt(dx * dx + dz * dz);
        if (mag < 0.001) {
            dx = (Math.random() - 0.5);
            dz = (Math.random() - 0.5);
            mag = Math.sqrt(dx * dx + dz * dz);
        }
        this.xd = (dx / mag) * scareTakeoffSpeed + (Math.random() - 0.5) * 0.05;
        this.zd = (dz / mag) * scareTakeoffSpeed + (Math.random() - 0.5) * 0.05;
        this.yd = 0.12 + Math.random() * 0.08;
        this.state = State.TAKING_OFF;
        this.setSprite(Util.getSprite("crow_fly_1"));
        this.perchTimer = 12;
        this.landingCooldown = 100;
        this.perchBlockPos = null;
        // Prompt group takeoff so flockmates flee too
        groupTakeoff();
    }

    // Generic takeoff when the perch block disappears
    private void perchBlockRemovedTakeoff() {
        // small random push
        this.xd += (Math.random() - 0.5) * 0.08;
        this.zd += (Math.random() - 0.5) * 0.08;
        this.yd = 0.12 + Math.random() * 0.05;
        this.state = State.TAKING_OFF;
        this.setSprite(Util.getSprite("crow_fly_1"));
        this.perchTimer = 10;
        this.landingCooldown = 100;
        this.perchBlockPos = null;
        // Tell flockmates to also take off
        groupTakeoff();
    }

    private void tickFlying() {
        // Make sure we don't repeatedly pick goals that end up at ground level
        double groundY = sampleGroundHeight(this.x, this.z);

        // Choose a new goal if we don't have one, it expired, or we're very close
        double dxToGoal = Double.isNaN(goalX) ? Double.POSITIVE_INFINITY : (goalX - this.x);
        double dyToGoal = Double.isNaN(goalY) ? Double.POSITIVE_INFINITY : (goalY - this.y);
        double dzToGoal = Double.isNaN(goalZ) ? Double.POSITIVE_INFINITY : (goalZ - this.z);
        double distSqToGoal = dxToGoal * dxToGoal + dyToGoal * dyToGoal + dzToGoal * dzToGoal;

        if (Double.isNaN(goalX) || goalTimer-- <= 0 || distSqToGoal < 0.5 * 0.5) {
            chooseNewGoal();
        }

        // FLOCK: compute neighbors and produce cohesion/alignment/separation forces
        List<CrowParticle> neighbors = getNeighbors(flockRadius);
        if (!neighbors.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            double avx = 0, avy = 0, avz = 0;
            int count = 0;
            for (CrowParticle nb : neighbors) {
                cx += nb.x;
                cy += nb.y;
                cz += nb.z;
                avx += nb.xd;
                avy += nb.yd;
                avz += nb.zd;
                count++;
            }
            cx /= count;
            cy /= count;
            cz /= count;
            avx /= count;
            avy /= count;
            avz /= count;

            // Cohesion: steer toward center
            double cohX = (cx - this.x) * cohesionStrength;
            double cohY = (cy - this.y) * cohesionStrength;
            double cohZ = (cz - this.z) * cohesionStrength;

            // Alignment: attempt to match velocity
            double aliX = (avx - this.xd) * alignmentStrength;
            double aliY = (avy - this.yd) * alignmentStrength;
            double aliZ = (avz - this.zd) * alignmentStrength;

            // Separation: push away from very close neighbors
            double sepX = 0, sepY = 0, sepZ = 0;
            for (CrowParticle nb : neighbors) {
                double dx = this.x - nb.x;
                double dy = this.y - nb.y;
                double dz = this.z - nb.z;
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 <= (separationDistance * separationDistance) && d2 > 0.0001) {
                    double d = Math.sqrt(d2);
                    double factor = (separationDistance - d) / separationDistance;
                    sepX += (dx / d) * factor;
                    sepY += (dy / d) * factor;
                    sepZ += (dz / d) * factor;
                }
            }
            sepX *= separationStrength;
            sepY *= separationStrength;
            sepZ *= separationStrength;

            // Apply small flocking influence directly to velocity (before clamping)
            this.xd += cohX + aliX + sepX;
            this.yd += cohY + aliY + sepY;
            this.zd += cohZ + aliZ + sepZ;
        }

        // If we're too close to ground, force a climb goal immediately
        if (this.y <= groundY + minFlightHeight + 0.3) {
            this.goalY = Math.max(this.goalY, this.y + takeoffClimb + Math.random() * 1.5);
            this.goalTimer = Math.max(this.goalTimer, 20);
        }

        // If we're too high above allowed flight height, bias/force a downward goal so
        // the crow returns downward
        double ceiling = groundY + maxFlightHeight;
        if (this.y >= ceiling - 0.5) {
            // prefer a point below the ceiling so it starts heading down
            this.goalY = Math.min(this.goalY, ceiling - 2.0 - Math.random() * 2.0);
            // shorten goal so it updates sooner
            this.goalTimer = Math.min(this.goalTimer, 40);
        }

        // Desired vector towards the goal
        double desiredX = goalX - this.x;
        double desiredY = goalY - this.y;
        double desiredZ = goalZ - this.z;
        double desiredDist = Math.sqrt(desiredX * desiredX + desiredY * desiredY + desiredZ * desiredZ);
        if (desiredDist > 0.0001) {
            // Normalize and scale to desired speed
            desiredX = (desiredX / desiredDist) * flySpeed;
            desiredY = (desiredY / desiredDist) * flySpeed;
            desiredZ = (desiredZ / desiredDist) * flySpeed;

            // Steering = desired - current velocity (limited)
            double steerX = desiredX - this.xd;
            double steerY = (desiredY - this.yd) * verticalSteerFactor; // stronger vertical steering
            double steerZ = desiredZ - this.zd;

            // If currently above ceiling, add a small extra downward bias to steering
            if (this.y >= ceiling - 0.5) {
                steerY -= 0.02 * verticalSteerFactor;
            }

            double steerMag = Math.sqrt(steerX * steerX + steerY * steerY + steerZ * steerZ);
            if (steerMag > steerStrength) {
                steerX = (steerX / steerMag) * steerStrength;
                steerY = (steerY / steerMag) * steerStrength;
                steerZ = (steerZ / steerMag) * steerStrength;
            }

            this.xd += steerX;
            this.yd += steerY;
            this.zd += steerZ;
        }

        // Clamp overall horizontal speed as before, and cap vertical speed to a
        // sensible climb/descent
        double horizontalSpeed = Math.sqrt(xd * xd + zd * zd);
        if (horizontalSpeed > flySpeed) {
            double scale = flySpeed / horizontalSpeed;
            this.xd *= scale;
            this.zd *= scale;
        }
        if (this.yd > maxVerticalSpeed)
            this.yd = maxVerticalSpeed;
        if (this.yd < -maxVerticalSpeed)
            this.yd = -maxVerticalSpeed;

        // Simple obstacle avoidance: look ahead and if blocked pick an evasive goal
        double lookX = this.x + this.xd * lookAheadMultiplier;
        double lookY = this.y + this.yd * lookAheadMultiplier;
        double lookZ = this.z + this.zd * lookAheadMultiplier;
        if (isBlocked(lookX, lookY, lookZ)) {
            // Try a quick upward evasive maneuver or pick a new higher goal
            if (!isBlocked(this.x, this.y + 2.0, this.z)) {
                this.yd = Math.max(this.yd, 0.12); // quick rise
            } else {
                double angle = Math.atan2(this.zd, this.xd) + (Math.random() < 0.5 ? Math.PI / 2 : -Math.PI / 2);
                this.goalX = this.x + Math.cos(angle) * (2 + Math.random() * 3);
                this.goalY = Math.max(this.y + 0.5, this.y + Math.random() * 2);
                this.goalZ = this.z + Math.sin(angle) * (2 + Math.random() * 3);
                this.goalTimer = 20 + (int) (Math.random() * 40);
            }
        }

        // Check for landing-scan behavior (rarer and only if cooldown expired)
        if (landingCooldown == 0 && Math.random() < this.perchingChance) {
            // Prefer to join a nearby perched crow (flock perching)
            for (CrowParticle nb : getNeighbors(12.0)) {
                if (nb.state == State.PERCHED && nb.perchBlockPos != null) {
                    BlockPos target = nb.perchBlockPos;
                    if (!level.getBlockState(target).isAir() && level.getBlockState(target.above()).isAir()) {
                        this.state = State.LANDING;
                        this.landingBlockPos = target;
                        this.landingTargetY = target.getY() + 1.0 + this.quadSize;
                        // Invite flockmates to land on the same perch
                        groupPerch(target);
                        return;
                    }
                }
            }

            for (int i = 1; i <= this.perchingDistance; i++) {
                BlockPos below = BlockPos.containing(x, y - i, z);
                if (!level.getBlockState(below).isAir()
                        && level.getBlockState(below.above()).isAir()
                        && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {

                    // Require a neighboring block (branch/cover) to avoid landing on open tree tops
                    // / flat ground
                    boolean hasNeighbor = !level.isEmptyBlock(below.north()) || !level.isEmptyBlock(below.south())
                            || !level.isEmptyBlock(below.east()) || !level.isEmptyBlock(below.west());
                    if (!hasNeighbor)
                        continue;

                    this.state = State.LANDING;
                    this.landingBlockPos = below;
                    this.landingTargetY = below.getY() + 1.0 + this.quadSize;
                    break;
                }
            }
            // If we just selected a perch, invite nearby flockmates to land there as well
            if (this.state == State.LANDING && this.landingBlockPos != null) {
                groupPerch(this.landingBlockPos);
            }
        }
    }

    // Pick a new wandering goal near the crow, biased slightly upward and slightly
    // in current motion direction
    private void chooseNewGoal() {
        double forwardBiasX = this.xd;
        double forwardBiasY = this.yd;
        double forwardBiasZ = this.zd;

        double randRadius = 2.5 + Math.random() * (goalRadius - 2.5);
        double angle = Math.random() * Math.PI * 2;
        double nx = Math.cos(angle) * randRadius + forwardBiasX * 5.0 * (Math.random() - 0.5);
        double nz = Math.sin(angle) * randRadius + forwardBiasZ * 5.0 * (Math.random() - 0.5);

        // ensure we pick a goal above ground and bias upwards when low or just took off
        double ground = sampleGroundHeight(this.x, this.z);
        double ny;
        if (this.y <= ground + minFlightHeight + 0.5 || landingCooldown > 0) {
            // force an upward goal to climb away from the perch/ground
            ny = this.y + 2.5 + Math.random() * 2.5;
        } else if (this.y >= ground + maxFlightHeight - 1.0) {
            // If we're currently above the allowed max height, pick a lower next goal
            ny = Math.max(ground + minFlightHeight, ground + maxFlightHeight - 2.0 - Math.random() * 3.0);
        } else {
            ny = this.y + (Math.random() - 0.5) * 2.0 + forwardBiasY * 1.5; // gentler vertical wander
            ny = Math.max(ny, ground + minFlightHeight);
        }

        // If there's a flock nearby, bias the goal toward the flock center so they move
        // together
        List<CrowParticle> neighbors = getNeighbors(flockRadius);
        if (!neighbors.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            for (CrowParticle nb : neighbors) {
                cx += nb.x;
                cy += nb.y;
                cz += nb.z;
            }
            cx /= neighbors.size();
            cy /= neighbors.size();
            cz /= neighbors.size();
            double baseX = this.x + nx;
            double baseY = ny;
            double baseZ = this.z + nz;
            // mix toward center
            this.goalX = baseX * (1.0 - flockGoalBias) + cx * flockGoalBias;
            this.goalY = baseY * (1.0 - flockGoalBias) + cy * flockGoalBias;
            this.goalZ = baseZ * (1.0 - flockGoalBias) + cz * flockGoalBias;
            // keep goals updating a bit more frequently so the flock stays cohesive
            this.goalTimer = Math.min(this.goalTimer, (goalDurationMin + goalDurationMax) / 4);
            return;
        }

        this.goalX = this.x + nx;
        this.goalY = Math.max(1.0, Math.min(255.0, ny)); // clamp within world height roughly
        this.goalZ = this.z + nz;

        this.goalTimer = goalDurationMin + (int) (Math.random() * (goalDurationMax - goalDurationMin));
    }

    // Checks if there's a solid/occupied collision at the given point (coarse
    // check)
    private boolean isBlocked(double px, double py, double pz) {
        BlockPos pos = BlockPos.containing(px, py, pz);
        if (level.isEmptyBlock(pos))
            return false;
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    // Find top-most solid block near the given x,z by scanning downward
    private double sampleGroundHeight(double px, double pz) {
        int startY = (int) Math.ceil(this.y);
        for (int y = startY; y >= Math.max(0, startY - 20); y--) {
            BlockPos pos = BlockPos.containing(px, y, pz);
            if (!level.getBlockState(pos).isAir()) {
                return pos.getY() + 1.0;
            }
        }
        return 0.0;
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
                    this.setSprite(Util.getSprite("crow_perch_1"));
                    this.perchTimer = this.perchingTime + (int) (Math.random() * this.perchingTime);
                    // Record the actual perch block so flockmates can join
                    this.perchBlockPos = this.landingBlockPos;
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

        if (Math.random() < 0.05) {
            this.setSprite(Util.getSprite("crow_perch_"
                    + (1 + (int) (Math.random() * 2))));
        }

        // If the block we're perching on disappears, take off
        if (this.perchBlockPos != null && level.getBlockState(this.perchBlockPos).isAir()) {
            perchBlockRemovedTakeoff();
            return;
        }

        // If a player gets too close, scare the crow and make it fly off
        double scareRadiusSq = scareRadius * scareRadius;
        for (Player p : this.level.players()) {
            double dx = p.getX() - this.x;
            double dz = p.getZ() - this.z;
            double distSq = dx * dx + dz * dz;
            double dy = Math.abs(p.getY() - this.y);
            if (distSq <= scareRadiusSq && dy < 3.0) {
                scareTakeoff(p);
                return;
            }
        }

        if (perchTimer-- <= 0) {
            this.state = State.TAKING_OFF;
            this.setSprite(Util.getSprite("crow_fly_1"));
            this.perchTimer = 20;
            // Tell nearby perched flockmates to also take off
            groupTakeoff();
        }
    }

    private void tickTakingOff() {
        // Clear any previous landing target
        this.landingTargetY = Double.NaN;
        this.landingBlockPos = null;
        // clear recorded perch so other crows no longer try to use it
        this.perchBlockPos = null;

        // Break collision with perch
        this.setPos(this.x, this.y + 0.05, this.z);

        // Stronger consistent upward speed and a small horizontal push so the crow can
        // climb away
        this.yd = 0.12 + Math.random() * 0.06;
        this.xd += (Math.random() - 0.5) * 0.05;
        this.zd += (Math.random() - 0.5) * 0.05;

        // Prevent immediate re-landing and give the crow a proper goal so it actually
        // flies away
        if (perchTimer-- <= 0) {
            this.state = State.FLYING;
            this.landingCooldown = 100; // a bit longer to avoid instant relanding
            chooseNewGoal();
            this.goalTimer = 30 + (int) (Math.random() * 40);
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
            // Prevent creating when we are at the cap
            if (ACTIVE_COUNT.get() >= MAX_ACTIVE_CROWS)
                return null;
            return new CrowParticle(level, x, y, z, this.sprite);
        }
    }
}