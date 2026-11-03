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
    // Jogador que receberá o desafio após a seleção do kit.
    private static final Map<UUID, UUID> pendingTargets = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TICK = new ConcurrentHashMap<>();

    /* ================= INIT ================= */

    public static void init(JavaPlugin pl) {
        plugin = pl;
    }

    /* ================= KIT (GUI) ================= */

    public static void selectKit(Player p, KitType kit) {
        selectedKit.put(p.getUniqueId(), kit);
        p.sendMessage("§aKit selecionado: §e" + kit.name());
    }

    public static boolean hasSelectedKit(Player p) {
        return selectedKit.containsKey(p.getUniqueId());
    }

    public static void setPendingTarget(Player challenger, Player target) {
        pendingTargets.put(challenger.getUniqueId(), target.getUniqueId());
    }

    public static boolean hasPendingTarget(Player challenger) {
        return pendingTargets.containsKey(challenger.getUniqueId());
    }

    public static Player getPendingTarget(Player challenger) {
        UUID targetId = pendingTargets.get(challenger.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public static void clearPendingTarget(Player challenger) {
        pendingTargets.remove(challenger.getUniqueId());
    }

    public static boolean hasPendingChallenge(Player target) {
        return challenges.containsKey(target.getUniqueId());
    }

    private static boolean hasOutgoingChallenge(UUID challengerId) {
        for (Challenge challenge : challenges.values()) {
            if (challenge.getChallenger().equals(challengerId)) {
                return true;
            }
        }
        return false;
    }

    public static void cancelSelection(Player challenger) {
        UUID uuid = challenger.getUniqueId();
        selectedKit.remove(uuid);
        pendingTargets.remove(uuid);
        ChallengeStateManager.clear(uuid);
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
        ChallengeStateManager.clear(challenge.getChallenger());

        target.sendMessage("§cVocê recusou o desafio.");

        if (challenger != null) {
            challenger.sendMessage("§cSeu desafio foi recusado.");
            resetCooldown(challenger);
        }
    }

    public static boolean challenge(Player challenger, Player target) {

        if (challenger == null || target == null || !challenger.isOnline() || !target.isOnline()) {
            return false;
        }
        if (challenger.equals(target)) {
            challenger.sendMessage("§cVocê não pode desafiar a si mesmo.");
            return false;
        }
        if (EventManager.isInEvent(challenger) || EventManager.isInEvent(target)) {
            challenger.sendMessage("§cVocê ou esse jogador está participando de um evento.");
            return false;
        }

        UUID uuid = challenger.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        if (inCooldown(challenger)) {
            challenger.sendMessage("§cAguarde §f" + getCooldown(challenger)
                    + "s §cpara desafiar novamente.");
            return false;
        }
        // Remove um cooldown antigo que já expirou.
        cooldowns.remove(uuid);

        if (challenger.getWorld() != target.getWorld()) {
            challenger.sendMessage("§cO jogador não está mais no mesmo mundo.");
            return false;
        }

        if (challenger.getLocation().distanceSquared(target.getLocation()) > 36) {
            challenger.sendMessage("§cO jogador está muito longe para receber o desafio.");
            return false;
        }

        if (!hasSelectedKit(challenger)) {
            challenger.sendMessage("§cSelecione um kit antes de desafiar.");
            return false;
        }
        if (challenges.containsKey(uuid)) {
            challenger.sendMessage("§cVocê já possui um desafio pendente para responder.");
            return false;
        }
        if (hasOutgoingChallenge(uuid)) {
            challenger.sendMessage("§cVocê já enviou um desafio que aguarda resposta.");
            return false;
        }
        if (challenges.containsKey(targetUuid)) {
            challenger.sendMessage("§cEsse jogador já possui um desafio pendente.");
            return false;
        }
        if (DuelManager.isInDuel(challenger) || DuelManager.isInDuel(target)) {
            challenger.sendMessage("§cVocê ou esse jogador já está em um duelo.");
            return false;
        }

        KitType kit = selectedKit.remove(uuid);
        pendingTargets.remove(uuid);
        applyCooldown(challenger);

        challenges.put(targetUuid, new Challenge(uuid, kit));
        ChallengeStateManager.set(uuid, ChallengeState.PENDING_RESPONSE);

        challenger.sendMessage("§aDesafio enviado para §f" + target.getName()
                + " §acom §e" + kit.name());

        target.sendMessage("§e" + challenger.getName()
                + " §ate desafiou para §e" + kit.name());
        target.sendMessage("§7Use §a/accept §7ou §c/deny");

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!challenges.containsKey(targetUuid)) return;

                clear(target);
                challenger.sendMessage("§cO desafio expirou.");
                target.sendMessage("§cO desafio expirou.");
                ChallengeStateManager.clear(uuid);
            }
        };

        task.runTaskLater(plugin, 20L * 30);
        timeouts.put(targetUuid, task);
        return true;
    }

    /* ================= ACCEPT ================= */

    public static void accept(Player target) {

        if (EventManager.isInEvent(target)) {
            target.sendMessage("§cVocê não pode aceitar duelos enquanto estiver em um evento.");
            return;
        }

        Challenge challenge = challenges.get(target.getUniqueId());
        if (challenge == null) {
            target.sendMessage("§cVocê não tem desafios pendentes.");
            return;
        }

        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        KitType kit = challenge.getKit();

        clear(target);
        ChallengeStateManager.clear(challenge.getChallenger());

        if (challenger == null) {
            target.sendMessage("§cO jogador não está online.");
            return;
        }
        if (EventManager.isInEvent(challenger) || EventManager.isInEvent(target)) {
            target.sendMessage("§cO duelo foi cancelado porque um dos jogadores entrou em um evento.");
            challenger.sendMessage("§cO duelo foi cancelado porque um dos jogadores entrou em um evento.");
            resetCooldown(challenger);
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
            DuelManager.remove(challenger);
            DuelManager.remove(target);
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
        pendingTargets.remove(uuid);
    }
}
