package nms.atmosphericfauna.modmenu;

import nms.atmosphericfauna.config.ConfigHandler;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuConfig {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.atmosphericfauna.config"))
                .setSavingRunnable(ConfigHandler::save);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // MARK: --- SPAWNING ---

        ConfigCategory spawning = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.spawning"));

        spawning.addEntry(entryBuilder
                .startBooleanToggle(Component
                        .translatable("option.atmosphericfauna.chunk_load_spawning"),
                        ConfigHandler.enableChunkLoadSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_CHUNK_LOAD_SPAWNING)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.chunk_load_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableChunkLoadSpawning = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startBooleanToggle(Component
                        .translatable("option.atmosphericfauna.ambient_spawning"),
                        ConfigHandler.enableAmbientSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_AMBIENT_SPAWNING)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.ambient_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableAmbientSpawning = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component
                        .translatable("option.atmosphericfauna.spawn_range_from_player"),
                        ConfigHandler.spawnRangeFromPlayer)
                .setDefaultValue(ConfigHandler.Defaults.SPAWN_RANGE_FROM_PLAYER)
                .setMin(32)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.spawn_range_from_player.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.spawnRangeFromPlayer = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.spawn_tick_delay"),
                        ConfigHandler.spawnTickDelay)
                .setDefaultValue(ConfigHandler.Defaults.SPAWN_TICK_DELAY)
                .setMin(1)
                .setTooltip(Component.translatable("option.atmosphericfauna.spawn_tick_delay.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.spawnTickDelay = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.attempts_per_tick"),
                        ConfigHandler.attemptsPerTick)
                .setDefaultValue(ConfigHandler.Defaults.ATTEMPTS_PER_TICK)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.attempts_per_tick.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.attemptsPerTick = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.search_radius"),
                        ConfigHandler.searchRadius)
                .setDefaultValue(ConfigHandler.Defaults.SEARCH_RADIUS)
                .setMin(1)
                .setTooltip(Component.translatable("option.atmosphericfauna.search_radius.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.searchRadius = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("option.atmosphericfauna.spawn_below_sea_level"),
                        ConfigHandler.spawnBelowSeaLevel)
                .setDefaultValue(ConfigHandler.Defaults.SPAWN_BELOW_SEA_LEVEL)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.spawn_below_sea_level.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.spawnBelowSeaLevel = newValue)
                .build());

        // MARK: --- BIRDS ---

        ConfigCategory birds = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.birds"));

        birds.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.max_active_birds"),
                        ConfigHandler.maxActiveBirds)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_BIRDS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.max_active_birds.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveBirds = newValue)
                .build());

        SubCategoryBuilder crows = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.crows"));

        crows.add(entryBuilder
                .startBooleanToggle(Component.translatable("option.atmosphericfauna.enable_crow_spawning"),
                        ConfigHandler.enableCrowSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_CROW_SPAWNING)
                .setTooltip(Component.translatable("option.atmosphericfauna.enable_crow_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableCrowSpawning = newValue)
                .build());

        crows.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.max_active_crows"),
                        ConfigHandler.maxActiveCrows)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_CROWS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.max_active_crows.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveCrows = newValue)
                .build());

        birds.addEntry(crows.build());

        SubCategoryBuilder blueJays = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.blue_jays"));

        blueJays.add(entryBuilder
                .startBooleanToggle(Component.translatable("option.atmosphericfauna.enable_blue_jay_spawning"),
                        ConfigHandler.enableBlueJaySpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_BLUE_JAY_SPAWNING)
                .setTooltip(Component.translatable("option.atmosphericfauna.enable_blue_jay_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableBlueJaySpawning = newValue)
                .build());

        blueJays.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.max_active_blue_jays"),
                        ConfigHandler.maxActiveBlueJays)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_BLUE_JAYS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.max_active_blue_jays.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveBlueJays = newValue)
                .build());

        birds.addEntry(blueJays.build());

        // MARK: --- DEBUG ---

        ConfigCategory debug = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.debug"));

        debug.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("option.atmosphericfauna.debug_text_spawning"),
                        ConfigHandler.debugTextSpawning)
                .setDefaultValue(ConfigHandler.Defaults.DEBUG_TEXT_SPAWNING)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.debug_text_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.debugTextSpawning = newValue)
                .build());

        debug.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("option.atmosphericfauna.debug_text_birds"),
                        ConfigHandler.debugTextBirds)
                .setDefaultValue(ConfigHandler.Defaults.DEBUG_TEXT_BIRDS)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.debug_text_birds.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.debugTextBirds = newValue)
                .build());

        return builder.build();
    }
}
