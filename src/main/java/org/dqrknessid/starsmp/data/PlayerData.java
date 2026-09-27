package org.dqrknessid.starsmp.data;

import org.dqrknessid.starsmp.star.StarType;

public class PlayerData {
    public enum ActivationMode {
        OFFHAND, COMMAND
    }

    private StarType currentStar;
    private TierState currentTier;
    private int energy; // We'll keep this but the user wanted to hide commands related to it.
    private boolean passiveToggled; // true = passive active (default on)
    private boolean hasReceivedInitialStar; // prevents re-gifting star on join
    private ActivationMode activationMode;
    private java.util.Set<java.util.UUID> trustedPlayers;
    private long firstJoinTime;

    public PlayerData() {
        this.currentStar = null;
        this.currentTier = TierState.DIM;
        this.energy = 0;
        this.passiveToggled = true;
        this.hasReceivedInitialStar = false;
        this.activationMode = ActivationMode.OFFHAND;
        this.trustedPlayers = new java.util.HashSet<>();
        this.firstJoinTime = System.currentTimeMillis();
    }

    public PlayerData(StarType currentStar, TierState currentTier, int energy,
            boolean passiveToggled, boolean hasReceivedInitialStar, ActivationMode activationMode) {
        this.currentStar = currentStar;
        this.currentTier = currentTier;
        this.energy = energy;
        this.passiveToggled = passiveToggled;
        this.hasReceivedInitialStar = hasReceivedInitialStar;
        this.activationMode = activationMode != null ? activationMode : ActivationMode.OFFHAND;
        this.trustedPlayers = new java.util.HashSet<>();
    }

    public StarType getCurrentStar() {
        return currentStar;
    }

    public void setCurrentStar(StarType currentStar) {
        this.currentStar = currentStar;
    }

    public TierState getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(TierState currentTier) {
        this.currentTier = currentTier;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy);
    }

    public void addEnergy(int amount) {
        this.energy = Math.max(0, this.energy + amount);
    }

    public boolean isPassiveToggled() {
        return passiveToggled;
    }

    public void setPassiveToggled(boolean toggled) {
        this.passiveToggled = toggled;
    }

    public boolean hasReceivedInitialStar() {
        return hasReceivedInitialStar;
    }

    public void setHasReceivedInitialStar(boolean b) {
        this.hasReceivedInitialStar = b;
    }

    public ActivationMode getActivationMode() {
        return activationMode;
    }

    public void setActivationMode(ActivationMode activationMode) {
        this.activationMode = activationMode;
    }

    public boolean isTrusted(java.util.UUID uuid) {
        return trustedPlayers.contains(uuid);
    }

    public void trustPlayer(java.util.UUID uuid) {
        trustedPlayers.add(uuid);
    }

    public void untrustPlayer(java.util.UUID uuid) {
        trustedPlayers.remove(uuid);
    }

    public java.util.Set<java.util.UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    public long getFirstJoinTime() {
        return firstJoinTime;
    }

    public void setFirstJoinTime(long time) {
        this.firstJoinTime = time;
    }
}
