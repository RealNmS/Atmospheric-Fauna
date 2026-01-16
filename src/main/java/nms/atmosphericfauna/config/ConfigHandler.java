package nms.atmosphericfauna.config;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.BaseBirdParticle;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;

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

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            @SuppressWarnings("null")
            ConfigData data = GSON.fromJson(reader, ConfigData.class);

            if (data != null) {
                // Verify that all variables are there
                ConfigData defaults = saveData(); // Generates a ConfigData with current default values
                boolean missing = false;

                try {
                    // Loop through categories (spawning, birds, debug)
                    for (Field categoryField : ConfigData.class.getFields()) {
                        Object loadedCategory = categoryField.get(data);
                        Object defaultCategory = categoryField.get(defaults);

                        // Loop through variables in each category
                        for (Field field : loadedCategory.getClass().getFields()) {
                            // If variable is missing (null), fill it from defaults
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
            public Integer spawnRangeFromPlayer;
            public Integer spawnTickDelay;
            public Integer attemptsPerTick;
            public Integer searchRadius;
        }

        // Birds Category
        public Birds birds = new Birds();

        public static class Birds {
            public Integer maxActiveBirds;
            public Integer maxActiveCrows;
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

        data.spawning.spawnRangeFromPlayer = AmbientSpawning.spawnRangeFromPlayer;
        data.spawning.spawnTickDelay = AmbientSpawning.spawnTickDelay;
        data.spawning.attemptsPerTick = AmbientSpawning.attemptsPerTick;
        data.spawning.searchRadius = AmbientSpawning.searchRadius;

        // Birds Category

        data.birds.maxActiveBirds = BaseBirdParticle.maxActiveBirds;
        data.birds.maxActiveCrows = CrowParticle.maxActiveCrows;

        // Debug Category

        data.debug.debugText = AmbientSpawning.debugText;
        data.debug.debugBirds = BaseBirdParticle.debugText;

        return data;
    }

    private static void loadData(ConfigData data) {

        // Spawning Category

        AmbientSpawning.spawnRangeFromPlayer = data.spawning.spawnRangeFromPlayer;
        AmbientSpawning.spawnTickDelay = data.spawning.spawnTickDelay;
        AmbientSpawning.attemptsPerTick = data.spawning.attemptsPerTick;
        AmbientSpawning.searchRadius = data.spawning.searchRadius;

        // Birds Category

        BaseBirdParticle.maxActiveBirds = data.birds.maxActiveBirds;
        CrowParticle.maxActiveCrows = data.birds.maxActiveCrows;

        // Debug Category

        AmbientSpawning.debugText = data.debug.debugText;
        BaseBirdParticle.debugText = data.debug.debugBirds;
    }
}
