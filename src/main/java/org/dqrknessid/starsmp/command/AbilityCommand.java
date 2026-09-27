package org.dqrknessid.starsmp.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.manager.CooldownManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.player.PlayerDataManager;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;

import java.util.ArrayList;
import java.util.List;

public class AbilityCommand implements CommandExecutor, TabCompleter {

    private final StarSMP plugin;
    private final StarManager starManager;
    private final CooldownManager cooldownManager;
    private final PlayerDataManager dataManager;

    public AbilityCommand(StarSMP plugin, StarManager starManager, CooldownManager cooldownManager,
            PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.cooldownManager = cooldownManager;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use abilities.");
            return true;
        }

        PlayerData data = dataManager.getState(player);
        if (data.getActivationMode() != PlayerData.ActivationMode.COMMAND) {
            MessageUtil.send(player,
                    "&cYour ability mode is set to OFFHAND (F). Use /controls to change it to COMMAND.");
            return true;
        }

        StarType type = starManager.getActiveStar(player);
        if (type == null) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.no-star", "&cYou don't have a Star yet."));
            return true;
        }

        TierState tier = starManager.getActiveTier(player);
        if (tier == TierState.SHATTERED || tier == TierState.DIM) {
            MessageUtil.send(player, "&cYour star must be Awakened (Tier 1) or higher to use abilities.");
            return true;
        }

        // Star must be anywhere in inventory or on cursor
        boolean hasStar = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.getItemManager().getStarTypeFromItem(item) == type) {
                hasStar = true;
                break;
            }
        }
        
        if (!hasStar && plugin.getItemManager().getStarTypeFromItem(player.getItemOnCursor()) == type) {
            hasStar = true;
        }

        if (!hasStar) {
            MessageUtil.send(player, "&cYou must have your Star in your inventory to use abilities.");
            return true;
        }

        Star star = starManager.getStarDefinition(type);
        if (star == null)
            return true;

        if (cooldownManager.isOnCooldown(player, 1)) {
            return true; // Message is sent inside CooldownManager
        }

        star.triggerAbility(player, tier);
        if (type == StarType.UMBRYX) {
            // Internally handled by UmbryxStar Dome cleanup task
        } else {
            int seconds = cooldownManager.getConfigCooldown(type, tier, 1);
            cooldownManager.setCooldown(player, 1, seconds);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
