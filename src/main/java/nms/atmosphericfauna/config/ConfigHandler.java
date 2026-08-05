package nms.atmosphericfauna.config;

import nms.atmosphericfauna.AtmosphericFauna;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigHandler {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(AtmosphericFauna.MOD_ID + ".json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Defaults {
        public static final boolean ENABLE_CHUNK_LOAD_SPAWNING = true;
        public static final boolean ENABLE_AMBIENT_SPAWNING = true;
        public static final int SPAWN_RANGE_FROM_PLAYER = 96;
        public static final int SPAWN_TICK_DELAY = 200;
        public static final int ATTEMPTS_PER_TICK = 15;
        public static final int SEARCH_RADIUS = 12;
        public static final boolean SPAWN_BELOW_SEA_LEVEL = false;
        public static final boolean DEBUG_TEXT_SPAWNING = false;

        public static final int MAX_ACTIVE_BIRDS = 100;
        public static final int MAX_ACTIVE_CROWS = 50;
        public static final int MAX_ACTIVE_BLUE_JAYS = 10;
        public static final boolean ENABLE_CROW_SPAWNING = true;
        public static final boolean ENABLE_BLUE_JAY_SPAWNING = true;
        public static final boolean DISABLE_CROW_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_CROW_SPAWN_BLOCK_CHECKS = false;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS = false;
        public static final boolean DEBUG_TEXT_BIRDS = false;
    }

    public static boolean enableChunkLoadSpawning = Defaults.ENABLE_CHUNK_LOAD_SPAWNING;
    public static boolean enableAmbientSpawning = Defaults.ENABLE_AMBIENT_SPAWNING;
    public static int spawnRangeFromPlayer = Defaults.SPAWN_RANGE_FROM_PLAYER;
    public static int spawnTickDelay = Defaults.SPAWN_TICK_DELAY;
    public static int attemptsPerTick = Defaults.ATTEMPTS_PER_TICK;
    public static int searchRadius = Defaults.SEARCH_RADIUS;
    public static boolean spawnBelowSeaLevel = Defaults.SPAWN_BELOW_SEA_LEVEL;
    public static boolean debugTextSpawning = Defaults.DEBUG_TEXT_SPAWNING;

    public static int maxActiveBirds = Defaults.MAX_ACTIVE_BIRDS;
    public static int maxActiveCrows = Defaults.MAX_ACTIVE_CROWS;
    public static int maxActiveBlueJays = Defaults.MAX_ACTIVE_BLUE_JAYS;
    public static boolean enableCrowSpawning = Defaults.ENABLE_CROW_SPAWNING;
    public static boolean enableBlueJaySpawning = Defaults.ENABLE_BLUE_JAY_SPAWNING;
    public static boolean disableCrowSpawnBiomeChecks = Defaults.DISABLE_CROW_SPAWN_BIOME_CHECKS;
    public static boolean disableCrowSpawnBlockChecks = Defaults.DISABLE_CROW_SPAWN_BLOCK_CHECKS;
    public static boolean disableBlueJaySpawnBiomeChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS;
    public static boolean disableBlueJaySpawnBlockChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS;
    public static boolean debugTextBirds = Defaults.DEBUG_TEXT_BIRDS;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);

            if (data != null) {
                ConfigData defaults = saveData();
                boolean missing = false;

                try {
                    for (Field categoryField : ConfigData.class.getFields()) {
                        Object loadedCategory = categoryField.get(data);
                        Object defaultCategory = categoryField.get(defaults);

                        for (Field field : loadedCategory.getClass().getFields()) {
                            if (field.get(loadedCategory) == null) {
                                field.set(loadedCategory, field.get(defaultCategory));
                                missing = true;
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    AtmosphericFauna.LOGGER.error("Failed to verify config integrity", e);
                }

                loadData(data);

                if (missing) {
                    save();
                }
            }
        } catch (IOException e) {
            AtmosphericFauna.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        ConfigData data = saveData();

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
            nms.atmosphericfauna.spawning.SpawnData.syncFromConfig();
        } catch (IOException e) {
            AtmosphericFauna.LOGGER.error("Failed to save config", e);
        }
    }

    public static class ConfigData {

        // Spawning Category
        public Spawning spawning = new Spawning();

        public static class Spawning {
            public Boolean enableChunkLoadSpawning;
            public Boolean enableAmbientSpawning;
            public Integer spawnRangeFromPlayer;
            public Integer spawnTickDelay;
            public Integer attemptsPerTick;
            public Integer searchRadius;
            public Boolean spawnBelowSeaLevel;
        }

        // Birds Category
        public Birds birds = new Birds();

        public static class Birds {
            public Integer maxActiveBirds;
            public Integer maxActiveCrows;
            public Integer maxActiveBlueJays;
            public Boolean enableCrowSpawning;
            public Boolean enableBlueJaySpawning;
            public Boolean disableCrowSpawnBiomeChecks;
            public Boolean disableCrowSpawnBlockChecks;
            public Boolean disableBlueJaySpawnBiomeChecks;
            public Boolean disableBlueJaySpawnBlockChecks;
        }

        // Debug Category
        public Debug debug = new Debug();

        public static class Debug {
            public Boolean debugText;
            public Boolean debugBirds;
        }

    }

    private static ConfigData saveData() {
        ConfigData data = new ConfigData();

        // Spawning Category
        data.spawning.enableChunkLoadSpawning = enableChunkLoadSpawning;
        data.spawning.enableAmbientSpawning = enableAmbientSpawning;
        data.spawning.spawnRangeFromPlayer = spawnRangeFromPlayer;
        data.spawning.spawnTickDelay = spawnTickDelay;
        data.spawning.attemptsPerTick = attemptsPerTick;
        data.spawning.searchRadius = searchRadius;
        data.spawning.spawnBelowSeaLevel = spawnBelowSeaLevel;

        // Birds Category
        data.birds.maxActiveBirds = maxActiveBirds;
        data.birds.maxActiveCrows = maxActiveCrows;
        data.birds.maxActiveBlueJays = maxActiveBlueJays;
        data.birds.enableCrowSpawning = enableCrowSpawning;
        data.birds.enableBlueJaySpawning = enableBlueJaySpawning;
        data.birds.disableCrowSpawnBiomeChecks = disableCrowSpawnBiomeChecks;
        data.birds.disableCrowSpawnBlockChecks = disableCrowSpawnBlockChecks;
        data.birds.disableBlueJaySpawnBiomeChecks = disableBlueJaySpawnBiomeChecks;
        data.birds.disableBlueJaySpawnBlockChecks = disableBlueJaySpawnBlockChecks;

        // Debug Category
        data.debug.debugText = debugTextSpawning;
        data.debug.debugBirds = debugTextBirds;

        return data;
    }

    private static void loadData(ConfigData data) {
        // Spawning Category
        enableChunkLoadSpawning = data.spawning.enableChunkLoadSpawning;
        enableAmbientSpawning = data.spawning.enableAmbientSpawning;
        spawnRangeFromPlayer = data.spawning.spawnRangeFromPlayer;
        spawnTickDelay = data.spawning.spawnTickDelay;
        attemptsPerTick = data.spawning.attemptsPerTick;
        searchRadius = data.spawning.searchRadius;
        spawnBelowSeaLevel = data.spawning.spawnBelowSeaLevel;

        // Birds Category
        maxActiveBirds = data.birds.maxActiveBirds;
        maxActiveCrows = data.birds.maxActiveCrows;
        maxActiveBlueJays = data.birds.maxActiveBlueJays;
        enableCrowSpawning = data.birds.enableCrowSpawning;
        enableBlueJaySpawning = data.birds.enableBlueJaySpawning;
        disableCrowSpawnBiomeChecks = data.birds.disableCrowSpawnBiomeChecks;
        disableCrowSpawnBlockChecks = data.birds.disableCrowSpawnBlockChecks;
        disableBlueJaySpawnBiomeChecks = data.birds.disableBlueJaySpawnBiomeChecks;
        disableBlueJaySpawnBlockChecks = data.birds.disableBlueJaySpawnBlockChecks;

        // Debug Category
        debugTextSpawning = data.debug.debugText;
        debugTextBirds = data.debug.debugBirds;

        nms.atmosphericfauna.spawning.SpawnData.syncFromConfig();
    }
}
