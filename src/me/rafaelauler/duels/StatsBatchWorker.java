package me.rafaelauler.duels;

import java.util.List;

public class StatsBatchWorker implements Runnable {

    private static final int BATCH_SIZE = 50;

    @Override
    public void run() {

        try {
            List<PlayerStats> batch =
                StatsSaveQueue.drain(BATCH_SIZE);

            if (batch.isEmpty()) return;

            DuelPlugin.getInstance()
                .getMySQL()
                .saveStatsBatch(batch);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
