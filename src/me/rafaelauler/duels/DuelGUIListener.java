package me.rafaelauler.duels;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DuelGUIListener implements Listener {

    /* ======================= GUI ======================= */
	  private boolean loaded = false;
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!e.getView().getTitle().equals(DuelGUI.TITLE)) return;
        e.setCancelled(true);

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

            case MUSHROOM_SOUP:
                QueueManager.join(p, KitType.SOUP);

                p.closeInventory();
                break;

            case DIAMOND_SWORD:
                QueueManager.join(p, KitType.UHC);

                p.closeInventory();
                break;

            case GOLD_SWORD:
                QueueManager.join(p, KitType.BOXING);
                

                p.closeInventory();
                break;
            case COBBLESTONE:
                QueueManager.join(p, KitType.BUILD);
                

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

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        Player dead = e.getEntity();
        Duel duel = DuelManager.get(dead);

        if (DuelsCommand.game.contains(dead.getName())) {
        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        if (duel == null) return;

        Player winner = duel.getOpponent(dead);
        DuelManager.end(winner); // passe o duelo explicitamente
        if (DuelProtectListener.BLOCK.keySet() != null) {
        	for (Block b : DuelProtectListener.BLOCK.keySet()) {
        		b.setType(Material.AIR);
        	}
        LobbyItems.give(dead);
        
        Bukkit.getScheduler().runTaskLater(DuelPlugin.getPlugin(DuelPlugin.class), () -> {

        	  if (dead != null) LobbyItems.give(dead);


        }, 50L);
        }}
    }



    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();

        if (DuelsCommand.game.contains(p.getName())) {
        new BukkitRunnable() {
            @Override
            public void run() {
                p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
                LobbyItems.give(p);
                if (!DuelsCommand.game.contains(p.getName())) {
                	DuelsCommand.game.add(p.getName());
                }
            }
        }.runTaskLater(DuelPlugin.getPlugin(DuelPlugin.class), 40L);
    }
    }

    /* ======================= QUIT ======================= */


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // Limpeza geral
        DuelManager.getBoxinghits().remove(uuid);
        QueueManager.leave(p);
        ClickCooldown.remove(uuid);
        DuelsCommand.game.remove(p.getName());

        // Encerra duelo se existir
        Duel duel = DuelManager.get(p);
        if (duel != null) {
            DuelManager.forceEnd(p);
        }

        // 🔥 Salva stats async
        Bukkit.getScheduler().runTaskAsynchronously(
            DuelPlugin.getInstance(), () -> {

                PlayerStats stats = PlayerStatsCache.get(uuid);

                if (stats == null) {
                    stats = DuelPlugin.getInstance()
                            .getMySQL()
                            .getStats(uuid);
                }

                if (stats != null) {
                    DuelPlugin.getInstance()
                            .getMySQL()
                            .saveStats(stats);
                }
                if (stats != null) {
                    Bukkit.getScheduler().runTaskAsynchronously(
                        DuelPlugin.getInstance(),
                        new StatsSaveTask(stats)
                    );
                    if (stats != null) {
                        StatsSaveQueue.enqueue(stats);
                        StatsSaveService.enqueue(stats);
                    }
                PlayerStatsCache.remove(uuid);
            }
            }
        );
    }
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        onQuit(new PlayerQuitEvent(e.getPlayer(), e.getLeaveMessage()));
    }


    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
    	if (!ArenaManager.getArenas().isEmpty()) return;

        // Evita carregar múltiplas vezes
        if (loaded) return;

        if (!event.getWorld().getName().equalsIgnoreCase("duels")) return;
        DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);

        // Espera 1 tick para garantir registro completo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            ArenaManager.loadArenas();
            loaded = true;

            Bukkit.getLogger().info("[Duels] Arenas carregadas após WorldLoadEvent.");

        }, 1L);
    }

    /* ======================= INTERAÇÕES ======================= */

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {

        Player challenger = e.getPlayer();

        if (!DuelsCommand.game.contains(challenger.getName())) return;
        if (!ChallengeManager.isSelecting(challenger)) return;
        if (!(e.getRightClicked() instanceof Player)) return;

        if (DuelManager.isInDuel(challenger))  return;
        e.setCancelled(true);

        Player target = (Player) e.getRightClicked();
        ChallengeManager.challenge(challenger, target);
        ChallengeChat.sendChallenge(challenger, target, KitType.SOUP);
    }
    

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
if (!DuelsCommand.game.contains(e.getPlayer().getName())) {
	return;
}
        if (e.getItem() == null) return;

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
                ChallengeManager.startSelecting(p);
            }
            return;
        }

        if (type == Material.BARRIER) {
            e.setCancelled(true);
            Bukkit.dispatchCommand(p, "lobby");
            return;
        }

        if (type == Material.ENDER_PEARL) {
            e.setCancelled(true);

            ChallengeManager.cancelSelecting(p);

            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
            p.playSound(p.getLocation(), Sound.valueOf("LEVEL_UP"), 1f, 1f);

            LobbyItems.give(p);
        }
    }
    

    /* ======================= DROP ======================= */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

       
        Bukkit.getScheduler().runTaskAsynchronously(
            DuelPlugin.getInstance(),
            () -> {
                PlayerStats stats = DuelPlugin.getInstance()
                    .getMySQL()
                    .getStats(p.getUniqueId());

                StatsCache.put(stats);
            }
        );
        PlayerStats win = PlayerStatsCache.get(p.getUniqueId());

        StatsSaveQueue.enqueue(win);
    }
    @EventHandler
    public void onJoin2(PlayerJoinEvent e) {
        var player = e.getPlayer();

        // 🔥 Evita recarregar se já estiver no cache
        if (PlayerStatsCache.get(player.getUniqueId()) != null) {
            return;
        }

        // 🔥 Carrega ASYNC
        Bukkit.getScheduler().runTaskAsynchronously(
            DuelPlugin.getInstance(),
            () -> loadStats(player)
        );
    }

    private void loadStats(org.bukkit.entity.Player p) {
        PlayerStats stats = DuelPlugin.getMy().getStats(p.getUniqueId());

        if (stats == null) {
            stats = new PlayerStats(p.getUniqueId(), 0, 0, 0);
        }
        else {
        stats = new PlayerStats(p.getUniqueId(), stats.getWins(), stats.getLosses(), stats.getWinstreak());
        }
        // 🔥 Coloca no cache
        PlayerStatsCache.put(stats);

    
    
}
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        Player p = e.getPlayer();

        if (!DuelManager.isInDuel(p)) return;

        ItemStack item = e.getItemDrop().getItemStack();

        if (item.getType() != Material.BOWL) {
            e.setCancelled(true);
        }
    }

    /* ======================= WORLD ======================= */

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (e.getFrom().getName().equalsIgnoreCase("duels")) {
            QueueManager.leave(e.getPlayer());
            DuelsCommand.game.remove(e.getPlayer().getName());
        }
    }

    /* ======================= GUI CLOSE ======================= */

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(DuelGUI.TITLE)) {
            DuelGUI.stopTask((Player) e.getPlayer());
        }
    }
}
