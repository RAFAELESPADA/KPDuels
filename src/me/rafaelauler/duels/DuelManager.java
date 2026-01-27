package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class DuelManager {

    private static final List<Duel> duels = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Integer> boxingHits = new HashMap<>();

    public static void add(Duel duel) {
        duels.add(duel);

        if (duel.getKit() == KitType.BOXING) {
            boxingHits.put(duel.getP1().getUniqueId(), 0);
            boxingHits.put(duel.getP2().getUniqueId(), 0);
        }
    }

    public static Duel get(Player p) {
        synchronized (duels) {
            for (Duel d : duels) {
                if (d.hasPlayer(p)) return d;
            }
        }
        return null;
    }
    public static void remove(Player p) {
        synchronized (duels) {
            duels.removeIf(d -> d.hasPlayer(p));
            boxingHits.remove(p.getUniqueId());
        }
    }
    public static boolean isInDuel(Player p) {
        return get(p) != null;
    }

    /** 🔥 ÚNICO JEITO CORRETO DE FINALIZAR UM DUELO */
    public static void end(Player winner) {

        Duel duel = get(winner);
        if (duel == null) return;

        synchronized (duels) {
            duel.end(winner);
            duels.remove(duel);
        }
    }

    /** ⚠️ Usado apenas para quit / crash */
    public static void forceEnd(Player p) {

        Duel duel = get(p);
        if (duel == null) return;

        synchronized (duels) {
            duel.end(null);
            duels.remove(duel);
        }
    }

    public static void addHit(Player p) {
        boxingHits.computeIfPresent(p.getUniqueId(), (k, v) -> v + 1);
    }

    public static int getHits(Player p) {
        return boxingHits.getOrDefault(p.getUniqueId(), 0);
    }

    public static Map<UUID, Integer> getBoxinghits() {
        return boxingHits;
    }

    public static List<Duel> getAllDuels() {
        return new ArrayList<>(duels);
    }
}