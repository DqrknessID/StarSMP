package org.dqrknessid.starsmp.star;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.dqrknessid.starsmp.data.TierState;

public interface Star {

    StarType getType();

    /**
     * Called continuously (e.g. every second or tick) to apply passive effects
     * based on the player's current tier.
     */
    void applyPassives(Player player, TierState tier);

    /**
     * Called when the player attacks another entity (useful for passives like Fire
     * Aspect or Electric charges).
     */
    void onAttack(EntityDamageByEntityEvent event, TierState tier);

    /**
     * Trigger Single Ability (Scales with Tier)
     * 
     * @param player The player triggering the ability
     * @param tier   The current Tier State of the player's star
     */
    void triggerAbility(Player player, TierState tier);

    /**
     * Called when a player quits or their data needs to be cleared.
     * Prevents memory leaks by clearing player-specific maps.
     */
    default void cleanup(java.util.UUID uuid) {}
}
