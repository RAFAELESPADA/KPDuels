package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SpawnListener implements Listener {

    private static final String DUELS_WORLD = "spawnd";
    private static final String SPAWN_WORLD = "spawnd";

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {

        Player p = e.getPlayer();
        String msg = e.getMessage().toLowerCase();

        // Detecta /spawn
        if (!msg.equals("/spawn") && !msg.startsWith("/spawn ")) return;

        e.setCancelled(true); // Cancela o comando original

        // ❌ Só funciona no mundo duels
        if (!p.getWorld().getName().equalsIgnoreCase(DUELS_WORLD)) {
            e.setCancelled(false);
            return;
        }

        // ❌ Bloquear em duelo
        if (DuelManager.isInDuel(p)) {
            p.sendMessage("§cVocê não pode usar /spawn durante um duelo!");
            return;
        }

        World spawn = Bukkit.getWorld(SPAWN_WORLD);

        if (spawn == null) {
            p.sendMessage("§cMundo de spawn não encontrado.");
            return;
        }

        // 🧹 Reset básico
        p.getInventory().clear();
        p.setHealth(20.0);
        p.setFoodLevel(20);

        // 📍 Teleportar
        p.teleport(spawn.getSpawnLocation());

        // 🎒 Itens do lobby
        LobbyItems.give(p);

        p.sendMessage("§aVocê voltou pro spawn do duels!");
    }
}

