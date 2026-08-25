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
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

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
    protected int peckTimer = 0;
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
    protected Double threatX = null;
    protected Double threatZ = null;
    protected double lastDistSqToGoal = -1.0;
    protected int stuckTicks = 0;
    protected double cachedGroundHeight = Double.NaN;
    protected double cachedDirX = 1.0;
    protected double cachedDirZ = 0.0;
    protected int groundScanTimer = 0;
    protected double heightAdherence = 0.0015; // How strongly the bird pulls back to its target height
    protected double heightTolerance = 8.0; // How far (in blocks) they can drift before they start caring

    protected final BirdAnimator animator;
    protected final EnvironmentScanner env;
    protected String baseSpriteName = null;
    protected String spriteName = null;
    protected boolean facingRight = false;

    public static final List<BaseBirdParticle> ALL_BIRDS = Collections.synchronizedList(new ArrayList<>());
    public static final List<List<BaseBirdParticle>> SPECIES_REGISTRY = new ArrayList<>();
    private final List<BaseBirdParticle> reusableNeighborList = new ArrayList<>();
    private final List<BaseBirdParticle> cachedFlockNeighbors = new ArrayList<>();
    private int neighborCacheTimer = 0;

    private static final Map<List<BaseBirdParticle>, SpatialGrid> SPECIES_GRIDS = new IdentityHashMap<>();
    private long currentCellKey = 0L;
    private boolean inGrid = false;

    private static final class SpatialGrid {
        final Map<Long, ArrayList<BaseBirdParticle>> cells = new HashMap<>();
        double cellSize;
    }

    private static long cellKey(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (long) (z & 0x1FFFFF);
    }

    protected static Minecraft mc = Minecraft.getInstance();

    // --- VARIABLES ---

    protected int tickOffset;
    private static float nextFlockSpeedOffset = 0.0f;
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
        this.animator = new BirdAnimator(this);
        this.env = new EnvironmentScanner(level);
        this.tickOffset = this.random.nextInt(100);
        this.groundScanTimer = this.random.nextInt(10);
        this.neighborCacheTimer = this.random.nextInt(20);
        if (this.removed)
            return;
        ALL_BIRDS.add(this);
        getSpeciesList().add(this);
    }

    // MARK: --- HELPER METHODS ---

    protected abstract List<BaseBirdParticle> getSpeciesList();

    public abstract int getSpeciesMaxCount();

    public static void reset() {
        ALL_BIRDS.clear();
        synchronized (SPECIES_REGISTRY) {
            for (int i = 0; i < SPECIES_REGISTRY.size(); i++) {
                SPECIES_REGISTRY.get(i).clear();
            }
        }
        SPECIES_GRIDS.clear();
    }

    @Override
    public void remove() {
        ALL_BIRDS.remove(this);
        getSpeciesList().remove(this);
        unregisterFromGrid();
        super.remove();
    }

    public void killSilently() {
        super.remove();
    }

    public static int clearAllParticles() {
        int count = ALL_BIRDS.size();
        for (BaseBirdParticle bird : ALL_BIRDS) {
            bird.killSilently();
        }
        reset();
        return count;
    }

    public static List<BaseBirdParticle> getAllBirds() {
        ALL_BIRDS.removeIf(p -> p.removed);
        synchronized (SPECIES_REGISTRY) {
            for (int i = 0; i < SPECIES_REGISTRY.size(); i++) {
                SPECIES_REGISTRY.get(i).removeIf(p -> p.removed);
            }
        }
        return ALL_BIRDS;
    }

    public static int getMaxActiveBirds() {
        return maxActiveBirds;
    }

    public static void setNextFlockSpeedOffset(float offset) {
        nextFlockSpeedOffset = offset;
    }

    public static float getNextFlockSpeedOffset() {
        return nextFlockSpeedOffset;
    }

    public String getBaseSpriteName() {
        return this.baseSpriteName;
    }

    public void applySprite(String name) {
        this.setSprite(BaseParticle.getSprite(name));
    }

    @Override
    protected float getU0() {
        return this.animator.isFacingRight() ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return this.animator.isFacingRight() ? super.getU0() : super.getU1();
    }

    private static void setState(BaseBirdParticle bird, State newState) {
        bird.state = newState;
        bird.animator.updateSprite(1, newState == State.PERCHED);
    }

    // MARK: --- TICK ---

    @Override
    public void tick() {
        if (this.age == 0 && this.yd >= 999.0) {
            this.y += this.random.nextFloat() * (this.preferredFlightHeight * 2.0);

            this.goalX = this.x + this.xd;
            this.goalY = this.y + (this.random.nextFloat() - 0.5f) * 2.0;
            this.goalZ = this.z + this.zd;
            this.goalTimer = this.goalDurationMax * 2;

            double mag = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            if (mag > 0.001) {
                this.xd = (this.xd / mag) * flySpeed;
                this.zd = (this.zd / mag) * flySpeed;
            }
            this.yd = (this.random.nextFloat() - 0.5f) * 0.1;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        updateGridPosition();

        if (Math.abs(this.xd) > 0.001 || Math.abs(this.zd) > 0.001) {
            this.cachedDirX = this.xd;
            this.cachedDirZ = this.zd;
        }

        float distanceAlpha = 1.0f;
        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(this.x, this.y, this.z);
            int renderDist = mc.options.renderDistance().get();
            double renderEdgeDist = renderDist * 16.0;
            double despawnDist = renderEdgeDist + 16.0;

            if (distSq > despawnDist * despawnDist) {
                for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                    if (!nb.removed) {
                        nb.remove();
                    }
                }
                this.remove();
                return;
            }

            // Birds in the grace distance remain active but are always invisible.
            if (distSq > renderEdgeDist * renderEdgeDist) {
                distanceAlpha = 0.0f;
            } else if (enableBirdDistanceFadeOut) {
                double fadeStartDist = renderEdgeDist * 0.8;
                if (distSq > fadeStartDist * fadeStartDist) {
                    double dist = Math.sqrt(distSq);
                    double t = (dist - fadeStartDist) / (renderEdgeDist - fadeStartDist);
                    distanceAlpha = (float) Math.max(0.0, 1.0 - t);
                }
            }
        }

        // Fade in on spawn to match, and let the engine's own fog shader haze the sprite at range
        float ageAlpha = Math.min(1.0f, this.age / 10.0f);
        this.alpha = Math.min(distanceAlpha, ageAlpha);

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
        if (state != State.DYING && this.baseSpriteName != null) {
            if (state != State.PERCHED) {
                int flapAdjustment = (int) (this.yd * 10);
                int effectiveFlapSpeed = Math.max(1, this.wingFlapSpeed - flapAdjustment);
                if ((this.age - this.wingFlapOffset) % effectiveFlapSpeed == 0) {
                    int frame = this.animator.getCurrentFrame();
                    this.animator.updateSprite(frame == 1 ? 2 : 1, false);
                }
            }

            if ((this.age + this.tickOffset) % 3 == 0) {
                this.animator.updateFacingDirection(state == State.PERCHED, this.x, this.z, this.cachedDirX,
                        this.cachedDirZ);
            }
        }

        // Debug
        if (debugTextBirds) {
            if ((this.age + this.tickOffset) % 10 == 0) {
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

        SpatialGrid grid = SPECIES_GRIDS.get(getSpeciesList());
        if (grid == null)
            return reusableNeighborList;

        double cs = grid.cellSize;
        int cellRange = (int) Math.ceil(radius / cs);
        if (cellRange < 1)
            cellRange = 1;

        int cx = (int) Math.floor(this.x / cs);
        int cy = (int) Math.floor(this.y / cs);
        int cz = (int) Math.floor(this.z / cs);

        for (int dx = -cellRange; dx <= cellRange; dx++) {
            for (int dy = -cellRange; dy <= cellRange; dy++) {
                for (int dz = -cellRange; dz <= cellRange; dz++) {
                    long key = cellKey(cx + dx, cy + dy, cz + dz);
                    ArrayList<BaseBirdParticle> cell = grid.cells.get(key);
                    if (cell == null)
                        continue;
                    int cellSize = cell.size();
                    for (int j = 0; j < cellSize; j++) {
                        BaseBirdParticle other = cell.get(j);
                        if (other == this || other.removed)
                            continue;

                        // Fast fail bounds check (avoids Math.abs overhead)
                        double ddx = other.x - this.x;
                        if (ddx > radius || ddx < -radius)
                            continue;
                        double ddy = other.y - this.y;
                        if (ddy > radius || ddy < -radius)
                            continue;
                        double ddz = other.z - this.z;
                        if (ddz > radius || ddz < -radius)
                            continue;

                        if (ddx * ddx + ddy * ddy + ddz * ddz <= rsq) {
                            reusableNeighborList.add(other);

                            /*
                             * Huge fps boost, but causes issues with flocking behavior if the flock is
                             * too large
                             * if (reusableNeighborList.size() >= 99) {
                             * return reusableNeighborList;
                             * }
                             */
                        }
                    }
                }
            }
        }
        return reusableNeighborList;
    }

    // Grid management
    private SpatialGrid getOrCreateGrid() {
        List<BaseBirdParticle> species = getSpeciesList();
        SpatialGrid grid = SPECIES_GRIDS.get(species);
        if (grid == null) {
            grid = new SpatialGrid();
            grid.cellSize = this.flockRadius;
            SPECIES_GRIDS.put(species, grid);
        }
        return grid;
    }

    private void registerInGrid() {
        if (inGrid)
            return;
        if (this.flockRadius <= 0.0)
            return;
        SpatialGrid grid = getOrCreateGrid();
        int cx = (int) Math.floor(this.x / grid.cellSize);
        int cy = (int) Math.floor(this.y / grid.cellSize);
        int cz = (int) Math.floor(this.z / grid.cellSize);
        long key = cellKey(cx, cy, cz);
        currentCellKey = key;
        inGrid = true;
        ArrayList<BaseBirdParticle> cell = grid.cells.get(key);
        if (cell == null) {
            cell = new ArrayList<>(4);
            grid.cells.put(key, cell);
        }
        cell.add(this);
    }

    private void unregisterFromGrid() {
        if (!inGrid)
            return;
        SpatialGrid grid = SPECIES_GRIDS.get(getSpeciesList());
        if (grid != null) {
            ArrayList<BaseBirdParticle> cell = grid.cells.get(currentCellKey);
            if (cell != null) {
                cell.remove(this);
                if (cell.isEmpty())
                    grid.cells.remove(currentCellKey);
            }
        }
        inGrid = false;
    }

    private void updateGridPosition() {
        if (!inGrid) {
            registerInGrid();
            return;
        }
        SpatialGrid grid = SPECIES_GRIDS.get(getSpeciesList());
        if (grid == null)
            return;
        int cx = (int) Math.floor(this.x / grid.cellSize);
        int cy = (int) Math.floor(this.y / grid.cellSize);
        int cz = (int) Math.floor(this.z / grid.cellSize);
        long newKey = cellKey(cx, cy, cz);
        if (newKey == currentCellKey)
            return;

        ArrayList<BaseBirdParticle> oldCell = grid.cells.get(currentCellKey);
        if (oldCell != null) {
            oldCell.remove(this);
            if (oldCell.isEmpty())
                grid.cells.remove(currentCellKey);
        }
        ArrayList<BaseBirdParticle> newCell = grid.cells.get(newKey);
        if (newCell == null) {
            newCell = new ArrayList<>(4);
            grid.cells.put(newKey, newCell);
        }
        newCell.add(this);
        currentCellKey = newKey;
    }

    // Ask nearby flockmates to randomly scatter and land around the flock's center
    private void groupPerch(BlockPos target) {
        if (target == null)
            return;

        List<BaseBirdParticle> flyingNeighbors = new ArrayList<>();
        Set<BaseBirdParticle> visited = new HashSet<>();
        List<BaseBirdParticle> queue = new ArrayList<>();

        visited.add(this);
        queue.add(this);

        int head = 0;
        while (head < queue.size()) {
            BaseBirdParticle current = queue.get(head++);

            for (BaseBirdParticle nb : current.cachedFlockNeighbors) {
                if (nb != null && !nb.removed && nb.state == State.FLYING && visited.add(nb)) {
                    flyingNeighbors.add(nb);
                    queue.add(nb);
                }
            }
        }

        if (flyingNeighbors.isEmpty())
            return;

        double cx = this.x;
        double cz = this.z;

        for (BaseBirdParticle nb : flyingNeighbors) {
            cx += nb.x;
            cz += nb.z;
        }

        cx /= (flyingNeighbors.size() + 1);
        cz /= (flyingNeighbors.size() + 1);

        Map<BlockPos, Integer> activeCounts = new HashMap<>();

        for (BaseBirdParticle nb : flyingNeighbors) {
            BlockPos chosenTarget = null;

            for (int attempts = 0; attempts < 10; attempts++) {
                int dx = (int) (this.random.nextGaussian() * 2.0);
                int dz = (int) (this.random.nextGaussian() * 2.0);
                int scanX = (int) cx + dx;
                int scanZ = (int) cz + dz;
                int scanY = (int) Math.floor(nb.y);

                BlockPos candidate = null;
                for (int dy = 0; dy >= -10; dy--) {
                    mutablePos.set(scanX, scanY + dy, scanZ);
                    BlockState state = level.getBlockState(mutablePos);
                    VoxelShape colShape = state.getCollisionShape(level, mutablePos);

                    if (!colShape.isEmpty()) {
                        mutablePos.move(Direction.UP);
                        BlockState aboveState = level.getBlockState(mutablePos);
                        if (aboveState.getCollisionShape(level, mutablePos).isEmpty()
                                && aboveState.getFluidState().isEmpty()) {
                            mutablePos.move(Direction.DOWN);
                            candidate = mutablePos.immutable();
                        }
                        break;
                    }
                }

                if (candidate != null) {
                    // Check if the block is too crowded and has line of sight
                    if (env.hasLineOfSight(nb.x, nb.y, nb.z, candidate.getX() + 0.5, candidate.getY() + 1.0,
                            candidate.getZ() + 0.5)) {
                        int currentCount = nb.countBirdsOnPerch(candidate) + activeCounts.getOrDefault(candidate, 0);
                        if (acceptsCrowd(currentCount)) {
                            chosenTarget = candidate;
                            break;
                        }
                    }
                }
            }

            // Fallback to the original bird's target if we couldn't find a unique spot
            if (chosenTarget == null) {
                int fallbackCount = nb.countBirdsOnPerch(target) + activeCounts.getOrDefault(target, 0);
                if (acceptsCrowd(fallbackCount)) {
                    chosenTarget = target;
                }
            }

            if (chosenTarget != null && chosenTarget.getY() <= nb.y) {
                activeCounts.put(chosenTarget, activeCounts.getOrDefault(chosenTarget, 0) + 1);

                nb.landingDelay = 10 + this.random.nextInt(35);
                nb.landingBlockPos = chosenTarget;
                nb.stuckTicks = 0;

                BlockState targetState = level.getBlockState(chosenTarget);
                VoxelShape visualShape = targetState.getShape(level, chosenTarget);
                double blockHeight = visualShape.isEmpty() ? 1.0 : visualShape.max(Direction.Axis.Y);

                BlockPos abovePos = chosenTarget.above();
                VoxelShape aboveVisual = level.getBlockState(abovePos).getShape(level, abovePos);
                if (!aboveVisual.isEmpty()) {
                    double aboveHeight = aboveVisual.max(Direction.Axis.Y);
                    if (aboveHeight <= 0.5) {
                        blockHeight += aboveHeight;
                    }
                }

                nb.landingTargetY = chosenTarget.getY() + blockHeight + nb.quadSize;
                setLandingOffsets(nb, visualShape);
            }
        }
    }

    // Ask nearby flockmates to take off with this bird, abort landings, or keep
    // flying
    private void groupTakeoff() {
        List<BaseBirdParticle> connectedFlock = new ArrayList<>();
        Set<BaseBirdParticle> visited = new HashSet<>();
        List<BaseBirdParticle> queue = new ArrayList<>();

        visited.add(this);
        queue.add(this);

        int head = 0;
        while (head < queue.size()) {
            BaseBirdParticle current = queue.get(head++);
            for (BaseBirdParticle nb : current.cachedFlockNeighbors) {
                if (nb != null && !nb.removed && visited.add(nb)) {
                    connectedFlock.add(nb);
                    queue.add(nb);
                }
            }
        }

        for (BaseBirdParticle nb : connectedFlock) {
            if (nb == this)
                continue;

            if (nb.state == State.PERCHED) {
                if (nb.perchTimer > 30) {
                    nb.perchTimer = 5 + this.random.nextInt(25);
                }
            } else if (nb.state == State.LANDING) {
                setState(nb, State.FLYING);
                nb.landingBlockPos = null;
                nb.landingTargetY = Double.NaN;
                nb.landingOffsetX = 0.0;
                nb.landingOffsetZ = 0.0;
                nb.landingDelay = 0;
                nb.landingCooldown = 200 + this.random.nextInt(400);

                nb.goalX = nb.x + nb.xd * 10.0;
                nb.goalY = nb.y + 3.0 + nb.random.nextFloat() * 2.0;
                nb.goalZ = nb.z + nb.zd * 10.0;
                nb.goalTimer = 20 + nb.random.nextInt(20);
            } else if (nb.state == State.FLYING) {
                if (nb.landingCooldown < 150) {
                    nb.landingCooldown = 200 + this.random.nextInt(400);
                }
                if (nb.goalTimer > 20) {
                    nb.goalTimer = nb.random.nextInt(10);
                }
            }
        }
    }

    // Takeoff logic
    private void performTakeoff(Double sourceX, Double sourceZ) {
        if (sourceX != null && sourceZ != null) {
            double dx = this.x - sourceX;
            double dz = this.z - sourceZ;
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

    // Calculates a random landing spot
    private void setLandingOffsets(BaseBirdParticle bird, VoxelShape shape) {
        if (shape.isEmpty()) {
            bird.landingOffsetX = (this.random.nextFloat() - 0.5f) * 0.8;
            bird.landingOffsetZ = (this.random.nextFloat() - 0.5f) * 0.8;
            return;
        }

        double minX = shape.min(Direction.Axis.X);
        double maxX = shape.max(Direction.Axis.X);
        double minZ = shape.min(Direction.Axis.Z);
        double maxZ = shape.max(Direction.Axis.Z);

        double centerX = (minX + maxX) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        double margin = 0.15;
        double width = (maxX - minX) - (margin * 2);
        double depth = (maxZ - minZ) - (margin * 2);

        if (width > 0) {
            bird.landingOffsetX = (centerX - 0.5) + (this.random.nextFloat() - 0.5) * width;
        } else {
            bird.landingOffsetX = centerX - 0.5;
        }

        if (depth > 0) {
            bird.landingOffsetZ = (centerZ - 0.5) + (this.random.nextFloat() - 0.5) * depth;
        } else {
            bird.landingOffsetZ = centerZ - 0.5;
        }
    }

    // Determines if a block is too crowded using a diminishing probability curve
    private boolean acceptsCrowd(int count) {
        if (count <= 1)
            return true;
        if (count == 2)
            return this.random.nextFloat() < 0.90f;
        if (count == 3)
            return this.random.nextFloat() < 0.70f;
        if (count == 4)
            return this.random.nextFloat() < 0.30f;
        if (count == 5)
            return this.random.nextFloat() < 0.10f;
        if (count == 6)
            return this.random.nextFloat() < 0.02f;
        return this.random.nextFloat() < 0.005f;
    }

    // Counts how many nearby flockmates are targeting or sitting on a specific
    // block
    private int countBirdsOnPerch(BlockPos pos) {
        int count = 0;
        if (pos.equals(this.landingBlockPos) || pos.equals(this.perchBlockPos))
            count++;
        for (BaseBirdParticle b : this.cachedFlockNeighbors) {
            if (pos.equals(b.landingBlockPos) || pos.equals(b.perchBlockPos)) {
                count++;
            }
        }
        return count;
    }

    // Pick a new wandering goal
    private void chooseNewGoal() {
        double randRadius = 2.5 + this.random.nextFloat() * (goalRadius - 2.5);
        double angle = this.random.nextFloat() * Math.PI * 2;
        double nx = Math.cos(angle) * randRadius + this.xd * 5.0 * (this.random.nextFloat() - 0.5f);
        double nz = Math.sin(angle) * randRadius + this.zd * 5.0 * (this.random.nextFloat() - 0.5f);

        // Ocean avoidance
        if (!this.fliesOverOcean) {
            int attempts = 0;
            while (env.isOceanBiome(this.x + nx, this.z + nz) && attempts < 15) {
                attempts++;
                if (attempts % 3 == 0)
                    randRadius += 15.0;

                angle = this.random.nextFloat() * Math.PI * 2;
                nx = Math.cos(angle) * randRadius;
                nz = Math.sin(angle) * randRadius;
            }
        }

        double ground = this.cachedGroundHeight;
        boolean isVoid = ground <= env.getLevelMinY() + 1.0;

        double absoluteCeiling = (env.getLevelMinY() + level.getHeight()) - 5.0;
        double targetHeight = isVoid ? this.y : Math.min(ground + preferredFlightHeight, absoluteCeiling);
        double ny;

        if (isVoid) {
            ny = this.y + (this.random.nextFloat() - 0.5f) * 15.0;
        } else if (this.y <= ground + minFlightHeight + 0.5) {
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

        double safeFloor = isVoid ? env.getLevelMinY() + 10.0 : env.getLevelMinY() + 1.0;
        ny = Math.max(safeFloor, Math.min(absoluteCeiling, ny));

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

    // MARK: --- TICK FLYING ---

    private void tickFlying() {
        if (this.landingDelay > 0) {
            this.landingDelay--;
            if (this.landingDelay <= 0 && this.landingBlockPos != null) {
                setState(this, State.LANDING);
                return;
            }
        }

        if (this.groundScanTimer-- <= 0 || Double.isNaN(this.cachedGroundHeight)) {
            this.cachedGroundHeight = env.sampleGroundHeight(this.x, this.y, this.z);
            this.groundScanTimer = 10;
        }

        double groundY = this.cachedGroundHeight;
        boolean isVoid = groundY <= env.getLevelMinY() + 1.0;

        double absoluteCeiling = (env.getLevelMinY() + level.getHeight()) - 5.0;

        double targetHeight = isVoid ? this.y : Math.min(groundY + preferredFlightHeight, absoluteCeiling);

        handleStuckDetection();

        if (neighborCacheTimer-- <= 0 || cachedFlockNeighbors.isEmpty()) {
            neighborCacheTimer = 2 + this.random.nextInt(3);
            cachedFlockNeighbors.clear();
            cachedFlockNeighbors.addAll(getNeighbors(flockRadius));
        }

        applyFlockingBehavior(groundY);

        if (!isVoid && this.y <= groundY + minFlightHeight + 0.3) {
            this.goalY = Math.max(this.goalY, this.y + takeoffClimb + this.random.nextFloat() * 1.5);
            this.goalTimer = Math.max(this.goalTimer, 20);
        }

        if (isVoid && this.y <= env.getLevelMinY() + 5.0) {
            this.goalY = Math.max(this.goalY, this.y + 10.0 + this.random.nextFloat() * 5.0);
            this.goalTimer = Math.max(this.goalTimer, 40);
        }

        if (this.y >= absoluteCeiling - 0.5) {
            this.goalY = Math.min(this.goalY, absoluteCeiling - 2.0 - this.random.nextFloat() * 2.0);
            this.goalTimer = Math.min(this.goalTimer, 40);
        }

        double lookX = this.x + this.xd * lookAheadMultiplier;
        double lookY = this.y + this.yd * lookAheadMultiplier;
        double lookZ = this.z + this.zd * lookAheadMultiplier;

        boolean blockAvoidance = env.isBlocked(lookX, lookY, lookZ);
        boolean waterAvoidance = !this.fliesOverOcean && env.isOceanBiome(lookX, lookZ);

        applyDesiredVector(targetHeight, absoluteCeiling, blockAvoidance, waterAvoidance);

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

        handleBlockAvoidance(blockAvoidance, waterAvoidance);

        scanForPerch();
    }

    // Stuck detection
    private void handleStuckDetection() {
        double dxToGoal = Double.isNaN(goalX) ? Double.POSITIVE_INFINITY : (goalX - this.x);
        double dyToGoal = Double.isNaN(goalY) ? Double.POSITIVE_INFINITY : (goalY - this.y);
        double dzToGoal = Double.isNaN(goalZ) ? Double.POSITIVE_INFINITY : (goalZ - this.z);
        double distSqToGoal = dxToGoal * dxToGoal + dyToGoal * dyToGoal + dzToGoal * dzToGoal;

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
    }

    // Flocking behavior
    private void applyFlockingBehavior(double groundY) {
        List<BaseBirdParticle> neighbors = this.cachedFlockNeighbors;
        boolean isScattering = this.flockCooldown > 0;

        if (neighbors.isEmpty())
            return;

        double sepDist = separationDistance;
        double sepDistSq = sepDist * sepDist;
        double invSepDist = 1.0 / sepDist;
        boolean checkOcean = !this.fliesOverOcean;
        double thisX = this.x;
        double thisY = this.y;
        double thisZ = this.z;

        double cx = 0, cy = 0, cz = 0;
        double avx = 0, avy = 0, avz = 0;
        double sepX = 0, sepY = 0, sepZ = 0;
        int flockingCount = 0;

        BaseBirdParticle leader = this;
        int leaderHash = leader.hashCode();

        int n = neighbors.size();
        for (int i = 0; i < n; i++) {
            BaseBirdParticle nb = neighbors.get(i);
            if (nb.state != State.FLYING)
                continue;

            int nbHash = nb.hashCode();
            if (nbHash < leaderHash) {
                leader = nb;
                leaderHash = nbHash;
            }

            double dx = thisX - nb.x;
            double dy = thisY - nb.y;
            double dz = thisZ - nb.z;
            double d2 = dx * dx + dy * dy + dz * dz;

            if (d2 <= sepDistSq) {
                if (d2 > 0.0) {
                    double d = Math.sqrt(d2);
                    double invD = 1.0 / d;
                    double factor = (sepDist - d) * invSepDist;
                    sepX += dx * invD * factor;
                    sepY += dy * invD * factor;
                    sepZ += dz * invD * factor;
                } else {
                    double angle = this.random.nextFloat() * Math.PI * 2;
                    sepX += Math.cos(angle);
                    sepZ += Math.sin(angle);
                }
            }

            if (!isScattering && nb.flockCooldown == 0) {
                if (checkOcean && env.isOceanBiome(nb.x, nb.z))
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
                double angleAway = Math.atan2(thisZ - cz, thisX - cx);
                if (Double.isNaN(angleAway))
                    angleAway = this.random.nextFloat() * Math.PI * 2;

                this.goalX = thisX + Math.cos(angleAway) * (goalRadius * 1.5);
                this.goalY = Math.max(groundY + minFlightHeight, thisY + (this.random.nextFloat() - 0.2f) * 10.0);
                this.goalZ = thisZ + Math.sin(angleAway) * (goalRadius * 1.5);
                this.goalTimer = 80;
            } else {
                // Normal Alignment and Cohesion
                double aliX = (avx - this.xd) * (alignmentStrength * 1.6);
                double aliY = (avy - this.yd) * (alignmentStrength * 1.2);
                double aliZ = (avz - this.zd) * (alignmentStrength * 1.6);

                double currentCohesion = cohesionStrength * 0.45;
                double cohX = (cx - thisX) * currentCohesion;
                double cohY = (cy - thisY) * currentCohesion;
                double cohZ = (cz - thisZ) * currentCohesion;

                this.xd += cohX + aliX;
                this.yd += cohY + aliY;
                this.zd += cohZ + aliZ;

                // Leader was found during the main pass — no second loop needed.
                if (leader != this) {
                    double aheadFactor = 4.0;
                    double syncX = thisX + (leader.xd * aheadFactor) + (cx - thisX) * 0.18;
                    double syncY = thisY + (cy - thisY) * 0.12;
                    double syncZ = thisZ + (leader.zd * aheadFactor) + (cz - thisZ) * 0.18;

                    if (this.fliesOverOcean || !env.isOceanBiome(syncX, syncZ)) {
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

    // Desired vector towards the goal
    private void applyDesiredVector(double targetHeight, double absoluteCeiling, boolean blockAvoidance,
            boolean waterAvoidance) {
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

            if (this.y >= absoluteCeiling - 0.5) {
                steerY -= 0.02 * verticalSteerFactor;
            } else {
                double heightDiff = targetHeight - this.y;
                if (Math.abs(heightDiff) > heightTolerance) {
                    boolean pullingUp = Math.signum(heightDiff) > 0;
                    if (!pullingUp || !env.isBlocked(this.x, this.y + 2.0, this.z)) {
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
    }

    // Immediate path is blocked
    private void handleBlockAvoidance(boolean blockAvoidance, boolean waterAvoidance) {
        boolean currentGoalInvalid = env.isBlocked(goalX, goalY, goalZ)
                || (!this.fliesOverOcean && env.isOceanBiome(goalX, goalZ));

        if (blockAvoidance || waterAvoidance) {
            this.xd = (this.xd * -0.3) + ((this.random.nextFloat() - 0.5f) * 0.08);
            this.zd = (this.zd * -0.3) + ((this.random.nextFloat() - 0.5f) * 0.08);

            double bounceSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            double maxBounceSpeed = 0.15;
            if (bounceSpeed > maxBounceSpeed) {
                this.xd = (this.xd / bounceSpeed) * maxBounceSpeed;
                this.zd = (this.zd / bounceSpeed) * maxBounceSpeed;
            }

            boolean ceilingDetected = env.isBlocked(this.x, this.y + 1.2, this.z)
                    || env.isBlocked(this.x, this.y + 0.5, this.z);
            boolean floorDetected = env.isBlocked(this.x, this.y - 1.2, this.z)
                    || env.isBlocked(this.x, this.y - 0.5, this.z);

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
    }

    public static void onSoundPlayed(double x, double y, double z, float volume) {
        List<BaseBirdParticle> birdsToStartle;
        synchronized (ALL_BIRDS) {
            birdsToStartle = new ArrayList<>(ALL_BIRDS);
        }
        for (BaseBirdParticle bird : birdsToStartle) {
            if (!bird.removed && (bird.state == State.PERCHED || bird.state == State.LANDING)) {
                bird.startle(x, y, z, volume);
            }
        }
    }

    public void startle(double sx, double sy, double sz, float volume) {
        // Cap volume multiplier between 0.5x and 3.0x to prevent tiny/massive extremes
        double effectiveRadius = this.scareRadius * Math.max(0.5, Math.min(volume, 3.0));
        double dx = this.x - sx;
        double dy = this.y - sy;
        double dz = this.z - sz;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Math.abs(dy) < 5.0 prevents zombies in a deep cave from scaring a bird in a
        // tree above
        if (distSq <= effectiveRadius * effectiveRadius && Math.abs(dy) < 5.0) {
            if (this.state == State.LANDING) {
                this.performTakeoff(sx, sz);
                this.landingDelay = 0;
                return;
            }

            boolean amIClosest = true;
            for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                if (nb.state == State.PERCHED) {
                    double nbDistSq = (nb.x - sx) * (nb.x - sx) + (nb.y - sy) * (nb.y - sy) + (nb.z - sz) * (nb.z - sz);
                    if (nbDistSq < distSq) {
                        amIClosest = false;
                        break;
                    }
                }
            }

            this.threatX = sx;
            this.threatZ = sz;

            if (amIClosest) {
                this.perchTimer = 0;
            } else {
                int waveDelay = (int) (Math.sqrt(distSq) * 2.0);
                if (this.perchTimer > waveDelay) {
                    this.perchTimer = waveDelay;
                }
            }
        }
    }

    // Perching Scan
    private void scanForPerch() {
        int effectiveFlockSize = this.cachedFlockNeighbors.size() + 1;

        if (landingCooldown == 0) {
            double weatherMultiplier = (this.level.isRaining() || this.level.isThundering()) ? 4.0 : 1.0;
            double baseChance = (this.perchingChance * weatherMultiplier) / effectiveFlockSize;
            float roll = this.random.nextFloat();

            if (roll < baseChance * 2.0) {
                if (roll < baseChance) {
                    for (BaseBirdParticle nb : this.cachedFlockNeighbors) {
                        if (nb.state == State.PERCHED && nb.perchBlockPos != null) {
                            BlockPos target = nb.perchBlockPos;
                            if (target.getY() <= this.y) {
                                BlockState targetState = level.getBlockState(target);
                                BlockState aboveState = level.getBlockState(target.above());

                                if (!targetState.getCollisionShape(level, target).isEmpty() &&
                                        aboveState.getCollisionShape(level, target.above()).isEmpty() &&
                                        aboveState.getFluidState().isEmpty()) {

                                    if (acceptsCrowd(countBirdsOnPerch(target))) {
                                        setState(this, State.LANDING);
                                        this.stuckTicks = 0;
                                        this.landingBlockPos = target;

                                        VoxelShape visualShape = targetState.getShape(level, target);
                                        double blockHeight = visualShape.isEmpty() ? 1.0
                                                : visualShape.max(Direction.Axis.Y);

                                        BlockPos abovePos = target.above();
                                        VoxelShape aboveVisual = level.getBlockState(abovePos).getShape(level,
                                                abovePos);
                                        if (!aboveVisual.isEmpty()) {
                                            double aboveHeight = aboveVisual.max(Direction.Axis.Y);
                                            if (aboveHeight <= 0.5) {
                                                blockHeight += aboveHeight;
                                            }
                                        }

                                        this.landingTargetY = target.getY() + blockHeight + this.quadSize;
                                        setLandingOffsets(this, visualShape);
                                        groupPerch(target);
                                        return;
                                    }
                                }
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
                    VoxelShape belowCollision = belowState.getCollisionShape(level, mutablePos);

                    if (belowCollision.isEmpty())
                        continue;

                    mutablePos.move(Direction.UP);
                    BlockState aboveState = level.getBlockState(mutablePos);

                    if (!aboveState.getCollisionShape(level, mutablePos).isEmpty()
                            || !aboveState.getFluidState().isEmpty())
                        break;

                    mutablePos.move(Direction.DOWN);

                    boolean hasNeighbor = !level.isEmptyBlock(mutablePos.set(px, checkY, pz - 1)) ||
                            !level.isEmptyBlock(mutablePos.set(px, checkY, pz + 1)) ||
                            !level.isEmptyBlock(mutablePos.set(px - 1, checkY, pz)) ||
                            !level.isEmptyBlock(mutablePos.set(px + 1, checkY, pz));

                    if (!hasNeighbor)
                        break;

                    BlockPos candidate = mutablePos.set(px, checkY, pz).immutable();

                    if (!acceptsCrowd(countBirdsOnPerch(candidate))) {
                        break;
                    }

                    if (!env.hasLineOfSight(this.x, this.y, this.z, candidate.getX() + 0.5, checkY + 1.0,
                            candidate.getZ() + 0.5)) {
                        break;
                    }

                    boolean isInteresting = false;
                    if (belowState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        isInteresting = true;
                    } else {
                        double minX = belowCollision.min(Direction.Axis.X);
                        double maxX = belowCollision.max(Direction.Axis.X);
                        double minZ = belowCollision.min(Direction.Axis.Z);
                        double maxZ = belowCollision.max(Direction.Axis.Z);

                        if (minX > 0.0 || maxX < 1.0 || minZ > 0.0 || maxZ < 1.0) {
                            isInteresting = true;
                        }
                    }

                    if (!isInteresting && roll >= baseChance) {
                        break;
                    }

                    setState(this, State.LANDING);
                    this.stuckTicks = 0;
                    this.landingBlockPos = candidate;
                    VoxelShape visualShape = belowState.getShape(level, mutablePos);
                    setLandingOffsets(this, visualShape);
                    double blockHeight = visualShape.isEmpty() ? 1.0 : visualShape.max(Direction.Axis.Y);

                    BlockPos abovePos = this.landingBlockPos.above();
                    VoxelShape aboveVisual = level.getBlockState(abovePos).getShape(level, abovePos);
                    if (!aboveVisual.isEmpty()) {
                        double aboveHeight = aboveVisual.max(Direction.Axis.Y);
                        if (aboveHeight <= 0.5) {
                            blockHeight += aboveHeight;
                        }
                    }

                    this.landingTargetY = checkY + blockHeight + this.quadSize;
                    break;
                }

                if (this.state == State.LANDING && this.landingBlockPos != null) {
                    groupPerch(this.landingBlockPos);
                }
            }
        }
    }

    // Failsafe for players sneaking up without making noise
    private void checkProximityScare() {
        if (this.threatX != null)
            return;

        if ((this.age + this.tickOffset) % 5 != 0)
            return;

        for (Player p : this.level.players()) {
            if (p.isSpectator())
                continue;
            float simulatedVolume = p.isCrouching() ? 0.5f : 1.0f;
            this.startle(p.getX(), p.getY(), p.getZ(), simulatedVolume);
        }
    }

    // MARK: --- TICK LANDING ---

    private void tickLanding() {
        checkProximityScare();

        this.perchedTimer = 0;

        // If target missing, abort to flying
        if (this.landingBlockPos == null || Double.isNaN(this.landingTargetY)) {
            setState(this, State.FLYING);
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.landingOffsetX = 0.0;
            this.landingOffsetZ = 0.0;
            this.stuckTicks = 0;
            return;
        }

        double targetX = this.landingBlockPos.getX() + 0.5 + this.landingOffsetX;
        double targetZ = this.landingBlockPos.getZ() + 0.5 + this.landingOffsetZ;

        double dx = targetX - this.x;
        double dy = this.landingTargetY - this.y;
        double dz = targetZ - this.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        double lookX = this.x + this.xd * lookAheadMultiplier;
        double lookY = this.y + this.yd * lookAheadMultiplier;
        double lookZ = this.z + this.zd * lookAheadMultiplier;

        if (this.stuckTicks++ > 100) {
            setState(this, State.FLYING);
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.stuckTicks = 0;
            return;
        }

        // Check if there is an obstacle
        if (env.isBlocked(lookX, lookY, lookZ, this.landingBlockPos)) {
            this.yd = 0.15 + this.random.nextFloat() * 0.1;
            this.xd *= 0.7;
            this.zd *= 0.7;
        } else if (dist > 0.001) {
            double speedScale = Math.min(1.0, dist / 4.0);
            double currentFlySpeed = Math.max(0.04, this.flySpeed * speedScale);

            double desiredXd = (dx / dist) * currentFlySpeed;
            double desiredYd = (dy / dist) * currentFlySpeed;
            double desiredZd = (dz / dist) * currentFlySpeed;

            double steerX = desiredXd - this.xd;
            double steerY = desiredYd - this.yd;
            double steerZ = desiredZd - this.zd;

            double currentSteerStrength = this.steerStrength * 2.8;
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

        // Prevent clipping through the landing block from above
        if (this.y <= this.landingTargetY && this.yd < 0) {
            this.yd = 0;
            this.y = this.landingTargetY;
        }

        double horizSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);

        // Snap if close and slow
        if (horizDist < 0.35 && Math.abs(this.y - this.landingTargetY) <= 0.1 && horizSpeed < 0.06) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos)
                    .getCollisionShape(level, this.landingBlockPos).isEmpty()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                setState(this, State.PERCHED);
                this.perchTimer = this.perchingTime + (int) (this.random.nextFloat() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
                this.stuckTicks = 0;
            } else {
                setState(this, State.FLYING);
                this.stuckTicks = 0;
            }
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.landingOffsetX = 0.0;
            this.landingOffsetZ = 0.0;
            return;
        }

        // Finalize if we reach the landing Y and are reasonably close horizontally
        if (this.y <= this.landingTargetY + 0.05 && horizDist < 0.6) {
            if (this.landingBlockPos != null && !level.getBlockState(this.landingBlockPos)
                    .getCollisionShape(level, this.landingBlockPos).isEmpty()) {
                this.setPos(targetX, this.landingTargetY, targetZ);
                this.xd = 0;
                this.zd = 0;
                this.yd = 0;
                setState(this, State.PERCHED);
                this.perchTimer = this.perchingTime + (int) (this.random.nextFloat() * this.perchingTime);
                this.perchBlockPos = this.landingBlockPos;
                this.stuckTicks = 0;
            } else {
                setState(this, State.FLYING);
                this.stuckTicks = 0;
            }
            this.landingTargetY = Double.NaN;
            this.landingBlockPos = null;
            this.landingOffsetX = 0.0;
            this.landingOffsetZ = 0.0;
        }
    }

    // MARK: --- TICK PERCHED ---

    private void tickPerched() {
        checkProximityScare();

        this.perchedTimer++;

        this.xd = 0;
        this.zd = 0;
        this.yd = 0;

        if (this.peckTimer > 0) {
            this.peckTimer--;
            if (this.peckTimer == 0) {
                this.animator.updateSprite(1, true);
            }
        } else if (this.random.nextFloat() < 0.01f) {
            this.animator.updateSprite(2, true);
            this.peckTimer = 10 + this.random.nextInt(11);
        }

        if (this.perchBlockPos != null
                && level.getBlockState(this.perchBlockPos).getCollisionShape(level, this.perchBlockPos).isEmpty()) {
            performTakeoff(null, null);
            return;
        }

        if (perchTimer-- <= 0) {
            if (this.threatX != null && this.threatZ != null) {
                performTakeoff(this.threatX, this.threatZ);
                this.threatX = null;
                this.threatZ = null;
            } else {
                setState(this, State.TAKING_OFF);
                this.perchTimer = 20;
                groupTakeoff();
            }
        }
    }

    // MARK: --- TICK TAKING OFF ---

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

        boolean pathBlocked = env.isBlocked(this.x + this.xd, this.y + this.yd + 0.5, this.z + this.zd)
                || env.isBlocked(this.x, this.y + 1.2, this.z);

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

    // MARK: --- TICK DYING ---

    private void tickDying() {
        this.yd -= 0.02;
        if (this.y < env.getLevelMinY() - 4.0 || this.age > this.lifetime + 240) {
            if (debugTextBirds) {
                AtmosphericFauna.LOGGER
                        .info("Bird particle removed due to age or falling out of world: " + this.baseSpriteName);
            }
            this.remove();
        }
    }
}
