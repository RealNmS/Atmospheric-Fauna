package nms.atmosphericfauna.particle.base;

import nms.atmosphericfauna.AtmosphericFauna;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseBirdParticle extends BaseParticle {

    private enum State {
        FLYING, LANDING, PERCHED, TAKING_OFF, DYING
    }

    protected State state = State.FLYING;

    protected double goalX = Double.NaN;
    protected double goalY = Double.NaN;
    protected double goalZ = Double.NaN;
    protected int goalTimer = 0;

    protected int perchTimer = 0;
    protected int perchedTimer = 0;
    protected int landingCooldown = random.nextInt(600);
    protected int flockCooldown = 0;
    protected Double landingTargetY = Double.NaN;
    protected BlockPos landingBlockPos = null;
    protected double landingOffsetX = 0.0;
    protected double landingOffsetZ = 0.0;
    protected int landingDelay = 0;
    protected BlockPos perchBlockPos = null;
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    protected Double takeoffGoalY = Double.NaN;
    protected int takeoffTime = 0;
    protected double lastDistSqToGoal = -1.0;
    protected int stuckTicks = 0;
    protected double heightAdherence = 0.0015; // How strongly the bird pulls back to its target height
    protected double heightTolerance = 8.0; // How far (in blocks) they can drift before they start caring

    protected String baseSpriteName = null;
    protected String spriteName = null;
    protected boolean facingRight = false;

    public static final List<BaseBirdParticle> ALL_BIRDS = Collections.synchronizedList(new ArrayList<>());
    public static final List<List<BaseBirdParticle>> SPECIES_REGISTRY = new ArrayList<>();
    private final List<BaseBirdParticle> reusableNeighborList = new ArrayList<>();
    private final List<BaseBirdParticle> cachedFlockNeighbors = new ArrayList<>();
    private int neighborCacheTimer = 0;

    protected static Minecraft mc = Minecraft.getInstance();

    // --- VARIABLES ---

    protected float flySpeed;
    protected int wingFlapSpeed;
    protected int wingFlapOffset;
    protected double steerStrength;
    protected double minFlightHeight;
    protected double preferredFlightHeight;
    protected double maxVerticalSpeed;
    protected double verticalSteerFactor;
    protected double takeoffClimb;
    protected double flockRadius;
    protected double cohesionStrength;
    protected double alignmentStrength;
    protected double separationDistance;
    protected double separationStrength;
    protected double flockGoalBias;
    protected int maxFlockSize = Integer.MAX_VALUE;
    protected boolean fliesOverOcean = true;

    protected double scareRadius;
    protected double scareTakeoffSpeed;

    protected double perchingChance;
    protected int perchingTime;
    protected int perchingDistance;

    protected double goalRadius;
    protected int goalDurationMin;
    protected int goalDurationMax;
    protected double lookAheadMultiplier;

    // --- CONSTRUCTORS ---

    protected BaseBirdParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        if (this.removed)
            return;
        ALL_BIRDS.add(this);
        getSpeciesList().add(this); // Track inside species partition[cite: 10]
    }

    // MARK: --- HELPER METHODS ---

    //? if <=1.21.1 {
    // private int getLevelMinY() { return this.level.getMinBuildHeight(); }
    //?} else {
    private int getLevelMinY() {
        return this.level.getMinY();
    }
    //?}

    protected abstract List<BaseBirdParticle> getSpeciesList();

    public abstract int getSpeciesMaxCount();

    public static void reset() {
        ALL_BIRDS.clear();
        synchronized (SPECIES_REGISTRY) {
            for (int i = 0; i < SPECIES_REGISTRY.size(); i++) {
                SPECIES_REGISTRY.get(i).clear();
            }
        }
    }

    @Override
    public void remove() {
        ALL_BIRDS.remove(this);
        getSpeciesList().remove(this);
        super.remove();
    }

    public static List<BaseBirdParticle> getAllBirds() {
        return ALL_BIRDS;
    }

    public static int getMaxActiveBirds() {
        return maxActiveBirds;
    }

    public String getBaseSpriteName() {
        return this.baseSpriteName;
    }

    private static void setState(BaseBirdParticle bird, State newState) {
        bird.state = newState;
        bird.setSpriteName(1);
    }

    // MARK: --- TICK ---

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(this.x, this.y, this.z);
            int renderDist = mc.options.renderDistance().get();
            double maxDist = (renderDist + 1) * 16.0;

            if (distSq > maxDist * maxDist) {
                for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                    if (!nb.removed) {
                        nb.remove();
                    }
                }
                this.remove();
                return;
            }
        }

        if (landingCooldown > 0)
            landingCooldown--;
        if (flockCooldown > 0)
            flockCooldown--;

        if (this.age++ >= this.lifetime && state != State.DYING) {
            setState(this, State.DYING);
        }

        switch (state) {
            case FLYING -> tickFlying();
            case LANDING -> tickLanding();
            case PERCHED -> tickPerched();
            case TAKING_OFF -> tickTakingOff();
            case DYING -> tickDying();
        }

        // Update sprite animation
        if (state != State.DYING && state != State.PERCHED && this.baseSpriteName != null) {
            int flapAdjustment = (int) (this.yd * 10);
            int effectiveFlapSpeed = Math.max(1, this.wingFlapSpeed - flapAdjustment);
            if ((this.age - this.wingFlapOffset) % effectiveFlapSpeed == 0) {
                int frame = 1;
                if (this.spriteName != null && !this.spriteName.isEmpty()) {
                    char lastChar = this.spriteName.charAt(this.spriteName.length() - 1);
                    if (Character.isDigit(lastChar)) {
                        frame = Character.getNumericValue(lastChar);
                    }
                }
                setSpriteName(frame == 1 ? 2 : 1);
            }
            if (this.age % 3 == 0)
                updateSpriteFacing();
        }

        // debug stuff
        if (debugTextBirds) {
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

    // Returns all other bird particles within radius
    private List<BaseBirdParticle> getNeighbors(double radius) {
        double rsq = radius * radius;
        reusableNeighborList.clear();

        List<BaseBirdParticle> speciesList = getSpeciesList();
        synchronized (speciesList) {
            int size = speciesList.size();
            for (int i = 0; i < size; i++) {
                BaseBirdParticle other = speciesList.get(i);
                if (other == this || other.level != this.level)
                    continue;

                // Fast fail bounds check
                if (Math.abs(other.x - this.x) > radius ||
                        Math.abs(other.y - this.y) > radius ||
                        Math.abs(other.z - this.z) > radius)
                    continue;

                double dx = other.x - this.x;
                double dy = other.y - this.y;
                double dz = other.z - this.z;

                if (dx * dx + dy * dy + dz * dz <= rsq) {
                    reusableNeighborList.add(other);
                }
            }
        }
        return reusableNeighborList;
    }

    // Ask nearby flockmates to go land on the given perch
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

                    for (int dy = 3; dy >= -3; dy--) {
                        mutablePos.set(target.getX() + dx, target.getY() + dy, target.getZ() + dz);
                        BlockState state = level.getBlockState(mutablePos);

                        if (!state.isAir()) {
                            mutablePos.move(Direction.UP);
                            if (level.getBlockState(mutablePos).isAir()) {
                                mutablePos.move(Direction.DOWN);
                                if (state.isFaceSturdy(level, mutablePos, Direction.UP)) {
                                    actualTarget = mutablePos.immutable();
                                    break;
                                }
                            }
                        }
                    }
                }

                if (actualTarget.getY() <= nb.y) {
                    nb.landingDelay = 10 + this.random.nextInt(35);
                    nb.landingBlockPos = actualTarget;
                    nb.landingTargetY = actualTarget.getY() + 1.0 + nb.quadSize;
                    nb.landingOffsetX = (this.random.nextFloat() - 0.5f) * 0.8;
                    nb.landingOffsetZ = (this.random.nextFloat() - 0.5f) * 0.8;
                }
            }
        }
    }

    // Ask nearby perched flockmates to take off with this bird
    private void groupTakeoff() {
        for (BaseBirdParticle nb : getNeighbors(flockRadius)) {
            if (nb == this)
                continue;
            if (nb.state == State.PERCHED) {
                if (nb.perchTimer > 30) {
                    nb.perchTimer = 5 + this.random.nextInt(25);
                }
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
                dx = (this.random.nextFloat() - 0.5f);
                dz = (this.random.nextFloat() - 0.5f);
                mag = Math.sqrt(dx * dx + dz * dz);
            }
            this.xd = (dx / mag) * scareTakeoffSpeed + (this.random.nextFloat() - 0.5f) * 0.05;
            this.zd = (dz / mag) * scareTakeoffSpeed + (this.random.nextFloat() - 0.5f) * 0.05;
        } else {
            this.xd = (this.random.nextFloat() - 0.5f) * 0.08;
            this.zd = (this.random.nextFloat() - 0.5f) * 0.08;
        }

        this.yd += 0.06 + this.random.nextFloat() * 0.06;
        this.perchTimer = 8 + this.random.nextInt(8);
        this.landingCooldown = 200 + this.perchedTimer + this.random.nextInt(400);

        double base = this.perchBlockPos != null ? this.perchBlockPos.getY() + 1.0 : this.y;
        this.perchBlockPos = null;
        this.takeoffGoalY = base + Math.max(0.8, takeoffClimb * (0.5 + this.random.nextDouble() * 0.8))
                + this.random.nextDouble() * 1.2;
        this.takeoffTime = 0;

        setState(this, State.TAKING_OFF);
        groupTakeoff();
    }

    // Pick a new wandering goal
    private void chooseNewGoal() {
        double randRadius = 2.5 + this.random.nextFloat() * (goalRadius - 2.5);
        double angle = this.random.nextFloat() * Math.PI * 2;
        double nx = Math.cos(angle) * randRadius + this.xd * 5.0 * (this.random.nextFloat() - 0.5f);
        double nz = Math.sin(angle) * randRadius + this.zd * 5.0 * (this.random.nextFloat() - 0.5f);

        // Water avoidance loop
        if (!this.fliesOverOcean) {
            int attempts = 0;
            while (isOceanBiome(this.x + nx, this.z + nz) && attempts < 15) {
                attempts++;
                if (attempts % 3 == 0)
                    randRadius += 15.0;

                angle = this.random.nextFloat() * Math.PI * 2;
                nx = Math.cos(angle) * randRadius;
                nz = Math.sin(angle) * randRadius;
            }
        }

        double ground = sampleGroundHeight(this.x, this.z);
        double absoluteCeiling = (getLevelMinY() + level.getHeight()) - 5.0;
        double targetHeight = Math.min(ground + preferredFlightHeight, absoluteCeiling);
        double ny;

        if (this.y <= ground + minFlightHeight + 0.5 || landingCooldown > 0) {
            ny = this.y + 2.5 + this.random.nextFloat() * 2.5;
        } else if (this.y >= absoluteCeiling - 1.0) {
            ny = this.y - 2.0 - this.random.nextFloat() * 3.0;
        } else {
            double heightDiff = targetHeight - this.y;
            double drift;

            if (Math.abs(heightDiff) > this.heightTolerance) {
                double pull = heightDiff * (this.heightAdherence * 50.0);
                drift = pull + (Math.signum(heightDiff) * this.random.nextFloat() * 2.0);
            } else {
                drift = Math.signum(heightDiff) * (this.random.nextFloat() * 1.5);
            }

            ny = this.y + (this.random.nextFloat() - 0.5f) * 2.0 + this.yd * 1.5 + drift;
            ny = Math.max(ny, ground + minFlightHeight);
        }

        ny = Math.max(getLevelMinY() + 1.0, Math.min(absoluteCeiling, ny));

        // Flock Bias
        List<BaseBirdParticle> neighbors = this.cachedFlockNeighbors;
        int flyingCount = 0;
        double cx = 0, cy = 0, cz = 0;

        for (BaseBirdParticle nb : neighbors) {
            if (nb.state == State.FLYING) {
                cx += nb.x;
                cy += nb.y;
                cz += nb.z;
                flyingCount++;
            }
        }

        if (flyingCount > 0) {
            cx /= flyingCount;
            cy /= flyingCount;
            cz /= flyingCount;

            if (this.maxFlockSize > 0 && flyingCount > this.maxFlockSize) {
                double pushX = this.x - cx;
                double pushZ = this.z - cz;
                double dist = Math.sqrt(pushX * pushX + pushZ * pushZ);

                if (dist > 0.001) {
                    pushX /= dist;
                    pushZ /= dist;
                } else {
                    pushX = (this.random.nextFloat() - 0.5f);
                    pushZ = (this.random.nextFloat() - 0.5f);
                }

                this.goalX = this.x + pushX * (goalRadius * 1.5) + (this.random.nextFloat() - 0.5f) * 5.0;
                this.goalY = ny;
                this.goalZ = this.z + pushZ * (goalRadius * 1.5) + (this.random.nextFloat() - 0.5f) * 5.0;
                this.goalTimer = Math.min(this.goalTimer, goalDurationMin / 2);
            } else {
                this.goalX = (this.x + nx) * (1.0 - flockGoalBias) + cx * flockGoalBias;
                this.goalY = ny * (1.0 - flockGoalBias) + cy * flockGoalBias;
                this.goalZ = (this.z + nz) * (1.0 - flockGoalBias) + cz * flockGoalBias;
                this.goalTimer = Math.min(this.goalTimer, (goalDurationMin + goalDurationMax) / 4);
            }
            return;
        }

        this.goalX = this.x + nx;
        this.goalY = ny;
        this.goalZ = this.z + nz;
        this.goalTimer = goalDurationMin + (int) (this.random.nextFloat() * (goalDurationMax - goalDurationMin));
    }

    private boolean isBlocked(double px, double py, double pz) {
        mutablePos.set(px, py, pz);
        BlockState state = level.getBlockState(mutablePos);

        if (state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES)) {
            return false;
        }

        return !state.getCollisionShape(level, mutablePos).isEmpty();
    }

    private boolean isOceanBiome(double px, double pz) {
        mutablePos.set(px, 62, pz);
        return level.getBiome(mutablePos).is(BiomeTags.IS_OCEAN);
    }

    private double sampleGroundHeight(double px, double pz) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(px), (int) Math.floor(pz));
        if (this.y >= surfaceY)
            return surfaceY;

        int startY = (int) Math.ceil(this.y);
        int endY = Math.max(getLevelMinY(), startY - 8);

        for (int y = startY; y >= endY; y--) {
            mutablePos.set(px, y, pz);
            if (!level.isEmptyBlock(mutablePos)) {
                return y + 1.0;
            }
        }
        return getLevelMinY();
    }

    // MARK: --- BEHAVIORS ---

    private void tickFlying() {
        if (this.landingDelay > 0) {
            this.landingDelay--;
            if (this.landingDelay <= 0 && this.landingBlockPos != null) {
                setState(this, State.LANDING);
                return;
            }
        }

        double groundY = sampleGroundHeight(this.x, this.z);

        double absoluteCeiling = (getLevelMinY() + level.getHeight()) - 5.0;
        double targetHeight = Math.min(groundY + preferredFlightHeight, absoluteCeiling);

        double dxToGoal = Double.isNaN(goalX) ? Double.POSITIVE_INFINITY : (goalX - this.x);
        double dyToGoal = Double.isNaN(goalY) ? Double.POSITIVE_INFINITY : (goalY - this.y);
        double dzToGoal = Double.isNaN(goalZ) ? Double.POSITIVE_INFINITY : (goalZ - this.z);
        double distSqToGoal = dxToGoal * dxToGoal + dyToGoal * dyToGoal + dzToGoal * dzToGoal;

        // Stuck detection
        if (Math.abs(this.lastDistSqToGoal - distSqToGoal) < 0.05) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
        }
        this.lastDistSqToGoal = distSqToGoal;

        if (Double.isNaN(goalX) || goalTimer-- <= 0 || distSqToGoal < 0.5 * 0.5 || this.stuckTicks > 40) {
            chooseNewGoal();
            this.stuckTicks = 0;
        }

        if (neighborCacheTimer-- <= 0 || cachedFlockNeighbors.isEmpty()) {
            neighborCacheTimer = 2 + this.random.nextInt(3);
            cachedFlockNeighbors.clear();
            cachedFlockNeighbors.addAll(getNeighbors(flockRadius));
        }

        List<BaseBirdParticle> neighbors = cachedFlockNeighbors;
        boolean isScattering = this.flockCooldown > 0;

        if (!neighbors.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            double avx = 0, avy = 0, avz = 0;
            double sepX = 0, sepY = 0, sepZ = 0;
            int flockingCount = 0;

            for (BaseBirdParticle nb : neighbors) {
                if (nb.state != State.FLYING)
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

                if (!isScattering && nb.flockCooldown == 0) {
                    if (!this.fliesOverOcean && isOceanBiome(nb.x, nb.z))
                        continue;

                    cx += nb.x;
                    cy += nb.y;
                    cz += nb.z;
                    avx += nb.xd;
                    avy += nb.yd;
                    avz += nb.zd;
                    flockingCount++;
                }
            }

            // Apply universal separation
            sepX *= separationStrength * 1.6;
            sepY *= separationStrength * 0.9;
            sepZ *= separationStrength * 1.6;
            this.xd += sepX;
            this.yd += sepY;
            this.zd += sepZ;

            // Process Flock Mind
            if (flockingCount > 0 && !isScattering) {
                cx /= flockingCount;
                cy /= flockingCount;
                cz /= flockingCount;
                avx /= flockingCount;
                avy /= flockingCount;
                avz /= flockingCount;

                boolean isOvercrowded = this.maxFlockSize > 0 && flockingCount >= this.maxFlockSize;

                if (isOvercrowded) {
                    this.flockCooldown = 200 + this.random.nextInt(200);

                    // Pick an escape goal entirely away from the flock's center
                    double angleAway = Math.atan2(this.z - cz, this.x - cx);
                    if (Double.isNaN(angleAway))
                        angleAway = this.random.nextFloat() * Math.PI * 2;

                    this.goalX = this.x + Math.cos(angleAway) * (goalRadius * 1.5);
                    this.goalY = Math.max(groundY + minFlightHeight, this.y + (this.random.nextFloat() - 0.2f) * 10.0);
                    this.goalZ = this.z + Math.sin(angleAway) * (goalRadius * 1.5);
                    this.goalTimer = 80; // Hard commit to this escape route
                } else {
                    // Normal Alignment and Cohesion
                    double aliX = (avx - this.xd) * (alignmentStrength * 1.6);
                    double aliY = (avy - this.yd) * (alignmentStrength * 1.2);
                    double aliZ = (avz - this.zd) * (alignmentStrength * 1.6);

                    double currentCohesion = cohesionStrength * 0.45;
                    double cohX = (cx - this.x) * currentCohesion;
                    double cohY = (cy - this.y) * currentCohesion;
                    double cohZ = (cz - this.z) * currentCohesion;

                    this.xd += cohX + aliX;
                    this.yd += cohY + aliY;
                    this.zd += cohZ + aliZ;

                    BaseBirdParticle leader = this;
                    for (BaseBirdParticle nb : neighbors) {
                        if (nb.state == State.FLYING && nb.hashCode() < leader.hashCode()) {
                            leader = nb;
                        }
                    }

                    if (leader != this) {
                        double aheadFactor = 4.0;
                        double syncX = this.x + (leader.xd * aheadFactor) + (cx - this.x) * 0.18;
                        double syncY = this.y + (leader.yd * Math.max(1.0, aheadFactor * 0.5)) + (cy - this.y) * 0.12;
                        double syncZ = this.z + (leader.zd * aheadFactor) + (cz - this.z) * 0.18;

                        if (this.fliesOverOcean || !isOceanBiome(syncX, syncZ)) {
                            this.goalX = syncX;
                            this.goalY = syncY;
                            this.goalZ = syncZ;
                            this.goalTimer = Math.min(this.goalTimer,
                                    Math.max(8, (goalDurationMin + goalDurationMax) / 6));
                        }
                    }
                }
            }
        }

        if (this.y <= groundY + minFlightHeight + 0.3) {
            this.goalY = Math.max(this.goalY, this.y + takeoffClimb + this.random.nextFloat() * 1.5);
            this.goalTimer = Math.max(this.goalTimer, 20);
        }

        if (this.y >= absoluteCeiling - 0.5) {
            this.goalY = Math.min(this.goalY, absoluteCeiling - 2.0 - this.random.nextFloat() * 2.0);
            this.goalTimer = Math.min(this.goalTimer, 40);
        }

        // Desired vector towards the goal
        double desiredX = goalX - this.x;
        double desiredY = goalY - this.y;
        double desiredZ = goalZ - this.z;
        double desiredDist = Math.sqrt(desiredX * desiredX + desiredY * desiredY + desiredZ * desiredZ);

        double lookX = this.x + this.xd * lookAheadMultiplier;
        double lookY = this.y + this.yd * lookAheadMultiplier;
        double lookZ = this.z + this.zd * lookAheadMultiplier;

        boolean blockAvoidance = isBlocked(lookX, lookY, lookZ);
        boolean waterAvoidance = !this.fliesOverOcean && isOceanBiome(lookX, lookZ);

        if (desiredDist > 0.0001) {
            desiredX = (desiredX / desiredDist) * flySpeed;
            desiredY = (desiredY / desiredDist) * flySpeed;
            desiredZ = (desiredZ / desiredDist) * flySpeed;

            double steerX = desiredX - this.xd;
            double steerY = (desiredY - this.yd) * verticalSteerFactor;
            double steerZ = desiredZ - this.zd;

            if (this.y >= absoluteCeiling - 0.5) {
                steerY -= 0.02 * verticalSteerFactor;
            } else {
                double heightDiff = targetHeight - this.y;
                if (Math.abs(heightDiff) > heightTolerance) {
                    boolean pullingUp = Math.signum(heightDiff) > 0;
                    if (!pullingUp || !isBlocked(this.x, this.y + 2.0, this.z)) {
                        steerY += Math.signum(heightDiff) * heightAdherence * verticalSteerFactor;
                    }
                }
            }

            double currentSteerStrength = steerStrength;
            if (blockAvoidance || waterAvoidance) {
                currentSteerStrength *= 4.0;
            }

            double steerMag = Math.sqrt(steerX * steerX + steerY * steerY + steerZ * steerZ);
            if (steerMag > currentSteerStrength) {
                steerX = (steerX / steerMag) * currentSteerStrength;
                steerY = (steerY / steerMag) * currentSteerStrength;
                steerZ = (steerZ / steerMag) * currentSteerStrength;
            }

            this.xd += steerX;
            this.yd += steerY;
            this.zd += steerZ;
        }

        // Apply speed limits
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

        boolean currentGoalInvalid = isBlocked(goalX, goalY, goalZ)
                || (!this.fliesOverOcean && isOceanBiome(goalX, goalZ));

        // Immediate path is blocked
        if (blockAvoidance || waterAvoidance) {
            this.xd = (this.xd * -0.3) + ((this.random.nextFloat() - 0.5f) * 0.08);
            this.zd = (this.zd * -0.3) + ((this.random.nextFloat() - 0.5f) * 0.08);

            double bounceSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            double maxBounceSpeed = 0.15;
            if (bounceSpeed > maxBounceSpeed) {
                this.xd = (this.xd / bounceSpeed) * maxBounceSpeed;
                this.zd = (this.zd / bounceSpeed) * maxBounceSpeed;
            }

            boolean ceilingDetected = isBlocked(this.x, this.y + 1.2, this.z)
                    || isBlocked(this.x, this.y + 0.5, this.z);
            boolean floorDetected = isBlocked(this.x, this.y - 1.2, this.z) || isBlocked(this.x, this.y - 0.5, this.z);

            if (ceilingDetected && floorDetected) {
                this.yd = (this.yd * -0.3) + ((this.random.nextFloat() - 0.5f) * 0.06);
                this.goalY = this.y + (this.random.nextFloat() - 0.5f);
            } else if (ceilingDetected) {
                this.yd = Math.min(this.yd * -0.4, -0.05) - (this.random.nextFloat() * 0.05);
                this.goalY = this.y - 1.5 - this.random.nextFloat();
                this.xd += (this.random.nextFloat() - 0.5f) * 0.4;
                this.zd += (this.random.nextFloat() - 0.5f) * 0.4;
            } else if (floorDetected) {
                this.yd = Math.max(this.yd * -0.4, 0.05) + (this.random.nextFloat() * 0.05);
                this.goalY = this.y + 1.5 + this.random.nextFloat();
            } else {
                this.yd = Math.max(this.yd, 0.08) + (this.random.nextFloat() * 0.05);
                this.goalY = this.y + 2.0 + this.random.nextFloat();
            }

            double pushX = Math.abs(this.xd) > 0.001 ? Math.signum(this.xd) : (this.random.nextFloat() - 0.5f);
            double pushZ = Math.abs(this.zd) > 0.001 ? Math.signum(this.zd) : (this.random.nextFloat() - 0.5f);

            this.goalX = this.x + (pushX * 5.0) + ((this.random.nextFloat() - 0.5f) * 4.0);
            this.goalZ = this.z + (pushZ * 5.0) + ((this.random.nextFloat() - 0.5f) * 4.0);

            this.goalTimer = 10 + this.random.nextInt(15);

        } else if (currentGoalInvalid) {
            chooseNewGoal();
            this.goalTimer = 20 + this.random.nextInt(20);
        }

        // Perching Scan
        int effectiveFlockSize = this.cachedFlockNeighbors.size() + 1;
        if (landingCooldown == 0 && this.random.nextFloat() < (this.perchingChance / effectiveFlockSize)) {
            for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                if (nb.state == State.PERCHED && nb.perchBlockPos != null) {
                    BlockPos target = nb.perchBlockPos;
                    if (target.getY() <= this.y) {
                        if (!level.getBlockState(target).isAir() && level.getBlockState(target.above()).isAir()) {
                            setState(this, State.LANDING);
                            this.landingBlockPos = target;
                            this.landingTargetY = target.getY() + 1.0 + this.quadSize;
                            this.landingOffsetX = (this.random.nextFloat() - 0.5f) * 0.8;
                            this.landingOffsetZ = (this.random.nextFloat() - 0.5f) * 0.8;
                            groupPerch(target);
                            return;
                        }
                    }
                }
            }

            int px = (int) Math.floor(this.x);
            int py = (int) Math.floor(this.y);
            int pz = (int) Math.floor(this.z);

            for (int i = 1; i <= this.perchingDistance; i++) {
                int checkY = py - i;
                mutablePos.set(px, checkY, pz);

                BlockState belowState = level.getBlockState(mutablePos);
                if (belowState.isAir())
                    continue;

                mutablePos.move(Direction.UP);
                if (!level.getBlockState(mutablePos).isAir())
                    continue;

                mutablePos.move(Direction.DOWN);
                if (!belowState.isFaceSturdy(level, mutablePos, Direction.UP))
                    continue;
                if (belowState.getCollisionShape(level, mutablePos).isEmpty())
                    continue;

                // Neighbor check reusing mutablePos entirely
                boolean hasNeighbor = !level.isEmptyBlock(mutablePos.set(px, checkY, pz - 1)) ||
                        !level.isEmptyBlock(mutablePos.set(px, checkY, pz + 1)) ||
                        !level.isEmptyBlock(mutablePos.set(px - 1, checkY, pz)) ||
                        !level.isEmptyBlock(mutablePos.set(px + 1, checkY, pz));

                if (!hasNeighbor)
                    continue;

                setState(this, State.LANDING);
                this.landingBlockPos = mutablePos.set(px, checkY, pz).immutable();
                this.landingOffsetX = (this.random.nextFloat() - 0.5f) * 0.8;
                this.landingOffsetZ = (this.random.nextFloat() - 0.5f) * 0.8;
                this.landingTargetY = checkY + 1.0 + this.quadSize;
                break;
            }

            if (this.state == State.LANDING && this.landingBlockPos != null) {
                groupPerch(this.landingBlockPos);
            }
        }
    }

    private void tickLanding() {
        double scareRadiusSq = scareRadius * scareRadius;
        for (Player p : this.level.players()) {
            if (p.isSpectator())
                continue;

            double dx = p.getX() - this.x;
            double dz = p.getZ() - this.z;
            double distSq = dx * dx + dz * dz;
            double dy = Math.abs(p.getY() - this.y);

            if (distSq <= scareRadiusSq && dy < 3.0) {
                performTakeoff(p);
                this.landingDelay = 0;
                return;
            }
        }

        this.perchedTimer = 0;

        // If target missing, abort to flying
        if (this.landingBlockPos == null || Double.isNaN(this.landingTargetY)) {
            setState(this, State.FLYING);
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
        double verticalDist = Math.max(0.0, this.y - this.landingTargetY);
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

        // Prevent clipping below the target height
        if (this.y <= this.landingTargetY) {
            this.yd = 0;
            this.y = this.landingTargetY;
        } else {
            double descent = Math.min(0.20, Math.max(0.06, verticalDist * 0.03));
            if (this.y - descent < this.landingTargetY) {
                this.yd = -(this.y - this.landingTargetY);
            } else {
                this.yd = -descent;
            }
        }

        double dx = targetX - this.x;
        double dz = targetZ - this.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double horizSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);

        // Snap if close and slow
        if (horizDist < 0.35 && Math.abs(this.y - this.landingTargetY) <= 0.01 && horizSpeed < 0.06) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos).isAir()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                setState(this, State.PERCHED);
                this.perchTimer = this.perchingTime + (int) (this.random.nextFloat() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
            } else {
                setState(this, State.FLYING);
            }
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            return;
        }

        // Finalize if we reach the landing Y and are reasonably close horizontally
        if (this.y <= this.landingTargetY + 0.05 && horizDist < 0.6) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos).isAir()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                setState(this, State.PERCHED);
                this.perchTimer = this.perchingTime + (int) (this.random.nextFloat() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
            } else {
                setState(this, State.FLYING);
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

        if (this.random.nextFloat() < 0.05f) {
            this.setSpriteName((1 + (int) (this.random.nextFloat() * 2)));
        }

        if (this.perchBlockPos != null && level.getBlockState(this.perchBlockPos).isAir()) {
            performTakeoff(null);
            return;
        }

        Player threateningPlayer = null;
        double closestDistSq = scareRadius * scareRadius;

        for (Player p : this.level.players()) {
            if (p.isSpectator())
                continue;

            double dx = p.getX() - this.x;
            double dz = p.getZ() - this.z;
            double distSq = dx * dx + dz * dz;
            double dy = Math.abs(p.getY() - this.y);

            if (distSq <= closestDistSq && dy < 3.0) {
                closestDistSq = distSq;
                threateningPlayer = p;
            }
        }

        if (threateningPlayer != null) {
            boolean amIClosest = true;
            for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                if (nb.state == State.PERCHED) {
                    double dx = nb.x - threateningPlayer.getX();
                    double dz = nb.z - threateningPlayer.getZ();
                    if ((dx * dx + dz * dz) < closestDistSq) {
                        amIClosest = false;
                        break;
                    }
                }
            }

            if (amIClosest) {
                this.perchTimer = 0;
            } else {
                int waveDelay = (int) (Math.sqrt(closestDistSq) * 2.0);
                if (this.perchTimer > waveDelay) {
                    this.perchTimer = waveDelay;
                }
            }
        }

        if (perchTimer-- <= 0) {
            if (threateningPlayer != null) {
                performTakeoff(threateningPlayer);
            } else {
                setState(this, State.TAKING_OFF);
                this.perchTimer = 20;
                groupTakeoff();
            }
        }
    }

    private void tickTakingOff() {
        this.landingTargetY = Double.NaN;
        this.landingBlockPos = null;
        this.landingOffsetX = 0.0;
        this.landingOffsetZ = 0.0;
        this.landingDelay = 0;
        this.perchBlockPos = null;

        if (this.takeoffTime == 0) {
            this.xd += (this.random.nextFloat() - 0.5f) * 0.08;
            this.zd += (this.random.nextFloat() - 0.5f) * 0.08;
        }
        this.takeoffTime++;

        if (Double.isNaN(this.takeoffGoalY)) {
            this.takeoffGoalY = this.y + 1.0 + this.random.nextDouble() * 0.8;
        }

        double remaining = this.takeoffGoalY - this.y;
        double desiredUp = 0.02 + Math.min(maxVerticalSpeed, Math.max(0.06, remaining * 0.12));
        this.yd += (desiredUp - this.yd) * 0.18;
        this.xd *= 0.995;
        this.zd *= 0.995;

        boolean pathBlocked = isBlocked(this.x + this.xd, this.y + this.yd + 0.5, this.z + this.zd)
                || isBlocked(this.x, this.y + 1.2, this.z);

        boolean takeoffComplete = (perchTimer-- <= 0 && (this.y >= this.takeoffGoalY - 0.15 || this.takeoffTime > 50));

        if (pathBlocked || takeoffComplete) {
            setState(this, State.FLYING);
            this.landingCooldown = 300 + this.random.nextInt(400);

            this.goalX = this.x + (this.xd * 15.0);
            this.goalY = this.y + (this.yd * 15.0);
            this.goalZ = this.z + (this.zd * 15.0);
            this.goalTimer = this.random.nextInt(20);

            this.takeoffGoalY = Double.NaN;
            this.takeoffTime = 0;
        }
    }

    private void tickDying() {
        this.yd -= 0.02;
        if (this.y < -64 || this.age > this.lifetime + 240) {
            if (debugTextBirds) {
                AtmosphericFauna.LOGGER
                        .info("Bird particle removed due to age or falling out of world: " + this.baseSpriteName);
            }
            this.remove();
        }
    }

    // MARK: --- SPRITE HANDLING ---

    protected void setSpriteName(Integer frame) {
        if (this.baseSpriteName == null) {
            return;
        }

        StringBuilder builder = new StringBuilder(this.baseSpriteName);
        if (this.state == State.PERCHED) {
            builder.append("_perched");
        } else {
            builder.append("_flying");
        }

        if (this.facingRight) {
            builder.append("_r");
        }

        builder.append("_").append(frame == null ? "1" : frame);

        this.spriteName = builder.toString();
        this.setSprite(getSprite(this.spriteName));
    }

    private int getFrameFromSpriteName(String spriteName) {
        if (spriteName == null || spriteName.isEmpty()) {
            return 1;
        }

        int idx = spriteName.length() - 1;
        while (idx >= 0 && Character.isDigit(spriteName.charAt(idx))) {
            idx--;
        }

        String num = spriteName.substring(idx + 1);
        if (!num.isEmpty()) {
            try {
                return Integer.parseInt(num);
            } catch (NumberFormatException ignored) {
                // fall back to frame 1
            }
        }

        return 1;
    }

    private boolean shouldFaceRightRelativeToCamera() {
        double horizSpeedSq = this.xd * this.xd + this.zd * this.zd;
        if (horizSpeedSq <= 0.0001) {
            return this.facingRight;
        }

        if (mc.player == null) {
            return this.facingRight;
        }

        double dx = this.x - mc.player.getX();
        double dz = this.z - mc.player.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq < 0.0001) {
            return this.facingRight;
        }

        double dist = Math.sqrt(distSq);
        double nx = dx / dist;
        double nz = dz / dist;

        double perpendicularSpeed = (this.zd * nx) - (this.xd * nz);

        if (Math.abs(perpendicularSpeed) < 0.01) {
            return this.facingRight;
        }

        return perpendicularSpeed > 0.0;
    }

    private void updateSpriteFacing() {
        this.facingRight = shouldFaceRightRelativeToCamera();
        setSpriteName(getFrameFromSpriteName(this.spriteName));
    }
}
