package me.rafaelauler.duels;


import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerTick {

    private static volatile int tick = 0;

    public static void start(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> tick++, 1L, 1L);
    }

    public static int get() {
        return tick;
    }
}

