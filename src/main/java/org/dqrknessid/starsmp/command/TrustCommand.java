package org.dqrknessid.starsmp.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.util.MessageUtil;

public class TrustCommand implements CommandExecutor {

    private final StarSMP plugin;

    public TrustCommand(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.send(player, "&cUsage: /trust <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.send(player, "&cPlayer not found.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.send(player, "&cYou cannot trust yourself.");
            return true;
        }

        PlayerData data = plugin.getStarManager().getPlayerData(player);
        if (data.isTrusted(target.getUniqueId())) {
            MessageUtil.send(player, "&c" + target.getName() + " is already trusted.");
            return true;
        }

        data.trustPlayer(target.getUniqueId());
        plugin.getPlayerDataManager().saveState(player);
        MessageUtil.send(player, "&aYou have trusted &e" + target.getName() + "&a. They will no longer be affected by your abilities.");
        return true;
    }
}
