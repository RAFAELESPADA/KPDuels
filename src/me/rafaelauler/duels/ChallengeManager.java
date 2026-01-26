package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ChallengeManager {

    private static JavaPlugin plugin;
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 10; // segundos
    private static final Map<UUID, UUID> challenges = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> timeouts = new HashMap<>();
    private static final Map<UUID, Boolean> selecting = new HashMap<>();

    /* ================= INIT ================= */

    public static void init(JavaPlugin pl) {
        plugin = pl;
    }

    /* ================= SELEÇÃO ================= */

    public static boolean isSelecting(Player p) {
        return selecting.containsKey(p.getUniqueId());
    }
    public static boolean inCooldown(Player p) {

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        return (now - last) < TimeUnit.SECONDS.toMillis(COOLDOWN_TIME);
    }

    public static long getCooldown(Player p) {

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        long left = TimeUnit.SECONDS.toMillis(COOLDOWN_TIME) - (now - last);
        return Math.max(0, left / 1000);
    }

    private static void applyCooldown(Player p) {
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public static void startSelecting(Player p) {
        selecting.put(p.getUniqueId(), true);
        p.sendMessage("§aClique em um jogador para desafiar!");
    }

    public static void cancelSelecting(Player p) {
        selecting.remove(p.getUniqueId());
    }
    public static void resetCooldown(Player p) {
        cooldowns.remove(p.getUniqueId());
    }
    /* ================= DESAFIO ================= */

    public static void challenge(Player challenger, Player target) {
    	if (inCooldown(challenger)) {
            challenger.sendMessage("§cAguarde §f" + getCooldown(challenger) + "s §cpara desafiar novamente.");
            return;
        }
        if (DuelManager.isInDuel(challenger) || DuelManager.isInDuel(target)) {
            challenger.sendMessage("§cAlguém já está em duelo.");
            return;
        }
        applyCooldown(challenger); // ⏱️ aplica cooldown
        challenges.put(target.getUniqueId(), challenger.getUniqueId());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (hasChallenge(target)) {
                    clearChallenge(target);
                    challenger.sendMessage("§cO desafio expirou.");
                    target.sendMessage("§cO desafio expirou.");
                }
            }
        };

        task.runTaskLater(plugin, 20L * 30);
        timeouts.put(target.getUniqueId(), task);
    }

    public static boolean hasChallenge(Player p) {
        return challenges.containsKey(p.getUniqueId());
    }

    public static Player getChallenger(Player target) {
        UUID id = challenges.get(target.getUniqueId());
        return id == null ? null : Bukkit.getPlayer(id);
    }

    /* ================= CLEAR ================= */

    private static void clearChallenge(Player target) {

        challenges.remove(target.getUniqueId());

        BukkitRunnable task = timeouts.remove(target.getUniqueId());
        if (task != null) task.cancel();
    }

    /* ================= ACEITAR / NEGAR ================= */

    public static void accept(Player target) {

        if (!hasChallenge(target)) {
            target.sendMessage("§cVocê não tem desafios pendentes.");
            return;
        }

        Player challenger = getChallenger(target);
        challenges.remove(target.getUniqueId());

        BukkitRunnable task = timeouts.remove(target.getUniqueId());
        if (task != null) task.cancel();

        if (challenger != null) {
            resetCooldown(challenger); // 🔥 RESET DO COOLDOWN
        }

        Duel duel = new Duel(challenger, target, ArenaManager.get(), KitType.UHC);
        DuelManager.add(duel);
    }


    public static void deny(Player target) {

        Player challenger = getChallenger(target);
        clearChallenge(target);

        if (challenger != null) {
            challenger.sendMessage("§cSeu desafio foi recusado.");
        }
        resetCooldown(challenger);
        target.sendMessage("§cVocê recusou o desafio.");
    }
}
