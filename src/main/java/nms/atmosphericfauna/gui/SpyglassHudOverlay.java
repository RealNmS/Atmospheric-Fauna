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

    private static float fadeAlpha = 0.0f;
    private static BaseBirdParticle lastBird = null;

    public static void register() {
        //? if <=1.21.5 {
        //      HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
        //?} else {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, "spyglass_hud"),
                (graphics, tickDelta) -> {
        //?}
                    if (!enableSpyglassInteraction)
                        return;

                    BaseBirdParticle currentBird = BaseBirdParticle.hoveredBird;

                    if (currentBird != null) {
                        lastBird = currentBird;
                        fadeAlpha = Math.min(1.0f, fadeAlpha + 0.1f);
                    } else {
                        fadeAlpha = Math.max(0.0f, fadeAlpha - 0.02f);
                    }

                    if (fadeAlpha <= 0.0f || lastBird == null)
                        return;

                    Minecraft client = Minecraft.getInstance();

                    int screenWidth = client.getWindow().getGuiScaledWidth();
                    int screenHeight = client.getWindow().getGuiScaledHeight();

                    int alphaInt = (int) (fadeAlpha * 255.0F);
                    int argbColor = (alphaInt << 24) | 0xFFFFFF;

                    String displayName = lastBird.getFormattedName();

                    int totalWidth = 48 + client.font.width(displayName);
                    int totalHeight = 40;

                    int x = (screenWidth - totalWidth) / 2;
                    int y = (screenHeight - totalHeight) / 2 + (screenHeight / 4);

                    int spriteX = x;
                    int spriteY = y + 4;

                    int textX = spriteX + 32 + 8;
                    int textY = y + 16;

                    Identifier bgMain = Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, "spyglass_background");
                    Identifier bgSprite = Identifier.withDefaultNamespace("hud/effect_background");
                    Identifier texture = Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID,
                            "textures/particle/" + lastBird.getBaseSpriteName() + "_perched_1.png");

                    // Render textures
                    //? if <=1.21.1 {
                    // com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
                    // graphics.blitSprite(bgMain, x + 16, y, totalWidth, totalHeight);
                    // graphics.blitSprite(bgSprite, spriteX, spriteY, 32, 32);
                    // graphics.blit(texture, spriteX, spriteY - 4, 0, 0, 32, 32, 32, 32);
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    // com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    //?} else if <=1.21.5 {
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, bgMain, x + 16, y, totalWidth, totalHeight, argbColor);
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, bgSprite, spriteX, spriteY, 32, 32, argbColor);
                    // graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, spriteX, spriteY - 4, 0.0F, 0.0F, 32, 32, 32, 32, argbColor);
                    //?} else {
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgMain, x + 16, y, totalWidth, totalHeight, argbColor);
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgSprite, spriteX, spriteY, 32, 32, argbColor);
                    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, spriteX, spriteY - 4, 0.0F, 0.0F, 32, 32, 32, 32, argbColor);
                    //?}

                    int shadowColor = (alphaInt << 24) | 0x3E3E3E;

                    //? if <=1.21.11 {
                    // graphics.drawString(client.font, displayName, textX + 1, textY + 1, shadowColor, false);
                    // graphics.drawString(client.font, displayName, textX, textY, argbColor, false);
                    //?} else {
                    graphics.text(client.font, displayName, textX + 1, textY + 1, shadowColor, false);
                    graphics.text(client.font, displayName, textX, textY, argbColor, false);
                    //?}
                });
    }
}
