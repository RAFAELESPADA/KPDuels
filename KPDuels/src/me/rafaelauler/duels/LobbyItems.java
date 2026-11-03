package me.rafaelauler.duels;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyItems {

    public static void give(Player p) {
        p.getInventory().clear();

        p.getInventory().setItem(0, createItem(Material.BLAZE_ROD, "§aDesafiar jogador", "§7Clique para desafiar um player para 1v1"));
        p.getInventory().setItem(1, createItem(Material.DIAMOND_SWORD, "§aEntrar em uma fila", "§7Clique para entrar em uma fila"));
        
        p.getInventory().setItem(7, createItem(Material.ENDER_PEARL, "§eIr para Spawn", "§7Clique para voltar ao spawn"));
        p.getInventory().setItem(8, createItem(Material.BARRIER, "§eVoltar ao lobby", "§7Clique para voltar ao lobby principal"));
        
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
