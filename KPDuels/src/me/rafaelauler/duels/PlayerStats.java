package me.rafaelauler.duels;


import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int wins;
    private int losses;
    private int winstreak;
    private transient int saveAttempts = 0;
    private volatile boolean dirty;
    public int incrementSaveAttempts() {
        return ++saveAttempts;
    }



    /* ================= CORE ================= */


    public PlayerStats(UUID uuid, int wins, int losses, int winstreak) {
        this.uuid = uuid;
        this.wins = wins;
        this.losses = losses;
        this.winstreak = winstreak;
    }
    public int getSaveAttempts() {
        return saveAttempts;
    }

    public void setSaveAttempts(int attempts) {
        this.saveAttempts = attempts;
    }

    public void resetSaveAttempts() {
        this.saveAttempts = 0;
    }
    public UUID getUuid() { return uuid; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getWinstreak() { return winstreak; }

    public void addWin() { wins++; winstreak++;  dirty = true; }
    public void addLoss() { losses++; winstreak = 0;   dirty = true; }
    public void resetWinstreak() { winstreak = 0 ;  dirty = true; }
    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }


}

