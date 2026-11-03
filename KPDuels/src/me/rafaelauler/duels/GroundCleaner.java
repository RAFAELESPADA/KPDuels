package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

public class GroundCleaner extends BukkitRunnable {

    @Override
    public void run() {

        World world = Bukkit.getWorld("Duels");

        if (world == null) {
            return;
        }

        int removed = 0;

        for (Entity entity : world.getEntities()) {

            if (entity instanceof Item) {
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            Bukkit.getLogger().info(
                "[Duels] Ground Cleaner removeu " + removed + " itens do chão."
            );
        }
    }
}