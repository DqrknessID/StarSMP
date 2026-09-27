package org.dqrknessid.starsmp.star;

public enum StarType {
    AQUALITH("Aqualith", "Star of Tides", "🌊", 101,
            "&8&o\"The ocean's rage, contained in a single drop.\"",
            "Regen in water/rain. Conduit Power. T2: Rain Resistance I.",
            "Leviathan Wake", "Expanding water surge that slows and blinds enemies.",
            "T2 Upgrade", "Wider radius, higher damage, applies Slowness II.",
            "T3 Upgrade", "Massive radius, severe damage, and permanent sustain."),
    PYRELION("Pyrelion", "Star of Cinders", "🔥", 102,
            "&8&o\"A flame that refuses to die, even in the void.\"",
            "Fire Resistance. Chance to heavily burn enemies on hit.",
            "Inferno Surge", "Expanding fire shockwave that ignites & knocks back.",
            "T2 Upgrade", "Wider radius, higher damage, longer burn.",
            "T3 Upgrade", "Massive radius and devastating burns."),
    ZEPHYROS("Zephyros", "Star of Gales", "🌪", 103,
            "&8&o\"The wind whispers secrets of destruction.\"",
            "Speed I. Negates damage every 9 hits with sonic knockback.",
            "Sonic Gale", "Directional sonic boom blast with Wither & Levitation.",
            "T2 Upgrade", "Longer range, higher damage, longer Wither.",
            "T3 Upgrade", "Massive damage and inflicts Weakness for 10s."),
    GRAVARYN("Gravaryn", "Star of Depths", "🪨", 104,
            "&8&o\"The crushing weight of a thousand mountains.\"",
            "Haste I. T2 grants Strength I & +4 Max Health.",
            "Stone Smash", "Ground slam dealing AOE damage & knockup with debris.",
            "T2 Upgrade", "Larger radius, higher damage, grants Strength I.",
            "T3 Upgrade", "Spawns temporary dripstone spikes under enemies."),
    VOLTYRA("Voltyra", "Star of Storms", "⚡", 105,
            "&8&o\"Judgment delivered at the speed of light.\"",
            "Hits charge electricity for stun proc. T2: Mini-lightning every 5 hits.",
            "Thunder Strike", "Repeated lightning strikes at target location.",
            "T2 Upgrade", "Paralyzes enemies (Slowness IX) and chains lightning.",
            "T3 Upgrade", "More strikes and applies Weakness to targets."),
    VERDARYN("Verdaryn", "Star of Wilds", "🌿", 106,
            "&8&o\"Nature's vengeance is slow, but absolute.\"",
            "Health Boost & restores hunger. T2: Resistance. Poison on hit.",
            "Bloom Surge", "Powerful self heal and damages nearby enemies.",
            "T2 Upgrade", "Grants Resistance and poisons enemies.",
            "T3 Upgrade", "Ancient Grove effect: Severe poison and slowness to enemies."),
    UMBRYX("Umbryx", "Star of Shadows", "🌑", 107,
            "&8&o\"Darkness is not the absence of light, but its master.\"",
            "Perm-Invisibility & Night Vision. T2: Wither chance on hit.",
            "Void Domain", "Create a particle dome with Repulsion and Wither I.",
            "T2 Upgrade", "Medium dome. Grants Strength & Regen to user.",
            "T3 Upgrade", "Large dome. Grants Speed II and severe Void Pull."),
    LUMINARY("Luminary", "Star of Radiance", "✨", 108,
            "&8&o\"A beacon in the endless night of the cosmos.\"",
            "Night Vision. Hits build Radiant Charges. T2: Strength during day.",
            "Radiant Pulse", "Consume charges for a powerful blinding light explosion.",
            "T2 Upgrade", "Explodes with larger radius and heavily ignites enemies.",
            "T3 Upgrade", "Massive radius, extreme damage, and heavy Glowing."),
    AETHERION("Aetherion", "Star of Gravity", "🌌", 109,
            "&8&o\"The void pulls all things toward their inevitable end.\"",
            "Resistance I. Crits deal +130% Bonus Damage. Void Pull.",
            "Gravity Well", "Pulls enemies in and crushes them with void force.",
            "T2 Upgrade", "Stronger pull radius and increased crushing damage.",
            "T3 Upgrade", "Spawns a lingering black hole with a void detonation."),
    CHRONOVAR("Chronovar", "Star of Echoes", "⏳", 110,
            "&8&o\"Time is a river, and I am the current.\"",
            "Speed I & Haste I. T2 grants Haste II.",
            "Time Stop", "Freezes entities in a radius completely.",
            "T2 Upgrade", "Longer freeze duration and wider radius.",
            "T3 Upgrade", "Maximum freeze duration and burst damage.");

    private final String name;
    private final String title;
    private final String symbol;
    private final int customModelData;
    private final String quote;
    private final String passiveDesc;
    private final String ability1Name;
    private final String ability1Desc;
    private final String ability2Name;
    private final String ability2Desc;
    private final String ability3Name;
    private final String ability3Desc;

    StarType(String name, String title, String symbol, int customModelData, String quote,
            String passiveDesc, String ability1Name, String ability1Desc,
            String ability2Name, String ability2Desc, String ability3Name, String ability3Desc) {
        this.name = name;
        this.title = title;
        this.symbol = symbol;
        this.customModelData = customModelData;
        this.quote = quote;
        this.passiveDesc = passiveDesc;
        this.ability1Name = ability1Name;
        this.ability1Desc = ability1Desc;
        this.ability2Name = ability2Name;
        this.ability2Desc = ability2Desc;
        this.ability3Name = ability3Name;
        this.ability3Desc = ability3Desc;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getQuote() {
        return quote;
    }

    public String getPassiveDesc() {
        return passiveDesc;
    }

    public String getAbility1Name() {
        return ability1Name;
    }

    public String getAbility1Desc() {
        return ability1Desc;
    }

    public String getAbility2Name() {
        return ability2Name;
    }

    public String getAbility2Desc() {
        return ability2Desc;
    }

    public String getAbility3Name() {
        return ability3Name;
    }

    public String getAbility3Desc() {
        return ability3Desc;
    }

    public String getDisplayName() {
        return symbol + " " + name + " &7— &6" + title;
    }
}
