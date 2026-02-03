package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ChallengeManager {

    private static JavaPlugin plugin;

    private static final long COOLDOWN_TIME = 10;

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Challenge> challenges = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> timeouts = new HashMap<>();
    private static final Map<UUID, KitType> selectedKit = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TICK = new ConcurrentHashMap<>();

    /* ================= INIT ================= */

    public static void init(JavaPlugin pl) {
        plugin = pl;
    }

    /* ================= KIT (GUI) ================= */

    public static void selectKit(Player p, KitType kit) {
        selectedKit.put(p.getUniqueId(), kit);
        p.sendMessage("§aKit selecionado: §e" + kit.name());
        p.sendMessage("§7Clique em um jogador para desafiar.");
    }

    public static boolean hasSelectedKit(Player p) {
        return selectedKit.containsKey(p.getUniqueId());
    }
    public static long getCooldown(Player p) {

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        long remainingMillis =
                TimeUnit.SECONDS.toMillis(COOLDOWN_TIME) - (now - last);

        return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(remainingMillis));
    }

    /* ================= COOLDOWN ================= */

    public static boolean inCooldown(Player p) {
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);
        return (now - last) < TimeUnit.SECONDS.toMillis(COOLDOWN_TIME);
    }

    private static void applyCooldown(Player p) {
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public static void resetCooldown(Player p) {
        cooldowns.remove(p.getUniqueId());
    }

    /* ================= DESAFIO ================= */
    public static void deny(Player target) {

        Challenge challenge = challenges.get(target.getUniqueId());
        if (challenge == null) {
            target.sendMessage("§cVocê não tem desafios pendentes.");
            return;
        }

        Player challenger = Bukkit.getPlayer(challenge.getChallenger());

        clear(target);

        target.sendMessage("§cVocê recusou o desafio.");

        if (challenger != null) {
            challenger.sendMessage("§cSeu desafio foi recusado.");
            resetCooldown(challenger);
        }
    }

    public static void challenge(Player challenger, Player target) {

        if (challenger.equals(target)) return;
        UUID uuid = challenger.getUniqueId();

        // 🔒 defesa absoluta
        if (cooldowns.containsKey(uuid)) return;
        if (challenger.getWorld() != target.getWorld()) return;

        if (challenger.getLocation().distanceSquared(target.getLocation()) > 36) {
            // > 6 blocos
            return;
        }

        if (challenger.equals(target)) return;
        if (!hasSelectedKit(challenger)) return;
        if (challenges.containsKey(target.getUniqueId())) return;
        if (DuelManager.isInDuel(challenger) || DuelManager.isInDuel(target)) return;

        KitType kit = selectedKit.remove(uuid);

        applyCooldown(challenger);

        challenges.put(
            target.getUniqueId(),
            new Challenge(uuid, kit)
        );

        challenger.sendMessage("§aDesafio enviado para §f" + target.getName()
                + " §acom §e" + kit.name());

        target.sendMessage("§e" + challenger.getName()
                + " §ate desafiou para §e" + kit.name());
        target.sendMessage("§7Use §a/accept §7ou §c/deny");

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!challenges.containsKey(target.getUniqueId())) return;

                clear(target);
                challenger.sendMessage("§cO desafio expirou.");
                target.sendMessage("§cO desafio expirou.");
                ChallengeStateManager.clear(uuid);
            }
        };

        task.runTaskLater(plugin, 20L * 30);
        timeouts.put(target.getUniqueId(), task);
    }

    /* ================= ACCEPT ================= */

    public static void accept(Player target) {

        Challenge challenge = challenges.get(target.getUniqueId());
        if (challenge == null) {
            target.sendMessage("§cVocê não tem desafios pendentes.");
            return;
        }

        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        KitType kit = challenge.getKit();

        clear(target);

        if (challenger == null) {
            target.sendMessage("§cO jogador não está online.");
            return;
        }

        Arena arena = ArenaManager.getFreeArena(kit);
        if (arena == null) {
            challenger.sendMessage("§cNenhuma arena disponível.");
            target.sendMessage("§cNenhuma arena disponível.");
            return;
        }

        Duel duel = new Duel(challenger, target, arena, kit);
        DuelManager.add(duel);

        if (!duel.start()) {
            DuelManager.forceEnd(challenger);
            ArenaManager.release(arena);
            return;
        }

        resetCooldown(challenger);
    }

    /* ================= CLEAR ================= */

    public static void clear(Player target) {

        UUID uuid = target.getUniqueId();

        challenges.remove(uuid);

        BukkitRunnable task = timeouts.remove(uuid);
        if (task != null) task.cancel();

        selectedKit.remove(uuid);
    }
}
