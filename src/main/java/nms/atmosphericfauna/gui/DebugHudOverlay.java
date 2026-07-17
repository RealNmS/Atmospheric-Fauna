package nms.atmosphericfauna.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.particle.BaseBirdParticle;
import nms.atmosphericfauna.particle.BlueJayParticle;
import nms.atmosphericfauna.particle.CrowParticle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DebugHudOverlay {
    public static boolean showDebug = false;

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, "debug_hud"),
                (guiGraphics, tickCounter) -> {
                    if (!showDebug)
                        return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.gui.hud.isHidden())
                        return;

                    Map<String, Integer> birdCounts = new HashMap<>();
                    synchronized (BaseBirdParticle.ALL_BIRDS) {
                        for (BaseBirdParticle bird : BaseBirdParticle.ALL_BIRDS) {
                            String type = bird.getBaseSpriteName();
                            if (type == null)
                                type = "unknown";
                            birdCounts.put(type, birdCounts.getOrDefault(type, 0) + 1);
                        }
                    }

                    List<Map.Entry<String, Integer>> sorted = birdCounts.entrySet().stream()
                            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                            .collect(Collectors.toList());

                    int y = 50;
                    int screenWidth = mc.getWindow().getGuiScaledWidth();

                    String title = "[AF] Debug";
                    int titleWidth = mc.font.width(title);
                    guiGraphics.fill(screenWidth - titleWidth - 14, y - 2, screenWidth - 2, y + 10, 0x80000000);
                    guiGraphics.text(mc.font, title, screenWidth - titleWidth - 8, y, 0xFFFFFFAA, false);
                    y += 12;

                    int total = 0;
                    for (Map.Entry<String, Integer> entry : sorted) {
                        int maxCount = switch (entry.getKey()) {
                            case "blue_jay" -> BlueJayParticle.getMaxActiveBirds();
                            case "crow" -> CrowParticle.getMaxActiveBirds();
                            default -> BaseBirdParticle.getMaxActiveBirds();
                        };

                        String text = entry.getKey() + ": " + entry.getValue() + " / " + maxCount;
                        int width = mc.font.width(text);
                        guiGraphics.fill(screenWidth - width - 14, y - 2, screenWidth - 2, y + 10, 0x80000000);
                        guiGraphics.text(mc.font, text, screenWidth - width - 8, y, 0xFFFFFFFF, false);
                        y += 12;
                        total += entry.getValue();
                    }

                    String totalText = "Total: " + total + " / " + BaseBirdParticle.getMaxActiveBirds();
                    int totalWidth = mc.font.width(totalText);
                    guiGraphics.fill(screenWidth - totalWidth - 14, y - 2, screenWidth - 2, y + 10, 0x80000000);
                    guiGraphics.text(mc.font, totalText, screenWidth - totalWidth - 8, y, 0xFFAAFFAA, false);
                });
    }
}