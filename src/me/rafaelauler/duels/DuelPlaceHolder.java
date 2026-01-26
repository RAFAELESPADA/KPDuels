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

        switch (identifier.toLowerCase()) {

            case "opponent":
                if (DuelManager.isInDuel(p)) {
                    return DuelManager.get(p).getOpponent(p).getName();
                }
                return "§cNenhum";

            case "hits":
                if (DuelManager.isInDuel(p) && DuelManager.get(p).getKit() == KitType.BOXING) {
                    return String.valueOf(DuelManager.getHits(p));
                }
                return "0";

            case "kit":
                if (DuelManager.isInDuel(p)) {
                    return DuelManager.get(p).getKit().name();
                }
                return "Nenhum";

            case "status":
                if (DuelManager.isInDuel(p)) {
                    return DuelManager.get(p).getState().name();
                }
                return "Nenhum";
        
    case "online":
        // Conta todos os players que estão em duelo
        int playersInDuels = DuelManager.getAllDuels().stream()
                .mapToInt(d -> d.getPlayers().length) // cada duelo tem p1 e p2
                .sum();
        return String.valueOf(playersInDuels);
}
        return null;
    }
}

