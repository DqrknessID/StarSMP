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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class VerdarynStar implements Star {

    private final StarSMP plugin;

    public VerdarynStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.VERDARYN;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        // Immortal feeling: Health Boost + Hunger Restoration
        if (player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.HEALTH_BOOST, 300, tier.getLevel() - 1, true, false, false));
        }
        
        // BUFF: Faster hunger restoration (Exactly every 30 ticks / 1.5s)
        if (player.getTicksLived() % 30 == 0 && player.getFoodLevel() < 20) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
        }

        if (tier.getLevel() >= 2 && player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0, true, false, false));
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;

        PlayerData data = plugin.getStarManager().getPlayerData((Player) event.getDamager());
        if (data.isTrusted(target.getUniqueId())) return;

        // Poison touch: small chance to poison
        if (Math.random() < plugin.getConfig().getDouble("stars.verdaryn.passive-poison-chance", 0.10)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); // Lengthened from 60
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), "starsmp.ability.verdaryn.cast", 2.0f, 1.0f);
        int baseRadius = plugin.getConfig().getInt("stars.verdaryn.active-dome-radius", 4);
        int radius = baseRadius + tier.getLevel() * 2;

        try {
            player.getWorld().spawnParticle(Particle.valueOf("VILLAGER_HAPPY"), player.getLocation(), 200, radius / 2.0, 1, radius / 2.0, 0.2);
        } catch (Exception ignored) {
            try { player.getWorld().spawnParticle(Particle.valueOf("HAPPY_VILLAGER"), player.getLocation(), 200, radius / 2.0, 1, radius / 2.0, 0.2); } catch (Exception ignored2) {}
        }

        double maxHealth = 20.0;
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            maxHealth = attr.getValue();
        }
        // Reverted healing multiplier
        player.setHealth(Math.min(maxHealth, player.getHealth() + (3.0 * tier.getLevel())));
        
        int duration = 100 + tier.getLevel() * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, tier.getLevel() - 1));
        
        // BUFF: Added Absorption hearts for dome duration (Increased duration)
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration * 2, 1)); // 4 Absorption hearts

        if (tier.getLevel() >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0));
        }

        // Setup persistent Verdaryn Dome
        Location center = player.getLocation();
        new BukkitRunnable() {
            int ticks = 0;
            java.util.Set<java.util.UUID> affectedEnemies = new java.util.HashSet<>();
            
            @Override
            public void run() {
                int maxTicks = plugin.getConfig().getInt("stars.verdaryn.active-dome-duration-ticks", 200);
                if (ticks >= maxTicks) { 
                    this.cancel();
                    // Restore health for everyone affected
                    for (java.util.UUID uuid : affectedEnemies) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            StarSMP.untrackHealthModification(p.getUniqueId());
                        }
                    }
                    return;
                }

                // Spherical particle dome (Denser: Step by PI/20 instead of PI/10)
                if (ticks % 10 == 0) {
                    for (double i = 0; i <= Math.PI; i += Math.PI / 20) {
                        double r = radius * Math.sin(i);
                        double y = radius * Math.cos(i) + 1.0;
                        for (double a = 0; a < Math.PI * 2; a += Math.PI / 20) {
                            double x = r * Math.cos(a);
                            double z = r * Math.sin(a);
                            try { center.getWorld().spawnParticle(Particle.valueOf("VILLAGER_HAPPY"), center.clone().add(x, y, z), 1, 0, 0, 0, 0); } catch (Exception ignored) {}
                            try { center.getWorld().spawnParticle(Particle.valueOf("HAPPY_VILLAGER"), center.clone().add(x, y, z), 1, 0, 0, 0, 0); } catch (Exception ignored) {}
                        }
                    }
                }

                PlayerData pData = plugin.getStarManager().getPlayerData(player);
                for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (e instanceof Player enemy && e != player) {
                        if (pData.isTrusted(enemy.getUniqueId())) continue;
                        
                        // Suppress max health
                        // BUFF: 30% reduction + Instant Harming on entry
                        double suppressionPercent = plugin.getConfig().getDouble("stars.verdaryn.active-health-suppression-percent", 0.30);
                        AttributeInstance it = enemy.getAttribute(Attribute.MAX_HEALTH);
                        if (it != null && it.getModifier(StarSMP.HEALTH_MODIFIER_KEY) == null) {
                            double currentBase = it.getBaseValue();
                            double reduction = currentBase * suppressionPercent;
                            
                            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                                StarSMP.HEALTH_MODIFIER_KEY,
                                -reduction,
                                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                            );
                            it.addModifier(modifier);
                            affectedEnemies.add(enemy.getUniqueId());
                            
                            // Apply Harming I once upon entry/suppression
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0));
                            
                            if (enemy.getHealth() > it.getValue()) enemy.setHealth(it.getValue());
                        }
                        
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0, false, false, false));
                        // BUFF: Increased Weakness and added Nausea (Pollen)
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false, false));
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false, false));
                        
                        if (ticks == 0) { // Burst damage/effects on the initial cast
                            double damageMult = plugin.getConfig().getDouble("stars.verdaryn.active-burst-damage-multiplier", 2.0);
                            enemy.damage(damageMult * tier.getLevel(), player);
                            if (tier.getLevel() >= 3) {
                                enemy.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                                enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            }
                        }
                    }
                }
                ticks += 5; // Run updates frequently
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
