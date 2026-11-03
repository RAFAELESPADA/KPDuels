package me.rafaelauler.duels;


import org.bukkit.entity.Player;

public class TitleUtil {

    public static void send(Player p, String title, String subtitle) {
        p.sendTitle(title, subtitle);
    }
}
