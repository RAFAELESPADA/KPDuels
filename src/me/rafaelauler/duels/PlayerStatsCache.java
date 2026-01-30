package me.rafaelauler.duels;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsCache {

    private static final long TTL = 5 * 60 * 1000; // 5 minutos
    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();

    /* ========================= */

    public static void put(PlayerStats stats) {
        CACHE.put(stats.getUuid(), new CacheEntry(stats, System.currentTimeMillis()));
    }

    public static PlayerStats get(UUID uuid) {
        CacheEntry entry = CACHE.get(uuid);

        if (entry == null) return null;

        if (entry.isExpired()) {
            CACHE.remove(uuid);
            return null;
        }

        return entry.stats;
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static Collection<PlayerStats> getAll() {
        return CACHE.values()
            .stream()
            .map(e -> e.stats)
            .toList();
    }

    public static void clear() {
        CACHE.clear();
    }
    public static void cleanup() {
        CACHE.entrySet().removeIf(e -> e.getValue().isExpired());
    }
    /* ========================= */

    private static class CacheEntry {
        private final PlayerStats stats;
        private final long lastAccess;

        CacheEntry(PlayerStats stats, long lastAccess) {
            this.stats = stats;
            this.lastAccess = lastAccess;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAccess > TTL;
        }
    }
}
