package org.dqrknessid.starsmp.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class StarSelectionUI {

    public StarSelectionUI() {
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtil.parse("&0Select Your Star Path"));

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(MessageUtil.parse(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        
        // ... (rest of the setItem calls remain the same as they use createStarIcon which I will fix)
        // Note: I will keep the items for brevity in this replace but I need to make sure they call the new createStarIcon correctly.
        // Actually I should probably include them to be sure.
        setupIcons(inv);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private void setupIcons(Inventory inv) {
        inv.setItem(10, createStarIcon(Material.PRISMARINE_CRYSTALS, "&bAqualith Star", 
            "&7Master of the Tides.",
            "&f- &7Dolphin's Grace & Water Breathing",
            "&f- &7Bonus damage in water",
            "&f- &9&lAbility: &7Create a tidal dome that slows",
            "&7  and blinds enemies."));

        inv.setItem(11, createStarIcon(Material.BLAZE_POWDER, "&cPyrelion Star",
            "&7Avatar of the Inferno.",
            "&f- &7Fire Resistance",
            "&f- &7Chance to burn enemies on hit",
            "&f- &c&lAbility: &7Unleash expanding rings of fire",
            "&7  that burn and damage nearby foes."));

        inv.setItem(12, createStarIcon(Material.FEATHER, "&fZephyros Star",
            "&7Spirit of the Gale.",
            "&f- &7Speed I & No Fall Damage",
            "&f- &7Passive knockback shield on hit",
            "&f- &f&lAbility: &7Rapid dash that knocks enemies",
            "&7  into the air and applies wither."));

        inv.setItem(13, createStarIcon(Material.NETHERITE_INGOT, "&8Gravaryn Star",
            "&7Titan of the Earth.",
            "&f- &7Haste & Strength passives",
            "&f- &7Increased maximum health",
            "&f- &8&lAbility: &7Gravity smash that knocks up",
            "&7  enemies and traps them in stone."));

        inv.setItem(14, createStarIcon(Material.COPPER_INGOT, "&eVoltyra Star",
            "&7Conduit of the Storm.",
            "&f- &7Chain lightning on critical hits",
            "&f- &7Lightning aura during storms",
            "&f- &e&lAbility: &7Summon multiple lightning strikes",
            "&7  upon nearby enemies."));

        inv.setItem(15, createStarIcon(Material.EMERALD, "&aVerdaryn Star",
            "&7Guardian of the Wild.",
            "&f- &7Passive Regeneration in nature",
            "&f- &7Poison on hit passive",
            "&f- &a&lAbility: &7Nature's Dome that heals allies",
            "&7  and suppresses enemy health."));

        inv.setItem(16, createStarIcon(Material.AMETHYST_SHARD, "&5Umbryx Star",
            "&7Shadow of the Void.",
            "&f- &7Permanent Invisibility passive",
            "&f- &7Wither strikes chance",
            "&f- &5&lAbility: &7Summon a void dome that blinds",
            "&7  enemies and buffs the caster."));

        inv.setItem(19, createStarIcon(Material.GLOWSTONE_DUST, "&6Luminary Star",
            "&7Herald of the Dawn.",
            "&f- &7Night Vision & Day Strength",
            "&f- &7Heal on hit passive",
            "&f- &6&lAbility: &7Radiant blast that burns and",
            "&7  reveals enemies with Glowing."));

        inv.setItem(20, createStarIcon(Material.ENDER_PEARL, "&dAetherion Star",
            "&7Architect of Space.",
            "&f- &7Massive critical damage bonus",
            "&f- &7Passive void pull on hit",
            "&f- &d&lAbility: &7Create a black hole that pulls",
            "&7  and crushes enemies inside."));

        inv.setItem(21, createStarIcon(Material.CLOCK, "&3Chronovar Star",
            "&7Lord of the Timeline.",
            "&f- &7Passive Speed & Haste",
            "&f- &7Chance to freeze enemies on hit",
            "&f- &3&lAbility: &7Stop time in a massive radius,",
            "&7  freezing all enemies in place."));
    }

    private ItemStack createStarIcon(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtil.parse(name));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.empty());
        for (String line : loreLines) {
            lore.add(MessageUtil.parse(line));
        }
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(MessageUtil.parse("&eClick to select this path!"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
