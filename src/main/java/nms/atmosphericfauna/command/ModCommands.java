package nms.atmosphericfauna.command;

import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import nms.atmosphericfauna.spawning.SpawnData;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//? if <=1.21.11 {
//  import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//?} else {
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

public class ModCommands {
    //? if <=1.21.4 {
    //  private static ClickEvent createUrlEvent(String url) { return new ClickEvent(ClickEvent.Action.OPEN_URL, url); }
    //?} else {
    private static ClickEvent createUrlEvent(String url) { return new ClickEvent.OpenUrl(java.net.URI.create(url)); }
    //?}

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

        if (!minecraft.player.getAbilities().instabuild) {
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
    //                                          context.getSource().sendFeedback(Component.literal(
    //                                                  "Unknown bird type '" + birdName + "'. Available birds: "
    //                                                          + String.join(", ", SpawnData.names()) + "."));
    //                                      } else if (minecraft.player == null || minecraft.level == null) {
    //                                          context.getSource().sendFeedback(Component.literal(
    //                                                  "You must be in a world to spawn birds."));
    //                                      } else if (!minecraft.player.getAbilities().instabuild) {
    //                                          context.getSource().sendFeedback(Component.literal(
    //                                                  "You must be in creative mode to use this command."));
    //                                      }
    //                                      return 0;
    //                                  }
    //
    //                                  context.getSource().sendFeedback(Component.literal(
    //                                          "Spawned " + amount + " " + birdName + (amount == 1 ? "" : "s")
    //                                                  + " at your location."));
    //                                  return amount;
    //                              }))
    //                      .executes(context -> {
    //                          String birdName = StringArgumentType.getString(context, "bird");
    //                          Minecraft minecraft = Minecraft.getInstance();
    //                          int amount = spawnBirds(birdName, 1, minecraft);
    //                          if (amount <= 0) {
    //                              SpawnData spawnData = findSpawnData(birdName);
    //                              if (spawnData == null) {
    //                                  context.getSource().sendFeedback(Component.literal(
    //                                          "Unknown bird type '" + birdName + "'. Available birds: "
    //                                                  + String.join(", ", SpawnData.names()) + "."));
    //                              } else if (minecraft.player == null || minecraft.level == null) {
    //                                  context.getSource().sendFeedback(Component.literal(
    //                                          "You must be in a world to spawn birds."));
    //                              } else if (!minecraft.player.getAbilities().instabuild) {
    //                                  context.getSource().sendFeedback(Component.literal(
    //                                          "You must be in creative mode to use this command."));
    //                              }
    //                              return 0;
    //                          }
    //
    //                          context.getSource().sendFeedback(Component.literal(
    //                                  "Spawned " + amount + " " + birdName + (amount == 1 ? "" : "s")
    //                                          + " at your location."));
    //                          return amount;
    //                      }));
    //
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
    //                        .executes(context -> {
    //                          int removed = BaseBirdParticle.clearAllParticles();
    //                          
    //                          String message = "Removed " + removed + " bird particle" + (removed == 1 ? "" : "s") + ".";
    //                          context.getSource().sendFeedback(Component.literal(message));
    //                          return 1;
    //                      }))
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
                                            context.getSource().sendFeedback(Component.literal(
                                                    "Unknown bird type '" + birdName + "'. Available birds: "
                                                            + String.join(", ", SpawnData.names()) + "."));
                                        } else if (minecraft.player == null || minecraft.level == null) {
                                            context.getSource().sendFeedback(Component.literal(
                                                    "You must be in a world to spawn birds."));
                                        } else if (!minecraft.player.getAbilities().instabuild) {
                                            context.getSource().sendFeedback(Component.literal(
                                                    "You must be in creative mode to use this command."));
                                        }
                                        return 0;
                                    }

                                    context.getSource().sendFeedback(Component.literal(
                                            "Spawned " + amount + " " + birdName + (amount == 1 ? "" : "s")
                                                    + " at your location."));
                                    return amount;
                                    }))
                                .executes(context -> {
                                    String birdName = StringArgumentType.getString(context, "bird");
                                    Minecraft minecraft = Minecraft.getInstance();
                                    int amount = spawnBirds(birdName, 1, minecraft);

                                    if (amount <= 0) {
                                    SpawnData spawnData = findSpawnData(birdName);
                                    if (spawnData == null) {
                                        context.getSource().sendFeedback(Component.literal(
                                            "Unknown bird type '" + birdName + "'. Available birds: "
                                                + String.join(", ", SpawnData.names()) + "."));
                                    } else if (minecraft.player == null || minecraft.level == null) {
                                        context.getSource().sendFeedback(Component.literal(
                                            "You must be in a world to spawn birds."));
                                    } else if (!minecraft.player.getAbilities().instabuild) {
                                        context.getSource().sendFeedback(Component.literal(
                                            "You must be in creative mode to use this command."));
                                    }
                                    return 0;
                                    }

                                    context.getSource().sendFeedback(Component.literal(
                                        "Spawned " + amount + " " + birdName + (amount == 1 ? "" : "s")
                                            + " at your location."));
                                    return amount;
                                }));

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
                                int removed = BaseBirdParticle.clearAllParticles();

                                String message = "Removed " + removed + " bird particle" + (removed == 1 ? "" : "s") + ".";
                                context.getSource().sendFeedback(Component.literal(message));
                                return 1;
                            }))
                    .then(spawnCommand));
        });
    //?}
    }
}