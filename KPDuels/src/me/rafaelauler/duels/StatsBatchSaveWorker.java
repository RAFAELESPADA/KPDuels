package me.rafaelauler.duels;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.Bukkit;
public class StatsBatchSaveWorker implements Runnable {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    @Override
    public void run() {

        List<PlayerStats> batch = StatsSaveQueue.drain(BATCH_SIZE);

        if (batch.isEmpty()) return;

        try {
            DuelPlugin.getInstance()
                .getMySQL()
                .saveStatsBatch(batch);

            batch.forEach(PlayerStats::resetSaveAttempts);

        } catch (SQLException ex) {

            Bukkit.getLogger().severe(
                "[Duels] Falha ao salvar batch (" + batch.size() + " stats)"
            );

            for (PlayerStats stats : batch) {

                int attempt = stats.incrementSaveAttempts();

                if (attempt >= MAX_RETRIES) {
                    Bukkit.getLogger().severe(
                        "[Duels] Stats PERDIDOS para " + stats.getUuid()
                    );
                    continue;
                }

                long delay = (long) Math.pow(2, attempt) * 20L;

                Bukkit.getScheduler().runTaskLaterAsynchronously(
                    DuelPlugin.getInstance(),
                    () -> StatsSaveQueue.enqueue(stats),
                    delay
                );
            }
        }
    }
}
