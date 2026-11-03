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

public class DuelGUI {

    public static final String TITLE = "§8Selecione um modo";
    private static final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    /* ======================= OPEN ======================= */

    public static void open(Player p) {

        // 🔥 Anti-spam de abertura
        if (ClickCooldown.inCooldown(p.getUniqueId())) {
            p.sendMessage("§cAguarde um instante...");
            return;
        }
        ClickCooldown.apply(p.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        p.openInventory(inv);
        startUpdateTask(p, inv);
    }

    /* ======================= UPDATE ======================= */

    public static void update(Player p, Inventory inv) {

        fill(inv);
        inv.setItem(9, kitItem(
                p,
                KitType.BUILD,
                Material.COBBLESTONE,
                "§aBuild",
                "§7Batalha com o BUILD ativado!"
        ));
        inv.setItem(11, kitItem(
                p,
                KitType.SUMO,
                Material.IRON_CHESTPLATE,
                "§aSumo",
                "§7Empurre o oponente"
        ));

        inv.setItem(13, kitItem(
                p,
                KitType.UHC,
                Material.DIAMOND_SWORD,
                "§cUHC",
                "§7PvP clássico"
        ));

        inv.setItem(15, kitItem(
                p,
                KitType.BOXING,
                Material.GOLDEN_SWORD,
                "§bBoxing",
                "§7Primeiro a 100 hits"
        ));

        inv.setItem(17, kitItem(
                p,
                KitType.SOUP,
                Material.MUSHROOM_STEW,
                "§eSopa",
                "§7Modo clássico com sopa"
        ));

        inv.setItem(16, kitItem(
                p,
                KitType.GLADIATOR,
                Material.IRON_BARS,
                "§6Gladiator",
                "§7Escape da jaula e derrote seu oponente"
        ));

        inv.setItem(22, item(
                Material.BOOK,
                "§eStats",
                "§7Clique para ver seus stats"
        ));

        inv.setItem(25, item(
                Material.BARRIER,
                "§cCancelar fila",
                "§7Clique para sair da fila"
        ));
    }

    /* ======================= KIT ITEM ======================= */

    private static ItemStack kitItem(Player p, KitType kit, Material mat, String name, String desc) {

        // Player já está nessa fila
        if (QueueManager.isInQueue(p, kit)) {
            return item(
                    mat,
                    name,
                    "§eProcurando oponente...",
                    "§7Aguarde um jogador entrar"
            );
        }

        int count = QueueManager.getQueueSize(kit);

        return item(
                mat,
                name,
                desc,
                "",
                "§fNa fila: §a" + count
        );
    }

    /* ======================= TASK ======================= */

    private static void startUpdateTask(Player p, Inventory inv) {

        stopTask(p);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {

                if (!p.isOnline()
                        || p.getOpenInventory() == null
                        || !p.getOpenInventory().getTitle().equals(TITLE)) {
                    stopTask(p);
                    return;
                }

                update(p, inv);
            }
        };

        task.runTaskTimer(
                DuelPlugin.getPlugin(DuelPlugin.class),
                0L,
                20L
        );

        tasks.put(p.getUniqueId(), task);
    }

    public static void stopTask(Player p) {
        BukkitRunnable task = tasks.remove(p.getUniqueId());
        if (task != null) task.cancel();
    }

    /* ======================= UTIL ======================= */

    private static void fill(Inventory inv) {
        ItemStack glass = item(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }
}
