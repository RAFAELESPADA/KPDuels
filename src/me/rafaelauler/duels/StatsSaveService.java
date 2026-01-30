package me.rafaelauler.duels;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

public final class StatsSaveService {

    private static final int MAX_RETRIES = 5;
    private static final int MAX_QUEUE_SIZE = 10_000;

    private static final BlockingQueue<PlayerStats> QUEUE =
        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "duels-stats-writer");
            t.setDaemon(true);
            return t;
        });

    private static volatile boolean shuttingDown = false;

    /* ================= API ================= */

    public static boolean enqueue(PlayerStats stats) {
        if (stats == null || shuttingDown) return false;
        return QUEUE.offer(stats);
    }

    public static void start() {
        EXECUTOR.execute(StatsSaveService::process);
    }

    public static void shutdownAndFlush() {

        shuttingDown = true;

        Bukkit.getLogger().info("[Duels] Flush final de stats...");

        PlayerStats stats;
        while ((stats = QUEUE.poll()) != null) {
            trySave(stats);
        }

        EXECUTOR.shutdown();
    }

    /* ================= WORKER ================= */

    private static void process() {
        while (!shuttingDown) {
            try {
                PlayerStats stats = QUEUE.take();
                trySave(stats);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /* ================= RETRY ================= */

    private static void trySave(PlayerStats stats) {

        try {
            DuelPlugin.getInstance()
                .getMySQL()
                .saveStats(stats);

            stats.resetSaveAttempts();

        } catch (Exception ex) {

            if (!(ex.getCause() instanceof SQLException)) {
                Bukkit.getLogger().severe(
                    "[Duels] Erro FATAL ao salvar stats "
                    + stats.getUuid()
                );
                ex.printStackTrace();
                return;
            }

            int attempt = stats.incrementSaveAttempts();

            if (attempt >= MAX_RETRIES) {
                Bukkit.getLogger().severe(
                    "[Duels] ❌ Stats descartados após "
                    + attempt + " tentativas: "
                    + stats.getUuid()
                );
                return;
            }

            long delay = backoff(attempt);

            EXECUTOR.schedule(
                () -> enqueue(stats),
                delay,
                TimeUnit.MILLISECONDS
            );
        }
    }

    private static long backoff(int attempt) {
        long base = (1L << attempt) * 500;
        long jitter = ThreadLocalRandom.current().nextLong(200);
        return Math.min(base + jitter, 15_000);
    }
}
