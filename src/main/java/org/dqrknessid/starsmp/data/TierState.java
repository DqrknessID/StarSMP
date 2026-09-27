package org.dqrknessid.starsmp.data;

public enum TierState {
    SHATTERED(-1, "Shattered"),
    DIM(0, "Dim"),
    AWAKENED(1, "Awakened"),
    ASCENDED(2, "Ascended"),
    ZENITH(3, "Zenith");

    private final int level;
    private final String displayName;

    TierState(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TierState fromLevel(int level) {
        for (TierState tier : values()) {
            if (tier.getLevel() == level) {
                return tier;
            }
        }
        return SHATTERED;
    }
}
