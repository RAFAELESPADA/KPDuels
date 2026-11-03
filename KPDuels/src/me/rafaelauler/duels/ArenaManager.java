package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ArenaManager {

    private static final List<Arena> arenas = new ArrayList<>();

    public static synchronized void add(Arena arena) { arenas.add(arena); }

    public static synchronized Arena get(Duel d) {
        if (d != null && d.getKit() == KitType.GLADIATOR) {
            return getFreeArena(KitType.GLADIATOR);
        }
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
        if (kit == KitType.GLADIATOR) {
            Arena randomArena = GladiatorArenaFactory.createRandomArena();
            if (randomArena != null) {
                arenas.add(randomArena);
            }
            return randomArena;
        }

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

    if (!plugin.getConfig().isConfigurationSection("arenas")) {
        Bukkit.getLogger().warning("[Duels] Seção arenas não encontrada!");
        return;
    }

    for (String name : plugin.getConfig()
            .getConfigurationSection("arenas").getKeys(false)) {

        String path = "arenas." + name;

        try {

            String worldName = plugin.getConfig().getString(path + ".world");

            if (worldName == null || worldName.isEmpty()) {
                throw new IllegalArgumentException("Mundo não definido.");
            }

            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Bukkit.getLogger().warning(
                    "[Duels] Mundo não carregado: " + worldName
                );
                continue;
            }

            String kitName = plugin.getConfig().getString(path + ".kit");

            if (kitName == null) {
                throw new IllegalArgumentException("Kit não definido.");
            }

            KitType kit = KitType.valueOf(kitName.toUpperCase());

            Location spawn1 = LocationUtil.fromString(
                plugin.getConfig().getString(path + ".spawn1"), world);

            Location spawn2 = LocationUtil.fromString(
                plugin.getConfig().getString(path + ".spawn2"), world);

            Location pos1 = LocationUtil.fromString(
                plugin.getConfig().getString(path + ".pos1"), world);

            Location pos2 = LocationUtil.fromString(
                plugin.getConfig().getString(path + ".pos2"), world);

            if (spawn1 == null || spawn2 == null ||
                pos1 == null || pos2 == null) {

                throw new IllegalArgumentException(
                    "Localizações incompletas ou inválidas."
                );
            }

            Arena arena = new Arena(
                name, kit, pos1, pos2, spawn1, spawn2
            );

            arena.setSchematic(
                plugin.getConfig().getString(path + ".schematic")
            );

            add(arena);

            Bukkit.getLogger().info(
                "[Duels] Arena carregada: " + name
            );

        } catch (Exception e) {

            Bukkit.getLogger().warning(
                "[Duels] Erro ao carregar arena " + name +
                ": " + e.getMessage()
            );
        }
    }

    Bukkit.getLogger().info(
        "[Duels] Total de arenas carregadas: " + arenas.size()
    );
}


    public static synchronized void release(Arena arena) {
        if (arena == null) {
            return;
        }

        if (arena.getName() != null
                && arena.getName().startsWith(GladiatorArenaFactory.RANDOM_ARENA_PREFIX)) {
            arenas.remove(arena);
            return;
        }

        arena.setInUse(false);
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
        plugin.getConfig().set(path + ".kit", arena.getKitType().name());
        plugin.getConfig().set(path + ".world", arena.getSpawn1().getWorld().getName());
        plugin.getConfig().set(path + ".spawn1", LocationUtil.toString(arena.getSpawn1()));
        plugin.getConfig().set(path + ".spawn2", LocationUtil.toString(arena.getSpawn2()));
        plugin.getConfig().set(path + ".pos1", LocationUtil.toString(arena.getPos1()));
        plugin.getConfig().set(path + ".pos2", LocationUtil.toString(arena.getPos2()));
        plugin.getConfig().set(path + ".schematic", arena.getSchematic());
        plugin.saveConfig();
    }
}
