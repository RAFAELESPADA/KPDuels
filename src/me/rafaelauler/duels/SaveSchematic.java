package me.rafaelauler.duels;

import java.io.File;
import java.io.FileOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

public class SaveSchematic implements CommandExecutor {

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
            p.sendMessage("§cUse /saveschematic <nome>");
            return true;
        }

        try {
            Selection sel = we().getSelection(p);
            if (sel == null) {
                p.sendMessage("§cSelecione a área com o WorldEdit primeiro!");
                return true;
            }

            File folder = new File(we().getDataFolder(), "schematics");
            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, args[0] + ".schematic");

            LocalSession session = we().getWorldEdit().getSession(p.getName());
            ClipboardHolder clipboardHolder = session.getClipboard();

            if (clipboardHolder == null) {
                p.sendMessage("§cErro: use //copy antes de salvar a schematic.");
                return true;
            }

            Clipboard clipboard = clipboardHolder.getClipboard();
            WorldData worldData = BukkitUtil.getLocalWorld(p.getWorld()).getWorldData();

            try (ClipboardWriter writer = ClipboardFormat.findByFile(file).getWriter(new FileOutputStream(file))) {
                writer.write(clipboard, worldData);
            }

            p.sendMessage("§aSchematic salva com sucesso: §f" + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
            p.sendMessage("§cOcorreu um erro ao salvar a schematic.");
        }

        return true;
    }
}
