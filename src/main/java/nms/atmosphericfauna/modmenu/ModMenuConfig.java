package nms.atmosphericfauna.modmenu;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;

public class ModMenuConfig {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.atmosphericfauna.config"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Spawning Category

        ConfigCategory spawning = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.spawning"));

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.spawn_range_from_player"),
                        AmbientSpawning.spawnRangeFromPlayer)
                .setDefaultValue(96)
                .setTooltip(Component.translatable("option.atmosphericfauna.spawn_range_from_player.tooltip"))
                .setSaveConsumer(newValue -> AmbientSpawning.spawnRangeFromPlayer = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.spawn_tick_delay"),
                        AmbientSpawning.spawnTickDelay)
                .setDefaultValue(200)
                .setTooltip(Component.translatable("option.atmosphericfauna.spawn_tick_delay.tooltip"))
                .setSaveConsumer(newValue -> AmbientSpawning.spawnTickDelay = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.attempts_per_tick"),
                        AmbientSpawning.attemptsPerTick)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("option.atmosphericfauna.attempts_per_tick.tooltip"))
                .setSaveConsumer(newValue -> AmbientSpawning.attemptsPerTick = newValue)
                .build());

        spawning.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.search_radius"),
                        AmbientSpawning.searchRadius)
                .setDefaultValue(8)
                .setTooltip(Component.translatable("option.atmosphericfauna.search_radius.tooltip"))
                .setSaveConsumer(newValue -> AmbientSpawning.searchRadius = newValue)
                .build());

        // Birds Category

        ConfigCategory birds = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.birds"));

        SubCategoryBuilder crows = entryBuilder
                .startSubCategory(Component.translatable("subcategory.atmosphericfauna.crows"));

        crows.add(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.max_active_crows"),
                        CrowParticle.maxActiveCrows)
                .setDefaultValue(120)
                .setTooltip(Component.translatable("option.atmosphericfauna.max_active_crows.tooltip"))
                .setSaveConsumer(newValue -> CrowParticle.maxActiveCrows = newValue)
                .build());

        birds.addEntry(crows.build());

        // Debug Category

        ConfigCategory debug = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.debug"));

        debug.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.atmosphericfauna.debug_text_spawning"),
                        AmbientSpawning.debugText)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.atmosphericfauna.debug_text_spawning.tooltip"))
                .setSaveConsumer(newValue -> AmbientSpawning.debugText = newValue)
                .build());

        return builder.build();
    }
}
