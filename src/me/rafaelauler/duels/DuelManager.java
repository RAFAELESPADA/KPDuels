package me.rafaelauler.duels;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class DuelManager {

    private static final List<Duel> duels = new ArrayList<>();
    private static final Map<UUID, Integer> boxingHits = new HashMap<>();
    private static MySQLManager mysql;

    public static void setMySQL(MySQLManager manager) { mysql = manager; }

    public static void add(Duel duel) {
        duels.add(duel);
        if (duel.getKit() == KitType.BOXING) {
            boxingHits.put(duel.getP1().getUniqueId(), 0);
            boxingHits.put(duel.getP2().getUniqueId(), 0);
        }
    }

    public static Duel get(Player p) {
        for (Duel d : duels) if (d.hasPlayer(p)) return d;
        return null;
    }

    public static boolean isInDuel(Player p) { return get(p) != null; }

    public static List<Duel> getAllDuels() { return new ArrayList<>(duels); }

    public static void addHit(Player p) {
        if (!boxingHits.containsKey(p.getUniqueId())) return;
        boxingHits.put(p.getUniqueId(), boxingHits.get(p.getUniqueId()) + 1);
    }

    public static int getHits(Player p) { return boxingHits.getOrDefault(p.getUniqueId(), 0); }

    public static void end(Duel duel, Player winner) {
        duel.end(winner);
        Player loser = duel.getOpponent(winner);

        try {
            PlayerStats winnerStats = mysql.getStats(winner.getUniqueId());
            winnerStats.addWin();
            mysql.saveStats(winnerStats);

            PlayerStats loserStats = mysql.getStats(loser.getUniqueId());
            loserStats.addLoss();
            mysql.saveStats(loserStats);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        duels.remove(duel);
        boxingHits.remove(duel.getP1().getUniqueId());
        boxingHits.remove(duel.getP2().getUniqueId());
    }
}
