package me.rafaelauler.duels;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum KitType {

    SUMO("Sumo", Material.IRON_CHESTPLATE),
    UHC("UHC", Material.DIAMOND_SWORD),
    BOXING("Boxing", Material.GOLDEN_SWORD),

    BUILD("Build", Material.COBBLESTONE),
    SOUP("Soup", Material.MUSHROOM_STEW),
    GLADIATOR("Gladiator", Material.IRON_BARS);
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
                    new ItemStack(Material.MUSHROOM_STEW)
            );
        }
    }
    private void giveGladiator(Player p) {
        // Armadura equipada (também há duas peças extras completas no inventário,
        // conforme a disposição da referência visual).
        p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        // Hotbar (slots 0 a 8).
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        p.getInventory().setItem(0, sword);
        p.getInventory().setItem(1, new ItemStack(Material.WATER_BUCKET));
        p.getInventory().setItem(2, new ItemStack(Material.LAVA_BUCKET));
        p.getInventory().setItem(3, new ItemStack(Material.OAK_PLANKS, 64));

        p.getInventory().setItem(4, new ItemStack(Material.IRON_PICKAXE));
        for (int slot = 5; slot <= 7; slot++) {
            p.getInventory().setItem(slot, new ItemStack(Material.MUSHROOM_STEW));
        }
        p.getInventory().setItem(8, new ItemStack(Material.COBBLESTONE_WALL, 64));

        // Primeira fileira do inventário (slots 9 a 17):
        // armadura extra, tigelas, três pilhas de cogumelo marrom e machado.
        setGladiatorRecraftRow(p, 9);

        // Segunda fileira do inventário (slots 18 a 26): repetição da primeira.
        setGladiatorRecraftRow(p, 18);
        p.getInventory().setItem(25, new ItemStack(Material.MUSHROOM_STEW));

        // Última fileira (slots 27 a 35): dois baldes de lava e sete sopas.
        p.getInventory().setItem(27, new ItemStack(Material.LAVA_BUCKET));
        p.getInventory().setItem(28, new ItemStack(Material.LAVA_BUCKET));
        for (int slot = 29; slot <= 35; slot++) {
            p.getInventory().setItem(slot, new ItemStack(Material.MUSHROOM_STEW));
        }
    }

    private void setGladiatorRecraftRow(Player p, int startSlot) {
        p.getInventory().setItem(startSlot, new ItemStack(Material.IRON_HELMET));
        p.getInventory().setItem(startSlot + 1, new ItemStack(Material.IRON_CHESTPLATE));
        p.getInventory().setItem(startSlot + 2, new ItemStack(Material.IRON_LEGGINGS));
        p.getInventory().setItem(startSlot + 3, new ItemStack(Material.IRON_BOOTS));
        p.getInventory().setItem(startSlot + 4, new ItemStack(Material.BOWL, 64));
        p.getInventory().setItem(startSlot + 5, new ItemStack(Material.RED_MUSHROOM, 64));
        p.getInventory().setItem(startSlot + 6, new ItemStack(Material.BROWN_MUSHROOM, 64));
        p.getInventory().setItem(startSlot + 7, new ItemStack(Material.IRON_AXE));
        p.getInventory().setItem(startSlot + 8, new ItemStack(Material.MUSHROOM_STEW));
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
            case GLADIATOR:
                giveGladiator(p);
                break;
            case UHC:
            	p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));

            	p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));

            	p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

            	p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                p.getInventory().addItem(
                        new ItemStack(Material.DIAMOND_SWORD),
                        new ItemStack(Material.GOLDEN_APPLE, 3),
                        new ItemStack(Material.GOLDEN_APPLE, (short) 1)
                );
                p.getInventory().addItem(new ItemStack(Material.BOW));
                p.getInventory().addItem( new ItemStack(Material.ARROW, 25));
                break;
            case BUILD:
            	p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));

            	p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));

            	p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

            	p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                p.getInventory().addItem(
                        new ItemStack(Material.IRON_SWORD),
                        new ItemStack(Material.GOLDEN_APPLE, 3),
                        new ItemStack(Material.OAK_WOOD, 64),

                        new ItemStack(Material.COBBLESTONE, 64),

                        new ItemStack(Material.OAK_LOG, 64),
                        new ItemStack(Material.IRON_AXE),
                        new ItemStack(Material.IRON_PICKAXE),
                        new ItemStack(Material.GLASS, 64),
                        new ItemStack(Material.STONE, 64),
                        new ItemStack(Material.DIRT, 64),

                        new ItemStack(Material.COBBLESTONE_WALL, 64),
                        new ItemStack(Material.GOLDEN_APPLE, (short) 1)
                );
                p.getInventory().addItem(new ItemStack(Material.BOW));
                p.getInventory().addItem( new ItemStack(Material.ARROW, 8));
                break;
            case BOXING:
                ItemStack i = 
                        new ItemStack(Material.DIAMOND_SWORD);
                i.addEnchantment(Enchantment.SHARPNESS, 2);
                        p.getInventory().addItem(i);
                break;
        }
    }
}
