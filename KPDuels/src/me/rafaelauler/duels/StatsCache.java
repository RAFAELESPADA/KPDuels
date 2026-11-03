package me.rafaelauler.duels;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsCache {

    private static final Map<UUID, PlayerStats> CACHE = new ConcurrentHashMap<>();

    public static PlayerStats get(UUID uuid) {
        return CACHE.getOrDefault(uuid, new PlayerStats(uuid, 0, 0, 0));
    }

    public static void put(PlayerStats stats) {
        CACHE.put(stats.getUuid(), stats);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }
}
