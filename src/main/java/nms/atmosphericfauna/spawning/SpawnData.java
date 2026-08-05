package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.config.ConfigHandler;
import nms.atmosphericfauna.particle.BlueJayParticle;
import nms.atmosphericfauna.particle.CrowParticle;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.function.IntSupplier;

public final class SpawnData {

    private final String name;
    private final SimpleParticleType particleType;
    private final int weight;
    private final int minPackSize;
    private final int maxPackSize;
    private final int minLightLevel;
    private final int maxLightLevel;
    private final boolean spawnInBadWeather;
    private final boolean spawnDuringDay;
    private final boolean spawnDuringNight;
    private boolean spawnAllowed;
    private boolean checkBiomeTags;
    private boolean checkSpawnBlocks;
    private final List<TagKey<Biome>> validBiomeTags;
    private final List<TagKey<Block>> validSpawnBlocks;
    private final IntSupplier availableSpots;

    public SpawnData(
            String name,
            SimpleParticleType particleType,
            int weight,
            int minPackSize,
            int maxPackSize,
            int minLightLevel,
            int maxLightLevel,
            boolean spawnInBadWeather,
            boolean spawnDuringDay,
            boolean spawnDuringNight,
            boolean spawnAllowed,
            boolean checkBiomeTags,
            boolean checkSpawnBlocks,
            List<TagKey<Biome>> validBiomeTags,
            List<TagKey<Block>> validSpawnBlocks,
            IntSupplier availableSpots) {
        this.name = name;
        this.particleType = particleType;
        this.weight = weight;
        this.minPackSize = minPackSize;
        this.maxPackSize = maxPackSize;
        this.minLightLevel = minLightLevel;
        this.maxLightLevel = maxLightLevel;
        this.spawnInBadWeather = spawnInBadWeather;
        this.spawnDuringDay = spawnDuringDay;
        this.spawnDuringNight = spawnDuringNight;
        this.spawnAllowed = spawnAllowed;
        this.checkBiomeTags = checkBiomeTags;
        this.checkSpawnBlocks = checkSpawnBlocks;
        this.validBiomeTags = validBiomeTags;
        this.validSpawnBlocks = validSpawnBlocks;
        this.availableSpots = availableSpots;
    }

    public String name() {
        return name;
    }

    public SimpleParticleType particleType() {
        return particleType;
    }

    public int weight() {
        return weight;
    }

    public int minPackSize() {
        return minPackSize;
    }

    public int maxPackSize() {
        return maxPackSize;
    }

    public int minLightLevel() {
        return minLightLevel;
    }

    public int maxLightLevel() {
        return maxLightLevel;
    }

    public boolean spawnInBadWeather() {
        return spawnInBadWeather;
    }

    public boolean spawnDuringDay() {
        return spawnDuringDay;
    }

    public boolean spawnDuringNight() {
        return spawnDuringNight;
    }

    public boolean spawnAllowed() {
        return spawnAllowed;
    }

    public void setSpawnAllowed(boolean spawnAllowed) {
        this.spawnAllowed = spawnAllowed;
    }

    public boolean checkBiomeTags() {
        return checkBiomeTags;
    }

    public void setCheckBiomeTags(boolean checkBiomeTags) {
        this.checkBiomeTags = checkBiomeTags;
    }

    public boolean checkSpawnBlocks() {
        return checkSpawnBlocks;
    }

    public void setCheckSpawnBlocks(boolean checkSpawnBlocks) {
        this.checkSpawnBlocks = checkSpawnBlocks;
    }

    public List<TagKey<Biome>> validBiomeTags() {
        return validBiomeTags;
    }

    public List<TagKey<Block>> validSpawnBlocks() {
        return validSpawnBlocks;
    }

    public IntSupplier availableSpots() {
        return availableSpots;
    }

    public static void syncFromConfig() {
        CROW.setSpawnAllowed(ConfigHandler.enableCrowSpawning);
        BLUE_JAY.setSpawnAllowed(ConfigHandler.enableBlueJaySpawning);
        CROW.setCheckBiomeTags(!ConfigHandler.disableCrowSpawnBiomeChecks);
        CROW.setCheckSpawnBlocks(!ConfigHandler.disableCrowSpawnBlockChecks);
        BLUE_JAY.setCheckBiomeTags(!ConfigHandler.disableBlueJaySpawnBiomeChecks);
        BLUE_JAY.setCheckSpawnBlocks(!ConfigHandler.disableBlueJaySpawnBlockChecks);
    }

    public static SpawnData byName(String name) {
        for (SpawnData spawnData : ALL_SPAWNS) {
            if (spawnData.name.equalsIgnoreCase(name)) {
                return spawnData;
            }
        }
        return null;
    }

    public static List<String> names() {
        return ALL_SPAWNS.stream().map(SpawnData::name).toList();
    }

    public static final SpawnData CROW = new SpawnData(
            "crow",
            AtmosphericFauna.CROW,
            30, 3, 9, 8, 15, true, true, true,
            ConfigHandler.enableCrowSpawning,
            !ConfigHandler.disableCrowSpawnBiomeChecks,
            !ConfigHandler.disableCrowSpawnBlockChecks,
            List.of(BiomeTags.IS_OVERWORLD),
            List.of(BlockTags.ANIMALS_SPAWNABLE_ON, BlockTags.DIRT, BlockTags.LEAVES, BlockTags.LOGS, BlockTags.SAND,
                    BlockTags.SNOW, BlockTags.TERRACOTTA, BlockTags.BASE_STONE_OVERWORLD),
            () -> Math.max(0, CrowParticle.getMaxActiveBirds() - CrowParticle.getCount()));

    public static final SpawnData BLUE_JAY = new SpawnData(
            "blue_jay",
            AtmosphericFauna.BLUE_JAY,
            25, 1, 2, 8, 15, false, true, false,
            ConfigHandler.enableBlueJaySpawning,
            !ConfigHandler.disableBlueJaySpawnBiomeChecks,
            !ConfigHandler.disableBlueJaySpawnBlockChecks,
            List.of(BiomeTags.IS_FOREST, BiomeTags.IS_JUNGLE, BiomeTags.IS_TAIGA),
            List.of(BlockTags.ANIMALS_SPAWNABLE_ON, BlockTags.DIRT, BlockTags.LEAVES, BlockTags.LOGS, BlockTags.SNOW,
                    BlockTags.BASE_STONE_OVERWORLD),
            () -> Math.max(0, BlueJayParticle.getMaxActiveBirds() - BlueJayParticle.getCount()));

    public static final List<SpawnData> ALL_SPAWNS = List.of(
            CROW,
            BLUE_JAY
    // Future bird types can be added here
    );
}
