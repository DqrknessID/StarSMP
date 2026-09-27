package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoltyraStar implements Star {

    private final StarSMP plugin;
    private final Map<UUID, Integer> chargeMap = new HashMap<>();
    private final Map<UUID, Integer> hitCounters = new HashMap<>();
    private final Map<UUID, Long> lastChargeTime = new HashMap<>();

    public VoltyraStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.VOLTYRA;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        // ADJUST: Charge Decay logic
        UUID id = player.getUniqueId();
        if (chargeMap.getOrDefault(id, 0) > 0) {
            long now = System.currentTimeMillis();
            long lastAttack = lastChargeTime.getOrDefault(id, 0L);
            
            // If idle for 10 seconds, start decaying
            if (now - lastAttack > 10000) {
                // Decay every 2 seconds
                if (player.getTicksLived() % 40 == 0) {
                    int current = chargeMap.get(id);
                    int next = Math.max(0, current - 5);
                    chargeMap.put(id, next);
                    
                    if (next > 0) {
                        player.sendActionBar(MessageUtil.parse("&e⚡ Charge Decaying: &f" + next));
                    } else {
                        player.sendActionBar(MessageUtil.parse("&7⚡ Charge Lost (Idle)"));
                    }
                }
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0 || event.getFinalDamage() <= 0)
            return;

        Player attacker = (Player) event.getDamager();
        UUID id = attacker.getUniqueId();

        // 200ms internal cooldown for charge to prevent CPS abuse
        long now = System.currentTimeMillis();
        if (now - lastChargeTime.getOrDefault(id, 0L) < 200) {
            return;
        }
        lastChargeTime.put(id, now);

        PlayerData data = plugin.getStarManager().getPlayerData(attacker);
        Entity target = event.getEntity();
        if (data.isTrusted(target.getUniqueId()))
            return;

        int chargePerHit = plugin.getConfig().getInt("stars.voltyra.passive-charge-per-hit", 10);
        int maxCharge = plugin.getConfig().getInt("stars.voltyra.passive-max-charge", 100);

        // Add charge
        int charge = chargeMap.getOrDefault(id, 0) + chargePerHit;

        if (charge >= maxCharge) {
            // Proc: stun + bonus damage
            if (target instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10)); // stun 2s
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
            }
            double bonus = plugin.getConfig().getDouble("stars.voltyra.proc-bonus-damage", 2.0);
            event.setDamage(event.getDamage() + bonus); 
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f);
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0)); // Glow effect
            try {
                attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 30, 0.5, 1,
                        0.5, 0.1);
                attacker.getWorld().spawnParticle(Particle.GLOW, attacker.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3,
                        0.1);
            } catch (Exception ignored) {
                attacker.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.5, 1, 0.5,
                        0.1);
            }
            charge = 0;
        } else {
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f,
                    0.5f + (charge / (float) maxCharge) * 1.5f);
            attacker.sendActionBar(MessageUtil.parse("&e⚡ Charge: &f" + charge + " / " + maxCharge));
        }
        chargeMap.put(id, charge);

        // Tier 2+ passive: every 5th hit → mini lightning
        if (tier.getLevel() >= 2) {
            int procEvery = plugin.getConfig().getInt("stars.voltyra.tier2-proc-every-hits", 5);
            int hits = hitCounters.getOrDefault(id, 0) + 1;
            if (hits >= procEvery) {
                target.getWorld().strikeLightningEffect(target.getLocation());
                double lightningDamage = plugin.getConfig().getDouble("stars.voltyra.tier2-lightning-damage", 2.0);
                if (target instanceof LivingEntity le) le.damage(lightningDamage);
                hits = 0;
            }
            hitCounters.put(id, hits);
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        Block b = player.getTargetBlockExact(30);
        if (b != null) {
            Location loc = b.getLocation().add(0.5, 0, 0.5);

            // Scaled strikes
            int strikes = tier.getLevel() * 2;
            new BukkitRunnable() {
                int count = 0;
                Entity lastTarget = null;

                @Override
                public void run() {
                    if (count >= strikes) {
                        this.cancel();
                        return;
                    }

                    Location strikeLoc = loc;
                    org.bukkit.Particle.DustOptions colordust;
                    if (tier.getLevel() >= 3) {
                        colordust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 1.5f); // Purple
                    } else if (tier.getLevel() == 2) {
                        colordust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 255), 1.5f); // Cyan
                    } else {
                        colordust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 0), 1.5f); // Yellow
                    }

                    // Try to chain to nearby entities instead of random radius if tier >= 2
                    if (tier.getLevel() >= 2) {
                        Entity bestTarget = null;
                        double closestDist = Double.MAX_VALUE;
                        PlayerData pData = plugin.getStarManager().getPlayerData(player);
                        Location searchCenter = lastTarget != null ? lastTarget.getLocation() : loc;
                        for (Entity e : player.getWorld().getNearbyEntities(searchCenter, 8, 5, 8)) {
                            if (e instanceof LivingEntity le && e != player && e != lastTarget) {
                                if (pData.isTrusted(le.getUniqueId()))
                                    continue;
                                double d = le.getLocation().distanceSquared(searchCenter);
                                if (d < closestDist) {
                                    closestDist = d;
                                    bestTarget = e;
                                }
                            }
                        }
                        if (bestTarget != null) {
                            strikeLoc = bestTarget.getLocation();
                            lastTarget = bestTarget;
                            try {
                                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, searchCenter, 20,
                                        0.5, 0.5, 0.5, 0.1);
                                player.playSound(searchCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f); // Crackle
                                                                                                                 // sound
                                // Simple trail
                                org.bukkit.util.Vector dir = strikeLoc.toVector().subtract(searchCenter.toVector());
                                double dist = dir.length();
                                dir.normalize();
                                for (double i = 0; i <= dist; i += 0.5) {
                                    player.getWorld().spawnParticle(Particle.DUST,
                                            searchCenter.clone().add(dir.clone().multiply(i)), 2, colordust);
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            // Fallback to random around loc
                            double angle = Math.random() * Math.PI * 2;
                            double radiusOffset = (count == 0) ? 0 : Math.random() * 5;
                            strikeLoc = loc.clone().add(Math.cos(angle) * radiusOffset, 0,
                                    Math.sin(angle) * radiusOffset);
                        }
                    } else {
                        double angle = Math.random() * Math.PI * 2;
                        double radiusOffset = (count == 0) ? 0 : Math.random() * 4;
                        strikeLoc = loc.clone().add(Math.cos(angle) * radiusOffset, 0, Math.sin(angle) * radiusOffset);
                    }

                    try {
                        player.getWorld().spawnParticle(Particle.DUST, strikeLoc, 30, 0.5, 2, 0.5, colordust);
                    } catch (Exception ignored1) {
                    }

                    player.getWorld().strikeLightningEffect(strikeLoc);
                    PlayerData pData = plugin.getStarManager().getPlayerData(player);
                    for (Entity e : player.getWorld().getNearbyEntities(strikeLoc, 4, 4, 4)) {
                        if (e instanceof LivingEntity le && e != player) {
                            if (pData.isTrusted(le.getUniqueId()))
                                continue;
                            le.damage(2.0 + tier.getLevel(), player); // Nerfed lightning damage
                            // Paralyze: Slowness high level (BUFFED/FIXED: Removed Jump Boost which caused sky-launch bug)
                            if (tier.getLevel() >= 2) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9, false, false, true));
                            }
                            if (tier.getLevel() >= 3) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                            }
                        }
                    }
                    count++;
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }
    }

    @Override
    public void cleanup(UUID uuid) {
        chargeMap.remove(uuid);
        hitCounters.remove(uuid);
        lastChargeTime.remove(uuid);
    }
}
