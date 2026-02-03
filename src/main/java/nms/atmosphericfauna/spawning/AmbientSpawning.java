package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.BaseBirdParticle;
import nms.atmosphericfauna.particle.CrowParticle;

import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
// import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class AmbientSpawning {

    // --- CONFIG STUFF ---

    public static boolean debugText = false;
    public static boolean spawnBelowSeaLevel = false;

    // --- SPAWN DATA CONSTANTS ---

    private record SpawnData(
            SimpleParticleType particleType,
            int weight,
            int minPackSize,
            int maxPackSize,
            int minLightLevel,
            int maxLightLevel,
            boolean spawnInBadWeather,
            boolean spawnDuringDay,
            boolean spawnDuringNight,
            TagKey<Biome> validBiomeTag,
            List<TagKey<Block>> validSpawnBlocks,
            IntSupplier availableSpots) {
    }

    private static final SpawnData CROW_SPAWN_DATA = new SpawnData(
            AtmosphericFauna.CROW,
            30, // weight
            3, 8, // pack size
            8, 15, // light level
            true, // spawn in bad weather
            true, // spawn during day
            true, // spawn during night
            BiomeTags.IS_OVERWORLD, // valid biome
            List.of(
                    BlockTags.DIRT,
                    BlockTags.LEAVES,
                    BlockTags.LOGS,
                    BlockTags.SAND,
                    BlockTags.SNOW,
                    BlockTags.BASE_STONE_OVERWORLD), // valid spawn blocks
            () -> Math.max(0, CrowParticle.maxActiveCrows - CrowParticle.getCount())); // max bird count

    private static final List<SpawnData> SPAWN_DATA_LIST = List.of(
            CROW_SPAWN_DATA
    // Future bird types can be added here
    );

    public static int spawnRangeFromPlayer = 96;
    private static final int TOTAL_SPAWN_WEIGHT = SPAWN_DATA_LIST.stream().mapToInt(SpawnData::weight).sum();
    public static int spawnTickDelay = 200;
    public static int attemptsPerTick = 15;
    public static int searchRadius = 12;

    // --- SPAWN LOGIC ---

    public static void tick(ClientLevel world) {
        if (world.getGameTime() % spawnTickDelay != 0) {
            return;
        }
        runSpawnAttempt(world);
    }

    public static void runSpawnAttempt(ClientLevel world) {
        if (TOTAL_SPAWN_WEIGHT <= 0) {
            return;
        }

        RandomSource random = world.getRandom();
        int choice = random.nextInt(TOTAL_SPAWN_WEIGHT);

        SpawnData selectedSpawn = null;
        int cumulativeWeight = 0;
        for (SpawnData data : SPAWN_DATA_LIST) {
            cumulativeWeight += data.weight();
            if (choice < cumulativeWeight) {
                selectedSpawn = data;
                break;
            }
        }

        if (selectedSpawn != null) {
            trySpawn(world, random, selectedSpawn);
        }
    }

    private synchronized static void trySpawn(ClientLevel world, RandomSource random, SpawnData spawnData) {
        if (debugText)
            AtmosphericFauna.LOGGER.info("Ambient spawning cycle started...");

        int availableGlobalSpots = BaseBirdParticle.maxActiveBirds - BaseBirdParticle.ALL_BIRDS.size();
        int availableTypedSpots = spawnData.availableSpots().getAsInt();
        int availableSpots = Math.min(availableGlobalSpots, availableTypedSpots);

        if (availableSpots < spawnData.minPackSize()) {
            return;
        }

        if (!spawnData.spawnInBadWeather() && (world.isRaining() || world.isThundering())) {
            return;
        }

        // Only spawn if time of day is right
        boolean isDay = world.getDayTime() % 24000 < 12000;
        if ((!spawnData.spawnDuringDay() && isDay) || (!spawnData.spawnDuringNight() && !isDay)) {
            return;
        }

        // Gather eligible players
        var players = world.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty())
            return;

        // Try several candidate positions to increase robustness
        for (int attempt = 0; attempt < attemptsPerTick; attempt++) {
            var player = players.get(random.nextInt(players.size()));
            BlockPos playerPos = player.blockPosition();

            double angle = random.nextFloat() * Math.PI * 2;
            double minDist = spawnRangeFromPlayer / 2.0;
            double distance = minDist + random.nextFloat() * (spawnRangeFromPlayer - minDist);

            int baseX = playerPos.getX() + (int) (Math.cos(angle) * distance);
            int baseZ = playerPos.getZ() + (int) (Math.sin(angle) * distance);

            BlockPos foundCenter = findValidSpawnNear(world, random, baseX, baseZ, spawnData, searchRadius, 12);

            if (foundCenter != null) {
                // Determine pack size
                int maxPackSize = Math.min(spawnData.maxPackSize(), availableSpots);
                int targetPackSize = random.nextInt(maxPackSize - spawnData.minPackSize() + 1)
                        + spawnData.minPackSize();

                int spawnedCount = 0;
                int failSafe = 0;

                // Try to spawn the whole pack
                while (spawnedCount < targetPackSize && failSafe < targetPackSize * 8) {
                    failSafe++;

                    int dx = random.nextInt(9) - 4;
                    int dz = random.nextInt(9) - 4;

                    BlockPos targetPos = foundCenter.offset(dx, 0, dz);
                    targetPos = adjustToGround(world, targetPos);

                    if (isValidSpawnLocation(world, targetPos, spawnData)) {
                        world.addParticle(spawnData.particleType(),
                                targetPos.getX() + 0.5,
                                targetPos.getY() + 0.5,
                                targetPos.getZ() + 0.5,
                                0, 0, 0); // velocity x, y, z
                        spawnedCount++;
                    }
                }

                if (debugText) {
                    if (spawnedCount >= targetPackSize) {
                        AtmosphericFauna.LOGGER
                                .info("SUCCESS: Spawned pack of " + spawnedCount + " crows at "
                                        + foundCenter.toShortString());
                    } else {
                        AtmosphericFauna.LOGGER.info(
                                "PARTIAL: Wanted " + targetPackSize + " but only found spots for "
                                        + spawnedCount);
                    }
                }

                if (spawnedCount > 0)
                    return;
            }
        }
    }

    // Helper to snap a position to the nearest solid ground within 3 blocks
    // vertical
    private static BlockPos adjustToGround(ClientLevel world, BlockPos pos) {
        if (!world.isEmptyBlock(pos.below()) && world.isEmptyBlock(pos))
            return pos;

        for (int i = 1; i <= 3; i++) {
            if (!world.isEmptyBlock(pos.below(i).below()) && world.isEmptyBlock(pos.below(i)))
                return pos.below(i);
            if (!world.isEmptyBlock(pos.above(i).below()) && world.isEmptyBlock(pos.above(i)))
                return pos.above(i);
        }
        return pos;
    }

    private static BlockPos findValidSpawnNear(ClientLevel world, RandomSource random, int centerX, int centerZ,
            SpawnData spawnData, int radius, int samples) {

        // Sample random spots
        for (int i = 0; i < samples; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int sx = centerX + dx;
            int sz = centerZ + dz;
            int sy = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            BlockPos candidate = new BlockPos(sx, sy, sz);

            if (isValidSpawnLocation(world, candidate, spawnData))
                return candidate;
        }
        return null;
    }

    private static boolean isValidSpawnLocation(ClientLevel world, BlockPos pos, SpawnData spawnData) {
        // Must have air above and block below
        if (!world.isEmptyBlock(pos.above()) || world.isEmptyBlock(pos.below()))
            return false;

        // Check for valid spawn blocks
        var stateBelow = world.getBlockState(pos.below());
        boolean isValidBlock = false;
        for (TagKey<Block> tag : spawnData.validSpawnBlocks()) {
            if (stateBelow.is(tag)) {
                isValidBlock = true;
                break;
            }
        }
        if (!isValidBlock)
            return false;

        // Biome Check
        if (!world.getBiome(pos).is(spawnData.validBiomeTag()))
            return false;

        // Height Check
        if (!spawnBelowSeaLevel && (pos.getY() < world.getSeaLevel()))
            return false;

        // Light Check
        int lightLevel = world.getMaxLocalRawBrightness(pos);

        return lightLevel >= spawnData.minLightLevel() && lightLevel <= spawnData.maxLightLevel();
    }
}
