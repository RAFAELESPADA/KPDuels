package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ArenaManager {

    private static final List<Arena> arenas = new ArrayList<>();

    public static void add(Arena arena) { arenas.add(arena); }

    public static synchronized Arena get(Duel d) {
        List<Arena> lista = new ArrayList<>();

        for (Arena arena : arenas) {
            if (arena.getKitType() == d.getKit() && !arena.isInUse()) {
                lista.add(arena);
            }
        }

        if (lista.isEmpty()) return null;

        Collections.shuffle(lista);
        return lista.get(0);
    }


    public static synchronized Arena getFreeArena(KitType kit) {
        System.out.println("=== ARENAS ===");
        getArenas().forEach(a ->
            System.out.println(a.getName() + " -> inUse=" + a.isInUse() + ", kit=" + a.getKitType())
        );

        for (Arena arena : arenas) {
            if (!arena.isInUse() && arena.getKitType() == kit) {
                return arena;
            }
        }

        return null;
    }
    public static void loadArenas() {
        DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);
        arenas.clear();
        if (!plugin.getConfig().contains("arenas")) {
            Bukkit.getLogger().warning("[Duels] Nenhuma arena encontrada no config!");
            return;
        }

        for (String name : plugin.getConfig().getConfigurationSection("arenas").getKeys(false)) {

            String path = "arenas." + name;

            String worldName = plugin.getConfig().getString(path + ".world");
            Bukkit.getLogger().info("[Duels] Tentando carregar arena " + name + " no mundo: " + worldName);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Bukkit.getLogger().warning("[Duels] Mundo inválido na arena " + name);
                continue;
            }
            KitType kit;
            try {
                kit = KitType.valueOf(plugin.getConfig().getString(path + ".kit").toUpperCase());
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Duels] Kit inválido na arena " + name);
                continue;
            }
            Location spawn1 = LocationUtil.fromString(plugin.getConfig().getString(path + ".spawn1"), world);
            Location spawn2 = LocationUtil.fromString(plugin.getConfig().getString(path + ".spawn2"), world);
            Location pos1   = LocationUtil.fromString(plugin.getConfig().getString(path + ".pos1"), world);
            Location pos2   = LocationUtil.fromString(plugin.getConfig().getString(path + ".pos2"), world);


            if (spawn1 == null || spawn2 == null || pos1 == null || pos2 == null) {
                Bukkit.getLogger().warning("[Duels] Arena " + name + " inválida, ignorando.");
                continue;
            }

            Arena arena = new Arena(name, kit, pos1, pos2, spawn1, spawn2);
            arena.setSchematic(plugin.getConfig().getString(path + ".schematic"));

            add(arena);

            Bukkit.getLogger().info("[Duels] Arena carregada: " + name);
        }

        Bukkit.getLogger().info("[Duels] Total de arenas carregadas: " + arenas.size());
    }

    public static synchronized void release(Arena arena) {
        if (arena != null) {
            arena.setInUse(false);
        }
    }

    public static List<Arena> getArenas() { return arenas; }
    public static Arena getByName(String name) {
        if (name == null) return null;

        for (Arena arena : arenas) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }
   
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
