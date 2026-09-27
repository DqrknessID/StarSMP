package org.dqrknessid.starsmp.listener;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.manager.CooldownManager;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PassiveTickListener implements Runnable {

    private final StarSMP plugin;
    private final StarManager starManager;
    private final ItemManager itemManager;
    private final CooldownManager cooldownManager;
    private final Set<UUID> crownBoostedPlayers = new HashSet<>();
    private final Map<UUID, Long> lastRegenMillis = new HashMap<>();

    public PassiveTickListener(StarSMP plugin, StarManager starManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.itemManager = itemManager;
        this.cooldownManager = plugin.getCooldownManager();
    }

    private int tickCounter = 0;

    public void start() {
        // Run every tick (1L) to support smooth movement passives
        Bukkit.getScheduler().runTaskTimer(plugin, this, 1L, 1L);
    }

    @Override
    public void run() {
        tickCounter++;
        boolean isHeavyTick = (tickCounter % 40 == 0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // ── Crown of Astral Supremacy passive effects ──
            if (isHeavyTick) {
                ItemStack helmet = player.getInventory().getHelmet();
                boolean wearingCrown = itemManager.isCustomItem(helmet, "CROWN_ASTRAL_SUPREMACY");
                applyOrRemoveCrownEffects(player, wearingCrown);
            }

            // ── Star passives ──
            if (!starManager.isPassiveActive(player))
                continue;

            StarType activeType = starManager.getActiveStar(player);
            if (activeType == null)
                continue;

            TierState tier = starManager.getActiveTier(player);
            if (tier == TierState.SHATTERED)
                continue;

            // Movement passives (Lava Swimming, etc.) run every tick
            // Heavy passives (Potion Effects, Inventory Checks) run every 40 ticks
            if (isHeavyTick) {
                handleHeavyPassives(player, activeType, tier);
            }
            
            // Movement passives
            Star star = starManager.getStarDefinition(activeType);
            if (star != null) {
                star.applyPassives(player, tier);
            }
        }
    }

    private void handleHeavyPassives(Player player, StarType activeType, TierState tier) {
        boolean foundCorrectStar = false;
        String playerUuidStr = player.getUniqueId().toString();
        boolean isAdmin = player.hasPermission("starsmp.admin");

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (itemManager.isCustomItem(item, "STAR")) {
                StarType typeInItem = itemManager.getStarTypeFromItem(item);
                String ownerUuid = itemManager.getOwnerUuidFromItem(item);

                if (typeInItem == activeType && (playerUuidStr.equals(ownerUuid) || ownerUuid == null) && !foundCorrectStar) {
                    foundCorrectStar = true;
                } else if (!isAdmin) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        
        ItemStack cursorItem = player.getItemOnCursor();
        if (itemManager.isCustomItem(cursorItem, "STAR")) {
            StarType typeInItem = itemManager.getStarTypeFromItem(cursorItem);
            String ownerUuid = itemManager.getOwnerUuidFromItem(cursorItem);

            if (typeInItem == activeType && (playerUuidStr.equals(ownerUuid) || ownerUuid == null) && !foundCorrectStar) {
                foundCorrectStar = true;
            } else if (!isAdmin) {
                player.setItemOnCursor(null);
            }
        }
        
        // This used to call applyPassives here, but now we call it in the main loop
        // to support movement. We should probably separate applyPassives into 
        // applyPeriodicPassives and applyTickPassives in the future, but for now 
        // each star's applyPassives will handle its own frequency internally if needed.
    }

    private void applyOrRemoveCrownEffects(Player player, boolean wearing) {
        UUID uuid = player.getUniqueId();
        if (wearing) {
            // 1. HP boost to 40 (20 hearts)
            AttributeInstance attr = getMaxHealthAttribute(player);
            if (attr != null && attr.getBaseValue() < 40.0) {
                attr.setBaseValue(40.0);
            }

            // 2. Strength I and Resistance I
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 50, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, false, false, true));

            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false, false));

            // 4. Glowing Aura (20 blocks)
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(20, 20, 20)) {
                if (entity instanceof Player nearby && nearby != player) {
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false, false));
                }
            }

            // 5. Regeneration (1 heart / 10s)
            long now = System.currentTimeMillis();
            long lastRegen = lastRegenMillis.getOrDefault(uuid, 0L);
            if (now - lastRegen >= 10000) {
                double currentHealth = player.getHealth();
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    double maxHealth = maxHealthAttr.getValue();
                    if (currentHealth < maxHealth) {
                        player.setHealth(Math.min(maxHealth, currentHealth + 2.0));
                    }
                }
                lastRegenMillis.put(uuid, now);
            }

            // 6. Half star cooldowns
            if (!crownBoostedPlayers.contains(uuid)) {
                crownBoostedPlayers.add(uuid);
                cooldownManager.setCrownBoost(uuid, true);
            }
        } else {
            if (crownBoostedPlayers.contains(uuid)) {
                crownBoostedPlayers.remove(uuid);
                lastRegenMillis.remove(uuid);
                cooldownManager.setCrownBoost(uuid, false);

                // Restore max health to default 20 (stays the same in vanilla)
                AttributeInstance attr = getMaxHealthAttribute(player);
                if (attr != null && attr.getBaseValue() > 20.0) {
                    attr.setBaseValue(20.0);
                }
            }
        }
    }

    /** Gets the MAX_HEALTH attribute. */
    private AttributeInstance getMaxHealthAttribute(Player player) {
        return player.getAttribute(Attribute.MAX_HEALTH);
    }

    public void removePlayer(UUID uuid) {
        crownBoostedPlayers.remove(uuid);
        lastRegenMillis.remove(uuid);
        cooldownManager.setCrownBoost(uuid, false);
    }
}
