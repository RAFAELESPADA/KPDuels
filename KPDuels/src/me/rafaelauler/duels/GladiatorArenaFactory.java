package me.rafaelauler.duels;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Gera uma arena temporária do Gladiator em uma posição aleatória do mundo Duels.
 * A área é escolhida sobre terreno sólido e afastada do spawn, de jogadores e
 * das arenas já cadastradas. A própria GladiatorCage guarda/restaura os blocos.
 */
public final class GladiatorArenaFactory {

    public static final String RANDOM_ARENA_PREFIX = "gladiator-random-";
    private static final int CAGE_HALF_WIDTH = GladiatorCage.WIDTH / 2;
    private static final int CAGE_HEIGHT = GladiatorCage.HEIGHT;

    private GladiatorArenaFactory() {
    }

    public static Arena createRandomArena() {
        DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);
        String configuredWorld = plugin.getConfig().getString("gladiator.world", "Duels");
        World world = findWorld(configuredWorld);

        if (world == null) {
            plugin.getLogger().warning("[Duels] Mundo do Gladiator não encontrado: " + configuredWorld);
            return null;
        }

        int radius = Math.max(64, plugin.getConfig().getInt("gladiator.random-radius", 5000));
        int minDistance = Math.max(0, plugin.getConfig().getInt("gladiator.min-distance-from-spawn", 150));
        int attempts = Math.max(1, plugin.getConfig().getInt("gladiator.attempts", 50));
        Location worldSpawn = world.getSpawnLocation();
        WorldBorder border = world.getWorldBorder();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int centerX = worldSpawn.getBlockX()
                    + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int centerZ = worldSpawn.getBlockZ()
                    + ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            double dx = centerX - worldSpawn.getX();
            double dz = centerZ - worldSpawn.getZ();
            if ((dx * dx) + (dz * dz) < (double) minDistance * minDistance) {
                continue;
            }

            int minX = centerX - CAGE_HALF_WIDTH;
            int maxX = minX + GladiatorCage.WIDTH - 1;
            int minZ = centerZ - CAGE_HALF_WIDTH;
            int maxZ = minZ + GladiatorCage.WIDTH - 1;

            // Confere os quatro cantos para garantir que toda a jaula fique dentro da borda.
            if (!border.isInside(new Location(world, minX + 0.5, world.getMinHeight(), minZ + 0.5))
                    || !border.isInside(new Location(world, maxX + 0.5, world.getMinHeight(), maxZ + 0.5))) {
                continue;
            }

            // Mantém uma margem livre ao redor da jaula para não prender jogadores próximos.
            if (isNearOnlinePlayer(world, centerX, centerZ, CAGE_HALF_WIDTH + 24)) {
                continue;
            }

            if (overlapsAnotherArena(world, centerX, centerZ, 12)) {
                continue;
            }

            int highestGround = findGroundHeight(world, centerX, centerZ);
            int baseY;

            if (highestGround != Integer.MIN_VALUE
                    && highestGround > world.getMinHeight() + 3
                    && highestGround + 1 > world.getMinHeight()) {
                // Em mundos normais, deixa o piso logo acima do terreno.
                baseY = highestGround + 1;
            } else {
                // Suporte a mundos void: cria uma plataforma suspensa em uma área vazia.
                baseY = plugin.getConfig().getInt("gladiator.floating-y", 80);
                if (!isEmptyVolume(world, centerX, baseY, centerZ)) {
                    continue;
                }
            }

            int roofY = baseY + CAGE_HEIGHT - 1;
            if (baseY <= world.getMinHeight() || roofY >= world.getMaxHeight()) {
                continue;
            }

            // A jaula fica alinhada no eixo X, com os jogadores em lados opostos.
            Location pos1 = new Location(world, minX, baseY, minZ);
            Location pos2 = new Location(world, maxX, roofY, maxZ);
            Location referenceSpawn1 = new Location(world, centerX - 2, baseY + 1, centerZ);
            Location referenceSpawn2 = new Location(world, centerX + 2, baseY + 1, centerZ);

            String name = RANDOM_ARENA_PREFIX + System.currentTimeMillis() + "-" + attempt;
            return new Arena(name, KitType.GLADIATOR, pos1, pos2, referenceSpawn1, referenceSpawn2);
        }

        plugin.getLogger().warning("[Duels] Não foi encontrado terreno seguro para uma arena Gladiator após "
                + attempts + " tentativas. Aumente gladiator.random-radius ou gladiator.attempts no config.yml.");
        return null;
    }

    private static World findWorld(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        World exact = Bukkit.getWorld(name);
        if (exact != null) {
            return exact;
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(name)) {
                return world;
            }
        }
        return null;
    }

    /**
     * Retorna a maior altura de terreno sólido dentro da área 56x56.
     * A plataforma será construída nessa altura, nivelando a superfície da jaula.
     */
    private static int findGroundHeight(World world, int centerX, int centerZ) {
        int highest = Integer.MIN_VALUE;

        // Carrega primeiro o chunk central; as consultas seguintes carregam os vizinhos necessários.
        world.getChunkAt(centerX >> 4, centerZ >> 4).load();

        int minX = centerX - CAGE_HALF_WIDTH;
        int maxX = minX + GladiatorCage.WIDTH - 1;
        int minZ = centerZ - CAGE_HALF_WIDTH;
        int maxZ = minZ + GladiatorCage.WIDTH - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block top = world.getHighestBlockAt(x, z);
                Material type = top.getType();

                if (!type.isSolid() || top.isLiquid()) {
                    return Integer.MIN_VALUE;
                }

                highest = Math.max(highest, top.getY());
            }
        }

        // A jaula recebe um piso contínuo na maior altura encontrada, mesmo em terreno irregular.
        return highest;
    }


    private static boolean isEmptyVolume(World world, int centerX, int baseY, int centerZ) {
        int minX = centerX - CAGE_HALF_WIDTH;
        int maxX = minX + GladiatorCage.WIDTH - 1;
        int minZ = centerZ - CAGE_HALF_WIDTH;
        int maxZ = minZ + GladiatorCage.WIDTH - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = baseY; y < baseY + CAGE_HEIGHT; y++) {
                    if (!world.getBlockAt(x, y, z).getType().isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    private static boolean isNearOnlinePlayer(World world, int x, int z, int distance) {
        int distanceSquared = distance * distance;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(new Location(world, x + 0.5,
                    player.getLocation().getY(), z + 0.5)) < distanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsAnotherArena(World world, int centerX, int centerZ, int margin) {
        int minX = centerX - CAGE_HALF_WIDTH - margin;
        int maxX = centerX - CAGE_HALF_WIDTH + GladiatorCage.WIDTH - 1 + margin;
        int minZ = centerZ - CAGE_HALF_WIDTH - margin;
        int maxZ = centerZ - CAGE_HALF_WIDTH + GladiatorCage.WIDTH - 1 + margin;

        for (Arena arena : ArenaManager.getArenas()) {
            if (arena == null || arena.getMin() == null || arena.getMax() == null
                    || arena.getMin().getWorld() == null
                    || !world.equals(arena.getMin().getWorld())) {
                continue;
            }

            if (maxX >= arena.getMin().getBlockX() && minX <= arena.getMax().getBlockX()
                    && maxZ >= arena.getMin().getBlockZ() && minZ <= arena.getMax().getBlockZ()) {
                return true;
            }
        }
        return false;
    }
}
