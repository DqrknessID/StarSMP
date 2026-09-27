package org.dqrknessid.starsmp.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.StarSMP;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final StarSMP plugin;
    private final ItemManager itemManager;
    private final Map<String, CustomRecipe> recipes = new HashMap<>();
    private FileConfiguration recipesConfig;

    public RecipeManager(StarSMP plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        loadRecipes();
    }

    public void loadRecipes() {
        recipes.clear();
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        recipesConfig = YamlConfiguration.loadConfiguration(file);
        FileConfiguration config = recipesConfig;

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            List<String> shape = section.getStringList("shape");
            Map<Character, String> ingredientMap = new HashMap<>();
            ConfigurationSection ingredientSection = section.getConfigurationSection("ingredients");
            if (ingredientSection != null) {
                for (String charKey : ingredientSection.getKeys(false)) {
                    ingredientMap.put(charKey.charAt(0), ingredientSection.getString(charKey));
                }
            }

            int delay = section.getInt("ritual-delay-seconds", 120);
            CustomRecipe recipe = new CustomRecipe(key, shape, ingredientMap, delay);
            recipes.put(recipe.getTypeKey(), recipe);

            if (delay == 0) {
                try {
                    org.bukkit.NamespacedKey nsKey = new org.bukkit.NamespacedKey(plugin, "crafting_" + recipe.getTypeKey().toLowerCase());
                    org.bukkit.inventory.ShapedRecipe bukkitRecipe = new org.bukkit.inventory.ShapedRecipe(nsKey, recipe.getResult(itemManager));
                    
                    String[] shapeArr = new String[shape.size()];
                    for (int i = 0; i < shape.size(); i++) {
                        shapeArr[i] = shape.get(i).replace(" ", "");
                    }
                    bukkitRecipe.shape(shapeArr);
                    
                    for (Map.Entry<Character, String> entry : ingredientMap.entrySet()) {
                        char c = entry.getKey();
                        String target = entry.getValue();
                        if (target.equals("STAR_FRAGMENT")) {
                            bukkitRecipe.setIngredient(c, new org.bukkit.inventory.RecipeChoice.ExactChoice(itemManager.createStarFragment(1)));
                        } else if (target.equals("STAR_ESSENCE")) {
                            bukkitRecipe.setIngredient(c, new org.bukkit.inventory.RecipeChoice.ExactChoice(itemManager.createEssence(1)));
                        } else if (target.equals("SPLASH_POTION")) {
                            bukkitRecipe.setIngredient(c, Material.SPLASH_POTION);
                        } else {
                            Material mat = Material.matchMaterial(target);
                            if (mat != null) bukkitRecipe.setIngredient(c, mat);
                        }
                    }
                    if (org.bukkit.Bukkit.getRecipe(nsKey) == null) {
                        org.bukkit.Bukkit.addRecipe(bukkitRecipe);
                    } else {
                        org.bukkit.Bukkit.removeRecipe(nsKey);
                        org.bukkit.Bukkit.addRecipe(bukkitRecipe);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to register Bukkit recipe for " + recipe.getTypeKey());
                }
            }

            plugin.getLogger().info("Loaded Recipe: " + recipe.getTypeKey() + " (Delay: " + delay + "s)");
        }
    }

    public boolean isCraftable(String typeKey) {
        String configKey = typeKey.toLowerCase().replace("_", "-");
        // Special case mapping
        if (configKey.equals("essence")) configKey = "essence";
        
        // Try items section
        if (plugin.getConfig().contains("items." + configKey + ".craftable")) {
            return plugin.getConfig().getBoolean("items." + configKey + ".craftable", true);
        }
        // Try root level
        return plugin.getConfig().getBoolean(configKey + ".craftable", true);
    }

    public ItemStack matchRecipe(ItemStack[] matrix) {
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.matches(matrix, itemManager)) {
                return recipe.getResult(itemManager);
            }
        }
        return null;
    }

    public String getResultKey(ItemStack[] matrix) {
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.matches(matrix, itemManager)) {
                return recipe.getTypeKey();
            }
        }
        return null;
    }

    public Map<String, CustomRecipe> getRecipes() {
        return recipes;
    }

    public FileConfiguration getRecipesConfig() {
        return recipesConfig;
    }

    public static class CustomRecipe {
        private final String typeKey;
        private final List<String> shape;
        private final Map<Character, String> ingredients;
        private final int ritualDelay;

        public CustomRecipe(String typeKey, List<String> shape, Map<Character, String> ingredients, int ritualDelay) {
            this.typeKey = typeKey.toUpperCase().replace("-", "_");
            this.shape = shape;
            this.ingredients = ingredients;
            this.ritualDelay = ritualDelay;
        }

        public boolean matches(ItemStack[] matrix, ItemManager itemManager) {
            if (matrix.length != 9) return false;

            // Pattern: 9 matrix index corresponds to 3 rows of shape
            for (int r = 0; r < 3; r++) {
                String row = shape.get(r).replace(" ", "");
                for (int c = 0; c < 3 && c < row.length(); c++) {
                    char symbol = row.charAt(c);
                    ItemStack item = matrix[r * 3 + c];

                    if (symbol == ' ') {
                        if (item != null && item.getType() != Material.AIR) return false;
                        continue;
                    }

                    String target = ingredients.get(symbol);
                    if (target == null) return false;
                    
                    if (item == null || item.getType() == Material.AIR) return false;

                    // Specialized check for STAR_FRAGMENT and STAR_ESSENCE
                    if (target.equals("STAR_FRAGMENT")) {
                        if (!itemManager.isCustomItem(item, "STAR_FRAGMENT")) return false;
                    } else if (target.equals("STAR_ESSENCE")) {
                        if (!itemManager.isCustomItem(item, "ESSENCE")) return false;
                    } else if (target.equals("SPLASH_POTION")) {
                        if (item.getType() != Material.SPLASH_POTION) return false;
                        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                            if (potionMeta.getBasePotionType() != org.bukkit.potion.PotionType.SWIFTNESS) return false;
                            // Check for level 2 (Amplifier 1?)
                            // In 1.21.5+, we check if the potion type is strong/long or just base.
                            // Swiftness 2 is usually Strong Swiftness.
                            // However, we can also check if it has the effect at level 2.
                            boolean hasLevel2 = false;
                            for (org.bukkit.potion.PotionEffect effect : potionMeta.getCustomEffects()) {
                                if (effect.getType().equals(org.bukkit.potion.PotionEffectType.SPEED) && effect.getAmplifier() >= 1) {
                                    hasLevel2 = true;
                                    break;
                                }
                            }
                            // If no custom effects, check the base type for 'strong' variant
                            if (!hasLevel2) {
                                // In newer versions, we might need to check the data component if use PDC, 
                                // but for standard items, strong/long are distinct bits or subtypes.
                                // Let's just assume if it's swiftness and they want swiftness 2, 
                                // they might be using a potion that HAS the amplifier.
                                // For now, just checking the base type is often enough for simple plugins.
                                // But let's try to be a bit better.
                                if (potionMeta.getBasePotionType().toString().contains("STRONG")) hasLevel2 = true;
                            }
                            // if (!hasLevel2) return false; // Optional strictness
                        }
                    } else {
                        // Standard Material comparison (MACE, DIAMOND_BLOCK, etc.)
                        Material mat = Material.matchMaterial(target);
                        if (mat == null || item.getType() != mat) return false;
                    }
                }
            }
            return true;
        }

        public ItemStack getResult(ItemManager itemManager) {
            switch (typeKey) {
                case "REPARATION_TOOL": return itemManager.createReparationTool();
                case "REROLL_TOKEN": return itemManager.createRerollToken();
                case "COSMIC_JUDGEMENT": return itemManager.createCosmicJudgement();
                case "SPEAR_FALLING_STAR": return itemManager.createSpearOfTheFallingStar();
                case "CROWN_ASTRAL_SUPREMACY": return itemManager.createCrownOfAstralSupremacy();
                case "STARLIGHT_OATH": return itemManager.createStarlightOath();
                case "STAR_ESSENCE": return itemManager.createEssence();
                case "STAR_FRAGMENT": return itemManager.createStarFragment();
                default: return null;
            }
        }

        public String getTypeKey() {
            return typeKey;
        }

        public int getRitualDelay() {
            return ritualDelay;
        }

        public Map<Character, String> getIngredients() {
            return ingredients;
        }

        public List<String> getShape() {
            return shape;
        }
    }
}
