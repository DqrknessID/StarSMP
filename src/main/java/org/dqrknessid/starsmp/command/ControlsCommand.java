package org.dqrknessid.starsmp.command;

import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;

public class ControlsCommand implements CommandExecutor, TabCompleter {
    private final PlayerDataManager dataManager;

    public ControlsCommand(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use this command.");
            return true;
        }

        PlayerData data = dataManager.getState(player);
        if (data.getActivationMode() == PlayerData.ActivationMode.OFFHAND) {
            data.setActivationMode(PlayerData.ActivationMode.COMMAND);
            MessageUtil.send(player, "&aAbility activation mode set to: COMMAND (/ability1)");
        } else {
            data.setActivationMode(PlayerData.ActivationMode.OFFHAND);
            MessageUtil.send(player, "&aAbility activation mode set to: OFFHAND (Swap items - F)");
        }
        dataManager.saveState(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
