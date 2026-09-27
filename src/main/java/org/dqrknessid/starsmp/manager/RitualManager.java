package org.dqrknessid.starsmp.manager;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.util.MessageUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class RitualManager {

    private final StarSMP plugin;
    private final Map<UUID, RitualTask> activeRituals = new HashMap<>();

    public RitualManager(StarSMP plugin) {
        this.plugin = plugin;
    }

    public void startRitual(Player player, ItemStack result, String itemType, Location altarLoc, List<ItemStack> ingredients, int ritualDelay) {
        if (activeRituals.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, "&cYou are already performing a ritual.");
            return;
        }

        RitualTask task = new RitualTask(player, result, itemType, altarLoc, ingredients, ritualDelay);
        activeRituals.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth visuals
        
        // Broadcast
        String coords = "(" + altarLoc.getBlockX() + ", " + altarLoc.getBlockY() + ", " + altarLoc.getBlockZ() + ")";
        Bukkit.broadcast(MessageUtil.parse("&e&l✦ RITUAL &7| &f" + player.getName() + " &7has begun a celestial ritual at &e" + coords + " &7for &b").append(result.getItemMeta().displayName()).append(MessageUtil.parse("&7!")));
        
        // Discord Webhook
        sendDiscordWebhook(player, result, coords);
    }

    class RitualTask extends BukkitRunnable {
        private final Player player;
        private final ItemStack result;
        private final String itemType;
        private final Location altarLoc;
        private final Location floatingPos;
        private final int totalTime;
        private int remainingTicks;
        private final List<ItemStack> ingredients;
        private final BossBar bossBar;
        
        private ItemDisplay floatingItem;

        RitualTask(Player player, ItemStack result, String itemType, Location altarLoc, List<ItemStack> ingredients, int ritualDelay) {
            this.player = player;
            this.result = result;
            this.itemType = itemType;
            this.altarLoc = altarLoc.clone();
            this.ingredients = new ArrayList<>(ingredients);
            
            // Center of the enchanting table, floating slightly above
            this.floatingPos = altarLoc.clone().add(0.5, 1.25, 0.5);
            
            this.totalTime = ritualDelay;
            this.remainingTicks = totalTime * 20;

            // BossBar
            String barTitle = LegacyComponentSerializer.legacySection().serialize(
                MessageUtil.parse("&e&l✦ RITUAL: &b").append(result.getItemMeta().displayName())
            );
            this.bossBar = Bukkit.createBossBar(barTitle, BarColor.BLUE, BarStyle.SOLID);
            this.bossBar.addPlayer(player);
            this.bossBar.setProgress(1.0);

            spawnFloatingItem();
        }

        private void spawnFloatingItem() {
            floatingItem = altarLoc.getWorld().spawn(floatingPos, ItemDisplay.class, display -> {
                display.setItemStack(result);
                display.setGlowing(true);
                // Set initial scale
                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0), 
                    new Quaternionf(), 
                    new Vector3f(0.6f, 0.6f, 0.6f), 
                    new Quaternionf()));
            });
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancelRitual("&cRitual failed! You disconnected.", true);
                return;
            }

            if (player.getLocation().distanceSquared(altarLoc) > 144) { // 12 block radius
                cancelRitual("&cRitual failed! You strayed too far from the Altar.", true);
                return;
            }

            if (remainingTicks <= 0) {
                completeRitual();
                return;
            }

            // --- Visuals (Every Tick) ---
            
            // 1. Rotate the floating item
            if (floatingItem != null && floatingItem.isValid()) {
                float angle = (float) (System.currentTimeMillis() % 2000L) / 2000L * 360f;
                Location loc = floatingItem.getLocation();
                loc.setYaw(angle);
                floatingItem.teleport(loc);
                
                // Gentle bobbing
                double bob = Math.sin(System.currentTimeMillis() / 200.0) * 0.05;
                Location bobLoc = floatingPos.clone().add(0, bob, 0);
                bobLoc.setYaw(angle);
                floatingItem.teleport(bobLoc);
            }

            // 2. Beacon Beam (Dense particle column)
            for (double y = 0; y < 15; y += 0.5) {
                altarLoc.getWorld().spawnParticle(Particle.valueOf("END_ROD"), floatingPos.clone().add(0, y, 0), 1, 0.05, 0.05, 0.05, 0.01);
                if (remainingTicks % 2 == 0) {
                    altarLoc.getWorld().spawnParticle(Particle.valueOf("SOUL_FIRE_FLAME"), floatingPos.clone().add(0, y, 0), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // 3. Ground Swirl
            double angle = (remainingTicks * 0.1);
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;
            altarLoc.getWorld().spawnParticle(Particle.valueOf("ENCHANT"), altarLoc.clone().add(0.5 + x, 0.1, 0.5 + z), 5, 0.1, 0.1, 0.1, 0.05);

            // Update BossBar
            bossBar.removeAll();
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(altarLoc.getWorld()) && p.getLocation().distanceSquared(altarLoc) <= 900) { // 30 block radius
                    bossBar.addPlayer(p);
                }
            }
            double progress = (double) remainingTicks / (totalTime * 20);
            bossBar.setProgress(Math.clamp(progress, 0.0, 1.0));

            // --- Logic (Every Second) ---
            if (remainingTicks % 20 == 0) {
                int secondsLeft = remainingTicks / 20;
                if (secondsLeft % 10 == 0 || secondsLeft <= 5) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
                }
            }

            remainingTicks--;
        }

        private void completeRitual() {
            this.cancel();
            activeRituals.remove(player.getUniqueId());
            if (floatingItem != null) floatingItem.remove();
            if (bossBar != null) bossBar.removeAll();
            
            // Check limit again just in case someone else finished while I was waiting
            if (!plugin.getLegendaryLimitManager().canStillBeCrafted(itemType)) {
                cancelRitual("&cRitual failed! Someone else took the limit.", true);
                return;
            }

            plugin.getLegendaryLimitManager().incrementCount(itemType);
            player.getInventory().addItem(result);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, floatingPos, 1, 0, 0, 0, 0);
            MessageUtil.send(player, "&a&l✦ RITUAL SUCCESS! &7The stars have manifested your item.");
            
            String coords = "(" + altarLoc.getBlockX() + ", " + altarLoc.getBlockY() + ", " + altarLoc.getBlockZ() + ")";
            int count = plugin.getLegendaryLimitManager().getCurrentCount(itemType);
            int limit = plugin.getLegendaryLimitManager().getLimit(itemType);
            String limitDisplay = (limit > 0) ? " (&f" + count + "&7/&f" + limit + "&7)" : "";
            
            Bukkit.broadcast(MessageUtil.parse("&e&l✦ RITUAL &7| &f" + player.getName() + " &7has successfully manifested &b")
                .append(result.getItemMeta().displayName())
                .append(MessageUtil.parse("&7 at &e" + coords + limitDisplay + "&7!")));
        }
        
        public void cancelRitual(String msg, boolean dropItems) {
            this.cancel();
            activeRituals.remove(player.getUniqueId());
            if (floatingItem != null) floatingItem.remove();
            if (bossBar != null) bossBar.removeAll();
            
            if (player.isOnline()) {
                MessageUtil.send(player, msg);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
            
            if (dropItems) {
                for (ItemStack stack : ingredients) {
                    if (stack != null && stack.getType() != Material.AIR) {
                        altarLoc.getWorld().dropItemNaturally(altarLoc.clone().add(0.5, 1, 0.5), stack);
                    }
                }
            }
        }
    }

    public void cleanup() {
        for (RitualTask task : new ArrayList<>(activeRituals.values())) {
            task.cancelRitual("&cServer is shutting down. Ritual failed.", true);
        }
        activeRituals.clear();
    }

    private void sendDiscordWebhook(Player player, ItemStack item, String coords) {
        String webhookUrl = plugin.getConfig().getString("altar.discord-webhook-url", "");
        if (webhookUrl.isEmpty()) return;

        String itemName = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName()).replaceAll("§[0-9a-fk-or]", "");
        String json = "{\"content\": null,\"embeds\": [{\"title\": \"✦ Celestial Ritual Started ✦\",\"description\": \"**Player:** " + player.getName() + "\\n**Item:** " + itemName + "\\n**Coordinates:** " + coords + "\",\"color\": 5814783}],\"attachments\": []}";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Java-Webhook");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }
}
