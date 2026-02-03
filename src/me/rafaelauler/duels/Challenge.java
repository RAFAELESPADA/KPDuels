package me.rafaelauler.duels;
import java.util.UUID;

public class Challenge {

    private final UUID challenger;
    private final KitType kit;

    public Challenge(UUID challenger, KitType kit) {
        this.challenger = challenger;
        this.kit = kit;
    }

    public UUID getChallenger() {
        return challenger;
    }

    public KitType getKit() {
        return kit;
    }
}
