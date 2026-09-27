package org.dqrknessid.starsmp.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.RecipeManager.CustomRecipe;

public class CraftingListener implements Listener {

    private final StarSMP plugin;

    public CraftingListener(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        
        CustomRecipe recipe = null;
        for (CustomRecipe r : plugin.getRecipeManager().getRecipes().values()) {
            if (r.matches(matrix, plugin.getItemManager())) {
                recipe = r;
                break;
            }
        }
        
        // --- Limit Checks for normal items (MACE, DIAMOND_SPEAR, etc.) ---
        ItemStack result = inv.getResult();
        if (result != null) {
            String limitKey = result.getType().name();
            if (plugin.getConfig().contains("limits." + limitKey)) {
                if (!plugin.getLegendaryLimitManager().isCraftingEnabled()) {
                    inv.setResult(null);
                    return;
                }
                if (!plugin.getLegendaryLimitManager().canStillBeCrafted(limitKey)) {
                    inv.setResult(null);
                    return;
                }
            }
        }

        if (recipe == null) {
            // Block any custom items from being used in vanilla recipes
            for (ItemStack item : matrix) {
                if (item != null && plugin.getItemManager().isAnyCustomItem(item)) {
                    inv.setResult(null);
                    return;
                }
            }
            return;
        }

        String typeKey = recipe.getTypeKey();

        // 1. Check Global Crafting Toggle
        if (!plugin.getLegendaryLimitManager().isCraftingEnabled()) {
            inv.setResult(null);
            return;
        }

        // 2. For Altar-only recipes, block them from normal crafting completely.
        if (recipe.getRitualDelay() > 0) {
            inv.setResult(null);
            return;
        }

        // 3. Check if the item is craftable in config
        if (!plugin.getRecipeManager().isCraftable(typeKey)) {
            inv.setResult(null);
            return;
        }

        // 4. Check Server Limits
        if (!plugin.getLegendaryLimitManager().canStillBeCrafted(typeKey)) {
            inv.setResult(null);
            return;
        }

        // 5. Set Result for Fast Recipes (delay == 0)
        inv.setResult(recipe.getResult(plugin.getItemManager()));
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        // --- Custom Recipe (StarSMP) ---
        org.dqrknessid.starsmp.manager.RecipeManager.CustomRecipe matchedRecipe = null;
        for (org.dqrknessid.starsmp.manager.RecipeManager.CustomRecipe r : plugin.getRecipeManager().getRecipes().values()) {
            if (r.matches(matrix, plugin.getItemManager())) {
                matchedRecipe = r;
                break;
            }
        }

        if (matchedRecipe != null) {
            // Check Global Crafting Toggle
            if (!plugin.getLegendaryLimitManager().isCraftingEnabled()) {
                event.setCancelled(true);
                org.dqrknessid.starsmp.util.MessageUtil.send(player, "&cLegendary crafting is currently disabled!");
                return;
            }

            // For fast recipes (delay == 0), native Bukkit crafting handles consumption safely.
            if (matchedRecipe.getRitualDelay() == 0) {
                // Allow the craft, completely dupe-free since Bukkit owns it
                return;
            }

            // All other custom items must be crafted at the Altar.
            event.setCancelled(true);
            org.dqrknessid.starsmp.util.MessageUtil.send(player, "&cYou must perform a ritual at the Altar to craft this!");
            return;
        }

        // --- Normal Limited Items (MACE, DIAMOND_SPEAR, IRON_SPEAR, etc.) ---
        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (result == null) return;

        String limitKey = result.getType().name();
        if (plugin.getConfig().contains("limits." + limitKey)) {
            if (!plugin.getLegendaryLimitManager().isCraftingEnabled()) {
                event.setCancelled(true);
                org.dqrknessid.starsmp.util.MessageUtil.send(player, "&cLegendary crafting is currently disabled!");
                return;
            }

            if (!plugin.getLegendaryLimitManager().canStillBeCrafted(limitKey)) {
                event.setCancelled(true);
                return;
            }

            // Prevent shift-click multi-craft for limited vanilla items to ensure accurate counting
            if (event.isShiftClick()) {
                event.setCancelled(true);
                org.dqrknessid.starsmp.util.MessageUtil.send(player, "&cYou cannot shift-click to craft Legendary items!");
                return;
            }

            // Allow standard crafting to consume ingredients automatically
            plugin.getLegendaryLimitManager().incrementCount(limitKey);

            int count = plugin.getLegendaryLimitManager().getCurrentCount(limitKey);
            int limit = plugin.getLegendaryLimitManager().getLimit(limitKey);
            String limitDisplay = (limit > 0) ? " (&f" + count + "&7/&f" + limit + "&7)" : "";

            org.bukkit.Bukkit.broadcast(org.dqrknessid.starsmp.util.MessageUtil.parse("&e&l✦ CRAFT &7| &f" + player.getName() + " &7has crafted &b")
                .append(result.getItemMeta().hasDisplayName() ? result.getItemMeta().displayName() : net.kyori.adventure.text.Component.text(result.getType().name()))
                .append(org.dqrknessid.starsmp.util.MessageUtil.parse("&7" + limitDisplay + "!")));
        }
    }
}
