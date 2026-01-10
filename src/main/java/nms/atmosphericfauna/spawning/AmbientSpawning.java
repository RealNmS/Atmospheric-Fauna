package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
// import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import java.util.List;

public class AmbientSpawning {

    public static boolean debugText = false;

    // --- SPAWN DATA CONSTANTS ---

    private record SpawnData(
            SimpleParticleType particleType,
            int weight,
            int minPackSize,
            int maxPackSize,
            int minLightLevel,
            int maxLightLevel,
            Integer maxSpawnHeight,
            boolean spawnInBadWeather,
            boolean spawnDuringDay,
            boolean spawnDuringNight,
            TagKey<Biome> validBiomeTag) {
    }

    private static final SpawnData CROW_SPAWN_DATA = new SpawnData(
            AtmosphericFauna.CROW,
            30, // weight
            3, 8, // pack size
            8, 15, // light level
            null, // max spawn height
            true, // spawn in bad weather
            true, // spawn during day
            true, // spawn during night
            BiomeTags.IS_OVERWORLD);

    private static final List<SpawnData> SPAWN_DATA_LIST = List.of(
            CROW_SPAWN_DATA
    // Future bird types can be added here
    );

    public static int spawnRangeFromPlayer = 96;
    private static final int TOTAL_SPAWN_WEIGHT = SPAWN_DATA_LIST.stream().mapToInt(SpawnData::weight).sum();
    public static int spawnTickDelay = 200;
    public static int attemptsPerTick = 15;
    public static int searchRadius = 12;

    public static void tick(ClientLevel world) {
        if (world.getGameTime() % spawnTickDelay != 0) {
            return;
        }

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

    private static void trySpawn(ClientLevel world, RandomSource random, SpawnData spawnData) {
        if (debugText)
            System.out.println("[AtmosphericFauna] Ambient spawning cycle started...");

        if (!spawnData.spawnInBadWeather() && (world.isRaining() || world.isThundering())) {
            return;
        }

        // Only spawn if time of day is right
        boolean isDay = world.getDayTime() < 12000;
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
                int targetPackSize = random.nextInt(spawnData.maxPackSize() - spawnData.minPackSize() + 1)
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
                    if (spawnedCount >= spawnData.minPackSize()) {
                        System.out.println("[AtmosphericFauna] SUCCESS: Spawned pack of " + spawnedCount + " crows at "
                                + foundCenter.toShortString());
                    } else {
                        System.out.println(
                                "[AtmosphericFauna] PARTIAL: Wanted " + targetPackSize + " but only found spots for "
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

        // Biome Check
        if (!world.getBiome(pos).is(spawnData.validBiomeTag()))
            return false;

        // Height Check
        if (spawnData.maxSpawnHeight() != null && pos.getY() > spawnData.maxSpawnHeight())
            return false;

        // Light Check
        int sky = world.getBrightness(LightLayer.SKY, pos);
        int block = world.getBrightness(LightLayer.BLOCK, pos);
        int lightLevel = Math.max(sky, block);

        return lightLevel >= spawnData.minLightLevel() && lightLevel <= spawnData.maxLightLevel();
    }
}
