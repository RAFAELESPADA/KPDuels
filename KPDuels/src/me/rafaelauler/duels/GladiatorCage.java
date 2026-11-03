package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * Creates a temporary 56x7x56 Gladiator glass cage inside a configured arena.
 * The original block states are kept so the world can be restored exactly
 * when the duel ends, including when players break or replace cage blocks.
 */
public final class GladiatorCage {

    public static final int WIDTH = 56; // 8 vezes a largura original de 7 blocos.
    public static final int HEIGHT = 7;
    private static final int ROOF_OFFSET = HEIGHT - 1;

    private final Arena arena;
    private final List<BlockState> originalStates = new ArrayList<>();

    private Location spawn1;
    private Location spawn2;
    private boolean removed;

    public GladiatorCage(Arena arena) {
        if (arena == null) {
            throw new IllegalArgumentException("Arena não definida.");
        }
        this.arena = arena;
    }

    public void build() {
        Location configuredSpawn1 = arena.getSpawn1();
        Location configuredSpawn2 = arena.getSpawn2();
        Location min = arena.getMin();
        Location max = arena.getMax();

        if (configuredSpawn1 == null || configuredSpawn2 == null ||
                min == null || max == null || min.getWorld() == null) {
            throw new IllegalArgumentException("A arena Gladiator possui localizações incompletas.");
        }

        World world = min.getWorld();
        if (!world.equals(max.getWorld()) ||
                !world.equals(configuredSpawn1.getWorld()) ||
                !world.equals(configuredSpawn2.getWorld())) {
            throw new IllegalArgumentException("Os pontos da arena Gladiator precisam estar no mesmo mundo.");
        }

        if (configuredSpawn1.getBlockY() != configuredSpawn2.getBlockY()) {
            throw new IllegalArgumentException("Os dois spawns Gladiator precisam estar na mesma altura.");
        }

        int centerX = Math.floorDiv(
                configuredSpawn1.getBlockX() + configuredSpawn2.getBlockX(), 2
        );
        int centerZ = Math.floorDiv(
                configuredSpawn1.getBlockZ() + configuredSpawn2.getBlockZ(), 2
        );
        int baseY = configuredSpawn1.getBlockY() - 1;

        int deltaX = Math.abs(configuredSpawn1.getBlockX() - configuredSpawn2.getBlockX());
        int deltaZ = Math.abs(configuredSpawn1.getBlockZ() - configuredSpawn2.getBlockZ());
        boolean alongX = deltaX >= deltaZ;

        // Mantém exatamente 56 blocos em cada dimensão horizontal.
        int minX = centerX - (WIDTH / 2);
        int maxX = minX + WIDTH - 1;
        int minZ = centerZ - (WIDTH / 2);
        int maxZ = minZ + WIDTH - 1;
        int roofY = baseY + ROOF_OFFSET;

        if (minX < min.getBlockX() || maxX > max.getBlockX() ||
                minZ < min.getBlockZ() || maxZ > max.getBlockZ() ||
                baseY < min.getBlockY() || roofY > max.getBlockY()) {
            throw new IllegalArgumentException(
                    "A seleção da arena precisa comportar uma jaula de 56x7x56 blocos ao redor dos spawns."
            );
        }

        // Os jogadores começam em lados opostos, próximos às paredes da jaula.
        if (alongX) {
            spawn1 = new Location(world, minX + 1.5, baseY + 1, centerZ + 0.5, -90F, 0F);
            spawn2 = new Location(world, maxX - 0.5, baseY + 1, centerZ + 0.5, 90F, 0F);
        } else {
            spawn1 = new Location(world, centerX + 0.5, baseY + 1, minZ + 1.5, 0F, 0F);
            spawn2 = new Location(world, centerX + 0.5, baseY + 1, maxZ - 0.5, 180F, 0F);
        }

        try {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = baseY; y <= roofY; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        originalStates.add(block.getState());

                        Material type;
                        if (y == baseY) {
                            type = Material.STONE_BRICKS;
                        } else if (y == roofY) {
                            type = Material.GLASS;
                        } else if (x == minX || x == maxX || z == minZ || z == maxZ) {
                            type = Material.GLASS;
                        } else {
                            type = Material.AIR;
                        }

                        block.setType(type, false);
                    }
                }
            }
        } catch (RuntimeException exception) {
            remove();
            throw exception;
        }
    }

    public void remove() {
        if (removed) {
            return;
        }

        // Reverse order helps restore blocks with dependencies more safely.
        for (int i = originalStates.size() - 1; i >= 0; i--) {
            originalStates.get(i).update(true, false);
        }

        originalStates.clear();
        removed = true;
    }

    public Location getSpawn1() {
        return spawn1 == null ? null : spawn1.clone();
    }

    public Location getSpawn2() {
        return spawn2 == null ? null : spawn2.clone();
    }
}
