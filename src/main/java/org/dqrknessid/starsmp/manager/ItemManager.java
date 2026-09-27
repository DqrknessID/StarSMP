package org.dqrknessid.starsmp.manager;

import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.StarType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final StarSMP plugin;
    private final NamespacedKey starItemKey;
    private final NamespacedKey customItemTypeKey;
    private final NamespacedKey starOwnerKey;

    public ItemManager(StarSMP plugin) {
        this.plugin = plugin;
        this.starItemKey = new NamespacedKey(plugin, "is_star_item");
        this.customItemTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.starOwnerKey = new NamespacedKey(plugin, "star_owner");
    }

    // ─── Star Item (held in inventory for passives) ───

    public ItemStack createStarItem(org.bukkit.entity.Player owner, StarType type, TierState tier) {
        String matName = plugin.getConfig().getString("items.star-item.material", "NETHER_STAR");
        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(
                    type.getSymbol() + " &e" + type.getName() + " &7\u2014 &6" + type.getTitle()));

            List<String> loreStrings = new ArrayList<>();
            loreStrings.add("");
            loreStrings.add(type.getQuote());
            loreStrings.add("");
            loreStrings.add("&7A celestial fragment holding");
            loreStrings.add("&7the power of &e" + type.getName() + "&7.");
            loreStrings.add("");
            loreStrings.add("&6Tier: &f" + (tier != null ? tier.getDisplayName() : "Unknown"));
            loreStrings.add("");

            String passiveString = type.getPassiveDesc();
            if (tier == null || tier.getLevel() < 2) {
                if (passiveString.contains(" T2:")) {
                    passiveString = passiveString.substring(0, passiveString.indexOf(" T2:"));
                }
            }
            loreStrings.add("&bPassive: &f" + passiveString);
            loreStrings.add("");

            if (tier != null && tier.getLevel() >= 1) {
                loreStrings.add("&aAbility 1: &f" + type.getAbility1Name());
                loreStrings.add("&7 \u2514 " + type.getAbility1Desc());
            } else {
                loreStrings.add("&8Ability 1: Locked (Requires Tier 1)");
            }

            if (tier != null && tier.getLevel() >= 2) {
                loreStrings.add("&aAbility 2: &f" + type.getAbility2Name());
                loreStrings.add("&7 \u2514 " + type.getAbility2Desc());
            } else {
                loreStrings.add("&8Ability 2: Locked (Requires Tier 2)");
            }

            if (tier != null && tier.getLevel() >= 3) {
                loreStrings.add("&aAbility 3: &f" + type.getAbility3Name());
                loreStrings.add("&7 \u2514 " + type.getAbility3Desc());
            } else {
                loreStrings.add("&8Ability 3: Locked (Requires Tier 3)");
            }

            loreStrings.add("");
            loreStrings.add("&8Bound to player. Cannot be dropped.");
            loreStrings.add("&8Keep anywhere in your inventory.");
            meta.lore(MessageUtil.parseList(loreStrings));

            meta.getPersistentDataContainer().set(starItemKey, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, "STAR");
            if (owner != null) {
                meta.getPersistentDataContainer().set(starOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            }
            item.setItemMeta(meta);

            // Set Data Component AFTER meta to ensure it's not purged
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
                CustomModelData.customModelData().addString(type.name().toLowerCase()).build());
        }
        return item;
    }

    public ItemStack createStarItem(StarType type, TierState tier) {
        return createStarItem(null, type, tier);
    }

    // ─── Star Essence ───

    public ItemStack createEssence() {
        return createEssence(1);
    }

    public ItemStack createEssence(int amount) {
        String matName = plugin.getConfig().getString("items.essence.material", "AMETHYST_SHARD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            mat = Material.AMETHYST_SHARD;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("items.essence.name", "&d✦ Star Essence");
            meta.displayName(MessageUtil.parse(name));

            List<String> loreStrings = new ArrayList<>();
            loreStrings.add("");
            loreStrings.add("&7A powerful cosmic residue left behind");
            loreStrings.add("&7when a Star wielder falls in battle.");
            loreStrings.add("");
            loreStrings.add("&eRight-click&b to ascend your Star's Tier.");
            loreStrings.add("&8Only usable if you possess an active Star.");
            meta.lore(MessageUtil.parseList(loreStrings));

            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, "ESSENCE");
            item.setItemMeta(meta);

            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("essence").build());
        }
        return item;
    }

    // ─── Star Reparation Tool ───

    public ItemStack createReparationTool() {
        String matName = plugin.getConfig().getString("items.reparation-tool.material", "BLAZE_ROD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            mat = Material.BLAZE_ROD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("items.reparation-tool.name", "&6⚙ Star Reparation Tool");
            meta.displayName(MessageUtil.parse(name));

            List<String> loreStrings = new ArrayList<>();
            loreStrings.add("");
            loreStrings.add("&7A precision instrument capable of mending");
            loreStrings.add("&7the shattered core of a dead Star.");
            loreStrings.add("");
            loreStrings.add("&eRight-click&b to repair your Shattered Star.");
            loreStrings.add("&8Restores your Star to Tier 0 (Dim).");
            meta.lore(MessageUtil.parseList(loreStrings));

            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, "REPARATION_TOOL");
            item.setItemMeta(meta);

            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("reparation_tool").build());
        }
        return item;
    }

    // ─── Reroll Token ───

    public ItemStack createRerollToken() {
        String matName = plugin.getConfig().getString("items.reroll-token.material", "COMPASS");
        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            mat = Material.COMPASS;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("items.reroll-token.name", "&b↺ Star Reroll Token");
            meta.displayName(MessageUtil.parse(name));

            List<String> loreStrings = new ArrayList<>();
            loreStrings.add("");
            loreStrings.add("&7A mysterious artifact that scrambles your");
            loreStrings.add("&7astronomic alignment, resetting your fate.");
            loreStrings.add("");
            loreStrings.add("&eRight-click&b to Reroll your active Star.");
            loreStrings.add("&c⚠ Warning: Resets progression to Tier 0!");
            meta.lore(MessageUtil.parseList(loreStrings));

            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, "REROLL_TOKEN");
            item.setItemMeta(meta);

            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("reroll_token").build());
        }
        return item;
    }

    // ─── Star Fragment ───

    public ItemStack createStarFragment() {
        return createStarFragment(1);
    }

    public ItemStack createStarFragment(int amount) {
        String matName = plugin.getConfig().getString("items.star-fragment.material", "BLAZE_POWDER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null)
            mat = Material.BLAZE_POWDER;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("items.star-fragment.name", "&aFragmented Starlight");
            meta.displayName(MessageUtil.parse(name));

            List<String> loreStrings = new ArrayList<>();
            loreStrings.add("");
            loreStrings.add("&7A crystalline shard radiating faint");
            loreStrings.add("&7pulses of an ancient celestial energy.");
            loreStrings.add("");
            loreStrings.add("&8Crafting material used for Star tools.");
            meta.lore(MessageUtil.parseList(loreStrings));

            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, "STAR_FRAGMENT");
            item.setItemMeta(meta);

            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("star_fragment").build());
        }
        return item;
    }

    // ─── Custom Epic Items ───

    private ItemStack createCustomItem(Material mat, String name, String modelId, String typeKey, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            meta.getPersistentDataContainer().set(customItemTypeKey, PersistentDataType.STRING, typeKey);

            if (loreLines != null && loreLines.length > 0) {
                List<String> loreStrings = new ArrayList<>();
                for (String line : loreLines) {
                    loreStrings.add(line);
                }
                meta.lore(MessageUtil.parseList(loreStrings));
            }

            item.setItemMeta(meta);

            // 1.21.5 modern selection string
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
                CustomModelData.customModelData().addString(modelId).build());
        }
        return item;
    }

    public ItemStack createCrownOfAstralSupremacy() {
        ItemStack item = createCustomItem(Material.NETHERITE_HELMET, "&6&lCrown of Astral Supremacy", "crown_astral_supremacy",
                "CROWN_ASTRAL_SUPREMACY",
                "",
                "&8&o\"The one who outshined eternity.\"",
                "",
                "&7A divine relic forged from the nucleus",
                "&7of a hypernova. Its golden radiance",
                "&7warps the fabric of reality itself.",
                "",
                "&ePassive: &fAstral Radiance &b(20 Blocks Aura)",
                "&ePassive: &fHeart of Supremacy &b(20 Hearts)",
                "&ePassive: &fCosmic Alacrity &b(-50% Star CDR)",
                "&6Ability: &fAstral Parry &b(15% Parry Chance)",
                "",
                "&6&lGODLY ARTIFACT");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 4, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.RESPIRATION, 3, true);
            item.setItemMeta(meta);
            
            // Re-apply data for visual selection (since setItemMeta might clear it in some versions)
            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("crown_astral_supremacy").build());
        }
        return item;
    }

    public ItemStack createCosmicJudgement() {
        // Standard 1.21+ Mace
        ItemStack item = createCustomItem(Material.valueOf("MACE"), "&5&lCosmic Judgement", "cosmic_judgement", "COSMIC_JUDGEMENT",
                "",
                "&8&o\"The cosmos bends to the will of its judge.\"",
                "",
                "&dAbility: Cosmic Freeze &7(Shift + Right Click)",
                "&7Unleashes a shockwave in a &b7-block radius&7,",
                "&7freezing enemies for &b5 seconds&7 and launching",
                "&7the wielder &b10 blocks&7 into the sky.",
                "",
                "&cHit Cooldown: &745s &7| &cAbility: &71m 30s",
                "",
                "&5&lLEGENDARY ARTIFACT");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            try {
                org.bukkit.enchantments.Enchantment density = org.bukkit.enchantments.Enchantment.DENSITY;
                if (density != null)
                    meta.addEnchant(density, 3, true);
            } catch (Exception ignored) {
            }
            item.setItemMeta(meta);
            
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
                CustomModelData.customModelData().addString("cosmic_judgement").build());
        }
        return item;
    }

    public ItemStack createSpearOfTheFallingStar() {
        Material spearMat = Material.matchMaterial("NETHERITE_SPEAR");
        if (spearMat == null) return null; // Disabled for versions below 1.21.11

        ItemStack item = createCustomItem(spearMat, "&e&lSpear of the Falling Star", "spear_falling_star",
                "SPEAR_FALLING_STAR",
                "",
                "&8&o\"A strike that pierces the void.\"",
                "",
                "&eAbility: Starfall Overdrive &7(Shift + Right Click)",
                "&7Surges with meteor-like speed, gaining &bSpeed V &7for &b10s&7.",
                "&7During this window, every hit &cbleeds&7 targets for &b5s&7.",
                "",
                "&cCooldown: &73m",
                "",
                "&e&lEPIC ARTIFACT");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
            
            // Lunge Enchantment (Modded/Custom)
            try {
                org.bukkit.enchantments.Enchantment lunge = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("minecraft:lunge")));
                    
                if (lunge != null) meta.addEnchant(lunge, 3, true);
                else {
                    // Try looking for it in current project or mod namespace if minecraft:lunge is not found
                    lunge = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT)
                        .get(TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("starsmp:lunge")));
                    if (lunge != null) meta.addEnchant(lunge, 3, true);
                }
            } catch (Exception ignored) {
                // If the enchantment doesn't exist as a registered key, we'll just skip it for now
                // but the user's modded server should have it.
            }
            
            item.setItemMeta(meta);
            
            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("spear_falling_star").build());
        }
        return item;
    }

    public ItemStack createStarlightOath() {
        ItemStack item = createCustomItem(Material.NETHERITE_SWORD, "&b&lStarlight Oath", "starlight_oath",
                "STARLIGHT_OATH",
                "",
                "&8&o\"A vow sworn to the constellations.\"",
                "",
                "&bAbility: Celestial Vow &7(Shift + Right Click)",
                "&7Enter a celestial trance for &f30s&7, gaining:",
                "&7\u2514 &cStrength II &7& &bSpeed I",
                "&7\u2514 &6Absolute Critical Hits",
                "&7\u2514 &eUnstoppable Strikes &7(Bypasses Shields)",
                "",
                "&cCooldown: &72m",
                "",
                "&b&lEPIC ARTIFACT");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LOOTING, 5, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1, true);
            item.setItemMeta(meta);
            
            // item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, 
            //     CustomModelData.customModelData().addString("starlight_oath").build());
        }
        return item;
    }

    public ItemStack createNebulaGuard() {
        ItemStack item = createCustomItem(Material.SHIELD, "&d&lNebula Guard", "nebula_guard",
                "NEBULA_GUARD",
                "",
                "&8&o\"A bastion of the cosmic void.\"",
                "",
                "&dAbility: Void Repulsion &7(Shift + Right Click)",
                "&7Generates a gravity shockwave that &bknocks back",
                "&7nearby enemies and &eabsorbs &7all incoming",
                "&7projectile damage for &b5 seconds&7.",
                "",
                "&cCooldown: &71m 30s",
                "",
                "&d&lLEGENDARY ARTIFACT");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
            
            item.setData(io.papermc.paper.datacomponent.DataComponentTypes.CUSTOM_MODEL_DATA, 
                io.papermc.paper.datacomponent.item.CustomModelData.customModelData().addString("nebula_guard").build());
        }
        return item;
    }

    // ─── Utility ───

    public boolean isCustomItem(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        String itemType = meta.getPersistentDataContainer().get(customItemTypeKey, PersistentDataType.STRING);
        return type.equals(itemType);
    }

    public boolean isAnyCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        return meta.getPersistentDataContainer().has(customItemTypeKey, PersistentDataType.STRING);
    }

    public boolean isRestrictedStorageItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        String type = meta.getPersistentDataContainer().get(customItemTypeKey, PersistentDataType.STRING);
        if (type == null) return false;

        return type.equals("STAR") || 
               type.equals("COSMIC_JUDGEMENT") || 
               type.equals("SPEAR_FALLING_STAR") || 
               type.equals("CROWN_ASTRAL_SUPREMACY");
    }

    /** Get the StarType from a star item, or null if not a star item. */
    public StarType getStarTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;
        String starName = meta.getPersistentDataContainer().get(starItemKey, PersistentDataType.STRING);
        if (starName == null)
            return null;
        try {
            return StarType.valueOf(starName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getOwnerUuidFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(starOwnerKey, PersistentDataType.STRING);
    }
}
