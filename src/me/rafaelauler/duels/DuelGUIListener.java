package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DuelGUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!e.getView().getTitle().equals(DuelGUI.TITLE)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {

            case IRON_CHESTPLATE:
                QueueManager.join(p, KitType.SUMO);
                break;
            case MUSHROOM_SOUP:
                QueueManager.join(p, KitType.SOUP);
                break;
            case DIAMOND_SWORD:
                QueueManager.join(p, KitType.UHC);
                break;

            case GOLD_SWORD:
                QueueManager.join(p, KitType.BOXING);
                break;

            case BARRIER:
                QueueManager.cancel(p);
                break;

            default:
                return;
        }

        p.closeInventory();
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent e) {

        if (!(e.getRightClicked() instanceof Player)) return;

        Player challenger = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        if (!ChallengeManager.isSelecting(challenger)) return;
        if (e.getPlayer().getItemInHand().getType() == Material.BLAZE_ROD) {
            ChallengeManager.startSelecting(e.getPlayer());
        }
        e.setCancelled(true);
        ChallengeChat.sendChallenge(challenger, target);
        ChallengeManager.challenge(challenger, target);
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (e.getItem() == null) return;

        Player p = e.getPlayer();
        Material type = e.getItem().getType();

        if (type == Material.DIAMOND_SWORD) {
        	 if (DuelManager.isInDuel(p)) {
        	        e.setCancelled(true);
        	        return;
        	    }
            e.setCancelled(true);
            ChallengeManager.startSelecting(p);
            return;
        }

        if (type == Material.ENDER_PEARL) {
        	 if (DuelManager.isInDuel(p)) {
        	        e.setCancelled(true);
        	        return;
        	    }
            e.setCancelled(true);
            ChallengeManager.cancelSelecting(p);
            // 🧹 Reset básico
            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);

            // 📍 Teleportar
            p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
p.playSound(p.getLocation(), Sound.valueOf("LEVEL_UP"), 10f, 10f);
            // 🎒 Itens do lobby
            LobbyItems.give(p);
        }
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        if (DuelManager.isInDuel(e.getPlayer())) {
        	if (e.getItemDrop().getItemStack() != new ItemStack(Material.MUSHROOM_SOUP)) {
            e.setCancelled(true);
        }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(DuelGUI.TITLE)) {
            DuelGUI.stopTask((Player) e.getPlayer());
        }
    }
}
