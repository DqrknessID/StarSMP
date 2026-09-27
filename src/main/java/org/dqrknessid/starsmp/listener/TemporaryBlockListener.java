package org.dqrknessid.starsmp.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Item;

import java.util.HashSet;
import java.util.Set;

public class TemporaryBlockListener implements Listener {

    private final Set<Block> temporaryBlocks = new HashSet<>();

    public void addBlock(Block block) {
        temporaryBlocks.add(block);
    }

    public void removeBlock(Block block) {
        temporaryBlocks.remove(block);
    }

    public void revertAll() {
        for (Block b : temporaryBlocks) {
            b.setType(org.bukkit.Material.AIR);
        }
        temporaryBlocks.clear();
    }

    public boolean isTemporaryBlock(Block block) {
        return temporaryBlocks.contains(block);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (temporaryBlocks.contains(event.getBlock()) || temporaryBlocks.contains(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingBlock() != null && temporaryBlocks.contains(event.getIgnitingBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.getIgnitingBlock() != null && temporaryBlocks.contains(event.getIgnitingBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (temporaryBlocks.contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (temporaryBlocks.contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemBurn(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                Block b = event.getEntity().getLocation().getBlock();
                if (temporaryBlocks.contains(b)) {
                    event.setCancelled(true);
                    event.getEntity().setFireTicks(0);
                }
            }
        }
    }
}
