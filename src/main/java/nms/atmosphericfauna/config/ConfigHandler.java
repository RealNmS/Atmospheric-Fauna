package nms.atmosphericfauna.config;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
                loadData(data);
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
            public int spawnRangeFromPlayer;
            public int spawnTickDelay;
            public int attemptsPerTick;
            public int searchRadius;
        }

        // Birds Category
        public Birds birds = new Birds();

        public static class Birds {
            public int maxActiveCrows;
        }

        // Debug Category
        public Debug debug = new Debug();

        public static class Debug {
            public boolean debugText;
            public boolean debugCrows;
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

        data.birds.maxActiveCrows = CrowParticle.maxActiveCrows;

        // Debug Category

        data.debug.debugText = AmbientSpawning.debugText;
        data.debug.debugCrows = CrowParticle.debugText;

        return data;
    }

    private static void loadData(ConfigData data) {

        // Spawning Category

        AmbientSpawning.spawnRangeFromPlayer = data.spawning.spawnRangeFromPlayer;
        AmbientSpawning.spawnTickDelay = data.spawning.spawnTickDelay;
        AmbientSpawning.attemptsPerTick = data.spawning.attemptsPerTick;
        AmbientSpawning.searchRadius = data.spawning.searchRadius;

        // Birds Category

        CrowParticle.maxActiveCrows = data.birds.maxActiveCrows;

        // Debug Category

        AmbientSpawning.debugText = data.debug.debugText;
        CrowParticle.debugText = data.debug.debugCrows;
    }
}
