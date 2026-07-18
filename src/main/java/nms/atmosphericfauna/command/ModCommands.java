package nms.atmosphericfauna.command;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.BaseBirdParticle;

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
                                String message = "Removed " + removed + " bird particle" + (removed == 1 ? "" : "s")
                                        + ".";
                                context.getSource().sendFeedback(Component.literal(message));
                                return 1;
                            })));
        });
    }
}