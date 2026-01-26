package me.rafaelauler.duels;


import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int wins;
    private int losses;
    private int winstreak;

    public PlayerStats(UUID uuid, int wins, int losses, int winstreak) {
        this.uuid = uuid;
        this.wins = wins;
        this.losses = losses;
        this.winstreak = winstreak;
    }

    public UUID getUuid() { return uuid; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getWinstreak() { return winstreak; }

    public void addWin() { wins++; winstreak++; }
    public void addLoss() { losses++; winstreak = 0; }
    public void resetWinstreak() { winstreak = 0; }
}

