package me.rafaelauler.duels;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SoupListener implements Listener {

    private static final double HEALTH_HEAL = 7.0; // 3.5 corações
    private static final int FOOD_HEAL = 6;        // stamina

    @EventHandler
    public void onSoup(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        if (!DuelManager.isInDuel(p)) return;

        Duel duel = DuelManager.get(p);
        if (duel.getKit() != KitType.SOUP) return;

        if (p.getItemInHand() == null) return;
        if (p.getItemInHand().getType() != Material.MUSHROOM_SOUP) return;

        // Se já estiver full, não usa
        if (p.getHealth() >= 20.0 && p.getFoodLevel() >= 20) return;

        e.setCancelled(true);

        /* ================= VIDA ================= */
        if (p.getHealth() < 20.0) {
            p.setHealth(Math.min(20.0, p.getHealth() + HEALTH_HEAL));
        }

        /* ================= STAMINA ================= */
        if (p.getFoodLevel() < 20) {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + FOOD_HEAL));
        }

        // Remove sopa
        p.setItemInHand(new ItemStack(Material.BOWL));
    }

@EventHandler
public void onFood(FoodLevelChangeEvent e) {

    if (!(e.getEntity() instanceof Player)) return;
    Player p = (Player) e.getEntity();

    if (!DuelManager.isInDuel(p)) return;

    Duel duel = DuelManager.get(p);
    if (duel.getKit() != KitType.SOUP) return;

    // Cancela regen automática
    if (e.getFoodLevel() > p.getFoodLevel()) {
        e.setCancelled(true);
    }
}
}