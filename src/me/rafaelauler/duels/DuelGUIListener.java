package me.rafaelauler.duels;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DuelGUIListener implements Listener {

    /* ======================= GUI ======================= */
	private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    @EventHandler
    public void onClick(InventoryClickEvent e) {

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

        duel.clearPlacedBlocks();

        LobbyItems.give(dead);
        
        Bukkit.getScheduler().runTaskLater(DuelPlugin.getPlugin(DuelPlugin.class), () -> {

        	  if (dead != null) LobbyItems.give(dead);


        }, 50L);
        }}
    



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
        UUID uuid = e.getPlayer().getUniqueId();

        DuelManager.getBoxinghits().remove(uuid);
        QueueManager.leave(e.getPlayer());
        ClickCooldown.remove(uuid);
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

        if (!event.getWorld().getName().equalsIgnoreCase("duels")) return;
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
        if (!DuelsCommand.game.contains(challenger.getName())) return;

        UUID uuid = challenger.getUniqueId();

        // 1️⃣ Packet spam / macro
        if (!TickGuard.allow(uuid)) {
            e.setCancelled(true);
            return;
        }

        // 2️⃣ Double click
        if (!ClickGuard.allow(uuid)) {
            e.setCancelled(true);
            return;
        }

        // 3️⃣ State validation (packet injection)
        if (!ChallengeStateManager.is(uuid, ChallengeState.SELECTING_TARGET)) {
            e.setCancelled(true);
            return;
        }

        // 4️⃣ Kit validation
        if (!ChallengeManager.hasSelectedKit(challenger)) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        Player target = (Player) e.getRightClicked();
        ChallengeStateManager.set(uuid, ChallengeState.PENDING_RESPONSE);
        ChallengeManager.challenge(challenger, target);
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
    // 🚨 packet fake
    return;
}
        if (!ClickGuard.allow(p.getUniqueId())) return;

        if (e.getCurrentItem() == null) return;

        KitType kit = null;

        switch (e.getCurrentItem().getType()) {
            case DIAMOND_SWORD: kit = KitType.UHC; break;
            case MUSHROOM_SOUP: kit = KitType.SOUP; break;
            case IRON_CHESTPLATE: kit = KitType.SUMO; break;
            case GOLD_SWORD: kit = KitType.BOXING; break;
            case COBBLESTONE: kit = KitType.BUILD; break;
            default: return;
        }

        ChallengeManager.selectKit(p, kit);
        ChallengeStateManager.set(p.getUniqueId(), ChallengeState.SELECTING_TARGET);
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
            Bukkit.dispatchCommand(p, "lobby");
            return;
        }

        if (type == Material.ENDER_PEARL) {
            e.setCancelled(true);

            ChallengeManager.clear(p);

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
