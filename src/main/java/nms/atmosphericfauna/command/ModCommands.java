package nms.atmosphericfauna.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import nms.atmosphericfauna.gui.DebugHudOverlay;

public class ModCommands {

    public static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("atmosphericfauna")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Welcome to Atmospheric Fauna!"));
                        return 1;
                    })
                    .then(ClientCommands.literal("debug")
                            .executes(context -> {
                                DebugHudOverlay.showDebug = !DebugHudOverlay.showDebug;

                                String state = DebugHudOverlay.showDebug ? "enabled" : "disabled";
                                context.getSource().sendFeedback(
                                        Component.literal("Atmospheric Fauna debug scoreboard " + state + "."));
                                return 1;
                            })));
        });
    }
}