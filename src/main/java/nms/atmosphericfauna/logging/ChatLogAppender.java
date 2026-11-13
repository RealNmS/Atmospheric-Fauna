package nms.atmosphericfauna.logging;

import nms.atmosphericfauna.AtmosphericFauna;
import nms.atmosphericfauna.config.ConfigHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ChatLogAppender extends AbstractAppender {
    private static final String APPENDER_NAME = AtmosphericFauna.MOD_ID + "-chat";

    private ChatLogAppender() {
        super(APPENDER_NAME, null, PatternLayout.createDefaultLayout(), false, null);
    }

    public static void install() {
        Logger logger = (Logger) LogManager.getContext(false).getLogger(AtmosphericFauna.MOD_ID);
        if (logger.getAppenders().containsKey(APPENDER_NAME)) {
            return;
        }

        ChatLogAppender appender = new ChatLogAppender();
        appender.start();
        logger.addAppender(appender);
    }

    @Override
    public void append(LogEvent event) {
        if (!ConfigHandler.showLogsInChat) {
            return;
        }

        String message = event.getMessage().getFormattedMessage();
        Minecraft.getInstance().execute(() -> {
            if (ConfigHandler.showLogsInChat && Minecraft.getInstance().player != null) {
                String level = event.getLevel().name();
                MutableComponent levelComponent = Component.literal("[" + level + "]")
                        .withStyle(getLevelColor(level));
                MutableComponent messageComponent = Component.literal(" " + message)
                        .withStyle(ChatFormatting.WHITE);
                //? if >=26.2 {
                Minecraft.getInstance().player.sendSystemMessage(
                        levelComponent.append(messageComponent));
                //?} else {
                // Minecraft.getInstance().player.displayClientMessage(
                //         levelComponent.append(messageComponent), false);
                //?}
            }
        });
    }

    private static ChatFormatting getLevelColor(String level) {
        return switch (level) {
            case "TRACE", "DEBUG" -> ChatFormatting.GRAY;
            case "INFO" -> ChatFormatting.GREEN;
            case "WARN" -> ChatFormatting.YELLOW;
            case "ERROR" -> ChatFormatting.RED;
            case "FATAL" -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.WHITE;
        };
    }
}
