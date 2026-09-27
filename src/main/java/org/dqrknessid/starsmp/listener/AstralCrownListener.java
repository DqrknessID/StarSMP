package org.dqrknessid.starsmp.listener;

import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.manager.ItemManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AstralCrownListener implements Listener {

    private final StarSMP plugin;
    private final ItemManager itemManager;
    private final Random random = new Random();
    private final Map<UUID, Long> parryCooldowns = new HashMap<>();

    public AstralCrownListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack helmet = victim.getInventory().getHelmet();
        if (!itemManager.isCustomItem(helmet, "CROWN_ASTRAL_SUPREMACY")) return;

        UUID uuid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Cooldown check (10 seconds)
        if (parryCooldowns.getOrDefault(uuid, 0L) > now) return;

        // Chance to Parry
        double parryChance = plugin.getConfig().getDouble("items.crown-astral-supremacy.parry-chance", 0.15);
        if (random.nextDouble() < parryChance) {
            event.setCancelled(true);
            parryCooldowns.put(uuid, now + 10000L); // 10s fixed or maybe configurable? Let's keep it 10s or add to config

            // Visuals
            victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.5f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);

            // Feedback
            victim.sendActionBar(MessageUtil.parse("&e✦ &6&lPARRY!&e ✦"));
            
            if (event.getDamager() instanceof Player attacker) {
                MessageUtil.send(attacker, "&c⚠ &7" + victim.getName() + " parried your attack!");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        parryCooldowns.remove(event.getPlayer().getUniqueId());
    }
}
