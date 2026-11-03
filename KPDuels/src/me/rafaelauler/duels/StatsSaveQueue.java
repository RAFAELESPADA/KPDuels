package me.rafaelauler.duels;


import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StatsSaveQueue {


	    private static final Set<UUID> QUEUED = ConcurrentHashMap.newKeySet();
	    private static final Queue<PlayerStats> QUEUE = new ConcurrentLinkedQueue<>();

	    public static void enqueue(PlayerStats stats) {
	        if (!stats.isDirty()) return;

	        if (QUEUED.add(stats.getUuid())) {
	            QUEUE.add(stats);
	        }
	    }
	    public static PlayerStats poll() {
	        return QUEUE.poll(); // retorna null se estiver vazia
	    }
	    public static List<PlayerStats> drain(int max) {
	        List<PlayerStats> batch = new ArrayList<>(max);

	        while (batch.size() < max) {
	            PlayerStats stats = QUEUE.poll();
	            if (stats == null) break;

	            QUEUED.remove(stats.getUuid());
	            batch.add(stats);
	        }

	        return batch;
	    }

	    public static boolean isEmpty() {
	        return QUEUE.isEmpty();
	    }
	    public static int size() {
	        return QUEUE.size();
	    }
	}


