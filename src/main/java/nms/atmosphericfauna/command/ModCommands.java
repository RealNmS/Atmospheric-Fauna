package nms.atmosphericfauna.command;

import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//? if <=1.21.11 {
//  import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//?} else {
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class ModCommands {

//? if <=1.21.4 {
//  private static ClickEvent createUrlEvent(String url) { return new ClickEvent(ClickEvent.Action.OPEN_URL, url); }
//?} else {
    private static ClickEvent createUrlEvent(String url) { return new ClickEvent.OpenUrl(java.net.URI.create(url)); }
//?}

    public static void registerClientCommands() {
//? if <=1.21.11 {
//      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
//          dispatcher.register(ClientCommandManager.literal("atmosphericfauna")
//                  .executes(context -> {
//                      context.getSource().sendFeedback(Component.literal("Welcome to Atmospheric Fauna!").withStyle(ChatFormatting.GREEN));
//                      context.getSource().sendFeedback(Component.literal("📖 ")
//                              .append(Component.literal("Click here for the Official Wiki")
//                                      .withStyle(style -> style.withColor(ChatFormatting.AQUA)
//                                              .withUnderlined(true)
//                                              .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))));
//                      return 1;
//                  })
//                  .then(ClientCommandManager.literal("help")
//                          .executes(context -> {
//                              context.getSource().sendFeedback(Component.literal("--- Atmospheric Fauna Help ---").withStyle(ChatFormatting.GOLD));
//                              context.getSource().sendFeedback(Component.literal("• Learn about bird behaviors: ")
//                                      .append(Component.literal("[Read the Wiki]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
//                                              .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))));
//                              context.getSource().sendFeedback(Component.literal("• Found a bug or have ideas? ")
//                                      .append(Component.literal("[Report an Issue]").withStyle(style -> style.withColor(ChatFormatting.RED)
//                                              .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/issues")))));
//                              return 1;
//                          }))
//                  .then(ClientCommandManager.literal("debug")
//                          .executes(context -> {
//                              DebugHudOverlay.showDebug = !DebugHudOverlay.showDebug;
//                              String state = DebugHudOverlay.showDebug ? "enabled" : "disabled";
//                              context.getSource().sendFeedback(
//                                      Component.literal("Atmospheric Fauna debug scoreboard " + state + "."));
//                              return 1;
//                          }))
//                  .then(ClientCommandManager.literal("clearBirds")
//                          .executes(context -> {
//                              List<BaseBirdParticle> birds = new ArrayList<>(BaseBirdParticle.getAllBirds());
//                              int removed = 0;
//                              for (BaseBirdParticle bird : birds) {
//                                  if (bird == null) {
//                                      continue;
//                                  }
//                                  bird.remove();
//                                  removed++;
//                              }
//                              String message = "Removed " + removed + " bird particle" + (removed == 1 ? "" : "s") + ".";
//                              context.getSource().sendFeedback(Component.literal(message));
//                              return 1;
//                          })));
//      });
//?} else {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("atmosphericfauna")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Welcome to Atmospheric Fauna!").withStyle(ChatFormatting.GREEN));
                        context.getSource().sendFeedback(Component.literal("📖 ")
                                .append(Component.literal("Click here for the Official Wiki")
                                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                                .withUnderlined(true)
                                                .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))));
                        return 1;
                    })
                    .then(ClientCommands.literal("help")
                            .executes(context -> {
                                context.getSource().sendFeedback(Component.literal("--- Atmospheric Fauna Help ---").withStyle(ChatFormatting.GOLD));
                                context.getSource().sendFeedback(Component.literal("• Learn about bird behaviors: ")
                                        .append(Component.literal("[Read the Wiki]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                                .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))));
                                context.getSource().sendFeedback(Component.literal("• Found a bug or have ideas? ")
                                        .append(Component.literal("[Report an Issue]").withStyle(style -> style.withColor(ChatFormatting.RED)
                                                .withClickEvent(createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/issues")))));
                                return 1;
                            }))
                    .then(ClientCommands.literal("debug")
                            .executes(context -> {
                                DebugHudOverlay.showDebug = !DebugHudOverlay.showDebug;
                                String state = DebugHudOverlay.showDebug ? "enabled" : "disabled";
                                context.getSource().sendFeedback(
                                        Component.literal("Atmospheric Fauna debug scoreboard " + state + "."));
                                return 1;
                            }))
                    .then(ClientCommands.literal("clearBirds")
                            .executes(context -> {
                                List<BaseBirdParticle> birds = new ArrayList<>(BaseBirdParticle.getAllBirds());
                                int removed = 0;
                                for (BaseBirdParticle bird : birds) {
                                    if (bird == null) {
                                        continue;
                                    }
                                    bird.remove();
                                    removed++;
                                }
                                String message = "Removed " + removed + " bird particle" + (removed == 1 ? "" : "s") + ".";
                                context.getSource().sendFeedback(Component.literal(message));
                                return 1;
                            })));
        });
//?}
    }
}