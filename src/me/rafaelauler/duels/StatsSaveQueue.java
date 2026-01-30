package me.rafaelauler.duels;


import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StatsSaveQueue {

    private static final Queue<PlayerStats> QUEUE = new ConcurrentLinkedQueue<>();

    private StatsSaveQueue() {}

    public static void enqueue(PlayerStats stats) {
        if (stats != null) {
            QUEUE.add(stats);
        }
    }

    public static PlayerStats poll() {
        return QUEUE.poll();
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }
    public static List<PlayerStats> drain(int max) {

        List<PlayerStats> list = new ArrayList<>(max);

        for (int i = 0; i < max; i++) {
            PlayerStats stats = QUEUE.poll();
            if (stats == null) break;
            list.add(stats);
        }

        return list;
    }
}

