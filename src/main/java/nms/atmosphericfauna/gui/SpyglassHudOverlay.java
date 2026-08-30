package nms.atmosphericfauna.gui;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import static nms.atmosphericfauna.config.ConfigHandler.enableSpyglassInteraction;

//? if <=1.21.5 {
//  import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?} else {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class SpyglassHudOverlay {
    public static void register() {
        //? if <=1.21.5 {
        //      HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
        //?} else {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, "spyglass_hud"),
                (graphics, tickDelta) -> {
        //?}
                    if (!enableSpyglassInteraction)
                        return;

                    BaseBirdParticle bird = BaseBirdParticle.hoveredBird;
                    if (bird == null)
                        return;

                    Minecraft client = Minecraft.getInstance();

                    int x = 4;
                    int y = 4;

                    String displayName = bird.getBaseSpriteName().replace("_", " ").toUpperCase();

                    int textWidth = client.font.width(displayName);

                    int totalWidth = 52 + textWidth;
                    int totalHeight = 40;

                    int spriteX = x + 4;
                    int spriteY = y + 4;

                    int textX = spriteX + 32 + 8;
                    int textY = y + 16;

                    Identifier bgMain = Identifier.withDefaultNamespace("toast/advancement");
                    Identifier bgSprite = Identifier.withDefaultNamespace("toast/recipe");
                    Identifier texture = Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID,
                            "textures/particle/" + bird.getBaseSpriteName() + "_perched_1.png");

                    //? if <=1.21.1 {
                    // com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    // graphics.blitSprite(bgMain, x, y, totalWidth, totalHeight);
                    // graphics.blitSprite(bgSprite, spriteX, spriteY, 32, 32);
                    // graphics.blit(texture, spriteX, spriteY, 0, 0, 32, 32, 32, 32);
                    // com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    //?} else if <=1.21.5 {
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                    // bgMain, x, y, totalWidth, totalHeight);
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                    // bgSprite, spriteX, spriteY, 32, 32);
                    // graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture,
                    // spriteX, spriteY, 0.0F, 0.0F, 32, 32, 32, 32);
                    //?} else {
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgMain, x, y,
                            totalWidth, totalHeight);
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgSprite,
                            spriteX, spriteY, 32, 32);
                    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, spriteX,
                            spriteY, 0.0F, 0.0F, 32, 32, 32, 32);
                    //?}

                    //? if <=1.21.11 {
                    // graphics.drawString(client.font, displayName, textX, textY, -1, true);
                    //?} else {
                    graphics.text(client.font, displayName, textX, textY, -1, true);
                    //?}
                });
    }
}
