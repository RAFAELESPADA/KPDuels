package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWinnerCommand implements CommandExecutor {

    private final EventManager events;

    public SetWinnerCommand(EventManager events) {
        this.events = events;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§cVocê não tem permissão para isso. (event.admin)");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUse: /setwinner <jogador>");
            return true;
        }
        Player winner = Bukkit.getPlayerExact(args[0]);
        if (winner == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return true;
        }
        if (!events.isEventStarted()) {
            sender.sendMessage("§cO evento ainda não foi iniciado.");
            return true;
        }
        if (!events.isMember(winner) || events.isSpectator(winner)) {
            sender.sendMessage("§cO jogador precisa estar vivo e participando do evento.");
            return true;
        }
        events.setWinner(winner);
        sender.sendMessage("§a" + winner.getName() + " foi marcado como vencedor. Evento encerrado.");
        return true;
    }
}
