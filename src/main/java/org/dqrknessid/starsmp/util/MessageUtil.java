package org.dqrknessid.starsmp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Parse a legacy string (with & codes) into a Component.
     */
    public static Component parse(String message) {
        if (message == null) return Component.empty();
        return SERIALIZER.deserialize(message);
    }

    /**
     * Send a legacy message to a CommandSender.
     */
    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null) return;
        sender.sendMessage(parse(message));
    }

    /**
     * Broadcast a legacy message to all players.
     */
    public static void broadcast(String message) {
        if (message == null) return;
        Bukkit.broadcast(parse(message));
    }

    /**
     * Convert a list of legacy strings to a list of Components.
     */
    public static List<Component> parseList(List<String> lines) {
        List<Component> components = new ArrayList<>();
        if (lines == null) return components;
        for (String line : lines) {
            components.add(parse(line));
        }
        return components;
    }
}
