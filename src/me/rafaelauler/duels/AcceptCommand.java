package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player target = (Player) sender;

        if (!ChallengeManager.hasChallenge(target)) {
            target.sendMessage("§cVocê não tem desafios pendentes!");
            return true;
        }

        if (args.length == 0) {
            target.sendMessage("§cSelecione um kit!");
            return true;
        }

        KitType kit;
        try {
            kit = KitType.valueOf(args[0].toUpperCase());
        } catch (Exception e) {
            target.sendMessage("§cKit inválido!");
            return true;
        }

        Player challenger = ChallengeManager.getChallenger(target);
        ChallengeManager.cancelSelecting(target);

        if (challenger == null) {
            target.sendMessage("§cO jogador não está mais online.");
            return true;
        }

        Duel duel = new Duel(
                challenger,
                target,
                ArenaManager.get(),
                kit
        );

        DuelManager.add(duel);
        return true;
    }
}
