package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Duel {

    private final Player p1;
    private final Player p2;
    private final Arena arena;
    private final KitType kit;
    private DuelState state;

    public Duel(Player p1, Player p2, Arena arena, KitType kit) {
        this.p1 = p1;
        this.p2 = p2;
        this.arena = arena;
        this.kit = kit;
        start();
    }

    private void start() {
        state = DuelState.STARTING;

        arena.teleport(p1, p2);

        preparePlayer(p1);
        preparePlayer(p2);

        kit.apply(p1);
        kit.apply(p2);

        state = DuelState.FIGHTING;

        p1.sendMessage("§aDuelo iniciado contra §f" + p2.getName());
        p2.sendMessage("§aDuelo iniciado contra §f" + p1.getName());
    }

    public void end(Player winner) {
        if (state == DuelState.ENDED) return;
        state = DuelState.ENDED;

        Player loser = getOpponent(winner);

        winner.sendMessage("§aVocê venceu o duelo!");
        loser.sendMessage("§cVocê perdeu o duelo!");

        resetPlayer(winner);
        resetPlayer(loser);

        arena.release(); // libera arena
    }

    public Player[] getPlayers() { return new Player[]{p1, p2}; }

    private void preparePlayer(Player p) {
        p.closeInventory();
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    private void resetPlayer(Player p) {
        preparePlayer(p);
        p.teleport(Bukkit.getWorld("world").getSpawnLocation());
    }

    public boolean hasPlayer(Player p) { return p.equals(p1) || p.equals(p2); }

    public Player getOpponent(Player p) { return p.equals(p1) ? p2 : p1; }

    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public KitType getKit() { return kit; }
    public DuelState getState() { return state; }
}
