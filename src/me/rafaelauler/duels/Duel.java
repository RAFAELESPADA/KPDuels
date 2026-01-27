package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Duel {
	  private static MySQLManager mysql;

	    public static void setMySQL(MySQLManager manager) { mysql = manager; }
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
        this.state = DuelState.STARTING;
    }

    public boolean start() {
        if (arena == null) {
            p1.sendMessage("§cNão há arenas disponíveis no momento.");
            p2.sendMessage("§cNão há arenas disponíveis no momento.");
            return false;
        }
        if (arena.isInUse()) return false;
        arena.setInUse(true);
        arena.teleport(p1, p2);

        preparePlayer(p1);
        preparePlayer(p2);

        kit.apply(p1);
        kit.apply(p2);

        state = DuelState.FIGHTING;

        p1.sendMessage("§aDuelo iniciado contra §f" + p2.getName());
        p2.sendMessage("§aDuelo iniciado contra §f" + p1.getName());
        System.out.println("[DUEL] iniciado em " + arena.getName());
        return true;
    }


    public void end(Player winner) {
        if (state == DuelState.ENDED) return;
        state = DuelState.ENDED;

        Player loser = winner != null ? getOpponent(winner) : null;

        // Atualiza stats
        if (mysql != null && winner != null && loser != null) {
            try {
                if (!mysql.isConnected()) mysql.connect();

                PlayerStats win = mysql.getStats(winner.getUniqueId());
                PlayerStats lose = mysql.getStats(loser.getUniqueId());

                win.addWin();           // ✅ aumenta vitórias
                lose.addLoss();         // ✅ aumenta derrotas
                lose.resetWinstreak();  // ✅ zera streak do perdedor

                mysql.saveStats(win);   // ✅ salva no banco
                mysql.saveStats(lose);  // ✅ salva no banco

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Limpeza
        DuelManager.getBoxinghits().remove(p1.getUniqueId());
        DuelManager.getBoxinghits().remove(p2.getUniqueId());

        if (arena != null) ArenaManager.release(arena);

        resetPlayer(winner);
     
        DuelManager.remove(p1);
        DuelManager.remove(p2);
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
        LobbyItems.give(p);
        p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
    }

    public boolean hasPlayer(Player p) { return p.equals(p1) || p.equals(p2); }

    public Player getOpponent(Player p) { return p.equals(p1) ? p2 : p1; }

    public Player getP1() { return p1; }
    public Arena getArena() {
        return arena;
    }
    public Player getP2() { return p2; }
    public KitType getKit() { return kit; }
    public DuelState getState() { return state; }
}
