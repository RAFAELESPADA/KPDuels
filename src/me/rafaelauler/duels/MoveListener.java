package me.rafaelauler.duels;


import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;


public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Duel duel = DuelManager.get(e.getPlayer());
        if (duel == null) return;
if (duel.getKit() == KitType.SUMO) {
    Block block = e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
            DuelManager.end(duel, duel.getOpponent(e.getPlayer()));
        }
    }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {

        Player p = e.getPlayer();

        if (!p.getWorld().getName().equalsIgnoreCase("duels")) return;

        if (DuelManager.isInDuel(p)) return; // 🔒 BLOQUEIA

        p.getInventory().clear();
        LobbyItems.give(p);
    }

@EventHandler
public void onMove2(PlayerMoveEvent e) {

    Duel duel = DuelManager.get(e.getPlayer());

    if (duel != null && duel.getState() == DuelState.STARTING) {
        if (e.getFrom().distance(e.getTo()) > 0) {
            e.setTo(e.getFrom());
        }
    }
}
}