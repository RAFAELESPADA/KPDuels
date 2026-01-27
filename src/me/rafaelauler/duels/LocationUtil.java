package me.rafaelauler.duels;


import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static String toString(Location l) {
        return l.getX() + "," +
               l.getY() + "," +
               l.getZ() + "," +
               l.getYaw() + "," +
               l.getPitch();
    }

    public static Location fromString(String s, World world) {
        if (s == null || world == null) return null;

        String[] split = s.split(",");

        double x = Double.parseDouble(split[0]);
        double y = Double.parseDouble(split[1]);
        double z = Double.parseDouble(split[2]);

        float yaw = split.length > 3 ? Float.parseFloat(split[3]) : 0f;
        float pitch = split.length > 4 ? Float.parseFloat(split[4]) : 0f;

        return new Location(world, x, y, z, yaw, pitch);
    }

}
