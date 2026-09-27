package org.dqrknessid.starsmp.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dqrknessid.starsmp.StarSMP;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LegendaryLimitManager {

    private final StarSMP plugin;
    private final File dataFile;
    private FileConfiguration data;
    private final Map<String, Integer> currentCounts = new HashMap<>();

    public LegendaryLimitManager(StarSMP plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "legendary_limits.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        // Initial setup for existing legendary items
        currentCounts.clear();
        for (String key : data.getKeys(false)) {
            currentCounts.put(key, data.getInt(key, 0));
        }
    }

    public synchronized void saveData() {
        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            data.set(entry.getKey(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException ignored) {}
    }

    public synchronized boolean canStillBeCrafted(String itemType) {
        // Global Limiter checks
        if (itemType.equals("COSMIC_JUDGEMENT") && plugin.getConfig().getBoolean("limits.MaceLimiter", true)) {
            if (getCurrentCount("COSMIC_JUDGEMENT") >= 1) return false;
        }
        if (itemType.equals("SPEAR_FALLING_STAR") && plugin.getConfig().getBoolean("limits.SpearLimiter", true)) {
            if (getCurrentCount("SPEAR_FALLING_STAR") >= 1) return false;
        }

        int limit = plugin.getConfig().getInt("limits." + itemType, -1);
        if (limit == -1) return true; // No limit
        
        int currentCount = currentCounts.getOrDefault(itemType, 0);
        return currentCount < limit;
    }

    public synchronized void incrementCount(String itemType) {
        currentCounts.put(itemType, currentCounts.getOrDefault(itemType, 0) + 1);
        saveData();
    }

    public synchronized void decrementCount(String itemType) {
        int count = currentCounts.getOrDefault(itemType, 0);
        if (count > 0) {
            currentCounts.put(itemType, count - 1);
            saveData();
        }
    }

    public int getCurrentCount(String itemType) {
        return currentCounts.getOrDefault(itemType, 0);
    }

    public synchronized void resetAllCounts() {
        currentCounts.clear();
        for (String key : data.getKeys(false)) {
            data.set(key, 0);
        }
        saveData();
    }

    public int getLimit(String itemType) {
        return plugin.getConfig().getInt("limits." + itemType, -1);
    }

    public boolean isCraftingEnabled() {
        return plugin.getConfig().getBoolean("limits.legendary-craftable", true);
    }
}
