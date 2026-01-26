package me.rafaelauler.duels;


import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ParticleUtil {

    public static void hit(Player target) {
        Location loc = target.getLocation().add(0, 1, 0);

        target.getWorld().playEffect(loc, Effect.CRIT, 0);
    }
}

