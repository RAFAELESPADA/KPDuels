package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    // ====================================
    // BLOQUEIA PLAYER STARTING / RESTRINGE MOVIMENTO
    // ====================================
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Duel duel = DuelManager.get(p);
        if (duel == null) return;

        // 🔒 BLOQUEIA QUANDO O DUEL ESTÁ STARTING
        if (duel.getState() == DuelState.STARTING) {
            if (!e.getFrom().equals(e.getTo())) {
                e.setTo(e.getFrom());
            }
            return;
        }

        // ====================================
        // SUMO: verifica se caiu na água
        // ====================================
        if (duel.getKit() == KitType.SUMO) {
            Block below = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (below.getType() == Material.WATER || below.getType() == Material.STATIONARY_WATER) {
                // o oponente vence
                DuelManager.end(duel.getOpponent(p));
               p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
                LobbyItems.give(p);
                return;
            }
        }

        // ====================================
        // Checa distância máxima (fuga ou sair do duelo)
        // ====================================
        Player opponent = duel.getOpponent(p);
        if (opponent != null && p.getLocation().distance(opponent.getLocation()) > 157) {
            DuelManager.end(opponent);
        }
    }

    // ====================================
    // QUANDO MUDAR DE WORLD
    // ====================================
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();

        // se estava em duelo, termina
        Duel duel = DuelManager.get(p);
        if (duel != null) {
            DuelManager.end(duel.getOpponent(p));
        }

        // limpa inventário e dá itens de lobby se não estiver mais em duelo
        if (!DuelManager.isInDuel(p)) {
            p.getInventory().clear();
            LobbyItems.give(p);
        }
    }
}
