package org.dqrknessid.starsmp.listener;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NebulaGuardListener implements Listener {

    private final StarSMP plugin;
    private final ItemManager itemManager;
    private final Map<UUID, Long> shieldWindows = new HashMap<>();

    public NebulaGuardListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        startVisualTask();
    }

    private void startVisualTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shieldWindows.containsKey(player.getUniqueId())) {
                        long expiry = shieldWindows.get(player.getUniqueId());
                        if (System.currentTimeMillis() > expiry) {
                            shieldWindows.remove(player.getUniqueId());
                            MessageUtil.send(player, "&d&l✦ VOID REPULSION ENDED! &7Your protective field has collapsed.");
                            continue;
                        }

                        // Shield visual (Rotating particles)
                        double time = System.currentTimeMillis() / 1000.0;
                        for (int i = 0; i < 3; i++) {
                            double angle = time * 2.0 + (i * (Math.PI * 2 / 3));
                            double x = Math.cos(angle) * 1.5;
                            double z = Math.sin(angle) * 1.5;
                            player.getWorld().spawnParticle(Particle.valueOf("WITCH"), player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0);
                            player.getWorld().spawnParticle(Particle.valueOf("END_ROD"), player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;
        if (!itemManager.isCustomItem(item, "NEBULA_GUARD")) return;

        event.setCancelled(true);

        // Cooldown ID 7 for Nebula Guard
        if (plugin.getCooldownManager().isOnCooldown(player, 7)) return;
        if (shieldWindows.containsKey(player.getUniqueId())) return;

        // Activate Ability
        int cooldown = plugin.getConfig().getInt("items.nebula-guard.ability-cooldown", 90);
        plugin.getCooldownManager().setCooldown(player, 7, cooldown);
        
        int durationTicks = plugin.getConfig().getInt("items.nebula-guard.repulsion-duration-ticks", 100);
        shieldWindows.put(player.getUniqueId(), System.currentTimeMillis() + (durationTicks * 50L)); 

        // Gravity Shockwave
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.5f);
        
        int radius = plugin.getConfig().getInt("items.nebula-guard.repulsion-radius", 6);
        double strength = plugin.getConfig().getDouble("items.nebula-guard.repulsion-strength", 2.0);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && e != player) {
                Vector dir = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                le.setVelocity(dir.multiply(strength).setY(0.8));
                le.getWorld().spawnParticle(Particle.valueOf("REVERSE_PORTAL"), le.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }

        MessageUtil.send(player, "&d&l✦ VOID REPULSION! &7Nearby enemies knocked back. Projectile immunity for " + (durationTicks/20) + "s.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Projectile)) return;

        if (shieldWindows.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
            player.getWorld().spawnParticle(Particle.valueOf("FLASH"), event.getDamager().getLocation(), 1);
        }
    }
}
