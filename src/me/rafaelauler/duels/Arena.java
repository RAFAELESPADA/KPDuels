package me.rafaelauler.duels;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Arena {

    private final String name;
    private Location pos1, pos2;       // Área da arena
    private Location spawn1, spawn2;   // Spawns de jogadores
    private String schematic;           // Nome da schematic
    private boolean inUse = false;

    public Arena(String name, Location pos1, Location pos2, Location spawn1, Location spawn2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public void teleport(Player p1, Player p2) {
        inUse = true;
        p1.teleport(spawn1);
        p2.teleport(spawn2);
    }

    public void release() { inUse = false; }

    public boolean isInUse() { return inUse; }

    public String getName() { return name; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getSchematic() { return schematic; }

    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public void setSchematic(String schematic) { this.schematic = schematic; }
}
