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
        public static final boolean ENABLE_MIDAIR_BORDER_SPAWNING = false;
        public static final int SPAWN_RANGE_FROM_PLAYER = 96;
        public static final int SPAWN_TICK_DELAY = 200;
        public static final int ATTEMPTS_PER_TICK = 15;
        public static final int SEARCH_RADIUS = 12;
        public static final boolean SPAWN_BELOW_SEA_LEVEL = false;
        public static final boolean SPAWN_AROUND_SPECTATORS = true;

        public static final int MAX_ACTIVE_BIRDS = 100;
        // Blue Jay
        public static final boolean ENABLE_BLUE_JAY_SPAWNING = true;
        public static final int MAX_ACTIVE_BLUE_JAYS = 10;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS = false;
        // Common Swift
        public static final boolean ENABLE_COMMON_SWIFT_SPAWNING = true;
        public static final int MAX_ACTIVE_COMMON_SWIFTS = 12;
        public static final boolean DISABLE_COMMON_SWIFT_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_COMMON_SWIFT_SPAWN_BLOCK_CHECKS = false;
        // Crow
        public static final boolean ENABLE_CROW_SPAWNING = true;
        public static final int MAX_ACTIVE_CROWS = 50;
        public static final boolean DISABLE_CROW_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_CROW_SPAWN_BLOCK_CHECKS = false;
        // Northern Cardinal
        public static final boolean ENABLE_NORTHERN_CARDINAL_SPAWNING = true;
        public static final int MAX_ACTIVE_NORTHERN_CARDINALS = 10;
        public static final boolean DISABLE_NORTHERN_CARDINAL_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_NORTHERN_CARDINAL_SPAWN_BLOCK_CHECKS = false;

        public static final boolean DEBUG_TEXT_SPAWNING = false;
        public static final boolean DEBUG_TEXT_BIRDS = false;
        public static final boolean ENABLE_DEBUG_SCREEN_ON_JOIN = false;
    }

    public static boolean enableChunkLoadSpawning = Defaults.ENABLE_CHUNK_LOAD_SPAWNING;
    public static boolean enableAmbientSpawning = Defaults.ENABLE_AMBIENT_SPAWNING;
    public static boolean enableMidairBorderSpawning = Defaults.ENABLE_MIDAIR_BORDER_SPAWNING;
    public static int spawnRangeFromPlayer = Defaults.SPAWN_RANGE_FROM_PLAYER;
    public static int spawnTickDelay = Defaults.SPAWN_TICK_DELAY;
    public static int attemptsPerTick = Defaults.ATTEMPTS_PER_TICK;
    public static int searchRadius = Defaults.SEARCH_RADIUS;
    public static boolean spawnBelowSeaLevel = Defaults.SPAWN_BELOW_SEA_LEVEL;
    public static boolean spawnAroundSpectators = Defaults.SPAWN_AROUND_SPECTATORS;

    public static int maxActiveBirds = Defaults.MAX_ACTIVE_BIRDS;
    // Blue Jay
    public static boolean enableBlueJaySpawning = Defaults.ENABLE_BLUE_JAY_SPAWNING;
    public static int maxActiveBlueJays = Defaults.MAX_ACTIVE_BLUE_JAYS;
    public static boolean disableBlueJaySpawnBiomeChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS;
    public static boolean disableBlueJaySpawnBlockChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS;
    // Common Swift
    public static boolean enableCommonSwiftSpawning = Defaults.ENABLE_COMMON_SWIFT_SPAWNING;
    public static int maxActiveCommonSwifts = Defaults.MAX_ACTIVE_COMMON_SWIFTS;
    public static boolean disableCommonSwiftSpawnBiomeChecks = Defaults.DISABLE_COMMON_SWIFT_SPAWN_BIOME_CHECKS;
    public static boolean disableCommonSwiftSpawnBlockChecks = Defaults.DISABLE_COMMON_SWIFT_SPAWN_BLOCK_CHECKS;
    // Crow
    public static boolean enableCrowSpawning = Defaults.ENABLE_CROW_SPAWNING;
    public static int maxActiveCrows = Defaults.MAX_ACTIVE_CROWS;
    public static boolean disableCrowSpawnBiomeChecks = Defaults.DISABLE_CROW_SPAWN_BIOME_CHECKS;
    public static boolean disableCrowSpawnBlockChecks = Defaults.DISABLE_CROW_SPAWN_BLOCK_CHECKS;
    // Northern Cardinal
    public static boolean enableNorthernCardinalSpawning = Defaults.ENABLE_NORTHERN_CARDINAL_SPAWNING;
    public static int maxActiveNorthernCardinals = Defaults.MAX_ACTIVE_NORTHERN_CARDINALS;
    public static boolean disableNorthernCardinalSpawnBiomeChecks = Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BIOME_CHECKS;
    public static boolean disableNorthernCardinalSpawnBlockChecks = Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BLOCK_CHECKS;

    public static boolean debugTextSpawning = Defaults.DEBUG_TEXT_SPAWNING;
    public static boolean debugTextBirds = Defaults.DEBUG_TEXT_BIRDS;
    public static boolean enableDebugScreenOnJoin = Defaults.ENABLE_DEBUG_SCREEN_ON_JOIN;

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
            public Boolean enableMidairBorderSpawning;
            public Integer spawnRangeFromPlayer;
            public Integer spawnTickDelay;
            public Integer attemptsPerTick;
            public Integer searchRadius;
            public Boolean spawnBelowSeaLevel;
            public Boolean spawnAroundSpectators;
        }

        // Birds Category
        public Birds birds = new Birds();

        public static class Birds {
            public Integer maxActiveBirds;
            // Blue Jay
            public Boolean enableBlueJaySpawning;
            public Integer maxActiveBlueJays;
            public Boolean disableBlueJaySpawnBiomeChecks;
            public Boolean disableBlueJaySpawnBlockChecks;
            // Common Swift
            public Boolean enableCommonSwiftSpawning;
            public Integer maxActiveCommonSwifts;
            public Boolean disableCommonSwiftSpawnBiomeChecks;
            public Boolean disableCommonSwiftSpawnBlockChecks;
            // Crow
            public Boolean enableCrowSpawning;
            public Integer maxActiveCrows;
            public Boolean disableCrowSpawnBiomeChecks;
            public Boolean disableCrowSpawnBlockChecks;
            // Northern Cardinal
            public Boolean enableNorthernCardinalSpawning;
            public Integer maxActiveNorthernCardinals;
            public Boolean disableNorthernCardinalSpawnBiomeChecks;
            public Boolean disableNorthernCardinalSpawnBlockChecks;
        }

        // Debug Category
        public Debug debug = new Debug();

        public static class Debug {
            public Boolean debugText;
            public Boolean debugBirds;
            public Boolean enableDebugScreenOnJoin;
        }

    }

    private static ConfigData saveData() {
        ConfigData data = new ConfigData();

        // Spawning Category
        data.spawning.enableChunkLoadSpawning = enableChunkLoadSpawning;
        data.spawning.enableAmbientSpawning = enableAmbientSpawning;
        data.spawning.enableMidairBorderSpawning = enableMidairBorderSpawning;
        data.spawning.spawnRangeFromPlayer = spawnRangeFromPlayer;
        data.spawning.spawnTickDelay = spawnTickDelay;
        data.spawning.attemptsPerTick = attemptsPerTick;
        data.spawning.searchRadius = searchRadius;
        data.spawning.spawnBelowSeaLevel = spawnBelowSeaLevel;
        data.spawning.spawnAroundSpectators = spawnAroundSpectators;

        // Birds Category
        data.birds.maxActiveBirds = maxActiveBirds;
        // Blue Jay
        data.birds.enableBlueJaySpawning = enableBlueJaySpawning;
        data.birds.maxActiveBlueJays = maxActiveBlueJays;
        data.birds.disableBlueJaySpawnBiomeChecks = disableBlueJaySpawnBiomeChecks;
        data.birds.disableBlueJaySpawnBlockChecks = disableBlueJaySpawnBlockChecks;
        // Common Swift
        data.birds.enableCommonSwiftSpawning = enableCommonSwiftSpawning;
        data.birds.maxActiveCommonSwifts = maxActiveCommonSwifts;
        data.birds.disableCommonSwiftSpawnBiomeChecks = disableCommonSwiftSpawnBiomeChecks;
        data.birds.disableCommonSwiftSpawnBlockChecks = disableCommonSwiftSpawnBlockChecks;
        // Crow
        data.birds.enableCrowSpawning = enableCrowSpawning;
        data.birds.maxActiveCrows = maxActiveCrows;
        data.birds.disableCrowSpawnBiomeChecks = disableCrowSpawnBiomeChecks;
        data.birds.disableCrowSpawnBlockChecks = disableCrowSpawnBlockChecks;
        // Northern Cardinal
        data.birds.enableNorthernCardinalSpawning = enableNorthernCardinalSpawning;
        data.birds.maxActiveNorthernCardinals = maxActiveNorthernCardinals;
        data.birds.disableNorthernCardinalSpawnBiomeChecks = disableNorthernCardinalSpawnBiomeChecks;
        data.birds.disableNorthernCardinalSpawnBlockChecks = disableNorthernCardinalSpawnBlockChecks;

        // Debug Category
        data.debug.debugText = debugTextSpawning;
        data.debug.debugBirds = debugTextBirds;
        data.debug.enableDebugScreenOnJoin = enableDebugScreenOnJoin;

        return data;
    }

    private static void loadData(ConfigData data) {
        // Spawning Category
        enableChunkLoadSpawning = data.spawning.enableChunkLoadSpawning;
        enableAmbientSpawning = data.spawning.enableAmbientSpawning;
        enableMidairBorderSpawning = data.spawning.enableMidairBorderSpawning;
        spawnRangeFromPlayer = data.spawning.spawnRangeFromPlayer;
        spawnTickDelay = data.spawning.spawnTickDelay;
        attemptsPerTick = data.spawning.attemptsPerTick;
        searchRadius = data.spawning.searchRadius;
        spawnBelowSeaLevel = data.spawning.spawnBelowSeaLevel;
        spawnAroundSpectators = data.spawning.spawnAroundSpectators;

        // Birds Category
        maxActiveBirds = data.birds.maxActiveBirds;
        // Blue Jay
        enableBlueJaySpawning = data.birds.enableBlueJaySpawning;
        maxActiveBlueJays = data.birds.maxActiveBlueJays;
        disableBlueJaySpawnBiomeChecks = data.birds.disableBlueJaySpawnBiomeChecks;
        disableBlueJaySpawnBlockChecks = data.birds.disableBlueJaySpawnBlockChecks;
        // Common Swift
        enableCommonSwiftSpawning = data.birds.enableCommonSwiftSpawning;
        maxActiveCommonSwifts = data.birds.maxActiveCommonSwifts;
        disableCommonSwiftSpawnBiomeChecks = data.birds.disableCommonSwiftSpawnBiomeChecks;
        disableCommonSwiftSpawnBlockChecks = data.birds.disableCommonSwiftSpawnBlockChecks;
        // Crow
        enableCrowSpawning = data.birds.enableCrowSpawning;
        maxActiveCrows = data.birds.maxActiveCrows;
        disableCrowSpawnBiomeChecks = data.birds.disableCrowSpawnBiomeChecks;
        disableCrowSpawnBlockChecks = data.birds.disableCrowSpawnBlockChecks;
        // Northern Cardinal
        enableNorthernCardinalSpawning = data.birds.enableNorthernCardinalSpawning;
        maxActiveNorthernCardinals = data.birds.maxActiveNorthernCardinals;
        disableNorthernCardinalSpawnBiomeChecks = data.birds.disableNorthernCardinalSpawnBiomeChecks;
        disableNorthernCardinalSpawnBlockChecks = data.birds.disableNorthernCardinalSpawnBlockChecks;

        // Debug Category
        debugTextSpawning = data.debug.debugText;
        debugTextBirds = data.debug.debugBirds;
        enableDebugScreenOnJoin = data.debug.enableDebugScreenOnJoin;

        nms.atmosphericfauna.spawning.SpawnData.syncFromConfig();
    }
}
