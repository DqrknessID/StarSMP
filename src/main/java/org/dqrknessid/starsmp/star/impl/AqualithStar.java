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

import java.util.*;

public class AqualithStar implements Star {

    private final StarSMP plugin;
    private final Map<UUID, Integer> undertowHits = new HashMap<>();

    public AqualithStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.AQUALITH;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        if (player.getTicksLived() % 40 == 0) {
            // Dolphin's Grace + Water Breathing
            int speedLevel = plugin.getConfig().getInt("stars.aqualith.passive-underwater-speed", 2);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 300, speedLevel, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300, 0, true, false, false));
        }
        
        // NERFED: Regen in water/rain replaced with periodic heal pulse (0.5 heart every 4 seconds)
        if (player.isInWater() || player.getWorld().hasStorm()) {
            if (player.getTicksLived() % 80 == 0) {
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                if (player.getHealth() < maxHealth) {
                    player.setHealth(Math.min(maxHealth, player.getHealth() + 1.0));
                    player.getWorld().spawnParticle(Particle.valueOf("WATER_DROP"), player.getLocation().add(0, 1.2, 0), 5, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }

        // Tier 2+ passive: Rain/Water = Resistance I + Conduit Power
        if (tier.getLevel() >= 2 && player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 300, 0, true, false, false));
            if (player.isInWater() || player.getWorld().hasStorm()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0, true, false, false));
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;

        // Tidal Instinct: +12% damage to wet enemies (NERFED from 20%)
        if (target.isInWater() || target.getWorld().hasStorm()) {
            event.setDamage(event.getDamage() * 1.12);
        }
        
        Player attacker = player(event);
        PlayerData data = plugin.getStarManager().getPlayerData(attacker);
        if (data.isTrusted(target.getUniqueId())) return;
        
        UUID targetId = target.getUniqueId();
        int hits = undertowHits.getOrDefault(targetId, 0) + 1;
        if (hits >= 3) {
            // BUFF: Slowness II and Weakness
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
            player(event).playSound(player(event).getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.2f);
            try {
                player(event).getWorld().spawnParticle(Particle.valueOf("WATER_SPLASH"), target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                player(event).playSound(player(event).getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.5f, 1.5f);
            } catch (Exception ignored) {}
            hits = 0;
        }
        undertowHits.put(targetId, hits);

        new BukkitRunnable() {
            @Override
            public void run() {
                undertowHits.remove(targetId);
            }
        }.runTaskLater(plugin, 60L);
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        int maxRadius = 3 + (int)(tier.getLevel() * 1.5); // NERFED radius scaling
        int maxPhasesValue = 3; // Expand, Contract, Expand

        player.playSound(player.getLocation(), "starsmp.ability.aqualith.splash", 2.0f, 1.0f);

        new BukkitRunnable() {
            int currentPhase = 0;
            int currentRadius = 0;
            final Set<Block> previousBlocks = new HashSet<>();
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                // Cleanup previous ring
                for (Block b : previousBlocks) {
                    if (b.getType() == Material.WATER) {
                        b.setType(Material.AIR);
                    }
                    plugin.getTemporaryBlockListener().removeBlock(b);
                }
                previousBlocks.clear();

                if (currentPhase >= maxPhasesValue) {
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Spawn water ring or dome
                if (tier.getLevel() >= 3) {
                    for (int x = -currentRadius; x <= currentRadius; x++) {
                        for (int y = 0; y <= currentRadius; y++) {
                            for (int z = -currentRadius; z <= currentRadius; z++) {
                                double dist = Math.sqrt(x * x + y * y + z * z);
                                if (dist >= currentRadius - 0.5 && dist <= currentRadius + 0.5) {
                                    Block b = loc.getBlock().getRelative(x, y, z);
                                    if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS
                                            || b.getType() == Material.SHORT_GRASS) {
                                        b.setType(Material.WATER);
                                        previousBlocks.add(b);
                                        plugin.getTemporaryBlockListener().addBlock(b);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (int x = -currentRadius; x <= currentRadius; x++) {
                        for (int z = -currentRadius; z <= currentRadius; z++) {
                            double dist = Math.sqrt(x * x + z * z);
                            if (dist >= currentRadius - 0.5 && dist <= currentRadius + 0.5) {
                                Block b = loc.getBlock().getRelative(x, 0, z);
                                if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS
                                        || b.getType() == Material.SHORT_GRASS) {
                                    b.setType(Material.WATER);
                                    previousBlocks.add(b);
                                    plugin.getTemporaryBlockListener().addBlock(b);
                                }
                            }
                        }
                    }
                }

                // Affect entities on the ring
                int hitBoxHeight = (tier.getLevel() >= 3) ? currentRadius + 1 : 3;
                PlayerData data = plugin.getStarManager().getPlayerData(player);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, currentRadius + 1, hitBoxHeight,
                        currentRadius + 1)) {
                    if (e instanceof LivingEntity le && e != player) {
                        if (data.isTrusted(le.getUniqueId())) continue;
                        
                        double dist = le.getLocation().distance(loc);
                        if (dist >= currentRadius - 1.5 && dist <= currentRadius + 1.5) {
                            if (!hitEntities.contains(le.getUniqueId())) {
                                le.addPotionEffect(
                                        new PotionEffect(PotionEffectType.SLOWNESS, 100 + tier.getLevel() * 30,
                                                tier.getLevel() - 1));
                                le.addPotionEffect(
                                        new PotionEffect(PotionEffectType.BLINDNESS, 30 + tier.getLevel() * 10, 0));
                                if (tier.getLevel() >= 2) {
                                    le.addPotionEffect(
                                        new PotionEffect(PotionEffectType.WEAKNESS, 80 + tier.getLevel() * 20, 0));
                                }
                                // NERFED: Reduced active damage from 3.5x to 2.8x
                                le.damage(tier.getLevel() * 2.8);
                                hitEntities.add(le.getUniqueId());
                            }
                        }
                    }
                }

                loc.getWorld().playSound(loc, Sound.WEATHER_RAIN, 0.5f, 1.0f);
                loc.getWorld().spawnParticle(Particle.valueOf("DOLPHIN"), loc, 80 + tier.getLevel() * 30, currentRadius,
                        1.5, currentRadius,
                        0.2);
                try {
                    loc.getWorld().spawnParticle(Particle.valueOf("WATER_DROP"), loc, 150, currentRadius, 1.5,
                            currentRadius, 0.2);
                } catch (Exception ignored) {
                    loc.getWorld().spawnParticle(Particle.valueOf("WATER_SPLASH"), loc, 150, currentRadius, 1.5,
                            currentRadius, 0.2);
                }

                // Update radius
                if (currentPhase == 0 || currentPhase == 2) {
                    currentRadius++;
                    if (currentRadius > maxRadius) {
                        currentPhase++;
                        currentRadius = maxRadius - 1;
                        hitEntities.clear();
                    }
                } else {
                    currentRadius--;
                    if (currentRadius < 0) {
                        currentPhase++;
                        currentRadius = 1;
                        hitEntities.clear();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private Player player(EntityDamageByEntityEvent e) {
        return (Player) e.getDamager();
    }

    @Override
    public void cleanup(UUID uuid) {
        undertowHits.remove(uuid);
    }
}
