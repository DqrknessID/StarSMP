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

public class AetherionStar implements Star {

    private final StarSMP plugin;

    public AetherionStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.AETHERION;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0, true, false, false));
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;
        Player attacker = (Player) event.getDamager();

        // Massive Crit Buff
        if (event.isCritical()) {
            double multiplier = plugin.getConfig().getDouble("stars.aetherion.passive-crit-multiplier", 1.5);
            event.setDamage(event.getDamage() * multiplier);
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
            attacker.getWorld().spawnParticle(Particle.valueOf("CRIT"), target.getLocation(), 20, 0.3, 0.3, 0.3, 0.2);
            attacker.getWorld().spawnParticle(Particle.valueOf("ENCHANTED_HIT"), target.getLocation(), 15, 0.3, 0.3, 0.3, 0.2);
        }

        // Void Pull Passive (Capped Y-boost to prevent "infinite levitation" feel)
        double pullStrength = plugin.getConfig().getDouble("stars.aetherion.passive-pull-strength", 0.3);
        Vector currentVel = target.getVelocity();
        Vector pull = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).normalize()
                .multiply(pullStrength + (tier.getLevel() * 0.1));
        
        // Only apply Y boost if they aren't already flying up too fast
        if (currentVel.getY() < 0.4) {
            pull.setY(0.15); 
        } else {
            pull.setY(0);
        }
        
        target.setVelocity(currentVel.add(pull));
        
        if (Math.random() < 0.2) {
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 2.0f);
            try { attacker.getWorld().spawnParticle(Particle.valueOf("PORTAL"), target.getLocation(), 15, 0.5, 0.5, 0.5, 0.1); } catch (Exception ignored) {}
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), "starsmp.ability.aetherion.blackhole", 2.0f, 1.0f);
        try {
            player.getWorld().spawnParticle(Particle.valueOf("DRAGON_BREATH"), player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        } catch (Exception ignored) {}

        Location center = player.getTargetBlock(null, 15).getLocation().add(0.5, 1.2, 0.5);
        
        int baseDuration = plugin.getConfig().getInt("stars.aetherion.active-blackhole-duration-ticks", 100);
        int duration = baseDuration + (tier.getLevel() * 40); 
        double radius = plugin.getConfig().getDouble("stars.aetherion.active-blackhole-radius", 5.5);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                    try {
                        center.getWorld().spawnParticle(Particle.valueOf("SONIC_BOOM"), center, 1);
                    } catch (Exception ignored) {
                        try { center.getWorld().spawnParticle(Particle.valueOf("EXPLOSION_HUGE"), center, 1); } catch (Exception ignored2) {}
                    }
                    if (tier.getLevel() >= 3) {
                        PlayerData data = plugin.getStarManager().getPlayerData(player);
                        double finalDamage = plugin.getConfig().getDouble("stars.aetherion.active-final-damage", 12.0);
                        for (Entity e : center.getWorld().getNearbyEntities(center, 4, 4, 4)) {
                            if (e instanceof LivingEntity le && e != player) {
                                if (data.isTrusted(le.getUniqueId())) continue;
                                le.damage(finalDamage, player);
                            }
                        }
                    }
                    return;
                }

                try {
                    center.getWorld().spawnParticle(Particle.valueOf("SQUID_INK"), center, 10, 0.05, 0.05, 0.05, 0.02);
                    if (ticks % 3 == 0) {
                        center.getWorld().spawnParticle(Particle.valueOf("REVERSE_PORTAL"), center, 15, radius, radius, radius, 0.2);
                    }
                } catch (Exception ignored) {}

                if (ticks % 10 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                }

                PlayerData data = plugin.getStarManager().getPlayerData(player);
                for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (e instanceof LivingEntity le && e != player) {
                        if (data.isTrusted(le.getUniqueId())) continue;
                        
                        Vector vectorToCenter = center.toVector().subtract(le.getLocation().toVector());
                        double distance = vectorToCenter.length();
                        
                        if (distance > 0.5) {
                            Vector pullDir = vectorToCenter.normalize();
                            double strength = 0.12 + (tier.getLevel() * 0.03); // Slightly reduced strength
                            
                            if (distance < 1.5) {
                                // Smoother center hover: don't hard-lock velocity, just dampen and lift slightly
                                Vector currentVel = le.getVelocity();
                                le.setVelocity(new Vector(currentVel.getX() * 0.8, 0.05, currentVel.getZ() * 0.8));
                                le.setNoDamageTicks(0);
                            } else {
                                // Additive pull with a cap to prevent insane acceleration
                                Vector pull = pullDir.multiply(strength);
                                Vector newVel = le.getVelocity().add(pull);
                                
                                // Cap the velocity to prevent "shooting off" at light speed
                                if (newVel.length() > 1.5) {
                                    newVel = newVel.normalize().multiply(1.5);
                                }
                                le.setVelocity(newVel);
                            }
                        }

                        if (ticks % 5 == 0) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, tier.getLevel()));
                        }

                        // Tick Damage
                        if (distance < 2.5) {
                            if (ticks % 20 == 0) {
                                double tickDamage = plugin.getConfig().getDouble("stars.aetherion.active-blackhole-damage-per-sec", 1.5);
                                le.damage(tickDamage * tier.getLevel(), player);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
