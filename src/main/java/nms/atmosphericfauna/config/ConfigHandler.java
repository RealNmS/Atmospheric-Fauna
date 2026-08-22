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

        public static final boolean ENABLE_BIRD_DISTANCE_FADE_OUT = true;

        public static final int MAX_ACTIVE_BIRDS = 100;
        // Blue Jay
        public static final boolean ENABLE_BLUE_JAY_SPAWNING = true;
        public static final int MAX_ACTIVE_BLUE_JAYS = 10;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS = false;
        public static final int MIN_PACK_SIZE_BLUE_JAY = 1;
        public static final int MAX_PACK_SIZE_BLUE_JAY = 2;
        public static final int MAX_FLOCK_SIZE_BLUE_JAY = 3;
        // Common Swift
        public static final boolean ENABLE_COMMON_SWIFT_SPAWNING = true;
        public static final int MAX_ACTIVE_COMMON_SWIFTS = 12;
        public static final boolean DISABLE_COMMON_SWIFT_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_COMMON_SWIFT_SPAWN_BLOCK_CHECKS = false;
        public static final int MIN_PACK_SIZE_COMMON_SWIFT = 4;
        public static final int MAX_PACK_SIZE_COMMON_SWIFT = 12;
        public static final int MAX_FLOCK_SIZE_COMMON_SWIFT = 12;
        // Crow
        public static final boolean ENABLE_CROW_SPAWNING = true;
        public static final int MAX_ACTIVE_CROWS = 50;
        public static final boolean DISABLE_CROW_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_CROW_SPAWN_BLOCK_CHECKS = false;
        public static final int MIN_PACK_SIZE_CROW = 3;
        public static final int MAX_PACK_SIZE_CROW = 9;
        public static final int MAX_FLOCK_SIZE_CROW = 0;
        // Northern Cardinal
        public static final boolean ENABLE_NORTHERN_CARDINAL_SPAWNING = true;
        public static final int MAX_ACTIVE_NORTHERN_CARDINALS = 10;
        public static final boolean DISABLE_NORTHERN_CARDINAL_SPAWN_BIOME_CHECKS = false;
        public static final boolean DISABLE_NORTHERN_CARDINAL_SPAWN_BLOCK_CHECKS = false;
        public static final int MIN_PACK_SIZE_NORTHERN_CARDINAL = 1;
        public static final int MAX_PACK_SIZE_NORTHERN_CARDINAL = 3;
        public static final int MAX_FLOCK_SIZE_NORTHERN_CARDINAL = 3;

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

    public static boolean enableBirdDistanceFadeOut = Defaults.ENABLE_BIRD_DISTANCE_FADE_OUT;

    public static int maxActiveBirds = Defaults.MAX_ACTIVE_BIRDS;
    // Blue Jay
    public static boolean enableBlueJaySpawning = Defaults.ENABLE_BLUE_JAY_SPAWNING;
    public static int maxActiveBlueJays = Defaults.MAX_ACTIVE_BLUE_JAYS;
    public static boolean disableBlueJaySpawnBiomeChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS;
    public static boolean disableBlueJaySpawnBlockChecks = Defaults.DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS;
    public static int minPackSizeBlueJay = Defaults.MIN_PACK_SIZE_BLUE_JAY;
    public static int maxPackSizeBlueJay = Defaults.MAX_PACK_SIZE_BLUE_JAY;
    public static int maxFlockSizeBlueJay = Defaults.MAX_FLOCK_SIZE_BLUE_JAY;
    // Common Swift
    public static boolean enableCommonSwiftSpawning = Defaults.ENABLE_COMMON_SWIFT_SPAWNING;
    public static int maxActiveCommonSwifts = Defaults.MAX_ACTIVE_COMMON_SWIFTS;
    public static boolean disableCommonSwiftSpawnBiomeChecks = Defaults.DISABLE_COMMON_SWIFT_SPAWN_BIOME_CHECKS;
    public static boolean disableCommonSwiftSpawnBlockChecks = Defaults.DISABLE_COMMON_SWIFT_SPAWN_BLOCK_CHECKS;
    public static int minPackSizeCommonSwift = Defaults.MIN_PACK_SIZE_COMMON_SWIFT;
    public static int maxPackSizeCommonSwift = Defaults.MAX_PACK_SIZE_COMMON_SWIFT;
    public static int maxFlockSizeCommonSwift = Defaults.MAX_FLOCK_SIZE_COMMON_SWIFT;
    // Crow
    public static boolean enableCrowSpawning = Defaults.ENABLE_CROW_SPAWNING;
    public static int maxActiveCrows = Defaults.MAX_ACTIVE_CROWS;
    public static boolean disableCrowSpawnBiomeChecks = Defaults.DISABLE_CROW_SPAWN_BIOME_CHECKS;
    public static boolean disableCrowSpawnBlockChecks = Defaults.DISABLE_CROW_SPAWN_BLOCK_CHECKS;
    public static int minPackSizeCrow = Defaults.MIN_PACK_SIZE_CROW;
    public static int maxPackSizeCrow = Defaults.MAX_PACK_SIZE_CROW;
    public static int maxFlockSizeCrow = Defaults.MAX_FLOCK_SIZE_CROW;
    // Northern Cardinal
    public static boolean enableNorthernCardinalSpawning = Defaults.ENABLE_NORTHERN_CARDINAL_SPAWNING;
    public static int maxActiveNorthernCardinals = Defaults.MAX_ACTIVE_NORTHERN_CARDINALS;
    public static boolean disableNorthernCardinalSpawnBiomeChecks = Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BIOME_CHECKS;
    public static boolean disableNorthernCardinalSpawnBlockChecks = Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BLOCK_CHECKS;
    public static int minPackSizeNorthernCardinal = Defaults.MIN_PACK_SIZE_NORTHERN_CARDINAL;
    public static int maxPackSizeNorthernCardinal = Defaults.MAX_PACK_SIZE_NORTHERN_CARDINAL;
    public static int maxFlockSizeNorthernCardinal = Defaults.MAX_FLOCK_SIZE_NORTHERN_CARDINAL;

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

        // Visuals Category
        public Visuals visuals = new Visuals();

        public static class Visuals {
            public Boolean enableBirdDistanceFadeOut;
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
            public Integer minPackSizeBlueJay;
            public Integer maxPackSizeBlueJay;
            public Integer maxFlockSizeBlueJay;
            // Common Swift
            public Boolean enableCommonSwiftSpawning;
            public Integer maxActiveCommonSwifts;
            public Boolean disableCommonSwiftSpawnBiomeChecks;
            public Boolean disableCommonSwiftSpawnBlockChecks;
            public Integer minPackSizeCommonSwift;
            public Integer maxPackSizeCommonSwift;
            public Integer maxFlockSizeCommonSwift;
            // Crow
            public Boolean enableCrowSpawning;
            public Integer maxActiveCrows;
            public Boolean disableCrowSpawnBiomeChecks;
            public Boolean disableCrowSpawnBlockChecks;
            public Integer minPackSizeCrow;
            public Integer maxPackSizeCrow;
            public Integer maxFlockSizeCrow;
            // Northern Cardinal
            public Boolean enableNorthernCardinalSpawning;
            public Integer maxActiveNorthernCardinals;
            public Boolean disableNorthernCardinalSpawnBiomeChecks;
            public Boolean disableNorthernCardinalSpawnBlockChecks;
            public Integer minPackSizeNorthernCardinal;
            public Integer maxPackSizeNorthernCardinal;
            public Integer maxFlockSizeNorthernCardinal;
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

        // Visuals Category
        data.visuals.enableBirdDistanceFadeOut = enableBirdDistanceFadeOut;

        // Birds Category
        data.birds.maxActiveBirds = maxActiveBirds;
        // Blue Jay
        data.birds.enableBlueJaySpawning = enableBlueJaySpawning;
        data.birds.maxActiveBlueJays = maxActiveBlueJays;
        data.birds.disableBlueJaySpawnBiomeChecks = disableBlueJaySpawnBiomeChecks;
        data.birds.disableBlueJaySpawnBlockChecks = disableBlueJaySpawnBlockChecks;
        data.birds.minPackSizeBlueJay = minPackSizeBlueJay;
        data.birds.maxPackSizeBlueJay = maxPackSizeBlueJay;
        data.birds.maxFlockSizeBlueJay = maxFlockSizeBlueJay;
        // Common Swift
        data.birds.enableCommonSwiftSpawning = enableCommonSwiftSpawning;
        data.birds.maxActiveCommonSwifts = maxActiveCommonSwifts;
        data.birds.disableCommonSwiftSpawnBiomeChecks = disableCommonSwiftSpawnBiomeChecks;
        data.birds.disableCommonSwiftSpawnBlockChecks = disableCommonSwiftSpawnBlockChecks;
        data.birds.minPackSizeCommonSwift = minPackSizeCommonSwift;
        data.birds.maxPackSizeCommonSwift = maxPackSizeCommonSwift;
        data.birds.maxFlockSizeCommonSwift = maxFlockSizeCommonSwift;
        // Crow
        data.birds.enableCrowSpawning = enableCrowSpawning;
        data.birds.maxActiveCrows = maxActiveCrows;
        data.birds.disableCrowSpawnBiomeChecks = disableCrowSpawnBiomeChecks;
        data.birds.disableCrowSpawnBlockChecks = disableCrowSpawnBlockChecks;
        data.birds.minPackSizeCrow = minPackSizeCrow;
        data.birds.maxPackSizeCrow = maxPackSizeCrow;
        data.birds.maxFlockSizeCrow = maxFlockSizeCrow;
        // Northern Cardinal
        data.birds.enableNorthernCardinalSpawning = enableNorthernCardinalSpawning;
        data.birds.maxActiveNorthernCardinals = maxActiveNorthernCardinals;
        data.birds.disableNorthernCardinalSpawnBiomeChecks = disableNorthernCardinalSpawnBiomeChecks;
        data.birds.disableNorthernCardinalSpawnBlockChecks = disableNorthernCardinalSpawnBlockChecks;
        data.birds.minPackSizeNorthernCardinal = minPackSizeNorthernCardinal;
        data.birds.maxPackSizeNorthernCardinal = maxPackSizeNorthernCardinal;
        data.birds.maxFlockSizeNorthernCardinal = maxFlockSizeNorthernCardinal;

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

        // Visuals Category
        enableBirdDistanceFadeOut = data.visuals.enableBirdDistanceFadeOut;

        // Birds Category
        maxActiveBirds = data.birds.maxActiveBirds;
        // Blue Jay
        enableBlueJaySpawning = data.birds.enableBlueJaySpawning;
        maxActiveBlueJays = data.birds.maxActiveBlueJays;
        disableBlueJaySpawnBiomeChecks = data.birds.disableBlueJaySpawnBiomeChecks;
        disableBlueJaySpawnBlockChecks = data.birds.disableBlueJaySpawnBlockChecks;
        minPackSizeBlueJay = data.birds.minPackSizeBlueJay;
        maxPackSizeBlueJay = data.birds.maxPackSizeBlueJay;
        maxFlockSizeBlueJay = data.birds.maxFlockSizeBlueJay;
        // Common Swift
        enableCommonSwiftSpawning = data.birds.enableCommonSwiftSpawning;
        maxActiveCommonSwifts = data.birds.maxActiveCommonSwifts;
        disableCommonSwiftSpawnBiomeChecks = data.birds.disableCommonSwiftSpawnBiomeChecks;
        disableCommonSwiftSpawnBlockChecks = data.birds.disableCommonSwiftSpawnBlockChecks;
        minPackSizeCommonSwift = data.birds.minPackSizeCommonSwift;
        maxPackSizeCommonSwift = data.birds.maxPackSizeCommonSwift;
        maxFlockSizeCommonSwift = data.birds.maxFlockSizeCommonSwift;
        // Crow
        enableCrowSpawning = data.birds.enableCrowSpawning;
        maxActiveCrows = data.birds.maxActiveCrows;
        disableCrowSpawnBiomeChecks = data.birds.disableCrowSpawnBiomeChecks;
        disableCrowSpawnBlockChecks = data.birds.disableCrowSpawnBlockChecks;
        minPackSizeCrow = data.birds.minPackSizeCrow;
        maxPackSizeCrow = data.birds.maxPackSizeCrow;
        maxFlockSizeCrow = data.birds.maxFlockSizeCrow;
        // Northern Cardinal
        enableNorthernCardinalSpawning = data.birds.enableNorthernCardinalSpawning;
        maxActiveNorthernCardinals = data.birds.maxActiveNorthernCardinals;
        disableNorthernCardinalSpawnBiomeChecks = data.birds.disableNorthernCardinalSpawnBiomeChecks;
        disableNorthernCardinalSpawnBlockChecks = data.birds.disableNorthernCardinalSpawnBlockChecks;
        minPackSizeNorthernCardinal = data.birds.minPackSizeNorthernCardinal;
        maxPackSizeNorthernCardinal = data.birds.maxPackSizeNorthernCardinal;
        maxFlockSizeNorthernCardinal = data.birds.maxFlockSizeNorthernCardinal;

        // Debug Category
        debugTextSpawning = data.debug.debugText;
        debugTextBirds = data.debug.debugBirds;
        enableDebugScreenOnJoin = data.debug.enableDebugScreenOnJoin;

        nms.atmosphericfauna.spawning.SpawnData.syncFromConfig();
    }
}
