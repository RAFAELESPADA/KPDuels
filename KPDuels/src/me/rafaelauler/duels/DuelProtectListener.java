package me.rafaelauler.duels;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
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
                !cmd.equals("/deny") &&
                !cmd.equals("/tell") &&
                !cmd.equals("/ban") &&
                !cmd.equals("/tempban") &&
                !cmd.equals("/kick") &&
                !cmd.equals("/duels")) {

                e.setCancelled(true);
                p.sendMessage("§cVocê não pode usar comandos enquanto estiver em duelo!");
            }
        }
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!e.getView().getTitle().equals(StatsGUI.TITLE)) return;
        if (e.getView().getTopInventory() == null) return;
        if (!e.getView().getTopInventory().getViewers().equals(StatsGUI.TITLE)) return;
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

        if (duel.getKit() != KitType.BUILD && duel.getKit() != KitType.GLADIATOR) {
            e.setCancelled(true);
            return;
        }

        if (!duel.isInsideArena(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        duel.registerBlock(e.getBlock(), e.getBlockReplacedState(), p);
    }
    /**
     * Baldes não disparam BlockPlaceEvent. Registra o bloco substituído para
     * que a água/lava colocada pelos jogadores seja removida ao encerrar o Glad.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGladiatorBucketEmpty(PlayerBucketEmptyEvent e) {
        Player player = e.getPlayer();
        Duel duel = DuelManager.get(player);
        if (duel == null || duel.getKit() != KitType.GLADIATOR) {
            return;
        }

        Material bucket = e.getBucket();
        if (bucket != Material.WATER_BUCKET && bucket != Material.LAVA_BUCKET) {
            return;
        }

        Block target = e.getBlockClicked().getRelative(e.getBlockFace());
        if (!duel.isInsideArena(target.getLocation())) {
            e.setCancelled(true);
            return;
        }

        // Captura o estado do alvo antes que ele vire água ou lava.
        duel.registerTemporaryBlock(target);
    }

    /**
     * Registra também os blocos alcançados pelo fluxo. Assim, até líquidos que
     * se espalharem pela jaula são restaurados junto com a arena.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGladiatorFluidFlow(BlockFromToEvent e) {
        Material fluid = e.getBlock().getType();
        if (fluid != Material.WATER && fluid != Material.LAVA) {
            return;
        }

        Duel duel = DuelManager.getGladiatorAt(e.getBlock().getLocation());
        if (duel == null) {
            return;
        }

        Block destination = e.getToBlock();
        if (!duel.isInsideArena(destination.getLocation())) {
            // Impede que um líquido do duelo escape e altere o mundo externo.
            e.setCancelled(true);
            return;
        }

        // Salva o estado anterior do destino antes de o líquido ocupar o bloco.
        duel.registerTemporaryBlock(destination);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!DuelManager.isInDuel(p)) return;

        Duel duel = DuelManager.get(p);

        if (duel.getKit() != KitType.BUILD && duel.getKit() != KitType.GLADIATOR) {
            e.setCancelled(true);
            return;
        }

        if (!duel.isInsideArena(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        // Gladiator: só permite quebrar blocos colocados pelo próprio jogador.
        // A jaula (vidro e piso) e os blocos do adversário permanecem protegidos.
        if (duel.getKit() == KitType.GLADIATOR) {
            if (!duel.wasPlacedBy(e.getBlock(), p)) {
                e.setCancelled(true);
            }
            return;
        }

        if (!duel.containsBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }
    // Remove o dono do registro somente depois que a quebra realmente foi concluída.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulGladiatorBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!DuelManager.isInDuel(p)) {
            return;
        }

        Duel duel = DuelManager.get(p);
        if (duel.getKit() == KitType.GLADIATOR && duel.wasPlacedBy(e.getBlock(), p)) {
            duel.forgetPlacedBlockOwner(e.getBlock());
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

