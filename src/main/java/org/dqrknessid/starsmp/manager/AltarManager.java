package org.dqrknessid.starsmp.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.dqrknessid.starsmp.StarSMP;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AltarManager implements Listener {

    private final StarSMP plugin;
    private final Set<Location> altarLocations = new HashSet<>();

    public AltarManager(StarSMP plugin) {
        this.plugin = plugin;
        loadAltars();
    }

    public void loadAltars() {
        altarLocations.clear();
        List<String> locStrings = plugin.getConfig().getStringList("altar.locations");
        for (String s : locStrings) {
            String[] parts = s.split(",");
            if (parts.length == 4) {
                World w = Bukkit.getWorld(parts[0]);
                if (w != null) {
                    try {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        altarLocations.add(new Location(w, x, y, z));
                        plugin.getLogger().info("Loaded Altar at: " + parts[1] + ", " + parts[2] + ", " + parts[3]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void addAltar(Location loc) {
        // Core of the altar is the enchanting table
        Location core = loc.clone().getBlock().getLocation();
        if (altarLocations.contains(core)) return;

        altarLocations.add(core);
        saveAltars();
        spawnAltarStructure(core);
    }

    public void removeAltar(Location loc) {
        Location core = loc.clone().getBlock().getLocation();
        if (altarLocations.remove(core)) {
            saveAltars();
            plugin.getLogger().info("Removed Altar at: " + core.getBlockX() + ", " + core.getBlockY() + ", " + core.getBlockZ());
        }
    }

    private void saveAltars() {
        List<String> locStrings = new ArrayList<>();
        for (Location loc : altarLocations) {
            locStrings.add(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        plugin.getConfig().set("altar.locations", locStrings);
        plugin.saveConfig();
    }

    private void spawnAltarStructure(Location loc) {
        World w = loc.getWorld();
        File structureFile = new File(plugin.getDataFolder(), "altar.nbt");
        
        // Try to export from resources if it doesn't exist
        if (!structureFile.exists()) {
            try {
                plugin.saveResource("structures/altar.nbt", false);
                File exported = new File(plugin.getDataFolder(), "structures/altar.nbt");
                if (exported.exists()) {
                    exported.renameTo(structureFile);
                }
            } catch (Exception ignored) {}
        }

        if (structureFile.exists()) {
            try {
                StructureManager sm = Bukkit.getStructureManager();
                Structure structure = sm.loadStructure(structureFile);
                
                // Offset calculation (Assuming structure is saved from a corner, we center it)
                int offsetX = -(structure.getSize().getBlockX() / 2);
                int offsetZ = -(structure.getSize().getBlockZ() / 2);
                
                // Place it with the core location at the center of the X/Z foundation
                structure.place(loc.clone().add(offsetX, 0, offsetZ), false, 
                        org.bukkit.block.structure.StructureRotation.NONE, 
                        org.bukkit.block.structure.Mirror.NONE, 0, 1.0f, new java.util.Random());
                
                // Ensure the center block is specifically an Enchanting Table (core center)
                loc.getBlock().setType(Material.ENCHANTING_TABLE);
                
                plugin.getLogger().info("Spawned Celestial Altar from NBT at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to spawn altar from NBT: " + e.getMessage());
            }
        }

        // Manual Fallback if NBT is missing or fails
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block b = w.getBlockAt(bx + x, by - 1, bz + z);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) b.setType(Material.QUARTZ_BLOCK);
                else if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                    if (Math.abs(x) == 1 && Math.abs(z) == 1) b.setType(Material.OBSIDIAN);
                    else b.setType(Material.CRYING_OBSIDIAN);
                } else b.setType(Material.AMETHYST_BLOCK);
            }
        }

        int[][] corners = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] corner : corners) {
            int cx = bx + corner[0];
            int cz = bz + corner[1];
            w.getBlockAt(cx, by - 1, cz).setType(Material.PURPUR_BLOCK);
            w.getBlockAt(cx, by, cz).setType(Material.QUARTZ_PILLAR);
            w.getBlockAt(cx, by + 1, cz).setType(Material.PURPUR_BLOCK);
            w.getBlockAt(cx, by + 2, cz).setType(Material.AMETHYST_BLOCK);
        }
        w.getBlockAt(bx, by, bz).setType(Material.ENCHANTING_TABLE);
        plugin.getLogger().info("Spawned Manual Celestial Altar at " + bx + " " + by + " " + bz);
    }

    public boolean isAtAltar(Location loc) {
        for (Location altar : altarLocations) {
            if (altar.getWorld().equals(loc.getWorld()) && altar.distanceSquared(loc) <= 25) { 
                return true;
            }
        }
        return false;
    }

    public boolean isNearAltar(Location loc, double radius) {
        double radiusSquared = radius * radius;
        for (Location altar : altarLocations) {
            if (altar.getWorld().equals(loc.getWorld()) && altar.distanceSquared(loc) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    public boolean isAltarBlock(Location loc) {
        return isNearAltar(loc, 3.0);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isNearAltar(event.getBlock().getLocation(), 30.0)) {
            if (!event.getPlayer().hasPermission("starsmp.admin")) {
                event.setCancelled(true);
                MessageUtil.send(event.getPlayer(), "&cThese lands are protected by ancient celestial energy.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isNearAltar(event.getBlock().getLocation(), 30.0)) {
            if (!event.getPlayer().hasPermission("starsmp.admin")) {
                event.setCancelled(true);
                MessageUtil.send(event.getPlayer(), "&cYou cannot build near a Celestial Altar.");
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isNearAltar(block.getLocation(), 30.0));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isNearAltar(block.getLocation(), 30.0));
    }

    public Set<Location> getAltarLocations() {
        return altarLocations;
    }
}
