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
            String cmd = msg.split(" ")[0];

            if (!cmd.equals("/duel") &&
                !cmd.equals("/accept") &&
                !cmd.equals("/deny")) {

                e.setCancelled(true);
                p.sendMessage("§cVocê não pode usar comandos enquanto estiver em duelo!");
            }
        }
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!e.getView().getTitle().equals(StatsGUI.TITLE)) return;
        if (e.getView().getTopInventory() == null) return;
        if (!e.getView().getTopInventory().getTitle().equals(StatsGUI.TITLE)) return;
        e.setCancelled(true);
    }
    // Bloquear teleport externo
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {

        Player p = e.getPlayer();

        if (!DuelManager.isInDuel(p)) return;

        switch (e.getCause()) {

            case COMMAND:
            case ENDER_PEARL:
            case SPECTATE:
                e.setCancelled(true);
                p.sendMessage("§cVocê não pode teleportar durante um duelo!");
                break;

            default:
                // PLUGIN, RESPAWN, UNKNOWN, NETHER_PORTAL, etc → libera
                break;
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
                if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    // Bloquear colocar ou quebrar blocos
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!DuelManager.isInDuel(p)) return;

        Duel duel = DuelManager.get(p);

        if (duel.getKit() != KitType.BUILD) {
            e.setCancelled(true);
            return;
        }

        if (!duel.isInsideArena(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        duel.registerBlock(e.getBlock());
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!DuelManager.isInDuel(p)) return;

        Duel duel = DuelManager.get(p);

        if (duel.getKit() != KitType.BUILD) {
            e.setCancelled(true);
            return;
        }

        if (!duel.isInsideArena(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (!duel.containsBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onDa2mage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        Player p = (Player) e.getEntity();
        if (!DuelManager.isInDuel(p)) return;

        Duel duel = DuelManager.get(p);

        if (!duel.isInsideArena(p.getLocation())) {
            e.setCancelled(true);
        }
    }



    // Bloquear abrir inventário externo


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

