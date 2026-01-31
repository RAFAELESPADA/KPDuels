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
    	    if (!toRetry.isDirty()) {
    	        continue;
    	    }
    	    try {
    	        DuelPlugin.getInstance()
    	            .getMySQL()
    	            .saveStats(toRetry);

    	        toRetry.resetSaveAttempts();

    	    } catch (Exception ex) {

    	    	boolean isSqlError =
    	    		    ex instanceof SQLException ||
    	    		    ex.getCause() instanceof SQLException;

    	    		if (!isSqlError) {
    	    		    Bukkit.getLogger().severe(
    	    		        "[Duels] Erro NÃO SQL ao salvar stats de " + toRetry.getUuid()
    	    		    );
    	    		    ex.printStackTrace();
    	    		    continue;
    	    		}

    	        attempt++;
    	        toRetry.setSaveAttempts(attempt);
    	        toRetry.markClean();
    	        if (attempt >= MAX_RETRIES) {
    	            Bukkit.getLogger().severe(
    	                "[Duels] ❌ Stats descartados para "
    	                + toRetry.getUuid()
    	                + " após " + attempt + " tentativas"
    	            );
    	            continue;
    	        }

    	        long delay = Math.min(
    	        	    (1L << attempt) * 10L,
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
