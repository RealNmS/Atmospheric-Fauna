package nms.atmosphericfauna.particle;

import nms.atmosphericfauna.AtmosphericFauna;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public abstract class BaseBirdParticle extends BaseParticle {

    private final StringBuilder sb = new StringBuilder();

    private enum State {
        FLYING,
        LANDING,
        PERCHED,
        TAKING_OFF,
        DYING
    }

    protected State state = State.FLYING;

    protected double goalX = Double.NaN;
    protected double goalY = Double.NaN;
    protected double goalZ = Double.NaN;
    protected int goalTimer = 0;

    protected int perchTimer = 0;
    protected int perchedTimer = 0;
    protected int landingCooldown = random.nextInt(600);
    protected Double landingTargetY = Double.NaN;
    protected BlockPos landingBlockPos = null;
    protected double landingOffsetX = 0.0;
    protected double landingOffsetZ = 0.0;
    protected BlockPos perchBlockPos = null; // stores actual perch while perched

    protected int wingFlapSpeed = 4;
    protected int wingFlapOffset = random.nextInt(wingFlapSpeed);

    protected String baseSpriteName = null;
    protected String spriteName = null;
    protected static final Map<String, Boolean> MIRROR_SPRITE_CACHE = new ConcurrentHashMap<>();
    protected boolean facingRight = false;

    private static final Set<BaseBirdParticle> ALL_BIRDS = ConcurrentHashMap.newKeySet();
    public static int maxActiveBirds = 100; // configurable max active birds

    // --- CONFIG STUFF ---

    public static boolean debugText = false;

    // --- VARIABLES ---

    protected float flySpeed;
    protected double steerStrength;
    protected double minFlightHeight; // how many blocks above ground
    protected double maxFlightHeight; // how many blocks from void
    protected double maxVerticalSpeed;
    protected double verticalSteerFactor;
    protected double takeoffClimb;
    protected double flockRadius;
    protected double cohesionStrength;
    protected double alignmentStrength;
    protected double separationDistance;
    protected double separationStrength;
    protected double flockGoalBias;

    protected double scareRadius; // horizontal distance that startles perched birds
    protected double scareTakeoffSpeed; // horizontal speed applied when scared

    protected double perchingChance;
    protected int perchingTime; // base time spent perched
    protected int perchingDistance; // how many blocks down to scan for landing spots

    protected double goalRadius;
    protected int goalDurationMin;
    protected int goalDurationMax;
    protected double lookAheadMultiplier;

    // --- CONSTRUCTORS ---

    protected BaseBirdParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);

        ALL_BIRDS.add(this);
        if (ALL_BIRDS.size() >= maxActiveBirds) {
            this.remove();
            return;
        }
    }

    // --- TICK ---

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(this.x, this.y, this.z);
            int renderDist = mc.options.renderDistance().get();
            double maxDist = (renderDist + 1) * 16.0;
            if (distSq > maxDist * maxDist) {
                this.remove();
                return;
            }
        }

        if (landingCooldown > 0)
            landingCooldown--;

        if (this.age++ >= this.lifetime && state != State.DYING) {
            this.state = State.DYING;
        }

        switch (state) {
            case FLYING -> tickFlying();
            case LANDING -> tickLanding();
            case PERCHED -> tickPerched();
            case TAKING_OFF -> tickTakingOff();
            case DYING -> tickDying();
        }

        // Update flapping animation
        if (state != State.DYING && state != State.PERCHED && this.baseSpriteName != null) {
            if (this.age % ((this.wingFlapSpeed - ((int) (this.yd * 20))) != 0
                    ? (this.wingFlapSpeed - ((int) (this.yd * 20)))
                    : 1) == this.wingFlapOffset) {
                int frame = (this.spriteName.charAt((this.spriteName.length() - 1)) - '0');
                if (frame == 1)
                    frame = 2;
                else
                    frame = 1;
                setSpriteName(frame);
            }
            if (this.age % 3 == 0)
                updateSpriteFacing();
        }

        // ONLY FOR TESTING
        if (debugText) {
            if (this.age % 10 == 0) {
                AtmosphericFauna.LOGGER.info(this.baseSpriteName + " #" + this.hashCode() + " | State: " + this.state +
                        " | Height: "
                        + String.format("%.2f", this.y));
                AtmosphericFauna.LOGGER.info(this.baseSpriteName + " #" + this.hashCode() + " | xd: " +
                        String.format("%.3f", this.xd) + " | yd: "
                        + String.format("%.3f", this.yd) + " | zd: " + String.format("%.3f",
                                this.zd));
            }
        }

        this.move(this.xd, this.yd, this.zd);
    }

    // --- HELPER METHODS ---

    @Override
    public void remove() {
        ALL_BIRDS.remove(this);
        super.remove();
    }

    // Returns other bird particles within radius (in the same level)
    private List<BaseBirdParticle> getNeighbors(double radius) {
        double rsq = radius * radius;
        List<BaseBirdParticle> out = new ArrayList<>();
        for (BaseBirdParticle other : ALL_BIRDS) {
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
        for (BaseBirdParticle nb : getNeighbors(flockRadius)) {
            if (nb == this)
                continue;
            if (nb.state == State.FLYING) {
                BlockPos actualTarget = target;

                // Try to find a slightly different spot nearby
                if (random.nextFloat() < 0.9) {
                    int dx = random.nextInt(7) - 3;
                    int dz = random.nextInt(7) - 3;

                    // Scan vertical range to find ground at this offset
                    for (int dy = 3; dy >= -3; dy--) {
                        BlockPos p = target.offset(dx, dy, dz);
                        if (!level.getBlockState(p).isAir() &&
                                level.getBlockState(p.above()).isAir() &&
                                level.getBlockState(p).isFaceSturdy(level, p, Direction.UP)) {
                            actualTarget = p;
                            break;
                        }
                    }
                }

                nb.state = State.LANDING;
                nb.landingBlockPos = actualTarget;
                nb.landingTargetY = actualTarget.getY() + 1.0 + nb.quadSize;
                nb.landingOffsetX = (Math.random() - 0.5) * 0.8;
                nb.landingOffsetZ = (Math.random() - 0.5) * 0.8;
            }
        }
    }

    // Ask nearby perched flockmates to take off with this bird
    private void groupTakeoff() {
        for (BaseBirdParticle nb : getNeighbors(flockRadius)) {
            if (nb == this)
                continue;
            if (nb.state == State.PERCHED) {
                nb.state = State.TAKING_OFF;
                nb.setSpriteName(1);
                nb.perchTimer = 5;
                nb.landingCooldown = 100 + nb.perchedTimer;
                nb.perchBlockPos = null;
            }
        }
    }

    // Takeoff logic
    private void performTakeoff(Player source) {
        if (source != null) {
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
        } else {
            this.xd = (Math.random() - 0.5) * 0.08;
            this.zd = (Math.random() - 0.5) * 0.08;
        }
        this.yd = 0.12 + Math.random() * 0.05;
        this.perchTimer = 12;
        this.landingCooldown = 100 + this.perchedTimer;
        this.perchBlockPos = null;

        this.state = State.TAKING_OFF;
        this.setSpriteName(1);
        groupTakeoff();
    }

    // Pick a new wandering goal near the bird, biased slightly upward and slightly
    // in current motion direction
    private void chooseNewGoal() {
        double forwardBiasX = this.xd;
        double forwardBiasY = this.yd;
        double forwardBiasZ = this.zd;

        double randRadius = 2.5 + Math.random() * (goalRadius - 2.5);
        double angle = Math.random() * Math.PI * 2;
        double nx = Math.cos(angle) * randRadius + forwardBiasX * 5.0 * (Math.random() - 0.5);
        double nz = Math.sin(angle) * randRadius + forwardBiasZ * 5.0 * (Math.random() - 0.5);

        // Ensure we pick a goal above ground and bias upwards when low or just took off
        double ground = sampleGroundHeight(this.x, this.z);
        double ny;
        if (this.y <= ground + minFlightHeight + 0.5 || landingCooldown > 0) {
            ny = this.y + 2.5 + Math.random() * 2.5;
        } else if (this.y >= ground + maxFlightHeight - 1.0) {
            ny = Math.max(ground + minFlightHeight, ground + maxFlightHeight - 2.0 - Math.random() * 3.0);
        } else {
            ny = this.y + (Math.random() - 0.5) * 2.0 + forwardBiasY * 1.5;
            ny = Math.max(ny, ground + minFlightHeight);
        }

        // If there's a flock nearby, bias the goal toward the flock center so they move
        // together
        List<BaseBirdParticle> neighbors = getNeighbors(flockRadius);
        if (!neighbors.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            for (BaseBirdParticle nb : neighbors) {
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

            this.goalX = baseX * (1.0 - flockGoalBias) + cx * flockGoalBias;
            this.goalY = baseY * (1.0 - flockGoalBias) + cy * flockGoalBias;
            this.goalZ = baseZ * (1.0 - flockGoalBias) + cz * flockGoalBias;

            this.goalTimer = Math.min(this.goalTimer, (goalDurationMin + goalDurationMax) / 4);
            return;
        }

        this.goalX = this.x + nx;
        this.goalY = Math.max(1.0, Math.min(255.0, ny));
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

    // --- BEHAVIORS ---

    private void tickFlying() {
        double groundY = sampleGroundHeight(this.x, this.z);

        double dxToGoal = Double.isNaN(goalX) ? Double.POSITIVE_INFINITY : (goalX - this.x);
        double dyToGoal = Double.isNaN(goalY) ? Double.POSITIVE_INFINITY : (goalY - this.y);
        double dzToGoal = Double.isNaN(goalZ) ? Double.POSITIVE_INFINITY : (goalZ - this.z);
        double distSqToGoal = dxToGoal * dxToGoal + dyToGoal * dyToGoal + dzToGoal * dzToGoal;

        if (Double.isNaN(goalX) || goalTimer-- <= 0 || distSqToGoal < 0.5 * 0.5) {
            chooseNewGoal();
        }

        // Flocking behavior
        List<BaseBirdParticle> neighbors = getNeighbors(flockRadius);
        if (!neighbors.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            double avx = 0, avy = 0, avz = 0;
            int count = 0;
            for (BaseBirdParticle nb : neighbors) {
                if (nb.state == State.PERCHED || nb.state == State.DYING)
                    continue;
                cx += nb.x;
                cy += nb.y;
                cz += nb.z;
                avx += nb.xd;
                avy += nb.yd;
                avz += nb.zd;
                count++;
            }

            if (count > 0) {
                cx /= count;
                cy /= count;
                cz /= count;
                avx /= count;
                avy /= count;
                avz /= count;

                double cohX = (cx - this.x) * cohesionStrength;
                double cohY = (cy - this.y) * cohesionStrength;
                double cohZ = (cz - this.z) * cohesionStrength;

                double aliX = (avx - this.xd) * alignmentStrength;
                double aliY = (avy - this.yd) * alignmentStrength;
                double aliZ = (avz - this.zd) * alignmentStrength;

                double sepX = 0, sepY = 0, sepZ = 0;
                for (BaseBirdParticle nb : neighbors) {
                    if (nb.state == State.PERCHED || nb.state == State.DYING)
                        continue;
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

                this.xd += cohX + aliX + sepX;
                this.yd += cohY + aliY + sepY;
                this.zd += cohZ + aliZ + sepZ;
            }
        }

        if (this.y <= groundY + minFlightHeight + 0.3) {
            this.goalY = Math.max(this.goalY, this.y + takeoffClimb + Math.random() * 1.5);
            this.goalTimer = Math.max(this.goalTimer, 20);
        }

        double ceiling = groundY + maxFlightHeight;
        if (this.y >= ceiling - 0.5) {
            this.goalY = Math.min(this.goalY, ceiling - 2.0 - Math.random() * 2.0);
            this.goalTimer = Math.min(this.goalTimer, 40);
        }

        // Desired vector towards the goal
        double desiredX = goalX - this.x;
        double desiredY = goalY - this.y;
        double desiredZ = goalZ - this.z;
        double desiredDist = Math.sqrt(desiredX * desiredX + desiredY * desiredY + desiredZ * desiredZ);
        if (desiredDist > 0.0001) {
            desiredX = (desiredX / desiredDist) * flySpeed;
            desiredY = (desiredY / desiredDist) * flySpeed;
            desiredZ = (desiredZ / desiredDist) * flySpeed;

            double steerX = desiredX - this.xd;
            double steerY = (desiredY - this.yd) * verticalSteerFactor;
            double steerZ = desiredZ - this.zd;

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
            if (!isBlocked(this.x, this.y + 2.0, this.z)) {
                this.yd = Math.max(this.yd, 0.12);
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
            for (BaseBirdParticle nb : getNeighbors(12.0)) {
                if (nb.state == State.PERCHED && nb.perchBlockPos != null) {
                    BlockPos target = nb.perchBlockPos;
                    if (!level.getBlockState(target).isAir() && level.getBlockState(target.above()).isAir()) {
                        this.state = State.LANDING;
                        this.landingBlockPos = target;
                        this.landingTargetY = target.getY() + 2.0 + this.quadSize;
                        groupPerch(target);
                        return;
                    }
                }
            }

            for (int i = 1; i <= this.perchingDistance; i++) {
                BlockPos below = BlockPos.containing(this.x, this.y - i, this.z);
                BlockPos above = below.above();

                // Basic checks: below must be solid, above must be air
                if (level.getBlockState(below).isAir())
                    continue;
                if (!level.getBlockState(above).isAir())
                    continue;

                // Ensure the top face is sturdy enough to land on (avoid tiny/wire blocks)
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))
                    continue;

                // Ensure there's some collision shape to stand on
                if (level.getBlockState(below).getCollisionShape(level, below).isEmpty())
                    continue;

                // Require a neighboring block (branch/cover) to avoid landing on open tree tops
                // / flat ground
                boolean hasNeighbor = !level.isEmptyBlock(below.north()) || !level.isEmptyBlock(below.south())
                        || !level.isEmptyBlock(below.east()) || !level.isEmptyBlock(below.west());
                if (!hasNeighbor)
                    continue;

                // Success: choose this block as perch. Use a corrected landing Y so the bird
                // sits at
                // block top (subtract quadSize rather than add to avoid floating too high)
                this.state = State.LANDING;
                this.landingBlockPos = below;
                this.landingOffsetX = (Math.random() - 0.5) * 0.8;
                this.landingOffsetZ = (Math.random() - 0.5) * 0.8;
                this.landingTargetY = below.getY() + 1.0 + this.quadSize;
                break;
            }

            if (this.state == State.LANDING && this.landingBlockPos != null) {
                groupPerch(this.landingBlockPos);
            }
        }
    }

    private void tickLanding() {
        this.perchedTimer = 0;

        // If target missing, abort to flying
        if (this.landingBlockPos == null || Double.isNaN(this.landingTargetY)) {
            this.state = State.FLYING;
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.landingOffsetX = 0.0;
            this.landingOffsetZ = 0.0;
            return;
        }

        double targetX = this.landingBlockPos.getX() + 0.5 + this.landingOffsetX;
        double targetZ = this.landingBlockPos.getZ() + 0.5 + this.landingOffsetZ;

        // Gentle horizontal damping so steering is stable
        this.xd *= 0.98;
        this.zd *= 0.98;

        // Estimate time to land
        double verticalDist = this.y - this.landingTargetY;
        double timeToLand;
        if (this.yd < -0.001) {
            timeToLand = verticalDist / -this.yd;
            if (timeToLand < 0.1)
                timeToLand = 0.1;
        } else {
            timeToLand = Math.max(0.5, verticalDist / 0.06);
        }

        double desiredXd = (targetX - this.x) / timeToLand;
        double desiredZd = (targetZ - this.z) / timeToLand;

        double maxLandingSpeed = 0.07;
        double desiredHoriz = Math.sqrt(desiredXd * desiredXd + desiredZd * desiredZd);
        if (desiredHoriz > maxLandingSpeed) {
            double s = maxLandingSpeed / desiredHoriz;
            desiredXd *= s;
            desiredZd *= s;
        }

        double steerFactor = 0.25;
        this.xd += (desiredXd - this.xd) * steerFactor;
        this.zd += (desiredZd - this.zd) * steerFactor;

        // gentle descent proportional to remaining distance
        double descent = Math.min(0.20, Math.max(0.06, verticalDist * 0.03));
        this.yd = -descent;

        double dx = targetX - this.x;
        double dz = targetZ - this.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double horizSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);

        // Snap if close and slow
        if (horizDist < 0.35 && Math.abs(this.y - this.landingTargetY) < 0.25 && horizSpeed < 0.06) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos).isAir()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                this.state = State.PERCHED;
                this.setSpriteName(1);
                this.perchTimer = this.perchingTime + (int) (Math.random() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
            } else {
                this.state = State.FLYING;
            }
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            return;
        }

        // Finalize if we pass the landing Y and are reasonably close horizontally
        if (this.y <= this.landingTargetY + 0.2 && horizDist < 0.6) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos).isAir()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                this.state = State.PERCHED;
                this.setSpriteName(1);
                this.perchTimer = this.perchingTime + (int) (Math.random() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
            } else {
                this.state = State.FLYING;
            }
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.landingOffsetX = 0.0;
            this.landingOffsetZ = 0.0;
        }
    }

    private void tickPerched() {
        this.perchedTimer++;

        this.xd = 0;
        this.zd = 0;
        this.yd = 0;

        if (Math.random() < 0.05) {
            this.setSpriteName((1 + (int) (Math.random() * 2)));
        }

        if (this.perchBlockPos != null && level.getBlockState(this.perchBlockPos).isAir()) {
            performTakeoff(null);
            return;
        }

        // If a player gets too close, scare the bird and make it fly off
        double scareRadiusSq = scareRadius * scareRadius;
        for (Player p : this.level.players()) {
            // Ignore spectator players
            if (p.isSpectator())
                continue;

            double dx = p.getX() - this.x;
            double dz = p.getZ() - this.z;
            double distSq = dx * dx + dz * dz;
            double dy = Math.abs(p.getY() - this.y);
            if (distSq <= scareRadiusSq && dy < 3.0) {
                performTakeoff(p);
                return;
            }
        }

        if (perchTimer-- <= 0) {
            this.state = State.TAKING_OFF;
            this.setSpriteName(1);
            this.perchTimer = 20;
            groupTakeoff();
        }
    }

    private void tickTakingOff() {
        this.landingTargetY = Double.NaN;
        this.landingBlockPos = null;
        this.landingOffsetX = 0.0;
        this.landingOffsetZ = 0.0;
        this.perchBlockPos = null;

        this.setPos(this.x, this.y + 0.05, this.z);

        this.yd = 0.12 + Math.random() * 0.06;
        this.xd += (Math.random() - 0.5) * 0.05;
        this.zd += (Math.random() - 0.5) * 0.05;

        if (perchTimer-- <= 0) {
            this.state = State.FLYING;
            this.landingCooldown = 100;
            chooseNewGoal();
            this.goalTimer = 30 + (int) (Math.random() * 40);
        }
    }

    private void tickDying() {
        this.yd -= 0.02;

        // Remove if we hit the void or have fallen far enough
        if (this.y < -64) {
            if (debugText) {
                AtmosphericFauna.LOGGER.info(
                        this.baseSpriteName + " #" + this.hashCode() + " has died at age " + this.age + " ticks.");
            }
            this.remove();
        }

        // Hard limit to prevent memory leaks if it falls forever
        if (this.age > this.lifetime + 200) {
            if (debugText) {
                AtmosphericFauna.LOGGER.info(this.baseSpriteName + " #" + this.hashCode()
                        + " forcibly removed after exceeding death time limit.");
            }
            this.remove();
        }
    }

    // --- SPRITE HANDLING ---

    protected void setSpriteName(Integer frame) {
        sb.setLength(0);
        sb.append(this.baseSpriteName);

        if (this.state != State.PERCHED) {
            sb.append("_flying");
        } else {
            sb.append("_").append(this.state.toString().toLowerCase());
        }

        if (this.facingRight) {
            sb.append("_r");
        }

        sb.append("_").append(frame == null ? "1" : frame);

        this.spriteName = sb.toString();
        this.setSprite(getSprite(this.spriteName));
    }

    // compute desired facing: prefer motion if strong, otherwise face camera/player
    // Improved facing logic: calculates movement relative to the camera's view
    // plane
    private void updateSpriteFacing() {
        double horizSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
        double motionThreshold = 0.01;

        if (horizSpeed > motionThreshold) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                float yaw = player.getYRot();

                double yawRad = Math.toRadians(yaw);
                double lookX = -Math.sin(yawRad);
                double lookZ = Math.cos(yawRad);

                double rightX = -lookZ;
                double rightZ = lookX;

                double dot = (this.xd * rightX) + (this.zd * rightZ);

                this.facingRight = dot > 0;
            } else {
                this.facingRight = this.xd > 0;
            }
        }

        int frame = 1;
        if (this.spriteName != null && !this.spriteName.isEmpty()) {
            char c = this.spriteName.charAt(this.spriteName.length() - 1);
            if (Character.isDigit(c)) {
                frame = Character.getNumericValue(c);
            }
        }
        setSpriteName(frame);
    }
}
