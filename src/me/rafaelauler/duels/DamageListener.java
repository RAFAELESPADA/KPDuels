package me.rafaelauler.duels;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;


public class DamageListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();
        Duel duel = DuelManager.get(p);
        if (duel == null) return;

        if (duel.getKit() == KitType.BOXING) {
            e.setDamage(0.1);
            DuelManager.addHit(p, (Player) e.getEntity());

            if (DuelManager.getHits(p) >= 100) {
                DuelManager.end(duel, p);
            }
        }

        if (duel.getKit() == KitType.SUMO) {
            e.setCancelled(true);
        }
    }
}
