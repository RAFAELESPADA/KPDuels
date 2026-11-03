package me.rafaelauler.duels;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class EventHungerListener implements Listener {

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Não permite perder fome enquanto estiver no evento.
        if (!EventManager.isInEvent(player)) return;

        event.setCancelled(true);

        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }
}