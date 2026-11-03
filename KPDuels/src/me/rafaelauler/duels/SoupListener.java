package me.rafaelauler.duels;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SoupListener implements Listener {

    private static final double HEALTH_HEAL = 7.0D; // 3,5 corações
    private static final int FOOD_HEAL = 6;
    private static final float SATURATION_HEAL = 6.0F;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSoup(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // Evita consumir duas vezes por causa da mão secundária.
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        EventManager eventManager = EventManager.getInstance();
        boolean eventSoupMode = eventManager != null && (
                (eventManager.isInMatch(player) && "1v1".equals(eventManager.getMatchMode(player)))
                || eventManager.isLavaParticipant(player));

        if (!eventSoupMode) {
            if (!DuelManager.isInDuel(player)) return;
            Duel duel = DuelManager.get(player);
            if (duel == null || (duel.getKit() != KitType.SOUP && duel.getKit() != KitType.GLADIATOR)) {
                return;
            }
        }

        ItemStack held = event.getItem();
        if (held == null || held.getType() != Material.MUSHROOM_STEW) {
            return;
        }

        // Impede que o clique abra blocos ou use o consumo vanilla da sopa.
        event.setCancelled(true);

        double maxHealth = player.getMaxHealth();
        boolean needsHealth = player.getHealth() < maxHealth;
        boolean needsFood = player.getFoodLevel() < 20;
        if (!needsHealth && !needsFood) {
            return; // Não gasta uma sopa quando vida e fome já estão cheias.
        }

        if (needsHealth) {
            player.setHealth(Math.min(maxHealth, player.getHealth() + HEALTH_HEAL));
        }

        if (needsFood) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + FOOD_HEAL));
            player.setSaturation(Math.min(20.0F, player.getSaturation() + SATURATION_HEAL));
        }

        // A sopa consumida vira tigela, que pode ser usada no recraft.
        player.getInventory().setItemInMainHand(new ItemStack(Material.BOWL));
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalRegeneration(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        EventManager eventManager = EventManager.getInstance();
        if (eventManager != null && (
                (eventManager.isInMatch(player) && "1v1".equals(eventManager.getMatchMode(player)))
                || eventManager.isLavaParticipant(player))) {
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
                event.setCancelled(true);
            }
            return;
        }

        if (!DuelManager.isInDuel(player)) {
            return;
        }

        Duel duel = DuelManager.get(player);
        if (duel == null || (duel.getKit() != KitType.SOUP && duel.getKit() != KitType.GLADIATOR)) {
            return;
        }

        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!DuelManager.isInDuel(player)) {
            return;
        }

        Duel duel = DuelManager.get(player);
        if (duel == null || (duel.getKit() != KitType.SOUP && duel.getKit() != KitType.GLADIATOR)) {
            return;
        }

        // Nestes modos a fome é recuperada pelas sopas, não pela regeneração natural.
        if (event.getFoodLevel() > player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }
}
