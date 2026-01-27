package me.rafaelauler.duels;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum KitType {

    SUMO("Sumo", Material.IRON_CHESTPLATE),
    UHC("UHC", Material.DIAMOND_SWORD),
    BOXING("Boxing", Material.GOLD_SWORD),
	SOUP("Soup", Material.MUSHROOM_SOUP);
    private final String display;
    private final Material icon;

    KitType(String display, Material icon) {
        this.display = display;
        this.icon = icon;
    }

    public String getDisplay() {
        return display;
    }

    public Material getIcon() {
        return icon;
    }

    /* ================= APPLY KIT ================= */
    private void giveSoup(Player p) {

        p.getInventory().addItem(
                new ItemStack(Material.DIAMOND_SWORD)
        );
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));

    	p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));

    	p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

    	p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        for (int i = 0; i < 35; i++) {
            p.getInventory().addItem(
                    new ItemStack(Material.MUSHROOM_SOUP)
            );
        }
    }
    public void apply(Player p) {

        p.getInventory().clear();

        switch (this) {

            case SUMO:
                // sem itens (empurrão)
                break;
            case SOUP:
                giveSoup(p);
                break;
            case UHC:
            	p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));

            	p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));

            	p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

            	p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                p.getInventory().addItem(
                        new ItemStack(Material.DIAMOND_SWORD),
                        new ItemStack(Material.GOLDEN_APPLE, 3)
                );
                new ItemStack(Material.BOW);
                new ItemStack(Material.ARROW, 24);
                break;

            case BOXING:
                ItemStack i = 
                        new ItemStack(Material.DIAMOND_SWORD);
                i.addEnchantment(Enchantment.DAMAGE_ALL, 2);
                        p.getInventory().addItem(i);
                break;
        }
    }
}
