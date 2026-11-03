package me.rafaelauler.duels;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TickGuard {

    private static final Map<UUID, Integer> LAST_TICK = new ConcurrentHashMap<>();

    public static boolean allow(UUID uuid) {

        int now = ServerTick.get();
        Integer last = LAST_TICK.put(uuid, now);

        return last == null || last != now;
    }

    public static void clear(UUID uuid) {
        LAST_TICK.remove(uuid);
    }
}
