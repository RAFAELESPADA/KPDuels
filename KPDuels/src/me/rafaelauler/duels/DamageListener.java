package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import net.wavemc.core.bukkit.api.HelixActionBar;

public class DamageListener implements Listener {

	
	  @EventHandler
	    public void onDama2ge(FoodLevelChangeEvent e) {
		  if (DuelsCommand.game.contains(e.getEntity().getName())) {
			  e.setCancelled(true);
	  }
	  }
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Player)) return;

        Player damager = (Player) e.getDamager();
        Player victim  = (Player) e.getEntity();

        Duel duel = DuelManager.get(damager);
        if (duel == null) return;

        // 🔒 Garante que está batendo no oponente
        if (!duel.hasPlayer(victim)) {
            e.setCancelled(true);
            return;
        }

        // 🥊 BOXING
        if (duel.getKit() == KitType.BOXING) {
            e.setDamage(0);

            DuelManager.addHit(damager);
HelixActionBar.send(damager, ChatColor.GREEN + "Hits: " + ChatColor.WHITE  + DuelManager.getHits(damager));
            if (DuelManager.getHits(damager) >= 100) {
                DuelManager.end(damager); // vencedor
                victim.teleport(Bukkit.getWorld("duels").getSpawnLocation());
                LobbyItems.give(victim);
            }
            return;
        }

        // 🟦 SUMO
        if (duel.getKit() == KitType.SUMO) {
            e.setDamage(0);
            return;
        }
    }
}
