package nms.atmosphericfauna.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

public class ModCommands {

    public static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("atmosphericfauna")
                    .executes(context -> {
                        context.getSource()
                                .sendFeedback(Component.literal("Welcome to Atmospheric Fauna!"));
                        return 1;
                    }));
        });
    }
}