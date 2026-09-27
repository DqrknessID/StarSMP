package org.dqrknessid.starsmp.manager;

import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.player.PlayerDataManager;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StarManager {

    private final StarSMP plugin;
    private final PlayerDataManager dataManager;
    private final Map<StarType, Star> registeredStars;
    private final Random random;

    public StarManager(StarSMP plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.registeredStars = new HashMap<>();
        this.random = new Random();
    }

    public void registerStar(Star star) {
        registeredStars.put(star.getType(), star);
    }

    public Star getStarDefinition(StarType type) {
        return registeredStars.get(type);
    }

    public Map<StarType, Star> getAllStars() {
        return registeredStars;
    }

    // ─── Give / Set ───

    public void giveRandomStar(Player player) {
        StarType[] types = StarType.values();
        StarType randomType = types[random.nextInt(types.length)];
        setStar(player, randomType);
    }

    public void setStar(Player player, StarType type) {
        PlayerData state = dataManager.getState(player);
        state.setCurrentStar(type);
        state.setCurrentTier(TierState.DIM);
        state.setHasReceivedInitialStar(true);
        dataManager.saveState(player);

        String msg = plugin.getConfig().getString("messages.received-star", "&aYou have received the %star%&a!");
        MessageUtil.send(player, msg.replace("%star%", type.getSymbol() + " " + type.getName()));
    }

    // ─── Remove / Delete ───

    public void removeStar(Player player) {
        PlayerData state = dataManager.getState(player);
        state.setCurrentStar(null);
        state.setCurrentTier(TierState.DIM);
        state.setEnergy(0);
        dataManager.saveState(player);
    }

    public void deleteStarData(Player player) {
        dataManager.deleteStarData(player);
    }

    // ─── Tier ───

    public boolean increaseTier(Player player) {
        PlayerData state = dataManager.getState(player);
        if (state.getCurrentStar() == null)
            return false;

        int currentLvl = state.getCurrentTier().getLevel();
        if (currentLvl == -1) {
            String msg = plugin.getConfig().getString("messages.shattered-star",
                    "&4Your Star is Shattered! Repair it first.");
            MessageUtil.send(player, msg);
            return false;
        }

        if (currentLvl < TierState.ZENITH.getLevel()) {
            state.setCurrentTier(TierState.fromLevel(currentLvl + 1));
            dataManager.saveState(player);

            String msg = plugin.getConfig().getString("messages.tier-up",
                    "&aYour Star has ascended to &e%tier%&a!");
            MessageUtil.send(player, msg.replace("%tier%", state.getCurrentTier().getDisplayName()));
            return true;
        } else {
            String msg = plugin.getConfig().getString("messages.already-max-tier",
                    "&6Your Star is already at Zenith!");
            MessageUtil.send(player, msg);
            return false;
        }
    }

    public void decreaseTier(Player player) {
        PlayerData state = dataManager.getState(player);
        if (state.getCurrentStar() == null)
            return;

        int currentLvl = state.getCurrentTier().getLevel();
        if (currentLvl > TierState.SHATTERED.getLevel()) {
            state.setCurrentTier(TierState.fromLevel(currentLvl - 1));
            dataManager.saveState(player);

            String msg = plugin.getConfig().getString("messages.tier-down",
                    "&cYour Star shattered down to &e%tier%&c...");
            MessageUtil.send(player, msg.replace("%tier%", state.getCurrentTier().getDisplayName()));
        }
    }

    public void setTier(Player player, TierState tier) {
        PlayerData state = dataManager.getState(player);
        state.setCurrentTier(tier);
        dataManager.saveState(player);
    }

    // ─── Repair ───

    public void repairStar(Player player) {
        PlayerData state = dataManager.getState(player);
        if (state.getCurrentStar() != null && state.getCurrentTier() == TierState.SHATTERED) {
            state.setCurrentTier(TierState.DIM);
            dataManager.saveState(player);
            MessageUtil.send(player, "&aYour Star has been repaired! Restored to Dim (Tier 0).");
        } else {
            MessageUtil.send(player, "&cYour Star is not shattered, or you don't have one.");
        }
    }

    // ─── Reroll ───

    public void rerollStar(Player player) {
        StarType current = getActiveStar(player);
        StarType[] types = StarType.values();
        StarType newType;
        do {
            newType = types[random.nextInt(types.length)];
        } while (newType == current && types.length > 1);

        setStar(player, newType); // setStar resets tier to DIM
    }

    // ─── Energy ───

    public int getEnergy(Player player) {
        return dataManager.getState(player).getEnergy();
    }

    public void setEnergy(Player player, int energy) {
        PlayerData state = dataManager.getState(player);
        state.setEnergy(energy);
        dataManager.saveState(player);
    }

    public void addEnergy(Player player, int amount) {
        PlayerData state = dataManager.getState(player);
        state.addEnergy(amount);
        dataManager.saveState(player);
    }

    // ─── Getters ───

    public PlayerData getPlayerData(Player player) {
        return dataManager.getState(player);
    }

    public StarType getActiveStar(Player player) {
        return dataManager.getState(player).getCurrentStar();
    }

    public TierState getActiveTier(Player player) {
        return dataManager.getState(player).getCurrentTier();
    }

    public boolean hasReceivedStar(Player player) {
        return dataManager.getState(player).hasReceivedInitialStar();
    }

    public void setHasReceivedStar(Player player, boolean b) {
        dataManager.getState(player).setHasReceivedInitialStar(b);
        dataManager.saveState(player);
    }

    // ─── Toggle ───

    public void togglePassive(Player player) {
        PlayerData state = dataManager.getState(player);
        state.setPassiveToggled(!state.isPassiveToggled());
        dataManager.saveState(player);
        if (state.isPassiveToggled()) {
            MessageUtil.send(player, "&aPassive effects enabled.");
        } else {
            MessageUtil.send(player, "&ePassive effects disabled.");
        }
    }

    public boolean isPassiveActive(Player player) {
        return dataManager.getState(player).isPassiveToggled();
    }

    public void cleanupPlayer(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        for (Star star : registeredStars.values()) {
            star.cleanup(uuid);
        }
    }
}
