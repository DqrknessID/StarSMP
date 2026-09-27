package org.dqrknessid.starsmp.star.impl;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LuminaryStar implements Star {

    private final StarSMP plugin;
    private final Map<UUID, Integer> hitCharges = new HashMap<>();

    public LuminaryStar(StarSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StarType getType() {
        return StarType.LUMINARY;
    }

    @Override
    public void applyPassives(Player player, TierState tier) {
        if (tier.getLevel() < 0)
            return;

        // Night Vision > 200 ticks prevents vanilla screen pulse
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false, false));

        if (tier.getLevel() >= 2) {
            long time = player.getWorld().getTime();
            if (time < 12000) { // daytime
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0, true, false, false));
                
                // Periodic heal pulse (0.5 heart every 4 seconds)
                if (player.getTicksLived() % 80 == 0) {
                    double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    if (player.getHealth() < maxHealth) {
                        player.setHealth(Math.min(maxHealth, player.getHealth() + 1.0));
                        player.getWorld().spawnParticle(Particle.valueOf("VILLAGER_HAPPY"), player.getLocation().add(0, 1.2, 0), 5, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, TierState tier) {
        if (tier.getLevel() < 0)
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;

        Player player = player(event);
        if (player == null) return;
        PlayerData data = plugin.getStarManager().getPlayerData(player);
        if (data.isTrusted(target.getUniqueId())) return;
        
        // Passive Heal Chance
        double healChance = plugin.getConfig().getDouble("stars.luminary.passive-heal-chance", 0.15);
        if (Math.random() < healChance) {
            double healAmount = plugin.getConfig().getDouble("stars.luminary.passive-heal-amount", 2.0);
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
            player.getWorld().spawnParticle(Particle.valueOf("HEART"), player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        }

        UUID id = player.getUniqueId();
        int maxCharge = tier.getLevel() * 6; // BUFF: More max charges
        int current = hitCharges.getOrDefault(id, 0);

        if (current < maxCharge) {
            hitCharges.put(id, current + 1);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f,
                    1.0f + (current / (float) maxCharge));

            player.sendActionBar(MessageUtil.parse("&e✨ Radiant Charges: &f" + (current + 1) + " / " + maxCharge));

            // Visual indicator of charge gathering
            player.getWorld().spawnParticle(Particle.valueOf("END_ROD"), target.getLocation().add(0, 1, 0), 2, 0.2, 0.2,
                    0.2, 0.05);
        }
    }

    @Override
    public void triggerAbility(Player player, TierState tier) {
        if (tier.getLevel() < 1)
            return;

        UUID id = player.getUniqueId();
        int charge = hitCharges.getOrDefault(id, 0);

        if (charge == 0) {
            MessageUtil.send(player, "&cYou have no Radiant Charges! Build them by attacking enemies.");
            return;
        }

        hitCharges.put(id, 0); // Consume charges

        // BUFF: Purifying Light (Clear negative effects)
        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            if (type.equals(PotionEffectType.POISON) || type.equals(PotionEffectType.WITHER) || 
                type.equals(PotionEffectType.SLOWNESS) || type.equals(PotionEffectType.WEAKNESS) ||
                type.equals(PotionEffectType.BLINDNESS) || type.equals(PotionEffectType.DARKNESS) ||
                type.equals(PotionEffectType.MINING_FATIGUE) || type.equals(PotionEffectType.NAUSEA)) {
                player.removePotionEffect(type);
            }
        }

        // BUFF: Solar Shield on use (Capped at Resistance I)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, false, false, true));
        // BUFF: Burst of Light (Speed III)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, false, true));

        player.playSound(player.getLocation(), "starsmp.ability.luminary.blast", 2.0f, 1.0f);

        int radius = 5 + (charge / 2); // Increased base radius
        double damage = 4.0 + (charge * 1.2); // Nerfed Scaling: 1.2 from 2.0
        int glowingTicks = 300 + (charge * 60); // Increased glowing duration

        try {
            player.getWorld().spawnParticle(Particle.valueOf("EXPLOSION_EMITTER"), player.getLocation(), 1, 0, 0, 0, 0); // massive
                                                                                                                          // explosion
                                                                                                                          // particle
        } catch (Exception ignored) {
            player.getWorld().spawnParticle(Particle.valueOf("EXPLOSION"), player.getLocation(), 1, 0, 0, 0, 0);
        }

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 350 + charge * 40,
                radius / 1.5, 2, radius / 1.5, 0.3);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 350 + charge * 40,
                radius / 1.5, 2, radius / 1.5, 0.3);
        try {
            player.getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 3 + charge / 2, 1, 1, 1,
                    0);
        } catch (Exception ignored) {
        }

        PlayerData pData = plugin.getStarManager().getPlayerData(player);
        for (Entity e : player.getNearbyEntities(radius, 5, radius)) {
            if (e instanceof LivingEntity le && e != player) {
                if (pData.isTrusted(le.getUniqueId())) continue;
                le.damage(damage, player);
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingTicks, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60 + charge * 5, 0)); // Nerfed duration

                if (tier.getLevel() >= 2) {
                    le.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                    le.setFireTicks(100 + charge * 20);
                }
                if (tier.getLevel() >= 3) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100 + charge * 10, 1));
                }
            }
        }
    }

    private Player player(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p)
            return p;
        return null;
    }

    @Override
    public void cleanup(UUID uuid) {
        hitCharges.remove(uuid);
    }
}
