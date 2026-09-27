package org.dqrknessid.starsmp.manager;

import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.star.StarType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class CooldownManager {

    private final StarSMP plugin;
    private final StarManager starManager;
    private final Map<UUID, Map<Integer, Long>> cooldownsMap;
    private final Set<UUID> zeroCooldownPlayers = new HashSet<>();
    private final Set<UUID> crownBoostedPlayers = new HashSet<>();

    public CooldownManager(StarSMP plugin, StarManager starManager) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.cooldownsMap = new HashMap<>();
        startTask();
    }

    private void startTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(cooldownsMap.keySet())) {
                Player player = org.bukkit.Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                Map<Integer, Long> cds = cooldownsMap.get(uuid);
                if (cds == null) continue;

                for (int index : new HashSet<>(cds.keySet())) {
                    long expiry = cds.get(index);
                    if (System.currentTimeMillis() >= expiry) {
                        cds.remove(index);
                        String abilityName = getAbilityName(index);
                        MessageUtil.send(player, "&a" + abilityName + " Ready");
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                    }
                }
            }
        }, 5L, 5L);
    }

    private String getAbilityName(int index) {
        return switch (index) {
            case 1 -> "Ability";
            case 2 -> "Double Jump";
            case 3 -> "Cosmic Judgement";
            case 4 -> "Mace Hit";
            case 5 -> "Starfall Overdrive";
            case 6 -> "Starlight Oath";
            default -> "Ability";
        };
    }

    public void toggleZeroCooldown(Player player) {
        if (zeroCooldownPlayers.contains(player.getUniqueId())) {
            zeroCooldownPlayers.remove(player.getUniqueId());
            MessageUtil.send(player, "&eZero cooldown mode disabled.");
        } else {
            zeroCooldownPlayers.add(player.getUniqueId());
            MessageUtil.send(player, "&aZero cooldown mode enabled.");
        }
    }

    public boolean isOnCooldown(Player player, int abilityIndex) {
        return isOnCooldown(player, abilityIndex, true);
    }

    public boolean isOnCooldownQuietly(Player player, int abilityIndex) {
        return isOnCooldown(player, abilityIndex, false);
    }

    private boolean isOnCooldown(Player player, int abilityIndex, boolean notify) {
        UUID id = player.getUniqueId();
        if (zeroCooldownPlayers.contains(id))
            return false;
        if (!cooldownsMap.containsKey(id) || !cooldownsMap.get(id).containsKey(abilityIndex))
            return false;

        long expiry = cooldownsMap.get(id).get(abilityIndex);
        if (System.currentTimeMillis() >= expiry) {
            cooldownsMap.get(id).remove(abilityIndex);
            if (notify) {
                String abilityName = getAbilityName(abilityIndex);
                MessageUtil.send(player, "&a" + abilityName + " Ready");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
            }
            return false;
        }

        if (notify) {
            long remainingSec = (expiry - System.currentTimeMillis()) / 1000 + 1;
            String msg = plugin.getConfig().getString("messages.cooldown",
                    "&cAbility on cooldown! &7%seconds%s remaining.");
            MessageUtil.send(player, msg.replace("%seconds%", String.valueOf(remainingSec)));
        }
        return true;
    }

    public void setCooldown(Player player, int abilityIndex, int seconds) {
        UUID id = player.getUniqueId();
        cooldownsMap.putIfAbsent(id, new HashMap<>());

        // Chronovar passive: cooldown reduction
        StarType star = starManager.getActiveStar(player);
        if (star == StarType.CHRONOVAR) {
            double cdr = plugin.getConfig().getDouble("stars.chronovar.passive-cdr", 0.10);
            seconds = Math.max(1, (int) (seconds * (1.0 - cdr)));
        }

        // Crown of Astral Supremacy: halve cooldown
        if (crownBoostedPlayers.contains(id)) {
            seconds = Math.max(1, seconds / 2);
        }

        cooldownsMap.get(id).put(abilityIndex, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getConfigCooldown(StarType type, org.dqrknessid.starsmp.data.TierState tier, int abilityIndex) {
        String starKey = type.name().toLowerCase();
        int defaultCd = 90;
        if (tier != null) {
            if (type == StarType.AETHERION) {
                defaultCd = 180;
            } else if (tier.getLevel() == 2) {
                defaultCd = 75;
            } else if (tier.getLevel() >= 3) {
                defaultCd = 60;
            }
        }
        return plugin.getConfig().getInt("stars." + starKey + ".cooldown", defaultCd);
    }

    public void clearCooldowns(Player player) {
        UUID id = player.getUniqueId();
        cooldownsMap.remove(id);
        zeroCooldownPlayers.remove(id);
        crownBoostedPlayers.remove(id);
    }

    public void setCrownBoost(UUID uuid, boolean boosted) {
        if (boosted) {
            crownBoostedPlayers.add(uuid);
        } else {
            crownBoostedPlayers.remove(uuid);
        }
    }
}
