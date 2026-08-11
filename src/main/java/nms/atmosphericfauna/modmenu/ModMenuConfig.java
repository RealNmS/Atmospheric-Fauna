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
                .startBooleanToggle(Component
                        .translatable("option.atmosphericfauna.midair_border_spawning"),
                        ConfigHandler.enableMidairBorderSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_MIDAIR_BORDER_SPAWNING)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.midair_border_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableMidairBorderSpawning = newValue)
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

        spawning.addEntry(entryBuilder
                .startBooleanToggle(Component
                        .translatable("option.atmosphericfauna.spawn_around_spectators"),
                        ConfigHandler.spawnAroundSpectators)
                .setDefaultValue(ConfigHandler.Defaults.SPAWN_AROUND_SPECTATORS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.spawn_around_spectators.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.spawnAroundSpectators = newValue)
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

        SubCategoryBuilder blueJays = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.blue_jays"));

        blueJays.add(entryBuilder
                .startBooleanToggle(Component.translatable("option.atmosphericfauna.species.enable_spawning"),
                        ConfigHandler.enableBlueJaySpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_BLUE_JAY_SPAWNING)
                .setTooltip(Component.translatable("option.atmosphericfauna.species.enable_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableBlueJaySpawning = newValue)
                .build());

        blueJays.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.species.max_active"),
                        ConfigHandler.maxActiveBlueJays)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_BLUE_JAYS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.species.max_active.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveBlueJays = newValue)
                .build());

        blueJays.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_biome_checks"),
                        ConfigHandler.disableBlueJaySpawnBiomeChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_BLUE_JAY_SPAWN_BIOME_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_biome_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableBlueJaySpawnBiomeChecks = newValue)
                .build());

        blueJays.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_block_checks"),
                        ConfigHandler.disableBlueJaySpawnBlockChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_BLUE_JAY_SPAWN_BLOCK_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_block_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableBlueJaySpawnBlockChecks = newValue)
                .build());

        birds.addEntry(blueJays.build());

        SubCategoryBuilder commonSwifts = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.common_swifts"));

        commonSwifts.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable("option.atmosphericfauna.species.enable_spawning"),
                        ConfigHandler.enableCommonSwiftSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_COMMON_SWIFT_SPAWNING)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.species.enable_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableCommonSwiftSpawning = newValue)
                .build());

        commonSwifts.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.species.max_active"),
                        ConfigHandler.maxActiveCommonSwifts)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_COMMON_SWIFTS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.species.max_active.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveCommonSwifts = newValue)
                .build());

        commonSwifts.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_biome_checks"),
                        ConfigHandler.disableCommonSwiftSpawnBiomeChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_COMMON_SWIFT_SPAWN_BIOME_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_biome_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableCommonSwiftSpawnBiomeChecks = newValue)
                .build());

        commonSwifts.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_block_checks"),
                        ConfigHandler.disableCommonSwiftSpawnBlockChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_COMMON_SWIFT_SPAWN_BLOCK_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_block_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableCommonSwiftSpawnBlockChecks = newValue)
                .build());

        birds.addEntry(commonSwifts.build());

        SubCategoryBuilder crows = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.crows"));

        crows.add(entryBuilder
                .startBooleanToggle(Component.translatable("option.atmosphericfauna.species.enable_spawning"),
                        ConfigHandler.enableCrowSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_CROW_SPAWNING)
                .setTooltip(Component.translatable("option.atmosphericfauna.species.enable_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableCrowSpawning = newValue)
                .build());

        crows.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.species.max_active"),
                        ConfigHandler.maxActiveCrows)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_CROWS)
                .setMin(0)
                .setTooltip(Component.translatable("option.atmosphericfauna.species.max_active.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveCrows = newValue)
                .build());

        crows.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_biome_checks"),
                        ConfigHandler.disableCrowSpawnBiomeChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_CROW_SPAWN_BIOME_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_biome_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableCrowSpawnBiomeChecks = newValue)
                .build());

        crows.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_block_checks"),
                        ConfigHandler.disableCrowSpawnBlockChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_CROW_SPAWN_BLOCK_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_block_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableCrowSpawnBlockChecks = newValue)
                .build());

        birds.addEntry(crows.build());

        SubCategoryBuilder northernCardinals = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.northern_cardinals"));

        northernCardinals.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.enable_spawning"),
                        ConfigHandler.enableNorthernCardinalSpawning)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_NORTHERN_CARDINAL_SPAWNING)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.enable_spawning.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableNorthernCardinalSpawning = newValue)
                .build());

        northernCardinals.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.species.max_active"),
                        ConfigHandler.maxActiveNorthernCardinals)
                .setDefaultValue(ConfigHandler.Defaults.MAX_ACTIVE_NORTHERN_CARDINALS)
                .setMin(0)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.species.max_active.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.maxActiveNorthernCardinals = newValue)
                .build());

        northernCardinals.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_biome_checks"),
                        ConfigHandler.disableNorthernCardinalSpawnBiomeChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BIOME_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_biome_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableNorthernCardinalSpawnBiomeChecks = newValue)
                .build());

        northernCardinals.add(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.atmosphericfauna.species.ignore_block_checks"),
                        ConfigHandler.disableNorthernCardinalSpawnBlockChecks)
                .setDefaultValue(ConfigHandler.Defaults.DISABLE_NORTHERN_CARDINAL_SPAWN_BLOCK_CHECKS)
                .setTooltip(Component.translatable(
                        "option.atmosphericfauna.species.ignore_block_checks.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.disableNorthernCardinalSpawnBlockChecks = newValue)
                .build());

        birds.addEntry(northernCardinals.build());

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

        debug.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("option.atmosphericfauna.enable_debug_screen_on_join"),
                        ConfigHandler.enableDebugScreenOnJoin)
                .setDefaultValue(ConfigHandler.Defaults.ENABLE_DEBUG_SCREEN_ON_JOIN)
                .setTooltip(Component
                        .translatable("option.atmosphericfauna.enable_debug_screen_on_join.tooltip"))
                .setSaveConsumer(newValue -> ConfigHandler.enableDebugScreenOnJoin = newValue)
                .build());

        return builder.build();
    }
}
