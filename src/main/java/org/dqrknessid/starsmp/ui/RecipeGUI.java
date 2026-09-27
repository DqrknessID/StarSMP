package org.dqrknessid.starsmp.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.manager.RecipeManager.CustomRecipe;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeGUI implements Listener {

    public static class RecipeMainHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    public static class RecipeDetailHolder implements InventoryHolder {
        private final String recipeKey;
        public RecipeDetailHolder(String recipeKey) { this.recipeKey = recipeKey; }
        public String getRecipeKey() { return recipeKey; }
        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    private final StarSMP plugin;
    private final Component title = MessageUtil.parse("&6&l✦ ALL RECIPES ✦");

    public RecipeGUI(StarSMP plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new RecipeMainHolder(), 27, title);

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        Map<String, CustomRecipe> recipes = plugin.getRecipeManager().getRecipes();
        int slot = 10;
        for (CustomRecipe recipe : recipes.values()) {
            if (slot > 16) break; // Simple cap for now
            
            ItemStack result = recipe.getResult(plugin.getItemManager());
            if (result == null) continue;

            ItemMeta meta = result.getItemMeta();
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("&e&l▶ Click to view recipe"));
            
            // Craftable status
            boolean isCraftable = plugin.getRecipeManager().isCraftable(recipe.getTypeKey());
            lore.add(isCraftable ? MessageUtil.parse("&a[CRAFTABLE]") : MessageUtil.parse("&c[DISABLED]"));
            
            meta.lore(lore);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "recipe_key"), PersistentDataType.STRING, recipe.getTypeKey());
            result.setItemMeta(meta);

            inv.setItem(slot++, result);
        }

        player.openInventory(inv);
    }

    public void openRecipeDetail(Player player, String typeKey) {
        CustomRecipe recipe = plugin.getRecipeManager().getRecipes().get(typeKey);
        if (recipe == null) return;

        Component detailTitle = MessageUtil.parse("&0Recipe: " + typeKey.replace("_", " "));
        Inventory inv = Bukkit.createInventory(new RecipeDetailHolder(typeKey), 45, detailTitle);

        // Background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(MessageUtil.parse("&c&l◀ Back to List"));
        back.setItemMeta(backMeta);
        inv.setItem(40, back);

        // Result slot
        ItemStack result = recipe.getResult(plugin.getItemManager());
        inv.setItem(24, result);

        // 3x3 Grid (slots 10,11,12, 19,20,21, 28,29,30)
        int[] grid = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        List<String> shape = recipe.getShape();
        Map<Character, String> ingredients = recipe.getIngredients();

        for (int i = 0; i < shape.size() && i < 3; i++) {
            String row = shape.get(i).replace(" ", "");
            for (int j = 0; j < 3 && j < row.length(); j++) {
                char c = row.charAt(j);
                if (c == ' ') continue;
                String ingKey = ingredients.get(c);
                if (ingKey == null) continue;

                ItemStack item = resolveIngredient(player, ingKey);
                inv.setItem(grid[i * 3 + j], item);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack resolveIngredient(Player player, String key) {
        if (key.equals("STAR_ESSENCE")) return plugin.getItemManager().createEssence();
        if (key.equals("STAR_FRAGMENT")) return plugin.getItemManager().createStarFragment();
        if (key.equals("PLAYER_HEAD")) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                meta.displayName(MessageUtil.parse("&e&lKilled Player's Head"));
                skull.setItemMeta(meta);
            }
            return skull;
        }
        
        Material mat = Material.matchMaterial(key);
        if (mat == null) {
            if (key.contains("POTION")) return new ItemStack(Material.SPLASH_POTION);
            return new ItemStack(Material.BARRIER);
        }
        return new ItemStack(mat);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;
        
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof RecipeMainHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null) return;

            String key = clicked.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "recipe_key"), PersistentDataType.STRING);
            
            if (key != null) {
                openRecipeDetail(player, key);
            }
        } else if (top.getHolder() instanceof RecipeDetailHolder) {
            event.setCancelled(true);
            if (event.getSlot() == 40) {
                openMain(player);
            }
        }
    }
}
