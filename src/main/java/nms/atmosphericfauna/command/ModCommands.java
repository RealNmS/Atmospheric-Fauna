package nms.atmosphericfauna.command;

import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import nms.atmosphericfauna.spawning.SpawnData;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//? if <=1.21.11 {
// import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//?} else {
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//?}
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.MutableComponent;

public class ModCommands {
    private static Screen screenToOpen = null;

    //? if <=1.21.4 {
    // private static ClickEvent createUrlEvent(String url) { return new ClickEvent(ClickEvent.Action.OPEN_URL, url); }
    // private static ClickEvent createRunCommandEvent(String command) { return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command); }
    //?} else {
    private static ClickEvent createUrlEvent(String url) { return new ClickEvent.OpenUrl(java.net.URI.create(url)); }
    private static ClickEvent createRunCommandEvent(String command) { return new ClickEvent.RunCommand(command); }
    //?}

    private static MutableComponent getPrefix() {
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("Atmospheric Fauna").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent getHeader(String title) {
        return Component.literal("--- [ ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(title).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" ] ---").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent createLinkButton(String label, String icon, ChatFormatting color, ClickEvent clickEvent) {
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(icon + " ").withStyle(color))
                .append(Component.literal(label).withStyle(style -> style.withColor(color).withUnderlined(true).withClickEvent(clickEvent)))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static SpawnData findSpawnData(String birdName) {
        return SpawnData.byName(birdName);
    }

    private static int spawnBirds(String birdName, int amount, Minecraft minecraft) {
        SpawnData spawnData = findSpawnData(birdName);
        if (spawnData == null) {
            return 0;
        }

        ClientLevel level = minecraft.level;
        if (minecraft.player == null || level == null) {
            return 0;
        }

        var player = minecraft.player;
        RandomSource random = level.getRandom();

        BaseBirdParticle.setNextFlockSpeedOffset((random.nextFloat() - 0.5f) * 0.04f);

        for (int index = 0; index < amount; index++) {
            double offsetX = (random.nextDouble() - 0.5D) * 1.5D;
            double offsetY = 0.5D + random.nextDouble() * 0.75D;
            double offsetZ = (random.nextDouble() - 0.5D) * 1.5D;

            level.addParticle(spawnData.particleType(),
                    player.getX() + offsetX,
                    player.getY() + offsetY,
                    player.getZ() + offsetZ,
                    (random.nextDouble() - 0.5D) * 0.05D,
                    0.05D + random.nextDouble() * 0.05D,
                    (random.nextDouble() - 0.5D) * 0.05D);
        }

        return amount;
    }

    public static void registerClientCommands() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenToOpen != null) {
                Screen screen = screenToOpen;
                screenToOpen = null;
                //? if >=26.2 {
                // client.setScreenAndShow(screen);
                //?} else {
                client.setScreen(screen);
                //?}
            }
        });

    //? if <=1.21.11 {
    //      var spawnCommand = ClientCommandManager.literal("spawn")
    //              .then(ClientCommandManager.argument("bird", StringArgumentType.word())
    //                      .suggests((context, builder) -> SharedSuggestionProvider.suggest(SpawnData.names(), builder))
    //                      .then(ClientCommandManager.argument("amount", IntegerArgumentType.integer(1))
    //                              .executes(context -> {
    //                                  String birdName = StringArgumentType.getString(context, "bird");
    //                                  int amount = IntegerArgumentType.getInteger(context, "amount");
    //                                  Minecraft minecraft = Minecraft.getInstance();
    //                                  int spawned = spawnBirds(birdName, amount, minecraft);
    //                                  if (spawned <= 0) {
    //                                      SpawnData spawnData = findSpawnData(birdName);
    //                                      if (spawnData == null) {
    //                                          context.getSource().sendFeedback(getPrefix()
    //                                                  .append(Component.literal("Unknown bird type '").withStyle(ChatFormatting.RED))
    //                                                  .append(Component.literal(birdName).withStyle(ChatFormatting.YELLOW))
    //                                                  .append(Component.literal("'. Available: ").withStyle(ChatFormatting.RED))
    //                                                  .append(Component.literal(String.join(", ", SpawnData.names())).withStyle(ChatFormatting.GRAY)));
    //                                      } else if (minecraft.player == null || minecraft.level == null) {
    //                                          context.getSource().sendFeedback(getPrefix()
    //                                                  .append(Component.literal("You must be in a world to spawn birds.").withStyle(ChatFormatting.RED)));
    //                                      }
    //                                      return 0;
    //                                  }
    //
    //                                  context.getSource().sendFeedback(getPrefix()
    //                                          .append(Component.literal("Spawned ").withStyle(ChatFormatting.GRAY))
    //                                          .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
    //                                          .append(Component.literal(" " + birdName + (amount == 1 ? "" : "s") + " at your location.").withStyle(ChatFormatting.GRAY)));
    //                                  return amount;
    //                              }))
    //                      .executes(context -> {
    //                          String birdName = StringArgumentType.getString(context, "bird");
    //                          Minecraft minecraft = Minecraft.getInstance();
    //                          int amount = spawnBirds(birdName, 1, minecraft);
    //                          if (amount <= 0) {
    //                              SpawnData spawnData = findSpawnData(birdName);
    //                              if (spawnData == null) {
    //                                  context.getSource().sendFeedback(getPrefix()
    //                                          .append(Component.literal("Unknown bird type '").withStyle(ChatFormatting.RED))
    //                                          .append(Component.literal(birdName).withStyle(ChatFormatting.YELLOW))
    //                                          .append(Component.literal("'. Available: ").withStyle(ChatFormatting.RED))
    //                                          .append(Component.literal(String.join(", ", SpawnData.names())).withStyle(ChatFormatting.GRAY)));
    //                              } else if (minecraft.player == null || minecraft.level == null) {
    //                                  context.getSource().sendFeedback(getPrefix()
    //                                          .append(Component.literal("You must be in a world to spawn birds.").withStyle(ChatFormatting.RED)));
    //                              }
    //                              return 0;
    //                          }
    //
    //                          context.getSource().sendFeedback(getPrefix()
    //                                  .append(Component.literal("Spawned ").withStyle(ChatFormatting.GRAY))
    //                                  .append(Component.literal("1 ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
    //                                  .append(Component.literal(birdName + " at your location.").withStyle(ChatFormatting.GRAY)));
    //                          return amount;
    //                      }));
    //
    //      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
    //          dispatcher.register(ClientCommandManager.literal("atmosphericfauna")
    //                  .executes(context -> {
    //                      context.getSource().sendFeedback(getHeader("Atmospheric Fauna"));
    //                      context.getSource().sendFeedback(Component.literal("Birds, insects, and other fauna making your world feel alive.").withStyle(ChatFormatting.GRAY));
    //                      context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                              .append(createLinkButton("Mod Settings", "⚙", ChatFormatting.YELLOW, createRunCommandEvent("/atmosphericfauna config"))));
    //                      context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                              .append(createLinkButton("Official Wiki", "📖", ChatFormatting.AQUA, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki"))));
    //                      context.getSource().sendFeedback(Component.literal("Type ").withStyle(ChatFormatting.GRAY)
    //                              .append(Component.literal("/atmosphericfauna help").withStyle(ChatFormatting.GREEN))
    //                              .append(Component.literal(" for available commands.").withStyle(ChatFormatting.GRAY)));
    //                      return 1;
    //                  })
    //                  .then(ClientCommandManager.literal("config")
    //                          .executes(context -> {
    //                              screenToOpen = nms.atmosphericfauna.gui.ConfigScreenHelper.createScreen(null);
    //                              return 1;
    //                          }))
    //                  .then(ClientCommandManager.literal("help")
    //                          .executes(context -> {
    //                              context.getSource().sendFeedback(getHeader("Command Help"));
    //                              context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                                      .append(Component.literal("/af config ").withStyle(ChatFormatting.GREEN))
    //                                      .append(Component.literal("- Open mod settings").withStyle(ChatFormatting.GRAY)));
    //                              context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                                      .append(Component.literal("/af spawn <bird> [amount] ").withStyle(ChatFormatting.GREEN))
    //                                      .append(Component.literal("- Spawn bird particles").withStyle(ChatFormatting.GRAY)));
    //                              context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                                      .append(Component.literal("/af clearBirds ").withStyle(ChatFormatting.GREEN))
    //                                      .append(Component.literal("- Remove active bird particles").withStyle(ChatFormatting.GRAY)));
    //                              context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
    //                                      .append(Component.literal("/af debug ").withStyle(ChatFormatting.GREEN))
    //                                      .append(Component.literal("- Toggle debug overlay").withStyle(ChatFormatting.GRAY)));
    //                              context.getSource().sendFeedback(Component.literal("Links: ").withStyle(ChatFormatting.GRAY)
    //                                      .append(createLinkButton("Wiki", "📖", ChatFormatting.AQUA, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))
    //                                      .append(Component.literal(" "))
    //                                      .append(createLinkButton("Report Bug", "🐛", ChatFormatting.RED, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/issues"))));
    //                              return 1;
    //                          }))
    //                  .then(ClientCommandManager.literal("debug")
    //                          .executes(context -> {
    //                              DebugHudOverlay.showDebug = !DebugHudOverlay.showDebug;
    //                              Component status = DebugHudOverlay.showDebug
    //                                      ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
    //                                      : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    //                              context.getSource().sendFeedback(getPrefix()
    //                                      .append(Component.literal("Debug scoreboard overlay is now ").withStyle(ChatFormatting.GRAY))
    //                                      .append(status)
    //                                      .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
    //                              return 1;
    //                          }))
    //                  .then(ClientCommandManager.literal("clearBirds")
    //                          .executes(context -> {
    //                              int removed = BaseBirdParticle.clearAllParticles();
    //                              context.getSource().sendFeedback(getPrefix()
    //                                      .append(Component.literal("Cleared ").withStyle(ChatFormatting.GRAY))
    //                                      .append(Component.literal(String.valueOf(removed)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
    //                                      .append(Component.literal(" active bird particle" + (removed == 1 ? "" : "s") + ".").withStyle(ChatFormatting.GRAY)));
    //                              return 1;
    //                          }))
    //                  .then(spawnCommand));
    //      });
    //?} else {
        var spawnCommand = ClientCommands.literal("spawn")
                .then(ClientCommands.argument("bird", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(SpawnData.names(), builder))
                        .then(ClientCommands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    String birdName = StringArgumentType.getString(context, "bird");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    Minecraft minecraft = Minecraft.getInstance();
                                    int spawned = spawnBirds(birdName, amount, minecraft);

                                    if (spawned <= 0) {
                                        SpawnData spawnData = findSpawnData(birdName);
                                        if (spawnData == null) {
                                            context.getSource().sendFeedback(getPrefix()
                                                    .append(Component.literal("Unknown bird type '").withStyle(ChatFormatting.RED))
                                                    .append(Component.literal(birdName).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal("'. Available: ").withStyle(ChatFormatting.RED))
                                                    .append(Component.literal(String.join(", ", SpawnData.names())).withStyle(ChatFormatting.GRAY)));
                                        } else if (minecraft.player == null || minecraft.level == null) {
                                            context.getSource().sendFeedback(getPrefix()
                                                    .append(Component.literal("You must be in a world to spawn birds.").withStyle(ChatFormatting.RED)));
                                        }
                                        return 0;
                                    }

                                    context.getSource().sendFeedback(getPrefix()
                                            .append(Component.literal("Spawned ").withStyle(ChatFormatting.GRAY))
                                            .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                            .append(Component.literal(" " + birdName + (amount == 1 ? "" : "s") + " at your location.").withStyle(ChatFormatting.GRAY)));
                                    return amount;
                                }))
                        .executes(context -> {
                            String birdName = StringArgumentType.getString(context, "bird");
                            Minecraft minecraft = Minecraft.getInstance();
                            int amount = spawnBirds(birdName, 1, minecraft);

                            if (amount <= 0) {
                                SpawnData spawnData = findSpawnData(birdName);
                                if (spawnData == null) {
                                    context.getSource().sendFeedback(getPrefix()
                                            .append(Component.literal("Unknown bird type '").withStyle(ChatFormatting.RED))
                                            .append(Component.literal(birdName).withStyle(ChatFormatting.YELLOW))
                                            .append(Component.literal("'. Available: ").withStyle(ChatFormatting.RED))
                                            .append(Component.literal(String.join(", ", SpawnData.names())).withStyle(ChatFormatting.GRAY)));
                                } else if (minecraft.player == null || minecraft.level == null) {
                                    context.getSource().sendFeedback(getPrefix()
                                            .append(Component.literal("You must be in a world to spawn birds.").withStyle(ChatFormatting.RED)));
                                }
                                return 0;
                            }

                            context.getSource().sendFeedback(getPrefix()
                                    .append(Component.literal("Spawned ").withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal("1 ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                    .append(Component.literal(birdName + " at your location.").withStyle(ChatFormatting.GRAY)));
                            return amount;
                        }));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("atmosphericfauna")
                    .executes(context -> {
                        context.getSource().sendFeedback(getHeader("Atmospheric Fauna"));
                        context.getSource().sendFeedback(Component.literal("Birds, insects, and other fauna making your world feel alive.").withStyle(ChatFormatting.GRAY));
                        context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(createLinkButton("Mod Settings", "⚙", ChatFormatting.YELLOW, createRunCommandEvent("/atmosphericfauna config"))));
                        context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(createLinkButton("Official Wiki", "📖", ChatFormatting.AQUA, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki"))));
                        context.getSource().sendFeedback(Component.literal("Type ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal("/atmosphericfauna help").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" for available commands.").withStyle(ChatFormatting.GRAY)));
                        return 1;
                    })
                    .then(ClientCommands.literal("config")
                            .executes(context -> {
                                screenToOpen = nms.atmosphericfauna.gui.ConfigScreenHelper.createScreen(null);
                                return 1;
                            }))
                    .then(ClientCommands.literal("help")
                            .executes(context -> {
                                context.getSource().sendFeedback(getHeader("Command Help"));
                                context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                        .append(Component.literal("/af config ").withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal("- Open mod settings").withStyle(ChatFormatting.GRAY)));
                                context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                        .append(Component.literal("/af spawn <bird> [amount] ").withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal("- Spawn bird particles").withStyle(ChatFormatting.GRAY)));
                                context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                        .append(Component.literal("/af clearBirds ").withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal("- Remove active bird particles").withStyle(ChatFormatting.GRAY)));
                                context.getSource().sendFeedback(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                                        .append(Component.literal("/af debug ").withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal("- Toggle debug overlay").withStyle(ChatFormatting.GRAY)));
                                context.getSource().sendFeedback(Component.literal("Links: ").withStyle(ChatFormatting.GRAY)
                                        .append(createLinkButton("Wiki", "📖", ChatFormatting.AQUA, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/wiki")))
                                        .append(Component.literal(" "))
                                        .append(createLinkButton("Report Bug", "🐛", ChatFormatting.RED, createUrlEvent("https://github.com/RealNmS/Atmospheric-Fauna/issues"))));
                                return 1;
                            }))
                    .then(ClientCommands.literal("debug")
                            .executes(context -> {
                                DebugHudOverlay.showDebug = !DebugHudOverlay.showDebug;
                                Component status = DebugHudOverlay.showDebug
                                        ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                        : Component.literal("DISABLED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                                context.getSource().sendFeedback(getPrefix()
                                        .append(Component.literal("Debug scoreboard overlay is now ").withStyle(ChatFormatting.GRAY))
                                        .append(status)
                                        .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                return 1;
                            }))
                    .then(ClientCommands.literal("clearBirds")
                            .executes(context -> {
                                int removed = BaseBirdParticle.clearAllParticles();
                                context.getSource().sendFeedback(getPrefix()
                                        .append(Component.literal("Cleared ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.valueOf(removed)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                        .append(Component.literal(" active bird particle" + (removed == 1 ? "" : "s") + ".").withStyle(ChatFormatting.GRAY)));
                                return 1;
                            }))
                    .then(spawnCommand));
        });
    //?}
    }
}
