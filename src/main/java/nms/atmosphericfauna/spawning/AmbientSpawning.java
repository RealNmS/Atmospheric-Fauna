package nms.atmosphericfauna.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import nms.atmosphericfauna.AtmosphericFauna;
import java.util.List;

public class AmbientSpawning {

    // --- SPAWN DATA CONSTANTS ---

    private record SpawnData(
            SimpleParticleType particleType,
            int weight,
            int minPackSize,
            int maxPackSize,
            int minLightLevel,
            int maxLightLevel,
            int maxSpawnHeight,
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
            140, // max height
            true, // spawn in bad weather
            true, // spawn during day
            false, // spawn during night
            BiomeTags.IS_OVERWORLD);

    private static final List<SpawnData> SPAWN_DATA_LIST = List.of(
            CROW_SPAWN_DATA
    // Future bird types can be added here
    );

    private static final int SPAWN_RANGE_FROM_PLAYER = 48;
    private static final int TOTAL_SPAWN_WEIGHT = SPAWN_DATA_LIST.stream().mapToInt(SpawnData::weight).sum();
    private static final int SPAWN_TICK_DELAY = 200; // ~10 seconds
    private static final int ATTEMPTS_PER_TICK = 8; // number of candidate positions to try each spawn tick
    private static final int SEARCH_RADIUS = 6; // radius around chosen point to search for a valid spawn position

    /**
     * This method is called from a server tick event to attempt ambient spawns.
     * 
     * @param world The server world.
     */
    public static void tick(ServerLevel world) {
        if (world.getGameTime() % SPAWN_TICK_DELAY != 0) {
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

    private static void trySpawn(ServerLevel world, RandomSource random, SpawnData spawnData) {
        // Only spawn if weather conditions are right
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
        for (int attempt = 0; attempt < ATTEMPTS_PER_TICK; attempt++) {
            // pick a random player to spawn near
            var player = players.get(random.nextInt(players.size()));
            BlockPos playerPos = player.blockPosition();

            int baseX = playerPos.getX() + random.nextInt(SPAWN_RANGE_FROM_PLAYER * 2) - SPAWN_RANGE_FROM_PLAYER;
            int baseZ = playerPos.getZ() + random.nextInt(SPAWN_RANGE_FROM_PLAYER * 2) - SPAWN_RANGE_FROM_PLAYER;

            // search nearby for a valid spawn spot (helps avoid water, leaves, or other bad
            // spots)
            BlockPos found = findValidSpawnNear(world, random, baseX, baseZ, spawnData, SEARCH_RADIUS, 12);
            if (found != null) {
                // Spawn a small pack around that found position
                int packSize = random.nextInt(spawnData.maxPackSize() - spawnData.minPackSize() + 1)
                        + spawnData.minPackSize();
                int spawned = 0;
                int innerAttempts = 0;
                while (spawned < packSize && innerAttempts < packSize * 4) {
                    innerAttempts++;
                    BlockPos individualSpawnPos = found.offset(random.nextInt(5) - 2, random.nextInt(2),
                            random.nextInt(5) - 2);
                    if (isValidSpawnLocation(world, individualSpawnPos, spawnData)) {
                        world.sendParticles(spawnData.particleType(),
                                individualSpawnPos.getX() + 0.5,
                                individualSpawnPos.getY() + 1.0,
                                individualSpawnPos.getZ() + 0.5,
                                1, 0, 0, 0, 0);
                        spawned++;
                    }
                }
                // Success, don't keep trying more attempts this tick
                return;
            }
        }
        // If all attempts failed, optionally try again next tick
    }

    private static BlockPos findValidSpawnNear(ServerLevel world, RandomSource random, int centerX, int centerZ,
            SpawnData spawnData, int radius, int samples) {
        // Try the center first
        int yCenter = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        BlockPos center = new BlockPos(centerX, yCenter, centerZ);
        if (isValidSpawnLocation(world, center, spawnData))
            return center;

        // Random sampling in the radius to find nearby valid spots
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

    private static boolean isValidSpawnLocation(ServerLevel world, BlockPos pos, SpawnData spawnData) {
        if (pos.getY() > spawnData.maxSpawnHeight()) {
            return false;
        }

        int sky = world.getBrightness(LightLayer.SKY, pos);
        int block = world.getBrightness(LightLayer.BLOCK, pos);
        int lightLevel = Math.max(sky, block); // prefer the brighter of sky or block light
        if (lightLevel < spawnData.minLightLevel() || lightLevel > spawnData.maxLightLevel()) {
            return false;
        }

        if (!world.getBiome(pos).is(spawnData.validBiomeTag())) {
            return false;
        }

        return (world.isEmptyBlock(pos.above()) && !world.isEmptyBlock(pos));
    }
}
