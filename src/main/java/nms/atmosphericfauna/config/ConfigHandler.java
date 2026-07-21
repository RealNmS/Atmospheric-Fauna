package nms.atmosphericfauna.config;

import nms.atmosphericfauna.AtmosphericFauna;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigHandler {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(AtmosphericFauna.MOD_ID + ".json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enableChunkLoadSpawning = true;
    public static boolean enableAmbientSpawning = true;
    public static int spawnRangeFromPlayer = 96;
    public static int spawnTickDelay = 200;
    public static int attemptsPerTick = 15;
    public static int searchRadius = 12;
    public static boolean spawnBelowSeaLevel = false;
    public static boolean debugTextSpawning = false;

    public static int maxActiveBirds = 100;
    public static int maxActiveCrows = 50;
    public static int maxActiveBlueJays = 10;
    public static boolean debugTextBirds = false;

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

        // Debug Category
        debugTextSpawning = data.debug.debugText;
        debugTextBirds = data.debug.debugBirds;
    }
}
