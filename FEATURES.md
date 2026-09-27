# StarSMP Feature Index

This document outlines every feature in the StarSMP plugin, summarized in exactly one sentence each. For a full detailed breakdown of every Star's abilities, see [STAR_ABILITIES.md](file:///c:/Users/dqrknessid/IdeaProjects/StarSMP/STAR_ABILITIES.md).

## Core Mechanics
* **Star System**: Players can obtain and continuously upgrade magical "Stars" that reside in their inventory to grant unique passive and active elemental abilities.
* **Star Essence**: Players earn Star Essence from entity kills, which serves as the primary currency for upgrading Star tiers and crafting magical items.
* **Star Shattering**: A player's Star can shatter upon specific death circumstances, temporarily disabling it until fixed with a craftable Star Reparation Tool.
* **Gacha System**: An interactive, casino-style spinning GUI allows players to randomly obtain or reroll their assigned Star element, configurable to spin for 60 ticks.
* **Player Dropping Skulls**: A global setting exists to force players to drop their skulls upon death for use in rituals and trophies.

## Specific Star Features (Including Cooldowns)
* **Aqualith (Water)**: Features a 90-second cooldown, a primary ability that applies a 60-tick duration buff, and a secondary ability granting 70% damage reduction.
* **Pyrelion (Fire)**: Operates on a 100-second cooldown, inherently granting a 10% chance to burn attackers for 2 damage, with a flat 15% power bonus inside hot biomes.
* **Zephyros (Wind)**: Possesses a 90-second cooldown, negates damage every 9 hits, and negates all fall damage except when landing on Dripstone.
* **Gravaryn (Earth/Gravity)**: Features a 105-second cooldown and provides Haste II, Resistance I/II, and up to 4 extra hearts alongside a Slowness III smash ability.
* **Voltyra (Lightning)**: Features a 110-second cooldown, utilizing a passive stacking charge system (max 100, +10 per hit) that discharges effects every 5 hits at Tier 2.
* **Verdaryn (Nature)**: Operates on a 90-second cooldown, grants extra hearts, restores hunger every 2 seconds, and creates a life-draining Bloom Dome.
* **Umbryx (Darkness)**: Features a 240-second cooldown and creates a "Void Domain" particle dome that traps enemies inside while buffing the user.
* **Luminary (Light)**: Features a 100-second cooldown, grants Strength/Speed during the day, and releases a purifying light explosion that cleanses the user.
* **Aetherion (Cosmic)**: Summons a central black hole that heavily pulls all nearby entities inward for 80 ticks, operating on a 95-second cooldown.
* **Chronovar (Time)**: Features a 120-second cooldown, natively providing passive Speed, Haste I/II mining speed, and 10% CDR.

## Crafting & Rituals
* **Standard Custom Crafting**: Custom items like the Reroll Token, Essence, and Star Reparation tools are safely craftable via standard Bukkit registered recipes.
* **Fragmented Starlight**: A raw crafting material (Blaze Powder) that can be crafted securely in standard Bukkit crafting tables without exploit overlapping.
* **Altar Rituals**: Players can approach indestructible altars to ritually drop required items, triggering a process complete with localized BossBar tracking for players within 30 blocks.
* **Global Item Limiter**: The server actively limits the total existing number of specific legendary weapons (e.g., exactly 1 Mace or Diamond Spear per server) based on strict config limits.
* **Spear Loot Prevention**: If the global cap for Spears is actively maxed out, the system entirely prevents zombie/skeleton mobs from naturally dropping standard spear items.
* **Legendary Crafting Toggle**: Administrators can instantly enable or disable all legendary and ritual crafting via a single `legendary-craftable` configuration toggle.
* **Anti-Duplication Security**: The plugin prevents hovering exploits in GUIs, ensures players cannot drop essence items early, and wipes legacy bugged items automatically via background ticking.

## Legendary Weapons & Minor Items
* **Cosmic Judgement (Mace)**: A singular legendary mace that allows players to freeze enemies within a 7-block radius and launch themselves upward using a Shift+Right-Click ability.
* **Global Mace Cooldown**: Striking an entity with literally *any* Mace item type instantly triggers a configurable 45-second global cooldown to strictly balance standard damage mechanics.
* **Starlight Oath (Sword)**: A glowing legendary sword that grants a 30-second "Celestial Vow" mode featuring auto-critical hits, immediate shield-breaking properties, and massive speed/strength buffs.
* **Spear of the Falling Star**: A highly restricted, ritual-crafted legendary spear featuring custom throw and meteor-impact abilities that can wipe out groups.
* **Crown of Astral Supremacy**: A legendary wearable crown item offering unique cosmic benefits and status to its wearer.
* **Reroll Token (Compass)**: An accessible crafted token used inside the Gacha Spin GUI to reroll a player's assigned Star if they wish to switch elements.
* **Star Reparation Tool (Blaze Rod)**: An interactive tool specifically crafted and utilized solely to repair shattered Stars back to functional states.

## Configurable Settings
* **Messages**: Server administrators can customize all plugin text, prefixes, and warning messages to fit their server's aesthetic.
* **Items**: The base material, display name, and custom model data value for every custom item can be edited.
* **Altar & Rituals**: Ritual limits, indestructible properties, and Discord webhook integrations for the crafting altars are fully configurable.
* **Gacha GUI Features**: The spin duration, tick delay, and associated sound effects of the Star Gacha wheel can be fine-tuned.
* **Cooldowns & Abilities**: Every single Star's specific ability durations, cooldowns, and the global mace-hit cooldown can be precisely balanced via the config.
