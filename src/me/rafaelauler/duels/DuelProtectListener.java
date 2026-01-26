package me.rafaelauler.duels;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DuelProtectListener implements Listener {

    // Bloquear comandos enquanto estiver em duelo
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (DuelManager.isInDuel(p)) {
            String msg = e.getMessage().toLowerCase();
            // Permitir apenas comandos internos do duelo, ex: /duel /accept /deny
            if (!msg.startsWith("/duel") && !msg.startsWith("/accept") && !msg.startsWith("/deny")) {
                e.setCancelled(true);
                p.sendMessage("§cVocê não pode usar comandos enquanto estiver em duelo!");
            }
        }
    }

    // Bloquear teleport externo
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (DuelManager.isInDuel(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cVocê não pode teleportar durante um duelo!");
        }
    }

    // Bloquear teleports via plugin
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (DuelManager.isInDuel(p)) {
            Duel duel = DuelManager.get(p);
            if (duel.getState() != DuelState.FIGHTING) {
                // Impede sair da arena antes do duelo começar
                Location from = e.getFrom();
                Location to = e.getTo();
                if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    // Bloquear colocar ou quebrar blocos
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (DuelManager.isInDuel(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (DuelManager.isInDuel(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    // Bloquear abrir inventário externo
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (DuelManager.isInDuel(p)) {
                e.setCancelled(true);
            }
        }
    }

    // Bloquear morrer fora do duelo
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (DuelManager.isInDuel(p)) {
            e.setDeathMessage(null);
        }
    }

    // Bloquear danos externos (PvP fora do duelo)
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (DuelManager.isInDuel(p)) {
                Duel duel = DuelManager.get(p);
                if (duel.getState() != DuelState.FIGHTING) {
                    e.setCancelled(true);
                }
            }
        }
    }
}

