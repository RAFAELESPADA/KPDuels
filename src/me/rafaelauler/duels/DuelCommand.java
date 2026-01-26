package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class DuelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Somente jogadores!");
            return true;
        }

        Player p = (Player) sender;
        if (ChallengeManager.inCooldown(p)) {
            p.sendMessage("§cAguarde §f" + ChallengeManager.getCooldown(p) + "s §cpara desafiar novamente.");
            return true;
        }
        DuelGUI.open(p);
        return true;
    }
}
