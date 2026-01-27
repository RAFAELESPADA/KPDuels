package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClickCooldown {

    private static final long COOLDOWN_MS = 700; // 0.7s
    private static final Map<UUID, Long> cooldown = new HashMap<>();

    public static boolean inCooldown(UUID uuid) {
        return cooldown.containsKey(uuid)
                && (System.currentTimeMillis() - cooldown.get(uuid)) < COOLDOWN_MS;
    }

    public static void apply(UUID uuid) {
        cooldown.put(uuid, System.currentTimeMillis());
    }

    public static void remove(UUID uuid) {
        cooldown.remove(uuid);
    }
}
