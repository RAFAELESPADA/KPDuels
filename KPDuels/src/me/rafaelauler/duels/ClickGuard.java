package me.rafaelauler.duels;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickGuard {

    private static final long COOLDOWN_MS = 300;

    private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();

    public static boolean allow(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = LAST_CLICK.getOrDefault(uuid, 0L);

        if (now - last < COOLDOWN_MS) {
            return false;
        }

        LAST_CLICK.put(uuid, now);
        return true;
    }

    public static void clear(UUID uuid) {
        LAST_CLICK.remove(uuid);
    }
}
