package me.rafaelauler.duels;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

public class PlayerStatsCache {

    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long TTL = 10 * 60 * 1000; // 10 min

    // 🔥 SOMENTE GET — nunca cria
    public static PlayerStats get(UUID uuid) {
        CacheEntry entry = CACHE.get(uuid);
        if (entry == null) return null;

        entry.touch();
        return entry.stats;
    }

    // 🔥 PUT explícito após load
    public static void put(PlayerStats stats) {
        CACHE.put(stats.getUuid(), new CacheEntry(stats));
    }
    public static void touch(UUID uuid) {
        CacheEntry entry = CACHE.get(uuid);
        if (entry != null) entry.touch();
    }
    public static boolean contains(UUID uuid) {
        return CACHE.containsKey(uuid);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    // 🔥 Usado SOMENTE no onDisable / batch save
    public static Collection<PlayerStats> getAll() {
        return CACHE.values()
            .stream()
            .map(e -> e.stats)
            .collect(Collectors.toList());
    }

    // 🔥 TTL seguro
    public static void cleanup() {
        long now = System.currentTimeMillis();

        CACHE.entrySet().removeIf(e -> {
            UUID uuid = e.getKey();
            CacheEntry entry = e.getValue();

            // nunca remove player online
            if (Bukkit.getPlayer(uuid) != null) return false;

            return now - entry.lastAccess > TTL;
        });
    }

    /* ========================= */

    private static final class CacheEntry {
        private final PlayerStats stats;
        private volatile long lastAccess;

        CacheEntry(PlayerStats stats) {
            this.stats = stats;
            touch();
        }

        void touch() {
            lastAccess = System.currentTimeMillis();
        }
    }
}
