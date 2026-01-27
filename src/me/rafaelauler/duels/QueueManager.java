package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class QueueManager {

    private static final Map<KitType, Queue<Player>> queues = new HashMap<>();

    public static void join(Player player, KitType kit) {

        // Já está em duelo
        if (DuelManager.isInDuel(player)) {
            player.sendMessage("§cVocê já está em um duelo!");
            return;
        }

        // Já está em alguma fila
        if (isInQueue(player)) {
            player.sendMessage("§cVocê já está em uma fila!");
            return;
        }

        queues.putIfAbsent(kit, new LinkedList<>());
        Queue<Player> queue = queues.get(kit);

        // Fila vazia → entra e aguarda
        if (queue.isEmpty()) {
            queue.add(player);
            player.sendMessage("§aVocê entrou na fila do kit §f" + kit.name());
            QueueActionBar.start(player, kit);
            return;
        }

        // Já existe alguém aguardando
        Player opponent = queue.poll();

        while (opponent != null && !opponent.isOnline()) {
            opponent = queue.poll();
        }

        if (opponent == null) {
            queue.add(player);
            QueueActionBar.start(player, kit);
            return;
        }

        // Fechar inventários
        player.closeInventory();
        opponent.closeInventory();

        // Busca arena compatível com o kit
        Arena arena = ArenaManager.getFreeArena(kit);
        if (arena == null) {
            player.sendMessage("§cNenhuma arena disponível.");
            opponent.sendMessage("§cNenhuma arena disponível.");

            queue.add(opponent);
            QueueActionBar.start(opponent, kit);
            return;
        }

        // Para a action bar do oponente
        QueueActionBar.stop(opponent);

        // Cria o duelo
        Duel duel = new Duel(opponent, player, arena, kit);

        // Adiciona ANTES de start para que listeners reconheçam os jogadores
        DuelManager.add(duel);

        if (!duel.start()) {
            // Se falhar ao iniciar, libera arena e remove duelo
            DuelManager.remove(opponent);
            DuelManager.remove(player);
            ArenaManager.release(arena);

            // Devolve itens/lobby aos jogadores
            LobbyItems.give(opponent);
            LobbyItems.give(player);
            opponent.teleport(Bukkit.getWorld("duels").getSpawnLocation());
            player.teleport(Bukkit.getWorld("duels").getSpawnLocation());

            return;
        }

        // Mensagens de duelo iniciado
        player.sendMessage("§aDuelo encontrado!");
        opponent.sendMessage("§aDuelo encontrado!");
    }


    public static void leave(Player player) {
        if (!isInQueue(player)) return;

        queues.values().forEach(queue -> queue.remove(player));
        QueueActionBar.stop(player);
        player.sendMessage("§cVocê saiu da fila.");
    }

    // =====================================================
    // 🔍 MÉTODOS USADOS NA GUI
    // =====================================================

    public static boolean isInQueue(Player player) {
        return queues.values().stream().anyMatch(q -> q.contains(player));
    }

    public static boolean isInQueue(Player player, KitType kit) {
        return queues.containsKey(kit) && queues.get(kit).contains(player);
    }

    public static boolean isKitBusy(KitType kit) {
        return queues.containsKey(kit) && !queues.get(kit).isEmpty();
    }

    public static int getQueueSize(KitType kit) {
        return queues.getOrDefault(kit, new LinkedList<>()).size();
    }
}
