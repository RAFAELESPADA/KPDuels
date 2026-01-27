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
            target.sendMessage("§cUse: /accept <kit>");
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

        // Limpa desafio + timeout
        ChallengeManager.clear(target);

        if (challenger == null) {
            target.sendMessage("§cO jogador não está mais online.");
            return true;
        }

        // Segurança extra
        if (DuelManager.isInDuel(challenger) || DuelManager.isInDuel(target)) {
            target.sendMessage("§cAlguém já está em um duelo.");
            return true;
        }

        Arena arena = ArenaManager.getFreeArena(kit);
        if (arena == null) {
            target.sendMessage("§cNenhuma arena disponível.");
            challenger.sendMessage("§cNenhuma arena disponível.");
            return true;
        }

     // Adiciona antes de start para que listeners reconheçam o jogador
        Duel duel = new Duel(challenger, target, arena, kit);
        DuelManager.add(duel);

        if (!duel.start()) {
            // Se start falhar, remove do manager e libera arena
            DuelManager.forceEnd(challenger); // isso chama end(null) e reset dos dois jogadores
            ArenaManager.release(arena);
            return true;
        }

        ChallengeManager.resetCooldown(challenger);

        return true;
    }
}
