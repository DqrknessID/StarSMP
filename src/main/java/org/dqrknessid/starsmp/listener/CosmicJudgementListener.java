package org.dqrknessid.starsmp.listener;

import org.dqrknessid.starsmp.data.PlayerData;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.Vector;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.util.MessageUtil;


import java.util.*;

public class CosmicJudgementListener implements Listener {

    private final StarSMP plugin;
    private final ItemManager itemManager;

    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Set<UUID> launchedPlayers = new HashSet<>();

    // Scoreboard teams for colored glowing


    private int glowTaskId = -1;

    public CosmicJudgementListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        startGlowTask();
        startFallingParticleTask();
    }

    // ═══════════════════════════════════════════════════════
    // Purple Glowing Task (runs every 10 ticks)
    // ═══════════════════════════════════════════════════════

    private void startGlowTask() {
        glowTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean holdingMace = itemManager.isCustomItem(player.getInventory().getItemInMainHand(), "COSMIC_JUDGEMENT");
                    if (holdingMace && !frozenPlayers.contains(player.getUniqueId())) {
                        if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 110, 0, false, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
    }

    // ═══════════════════════════════════════════════════════
    // Intense Falling Particle Trail
    // ═══════════════════════════════════════════════════════

    private int fallingParticleTaskId = -1;

    private void startFallingParticleTask() {
        fallingParticleTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (!itemManager.isCustomItem(mainHand, "COSMIC_JUDGEMENT")) continue;
                    float fall = player.getFallDistance();
                    if (fall < 1.5f) continue;

                    Location loc = player.getLocation();
                    // Scale intensity with fall distance (capped for perf)
                    int intensity = (int) Math.min(fall * 2.5, 14);

                    try { loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0),
                            intensity, 0.15, 0.05, 0.15, 0.04); } catch (Exception ignored) {}
                    try { loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.2, 0),
                            intensity, 0.2, 0.05, 0.2, 0.05); } catch (Exception ignored) {}
                    if (fall > 5f) {
                        // Extra debris at high velocity
                        try { loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.8, 0),
                                intensity / 2, 0.3, 0.1, 0.3, 0.08); } catch (Exception ignored) {}
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L).getTaskId();
    }

    // ═══════════════════════════════════════════════════════
    // Shift + Right Click → Freeze Circle + Launch
    // ═══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Shift + Right Click for ability
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;
        if (!itemManager.isCustomItem(item, "COSMIC_JUDGEMENT")) return;

        event.setCancelled(true);

        // Check ability cooldown (90 seconds default)
        if (plugin.getCooldownManager().isOnCooldown(player, 3)) return;

        // ── Activate Ability ──
        int abilityCd = plugin.getConfig().getInt("items.cosmic-judgement.ability-cooldown", 90);
        plugin.getCooldownManager().setCooldown(player, 3, abilityCd);

        Location center = player.getLocation();

        // ── Phase 1: Particle Circle (flat) ──
        spawnFreezeCircle(center);

        // ── Sound Effects ──
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.5f);
        player.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.6f);
        player.getWorld().playSound(center, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.8f);
        player.getWorld().playSound(center, "item.cosmic_judgement.meteor_strike", 2.0f, 1.0f);

        // ── Phase 2: Freeze nearby enemies ──
        double radius = plugin.getConfig().getDouble("items.cosmic-judgement.freeze-radius", 7.0);
        List<Player> targets = new ArrayList<>();
        PlayerData attackerData = plugin.getStarManager().getPlayerData(player);
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player target && target != player) {
                if (attackerData.isTrusted(target.getUniqueId())) continue;
                
                double distSq = target.getLocation().toVector().setY(0)
                        .distanceSquared(center.toVector().setY(0));
                if (distSq <= radius * radius) {
                    targets.add(target);
                }
            }
        }

        for (Player target : targets) {
            freezePlayer(target);
        }

        // ── Phase 3: Launch the mace holder 10 blocks up ──
        launchPlayer(player);
    }

    // ═══════════════════════════════════════════════════════
    // Particle Circle (flat)
    // ═══════════════════════════════════════════════════════

    private void spawnFreezeCircle(Location center) {
        // Outer ring — 72 points (every 5°)
        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 5);
            double x = 7.0 * Math.cos(angle);
            double z = 7.0 * Math.sin(angle);
            Location point = center.clone().add(x, 0.2, z);
            try {
                center.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
            } catch (Exception ignored) {}
        }

        // Inner expanding rings for visual intensity
        for (double r = 1.0; r <= 7.0; r += 2.0) {
            int points = (int) (r * 10);
            double angleStep = 360.0 / points;
            for (int i = 0; i < points; i++) {
                double angle = Math.toRadians(i * angleStep);
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                Location point = center.clone().add(x, 0.1, z);
                try {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0, 0, 0, 0);
                } catch (Exception ignored) {}
            }
        }

        // Floor dust burst
        try {
            center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 0.3, 0),
                    20, 3.5, 0.1, 3.5, 0.01);
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════
    // Freeze Player
    // ═══════════════════════════════════════════════════════

    private void freezePlayer(Player target) {
        frozenPlayers.add(target.getUniqueId());
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "cosmic_frozen"), org.bukkit.persistence.PersistentDataType.BOOLEAN, true);

        int durationSec = plugin.getConfig().getInt("items.cosmic-judgement.freeze-duration", 5);
        int durationTicks = durationSec * 20;

        // Apply visual effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 255, false, false, false));
        try {
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 128, false, false, false));
        } catch (Exception ignored) {
            // Some versions may not support extreme levels
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false, false));

        // Freeze particles on target
        Location tLoc = target.getLocation();
        try {
            target.getWorld().spawnParticle(Particle.END_ROD, tLoc.clone().add(0, 1, 0), 25, 0.4, 0.8, 0.4, 0.05);
        } catch (Exception ignored) {}
        try {
            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, tLoc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
        } catch (Exception ignored) {}

        // Freeze sound
        target.playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f);

        // Send message to frozen target
        MessageUtil.send(target, "&b✦ &1You have been frozen by Cosmic Judgement!");

        // Schedule unfreeze after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                unfreezePlayer(target);
            }
        }.runTaskLater(plugin, (long) durationTicks);
    }

    private void unfreezePlayer(Player target) {
        frozenPlayers.remove(target.getUniqueId());
        target.getPersistentDataContainer().remove(new NamespacedKey(plugin, "cosmic_frozen"));

        // Remove potion effects
        target.removePotionEffect(PotionEffectType.SLOWNESS);
        target.removePotionEffect(PotionEffectType.JUMP_BOOST);
        target.removePotionEffect(PotionEffectType.GLOWING);



        // Unfreeze particles
        if (target.isOnline()) {
            try {
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.05);
            } catch (Exception ignored) {}
            MessageUtil.send(target, "&a✦ You are no longer frozen.");
        }
    }

    // ═══════════════════════════════════════════════════════
    // Launch Player
    // ═══════════════════════════════════════════════════════

    private void launchPlayer(Player player) {
        launchedPlayers.add(player.getUniqueId());

        double launchHeight = plugin.getConfig().getDouble("items.cosmic-judgement.launch-height", 8.0);
        double launchY = 0.6 + (launchHeight * 0.15);
        player.setVelocity(new Vector(0, launchY, 0));

        // Launch particles — intense burst at feet
        Location loc = player.getLocation();
        try {
            player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0),
                    40, 0.3, 0.1, 0.3, 0.2);
        } catch (Exception ignored) {}
        try {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0),
                    30, 0.5, 0.1, 0.5, 0.15);
        } catch (Exception ignored) {}
        try {
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.2, 0),
                    15, 0.4, 0.05, 0.4, 0.02);
        } catch (Exception ignored) {}

        // Launch sound
        player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.6f);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);
        player.getWorld().playSound(loc, "item.cosmic_judgement.comet_trail", 1.2f, 1.0f);

        // Trailing particles while ascending
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 30 || !player.isOnline() || ((org.bukkit.entity.Entity) player).isOnGround()) {
                    this.cancel();
                    return;
                }
                Location pLoc = player.getLocation();
                try {
                    player.getWorld().spawnParticle(Particle.END_ROD, pLoc, 3, 0.1, 0, 0.1, 0.02);
                } catch (Exception ignored) {}
                try {
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 2, 0.1, 0, 0.1, 0.01);
                } catch (Exception ignored) {}
                ticks++;
            }
        }.runTaskTimer(plugin, 2L, 2L);

        // Detect landing for impact particles
        new BukkitRunnable() {
            int ticks = 0;
            boolean wasInAir = false;
            @Override
            public void run() {
                if (ticks >= 200 || !player.isOnline()) {
                    launchedPlayers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                if (!((org.bukkit.entity.Entity) player).isOnGround()) {
                    wasInAir = true;
                }
                if (wasInAir && ((org.bukkit.entity.Entity) player).isOnGround()) {
                    // Landing impact!
                    spawnLandingImpact(player);
                    launchedPlayers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    private void spawnLandingImpact(Player player) {
        Location loc = player.getLocation();

        // Impact circle
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            for (double r = 1; r <= 3; r += 1.0) {
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                Location point = loc.clone().add(x, 0.1, z);
                try {
                    loc.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.02);
                } catch (Exception ignored) {}
            }
        }

        // Explosion at center
        try {
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.5, 0),
                    30, 0.5, 0.3, 0.5, 0.1);
        } catch (Exception ignored) {}
        try {
            loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.3, 0),
                    10, 0.8, 0.1, 0.8, 0.01);
        } catch (Exception ignored) {}

        // Impact sound
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
        loc.getWorld().playSound(loc, "item.cosmic_judgement.heavenfall_impact", 3.0f, 1.0f);
    }

    // ═══════════════════════════════════════════════════════
    // Freeze Movement Block
    // ═══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozenPlayers.contains(player.getUniqueId())) return;

        // Allow head rotation but block position changes
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            // Block movement, keep head rotation
            Location blocked = from.clone();
            blocked.setYaw(to.getYaw());
            blocked.setPitch(to.getPitch());
            event.setTo(blocked);
        }
    }

    // ═══════════════════════════════════════════════════════
    // Hit Cooldown — Blocks all mace damage
    // ═══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (!itemManager.isCustomItem(mainHand, "COSMIC_JUDGEMENT")) return;

        // Apply Smash Hit Bonus if currently launched and not on hit cooldown!
        if (launchedPlayers.contains(attacker.getUniqueId())) {
            if (!plugin.getCooldownManager().isOnCooldown(attacker, 4)) {
                double bonusMultiplier = plugin.getConfig().getDouble("items.cosmic-judgement.smash-bonus-damage-multiplier", 1.25);
                event.setDamage(event.getDamage() * bonusMultiplier);

                org.bukkit.entity.Entity victim = event.getEntity();
                // Aesthetic lightning strike on the target
                victim.getWorld().strikeLightningEffect(victim.getLocation());

                // High impact audio feedback
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.4f);

                // satisfying particle flash
                try {
                    victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                    victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.0);
                } catch (Exception ignored) {}

                MessageUtil.send(attacker, "&5&l✦ COSMIC SMASH! &dDealt massive bonus damage!");

                // Delay cooldown application by 1 tick to allow AOE smash hits in the same tick
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (attacker.isOnline() && !plugin.getCooldownManager().isOnCooldown(attacker, 4)) {
                            boolean enabled = plugin.getConfig().getBoolean("items.cosmic-judgement.hit-cooldown-enabled",
                                    plugin.getConfig().getBoolean("cooldowns.mace-hit-enabled", true));
                            if (enabled) {
                                startHitCooldown(attacker);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }



    private void startHitCooldown(Player player) {
        int maceCooldownSec = plugin.getConfig().getInt("items.cosmic-judgement.hit-cooldown",
                plugin.getConfig().getInt("cooldowns.mace-hit", 45));
        plugin.getCooldownManager().setCooldown(player, 4, maceCooldownSec);

        // Visual cooldown indicator on the mace material
        int ticks = maceCooldownSec * 20;
        player.setCooldown(Material.MACE, ticks); // Configurable cooldown

        MessageUtil.send(player, "&c⚔ &dCosmic Judgement &7is now on cooldown for &c" + maceCooldownSec + " seconds&7.");
    }

    // ═══════════════════════════════════════════════════════
    // Cleanup on player quit
    // ═══════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        frozenPlayers.remove(uuid);
        launchedPlayers.remove(uuid);
        event.getPlayer().removeMetadata("cosmic_frozen", plugin);


    }

    // ═══════════════════════════════════════════════════════
    // Full cleanup on disable
    // ═══════════════════════════════════════════════════════

    public void cleanup() {
        // Cancel tasks
        if (glowTaskId != -1) Bukkit.getScheduler().cancelTask(glowTaskId);
        if (fallingParticleTaskId != -1) Bukkit.getScheduler().cancelTask(fallingParticleTaskId);

        // Unfreeze all players
        for (UUID uuid : new HashSet<>(frozenPlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                unfreezePlayer(p);
            }
        }
        frozenPlayers.clear();
        launchedPlayers.clear();



        // Remove glowing from all online players who might still have it
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "cosmic_frozen"));
        }
    }
}
