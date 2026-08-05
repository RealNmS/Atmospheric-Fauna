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

            if (enableMidairBorderSpawning && random.nextBoolean()) {
                tryMidairSpawn(world, random, selectedSpawn);
            } else {
                trySpawn(world, random, selectedSpawn);
            }
        }
    }

    // MARK: --- SHARED VALIDATION HELPERS ---

    private static boolean passesBasicConditions(ClientLevel world, SpawnData spawnData, int availableSpots) {
        if (availableSpots < spawnData.minPackSize()) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.info("[AS] Not enough spots available");
            return false;
        }

        if (!spawnData.spawnInBadWeather() && (world.isRaining() || world.isThundering())) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.info("[AS] Not spawning due to bad weather");
            return false;
        }

        //? if <=1.21.4 {
        // boolean isDay = world.getDayTime() % 24000 < 13000;
        //?} else {
        boolean isDay = world.isBrightOutside();
        //?}

        if ((!spawnData.spawnDuringDay() && isDay) || (!spawnData.spawnDuringNight() && !isDay)) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.info("[AS] Not spawning due to time of day");
            return false;
        }

        return true;
    }

    private static boolean isValidSpawnLocation(ClientLevel world, BlockPos pos, SpawnData spawnData,
            boolean isMidair) {
        if (isMidair) {
            if (!world.isEmptyBlock(pos)) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] Could not find a valid air block for midair spawn");
                return false;
            }
        } else {
            if (!world.isEmptyBlock(pos.above()) || world.isEmptyBlock(pos.below())) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] Could not find a valid block to spawn");
                return false;
            }

            if (spawnData.checkSpawnBlocks()) {
                var stateBelow = world.getBlockState(pos.below());
                boolean isValidBlock = false;
                for (TagKey<Block> tag : spawnData.validSpawnBlocks()) {
                    if (stateBelow.is(tag)) {
                        isValidBlock = true;
                        break;
                    }
                }
                if (!isValidBlock) {
                    if (debugTextSpawning)
                        AtmosphericFauna.LOGGER.info("[AS] Invalid block tag");
                    return false;
                }
            }

            if (!spawnBelowSeaLevel && (pos.getY() < world.getSeaLevel())) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] Spawn is below sea level");
                return false;
            }
        }

        if (spawnData.checkBiomeTags()) {
            var biomeHolder = world.getBiome(pos);
            boolean biomeMatch = false;
            for (TagKey<Biome> tag : spawnData.validBiomeTags()) {
                if (biomeHolder.is(tag)) {
                    biomeMatch = true;
                    break;
                }
            }
            if (!biomeMatch) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] Could not find a valid biome");
                return false;
            }
        }

        int lightLevel = world.getMaxLocalRawBrightness(pos);
        boolean lightValid = lightLevel >= spawnData.minLightLevel() && lightLevel <= spawnData.maxLightLevel();

        if (!lightValid && debugTextSpawning) {
            AtmosphericFauna.LOGGER.info("[AS] Invalid light level");
        }

        return lightValid;
    }

    // MARK: --- MIDAIR BORDER SPAWNING ---

    private synchronized static void tryMidairSpawn(ClientLevel world, RandomSource random, SpawnData spawnData) {
        int availableSpots = Math.min(
                Math.max(0, BaseBirdParticle.getMaxActiveBirds() - BaseBirdParticle.getAllBirds().size()),
                spawnData.availableSpots().getAsInt());

        if (!passesBasicConditions(world, spawnData, availableSpots))
            return;

        var players = world.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty())
            return;

        for (int attempt = 0; attempt < attemptsPerTick; attempt++) {
            var player = players.get(random.nextInt(players.size()));

            double angle = random.nextFloat() * Math.PI * 2;
            double dist = spawnRangeFromPlayer;

            double spawnX = player.getX() + Math.cos(angle) * dist;
            double spawnZ = player.getZ() + Math.sin(angle) * dist;
            double spawnY = player.getY();

            BlockPos spawnPos = new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ);

            if (isValidSpawnLocation(world, spawnPos, spawnData, true)) {
                int maxPackSize = Math.min(spawnData.maxPackSize(), availableSpots);
                int targetPackSize = random.nextInt(maxPackSize - spawnData.minPackSize() + 1)
                        + spawnData.minPackSize();

                double targetX = player.getX() + (random.nextFloat() - 0.5f) * 16.0;
                double targetZ = player.getZ() + (random.nextFloat() - 0.5f) * 16.0;

                double vecX = targetX - spawnX;
                double vecZ = targetZ - spawnZ;

                for (int i = 0; i < targetPackSize; i++) {
                    world.addParticle(spawnData.particleType(),
                            spawnX + (random.nextFloat() - 0.5f) * 3.0,
                            spawnY,
                            spawnZ + (random.nextFloat() - 0.5f) * 3.0,
                            vecX, 1000.0, vecZ);
                }

                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] SUCCESS: Spawned midair pack of " + spawnData.name());
                return;
            }
        }
    }

    // MARK: --- NORMAL SPAWN ---

    private synchronized static void trySpawn(ClientLevel world, RandomSource random, SpawnData spawnData) {
        int availableSpots = Math.min(
                Math.max(0, BaseBirdParticle.getMaxActiveBirds() - BaseBirdParticle.getAllBirds().size()),
                spawnData.availableSpots().getAsInt());

        if (!passesBasicConditions(world, spawnData, availableSpots))
            return;

        var players = world.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty())
            return;

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
                int maxPackSize = Math.min(spawnData.maxPackSize(), availableSpots);
                int targetPackSize = random.nextInt(maxPackSize - spawnData.minPackSize() + 1)
                        + spawnData.minPackSize();

                int failSafe = 0;
                List<BlockPos> validSpots = new ArrayList<>();

                while (validSpots.size() < targetPackSize && failSafe < targetPackSize * 8) {
                    failSafe++;

                    int dx = random.nextInt(9) - 4;
                    int dz = random.nextInt(9) - 4;

                    BlockPos targetPos = foundCenter.offset(dx, 0, dz);
                    targetPos = adjustToGround(world, targetPos);

                    if (!validSpots.contains(targetPos) && isValidSpawnLocation(world, targetPos, spawnData, false)) {
                        validSpots.add(targetPos);
                    }
                }

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
                                + spawnData.name() + " at " + foundCenter.toShortString());
                    }
                    return;
                } else if (debugTextSpawning) {
                    AtmosphericFauna.LOGGER.info("[AS] ABORTED: Wanted " + targetPackSize + " but only found spots for "
                            + validSpots.size());
                }
            }
        }
    }

    // MARK: --- SPAWN LOCATION SEARCH HELPERS ---

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
        for (int i = 0; i < samples; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int sx = centerX + dx;
            int sz = centerZ + dz;
            int sy = world.getHeight(Heightmap.Types.WORLD_SURFACE, sx, sz);
            BlockPos candidate = new BlockPos(sx, sy, sz);

            // Pass false for the isMidair boolean
            if (isValidSpawnLocation(world, candidate, spawnData, false))
                return candidate;
        }
        return null;
    }
}
