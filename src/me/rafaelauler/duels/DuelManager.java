package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.wavemc.core.bukkit.api.HelixActionBar;

public class DuelManager {

    private static final List<Duel> duels = new ArrayList<>();
    private static final Map<UUID, Integer> boxingHits = new HashMap<>();

    private static final int BOXING_MAX_HITS = 100;

    public static void add(Duel duel) {
        duels.add(duel);

        if (duel.getKit() == KitType.BOXING) {
            for (Player p : duel.getPlayers()) {
                boxingHits.put(p.getUniqueId(), 0);
            }
        }
    }

    public static Duel get(Player p) {
        for (Duel d : duels) {
            if (d.hasPlayer(p)) return d;
        }
        return null;
    }
    public static List<Duel> getAllDuels() {
        return new ArrayList<>(duels);
    }
    public static void end(Duel duel, Player winner) {

        if (!duels.contains(duel)) return;

        duel.end(winner);

        for (Player p : duel.getPlayers()) {
            boxingHits.remove(p.getUniqueId());
        }

        duels.remove(duel);
    }

    /* ===========================
       BOXING SYSTEM
       =========================== */

    public static void addHit(Player damager, Player victim) {

        Duel duel = get(damager);
        if (duel == null) return;

        if (duel.getState() != DuelState.FIGHTING) return;
        if (duel.getKit() != KitType.BOXING) return;
        if (!duel.hasPlayer(victim)) return;

        int hits = boxingHits.getOrDefault(damager.getUniqueId(), 0) + 1;
        boxingHits.put(damager.getUniqueId(), hits);

        HelixActionBar.sendActionBar(
                damager,
                "§eHits: §a" + hits + "§7/§c" + BOXING_MAX_HITS,
                40
        );

        ParticleUtil.hit(victim);

        if (hits >= BOXING_MAX_HITS) {
            end(duel, damager);
        }
    }

    public static int getHits(Player p) {
        return boxingHits.getOrDefault(p.getUniqueId(), 0);
    }

    public static boolean isInDuel(Player p) {
        return get(p) != null;
    }
}
