package nms.atmosphericfauna.gui;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
//? if <=1.21.11 {
/*import net.minecraft.client.gui.GuiGraphics;
*//*?} */
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreenHelper {

    public static Screen createScreen(Screen parent) {
        if (isClothConfigAvailable()) {
            try {
                Class<?> configClass = Class.forName("nms.atmosphericfauna.modmenu.ModMenuConfig");
                return (Screen) configClass.getMethod("createConfigScreen", Screen.class).invoke(null, parent);
            } catch (Throwable t) {
            }
        }
        return createFallbackScreen(parent);
    }

    private static Screen createFallbackScreen(Screen parent) {
        return new Screen(Component.translatable("title.atmosphericfauna.config")) {
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
