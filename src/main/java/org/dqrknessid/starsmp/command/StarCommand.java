package org.dqrknessid.starsmp.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.animation.StarGachaAnimation;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.star.StarType;

import java.util.*;
import java.util.stream.Collectors;

public class StarCommand implements CommandExecutor, TabCompleter {

    private final StarSMP plugin;
    private final StarManager starManager;
    private final ItemManager itemManager;
    private final Set<CommandSender> pendingConfirmations = new HashSet<>();

    public StarCommand(StarSMP plugin, StarManager starManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- Player Commands ---
        switch (sub) {
            case "status" -> {
                if (sender instanceof Player player) {
                    showStatus(player);
                } else {
                    MessageUtil.send(sender, "&cOnly players can use this!");
                }
                return true;
            }
            case "toggle" -> {
                if (sender instanceof Player player) {
                    starManager.togglePassive(player);
                }
                return true;
            }
            case "recipes" -> {
                handleRecipes(sender);
                return true;
            }
            case "withdraw" -> {
                if (sender instanceof Player player) {
                    handleWithdraw(player);
                }
                return true;
            }
        }

        // --- Admin Commands ---
        if (!sender.hasPermission("starsmp.admin")) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }

        switch (sub) {
            case "set" -> {
                return handleSet(sender, args);
            }
            case "remove" -> {
                return handleRemove(sender, args);
            }
            case "settier" -> {
                return handleSetTier(sender, args);
            }
            case "repair" -> {
                return handleRepair(sender, args);
            }
            case "reroll" -> {
                return handleReroll(sender, args);
            }
            case "reset" -> {
                return handleReset(sender, args);
            }
            case "restart" -> {
                return handleRestart(sender, args);
            }
            case "gacha" -> {
                return handleGacha(sender, args);
            }
            case "giveitem" -> {
                return handleGiveItem(sender, args);
            }
            case "zerocooldown" -> {
                return handleZeroCooldown(sender, args);
            }
            case "resetcooldown" -> {
                return handleResetCooldown(sender, args);
            }
            case "info" -> {
                return handleInfo(sender, args);
            }
            case "history" -> {
                return handleHistory(sender, args);
            }
            case "select" -> {
                return handleSelect(sender, args);
            }
            case "start" -> {
                return handleStart(sender, args);
            }
            case "graceperiod" -> {
                return handleGracePeriod(sender, args);
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getAltarManager().loadAltars();
                plugin.getRecipeManager().loadRecipes();
                MessageUtil.send(sender, "&aStarSMP config and recipes reloaded.");
                return true;
            }
            case "setaltar" -> {
                if (sender instanceof Player player) {
                    plugin.getAltarManager().addAltar(player.getLocation());
                    MessageUtil.send(player, "&aAltar set and spawned at your location!");
                } else {
                    MessageUtil.send(sender, "&cOnly players can use this!");
                }
                return true;
            }
            case "removealtar" -> {
                return handleRemoveAltar(sender, args);
            }
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "&6--- StarSMP Commands ---");
        MessageUtil.send(sender, "&e/stars status - Check your current Star");
        MessageUtil.send(sender, "&e/stars toggle - Enable/Disable Passive");
        MessageUtil.send(sender, "&e/stars recipes - View all crafting recipes");
        MessageUtil.send(sender, "&e/stars withdraw - Convert Tier to Essence");
        MessageUtil.send(sender, "&e/stars select - Open the Star Selection GUI");

        if (sender.hasPermission("starsmp.admin")) {
            MessageUtil.send(sender, "&c/stars set <player> <star|random>");
            MessageUtil.send(sender, "&c/stars remove <player>");
            MessageUtil.send(sender, "&c/stars settier <player> <tier>");
            MessageUtil.send(sender, "&c/stars info <player>");
            MessageUtil.send(sender, "&c/stars history <player>");
            MessageUtil.send(sender, "&c/stars resetcooldown <player>");
            MessageUtil.send(sender, "&c/stars repair <player>");
            MessageUtil.send(sender, "&c/stars reroll <player>");
            MessageUtil.send(sender, "&c/stars reset <player|all|items>");
            MessageUtil.send(sender, "&c/stars restart <player|all>");
            MessageUtil.send(sender, "&c/stars gacha <player>");
            MessageUtil.send(sender, "&c/stars giveitem <player> <item> <amount>");
            MessageUtil.send(sender, "&c/stars zerocooldown <player>");
            MessageUtil.send(sender, "&c/stars start");
            MessageUtil.send(sender, "&c/stars graceperiod <seconds|off>");
            MessageUtil.send(sender, "&c/stars reload");
            MessageUtil.send(sender, "&c/stars setaltar");
            MessageUtil.send(sender, "&c/stars removealtar <x> <y> <z>");
        }
    }

    // --- Admin Handlers ---

    private boolean handleStart(CommandSender sender, String[] args) {
        if (plugin.getConfig().getBoolean("system.started", false)) {
            MessageUtil.send(sender, "&cStarSMP has already started!");
            return true;
        }

        if (!pendingConfirmations.contains(sender)) {
            pendingConfirmations.add(sender);
            MessageUtil.send(sender, "&6[StarSMP] &eYou are about to start the game and roll stars for everyone who hasn't one.");
            MessageUtil.send(sender, "&eRun the command again to confirm.");
            return true;
        }
        pendingConfirmations.remove(sender);

        plugin.getConfig().set("system.started", true);
        plugin.saveConfig();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!starManager.hasReceivedStar(p)) {
                StarType[] all = StarType.values();
                StarType win = all[new Random().nextInt(all.length)];
                new StarGachaAnimation(plugin, starManager, itemManager, p, win, null).start();
            }
        }
        MessageUtil.broadcast("&6StarSMP has OFFICIALLY STARTED! Good luck!");
        return true;
    }

    private boolean handleGracePeriod(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (plugin.isGracePeriodActive()) {
                MessageUtil.send(sender, "&bGrace Period is active for " + plugin.getGracePeriodRemaining() + " seconds.");
            } else {
                MessageUtil.send(sender, "&7Grace Period is currently inactive.");
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("off")) {
            plugin.setGracePeriod(-1);
            MessageUtil.broadcast("&c&lGrace Period has been DISABLED!");
            return true;
        }

        try {
            long seconds = Long.parseLong(args[1]);
            plugin.setGracePeriod(seconds);
            MessageUtil.broadcast("&b&lGrace Period has started for " + seconds + " seconds!");
            MessageUtil.broadcast("&7(Stars won't drop and Tiers won't decrease on death)");
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cUsage: /stars graceperiod <seconds|off>");
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }

        if (args[2].equalsIgnoreCase("random")) {
            starManager.giveRandomStar(target);
            updateInventories(target);
            MessageUtil.send(sender, "&aGave random star to " + target.getName());
            return true;
        }

        try {
            StarType type = StarType.valueOf(args[2].toUpperCase());
            starManager.setStar(target, type);
            updateInventories(target);
            MessageUtil.send(sender, "&aSet " + type.name() + " for " + target.getName());
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "&cInvalid Star Type.");
        }
        return true;
    }

    private void updateInventories(Player player) {
        StarType type = starManager.getActiveStar(player);
        TierState tier = starManager.getActiveTier(player);

        // Remove old stars
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (itemManager.isCustomItem(player.getInventory().getItem(i), "STAR")) {
                player.getInventory().setItem(i, null);
            }
        }

        if (type != null) {
            player.getInventory().addItem(itemManager.createStarItem(player, type, tier));
        }
    }

    private boolean handleGacha(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }

        // Clear old stars
        for (int i = 0; i < target.getInventory().getSize(); i++) {
            if (itemManager.isCustomItem(target.getInventory().getItem(i), "STAR")) {
                target.getInventory().setItem(i, null);
            }
        }

        starManager.removeStar(target);
        StarType[] all = StarType.values();
        StarType win = all[new Random().nextInt(all.length)];
        new StarGachaAnimation(plugin, starManager, itemManager, target, win, null).start();
        MessageUtil.send(sender, "&aStarted Title-based Star Gacha for " + target.getName());
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }

        starManager.removeStar(target);
        updateInventories(target);
        MessageUtil.send(sender, "&aRemoved star from " + target.getName());
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        if (!pendingConfirmations.contains(sender)) {
            pendingConfirmations.add(sender);
            MessageUtil.send(sender, "&6[StarSMP] &c&lWARNING: &eYou are about to reset &f" + args[1] + "&e.");
            MessageUtil.send(sender, "&eRun the command again to confirm.");
            return true;
        }
        pendingConfirmations.remove(sender);

        String type = args[1].toLowerCase();
        if (type.equalsIgnoreCase("all")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                starManager.deleteStarData(p);
                updateInventories(p);
            }
            MessageUtil.send(sender, "&cReset ALL online player data.");
        } else if (type.equalsIgnoreCase("items")) {
            plugin.getLegendaryLimitManager().resetAllCounts();
            MessageUtil.send(sender, "&cReset all legendary item limits and counts.");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                starManager.deleteStarData(target);
                updateInventories(target);
                MessageUtil.send(sender, "&aReset data for " + target.getName());
            } else {
                MessageUtil.send(sender, "&cPlayer not found or invalid type (all|items).");
            }
        }
        return true;
    }

    private boolean handleRestart(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        if (args[1].equalsIgnoreCase("all")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                starManager.setHasReceivedStar(p, false);
                p.kick(MessageUtil.parse("&eDataset Restarted. Rejoin for Gacha."));
            }
            MessageUtil.send(sender, "&aRestarted initial gacha for ALL players.");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                starManager.setHasReceivedStar(target, false);
                target.kick(MessageUtil.parse("&eDataset Restarted for you. Rejoin for Gacha."));
                MessageUtil.send(sender, "&aRestarted for " + target.getName());
            }
        }
        return true;
    }

    private boolean handleSetTier(CommandSender sender, String[] args) {
        if (args.length < 3)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null)
            return true;
        try {
            int level = Integer.parseInt(args[2]);
            TierState tier = TierState.fromLevel(level);
            starManager.setTier(target, tier);
            updateInventories(target);
            MessageUtil.send(sender, "&aSet " + target.getName() + " to tier " + level);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cLevel must be a number (-1 to 3).");
        }
        return true;
    }

    private boolean handleRepair(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            starManager.repairStar(target);
            updateInventories(target);
        }
        return true;
    }

    private boolean handleReroll(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            starManager.rerollStar(target);
            updateInventories(target);
        }
        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (args.length < 3)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null)
            return true;

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }

        switch (args[2].toLowerCase()) {
            case "essence" -> target.getInventory().addItem(itemManager.createEssence(amount));
            case "repair_tool" -> target.getInventory().addItem(itemManager.createReparationTool());
            case "reroll_token" -> target.getInventory().addItem(itemManager.createRerollToken());
            case "fragment" -> target.getInventory().addItem(itemManager.createStarFragment(amount));
            case "crown_astral_supremacy" -> target.getInventory().addItem(itemManager.createCrownOfAstralSupremacy());
            case "cosmic_judgement" -> target.getInventory().addItem(itemManager.createCosmicJudgement());
            /*
            case "trident_celestial_tides" ->
                target.getInventory().addItem(itemManager.createTridentOfCelestialTides());
            */
            case "spear_falling_star" -> {
                ItemStack spear = itemManager.createSpearOfTheFallingStar();
                if (spear != null) target.getInventory().addItem(spear);
                else MessageUtil.send(sender, "&cSpear is not available on this server version (Requires 1.21.11+).");
            }
            case "starlight_oath" -> target.getInventory().addItem(itemManager.createStarlightOath());
            case "nebula_guard" -> target.getInventory().addItem(itemManager.createNebulaGuard());
            /*
            case "constellation_breaker" -> target.getInventory().addItem(itemManager.createConstellationBreaker());
            case "starlight_oath" -> target.getInventory().addItem(itemManager.createStarlightOath());
            */
            default -> MessageUtil.send(sender, "&cUnknown item type or item is currently disabled.");
        }
        return true;
    }

    private boolean handleZeroCooldown(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }
        plugin.getCooldownManager().toggleZeroCooldown(target);
        MessageUtil.send(sender, "&aToggled Zero Cooldown for " + target.getName());
        return true;
    }

    private boolean handleResetCooldown(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                MessageUtil.send(sender, "&cUsage: /stars resetcooldown <player>");
                return true;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtil.send(sender, "&cPlayer not found.");
                return true;
            }
        }
        plugin.getCooldownManager().clearCooldowns(target);
        target.setCooldown(org.bukkit.Material.MACE, 0);
        target.setCooldown(org.bukkit.Material.SHIELD, 0);
        try {
            org.bukkit.Material spear = org.bukkit.Material.matchMaterial("NETHERITE_SPEAR");
            if (spear != null) target.setCooldown(spear, 0);
        } catch (Exception ignored) {}

        MessageUtil.send(sender, "&aReset all cooldowns for " + target.getName());
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }

        StarType type = starManager.getActiveStar(target);
        TierState tier = starManager.getActiveTier(target);

        MessageUtil.send(sender, "&6--- Player Info: " + target.getName() + " ---");
        MessageUtil.send(sender, "&eStar: " + (type != null ? type.getSymbol() + " " + type.getName() : "&7None"));
        MessageUtil.send(sender, "&eTier: " + (tier != null ? tier.getDisplayName() : "&7None"));
        MessageUtil.send(sender, "&eHP: &f" + String.format("%.1f", target.getHealth()) + " / " + target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        MessageUtil.send(sender, "&ePassive: " + (starManager.isPassiveActive(target) ? "&aON" : "&cOFF"));
        MessageUtil.send(sender, "&eLocation: &f" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ());
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }

        List<org.dqrknessid.starsmp.manager.DeathHistoryManager.DeathRecord> history = plugin.getDeathHistoryManager().getHistory(target.getUniqueId());
        MessageUtil.send(sender, "&6--- Death History: " + target.getName() + " ---");
        if (history.isEmpty()) {
            MessageUtil.send(sender, "&7No death records found.");
        } else {
            for (org.dqrknessid.starsmp.manager.DeathHistoryManager.DeathRecord record : history) {
                MessageUtil.send(sender, record.format());
            }
        }
        return true;
    }

    private boolean handleRemoveAltar(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.send(sender, "&cUsage: /stars removealtar <x> <y> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);
            org.bukkit.Location loc = new org.bukkit.Location(Bukkit.getWorlds().get(0), x, y, z);
            if (sender instanceof Player p) loc = new org.bukkit.Location(p.getWorld(), x, y, z);

            plugin.getAltarManager().removeAltar(loc);
            MessageUtil.send(sender, "&aAttempted to remove altar at " + x + " " + y + " " + z);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cCoordinates must be numbers.");
        }
        return true;
    }

    private void showStatus(Player player) {
        StarType type = starManager.getActiveStar(player);
        TierState tier = starManager.getActiveTier(player);

        MessageUtil.send(player, "&6--- Current Star ---");
        if (type == null) {
            MessageUtil.send(player, "&7You have no star.");
        } else {
            MessageUtil.send(player, "&eStar: " + type.getSymbol() + " " + type.getName());
            MessageUtil.send(player, "&eTier: " + tier.getDisplayName());
            MessageUtil.send(player, "&ePassive: "
                    + (starManager.isPassiveActive(player) ? "&aON" : "&cOFF"));
        }
    }

    private void handleRecipes(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can open the Recipe GUI.");
            return;
        }
        plugin.getRecipeGUI().openMain(player);
    }

    private void handleWithdraw(Player player) {
        if (!plugin.getConfig().getBoolean("system.started", false)) {
            MessageUtil.send(player, "&cStarSMP has not started yet!");
            return;
        }
        TierState current = starManager.getActiveTier(player);
        if (current.getLevel() <= 0) {
            MessageUtil.send(player, "&cYou need at least Tier 1 (Awakened) to withdraw a tier.");
            return;
        }

        starManager.setTier(player, TierState.fromLevel(current.getLevel() - 1));
        player.getInventory().addItem(itemManager.createEssence(1));
        updateInventories(player);
        MessageUtil.send(player, "&aWithdrew 1 Star Essence. Your tier has decreased.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("status", "toggle", "withdraw", "recipes", "select"));
            if (sender.hasPermission("starsmp.admin")) {
                subs.addAll(Arrays.asList("set", "remove", "settier", "repair", "reroll", "reset", "restart", "gacha",
                        "giveitem", "zerocooldown", "resetcooldown", "info", "history", "start", "graceperiod", "reload", "setaltar", "removealtar"));
            }
            return filter(subs, args[0]);
        }

        if (sender.hasPermission("starsmp.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("removealtar")) {
                List<String> locs = new ArrayList<>();
                for (org.bukkit.Location loc : plugin.getAltarManager().getAltarLocations()) {
                    if (args.length == 2) locs.add(String.valueOf(loc.getBlockX()));
                    if (args.length == 3) locs.add(String.valueOf(loc.getBlockY()));
                    if (args.length == 4) locs.add(String.valueOf(loc.getBlockZ()));
                }
                if (args.length <= 4) return filter(locs, args[args.length - 1]);
            }
            if (args.length == 2) {
                if (Arrays.asList("set", "remove", "settier", "repair", "reroll", "restart", "gacha", "giveitem", "zerocooldown", "resetcooldown", "info", "history").contains(sub)) {
                    return null; // default player completion
                }
                if (sub.equals("reset")) {
                    List<String> options = new ArrayList<>(Arrays.asList("all", "items"));
                    for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
                    return filter(options, args[1]);
                }
                if (sub.equals("graceperiod")) {
                    return filter(Arrays.asList("off", "300", "600", "1800", "3600"), args[1]);
                }
            }
            if (args.length == 3 && sub.equals("set")) {
                List<String> types = Arrays.stream(StarType.values()).map(Enum::name).collect(Collectors.toList());
                types.add("RANDOM");
                return filter(types, args[2]);
            }
            if (args.length == 3 && sub.equals("giveitem")) {
                return filter(Arrays.asList("essence", "repair_tool", "reroll_token", "fragment",
                        "crown_astral_supremacy", "cosmic_judgement", "spear_falling_star", "starlight_oath"), args[2]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> list, String query) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(query.toLowerCase())).collect(Collectors.toList());
    }

    private boolean handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cOnly players can use this!");
            return true;
        }
        plugin.getStarSelectionUI().open(player);
        return true;
    }
}
