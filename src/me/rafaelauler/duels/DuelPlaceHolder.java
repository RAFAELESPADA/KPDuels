package me.rafaelauler.duels;


import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class DuelPlaceHolder extends PlaceholderExpansion {

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "kpduels"; // O identificador que será usado nos placeholders: %duels_<name>%
    }

    @Override
    public String getAuthor() {
        return "Rafael";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (p == null) return "";

        PlayerStats stats = StatsCache.get(p.getUniqueId());

        switch (identifier.toLowerCase()) {

            case "wins":
                return String.valueOf(stats.getWins());

            case "losses":
                return String.valueOf(stats.getLosses());

            case "ws":
                return String.valueOf(stats.getWinstreak());

            case "opponent":
                if (DuelManager.isInDuel(p)) {
                    return DuelManager.get(p).getOpponent(p).getName();
                }
                return "§cNenhum";

            case "hits":
                if (DuelManager.isInDuel(p)
                        && DuelManager.get(p).getKit() == KitType.BOXING) {
                    return String.valueOf(DuelManager.getHits(p));
                }
                return "0";

            case "kit":
                return DuelManager.isInDuel(p)
                        ? DuelManager.get(p).getKit().name()
                        : "Nenhum";

            case "players":
                return String.valueOf(DuelsCommand.game.size());

            case "status":
                return DuelManager.isInDuel(p)
                        ? DuelManager.get(p).getState().name()
                        : "Nenhum";
            case "emduelo":
                return String.valueOf(
                    DuelManager.getAllDuels().stream()
                        .mapToInt(d -> d.getPlayers().length)
                        .sum()
                );
        }
            
        return "";
    
    }
}

