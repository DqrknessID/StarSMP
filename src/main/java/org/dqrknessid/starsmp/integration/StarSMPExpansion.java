package org.dqrknessid.starsmp.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.star.StarType;

public class StarSMPExpansion extends PlaceholderExpansion {

    private final StarSMP plugin;
    private final StarManager starManager;

    public StarSMPExpansion(StarSMP plugin, StarManager starManager) {
        this.plugin = plugin;
        this.starManager = starManager;
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "starsmp";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        StarType activeStar = starManager.getActiveStar(player);
        TierState activeTier = starManager.getActiveTier(player);

        switch (params.toLowerCase()) {
            case "star":
                return activeStar != null ? activeStar.getName() : "None";
            case "star_symbol":
                return activeStar != null ? activeStar.getSymbol() : "✦";
            case "star_color":
                if (activeStar == null) return "&7";
                return switch (activeStar) {
                    case AQUALITH -> "&b";
                    case PYRELION -> "&c";
                    case ZEPHYROS -> "&f";
                    case GRAVARYN -> "&5";
                    case VOLTYRA -> "&e";
                    case VERDARYN -> "&a";
                    case UMBRYX -> "&8";
                    case LUMINARY -> "&6";
                    case AETHERION -> "&d";
                    case CHRONOVAR -> "&3";
                };
            case "tier":
                return activeTier != null ? activeTier.getDisplayName() : "None";
            case "tier_level":
                return activeTier != null ? String.valueOf(activeTier.getLevel()) : "-1";
            case "passive":
                return starManager.isPassiveActive(player) ? "Enabled" : "Disabled";
            case "energy":
                return String.valueOf(starManager.getEnergy(player));
        }

        return null; 
    }
}
