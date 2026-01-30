package me.rafaelauler.duels;

import org.bukkit.Bukkit;

public class StatsSaveTask implements Runnable {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 40L; // 2 segundos

    private final PlayerStats stats;
    private int attempt = 0;

    public StatsSaveTask(PlayerStats stats) {
        this.stats = stats;
    }

    @Override
    public void run() {
        try {
            DuelPlugin.getInstance()
                .getMySQL()
                .saveStats(stats);

        } catch (Exception ex) {

            attempt++;

            if (attempt >= MAX_RETRIES) {
                Bukkit.getLogger().severe(
                    "[Duels] Falha ao salvar stats de "
                    + stats.getUuid() + " após "
                    + attempt + " tentativas"
                );
                return;
            }

            Bukkit.getLogger().warning(
                "[Duels] Retry " + attempt
                + " ao salvar stats de "
                + stats.getUuid()
            );

            Bukkit.getScheduler().runTaskLaterAsynchronously(
                DuelPlugin.getInstance(),
                this,
                RETRY_DELAY
            );
        }
    }
}

