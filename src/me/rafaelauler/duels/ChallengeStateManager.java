package me.rafaelauler.duels;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeStateManager {

    private static final Map<UUID, ChallengeState> STATES = new ConcurrentHashMap<>();

    public static ChallengeState get(UUID uuid) {
        return STATES.getOrDefault(uuid, ChallengeState.NONE);
    }

    public static void set(UUID uuid, ChallengeState state) {
        STATES.put(uuid, state);
    }

    public static void clear(UUID uuid) {
        STATES.remove(uuid);
    }

    public static boolean is(UUID uuid, ChallengeState state) {
        return get(uuid) == state;
    }
}
