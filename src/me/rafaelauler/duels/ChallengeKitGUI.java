package me.rafaelauler.duels;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChallengeKitGUI {

    public static final String TITLE = "§8Selecionar Kit";

    public static void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);

        inv.setItem(0, kit(Material.DIAMOND_SWORD, "§bUHC"));
        inv.setItem(1, kit(Material.MUSHROOM_SOUP, "§aSOUP"));
        inv.setItem(2, kit(Material.IRON_CHESTPLATE, "§eSUMO"));
        inv.setItem(3, kit(Material.GOLD_SWORD, "§6BOXING"));
        inv.setItem(4, kit(Material.COBBLESTONE, "§7BUILD"));

        p.openInventory(inv);
    }

    private static ItemStack kit(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Clique para selecionar"));
        item.setItemMeta(meta);
        return item;
    }
}
