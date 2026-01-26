package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("duels.admin")) {
            p.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length != 2) {
            p.sendMessage("§cUse /setduelspawn <arena> <1|2>");
            return true;
        }

        String arenaName = args[0].toLowerCase();
        int spawn;
        try { spawn = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) {
            p.sendMessage("§cUse 1 ou 2 para o spawn.");
            return true;
        }

        Arena arena = ArenaManager.getArenas().stream()
                .filter(a -> a.getName().equals(arenaName))
                .findFirst().orElse(null);

        if (arena == null) {
            p.sendMessage("§cArena não encontrada.");
            return true;
        }

        if (spawn == 1) arena.setSpawn1(p.getLocation());
        else if (spawn == 2) arena.setSpawn2(p.getLocation());
        else {
            p.sendMessage("§cUse apenas 1 ou 2.");
            return true;
        }

        ArenaManager.saveArena(arena);
        p.sendMessage("§aSpawn " + spawn + " da arena §f" + arenaName + " §asetado!");
        return true;
    }
}
