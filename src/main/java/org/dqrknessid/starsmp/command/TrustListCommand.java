package org.dqrknessid.starsmp.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrustListCommand implements CommandExecutor {

    private final StarSMP plugin;

    public TrustListCommand(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use this command.");
            return true;
        }

        PlayerData data = plugin.getStarManager().getPlayerData(player);
        Set<UUID> trusted = data.getTrustedPlayers();

        if (trusted.isEmpty()) {
            MessageUtil.send(player, "&cYou haven't trusted any players yet.");
            return true;
        }

        String names = trusted.stream()
                .map(uuid -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    String name = offlinePlayer.getName();
                    return name != null ? name : uuid.toString();
                })
                .collect(Collectors.joining("&7, &e"));

        MessageUtil.send(player, "&aYour Trusted Players: &e" + names);
        return true;
    }
}
