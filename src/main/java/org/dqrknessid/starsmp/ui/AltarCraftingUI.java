package org.dqrknessid.starsmp.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryHolder;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.RecipeManager.CustomRecipe;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AltarCraftingUI implements Listener {

    public static class AltarCraftingHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    private final StarSMP plugin;
    private final Component title = MessageUtil.parse("&5&l✦ ALTAR CRAFTING ✦");
    private final java.util.Map<java.util.UUID, org.bukkit.Location> playerAltarLocs = new java.util.HashMap<>();
    
    // Pattern for 3x3 grid in 5x9 inventory
    private final int[] matrixSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    private final int resultSlot = 24;
    private final int craftButtonSlot = 25;

    public AltarCraftingUI(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
    }

    public void open(Player player, org.bukkit.Location loc) {
        playerAltarLocs.put(player.getUniqueId(), loc);
        Inventory inv = Bukkit.createInventory(new AltarCraftingHolder(), 45, title);
        
        // Fill border
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(MessageUtil.parse(" "));
        glass.setItemMeta(meta);

        for (int i = 0; i < 45; i++) {
            if (!isMatrixSlot(i) && i != resultSlot && i != craftButtonSlot) {
                inv.setItem(i, glass);
            }
        }

        // Craft button
        ItemStack craftBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta btnMeta = craftBtn.getItemMeta();
        btnMeta.displayName(MessageUtil.parse("&6&l✦ START RITUAL ✦"));
        inv.setItem(craftButtonSlot, craftBtn);
        updateResult(inv); // Initialize result and button lore

        player.openInventory(inv);
    }

    private boolean isMatrixSlot(int slot) {
        for (int s : matrixSlots) if (s == slot) return true;
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AltarCraftingHolder)) return;
        
        int slot = event.getRawSlot();
        if (slot >= 45) return; // Player inventory

        // Cancel shift-clicks from player inventory to prevent landing in protected slots
        if (event.isShiftClick() && event.getClickedInventory() == event.getView().getBottomInventory()) {
            event.setCancelled(true);
            return;
        }

        // Cancel clicks on border and results
        if (slot < 45 && !isMatrixSlot(slot) && slot != craftButtonSlot) {
            event.setCancelled(true);
            return;
        }

        // Crafting logic
        if (slot == craftButtonSlot) {
            event.setCancelled(true);
            org.bukkit.Location altarLoc = playerAltarLocs.get(event.getWhoClicked().getUniqueId());
            handleCraft((Player) event.getWhoClicked(), event.getInventory(), altarLoc);
            return;
        }

        // Update result on matrix click (after a tick to sync inventory state)
        if (isMatrixSlot(slot)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateResult(event.getInventory()), 1L);
        }
    }

    private void updateResult(Inventory inv) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inv.getItem(matrixSlots[i]);
        }
        
        CustomRecipe recipe = null;
        for (CustomRecipe r : plugin.getRecipeManager().getRecipes().values()) {
            if (r.matches(matrix, plugin.getItemManager())) {
                recipe = r;
                break;
            }
        }

        if (recipe != null) {
            ItemStack result = recipe.getResult(plugin.getItemManager());
            inv.setItem(resultSlot, result);
            
            // Update button lore with duration
            ItemStack button = inv.getItem(craftButtonSlot);
            if (button != null) {
                ItemMeta meta = button.getItemMeta();
                List<Component> lore = new ArrayList<>();
                lore.add(MessageUtil.parse("&7Click to begin the celestial ritual."));
                lore.add(MessageUtil.parse("&7Duration: &b" + (recipe.getRitualDelay() / 60) + "m"));
                meta.lore(lore);
                button.setItemMeta(meta);
            }
        } else {
            inv.setItem(resultSlot, null);
            ItemStack button = inv.getItem(craftButtonSlot);
            if (button != null) {
                ItemMeta meta = button.getItemMeta();
                List<Component> lore = new ArrayList<>();
                lore.add(MessageUtil.parse("&7Invalid recipe patterns."));
                meta.lore(lore);
                button.setItemMeta(meta);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AltarCraftingHolder)) return;
        
        for (int slot : event.getRawSlots()) {
            if (slot < 45 && !isMatrixSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateResult(event.getInventory()), 1L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AltarCraftingHolder)) return;
        playerAltarLocs.remove(event.getPlayer().getUniqueId());
        
        // Drop items in matrix back to player
        for (int slot : matrixSlots) {
            ItemStack item = event.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), item);
            }
        }
    }

    private void handleCraft(Player player, Inventory inv, org.bukkit.Location altarLoc) {
        ItemStack[] matrix = new ItemStack[9];
        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(matrixSlots[i]);
            matrix[i] = stack;
            if (stack != null && stack.getType() != Material.AIR) {
                ingredients.add(stack.clone());
            }
        }

        // Check recipes
        ItemStack result = plugin.getRecipeManager().matchRecipe(matrix);
        
        if (result == null) {
            MessageUtil.send(player, "&cInvalid recipe! Check the patterns.");
            return;
        }

        // Start Ritual
        String itemType = plugin.getRecipeManager().getResultKey(matrix); // e.g., SPEAR_FALLING_STAR
        if (itemType == null) return;
        
        if (!plugin.getLegendaryLimitManager().isCraftingEnabled()) {
            MessageUtil.send(player, "&cLegendary crafting is currently disabled!");
            return;
        }

        if (!plugin.getRecipeManager().isCraftable(itemType)) {
            MessageUtil.send(player, "&cThis item is currently disabled in the configuration.");
            return;
        }

        if (!plugin.getLegendaryLimitManager().canStillBeCrafted(itemType)) {
            MessageUtil.send(player, "&cThe celestial essence of this item has already faded entirely.");
            MessageUtil.send(player, "&7Server limit reached: &e" + plugin.getLegendaryLimitManager().getCurrentCount(itemType) + "/" + plugin.getLegendaryLimitManager().getLimit(itemType));
            return;
        }

        // Get delay
        int ritualDelay = 120;
        for (CustomRecipe r : plugin.getRecipeManager().getRecipes().values()) {
            if (r.matches(matrix, plugin.getItemManager())) {
                ritualDelay = r.getRitualDelay();
                break;
            }
        }

        // Consume ingredients after confirming limit/ritual start
        for (int slot : matrixSlots) inv.setItem(slot, null);
        inv.setItem(resultSlot, null);
        player.closeInventory();

        plugin.getRitualManager().startRitual(player, result, itemType.toUpperCase(), altarLoc, ingredients, ritualDelay);
    }
}
