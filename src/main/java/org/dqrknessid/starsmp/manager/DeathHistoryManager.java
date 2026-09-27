package org.dqrknessid.starsmp.manager;

import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeathHistoryManager {

    private final StarSMP plugin;
    private final Map<UUID, List<DeathRecord>> history = new HashMap<>();

    public DeathHistoryManager(StarSMP plugin) {
        this.plugin = plugin;
    }

    public void recordDeath(Player player, String cause, String killer) {
        UUID uuid = player.getUniqueId();
        history.putIfAbsent(uuid, new ArrayList<>());
        
        List<DeathRecord> records = history.get(uuid);
        records.add(new DeathRecord(LocalDateTime.now(), cause, killer));
        
        int maxSize = plugin.getConfig().getInt("global.death-history-max-size", 20);
        if (records.size() > maxSize) {
            records.remove(0);
        }
    }

    public List<DeathRecord> getHistory(UUID uuid) {
        return history.getOrDefault(uuid, Collections.emptyList());
    }

    public static record DeathRecord(LocalDateTime timestamp, String cause, String killer) {
        public String format() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "&7[" + timestamp.format(formatter) + "] &cCause: &f" + cause + (killer != null ? " &7(by &e" + killer + "&7)" : "");
        }
    }
}
