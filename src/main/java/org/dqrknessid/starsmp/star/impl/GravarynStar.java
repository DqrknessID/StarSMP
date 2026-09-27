package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
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

public class GravarynStar implements Star {
    private static final NamespacedKey KB_KEY = NamespacedKey.fromString("starsmp:gravaryn_kb");
    private final StarSMP plugin;

    public GravarynStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.GRAVARYN;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        // Haste I
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 0, true, false, false));

        // Tier 2+: Resistance, Haste II, Strength, Health Boost, KB Resistance
        // Goliath Stance (Tier 3): Grants Resistance I, Knockback Resistance (70%), and Health Boost III (+6 Hearts).
        // Mountain's Bounty: Automatically restores 1 hunger point every 3 seconds (Tier 2+).
        if (tier.getLevel() >= 2) {
            if (player.getTicksLived() % 40 == 0) {
                int resAmp = 0; // Capped at Resistance I
                int healthAmp = (tier.getLevel() >= 3) ? 1 : 0; // Nerfed: Health Boost I or II (+4 Hearts)
                
                // Increased duration to 300 ticks (15s) to prevent heart flickering
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 300, healthAmp, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, resAmp, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 300, 1, true, false, false));
            }

            // Attribute Buff: Knockback Resistance completely removed (Nerf)
            org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null && kbAttr.getModifier(KB_KEY) != null) {
                kbAttr.removeModifier(KB_KEY);
            }
            
            // Counteract Hunger Drain from Health Boost: Restore 1 point every 3 seconds
            if (player.getTicksLived() % 60 == 0) {
                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                }
            }
        } else {
            // Remove KB modifier if dropped below T2
            org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null && kbAttr.getModifier(KB_KEY) != null) {
                kbAttr.removeModifier(KB_KEY);
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        // Not attack-proc focused
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        player.playSound(player.getLocation(), "starsmp.ability.gravaryn.smash", 2.0f, 1.0f);
        int radius = plugin.getConfig().getInt("stars.gravaryn.active-radius", 3) + tier.getLevel() * 2;
        double damage = 6.0 + (tier.getLevel() * 3.0); // Increased damage scaling

        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 300, radius / 2.0, 0.5,
                radius / 2.0, 0.5,
                Material.STONE.createBlockData());

        Location loc = player.getLocation();
        java.util.List<FallingBlock> fallingBlocks = new java.util.ArrayList<>();
        for (int i = 0; i < 20 + tier.getLevel() * 10; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.random() * radius;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            Block b = spawnLoc.getBlock().getRelative(0, -1, 0);
            Material mat = b.getType().isSolid() ? b.getType() : Material.DIRT;

            try {
                // Using modern spawn method instead of deprecated spawnFallingBlock
                FallingBlock fb = loc.getWorld().spawn(spawnLoc.add(0, 1, 0), FallingBlock.class, entity -> {
                    entity.setBlockData(mat.createBlockData());
                    entity.setDropItem(false);
                    entity.setVelocity(new Vector(0, 0.3 + Math.random() * 0.5, 0));
                });
                fallingBlocks.add(fb);
            } catch (Exception ignored) {
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (FallingBlock fb : fallingBlocks) {
                    if (fb != null && fb.isValid()) {
                        fb.remove();
                    }
                }
            }
        }.runTaskLater(plugin, 12L);

        PlayerData pData = plugin.getStarManager().getPlayerData(player);
        for (Entity e : player.getNearbyEntities(radius, 3, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                if (pData.isTrusted(target.getUniqueId())) continue;
                double finalDamage = damage;
                // Crushing Effect: 50% bonus damage if target is in the air
                if (target.getVelocity().getY() > 0 || !target.isOnGround()) {
                    finalDamage *= 1.5;
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.2);
                }
                
                target.damage(finalDamage);
                target.setVelocity(new Vector(0, 0.6 + tier.getLevel() * 0.25, 0)); // knockup (Buffed)
                target.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, 60 + tier.getLevel() * 20, 2)); // Slowness III (Buffed)

                // Tier 3 Spikes
                if (tier.getLevel() >= 3) {
                    Block foot = target.getLocation().getBlock();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (Math.abs(x) == Math.abs(z))
                                continue;
                            Block b = foot.getRelative(x, 0, z);
                            if (b.getType() == Material.AIR) {
                                try {
                                    b.setType(Material.POINTED_DRIPSTONE);
                                } catch (Exception ignored) {
                                }
                                plugin.getTemporaryBlockListener().addBlock(b);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (b.getType() == Material.POINTED_DRIPSTONE)
                                            b.setType(Material.AIR);
                                        plugin.getTemporaryBlockListener().removeBlock(b);
                                    }
                                }.runTaskLater(plugin, 60L);
                            }
                        }
                    }
                }
            }
        }
    }
}
