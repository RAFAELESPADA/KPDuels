
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

    /**
     * Verifica se o jogador participa de um evento
     * que permite cura instantânea por sopas.
     */
    private boolean isEventSoupMode(Player player) {
        EventManager manager = EventManager.getInstance();

        if (manager == null
                || !manager.isMember(player)
                || EventManager.isSpectator(player)) {
            return false;
        }

        if (manager.isInMatch(player)
                && "1v1".equalsIgnoreCase(manager.getMatchMode(player))) {
            return true;
        }

        if (manager.isLavaParticipant(player)) {
            return true;
        }

        String mode = manager.getPulledMode(player);

        return "sopa".equalsIgnoreCase(mode)
                || "arena".equalsIgnoreCase(mode);
    }

    /**
     * Verifica se o jogador está em um duelo
     * com kit SOUP ou GLADIATOR.
     */
    private boolean isDuelSoupMode(Player player) {
        if (!DuelManager.isInDuel(player)) {
            return false;
        }

        Duel duel = DuelManager.get(player);

        return duel != null
                && (duel.getKit() == KitType.SOUP
                    || duel.getKit() == KitType.GLADIATOR);
    }

    private boolean canUseSoup(Player player) {
        return isEventSoupMode(player) || isDuelSoupMode(player);
    }

    /**
     * Cura instantânea ao clicar com a sopa.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSoup(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!canUseSoup(player)) {
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getType() != Material.MUSHROOM_STEW) {
            return;
        }

        event.setCancelled(true);

        double maxHealth = player.getMaxHealth();

        boolean needsHealth = player.getHealth() < maxHealth;
        boolean needsFood = player.getFoodLevel() < 20;

        // Não desperdiça sopa com vida e fome completas.
        if (!needsHealth && !needsFood) {
            return;
        }

        if (needsHealth) {
            player.setHealth(
                Math.min(
                    maxHealth,
                    player.getHealth() + HEALTH_HEAL
                )
            );
        }

        if (needsFood) {
            player.setFoodLevel(
                Math.min(
                    20,
                    player.getFoodLevel() + FOOD_HEAL
                )
            );

            player.setSaturation(
                Math.min(
                    20.0F,
                    player.getSaturation() + SATURATION_HEAL
                )
            );
        }

        // Sopa consumida vira tigela para recraft.
        player.getInventory().setItemInMainHand(
            new ItemStack(Material.BOWL)
        );

        player.updateInventory();
    }

    /**
     * Impede regeneração natural nos modos de sopa.
     */
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onNaturalRegeneration(
            EntityRegainHealthEvent event) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!canUseSoup(player)) {
            return;
        }

        if (event.getRegainReason()
                == EntityRegainHealthEvent.RegainReason.SATIATED) {

            event.setCancelled(true);
        }
    }

    /**
     * Impede recuperação automática da fome.
     * As sopas continuam recuperando fome normalmente.
     */
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onFood(FoodLevelChangeEvent event) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!canUseSoup(player)) {
            return;
        }

        if (event.getFoodLevel() > player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }
}
