package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZephyrosStar implements Star {

    private final StarSMP plugin;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public ZephyrosStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.ZEPHYROS;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        if (player.getTicksLived() % 40 == 0) {
            // No fall damage + Speed I (+20%)
            // Increased duration to 300 ticks (15s) to prevent flickering
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0, true, false, false));
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        // Zephyros doesn't proc on attack
    }

    public void onDamaged(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 2)
            return;
        if (!(event.getEntity() instanceof Player player))
            return;

        // Zephyros doesn't negate falling block damage (e.g. Dripstone)
        if (event.getDamager() instanceof org.bukkit.entity.FallingBlock)
            return;

        if (event.getDamager() instanceof LivingEntity damager) {
            PlayerData data = plugin.getStarManager().getPlayerData(player);
            if (data.isTrusted(damager.getUniqueId())) {
                return; // Ignore trusted players completely for the passive
            }
        }

        int threshold = plugin.getConfig().getInt("stars.zephyros.passive-hit-counter", 9);
        UUID id = player.getUniqueId();
        int hits = hitCounters.getOrDefault(id, 0) + 1;

        if (hits >= threshold) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.5f);
            player.getWorld().spawnParticle(Particle.valueOf("CLOUD"), player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

            if (event.getDamager() instanceof LivingEntity damager) {
                double strength = plugin.getConfig().getDouble("stars.zephyros.active-knockback-strength", 1.5);
                double height = plugin.getConfig().getDouble("stars.zephyros.active-launch-height", 0.5);

                Vector knockback = damager.getLocation().toVector().subtract(player.getLocation().toVector())
                        .normalize().multiply(strength).setY(height);
                damager.setVelocity(knockback);
            }
            hits = 0;
        }
        hitCounters.put(id, hits);
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), "starsmp.ability.zephyros.dash", 2.0f, 1.0f);

        Vector dash = player.getLocation().getDirection().normalize();
        double pitch = player.getLocation().getPitch();
        if (pitch < -45) { // Looking upwards
            dash.multiply(1.0 + tier.getLevel() * 0.3); // Dampened momentum
        } else {
            dash.multiply(1.5 + tier.getLevel() * 0.5); // Fast horizontal dash
        }
        player.setVelocity(dash);

        java.util.Set<UUID> hitTargets = new java.util.HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 15) { // 15 ticks of damage frames during dash
                    this.cancel();
                    return;
                }

                Location currentLoc = player.getLocation();

                try {
                    player.getWorld().spawnParticle(Particle.valueOf("SONIC_BOOM"), currentLoc, 3, 0.5, 0.5, 0.5, 0);
                    player.getWorld().spawnParticle(Particle.valueOf("CLOUD"), currentLoc, 40, 0.5, 0.5, 0.5, 0.1);
                } catch (Exception ignored) {
                }

                PlayerData pData = plugin.getStarManager().getPlayerData(player);
                for (Entity e : player.getWorld().getNearbyEntities(currentLoc, 3.0, 3.0, 3.0)) {
                    if (e instanceof LivingEntity target && e != player && !hitTargets.contains(target.getUniqueId())) {
                        if (pData.isTrusted(target.getUniqueId())) continue;
                        target.damage(2.0 + tier.getLevel() * 2.0, player);
                        target.addPotionEffect(
                                new PotionEffect(PotionEffectType.WITHER, 100 * tier.getLevel(), tier.getLevel() - 1));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, tier.getLevel()));
                        target.setVelocity(new Vector(0, 1.0, 0)); // knockup

                        // Fake max HP drop by extra heavy damage scaling
                        if (tier.getLevel() >= 3) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                        }

                        hitTargets.add(target.getUniqueId());
                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void cleanup(UUID uuid) {
        hitCounters.remove(uuid);
    }
}
