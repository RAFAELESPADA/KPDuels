package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class QueueManager {

    private static final Map<KitType, Player> queue = new HashMap<>();

    public static void join(Player p, KitType kit) {

        if (isInQueue(p)) {
            p.sendMessage("§cVocê já está em uma fila!");
            return;
        }

        if (!queue.containsKey(kit)) {
            queue.put(kit, p);
            p.sendMessage("§aEntrou na fila de §f" + kit.name());
            return;
        }

        Player other = queue.remove(kit);
        Duel duel = new Duel(other, p, ArenaManager.get(), kit);
        DuelManager.add(duel);
    }

    public static void cancel(Player p) {
        KitType toRemove = null;

        for (Map.Entry<KitType, Player> entry : queue.entrySet()) {
            if (entry.getValue().equals(p)) {
                toRemove = entry.getKey();
                break;
            }
        }

        if (toRemove != null) {
            queue.remove(toRemove);
            p.sendMessage("§aVocê saiu da fila!");
        } else {
            p.sendMessage("§cVocê não está em nenhuma fila!");
        }
    }

    public static boolean isInQueue(Player p, KitType kit) {
        return queue.containsKey(kit) && queue.get(kit).equals(p);
    }
    public static boolean isKitBusy(KitType kit) {
        return queue.containsKey(kit);
    }
    public static boolean isInQueue(Player p) {
        return queue.containsValue(p);
    }
}
