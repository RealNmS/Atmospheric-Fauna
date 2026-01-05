package nms.atmosphericfauna.modmenu;

import org.jspecify.annotations.NonNull;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (isClothConfigAvailable()) {
            try {
                Class.forName("nms.atmosphericfauna.modmenu.ModMenuConfig");
                return ModMenuConfig::createConfigScreen;
            } catch (Throwable t) {
            }
        }

        // Fallback screen
        return parent -> new Screen(Component.translatable("title.atmosphericfauna.config")) {
            @Override
            protected void init() {
                super.init();
                Button info = Button.builder(Component.translatable("text.atmosphericfauna.cloth_missing"), b -> {
                })
                        .bounds(this.width / 2 - 170, this.height / 2 - 30, 340, 20)
                        .build();
                info.active = false;
                this.addRenderableWidget(info);

                this.addRenderableWidget(
                        Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(parent))
                                .bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20)
                                .build());
            }

            @Override
            public void render(@NonNull GuiGraphics gui, int mouseX, int mouseY, float delta) {
                gui.drawCenteredString(this.font,
                        Component.translatable("text.atmosphericfauna.cloth_missing").getString(), this.width / 2,
                        this.height / 2 - 6, 0xFF5555);
                super.render(gui, mouseX, mouseY, delta);
            }
        };
    }

    private static boolean isClothConfigAvailable() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
