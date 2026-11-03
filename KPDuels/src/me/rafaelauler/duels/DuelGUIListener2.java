package me.rafaelauler.duels;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DuelGUIListener2 implements Listener {

    /* ======================= GUI ======================= */
	private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    @EventHandler
    public void onClick(InventoryClickEvent e) {
    	 if (e.getView().getTitle().equals(StatsGUI.TITLE)) {
    		 e.setCancelled(true);
    		 return;
    	 }
        if (!e.getView().getTitle().equals(DuelGUI.TITLE)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        if (DuelManager.isInDuel(p)) {
            p.sendMessage("§cVocê já está em um duelo!");
            return;
        }
        if (ClickCooldown.inCooldown(p.getUniqueId())) {
            p.sendMessage("§cAguarde um instante...");
            return;
        }
        ClickCooldown.apply(p.getUniqueId());
        switch (e.getCurrentItem().getType()) {

            case IRON_CHESTPLATE:
                QueueManager.join(p, KitType.SUMO);

                p.closeInventory();
                break;

            case MUSHROOM_STEW:
                QueueManager.join(p, KitType.SOUP);

                p.closeInventory();
                break;

            case DIAMOND_SWORD:
                QueueManager.join(p, KitType.UHC);

                p.closeInventory();
                break;

            case GOLDEN_SWORD:
                QueueManager.join(p, KitType.BOXING);
                

                p.closeInventory();
                break;
            case COBBLESTONE:
                QueueManager.join(p, KitType.BUILD);
                

                p.closeInventory();
                break;

            case IRON_BARS:
                QueueManager.join(p, KitType.GLADIATOR);
                p.closeInventory();
                break;

            case BARRIER:
                QueueManager.leave(p);

                p.closeInventory();
                break;

            case BOOK:
                StatsGUI.open(p);
                break;

            default:
                return;
        }
    }

    /* ======================= MORTE ======================= */


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {

        Player dead = e.getEntity();

        if (dead.getWorld().getName().equalsIgnoreCase("Duels")) {
            DuelsCommand.game.add(dead.getName());
        }

        if (!DuelsCommand.game.contains(dead.getName())) {
            return;
        }

        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        Duel duel = DuelManager.get(dead);

        if (duel != null) {

            Player winner = duel.getOpponent(dead);

            if (winner != null && winner.isOnline()) {
            	DuelManager.end(winner, dead);
            	winner.sendMessage(ChatColor.GREEN + "Você venceu o duelo contra " + dead.getName());
            } else {
                duel.end(null, dead);
            }

            if (winner != null) {
                dead.sendMessage(ChatColor.RED + "Você perdeu o duelo contra " + winner.getName());
            } else {
                dead.sendMessage(ChatColor.RED + "Você perdeu o duelo.");
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {

        Player p = e.getPlayer();

        if (!DuelsCommand.game.contains(p.getName())) {
            return;
        }

        World world = Bukkit.getWorld("spawnd");

        if (world == null) {
            Bukkit.getLogger().warning(
                "Mundo spawnd não encontrado!"
            );
            return;
        }

        Location spawn = world.getSpawnLocation();

        // Define o destino no próprio processo de respawn
        e.setRespawnLocation(spawn);

        Bukkit.getScheduler().runTaskLater(
            DuelPlugin.getPlugin(DuelPlugin.class),
            () -> {

                if (!p.isOnline()) {
                    return;
                }

                // Limpeza do jogador
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.getInventory().setItemInOffHand(null);

                p.setGameMode(GameMode.ADVENTURE);
                p.setFireTicks(0);
                p.setFallDistance(0);
                p.setVelocity(new Vector(0, 0, 0));

                p.setAllowFlight(false);
                p.setFlying(false);
                p.setInvulnerable(false);

                // Itens do lobby
                LobbyItems.give(p);

                // Atualiza inventário do cliente
                p.updateInventory();

                p.sendMessage("TELEPORTADO PARA O SPAWN DO DUELS");

            },
            1L
        );
    }


    /* ======================= QUIT ======================= */


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        DuelManager.getBoxinghits().remove(uuid);
        QueueManager.leave(e.getPlayer());
        ClickCooldown.remove(uuid);
        ChallengeManager.cancelSelection(e.getPlayer());
        DuelsCommand.game.remove(e.getPlayer().getName());

        Duel duel = DuelManager.get(e.getPlayer());
        if (duel != null) {
            duel.handleQuit(e.getPlayer());
        }

        PlayerStats stats = PlayerStatsCache.get(uuid);
        if (stats != null && stats.isDirty()) {
            StatsSaveQueue.enqueue(stats);
        }
    }


    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {

        if (!event.getWorld().getName().equalsIgnoreCase("Duels")) return;
        if (!LOADED.compareAndSet(false, true)) return;

        DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ArenaManager.loadArenas();
            Bukkit.getLogger().info("[Duels] Arenas carregadas.");
        }, 1L);
    }


    /* ======================= INTERAÇÕES ======================= */

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {

        Player challenger = e.getPlayer();
        if (!(e.getRightClicked() instanceof Player)) return;

        ItemStack item = challenger.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        if (!DuelsCommand.game.contains(challenger.getName())) return;

        e.setCancelled(true);

        UUID uuid = challenger.getUniqueId();
        if (!TickGuard.allow(uuid) || !ClickGuard.allow(uuid)) return;

        Player target = (Player) e.getRightClicked();
        if (challenger.equals(target)) {
            challenger.sendMessage("§cVocê não pode desafiar a si mesmo.");
            return;
        }

        if (!challenger.getWorld().getName().equalsIgnoreCase("spawnd")
                || !target.getWorld().getName().equalsIgnoreCase("spawnd")
                || !DuelsCommand.game.contains(target.getName())) {
            challenger.sendMessage("§cO jogador precisa estar no lobby de Duels.");
            return;
        }

        if (DuelManager.isInDuel(challenger) || DuelManager.isInDuel(target)) {
            challenger.sendMessage("§cVocê ou esse jogador já está em um duelo.");
            return;
        }
        if (ChallengeManager.hasPendingChallenge(target)) {
            challenger.sendMessage("§cEsse jogador já possui um desafio pendente.");
            return;
        }
        if (ChallengeManager.inCooldown(challenger)) {
            challenger.sendMessage("§cAguarde §f" + ChallengeManager.getCooldown(challenger)
                    + "s §cpara desafiar novamente.");
            return;
        }

        // Fluxo antigo continua disponível: primeiro seleciona o kit e depois clica no alvo.
        if (ChallengeStateManager.is(uuid, ChallengeState.SELECTING_TARGET)
                && ChallengeManager.hasSelectedKit(challenger)) {
            ChallengeStateManager.set(uuid, ChallengeState.PENDING_RESPONSE);
            if (!ChallengeManager.challenge(challenger, target)) {
                ChallengeStateManager.set(uuid, ChallengeState.SELECTING_TARGET);
            }
            return;
        }

        // Novo fluxo: clicar no jogador abre a seleção de kit já vinculada a esse alvo.
        ChallengeManager.cancelSelection(challenger);
        ChallengeManager.setPendingTarget(challenger, target);
        ChallengeStateManager.set(uuid, ChallengeState.SELECTING_KIT);
        ChallengeKitGUI.open(challenger);
        challenger.sendMessage("§eSelecione o kit para desafiar §f" + target.getName() + "§e.");
    }


    @EventHandler
    public void onQuit2(PlayerQuitEvent e) {
        TickGuard.clear(e.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onKitSelect(InventoryClickEvent e) {

        if (!e.getView().getTitle().equals(ChallengeKitGUI.TITLE)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (!p.getOpenInventory().getTitle().equals(ChallengeKitGUI.TITLE)) {
            // Proteção contra cliques de inventário falsificados.
            return;
        }
        if (!DuelsCommand.game.contains(p.getName())
                || !p.getWorld().getName().equalsIgnoreCase("spawnd")
                || DuelManager.isInDuel(p)) {
            ChallengeManager.cancelSelection(p);
            p.closeInventory();
            return;
        }
        if (!ClickGuard.allow(p.getUniqueId())) return;

        if (e.getCurrentItem() == null) return;

        KitType kit = null;

        switch (e.getCurrentItem().getType()) {
            case DIAMOND_SWORD: kit = KitType.UHC; break;
            case MUSHROOM_STEW: kit = KitType.SOUP; break;
            case IRON_CHESTPLATE: kit = KitType.SUMO; break;
            case GOLDEN_SWORD: kit = KitType.BOXING; break;
            case COBBLESTONE: kit = KitType.BUILD; break;
            case IRON_BARS: kit = KitType.GLADIATOR; break;
            default: return;
        }

        ChallengeManager.selectKit(p, kit);

        // Se o menu foi aberto ao clicar em um jogador, envia o desafio imediatamente.
        if (ChallengeManager.hasPendingTarget(p)) {
            Player target = ChallengeManager.getPendingTarget(p);

            if (target == null || !target.isOnline()) {
                p.sendMessage("§cO jogador que você selecionou não está mais online.");
                ChallengeManager.cancelSelection(p);
                p.closeInventory();
                return;
            }
            if (!target.getWorld().getName().equalsIgnoreCase("spawnd")
                    || !DuelsCommand.game.contains(target.getName())
                    || DuelManager.isInDuel(target)) {
                p.sendMessage("§cEsse jogador não está mais disponível para um desafio.");
                ChallengeManager.cancelSelection(p);
                p.closeInventory();
                return;
            }

            if (!ChallengeManager.challenge(p, target)) {
                ChallengeManager.cancelSelection(p);
            }
            // Fecha somente depois do envio (ou cancelamento) para não apagar o alvo no InventoryCloseEvent.
            p.closeInventory();
            return;
        }

        // Fluxo alternativo: selecionar o kit primeiro e depois clicar no jogador.
        ChallengeStateManager.set(p.getUniqueId(), ChallengeState.SELECTING_TARGET);
        p.sendMessage("§7Agora clique no jogador que deseja desafiar.");
        p.closeInventory();
    }



    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
if (!DuelsCommand.game.contains(e.getPlayer().getName())) {
	return;
}
        if (e.getItem() == null) return;
        if (!TickGuard.allow(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        Player p = e.getPlayer();
        Material type = e.getItem().getType();

        if (type == Material.DIAMOND_SWORD) {
            e.setCancelled(true);
            if (!DuelManager.isInDuel(p)) {
                DuelGUI.open(p);
            }
            return;
        }
        if (type == Material.BLAZE_ROD) {
            e.setCancelled(true);
            if (!DuelManager.isInDuel(p)) {
                ChallengeKitGUI.open(p);
                ChallengeStateManager.set(p.getUniqueId(), ChallengeState.SELECTING_KIT);

            }
            return;
        }

        if (type == Material.BARRIER) {
            e.setCancelled(true);
            Bukkit.dispatchCommand(p, "dm open lobby");
            return;
        }

        if (type == Material.ENDER_PEARL) {
            e.setCancelled(true);

            ChallengeManager.clear(p);

            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.teleport(Bukkit.getWorld("spawnd").getSpawnLocation());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            LobbyItems.give(p);
        }
    }
    

    /* ======================= DROP ======================= */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        PlayerStatsCache.touch(uuid);
        if (PlayerStatsCache.contains(uuid)) return;
       
        DuelPlugin.getMy().loadStatsAsync(uuid, stats -> {
            // cache já foi populado
        });
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        Player p = e.getPlayer();

        // Proteção do spawn
        if (p.getWorld().getName().equalsIgnoreCase("spawnd")) {

            Material type = e.getItemDrop().getItemStack().getType();

            if (type == Material.BLAZE_ROD ||
                type == Material.ENDER_PEARL ||
                type == Material.DIAMOND_SWORD ||
                type == Material.BARRIER) {

                e.setCancelled(true);
                return;
            }
        }

        // Proteção durante os duelos
        if (DuelManager.isInDuel(p)) {

            ItemStack item = e.getItemDrop().getItemStack();

            if (item.getType() != Material.BOWL) {
                e.setCancelled(true);
            }
        }
    }

    /* ======================= WORLD ======================= */

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (e.getFrom().getName().equalsIgnoreCase("spawnd")) {
            QueueManager.leave(e.getPlayer());
            ChallengeManager.cancelSelection(e.getPlayer());
            DuelsCommand.game.remove(e.getPlayer().getName());
        }
    }

    /* ======================= GUI CLOSE ======================= */

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(DuelGUI.TITLE)) {
            DuelGUI.stopTask((Player) e.getPlayer());
        }
        if (e.getView().getTitle().equals(ChallengeKitGUI.TITLE)
                && e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            // Fechar/cancelar a seleção não pode deixar o alvo preso no estado do jogador.
            if (ChallengeManager.hasPendingTarget(p)) {
                ChallengeManager.cancelSelection(p);
            } else if (ChallengeStateManager.is(p.getUniqueId(), ChallengeState.SELECTING_KIT)) {
                ChallengeStateManager.clear(p.getUniqueId());
            }
        }
    }
}
