package org.dqrknessid.starsmp.listener;

import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.animation.StarGachaAnimation;
import org.dqrknessid.starsmp.data.PlayerData;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.manager.CooldownManager;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.player.PlayerDataManager;
import org.dqrknessid.starsmp.star.Star;
import org.dqrknessid.starsmp.star.StarType;
import org.dqrknessid.starsmp.star.impl.ZephyrosStar;
import org.dqrknessid.starsmp.ui.AltarCraftingUI;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.GameMode;

public class PlayerListener implements Listener {

    private final Map<UUID, Boolean> canDoubleJump = new HashMap<>();

    private final StarSMP plugin;
    private final StarManager starManager;
    private final ItemManager itemManager;
    private final PlayerDataManager dataManager;
    private final CooldownManager cooldownManager;

    public PlayerListener(StarSMP plugin, StarManager starManager, ItemManager itemManager,
            PlayerDataManager dataManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.itemManager = itemManager;
        this.dataManager = dataManager;
        this.cooldownManager = cooldownManager;

        // Periodic check to remove blocked spears consistently every 2 seconds (40 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("starsmp.admin")) continue;
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (isBlockedSpear(item)) {
                        player.getInventory().setItem(i, null);
                        MessageUtil.send(player, "&cDisabled spear removed!");
                    }
                }
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (isBlockedSpear(offhand)) {
                    player.getInventory().setItemInOffHand(null);
                    MessageUtil.send(player, "&cDisabled spear removed!");
                }
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        // Check if SpearLimiter is active
        if (plugin.getConfig().getBoolean("limits.SpearLimiter", true)) {
            // Remove any items that look like spears from the drops
            event.getDrops().removeIf(item -> {
                String type = item.getType().name();
                return type.endsWith("_SPEAR") || type.equals("SPEAR");
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        starManager.getPlayerData(player); // Load data

        // Force reset physics and flight on join to fix any "stuck" states
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setGravity(true);
        }
        
        if (!starManager.hasReceivedStar(player)) {
            if (plugin.getConfig().getBoolean("system.started", false)) {
                // Mark as received immediately to prevent relog exploits during animation
                starManager.setHasReceivedStar(player, true);
                
                StarType[] all = StarType.values();
                StarType win = all[new Random().nextInt(all.length)];
                new StarGachaAnimation(plugin, starManager, itemManager, player, win, null).start();
            }
        } else {
            updateStarInInventory(player);
        }

        // --- Altar Setup Notification ---
        if (player.hasPermission("starsmp.admin")) {
            if (plugin.getConfig().getBoolean("altar.enabled", true) && plugin.getAltarManager().getAltarLocations().isEmpty()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    MessageUtil.send(player, "&6&l[StarSMP] &cWarning: Altar system is enabled but no Altar locations are set!");
                    MessageUtil.send(player, "&eUse &f/stars setaltar &eat your location to set an altar.");
                }, 40L); // 2 second delay for visibility
            }
        }

        // --- Blocked Spears Removal ---
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isBlockedSpear(item)) {
                player.getInventory().setItem(i, null);
                MessageUtil.send(player, "&cDisabled spears removed from your inventory!");
            }
        }
        player.updateInventory();
    }

    public void updateStarInInventory(Player player) {
        StarType active = starManager.getActiveStar(player);
        if (active == null)
            return;
        TierState tier = starManager.getActiveTier(player);
        ItemStack updatedStar = itemManager.createStarItem(player, active, tier);

        boolean found = false;

        // Check cursor
        if (itemManager.isCustomItem(player.getItemOnCursor(), "STAR")) {
            player.setItemOnCursor(updatedStar);
            found = true;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack iter = player.getInventory().getItem(i);
            if (itemManager.isCustomItem(iter, "STAR")) {
                if (!found) {
                    player.getInventory().setItem(i, updatedStar);
                    found = true;
                } else {
                    player.getInventory().setItem(i, null); // remove dupes
                }
            }
        }

        if (!found) {
            player.getInventory().addItem(updatedStar);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        dataManager.removeState(player);
        starManager.cleanupPlayer(player);
        if (plugin.getPassiveTickListener() != null) {
            plugin.getPassiveTickListener().removePlayer(uuid);
        }
        
        cooldownManager.clearCooldowns(player);
        canDoubleJump.remove(uuid);
        zephyrosHits.remove(uuid);
        luminaryHits.remove(uuid);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        TierState currentTier = starManager.getActiveTier(victim);

        if (plugin.isGracePeriodActive() || !plugin.getConfig().getBoolean("system.started", false)) {
            if (!plugin.getConfig().getBoolean("system.started", false)) {
                // Not started -> Don't touch drops, don't decrease tier
                return;
            }
            MessageUtil.send(victim, "&bGrace Period is active! You didn't lose your Tier or drop Essence.");
            event.getDrops().removeIf(item -> itemManager.isCustomItem(item, "STAR"));
            return;
        }

        starManager.decreaseTier(victim);
        event.getDrops().removeIf(item -> itemManager.isCustomItem(item, "STAR"));

        if (plugin.getConfig().getBoolean("global.players-drop-skull", true)) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(victim);
                meta.displayName(MessageUtil.parse("&e&l" + victim.getName() + "'s Head"));
                skull.setItemMeta(meta);
            }
            victim.getWorld().dropItemNaturally(victim.getLocation(), skull);
        }

        Player killer = victim.getKiller();
        String cause = victim.getLastDamageCause() != null ? victim.getLastDamageCause().getCause().name() : "UNKNOWN";
        String killerName = killer != null ? killer.getName() : null;
        plugin.getDeathHistoryManager().recordDeath(victim, cause, killerName);

        if (killer != null && killer != victim) {
            boolean shatteredNoDrop = plugin.getConfig().getBoolean("tiers.shattered-no-drop", true);
            
            if (currentTier == TierState.SHATTERED && shatteredNoDrop) {
                MessageUtil.send(killer, "&8Target was Shattered. No essence dropped.");
            } else {
                victim.getWorld().dropItemNaturally(victim.getLocation(), itemManager.createEssence());
                MessageUtil.send(killer, "&d+1 Star Essence &7dropped from your kill!");
            }
        } else {
            boolean shatteredNoDrop = plugin.getConfig().getBoolean("tiers.shattered-no-drop", true);
            if (!(currentTier == TierState.SHATTERED && shatteredNoDrop)) {
                if (currentTier != null && currentTier.getLevel() > 0) {
                    event.getDrops().add(itemManager.createEssence());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        updateStarInInventory(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();

        // --- Blocked Spears Removal ---
        if (isBlockedSpear(current)) {
            event.setCancelled(true);
            event.setCurrentItem(null);
            if (event.getWhoClicked() instanceof Player player) {
                MessageUtil.send(player, "&cDisabled spear removed!");
                player.updateInventory();
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        if (isBlockedSpear(cursor)) {
            event.setCancelled(true);
            event.getWhoClicked().setItemOnCursor(null);
            if (event.getWhoClicked() instanceof Player player) {
                MessageUtil.send(player, "&cDisabled spear removed!");
                player.updateInventory();
            }
            return;
        }

        ItemStack extraInvolvedItem = null;
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            extraInvolvedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
        } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            extraInvolvedItem = event.getWhoClicked().getInventory().getItemInOffHand();
        }

        boolean currentRestricted = itemManager.isRestrictedStorageItem(current);
        boolean cursorRestricted = itemManager.isRestrictedStorageItem(cursor);
        boolean extraRestricted = itemManager.isRestrictedStorageItem(extraInvolvedItem);

        if (currentRestricted || cursorRestricted || extraRestricted) {
            Player clicker = (Player) event.getWhoClicked();
            if (clicker.hasPermission("starsmp.admin")) return; // Admin bypass

            org.bukkit.inventory.Inventory clicked = event.getClickedInventory();
            org.bukkit.inventory.Inventory top = event.getView().getTopInventory();

            // Allow clicking outside the inventory (to drop items)
            if (clicked == null) return;

            // Check if Mace is the ONLY restricted item involved
            boolean isOnlyMaceInvolved = true;
            if (currentRestricted && !itemManager.isCustomItem(current, "COSMIC_JUDGEMENT")) isOnlyMaceInvolved = false;
            if (cursorRestricted && !itemManager.isCustomItem(cursor, "COSMIC_JUDGEMENT")) isOnlyMaceInvolved = false;
            if (extraRestricted && !itemManager.isCustomItem(extraInvolvedItem, "COSMIC_JUDGEMENT")) isOnlyMaceInvolved = false;

            if (isOnlyMaceInvolved) {
                // Mace is allowed to be stored in/taken from chests (single, double, trapped chest, shulker boxes, ender chest)
                if (clicked == top && isStorage(top)) {
                    if (!isAllowedChestForMace(top)) {
                        event.setCancelled(true);
                        MessageUtil.send(clicker, "&cThe Cosmic Judgement can only be stored in chests or shulkers!");
                        return;
                    }
                }

                if (event.isShiftClick()) {
                    org.bukkit.inventory.Inventory destination = (clicked == top) ? event.getView().getBottomInventory() : top;
                    if (isStorage(destination) && !isAllowedChestForMace(destination)) {
                        event.setCancelled(true);
                        MessageUtil.send(clicker, "&cThe Cosmic Judgement can only be stored in chests or shulkers!");
                        return;
                    }
                }
                return; // Allowed in safe inventories / chests!
            }

            // Allow if clicking in a safe inventory
            if (!isStorage(clicked)) {
                // If shift-clicking, only block if the destination (opposite inventory) is storage
                if (event.isShiftClick()) {
                    org.bukkit.inventory.Inventory destination = (clicked == top) ? event.getView().getBottomInventory() : top;
                    if (isStorage(destination)) {
                        event.setCancelled(true);
                        MessageUtil.send(event.getWhoClicked(), "&cYou cannot store Legendary items or your Star in containers!");
                    }
                }
                return;
            }

            // Block direct clicks/placements in storage
            event.setCancelled(true);
            MessageUtil.send(event.getWhoClicked(), "&cYou cannot store Legendary items or your Star in containers!");
            return;
        }
    }

    private boolean isStorage(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return false;
        org.bukkit.event.inventory.InventoryType type = inv.getType();
        return type != org.bukkit.event.inventory.InventoryType.PLAYER &&
               type != org.bukkit.event.inventory.InventoryType.CRAFTING &&
               type != org.bukkit.event.inventory.InventoryType.WORKBENCH &&
               !(inv.getHolder() instanceof AltarCraftingUI.AltarCraftingHolder);
    }

    @SuppressWarnings("deprecation")
    private boolean isAllowedChestForMace(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return false;
        org.bukkit.event.inventory.InventoryType type = inv.getType();
        if (type == org.bukkit.event.inventory.InventoryType.CHEST ||
            type == org.bukkit.event.inventory.InventoryType.SHULKER_BOX ||
            type == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
            return true;
        }

        // Support copper chest plugin/mod-based chests
        try {
            String title = inv.getViewers().isEmpty() ? "" : inv.getViewers().get(0).getOpenInventory().getTitle().toLowerCase();
            if (title.contains("copper") || title.contains("chest")) return true;
        } catch (Exception ignored) {}
        try {
            String holderClass = inv.getHolder() != null ? inv.getHolder().getClass().getSimpleName().toLowerCase() : "";
            if (holderClass.contains("copper") || holderClass.contains("chest")) return true;
        } catch (Exception ignored) {}
        try {
            String invClass = inv.getClass().getSimpleName().toLowerCase();
            if (invClass.contains("copper") || invClass.contains("chest")) return true;
        } catch (Exception ignored) {}

        return false;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked().hasPermission("starsmp.admin")) return; // Admin bypass
        
        boolean isRestricted = itemManager.isRestrictedStorageItem(event.getOldCursor());
        if (!isRestricted) return;

        boolean isMace = itemManager.isCustomItem(event.getOldCursor(), "COSMIC_JUDGEMENT");

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0) continue; // Outside
            org.bukkit.inventory.Inventory clicked = (rawSlot < event.getView().getTopInventory().getSize()) 
                ? event.getView().getTopInventory() 
                : event.getView().getBottomInventory();
            
            if (isStorage(clicked)) {
                if (isMace && isAllowedChestForMace(clicked)) {
                    continue; // Allowed!
                }
                event.setCancelled(true);
                if (isMace) {
                    MessageUtil.send(event.getWhoClicked(), "&cThe Cosmic Judgement can only be stored in chests or shulkers!");
                } else {
                    MessageUtil.send(event.getWhoClicked(), "&cYou cannot store Legendary items or your Star in containers!");
                }
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof GlowItemFrame || event.getRightClicked() instanceof ArmorStand) {
            Player player = event.getPlayer();
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            if (itemManager.isCustomItem(mainHand, "STAR") || itemManager.isCustomItem(offHand, "STAR")) {
                event.setCancelled(true);
                MessageUtil.send(player, "&cYou cannot place your Star here!");
            }
        }
    }

    @EventHandler
    public void onEntityPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.hasPermission("starsmp.admin")) return;
            ItemStack item = event.getItem().getItemStack();
            if (isBlockedSpear(item)) {
                event.setCancelled(true);
                event.getItem().remove(); // delete the item from the ground
                MessageUtil.send(player, "&cDisabled spear removed!");
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("starsmp.admin")) return;
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (isBlockedSpear(item)) {
            player.getInventory().setItem(event.getNewSlot(), null);
            player.updateInventory();
            MessageUtil.send(player, "&cDisabled spear removed!");
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isBlockedSpear(offhand)) {
            player.getInventory().setItemInOffHand(null);
            player.updateInventory();
            MessageUtil.send(player, "&cDisabled spear removed!");
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (itemManager.isCustomItem(item, "STAR")) {
            if (event.getPlayer().hasPermission("starsmp.admin")) return;
            event.setCancelled(true);
            MessageUtil.send(event.getPlayer(), "&cYou cannot drop your Star!");
            return;
        }

        if (itemManager.isCustomItem(item, "COSMIC_JUDGEMENT") || 
            itemManager.isCustomItem(item, "SPEAR_FALLING_STAR") || 
            itemManager.isCustomItem(item, "CROWN_ASTRAL_SUPREMACY")) {
            event.getItemDrop().setGlowing(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("starsmp.admin")) {
            if (isBlockedSpear(event.getMainHandItem()) || isBlockedSpear(event.getOffHandItem())) {
                event.setCancelled(true);
                if (isBlockedSpear(event.getMainHandItem())) player.getInventory().setItemInMainHand(null);
                if (isBlockedSpear(event.getOffHandItem())) player.getInventory().setItemInOffHand(null);
                player.updateInventory();
                MessageUtil.send(player, "&cDisabled spear removed!");
                return;
            }
        }

        PlayerData data = dataManager.getState(player);

        if (data.getActivationMode() == PlayerData.ActivationMode.OFFHAND) {
            StarType type = starManager.getActiveStar(player);
            if (type != null) {
                TierState tier = starManager.getActiveTier(player);
                if (tier != TierState.SHATTERED && tier != TierState.DIM) {
                    boolean hasStar = false;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (itemManager.getStarTypeFromItem(item) == type) {
                            hasStar = true;
                            break;
                        }
                    }

                    if (!hasStar && itemManager.getStarTypeFromItem(player.getItemOnCursor()) == type) {
                        hasStar = true;
                    }

                    if (hasStar) {
                        event.setCancelled(true);
                        Star star = starManager.getStarDefinition(type);
                        if (star != null) {
                            if (!cooldownManager.isOnCooldown(player, 1)) {
                                star.triggerAbility(player, tier);
                                if (type != StarType.UMBRYX) {
                                    int seconds = cooldownManager.getConfigCooldown(type, tier, 1);
                                    cooldownManager.setCooldown(player, 1, seconds);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.ENCHANTING) {
            org.bukkit.Location loc = event.getInventory().getLocation();
            if (loc != null && plugin.getAltarManager().isAtAltar(loc)) {
                event.setCancelled(true);
                // Open our UI after a tiny delay to avoid inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getAltarUI().open((Player) event.getPlayer(), loc);
                }, 1L);
            }
        }
    }


    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        ItemStack item = event.getItem();

        if (isBlockedSpear(item)) {
            event.setCancelled(true);
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();
            MessageUtil.send(player, "&cDisabled spear removed! Only the Spear of the Falling Star is allowed.");
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
            if (plugin.getAltarManager().isAtAltar(event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                plugin.getAltarUI().open(player, event.getClickedBlock().getLocation());
                return;
            }
        }

        if (item == null)
            return;

        // Custom Item Interactivity Check
        if (itemManager.isCustomItem(item, "ESSENCE") || 
            itemManager.isCustomItem(item, "REPARATION_TOOL") || 
            itemManager.isCustomItem(item, "REROLL_TOKEN")) {
            
            if (!plugin.getConfig().getBoolean("system.started", false)) {
                event.setCancelled(true);
                MessageUtil.send(player, "&cStarSMP has not started yet!");
                return;
            }
        }

        if (itemManager.isCustomItem(item, "ESSENCE")) {
            event.setCancelled(true);
            if (starManager.increaseTier(player)) {
                updateStarInInventory(player);
                item.setAmount(item.getAmount() - 1);
            }
        } else if (itemManager.isCustomItem(item, "REPARATION_TOOL")) {
            event.setCancelled(true);
            TierState currentTier = starManager.getActiveTier(player);
            if (currentTier != TierState.SHATTERED) {
                MessageUtil.send(player, "&cYour Star is not shattered! No need for reparation.");
                return;
            }
            starManager.repairStar(player);
            updateStarInInventory(player);
            item.setAmount(item.getAmount() - 1);
            MessageUtil.send(player, "&aStar repaired successfully!");
        } else if (itemManager.isCustomItem(item, "REROLL_TOKEN")) {
            event.setCancelled(true);
            starManager.rerollStar(player);
            updateStarInInventory(player);
            item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler
    public void onPrepareEnchant(org.bukkit.event.enchantment.PrepareItemEnchantEvent event) {
        if (itemManager.isAnyCustomItem(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getAltarManager().isAtAltar(event.getEnchantBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        
        if (itemManager.isAnyCustomItem(first) || itemManager.isAnyCustomItem(second)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            ItemStack mainHand = attacker.getInventory().getItemInMainHand();
            if (isBlockedSpear(mainHand)) {
                event.setCancelled(true);
                attacker.getInventory().setItemInMainHand(null);
                attacker.updateInventory();
                MessageUtil.send(attacker, "&cDisabled spear removed!");
                return;
            }
            StarType active = starManager.getActiveStar(attacker);
            TierState tier = starManager.getActiveTier(attacker);
            if (active != null && tier != TierState.SHATTERED && starManager.isPassiveActive(attacker)) {
                if (isHoldingCorrectly(attacker, active)) {
                    Star star = starManager.getStarDefinition(active);
                    if (star != null)
                        star.onAttack(event, tier);
                        
                    // Zephyros hit charge removed for double jump to make it less buggy/annoying
                    
                    if (active == StarType.LUMINARY) {
                        int hits = luminaryHits.getOrDefault(attacker.getUniqueId(), 0) + 1;
                        if (hits >= 4) {
                            event.setDamage(event.getDamage() * 1.5);
                            try { attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1); } catch (Exception ignored) {}
                            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
                            luminaryHits.remove(attacker.getUniqueId());
                        } else {
                            luminaryHits.put(attacker.getUniqueId(), hits);
                        }
                    }
                }
            }
        }

        if (event.getEntity() instanceof Player victim) {
            StarType active = starManager.getActiveStar(victim);
            TierState tier = starManager.getActiveTier(victim);
            if (active != null && tier != TierState.SHATTERED && starManager.isPassiveActive(victim)) {
                if (isHoldingCorrectly(victim, active)) {
                    Star star = starManager.getStarDefinition(active);
                    if (star instanceof ZephyrosStar z)
                        z.onDamaged(event, tier);
                }
            }
        }
    }

    private final Map<UUID, Integer> zephyrosHits = new HashMap<>();
    private final Map<UUID, Integer> luminaryHits = new HashMap<>();

    @EventHandler
    public void onPlayerToggleFlight(org.bukkit.event.player.PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        StarType active = starManager.getActiveStar(player);
        if (active == StarType.ZEPHYROS && starManager.isPassiveActive(player)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);

            if (canDoubleJump.getOrDefault(player.getUniqueId(), false)) {
                if (cooldownManager.isOnCooldown(player, 2)) return;
                
                // NERF/FIX: Don't jump if sneaking (prevents bridging accidents)
                if (player.isSneaking()) return;

                cooldownManager.setCooldown(player, 2, 15);
                canDoubleJump.put(player.getUniqueId(), false);
                
                org.bukkit.util.Vector jump = player.getLocation().getDirection().normalize().multiply(1.2).setY(0.7);
                player.setVelocity(jump);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.2, 0.5, 0.1);
            }
        }
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        StarType active = starManager.getActiveStar(player);
        boolean isZephyros = active == StarType.ZEPHYROS && starManager.isPassiveActive(player);

        if (((org.bukkit.entity.Entity) player).isOnGround()) {
            if (isZephyros) {
                canDoubleJump.put(player.getUniqueId(), true); // Reset jump capability on ground
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            }
        }

        // Cleanup flight if they shouldn't have it (even in air)
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            if (!isZephyros) {
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            } else {
                // Zephyros players in survival should have allowFlight (for double jump)
                // but should NEVER be in the actual "flying" state.
                if (player.isFlying()) {
                    player.setFlying(false);
                }
            }
        }
    }

    @EventHandler
    public void onBowShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            StarType active = starManager.getActiveStar(player);
            TierState tier = starManager.getActiveTier(player);
            if (active == StarType.PYRELION && tier != null && tier.getLevel() >= 2 && starManager.isPassiveActive(player)) {
                if (isHoldingCorrectly(player, active)) {
                    event.getProjectile().setFireTicks(10000);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        StarType active = starManager.getActiveStar(player);
        TierState tier = starManager.getActiveTier(player);
        if (active == null || tier == TierState.SHATTERED || !starManager.isPassiveActive(player)) return;

        if (!isHoldingCorrectly(player, active)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (active == StarType.ZEPHYROS) {
                // Zephyros: Negate fall damage UNLESS landing on pointed dripstone (spikes)
                Material blockType = player.getLocation().getBlock().getType();
                Material belowType = player.getLocation().clone().add(0, -0.5, 0).getBlock().getType();
                if (blockType == Material.POINTED_DRIPSTONE || belowType == Material.POINTED_DRIPSTONE) {
                    return;
                }

                event.setCancelled(true);
                try { player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1); } catch (Exception ignored) {}
            } else if (active == StarType.GRAVARYN) {
                event.setCancelled(true);
                double originalDamage = event.getDamage();
                if (originalDamage > 0) {
                    double radius = Math.min(5.0, 2.0 + (originalDamage * 0.2));
                    double damage = originalDamage * 0.8;
                    try { player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1, 0, 0, 0, 0); } catch (Exception ignored) {}
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                    for (org.bukkit.entity.Entity e : player.getNearbyEntities(radius, radius, radius)) {
                        if (e instanceof org.bukkit.entity.LivingEntity le && le != player) {
                            le.damage(damage, player);
                        }
                    }
                }
                try { player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1); } catch (Exception ignored) {}
            }
        }

        if (player.hasMetadata("WaterVeil")) {
            long expiry = player.getMetadata("WaterVeil").get(0).asLong();
            if (System.currentTimeMillis() < expiry) {
                event.setDamage(event.getDamage() * 0.3);
            } else {
                player.removeMetadata("WaterVeil", plugin);
            }
        }
    }


    private boolean isBlockedSpear(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String type = item.getType().name();
        if (type.endsWith("_SPEAR") || type.equals("SPEAR")) {
            return !itemManager.isCustomItem(item, "SPEAR_FALLING_STAR");
        }
        return false;
    }

    private boolean isHoldingCorrectly(Player player, StarType activeType) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (itemManager.getStarTypeFromItem(item) == activeType) return true;
        }
        // Also check cursor item so abilities don't stop while moving items
        if (itemManager.getStarTypeFromItem(player.getItemOnCursor()) == activeType) return true;
        
        return false;
    }
}
