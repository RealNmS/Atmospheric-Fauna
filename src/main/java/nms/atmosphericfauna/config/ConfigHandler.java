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
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                AmbientSpawning.spawnRangeFromPlayer = data.spawnRangeFromPlayer;
                AmbientSpawning.spawnTickDelay = data.spawnTickDelay;
                AmbientSpawning.attemptsPerTick = data.attemptsPerTick;
                AmbientSpawning.searchRadius = data.searchRadius;
                AmbientSpawning.debugText = data.debugText;
                CrowParticle.maxActiveCrows = data.maxActiveCrows;
            }
        } catch (IOException e) {
            AtmosphericFauna.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.spawnRangeFromPlayer = AmbientSpawning.spawnRangeFromPlayer;
        data.spawnTickDelay = AmbientSpawning.spawnTickDelay;
        data.attemptsPerTick = AmbientSpawning.attemptsPerTick;
        data.searchRadius = AmbientSpawning.searchRadius;
        data.debugText = AmbientSpawning.debugText;
        data.maxActiveCrows = CrowParticle.maxActiveCrows;

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            AtmosphericFauna.LOGGER.error("Failed to save config", e);
        }
    }

    public static class ConfigData {
        public int spawnRangeFromPlayer = 96;
        public int spawnTickDelay = 100;
        public int attemptsPerTick = 15;
        public int searchRadius = 12;
        public boolean debugText = false;
        public int maxActiveCrows = 120;
    }
}
