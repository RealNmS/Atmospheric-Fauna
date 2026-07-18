package nms.atmosphericfauna.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
//? if <=1.21.11 {
/*import net.minecraft.client.gui.GuiGraphics;
*//*?} */
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
                this.addRenderableWidget(
                        //? if >=26.2 {
                        /*Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreenAndShow(parent))
                        *//*?} else {*/
                        Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(parent))
                        //?}
                                .bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20)
                                .build());
            }

            //? if <=1.21.11 {
            /*@Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
                super.render(guiGraphics, mouseX, mouseY, delta);
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable("text.atmosphericfauna.cloth_missing"), this.width / 2,
                        this.height / 2 - 15, 0xFFFFFFFF);
            }
            *//*?} else {*/
            @Override
            public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
                super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
                guiGraphics.centeredText(this.font,
                        Component.translatable("text.atmosphericfauna.cloth_missing"), this.width / 2,
                        this.height / 2 - 15, 0xFFFFFFFF);
            }
            //?}
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
