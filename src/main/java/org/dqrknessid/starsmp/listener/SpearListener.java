package org.dqrknessid.starsmp.listener;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
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
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.*;

public class SpearListener implements Listener {

    private final StarSMP plugin;
    private final ItemManager itemManager;
    private final Map<UUID, Long> bleedWindows = new HashMap<>();

    private int glowTaskId = -1;

    public SpearListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        startGlowTask();
    }



    private void startGlowTask() {
        glowTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean holdingSpear = itemManager.isCustomItem(player.getInventory().getItemInMainHand(), "SPEAR_FALLING_STAR");
                    if (holdingSpear) {
                        if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 110, 0, false, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
    }



    public void cleanup() {
        if (glowTaskId != -1) {
            Bukkit.getScheduler().cancelTask(glowTaskId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;
        if (!itemManager.isCustomItem(item, "SPEAR_FALLING_STAR")) return;

        event.setCancelled(true);

        // Check Cooldown (3 minutes = 180 seconds)
        if (plugin.getCooldownManager().isOnCooldown(player, 5)) return;

        // Activate Ability
        int cooldown = plugin.getConfig().getInt("items.spear-falling-star.ability-cooldown", 180);
        plugin.getCooldownManager().setCooldown(player, 5, cooldown);
        
        int durationTicks = plugin.getConfig().getInt("items.spear-falling-star.overdrive-duration-ticks", 600);
        int speedLevel = plugin.getConfig().getInt("items.spear-falling-star.overdrive-speed-level", 5) - 1;

        // 1. Give Speed for the duration
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, speedLevel)); 
        
        // 2. Enable Bleed Window for the duration
        bleedWindows.put(player.getUniqueId(), System.currentTimeMillis() + (durationTicks * 50L));
        
        // 3. Effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.8f);
        
        spawnActivationParticles(player);
        
        MessageUtil.send(player, "&e&l✦ STARFALL OVERDRIVE! &7You are now empowered with &bSpeed &f" + (speedLevel + 1) + " &7and &cBleeding Strikes &7for " + (durationTicks/20) + "s.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // Check if holding the spear
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (!itemManager.isCustomItem(item, "SPEAR_FALLING_STAR")) return;

        // Trust check
        PlayerData data = plugin.getStarManager().getPlayerData(attacker);
        if (data.isTrusted(victim.getUniqueId())) return;
        
        // Check Bleed Window
        Long expiry = bleedWindows.get(attacker.getUniqueId());
        if (expiry == null || System.currentTimeMillis() > expiry) {
            bleedWindows.remove(attacker.getUniqueId());
            return;
        }

        // Apply Bleed (5 seconds)
        applyBleed(victim, attacker);
    }

    private void applyBleed(LivingEntity victim, Player attacker) {
        // Simple visual for applying bleed
        attacker.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        int maxTicks = plugin.getConfig().getInt("items.spear-falling-star.bleed-duration-ticks", 100) / 20;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= maxTicks || victim.isDead() || !victim.isValid()) {
                    this.cancel();
                    return;
                }
                
                // Deal 2 damage (1 heart) every second
                victim.damage(2.0, attacker);
                
                // Visuals
                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.RED, 1.5f));
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.5f, 0.8f);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnActivationParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 0.1, 0);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(ticks * 18 + i * 45);
                    double x = 1.0 * Math.cos(angle);
                    double z = 1.0 * Math.sin(angle);
                    try {
                        player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, ticks * 0.05, z), 1, 0, 0, 0, 0.01);
                    } catch (Exception ignored) {}
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

}
