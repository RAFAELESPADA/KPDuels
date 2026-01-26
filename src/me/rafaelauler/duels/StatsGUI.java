package me.rafaelauler.duels;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class StatsGUI {

    public static final String TITLE = "§8Seus Stats";
    private static final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    private static MySQLManager mysql;

    public static void setMySQL(MySQLManager manager) {
        mysql = manager;
    }

    public static void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        p.openInventory(inv);
        startUpdateTask(p, inv);
    }

    public static void update(Player p, Inventory inv) {
        if (mysql == null) return;

        PlayerStats stats;
        try {
            stats = mysql.getStats(p.getUniqueId());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        inv.setItem(3, item(Material.DIAMOND_SWORD, "§aWins", "§7" + stats.getWins()));
        inv.setItem(4, item(Material.BARRIER, "§cLosses", "§7" + stats.getLosses()));
        inv.setItem(5, item(Material.GOLD_INGOT, "§6WinStreak", "§7" + stats.getWinstreak()));
    }

    private static void startUpdateTask(Player p, Inventory inv) {
        stopTask(p);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !p.getOpenInventory().getTitle().equals(TITLE)) {
                    stopTask(p);
                    return;
                }
                update(p, inv);
            }
        };
        task.runTaskTimer(DuelPlugin.getPlugin(DuelPlugin.class), 0L, 20L);
        tasks.put(p.getUniqueId(), task);
    }

    public static void stopTask(Player p) {
        BukkitRunnable task = tasks.remove(p.getUniqueId());
        if (task != null) task.cancel();
    }

    private static ItemStack item(Material mat, String name, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }
}
