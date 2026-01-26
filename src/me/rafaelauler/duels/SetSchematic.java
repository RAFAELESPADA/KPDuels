package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class SetSchematic implements CommandExecutor {

    private WorldEditPlugin we() {
        return (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("duels.admin")) {
            p.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length != 2) {
            p.sendMessage("§cUse /setschematic <arena> <nome>");
            return true;
        }

        String arenaName = args[0].toLowerCase();
        String schematicName = args[1];

        Arena arena = ArenaManager.getArenas().stream()
                .filter(a -> a.getName().equals(arenaName))
                .findFirst().orElse(null);

        if (arena == null) {
            p.sendMessage("§cArena não encontrada.");
            return true;
        }

        arena.setSchematic(schematicName);
        ArenaManager.saveArena(arena);

        p.sendMessage("§aSchematic da arena §f" + arenaName + " §asetada para §f" + schematicName);
        return true;
    }
}
