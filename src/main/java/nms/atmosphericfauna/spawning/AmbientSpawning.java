package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;

public class AmbientSpawning {

    // MARK: --- SPAWN LOGIC ---

    public static void tick(ClientLevel world) {
        if ((world.getGameTime() % spawnTickDelay != 0) || !enableAmbientSpawning) {
            return;
        }
        runSpawnAttempt(world);
    }

    public static void runSpawnAttempt(ClientLevel world) {
        List<SpawnData> eligibleSpawns = SpawnData.ALL_SPAWNS.stream()
                .filter(SpawnData::spawnAllowed)
                .toList();

        int totalWeight = eligibleSpawns.stream().mapToInt(SpawnData::weight).sum();
        if (totalWeight <= 0) {
            return;
        }

        if (BaseBirdParticle.getAllBirds().size() >= BaseBirdParticle.getMaxActiveBirds()) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.info("[AS] Skipped attempt: Global bird limit ("
                        + BaseBirdParticle.getMaxActiveBirds() + ") reached.");
            }
            return;
        }

        RandomSource random = world.getRandom();
        int choice = random.nextInt(totalWeight);

        SpawnData selectedSpawn = null;
        int cumulativeWeight = 0;
        for (SpawnData data : eligibleSpawns) {
            cumulativeWeight += data.weight();
            if (choice < cumulativeWeight) {
                selectedSpawn = data;
                break;
            }
        }

        if (selectedSpawn != null) {
            if (selectedSpawn.availableSpots().getAsInt() <= 0) {
                if (debugTextSpawning) {
                    AtmosphericFauna.LOGGER.info("[AS] Skipped attempt: Species limit reached.");
                }
                return;
            }
            trySpawn(world, random, selectedSpawn);
        }
    }

    private synchronized static void trySpawn(ClientLevel world, RandomSource random, SpawnData spawnData) {
        String particleName = spawnData.name();

        if (debugTextSpawning) {
            AtmosphericFauna.LOGGER.info("[AS] Attempting to spawn " + particleName);
        }

        int availableGlobalSpots = Math.max(0,
                BaseBirdParticle.getMaxActiveBirds() - BaseBirdParticle.getAllBirds().size());
        int availableTypedSpots = spawnData.availableSpots().getAsInt();
        int availableSpots = Math.min(availableGlobalSpots, availableTypedSpots);

        if (availableSpots < spawnData.minPackSize()) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER
                        .info("[AS] Not enough spots available" + " (global: " + availableGlobalSpots + ", typed: "
                                + availableTypedSpots + ")");
            return;
        }

        if (!spawnData.spawnInBadWeather() && (world.isRaining() || world.isThundering())) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER
                        .info("[AS] Not spawning due to bad weather" + " (raining: " + world.isRaining()
                                + ", thundering: " + world.isThundering() + ")");
            return;
        }

        // Only spawn if time of day is right
        //? if <=1.21.4 {
        // boolean isDay = world.getDayTime() % 24000 < 13000;
        //?} else {
        boolean isDay = world.isBrightOutside();
        //?}

        if ((!spawnData.spawnDuringDay() && isDay) || (!spawnData.spawnDuringNight() && !isDay)) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER
                        .info("[AS] Not spawning due to time of day" + " (isDay: " + isDay + ")");
            return;
        }

        // Gather eligible players
        var players = world.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty()) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER
                        .info("[AS] Not spawning due to no eligible players" + " (players: " + players.size() + ")");
            return;
        }

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

                int failSafe = 0;
                List<BlockPos> validSpots = new ArrayList<>();

                // Try to find enough valid spots for the ENTIRE pack before spawning any
                while (validSpots.size() < targetPackSize && failSafe < targetPackSize * 8) {
                    failSafe++;

                    int dx = random.nextInt(9) - 4;
                    int dz = random.nextInt(9) - 4;

                    BlockPos targetPos = foundCenter.offset(dx, 0, dz);
                    targetPos = adjustToGround(world, targetPos);

                    if (!validSpots.contains(targetPos) && isValidSpawnLocation(world, targetPos, spawnData)) {
                        validSpots.add(targetPos);
                    }
                }

                // Strictly require the full pack to be accommodated
                if (validSpots.size() == targetPackSize) {
                    for (BlockPos pos : validSpots) {
                        world.addParticle(spawnData.particleType(),
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                (random.nextFloat() - 0.5f) * 0.05, 0, (random.nextFloat() - 0.5f) * 0.05);
                    }

                    if (debugTextSpawning) {
                        AtmosphericFauna.LOGGER.info("[AS] SUCCESS: Spawned full pack of " + targetPackSize + " "
                                + particleName + " at " + foundCenter.toShortString());
                    }
                    return;
                } else {
                    if (debugTextSpawning) {
                        AtmosphericFauna.LOGGER.info("[AS] ABORTED: Wanted " + targetPackSize
                                + " but only found spots for " + validSpots.size());
                    }
                }
            }
        }

        if (debugTextSpawning) {
            AtmosphericFauna.LOGGER.info("[AS] FAIL: Could not find a valid block to spawn " + particleName
                    + " (Check light level, tags, or biome)");
        }
    }

    // MARK: --- SPAWN LOCATION HELPERS ---

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
            int sy = world.getHeight(Heightmap.Types.WORLD_SURFACE, sx, sz);
            BlockPos candidate = new BlockPos(sx, sy, sz);

            if (isValidSpawnLocation(world, candidate, spawnData))
                return candidate;
        }
        return null;
    }

    private static boolean isValidSpawnLocation(ClientLevel world, BlockPos pos, SpawnData spawnData) {
        // Must have air above and block below
        if (!world.isEmptyBlock(pos.above()) || world.isEmptyBlock(pos.below())) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.info("[AS] Could not find a valid block to spawn");
            }
            return false;
        }

        if (spawnData.checkSpawnBlocks()) {
            // Check for valid spawn blocks
            var stateBelow = world.getBlockState(pos.below());
            boolean isValidBlock = false;
            for (TagKey<Block> tag : spawnData.validSpawnBlocks()) {
                if (stateBelow.is(tag)) {
                    isValidBlock = true;
                    break;
                }
            }
            if (!isValidBlock) {
                if (debugTextSpawning) {
                    AtmosphericFauna.LOGGER.info("[AS] Could not find a valid block to spawn");
                }
                return false;
            }
        }

        if (spawnData.checkBiomeTags()) {
            // Valid biome check
            var biomeHolder = world.getBiome(pos);
            boolean biomeMatch = false;
            for (TagKey<Biome> tag : spawnData.validBiomeTags()) {
                if (biomeHolder.is(tag)) {
                    biomeMatch = true;
                    break;
                }
            }
            if (!biomeMatch) {
                if (debugTextSpawning) {
                    AtmosphericFauna.LOGGER.info("[AS] Could not find a valid biome to spawn");
                }
                return false;
            }
        }

        // Height Check
        if (!spawnBelowSeaLevel && (pos.getY() < world.getSeaLevel())) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.info("[AS] Could not find a valid height to spawn");
            }
            return false;
        }

        // Light Check
        int lightLevel = world.getMaxLocalRawBrightness(pos);
        boolean lightValid = lightLevel >= spawnData.minLightLevel() && lightLevel <= spawnData.maxLightLevel();

        if (!lightValid) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.info("[AS] Could not find a valid light level to spawn");
            }
        }

        return lightValid;
    }
}
