package me.rafaelauler.duels;

import java.sql.SQLException;

import org.bukkit.Bukkit;

public class StatsSaveWorker implements Runnable {

    private static final int MAX_RETRIES = 5;
    private static final long MAX_DELAY_TICKS = 20L * 60; // 1 minuto

    @Override
    public void run() {
        PlayerStats stats;

    	while ((stats = StatsSaveQueue.poll()) != null) {

    	    final PlayerStats toRetry = stats;

    	    int attempt = toRetry.getSaveAttempts();

    	    try {
    	        DuelPlugin.getInstance()
    	            .getMySQL()
    	            .saveStats(toRetry);

    	        toRetry.resetSaveAttempts();

    	    } catch (Exception ex) {

    	        Throwable cause = ex.getCause();

    	        if (!(cause instanceof SQLException)) {
    	            Bukkit.getLogger().severe(
    	                "[Duels] Erro NÃO SQL ao salvar stats de "
    	                + toRetry.getUuid()
    	            );
    	            ex.printStackTrace();
    	            continue;
    	        }

    	        attempt++;
    	        toRetry.setSaveAttempts(attempt);

    	        if (attempt >= MAX_RETRIES) {
    	            Bukkit.getLogger().severe(
    	                "[Duels] ❌ Stats descartados para "
    	                + toRetry.getUuid()
    	                + " após " + attempt + " tentativas"
    	            );
    	            continue;
    	        }

    	        long delay = Math.min(
    	            (long) Math.pow(2, attempt) * 20L,
    	            MAX_DELAY_TICKS
    	        );

    	        Bukkit.getScheduler().runTaskLaterAsynchronously(
    	            DuelPlugin.getInstance(),
    	            () -> StatsSaveQueue.enqueue(toRetry),
    	            delay
    	        );
    	    }
    	
            
        }
    }
}
