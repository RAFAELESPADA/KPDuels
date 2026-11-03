package me.rafaelauler.duels;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.session.ClipboardHolder;

public class SaveSchematic implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por um jogador.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUse: /saveschematic <nome>");
            return true;
        }

        // Aceita somente nomes simples para impedir caminhos como ../arquivo.
        String schematicName = args[0].toLowerCase(Locale.ROOT);
        if (schematicName.endsWith(".schem")) {
            schematicName = schematicName.substring(0, schematicName.length() - ".schem".length());
        }

        if (!schematicName.matches("[a-z0-9_-]{1,48}")) {
            player.sendMessage("§cNome inválido. Use apenas letras, números, '_' ou '-'.");
            return true;
        }

        Plugin worldEditPlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEditPlugin == null || !worldEditPlugin.isEnabled()) {
            player.sendMessage("§cWorldEdit não está instalado ou habilitado.");
            return true;
        }

        try {
            // A sessão do WorldEdit é obtida a partir do jogador Bukkit adaptado.
            LocalSession localSession = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(BukkitAdapter.adapt(player));

            final Clipboard clipboard;
            try {
                ClipboardHolder clipboardHolder = localSession.getClipboard();
                clipboard = clipboardHolder.getClipboard();
            } catch (EmptyClipboardException exception) {
                player.sendMessage("§cSua clipboard está vazia. Selecione a área e use //copy antes.");
                return true;
            }

            File schematicFolder = new File(worldEditPlugin.getDataFolder(), "schematics");
            Files.createDirectories(schematicFolder.toPath());

            Path schematicPath = new File(schematicFolder, schematicName + ".schem").toPath();

            // CREATE_NEW evita sobrescrever uma schematic existente acidentalmente.
            try (OutputStream outputStream = Files.newOutputStream(
                    schematicPath,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            ); ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(outputStream)) {
                writer.write(clipboard);
            } catch (FileAlreadyExistsException exception) {
                player.sendMessage("§cJá existe uma schematic com esse nome: §f" + schematicName + ".schem");
                return true;
            }

            player.sendMessage("§aSchematic salva com sucesso!");
            player.sendMessage("§7Arquivo: §f" + schematicName + ".schem");
            player.sendMessage("§7Local: §fplugins/WorldEdit/schematics/");

        } catch (IOException exception) {
            Bukkit.getLogger().severe("[Duels] Não foi possível salvar a schematic '" + schematicName + "'.");
            exception.printStackTrace();
            player.sendMessage("§cErro de I/O ao salvar a schematic. Verifique o console.");
        } catch (Exception exception) {
            Bukkit.getLogger().severe("[Duels] Erro ao salvar a schematic '" + schematicName + "'.");
            exception.printStackTrace();
            player.sendMessage("§cOcorreu um erro ao salvar a schematic. Verifique o console.");
        }

        return true;
    }
}
