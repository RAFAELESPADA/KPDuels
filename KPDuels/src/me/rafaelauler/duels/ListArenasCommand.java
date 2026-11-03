package me.rafaelauler.duels;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListArenasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("duels.admin")) {
            p.sendMessage("§cSem permissão.");
            return true;
        }

        if (ArenaManager.getArenas().isEmpty()) {
            p.sendMessage("§cNenhuma arena registrada.");
            return true;
        }

        p.sendMessage("§a§lARENAS DISPONÍVEIS:");

        for (Arena arena : ArenaManager.getArenas()) {

            String status = arena.isInUse()
                    ? "§c[EM USO]"
                    : "§a[LIVRE]";

            p.sendMessage("§f• §e" + arena.getName()
                    + " §7(§b" + arena.getKitType().name() + "§7) "
                    + status);
        }

        return true;
    }
}

