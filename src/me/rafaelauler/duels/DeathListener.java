package me.rafaelauler.duels;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        Player dead = e.getEntity();
        Duel duel = DuelManager.get(dead);

        // Não estava em duelo
        if (duel == null) return;

        // Cancela mensagens e drops
        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        Player killer = dead.getKiller();

        // Se morreu sem killer (queda, fogo etc)
        if (killer == null || !duel.hasPlayer(killer)) {
            killer = duel.getOpponent(dead);
        }

        DuelManager.end(duel, killer);
    }

@EventHandler
public void onRespawn(PlayerRespawnEvent e) {
    e.setRespawnLocation(e.getPlayer().getWorld().getSpawnLocation());
}

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        Player player = e.getPlayer();
        Duel duel = DuelManager.get(player);

        // Não estava em duelo
        if (duel == null) return;

        Player winner = duel.getOpponent(player);

        DuelManager.end(duel, winner);
    }
}

