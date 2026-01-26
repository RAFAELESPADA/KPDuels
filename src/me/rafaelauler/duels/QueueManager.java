package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

public class QueueManager {

    private static final Map<KitType, List<Player>> queues = new HashMap<>();

    public static void join(Player p, KitType kit) {
        queues.putIfAbsent(kit, new ArrayList<>());
        List<Player> list = queues.get(kit);

        if (list.contains(p)) {
            p.sendMessage("§cVocê já está na fila!");
            return;
        }

        if (list.isEmpty()) {
            list.add(p);
            p.sendMessage("§aVocê entrou na fila de §f" + kit.name());
            return;
        }

        Player other = list.remove(0);
        Duel duel = new Duel(other, p, ArenaManager.get(), kit);
        DuelManager.add(duel);
    }

    public static void cancel(Player p) {
        queues.values().forEach(list -> list.remove(p));
        p.sendMessage("§aVocê saiu da fila!");
    }

    public static boolean isInQueue(Player p, KitType kit) {
        return queues.getOrDefault(kit, Collections.emptyList()).contains(p);
    }
    public static boolean isKitBusy(KitType kit) {
        List<Player> list = queues.getOrDefault(kit, Collections.emptyList());
        return !list.isEmpty();
    }
    public static int getQueueSize(KitType kit) {
        return queues.getOrDefault(kit, Collections.emptyList()).size();
    }

    public static boolean isInDuel(Player p) {
        return DuelManager.isInDuel(p);
    }
}
