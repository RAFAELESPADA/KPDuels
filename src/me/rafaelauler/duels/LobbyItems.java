package me.rafaelauler.duels;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyItems {

    public static void give(Player p) {
        p.getInventory().clear();

        p.getInventory().setItem(0, createItem(Material.BLAZE_ROD, "§aDesafiar jogador", "§7Clique para desafiar um player"));
        p.getInventory().setItem(8, createItem(Material.ENDER_PEARL, "§eIr para Spawn", "§7Clique para voltar ao spawn"));
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
