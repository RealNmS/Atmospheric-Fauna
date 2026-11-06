package nms.atmosphericfauna.gui;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import static nms.atmosphericfauna.config.ConfigHandler.enableSpyglassInteraction;

//? if <=1.21.5 {
// import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
        // HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
        //?} else {
        HudElementRegistry.addLast(AtmosphericFauna.id("spyglass_hud"), (graphics, tickDelta) -> {
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

                    Identifier bgMain = AtmosphericFauna.id("spyglass_background");
                    Identifier bgSprite = AtmosphericFauna.defaultId("hud/effect_background");
                    Identifier texture = AtmosphericFauna
                            .id("textures/particle/" + lastBird.getBaseSpriteName() + "_perched_1.png");

                    // Render textures
                    //? if <=1.20.1 {
                    // com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
                    // Identifier bgMainTexture = AtmosphericFauna.id("textures/gui/sprites/spyglass_background.png");
                    // blitNineSliced(graphics, bgMainTexture, x + 22, y, totalWidth - 14, totalHeight);
                    // Identifier inventorySheet = AtmosphericFauna.defaultId("textures/gui/container/inventory.png");
                    // graphics.blit(inventorySheet, spriteX - 1, spriteY, 31, 31, 141.0F, 166.0F, 24, 24, 256, 256);
                    // graphics.blit(texture, spriteX - 1, spriteY - 5, 0, 0, 32, 32, 32, 32);
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    // com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    //?} else if <=1.21.1 {
                    // com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha);
                    // graphics.blitSprite(bgMain, x + 24, y, totalWidth - 16, totalHeight);
                    // graphics.blitSprite(bgSprite, spriteX, spriteY, 32, 32);
                    // graphics.blit(texture, spriteX, spriteY - 4, 0, 0, 32, 32, 32, 32);
                    // com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    // com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    //?} else if <=1.21.5 {
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, bgMain, x + 24, y, totalWidth - 16, totalHeight, argbColor);
                    // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, bgSprite, spriteX, spriteY, 32, 32, argbColor);
                    // graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, spriteX, spriteY - 4, 0.0F, 0.0F, 32, 32, 32, 32, argbColor);
                    //?} else {
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgMain, x + 24, y, totalWidth - 16, totalHeight, argbColor);
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

    // MARK: --- blitNineSliced ---
    // the texture didnt look right when using vanilla blitNineSliced, so i had ai vibecode a better one
    // usually i dont like vibecoding but i actually spent 2 days debugging the texture before
    // ai said "just make it yourself bruh"
    //? if <=1.20.1 {
    // private static void blitNineSliced(
    //         net.minecraft.client.gui.GuiGraphics graphics,
    //         Identifier texture,
    //         int x,
    //         int y,
    //         int width,
    //         int height
    // ) {
    //     final int textureWidth = 100;
    //     final int textureHeight = 100;
    //     final int border = 9;

    //     // Keep the corners from becoming larger than half the destination.
    //     int leftBorder = Math.min(border, width / 2);
    //     int rightBorder = Math.min(border, width / 2);
    //     int topBorder = Math.min(border, height / 2);
    //     int bottomBorder = Math.min(border, height / 2);

    //     int middleWidth = width - leftBorder - rightBorder;
    //     int middleHeight = height - topBorder - bottomBorder;

    //     // =========================================================
    //     // Corners
    //     // =========================================================

    //     // Top-left
    //     graphics.blit(
    //             texture,
    //             x, y,
    //             0, 0,
    //             leftBorder, topBorder,
    //             textureWidth, textureHeight
    //     );

    //     // Top-right
    //     graphics.blit(
    //             texture,
    //             x + width - rightBorder, y,
    //             textureWidth - rightBorder, 0,
    //             rightBorder, topBorder,
    //             textureWidth, textureHeight
    //     );

    //     // Bottom-left
    //     graphics.blit(
    //             texture,
    //             x, y + height - bottomBorder,
    //             0, textureHeight - bottomBorder,
    //             leftBorder, bottomBorder,
    //             textureWidth, textureHeight
    //     );

    //     // Bottom-right
    //     graphics.blit(
    //             texture,
    //             x + width - rightBorder,
    //             y + height - bottomBorder,
    //             textureWidth - rightBorder,
    //             textureHeight - bottomBorder,
    //             rightBorder, bottomBorder,
    //             textureWidth, textureHeight
    //     );

    //     // =========================================================
    //     // Top edge
    //     // =========================================================

    //     blitRepeatingHorizontal(
    //             graphics, texture,
    //             x + leftBorder, y,
    //             middleWidth, topBorder,
    //             border, 0,
    //             textureWidth - border * 2,
    //             textureWidth, textureHeight
    //     );

    //     // =========================================================
    //     // Bottom edge
    //     // =========================================================

    //     blitRepeatingHorizontal(
    //             graphics, texture,
    //             x + leftBorder,
    //             y + height - bottomBorder,
    //             middleWidth, bottomBorder,
    //             border,
    //             textureHeight - bottomBorder,
    //             textureWidth - border * 2,
    //             textureWidth, textureHeight
    //     );

    //     // =========================================================
    //     // Left edge
    //     // =========================================================

    //     blitRepeatingVertical(
    //             graphics, texture,
    //             x,
    //             y + topBorder,
    //             leftBorder,
    //             middleHeight,
    //             0,
    //             border,
    //             textureHeight - border * 2,
    //             textureWidth,
    //             textureHeight
    //     );

    //     // =========================================================
    //     // Right edge
    //     // =========================================================

    //     blitRepeatingVertical(
    //             graphics, texture,
    //             x + width - rightBorder,
    //             y + topBorder,
    //             rightBorder,
    //             middleHeight,
    //             textureWidth - rightBorder,
    //             border,
    //             textureHeight - border * 2,
    //             textureWidth,
    //             textureHeight
    //     );

    //     // =========================================================
    //     // Center
    //     // =========================================================

    //     blitRepeating(
    //             graphics, texture,
    //             x + leftBorder,
    //             y + topBorder,
    //             middleWidth,
    //             middleHeight,
    //             border,
    //             border,
    //             textureWidth - border * 2,
    //             textureHeight - border * 2,
    //             textureWidth,
    //             textureHeight
    //     );
    // }

    // private static void blitRepeatingHorizontal(
    //         net.minecraft.client.gui.GuiGraphics graphics,
    //         Identifier texture,
    //         int x,
    //         int y,
    //         int width,
    //         int height,
    //         int u,
    //         int v,
    //         int sourceWidth,
    //         int textureWidth,
    //         int textureHeight
    // ) {
    //     int remaining = width;
    //     int drawX = x;

    //     while (remaining > 0) {
    //         int sliceWidth = Math.min(remaining, sourceWidth);

    //         graphics.blit(
    //                 texture,
    //                 drawX, y,
    //                 u, v,
    //                 sliceWidth, height,
    //                 textureWidth, textureHeight
    //         );

    //         drawX += sliceWidth;
    //         remaining -= sliceWidth;
    //     }
    // }

    // private static void blitRepeatingVertical(
    //         net.minecraft.client.gui.GuiGraphics graphics,
    //         Identifier texture,
    //         int x,
    //         int y,
    //         int width,
    //         int height,
    //         int u,
    //         int v,
    //         int sourceHeight,
    //         int textureWidth,
    //         int textureHeight
    // ) {
    //     int remaining = height;
    //     int drawY = y;

    //     while (remaining > 0) {
    //         int sliceHeight = Math.min(remaining, sourceHeight);

    //         graphics.blit(
    //                 texture,
    //                 x, drawY,
    //                 u, v,
    //                 width, sliceHeight,
    //                 textureWidth, textureHeight
    //         );

    //         drawY += sliceHeight;
    //         remaining -= sliceHeight;
    //     }
    // }

    // private static void blitRepeating(
    //         net.minecraft.client.gui.GuiGraphics graphics,
    //         Identifier texture,
    //         int x,
    //         int y,
    //         int width,
    //         int height,
    //         int u,
    //         int v,
    //         int sourceWidth,
    //         int sourceHeight,
    //         int textureWidth,
    //         int textureHeight
    // ) {
    //     int remainingY = height;
    //     int drawY = y;

    //     while (remainingY > 0) {
    //         int sliceHeight = Math.min(remainingY, sourceHeight);

    //         int remainingX = width;
    //         int drawX = x;

    //         while (remainingX > 0) {
    //             int sliceWidth = Math.min(remainingX, sourceWidth);

    //             graphics.blit(
    //                     texture,
    //                     drawX, drawY,
    //                     u, v,
    //                     sliceWidth, sliceHeight,
    //                     textureWidth, textureHeight
    //             );

    //             drawX += sliceWidth;
    //             remainingX -= sliceWidth;
    //         }

    //         drawY += sliceHeight;
    //         remainingY -= sliceHeight;
    //     }
    // }
    //?}
}
