package me.rafaelauler.duels;


import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtil {

    public static String toString(Location l) {
        return l.getX() + "," +
               l.getY() + "," +
               l.getZ() + "," +
               l.getYaw() + "," +
               l.getPitch();
    }

    public static Location fromString(String s, String world) {
        String[] p = s.split(",");

        return new Location(
                Bukkit.getWorld(world),
                Double.parseDouble(p[0]),
                Double.parseDouble(p[1]),
                Double.parseDouble(p[2]),
                Float.parseFloat(p[3]),
                Float.parseFloat(p[4])
        );
    }
}
