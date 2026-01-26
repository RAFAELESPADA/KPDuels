package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArenaManager {

    private static final List<Arena> arenas = new ArrayList<>();

    public static void add(Arena arena) { arenas.add(arena); }

    public static Arena get() {
        Collections.shuffle(arenas);
        return arenas.get(0);
    }

    public static void release(Arena arena) { arena.release(); }

    public static List<Arena> getArenas() { return arenas; }

    public static void saveArena(Arena arena) {
        String path = "arenas." + arena.getName();
        DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);

        plugin.getConfig().set(path + ".world", arena.getSpawn1().getWorld().getName());
        plugin.getConfig().set(path + ".spawn1", LocationUtil.toString(arena.getSpawn1()));
        plugin.getConfig().set(path + ".spawn2", LocationUtil.toString(arena.getSpawn2()));
        plugin.getConfig().set(path + ".pos1", LocationUtil.toString(arena.getPos1()));
        plugin.getConfig().set(path + ".pos2", LocationUtil.toString(arena.getPos2()));
        plugin.getConfig().set(path + ".schematic", arena.getSchematic());
        plugin.saveConfig();
    }
}
