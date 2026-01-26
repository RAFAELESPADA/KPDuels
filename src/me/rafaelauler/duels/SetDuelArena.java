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

        if (args.length != 1) {
            p.sendMessage("§cUse /setduelarena <nome>");
            return true;
        }

        Selection sel = we().getSelection(p);
        if (sel == null) {
            p.sendMessage("§cSelecione a área com o WorldEdit primeiro.");
            return true;
        }

        Location pos1 = sel.getMinimumPoint();
        Location pos2 = sel.getMaximumPoint();
        String arenaName = args[0].toLowerCase();

        Arena arena = new Arena(arenaName, pos1, pos2, p.getLocation(), p.getLocation());
        ArenaManager.add(arena);
        ArenaManager.saveArena(arena);

        p.sendMessage("§aArena §f" + arenaName + " §acriada com sucesso!");
        p.sendMessage("§7Agora você pode definir o spawn com /setduelspawn <arena> <1|2> e salvar a schematic com /setschematic <arena> <nome>");
        return true;
    }
}
