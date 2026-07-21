package nms.atmosphericfauna.spawning;

import nms.atmosphericfauna.AtmosphericFauna;
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

public record SpawnData(
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
        List<TagKey<Biome>> validBiomeTags,
        List<TagKey<Block>> validSpawnBlocks,
        IntSupplier availableSpots) {

    public static final SpawnData CROW = new SpawnData(
            "crow",
            AtmosphericFauna.CROW,
            30, 3, 9, 8, 15, true, true, true,
            List.of(BiomeTags.IS_OVERWORLD),
            List.of(BlockTags.ANIMALS_SPAWNABLE_ON, BlockTags.DIRT, BlockTags.LEAVES, BlockTags.LOGS, BlockTags.SAND, BlockTags.SNOW, BlockTags.TERRACOTTA, BlockTags.BASE_STONE_OVERWORLD),
            () -> Math.max(0, CrowParticle.getMaxActiveBirds() - CrowParticle.getCount())
    );

    public static final SpawnData BLUE_JAY = new SpawnData(
            "blue_jay",
            AtmosphericFauna.BLUE_JAY,
            25, 1, 2, 8, 15, false, true, false,
            List.of(BiomeTags.IS_FOREST, BiomeTags.IS_JUNGLE, BiomeTags.IS_TAIGA),
            List.of(BlockTags.ANIMALS_SPAWNABLE_ON, BlockTags.DIRT, BlockTags.LEAVES, BlockTags.LOGS, BlockTags.SNOW, BlockTags.BASE_STONE_OVERWORLD),
            () -> Math.max(0, BlueJayParticle.getMaxActiveBirds() - BlueJayParticle.getCount())
    );

    public static final List<SpawnData> ALL_SPAWNS = List.of(
            CROW,
            BLUE_JAY
            // Future bird types can be added here
    );

    public static final int TOTAL_WEIGHT = ALL_SPAWNS.stream().mapToInt(SpawnData::weight).sum();
}
