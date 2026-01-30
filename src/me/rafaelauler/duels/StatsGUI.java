package me.rafaelauler.duels;

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

    public static void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        p.openInventory(inv);

        updateCached(p, inv);
        startUpdateTask(p, inv);
    }
    public static void refreshIfOpen(Player p, PlayerStats stats) {
        if (!p.isOnline()) return;

        if (!p.getOpenInventory().getTitle().equals(TITLE)) return;

        updateInventory(p.getOpenInventory().getTopInventory(), stats);
    }
    private static void updateCached(Player p, Inventory inv) {
        PlayerStats cached = PlayerStatsCache.get(p.getUniqueId());

        if (cached != null) {
            updateInventory(inv, cached);
            return;
        }

        DuelPlugin.getInstance().getMySQL()
            .loadStatsAsync(p.getUniqueId(), stats -> {
                if (p.isOnline()) {
                    updateInventory(inv, stats);
                }
            });
    }

    private static void updateInventory(Inventory inv, PlayerStats stats) {
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
                updateCached(p, inv);
            }
        };

        task.runTaskTimer(DuelPlugin.getInstance(), 0L, 20L);
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
