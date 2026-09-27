# 🌟 StarSMP: Star Abilities & Mechanics (Current)

This document provides a detailed breakdown of all Star abilities and mechanics currently implemented in the StarSMP plugin.

---

## 🌊 Aqualith (Water)
**Theme:** The ocean's rage and the flow of tides.
*   **Passives:**
    *   **Tidal Grace:** Restores 1 HP (0.5 Heart) every 4 seconds when in water or rain.
    *   **Deep Breath:** Permanent Conduit Power effects (Water breathing, Night Vision under water, Haste).
    *   **Rain Shield (Tier 2):** Grants Resistance I during rain.
*   **Ability: Leviathan Wake**
    *   Creates an expanding water surge around the player.
    *   Damages, slows, and blinds enemies.
    *   **Tiers:** Increases radius and damage. Tier 3 offers massive sustain.

## 🔥 Pyrelion (Fire)
**Theme:** Unstoppable heat and cinders.
*   **Passives:**
    *   **Flame Core:** Permanent Fire Resistance.
    *   **Lava Navigator:** Can swim quickly through Lava (Lava Swimming), allowing for fast travel in the Nether.
    *   **Ignite:** 10% chance to burn attackers and deal extra damage on hit.
    *   **Cinder Fury:** 5% damage bonus against enemies who are already on fire.
*   **Ability: Inferno Surge**
    *   An expanding fire shockwave that ignites and knocks back all nearby enemies.
    *   **Tiers:** Increases burn duration, damage, and radius. Tier 3 suppresses Max Health to 8 hearts.

## 🌪 Zephyros (Wind)
**Theme:** The whispers of destruction in the gale.
*   **Passives:**
    *   **Swift Current:** Permanent Speed I buff.
    *   **Wind Shield:** Negates damage completely every 9 hits. When triggered, it creates a sonic knockback around the player.
    *   **FALL DAMAGE IMMUNITY:** Negates all fall damage **EXCEPT** when landing on Pointed Dripstone (vulnerability).
*   **Ability: Sonic Gale**
    *   A directional sonic boom blast.
    *   Inflicts Wither and Levitation on targets hit.
    *   **Tiers:** Longer range, higher damage, and Tier 3 applies Weakness for 10 seconds.

## 🪨 Gravaryn (Earth)
**Theme:** The crushing weight of the mountains.
*   **Passives:**
    *   **Deep Miner:** Permanent Haste I buff.
    *   **Mountain's Might (Tier 2):** Grants **Haste II**, **Resistance I**, **Knockback Resistance (50%)**, and Health Boost I (+2 Hearts).
    *   **Goliath Stance (Tier 3):** Grants **Resistance I**, **Knockback Resistance (70%)**, and **Health Boost II** (+4 Hearts).
    *   **Mountain's Bounty:** Automatically restores 1 hunger point every 3 seconds (Tier 2+).
*   **Ability: Stone Smash**
    *   A powerful ground slam that deals AOE damage and knocks up enemies with flying debris.
    *   **Crushing Impact:** Deals **50% bonus damage** to enemies who are already in the air.
    *   **Tiers:** Larger radius and significantly higher damage. Tier 3 spawns temporary dripstone spikes under enemies.

## ⚡ Voltyra (Lightning)
**Theme:** Judgment delivered at the speed of light.
*   **Passives:**
    *   **Static Charge:** Every hit builds electricity (max 100). When full, the next hit procs a stun (Slowness IX).
    *   **Lightning Chain (Tier 2):** Procs a mini-lightning strike every 5 hits.
*   **Ability: Thunder Strike**
    *   Calls down repeated lightning strikes at the target location.
    *   **Tiers:** Paralyzes enemies, chains lightning, and Tier 3 applies Weakness.

## 🌿 Verdaryn (Nature)
**Theme:** Nature's slow but absolute vengeance.
*   **Passives:**
    *   **Eternal Growth:** Permanent Health Boost (extra hearts based on tier).
    *   **Natural Sustenance:** Automatically restores 1 hunger point (0.5 drumstick) every 1.5 seconds (Buffed).
    *   **Thorn Touch:** 5% chance to poison enemies on hit.
    *   **Bark Skin (Tier 2):** Permanent Resistance I.
*   **Ability: Bloom Surge**
    *   Creates a "Void-like" Nature Dome (Bloom Domain).
    *   **User Buffs:** Instant heal (3x Tier HP), Regeneration II, and **Absorption II** (4 extra hearts) for the dome's duration.
    *   **Enemy Debuffs:** 30% Max Health suppression, Poison, Weakness II, and **Nausea I** (Pollen effect).
    *   **Tiers:** Higher healing and more severe poison/slowness (Ancient Grove effect).

## 🌑 Umbryx (Shadow)
**Theme:** Mastery over the void and darkness.
*   **Passives:**
    *   **Shadow Walker:** Permanent Invisibility and Night Vision.
    *   **Wither Touch (Tier 2):** Chance to inflict Wither on hit.
*   **Ability: Void Domain**
    *   **The Trap:** Creates a bi-directional particle dome. Musuh cannot enter from outside, and musuh inside **cannot exit** (Trapped).
    *   **User Buffs:** Strength I and Regeneration II (T2+). Tier 3 grants Speed II.
    *   **Enemy Debuffs:** Darkness, Slowness II, and Wither I (T2+).
    *   **Void Pull (Tier 3):** Pulls enemies toward the center of the dome.

## ✨ Luminary (Light)
**Theme:** A beacon in the endless night.
*   **Passives:**
    *   **Cosmic Sight:** Permanent Night Vision.
    *   **Solar Flare (Tier 2):** Grants Strength I and **Speed I** during daytime.
    *   **Radiant Charge:** Hits build charges for the pulse ability.
*   **Ability: Radiant Pulse**
    *   Consumes charges to release a blinding light explosion.
    *   **Purifying Light:** Clears all negative potion effects (Poison, Wither, etc.) from the user on use.
    *   **Solar Shield:** Grants the user **Resistance I** and **Speed III** for a short duration.
    *   Blinds and reveals (Glowing) all nearby enemies.
    *   **Nerfed:** Radiant damage and blindness duration have been adjusted for better balance.

## 🌌 Aetherion (Gravity)
**Theme:** The inevitable pull toward the void.
*   **Passives:**
    *   **Gravity Master:** Permanent Resistance I.
    *   **Crushing Blows:** Critical hits (jump-hits) deal +130% Bonus Damage.
    *   **Void Pull:** Small chance to pull enemies toward you on hit.
*   **Ability: Gravity Well**
    *   Summons a central black hole that pulls all nearby entities inward and crushes them.
    *   **Tiers:** Stronger pull, higher crushing damage, and Tier 3 causes a massive void detonation at the end.

## ⏳ Chronovar (Time)
**Theme:** Flowing through the river of time.
*   **Passives:**
    *   **Temporal Haste:** Permanent Speed I and **Haste I** (Base). Tier 2 grants **Haste II**.
    *   **Time Warp:** 10% cooldown reduction (CDR) on all abilities.
*   **Ability: Time Stop**
    *   Completely freezes all entities (mobs and players) in a radius.
    *   **Tiers:** Longer freeze duration, wider radius, and Tier 3 adds a burst of damage upon unfreezing.

---

## 🛠 Global Mechanics

### **Biome Affinity**
Stars now gain bonus stats in their native environments (e.g., Aqualith in Oceans, Pyrelion in Deserts, Gravaryn in Deep Caves).

### **Star Upgrading**
*   Stars can be upgraded using **Star Essence** (earned from kills).
*   Higher Tiers (1-3) significantly boost ability range, damage, and unlock new passive effects.

### **Star Shattering**
*   Deaths can cause a Star to "Shatter," turning it into a Shattered Star item.
*   Shattered Stars grant **no passives** and **no abilities**.
*   Must be fixed using a **Star Reparation Tool**.

---
*Note: This document is updated as of May 12, 2026. Abilities are subject to balance changes.*
