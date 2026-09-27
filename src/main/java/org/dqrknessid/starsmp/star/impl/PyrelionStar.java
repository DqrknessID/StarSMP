package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PyrelionStar implements Star {

    private final StarSMP plugin;
    private final Random random = new Random();

    public PyrelionStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.PYRELION;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        // Fire Resistance (Apply every 2 seconds via ticksLived check)
        if (player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 0, true, false, false));
        }

        // BUFF: Lava Swimming (Increased travel speed in lava) - Runs every tick now!
        if (player.getLocation().getBlock().getType() == Material.LAVA || 
            player.getEyeLocation().getBlock().getType() == Material.LAVA) {
            
            org.bukkit.util.Vector direction = player.getLocation().getDirection();
            org.bukkit.util.Vector velocity = player.getVelocity();
            
            // Apply a consistent forward push while in lava
            // Using a smaller multiplier because it runs every tick
            double speed = 0.08; 
            
            // Only boost if moving or looking around to feel natural
            if (velocity.length() < 0.5) {
                player.setVelocity(velocity.add(direction.multiply(speed)).setY(velocity.getY() * 0.95));
            }
            
            if (player.getTicksLived() % 5 == 0) {
                player.getWorld().spawnParticle(Particle.valueOf("FLAME"), player.getLocation(), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;

        Player attacker = (Player) event.getDamager();
        PlayerData data = plugin.getStarManager().getPlayerData(attacker);
        if (data.isTrusted(target.getUniqueId())) return;

        // Cinder Fury: +5% to burning enemies (Nerfed from 10%)
        if (target.getFireTicks() > 0) {
            event.setDamage(event.getDamage() * 1.05);
        }

        // Heat Burn: 10% chance
        double burnChance = plugin.getConfig().getDouble("stars.pyrelion.passive-burn-chance", 0.10);
        double burnDmg = 1.5; // Nerfed from 2.0

        if (random.nextDouble() < burnChance) {
            target.damage(burnDmg);
            target.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            target.setFireTicks(80 + tier.getLevel() * 30); // Nerfed duration
            attacker.playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.5f);
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), "starsmp.ability.pyrelion.fire", 2.0f, 1.0f);

        new BukkitRunnable() {
            final int baseRadius = plugin.getConfig().getInt("stars.pyrelion.active-fire-radius", 3);
            final int maxRadius = baseRadius + tier.getLevel() * 2;
            final int maxPhases = 3; 
            final double baseDamage = 2.5; // Nerfed from 3.5
            final double damage = baseDamage * tier.getLevel();
            int currentPhase = 0;
            int currentRadius = 0;
            final Set<Block> previousBlocks = new HashSet<>();
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                // Cleanup previous ring
                for (Block b : previousBlocks) {
                    if (b.getType() == Material.FIRE) {
                        b.setType(Material.AIR);
                    }
                    plugin.getTemporaryBlockListener().removeBlock(b);
                }
                previousBlocks.clear();

                if (currentPhase >= maxPhases) {
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Spawn fire ring
                double thickness = (tier.getLevel() >= 2) ? 1.0 : 0.5;
                for (int x = -currentRadius; x <= currentRadius; x++) {
                    for (int z = -currentRadius; z <= currentRadius; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        if (dist >= currentRadius - thickness && dist <= currentRadius + thickness) {
                            
                            int bestY = 0;
                            for (int dy = 3; dy >= -3; dy--) {
                                Block floorCandidate = loc.getBlock().getRelative(x, dy, z);
                                if (!floorCandidate.isPassable() && floorCandidate.getRelative(0, 1, 0).isPassable()) {
                                    bestY = dy + 1;
                                    break;
                                }
                            }
                            
                            Block b = loc.getBlock().getRelative(x, bestY, z);
                            if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS
                                    || b.getType() == Material.SHORT_GRASS) {
                                // Don't spawn fire directly inside the player exactly
                                if (currentRadius > 0
                                        || b.getLocation().distanceSquared(loc.getBlock().getLocation()) > 0.1) {
                                    b.setType(Material.FIRE);
                                    previousBlocks.add(b);
                                    plugin.getTemporaryBlockListener().addBlock(b);
                                }
                            }
                        }
                    }
                }

                // Affect entities on the ring
                PlayerData data = plugin.getStarManager().getPlayerData(player);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, currentRadius + 1, 3, currentRadius + 1)) {
                    if (e instanceof LivingEntity le && e != player) {
                        if (data.isTrusted(le.getUniqueId())) continue;
                        
                        double dist = le.getLocation().distance(loc);
                        if (dist >= currentRadius - 1.5 && dist <= currentRadius + 1.5) {
                            if (!hitEntities.contains(le.getUniqueId())) {
                                le.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                                int burnTicks = plugin.getConfig().getInt("stars.pyrelion.active-burn-duration-ticks", 100);
                                le.setFireTicks(burnTicks + tier.getLevel() * 20);
                                le.damage(damage);
                                le.setVelocity(le.getLocation().toVector().subtract(loc.toVector()).normalize()
                                        .multiply(0.5 + tier.getLevel() * 0.2).setY(0.3));
                                if (tier.getLevel() >= 3) {
                                    AttributeInstance attr = le.getAttribute(Attribute.MAX_HEALTH);
                                    if (attr != null && attr.getModifier(StarSMP.PYRELION_HEALTH_KEY) == null) {
                                        double currentBaseValue = attr.getBaseValue();
                                        if (currentBaseValue > 18.0) {
                                            double diff = 18.0 - currentBaseValue;
                                            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                                                StarSMP.PYRELION_HEALTH_KEY,
                                                diff,
                                                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                                            );
                                            attr.addModifier(modifier);
                                            le.setHealth(Math.min(le.getHealth(), 18.0));

                                            // Persistent Expiry logic
                                            long expiryTime = System.currentTimeMillis() + 30000; // 30 seconds
                                            if (le instanceof Player p) {
                                                p.getPersistentDataContainer().set(StarSMP.PYRELION_EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG, expiryTime);
                                            }
                                            
                                            // Schedule restore in 30 seconds
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    StarSMP.untrackHealthModification(le.getUniqueId(), StarSMP.PYRELION_HEALTH_KEY);
                                                    if (le instanceof Player p) {
                                                        p.getPersistentDataContainer().remove(StarSMP.PYRELION_EXPIRY_KEY);
                                                    }
                                                }
                                            }.runTaskLater(plugin, 600L);
                                        }
                                    }
                                }
                                hitEntities.add(le.getUniqueId());
                            }
                        }
                    }
                }

                loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
                loc.getWorld().spawnParticle(Particle.valueOf("FLAME"), loc, 120 + tier.getLevel() * 40, currentRadius,
                        0.5, currentRadius,
                        0.05);

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
}
