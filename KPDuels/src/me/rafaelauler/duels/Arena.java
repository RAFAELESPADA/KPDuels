package me.rafaelauler.duels;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Arena {

    private final String name;
    private String schematic;
    private boolean inUse = false;

    private KitType kitType;
    private Location pos1, pos2;
    private Location spawn1, spawn2;
    private final Location min;
    private final Location max;
    public Arena(String name, KitType kitType, Location pos1, Location pos2, Location spawn1, Location spawn2) {
        this.name = name;
        this.kitType = kitType;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.min = new Location(
                pos1.getWorld(),
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        this.max = new Location(
                pos1.getWorld(),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public Location getMin() { return min; }
    public Location getMax() { return max; }
    

    public KitType getKitType() {
        return kitType;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }
    // ❌ NÃO mexe em inUse
    public void teleport(Player p1, Player p2) {
        p1.teleport(spawn1);
        p2.teleport(spawn2);
    }

    // Somente o ArenaManager pode chamar isso
    protected void setInUse(boolean use) {
        this.inUse = use;
    }

    protected void release() {
        this.inUse = false;
    }

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
