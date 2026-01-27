package me.rafaelauler.duels;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class SetDuelArena implements CommandExecutor {

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
            p.sendMessage("§cUse: /setduelarena <nome> <kit>");
            return true;
        }

        Selection sel = we().getSelection(p);
        if (sel == null) {
            p.sendMessage("§cSelecione a área com o WorldEdit primeiro.");
            return true;
        }

        String arenaName = args[0].toLowerCase();
        KitType kit;

        try {
            kit = KitType.valueOf(args[1].toUpperCase());
        } catch (Exception e) {
            p.sendMessage("§cKit inválido.");
            return true;
        }

        Location pos1 = sel.getMinimumPoint();
        Location pos2 = sel.getMaximumPoint();

        Location spawn = p.getLocation();

        Arena arena = new Arena(arenaName, kit, pos1, pos2, spawn, spawn);


        ArenaManager.add(arena);
        ArenaManager.saveArena(arena);

        p.sendMessage("§aArena §f" + arenaName + " §acriada com sucesso!");
        p.sendMessage("§7Kit: §f" + kit.name());
        p.sendMessage("§7Defina os spawns com:");
        p.sendMessage("§f/setduelspawn " + arenaName + " 1");
        p.sendMessage("§f/setduelspawn " + arenaName + " 2");

        return true;
    }
}
