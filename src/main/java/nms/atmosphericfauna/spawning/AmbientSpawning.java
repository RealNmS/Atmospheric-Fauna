package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        int currentBirdCount = BaseBirdParticle.getAllBirds().size();
        if (currentBirdCount >= BaseBirdParticle.getMaxActiveBirds()) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.warn("[AS] Skipped attempt: Global bird limit ("
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
                    AtmosphericFauna.LOGGER
                            .warn("[AS] Skipped attempt: Species limit reached. (" + selectedSpawn.name() + ")");
                }
                return;
            }

            if (enableMidairBorderSpawning && random.nextBoolean()) {
                tryMidairSpawn(world, random, selectedSpawn, currentBirdCount);
            } else {
                trySpawn(world, random, selectedSpawn, currentBirdCount);
            }
        }
    }

    // MARK: --- SHARED VALIDATION HELPERS ---

    private static boolean passesBasicConditions(ClientLevel world, SpawnData spawnData, int availableSpots) {
        if (availableSpots < spawnData.minPackSize()) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.warn("[AS] Not enough spots available for " + spawnData.name());
            return false;
        }

        if (!spawnData.spawnInBadWeather() && (world.isRaining() || world.isThundering())) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.warn("[AS] Not spawning due to bad weather for " + spawnData.name());
            return false;
        }

        if (world.dimensionType().hasSkyLight()) {
            //? if <=1.21.4 {
            // boolean isDay = world.getDayTime() % 24000 < 13000;
            //?} else {
            boolean isDay = world.isBrightOutside();
            //?}

            if ((!spawnData.spawnDuringDay() && isDay) || (!spawnData.spawnDuringNight() && !isDay)) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.warn("[AS] Not spawning due to time of day for " + spawnData.name());
                return false;
            }
        }

        return true;
    }

    private static boolean isValidSpawnLocation(ClientLevel world, BlockPos pos, SpawnData spawnData,
            boolean isMidair) {

        if (!world.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (debugTextSpawning)
                AtmosphericFauna.LOGGER.warn("[AS] Could not spawn: Chunk is not loaded");
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            double maxDist = mc.options.renderDistance().get() * 16.0;
            if (distSq > maxDist * maxDist) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.warn("[AS] Could not spawn: Location is outside render distance");
                return false;
            }
        }

        if (isMidair) {
            if (!world.isEmptyBlock(pos)) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER
                            .warn("[AS] Could not find a valid air block for midair spawn at " + pos.toShortString());
                return false;
            }
        } else {
            var stateAtPos = world.getBlockState(pos);
            var stateBelow = world.getBlockState(pos.below());

            if (!stateAtPos.getFluidState().isEmpty()
                    || !stateAtPos.getCollisionShape(world, pos).isEmpty()
                    || !world.isEmptyBlock(pos.above())
                    || world.isEmptyBlock(pos.below())
                    || !stateBelow.getFluidState().isEmpty()
                    || isTechnicalBlock(stateBelow)) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER
                            .warn("[AS] Could not find a valid block to spawn at " + pos.toShortString());
                return false;
            }

            if (spawnData.checkSpawnBlocks()) {
                boolean isValidBlock = false;
                for (TagKey<Block> tag : spawnData.validSpawnBlocks()) {
                    if (stateBelow.is(tag)) {
                        isValidBlock = true;
                        break;
                    }
                }
                if (!isValidBlock) {
                    if (debugTextSpawning)
                        AtmosphericFauna.LOGGER.warn("[AS] Invalid block tag: " + stateBelow.getBlock().toString());
                    return false;
                }
            }

            if (!spawnBelowSeaLevel && (pos.getY() < world.getSeaLevel())) {
                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.warn("[AS] Spawn is below sea level");
                return false;
            }
        }

        if (spawnData.checkBiomeTags()) {
            var biomeHolder = world.getBiome(pos);
            boolean biomeMatch = false;
            for (String biome : spawnData.validBiomes()) {
                if (matchesBiome(world, biomeHolder, biome)) {
                    biomeMatch = true;
                    break;
                }
            }
            if (!biomeMatch) {
                if (debugTextSpawning) {
                    String biomeName = biomeHolder.unwrapKey()
                            //? if <=1.21.9 {
                            // .map(key -> key.location().toString())
                            //?} else {
                            .map(key -> key.identifier().toString())
                            //?}
                            .orElse("unknown");
                    AtmosphericFauna.LOGGER
                            .warn("[AS] Could not find a valid biome: " + biomeName + " for " + spawnData.name());
                }
                return false;
            }
        }

        boolean lightValid = true;
        if (world.dimensionType().hasSkyLight()) {
            int lightLevel = world.getMaxLocalRawBrightness(pos);
            lightValid = lightLevel >= spawnData.minLightLevel() && lightLevel <= spawnData.maxLightLevel();

            if (!lightValid && debugTextSpawning) {
                AtmosphericFauna.LOGGER.warn("[AS] Invalid light level: " + lightLevel);
            } 
        }

        return lightValid;
    }

    private static boolean matchesBiome(ClientLevel world, Holder<Biome> biomeHolder, String configuredBiome) {
        String biome = configuredBiome.trim().toLowerCase(Locale.ROOT);
        boolean isTag = biome.startsWith("#");
        if (isTag) {
            biome = biome.substring(1);
        }

        int separator = biome.indexOf(':');
        String namespace = separator > 0 ? biome.substring(0, separator) : "minecraft";
        String path = separator > 0 ? biome.substring(separator + 1) : biome;

        try {
            var identifier = AtmosphericFauna.id(namespace, path);
            if (isTag) {
                var tagKey = TagKey.create(Registries.BIOME, identifier);
                if (biomeHolder.is(tagKey)) {
                    return true;
                }
                // Fallback for multiplayer servers (Paper, proxies, ViaVersion) that do not sync worldgen biome tags
                if (biomeHolder.tags().findAny().isEmpty()) {
                    if (namespace.equals("minecraft") && path.equals("is_overworld")) {
                        return world.dimensionType().hasSkyLight();
                    }
                    if (debugTextSpawning) {
                        AtmosphericFauna.LOGGER.warn(
                                "[AS] Server did not sync biome tags; cannot match tag '{}'", configuredBiome);
                    }
                }
                return false;
            }
            return biomeHolder.unwrapKey()
                    //? if <=1.21.9 {
                    // .map(key -> key.location().equals(identifier))
                    //?} else {
                    .map(key -> key.identifier().equals(identifier))
                    //?}
                    .orElse(false);
        } catch (IllegalArgumentException exception) {
            if (debugTextSpawning) {
                AtmosphericFauna.LOGGER.warn("[AS] Invalid configured biome '{}'", configuredBiome);
            }
            return false;
        }
    }

    private static boolean isTechnicalBlock(BlockState state) {
        return state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.LIGHT);
    }

    // MARK: --- MIDAIR BORDER SPAWNING ---

    private synchronized static void tryMidairSpawn(ClientLevel world, RandomSource random, SpawnData spawnData,
            int currentBirdCount) {
        int availableSpots = Math.min(
                Math.max(0, BaseBirdParticle.getMaxActiveBirds() - currentBirdCount),
                spawnData.availableSpots().getAsInt());

        if (!passesBasicConditions(world, spawnData, availableSpots))
            return;

        var players = world.players().stream().filter(p -> spawnAroundSpectators || !p.isSpectator()).toList();
        if (players.isEmpty())
            return;

        for (int attempt = 0; attempt < attemptsPerTick; attempt++) {
            var player = players.get(random.nextInt(players.size()));

            double angle = random.nextFloat() * Math.PI * 2;
            Minecraft mc = Minecraft.getInstance();
            double maxLoadedRadius = Math.max(16.0, (mc.options.renderDistance().get() - 1) * 16.0);
            double dynamicMaxRange = Math.min((double) spawnRangeFromPlayer, maxLoadedRadius);
            double dist = dynamicMaxRange;
            double spawnX = player.getX() + Math.cos(angle) * dist;
            double spawnZ = player.getZ() + Math.sin(angle) * dist;
            double spawnY = player.getY();

            BlockPos spawnPos = BlockPos.containing(spawnX, spawnY, spawnZ);

            if (isValidSpawnLocation(world, spawnPos, spawnData, true)) {
                int maxPackSize = Math.min(spawnData.maxPackSize(), availableSpots);
                int targetPackSize = random.nextInt(maxPackSize - spawnData.minPackSize() + 1)
                        + spawnData.minPackSize();

                double targetX = player.getX() + (random.nextFloat() - 0.5f) * 16.0;
                double targetZ = player.getZ() + (random.nextFloat() - 0.5f) * 16.0;

                double vecX = targetX - spawnX;
                double vecZ = targetZ - spawnZ;

                BaseBirdParticle.setNextFlockSpeedOffset((random.nextFloat() - 0.5f) * 0.04f);

                for (int i = 0; i < targetPackSize; i++) {
                    world.addParticle(spawnData.particleType(),
                            spawnX + (random.nextFloat() - 0.5f) * 3.0,
                            spawnY,
                            spawnZ + (random.nextFloat() - 0.5f) * 3.0,
                            vecX, 1000.0, vecZ);
                }

                if (debugTextSpawning)
                    AtmosphericFauna.LOGGER.info("[AS] Spawned midair pack of " + spawnData.name());
                return;
            }
        }
    }

    // MARK: --- NORMAL SPAWN ---

    private synchronized static void trySpawn(ClientLevel world, RandomSource random, SpawnData spawnData,
            int currentBirdCount) {
        int availableSpots = Math.min(
                Math.max(0, BaseBirdParticle.getMaxActiveBirds() - currentBirdCount),
                spawnData.availableSpots().getAsInt());

        if (!passesBasicConditions(world, spawnData, availableSpots))
            return;

        var players = world.players().stream().filter(p -> spawnAroundSpectators || !p.isSpectator()).toList();
        if (players.isEmpty())
            return;

        for (int attempt = 0; attempt < attemptsPerTick; attempt++) {
            var player = players.get(random.nextInt(players.size()));
            BlockPos playerPos = player.blockPosition();

            double angle = random.nextFloat() * Math.PI * 2;
            Minecraft mc = Minecraft.getInstance();
            double maxLoadedRadius = Math.max(16.0, (mc.options.renderDistance().get() - 1) * 16.0);
            double dynamicMaxRange = Math.min((double) spawnRangeFromPlayer, maxLoadedRadius);
            double curveFactor = dynamicMaxRange / (double) Math.max(1, spawnRangeFromPlayer);
            double minDist = (dynamicMaxRange / 2.0) * curveFactor;
            double distance = minDist + random.nextFloat() * (dynamicMaxRange - minDist);

            int baseX = playerPos.getX() + (int) Math.floor(Math.cos(angle) * distance);
            int baseZ = playerPos.getZ() + (int) Math.floor(Math.sin(angle) * distance);

            BlockPos foundCenter = findValidSpawnNear(world, random, baseX, baseZ, spawnData, searchRadius, 12);

            if (foundCenter != null) {
                int maxPackSize = Math.min(spawnData.maxPackSize(), availableSpots);
                int range = Math.max(1, maxPackSize - spawnData.minPackSize() + 1);
                int targetPackSize = random.nextInt(range) + spawnData.minPackSize();

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
                    BaseBirdParticle.setNextFlockSpeedOffset((random.nextFloat() - 0.5f) * 0.04f);

                    for (BlockPos pos : validSpots) {
                        world.addParticle(spawnData.particleType(),
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                (random.nextFloat() - 0.5f) * 0.05, 0, (random.nextFloat() - 0.5f) * 0.05);
                    }

                    if (debugTextSpawning) {
                        AtmosphericFauna.LOGGER.info("[AS] Spawned full pack of " + targetPackSize + " "
                                + spawnData.name() + " at " + foundCenter.toShortString());
                    }
                    return;
                } else if (debugTextSpawning) {
                    AtmosphericFauna.LOGGER.warn("[AS] Wanted " + targetPackSize + " but only found spots for "
                            + validSpots.size());
                }
            }
        }
    }

    // MARK: --- SPAWN LOCATION SEARCH HELPERS ---

    private static BlockPos adjustToGround(ClientLevel world, BlockPos pos) {
        if (!world.isEmptyBlock(pos.below()) && !isTechnicalBlock(world.getBlockState(pos.below())) && world.isEmptyBlock(pos))
            return pos;

        for (int i = 1; i <= 3; i++) {
            BlockPos down = pos.below(i);
            if (!world.isEmptyBlock(down.below()) && !isTechnicalBlock(world.getBlockState(down.below())) && world.isEmptyBlock(down))
                return down;
            BlockPos up = pos.above(i);
            if (!world.isEmptyBlock(up.below()) && !isTechnicalBlock(world.getBlockState(up.below())) && world.isEmptyBlock(up))
                return up;
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

            // If WORLD_SURFACE landed on a barrier/technical ceiling, scan downward for the ground below
            if (isTechnicalBlock(world.getBlockState(candidate.below()))) {
                //? if <=1.21.1 {
                // int minY = world.getMinBuildHeight();
                //?} else {
                int minY = world.getMinY();
                //?}
                BlockPos foundBelow = null;
                for (int y = sy - 1; y > minY; y--) {
                    BlockPos checkPos = new BlockPos(sx, y, sz);
                    BlockState belowState = world.getBlockState(checkPos.below());
                    if (!world.isEmptyBlock(checkPos.below())
                            && !isTechnicalBlock(belowState)
                            && world.isEmptyBlock(checkPos)
                            && world.isEmptyBlock(checkPos.above())) {
                        foundBelow = checkPos;
                        break;
                    }
                }
                if (foundBelow == null) {
                    continue;
                }
                candidate = foundBelow;
            }

            // Pass false for the isMidair boolean
            if (isValidSpawnLocation(world, candidate, spawnData, false))
                return candidate;
        }
        return null;
    }
}
