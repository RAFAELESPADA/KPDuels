package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Duel {

    private final Player p1;
    private final Player p2;
    private final Arena arena;
    private final KitType kit;
    private DuelState state;

    public Duel(Player p1, Player p2, Arena arena, KitType kit) {
        this.p1 = p1;
        this.p2 = p2;
        this.arena = arena;
        this.kit = kit;
        start();
    }

    private void start() {

        state = DuelState.STARTING;

        // Teleporta players
        arena.teleport(p1, p2);

        preparePlayer(p1);
        preparePlayer(p2);

        kit.apply(p1);
        kit.apply(p2);

        // Congela os players
        freeze(p1, true);
        freeze(p2, true);

        // Contagem 3…2…1
        new BukkitRunnable() {

            int count = 3;

            @Override
            public void run() {

                if (state != DuelState.STARTING) {
                    cancel();
                    return;
                }

                if (count > 0) {
                    sendCountdown(count);
                    count--;
                    return;
                }

                // Começa o duelo
                freeze(p1, false);
                freeze(p2, false);

                p1.playSound(p1.getLocation(), Sound.valueOf("NOTE_PLING"), 1f, 2f);
                p2.playSound(p2.getLocation(), Sound.valueOf("NOTE_PLING"), 1f, 2f);

                TitleUtil.send(p1, "§aLUTAR!", "§7Boa sorte!");
                TitleUtil.send(p2, "§aLUTAR!", "§7Boa sorte!");

                state = DuelState.FIGHTING;
                cancel();
            }

            private void sendCountdown(int i) {

                Sound sound = Sound.valueOf("NOTE_PLING");

                p1.playSound(p1.getLocation(), sound, 1f, 1f);
                p2.playSound(p2.getLocation(), sound, 1f, 1f);

                // Partículas
                countdownParticles(p1);
                countdownParticles(p2);

                TitleUtil.send(p1, "§e" + i, "§7Prepare-se...");
                TitleUtil.send(p2, "§e" + i, "§7Prepare-se...");
            }

        }.runTaskTimer(DuelPlugin.getPlugin(DuelPlugin.class), 0L, 20L);
    }

    private void countdownParticles(Player p) {
        p.getWorld().playEffect(
                p.getLocation().add(0, 1.2, 0),
                Effect.HAPPY_VILLAGER,
                0
        );
    }

    public void end(Player winner) {

        if (state == DuelState.ENDED) return;
        state = DuelState.ENDED;

        Player loser = getOpponent(winner);

        winner.sendMessage("§aVocê venceu!");
        loser.sendMessage("§cVocê perdeu!");

        resetPlayer(winner);
        resetPlayer(loser);

        // Reset arena automaticamente via WorldEdit
        if (arena.getSchematic() != null) {
            WorldEditUtil.paste(arena.getSchematic(), arena.getSpawn1());
        }

        arena.release();
    }

    public Player[] getPlayers() {
        return new Player[] { p1, p2 };
    }

    private void preparePlayer(Player p) {
        p.closeInventory();
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    private void resetPlayer(Player p) {
        preparePlayer(p);
        p.teleport(Bukkit.getWorld("duels").getSpawnLocation());
    }

    private void freeze(Player p, boolean freeze) {
        p.setWalkSpeed(freeze ? 0f : 0.2f);
        p.setFlySpeed(freeze ? 0f : 0.1f);
    }

    public boolean hasPlayer(Player p) {
        return p.equals(p1) || p.equals(p2);
    }

    public Player getOpponent(Player p) {
        return p.equals(p1) ? p2 : p1;
    }

    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public KitType getKit() { return kit; }
    public DuelState getState() { return state; }
}
