package org.dqrknessid.starsmp.player;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.StarType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> cache;

    private final NamespacedKey starKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey energyKey;
    private final NamespacedKey passiveKey;
    private final NamespacedKey initialStarKey;
    private final NamespacedKey activationModeKey;
    private final NamespacedKey trustedPlayersKey;
    private final NamespacedKey firstJoinKey;

    public PlayerDataManager(StarSMP plugin) {
        this.cache = new HashMap<>();
        this.starKey = new NamespacedKey(plugin, "star_type");
        this.tierKey = new NamespacedKey(plugin, "star_tier");
        this.energyKey = new NamespacedKey(plugin, "star_energy");
        this.passiveKey = new NamespacedKey(plugin, "star_passive_toggle");
        this.initialStarKey = new NamespacedKey(plugin, "received_initial_star");
        this.activationModeKey = new NamespacedKey(plugin, "activation_mode");
        this.trustedPlayersKey = new NamespacedKey(plugin, "trusted_players");
        this.firstJoinKey = new NamespacedKey(plugin, "first_join_time");
    }

    public PlayerData getState(Player player) {
        PlayerData cached = cache.get(player.getUniqueId());
        if (cached != null)
            return cached;
        return loadState(player);
    }

    private PlayerData loadState(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        StarType star = null;
        if (pdc.has(starKey, PersistentDataType.STRING)) {
            try {
                star = StarType.valueOf(pdc.get(starKey, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }

        TierState tier = TierState.DIM;
        if (pdc.has(tierKey, PersistentDataType.INTEGER)) {
            tier = TierState.fromLevel(pdc.get(tierKey, PersistentDataType.INTEGER));
        }

        int energy = pdc.getOrDefault(energyKey, PersistentDataType.INTEGER, 0);
        boolean passiveToggle = pdc.getOrDefault(passiveKey, PersistentDataType.BYTE, (byte) 1) == 1;
        boolean receivedInitial = pdc.getOrDefault(initialStarKey, PersistentDataType.BYTE, (byte) 0) == 1;

        PlayerData.ActivationMode activationMode = PlayerData.ActivationMode.OFFHAND; // default
        if (pdc.has(activationModeKey, PersistentDataType.STRING)) {
            try {
                activationMode = PlayerData.ActivationMode
                        .valueOf(pdc.get(activationModeKey, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }

        PlayerData state = new PlayerData(star, tier, energy, passiveToggle, receivedInitial, activationMode);

        long firstJoin = pdc.getOrDefault(firstJoinKey, PersistentDataType.LONG, System.currentTimeMillis());
        state.setFirstJoinTime(firstJoin);

        if (pdc.has(trustedPlayersKey, PersistentDataType.STRING)) {
            String stored = pdc.get(trustedPlayersKey, PersistentDataType.STRING);
            if (stored != null && !stored.isEmpty()) {
                String[] uuids = stored.split(",");
                for (String s : uuids) {
                    try {
                        state.trustPlayer(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        cache.put(player.getUniqueId(), state);
        return state;
    }

    public void saveState(Player player) {
        PlayerData state = cache.get(player.getUniqueId());
        if (state == null)
            return;

        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (state.getCurrentStar() != null) {
            pdc.set(starKey, PersistentDataType.STRING, state.getCurrentStar().name());
        } else {
            pdc.remove(starKey);
        }

        pdc.set(tierKey, PersistentDataType.INTEGER, state.getCurrentTier().getLevel());
        pdc.set(energyKey, PersistentDataType.INTEGER, state.getEnergy());
        pdc.set(passiveKey, PersistentDataType.BYTE, (byte) (state.isPassiveToggled() ? 1 : 0));
        pdc.set(initialStarKey, PersistentDataType.BYTE, (byte) (state.hasReceivedInitialStar() ? 1 : 0));
        pdc.set(activationModeKey, PersistentDataType.STRING, state.getActivationMode().name());
        pdc.set(firstJoinKey, PersistentDataType.LONG, state.getFirstJoinTime());

        java.util.Set<UUID> trusted = state.getTrustedPlayers();
        if (trusted != null && !trusted.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (UUID uuid : trusted) {
                if (!first) sb.append(",");
                sb.append(uuid.toString());
                first = false;
            }
            pdc.set(trustedPlayersKey, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(trustedPlayersKey);
        }
    }

    public void removeState(Player player) {
        cache.remove(player.getUniqueId());
    }

    /** Fully delete a player's star data (admin command) */
    public void deleteStarData(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(starKey);
        pdc.remove(tierKey);
        pdc.remove(energyKey);
        pdc.remove(initialStarKey);
        cache.remove(player.getUniqueId());
    }
}
