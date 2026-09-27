package org.dqrknessid.starsmp.animation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.dqrknessid.starsmp.util.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.starsmp.StarSMP;
import org.dqrknessid.starsmp.data.TierState;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.star.StarType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarGachaAnimation {
    private final StarSMP plugin;
    private final Player player;
    private final StarManager starManager;
    private final ItemManager itemManager;
    private final StarType targetStar;
    private final Runnable onComplete;

    public StarGachaAnimation(StarSMP plugin, StarManager starManager, ItemManager itemManager, Player player, StarType targetStar, Runnable onComplete) {
        this.plugin = plugin;
        this.starManager = starManager;
        this.itemManager = itemManager;
        this.player = player;
        this.targetStar = targetStar;
        this.onComplete = onComplete;
    }

    public void start() {
        StarType[] allStars = StarType.values();
        Random random = new Random();
        int rollCount = 30 + random.nextInt(10);
        
        List<StarType> spinOrder = new ArrayList<>();
        for (int i = 0; i < rollCount; i++) {
            spinOrder.add(allStars[random.nextInt(allStars.length)]);
        }
        spinOrder.add(targetStar);

        new BukkitRunnable() {
            int step = 0;
            int delay = 1;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                float progress = (float) step / rollCount;
                
                // Adjust delay to slow down
                if (progress > 0.5f) delay = 2;
                if (progress > 0.7f) delay = 3;
                if (progress > 0.85f) delay = 5;
                if (progress > 0.95f) delay = 8;

                // Simple tick logic based on delay steps
                if (step % delay != 0 && step < rollCount) {
                    step++;
                    return;
                }

                if (step >= rollCount) {
                    finish();
                    this.cancel();
                    return;
                }

                StarType current = spinOrder.get(step);
                Component mainTitle = Component.text("Your Star Is:", NamedTextColor.GOLD);
                Component subTitle = MessageUtil.parse(current.getDisplayName());
                player.showTitle(Title.title(mainTitle, subTitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ZERO)));
                
                float pitch = 0.5f + (progress * 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, pitch);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finish() {
        Component mainTitle = MessageUtil.parse("&e✦ " + targetStar.getDisplayName() + " &e✦");
        Component subTitle = Component.text("Power Awakened!", NamedTextColor.GREEN);
        
        player.showTitle(Title.title(mainTitle, subTitle, Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        starManager.setStar(player, targetStar);
        
        // Remove old stars
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (itemManager.isCustomItem(player.getInventory().getItem(i), "STAR")) {
                player.getInventory().setItem(i, null);
            }
        }
        
        // Add new star item
        player.getInventory().addItem(itemManager.createStarItem(player, targetStar, TierState.DIM));

        if (onComplete != null) {
            onComplete.run();
        }
    }
}
