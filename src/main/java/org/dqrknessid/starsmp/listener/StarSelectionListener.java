package org.dqrknessid.starsmp.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.star.StarType;
import org.dqrknessid.starsmp.util.MessageUtil;

public class StarSelectionListener implements Listener {

    public StarSelectionListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(MessageUtil.parse("&0Select Your Star Path"))) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        StarType selected = null;
        Material mat = item.getType();

        if (mat == Material.PRISMARINE_CRYSTALS) selected = StarType.AQUALITH;
        else if (mat == Material.BLAZE_POWDER) selected = StarType.PYRELION;
        else if (mat == Material.FEATHER) selected = StarType.ZEPHYROS;
        else if (mat == Material.NETHERITE_INGOT) selected = StarType.GRAVARYN;
        else if (mat == Material.COPPER_INGOT) selected = StarType.VOLTYRA;
        else if (mat == Material.EMERALD) selected = StarType.VERDARYN;
        else if (mat == Material.AMETHYST_SHARD) selected = StarType.UMBRYX;
        else if (mat == Material.GLOWSTONE_DUST) selected = StarType.LUMINARY;
        else if (mat == Material.ENDER_PEARL) selected = StarType.AETHERION;
        else if (mat == Material.CLOCK) selected = StarType.CHRONOVAR;

        if (selected != null) {
            player.closeInventory();
            
            // Execute the selection command logic
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "star set " + player.getName() + " " + selected.name());
            
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.getWorld().spawnParticle(org.bukkit.Particle.valueOf("FIREWORK"), player.getLocation(), 100, 1, 1, 1, 0.1);
        }
    }
}
