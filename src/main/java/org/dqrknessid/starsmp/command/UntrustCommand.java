package org.dqrknessid.starsmp.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.UUID;

public class UntrustCommand implements CommandExecutor {

    private final StarSMP plugin;

    public UntrustCommand(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.send(player, "&cUsage: /untrust <player>");
            return true;
        }

        PlayerData data = plugin.getStarManager().getPlayerData(player);
        UUID targetId = null;
        String targetName = args[0];

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            targetId = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Try to find in trusted list by name/UUID? 
            // Simplified: only online players for now, or match UUID if offline
            // But usually untrust should work even if offline if we had a name cache.
            // Let's just use what's in the set.
            for (UUID uuid : data.getTrustedPlayers()) {
                // If we had a name cache we could check here.
                // For now, only online or exact UUID.
                if (uuid.toString().equalsIgnoreCase(args[0])) {
                    targetId = uuid;
                    break;
                }
            }
        }

        if (targetId == null || !data.isTrusted(targetId)) {
            MessageUtil.send(player, "&c" + targetName + " is not in your trusted list.");
            return true;
        }

        data.untrustPlayer(targetId);
        plugin.getPlayerDataManager().saveState(player);
        MessageUtil.send(player, "&aYou have untrusted &e" + targetName + "&a. They will now be affected by your abilities again.");
        return true;
    }
}
