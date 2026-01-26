package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DenyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return false;
        Player target = (Player) sender;

        if (!ChallengeManager.hasChallenge(target)) {
            target.sendMessage("§cVocê não tem nenhum desafio pendente!");
            return true;
        }

        Player challenger = ChallengeManager.getChallenger(target);
        ChallengeManager.deny(target);

        if (challenger != null) {
            challenger.sendMessage("§c" + target.getName() + " recusou seu desafio.");
        }

        target.sendMessage("§aDesafio recusado.");
        return true;
    }
}
