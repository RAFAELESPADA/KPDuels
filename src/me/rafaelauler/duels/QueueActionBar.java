package me.rafaelauler.duels;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.wavemc.core.bukkit.api.HelixActionBar;

public class QueueActionBar {

    private static BukkitRunnable task;

    public static void start(Player player, KitType kit) {

        stop(player);

        task = new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (!QueueManager.isInQueue(player)) {
                    cancel();
                    return;
                }

               HelixActionBar.send(player, 
                                "§eAguardando oponente §8• §fKit: §b" + kit.name()
                        )
                ;
            }
        };

        task.runTaskTimer(DuelPlugin.getPlugin(DuelPlugin.class), 0L, 20L);
    }

    public static void stop(Player player) {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
