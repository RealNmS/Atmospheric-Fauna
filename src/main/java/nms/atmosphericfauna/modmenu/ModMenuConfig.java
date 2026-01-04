package nms.atmosphericfauna.modmenu;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nms.atmosphericfauna.particle.CrowParticle;

public class ModMenuConfig {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.atmosphericfauna.config"));

        ConfigCategory general = builder
                .getOrCreateCategory(Component.translatable("category.atmosphericfauna.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startIntField(Component.translatable("option.atmosphericfauna.max_active_crows"),
                        CrowParticle.maxActiveCrows)
                .setDefaultValue(120)
                .setTooltip(Component.translatable("option.atmosphericfauna.max_active_crows.tooltip"))
                .setSaveConsumer(newValue -> CrowParticle.maxActiveCrows = newValue)
                .build());

        return builder.build();
    }
}
