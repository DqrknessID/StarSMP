package org.dqrknessid.starsmp.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StarlightOathListener implements Listener {

    private final StarSMP plugin;
    private final ItemManager itemManager;
    private final Map<UUID, Long> vowWindows = new HashMap<>();

    public StarlightOathListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        startVisualTask();
        startGlowTask();
    }

    private void startVisualTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Long expiry = vowWindows.get(player.getUniqueId());
                    if (expiry == null || System.currentTimeMillis() > expiry) {
                        if (expiry != null) {
                            vowWindows.remove(player.getUniqueId());
                            player.removePotionEffect(PotionEffectType.GLOWING);
                            MessageUtil.send(player, "&b&l✦ CELESTIAL VOW ENDED! &7Your celestial trance has faded.");
                        }
                        continue;
                    }

                    // Active Visuals
                    player.getWorld().spawnParticle(Particle.valueOf("END_ROD"), player.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.02);
                    if (System.currentTimeMillis() % 1000 < 500) {
                        player.getWorld().spawnParticle(Particle.valueOf("WITCH"), player.getLocation().add(0, 1, 0), 2, 0.2, 0.5, 0.2, 0.01);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // Glow task for Starlight Oath sword
    private void startGlowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (itemManager.isCustomItem(player.getInventory().getItemInMainHand(), "STARLIGHT_OATH")) {
                        // Apply glowing effect
                        if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 110, 0, false, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;
        if (!itemManager.isCustomItem(item, "STARLIGHT_OATH")) return;

        event.setCancelled(true);

        if (plugin.getCooldownManager().isOnCooldown(player, 6)) return;
        if (vowWindows.containsKey(player.getUniqueId())) return;

        // Activate Vow (Nerfed: 140s from 120s)
        int cooldown = plugin.getConfig().getInt("items.starlight-oath.ability-cooldown", 140);
        plugin.getCooldownManager().setCooldown(player, 6, cooldown);
        
        int durationTicks = plugin.getConfig().getInt("items.starlight-oath.vow-duration-ticks", 600);
        vowWindows.put(player.getUniqueId(), System.currentTimeMillis() + (durationTicks * 50L)); 
        
        int strengthLevel = plugin.getConfig().getInt("items.starlight-oath.strength-level", 2) - 1;
        int speedLevel = plugin.getConfig().getInt("items.starlight-oath.speed-level", 1) - 1;

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, strengthLevel)); 
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, speedLevel)); 
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0));
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
        
        MessageUtil.send(player, "&b&l✦ CELESTIAL VOW! &7You have entered a " + (durationTicks/20) + "s celestial trance.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwordAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Check Vow Window
        Long expiry = vowWindows.get(attacker.getUniqueId());
        if (expiry == null || System.currentTimeMillis() > expiry) return;
        
        // If holding the sword
        if (!itemManager.isCustomItem(attacker.getInventory().getItemInMainHand(), "STARLIGHT_OATH")) return;

        // 1. Auto Crit Logic
        // Boost damage by 50% (standard crit multiplier)
        double baseDamage = event.getDamage();
        event.setDamage(baseDamage * 1.5);

        // Crit Visuals & Sound
        attacker.getWorld().spawnParticle(Particle.valueOf("CRIT"), event.getEntity().getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

        // 2. Shield Bypass Logic (Player vs Player)
        if (event.getEntity() instanceof Player victim) {
            if (victim.isBlocking() && victim.getCooldown(Material.SHIELD) <= 0) {
                // Disable shield (Nerfed: 60 ticks / 3 seconds)
                int breakTicks = plugin.getConfig().getInt("items.starlight-oath.shield-break-duration-ticks", 60);
                victim.setCooldown(Material.SHIELD, breakTicks);
                victim.clearActiveItem(); // Force stop blocking
                
                // Visuals for shield break
                attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
                attacker.getWorld().spawnParticle(Particle.valueOf("CRIT"), victim.getLocation().add(0, 1.2, 0), 15, 0.4, 0.1, 0.4, 0.2);
                
                MessageUtil.send(attacker, "&e&l✦ SHIELD BREAKER! &7Your strike pierced " + victim.getName() + "'s defense.");
            }
        }
    }


}
