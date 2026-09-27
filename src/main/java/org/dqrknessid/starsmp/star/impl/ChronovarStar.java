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
import org.dqrknessid.starsmp.util.MessageUtil;

public class ChronovarStar implements Star {

    private final StarSMP plugin;
    private final java.util.Map<java.util.UUID, java.util.Set<Entity>> castingFrozen = new java.util.HashMap<>();

    public ChronovarStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.CHRONOVAR;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        if (player.getTicksLived() % 40 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0, true, false, false));

            // Chronovar Haste: Accelerating personal time (Mining Speed)
            int baseHaste = plugin.getConfig().getInt("stars.chronovar.passive-haste-level", 0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, baseHaste, true, false, false));

            if (tier.getLevel() >= 2) {
                int t2Haste = plugin.getConfig().getInt("stars.chronovar.tier2-haste-level", 1);
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, t2Haste, true, false, false));
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

        // Time Strike: Small chance to inflict heavy slowness (time slow)
        if (Math.random() < 0.10 + (tier.getLevel() * 0.05)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4));
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 2.0f);
            
            try {
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        // ADJUST: Ticking sound before activation (Telegraphing)
        new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                if (timer >= 3) { // 1.5 seconds (3 * 10 ticks)
                    activateTimeStop(player, tier);
                    this.cancel();
                    return;
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.5f, 0.5f + (timer * 0.5f));
                player.getWorld().spawnParticle(Particle.valueOf("WAX_OFF"), player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                timer++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void activateTimeStop(Player player, TierState tier) {
        player.playSound(player.getLocation(), "starsmp.ability.chronovar.timestop", 2.0f, 1.0f);
        try {
            player.getWorld().spawnParticle(Particle.valueOf("SONIC_BOOM"), player.getLocation(), 3);
            player.getWorld().spawnParticle(Particle.valueOf("END_ROD"), player.getLocation(), 400, 3, 0.5, 3, 0.5);
            player.getWorld().spawnParticle(Particle.valueOf("FIREWORK"), player.getLocation(), 300, 3, 0.5, 3, 0.5);
        } catch (Exception ignored) {
        }
        MessageUtil.send(player, "&dTime Stop activated!");

        int baseDuration = plugin.getConfig().getInt("stars.chronovar.active-time-stop-duration-ticks", 60);
        int duration = baseDuration + (tier.getLevel() * 20);
        
        int baseRadius = plugin.getConfig().getInt("stars.chronovar.active-time-stop-radius", 5);
        int radius = baseRadius + tier.getLevel() * 3;
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0));

        java.util.Set<Entity> frozen = new java.util.HashSet<>();
        castingFrozen.put(player.getUniqueId(), frozen);

        PlayerData data = plugin.getStarManager().getPlayerData(player);
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && e != player) {
                if (data.isTrusted(le.getUniqueId())) continue;
                frozen.add(le);
                le.setGravity(false);

                // Hard Root
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 127)); 
                le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128)); 
                le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 10)); 

                le.getWorld().spawnParticle(Particle.valueOf("SNOWFLAKE"), le.getLocation(), 20, 0.5, 1, 0.5, 0);
                try {
                    le.getWorld().spawnParticle(Particle.valueOf("ENCHANT"), le.getLocation(), 30, 0.5, 1, 0.5, 0.5);
                } catch (Exception ignored) {}

                if (tier.getLevel() >= 3) {
                    le.damage(8.0, player);
                }
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    for (Entity e : frozen) {
                        if (e.isValid()) e.setGravity(true);
                    }
                    castingFrozen.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                for (Entity e : frozen) {
                    if (e.isValid()) {
                        e.setVelocity(new Vector(0, 0, 0)); 
                        e.setFallDistance(0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void cleanup(java.util.UUID uuid) {
        java.util.Set<Entity> frozen = castingFrozen.remove(uuid);
        if (frozen != null) {
            for (Entity e : frozen) {
                if (e.isValid()) e.setGravity(true);
            }
        }
    }
}
