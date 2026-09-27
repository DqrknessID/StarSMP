package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.*;

public class UmbryxStar implements Star {

    private final StarSMP plugin;
    private final Map<UUID, Integer> witherCounters = new HashMap<>();
    private final Map<UUID, Runnable> activeDomes = new HashMap<>();

    public UmbryxStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.UMBRYX;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        if (player.getTicksLived() % 40 == 0) {
            // Permanent Invisibility + Night Vision (BUFFED)
            // Increased duration to 300 ticks (15s) to prevent flickering
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false, false));
        }

        // ADJUST: Sprinting smoke particles (Visual giveaway for enemies)
        if (player.isSprinting() && !player.isInWater()) {
            if (player.getTicksLived() % 2 == 0) {
                player.getWorld().spawnParticle(Particle.valueOf("SMOKE"), player.getLocation().add(0, 0.1, 0), 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 2)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;
        if (!(event.getDamager() instanceof Player attacker))
            return;

        PlayerData data = plugin.getStarManager().getPlayerData(attacker);
        if (data.isTrusted(target.getUniqueId())) return;

        // Tier 2+: blind/wither chance
        double blindChance = plugin.getConfig().getDouble("stars.umbryx.passive-blind-chance", 0.05);
        
        if (Math.random() < blindChance) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.5f);
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        if (activeDomes.containsKey(player.getUniqueId())) {
            activeDomes.get(player.getUniqueId()).run();
            activeDomes.remove(player.getUniqueId());
            return;
        }

        player.playSound(player.getLocation(), "starsmp.ability.umbryx.dome", 2.0f, 0.8f);
        int durationTicks = plugin.getConfig().getInt("stars.umbryx.ability-duration", 600);
        
        Location center = player.getLocation().add(0, 1, 0); 
        double radius = tier.getLevel() == 3 ? 10.0 : 4.0 + (tier.getLevel() * 1.5); 

        // Repulsion & Particle logic
        Runnable domeTask = new Runnable() {
            @Override
            public void run() {
                // This will be handled by the loop below
            }
        };
        activeDomes.put(player.getUniqueId(), domeTask);

        new BukkitRunnable() {
            int ticks = 0;
            final double radiusSq = radius * radius;

            @Override
            public void run() {
                if (!activeDomes.containsKey(player.getUniqueId()) || ticks >= durationTicks) {
                    this.cancel();
                    activeDomes.remove(player.getUniqueId());
                    
                    // Apply cooldown now that the ability has ended
                    int seconds = plugin.getCooldownManager().getConfigCooldown(getType(), tier, 1);
                    plugin.getCooldownManager().setCooldown(player, 1, seconds);
                    
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                    return;
                }

                // --- 1. Visuals: Sphere Particles (BUFFED DENSITY) ---
                int particlesPerTick = (int) (radius * 15);
                for (int i = 0; i < particlesPerTick; i++) {
                    double phi = Math.acos(1 - 2 * Math.random());
                    double theta = 2 * Math.PI * Math.random();
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);
                    try {
                        Location pLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.valueOf("SQUID_INK"), pLoc, 1, 0.02, 0.02, 0.02, 0.01);
                        if (ticks % 3 == 0) {
                            center.getWorld().spawnParticle(Particle.valueOf("SMOKE"), pLoc, 1, 0, 0, 0, 0);
                        }
                        if (ticks % 10 == 0) {
                            center.getWorld().spawnParticle(Particle.valueOf("WITCH"), pLoc, 1, 0, 0, 0, 0);
                        }
                    } catch (Exception ignored) {}
                }

                // --- 2. Repulsion & Boundary Logic ---
                PlayerData pData = plugin.getStarManager().getPlayerData(player);
                for (Entity e : center.getWorld().getNearbyEntities(center, radius + 2, radius + 2, radius + 2)) {
                    if (e instanceof LivingEntity le) {
                        double distSq = le.getLocation().distanceSquared(center);
                        boolean isInside = distSq < radiusSq;
                        
                        // Boundary Repulsion (Keep people in AND out)
                        double shellInner = (radius - 0.7) * (radius - 0.7);
                        double shellOuter = (radius + 0.7) * (radius + 0.7);
                        
                        if (distSq > shellInner && distSq < shellOuter) {
                            org.bukkit.util.Vector vec = le.getLocation().toVector().subtract(center.toVector()).normalize();
                            if (le != player && !pData.isTrusted(le.getUniqueId())) {
                                // If they were inside, push them back in. If outside, push them out.
                                double strength = plugin.getConfig().getDouble("stars.umbryx.dome-repulsion-strength", 0.5);
                                boolean trapEnabled = plugin.getConfig().getBoolean("stars.umbryx.dome-trap-enabled", true);
                                
                                if (distSq > radiusSq) {
                                    le.setVelocity(vec.multiply(strength)); // Push Out
                                } else if (trapEnabled) {
                                    le.setVelocity(vec.multiply(-strength)); // Push In
                                }
                                
                                if (le instanceof Player p && ticks % 20 == 0) {
                                    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.5f);
                                    if (trapEnabled && distSq < radiusSq) {
                                        MessageUtil.send(p, "&5The Void boundary is impenetrable! You are trapped!");
                                    } else {
                                        MessageUtil.send(p, "&5The Void boundary is impenetrable!");
                                    }
                                }
                            }
                        }

                        // --- 3. Effects ---
                        if (isInside && e != player) {
                            if (pData.isTrusted(le.getUniqueId())) continue;
                            
                            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, false, false));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                            
                            // NERF: Reduced Wither frequency and capped at Level I
                            if (tier.getLevel() >= 2 && ticks % 40 == 0) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                            }
                        }
                    }
                }

                // --- 4. Player Buffs ---
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, false, false, true));
                if (tier.getLevel() >= 2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, true));
                }
                if (tier.getLevel() >= 3) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false, true));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void cleanup(UUID uuid) {
        witherCounters.remove(uuid);
        if (activeDomes.containsKey(uuid)) {
            activeDomes.get(uuid).run();
            activeDomes.remove(uuid);
        }
    }
}
