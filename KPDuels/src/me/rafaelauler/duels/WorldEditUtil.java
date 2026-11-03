package me.rafaelauler.duels;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

public final class WorldEditUtil {

    private static final String SCHEMATICS_FOLDER = "schematics";

    private WorldEditUtil() {
        // Utility class.
    }

    /**
     * Cola uma schematic na localização informada.
     *
     * Aceita arquivos .schem e .schematic dentro da pasta
     * plugins/WorldEdit/schematics/.
     *
     * O método pode ser chamado de uma thread assíncrona: nesse caso,
     * a operação será agendada para a thread principal do servidor.
     */
    public static void paste(String schematic, Location location) {
        Plugin worldEditPlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEditPlugin == null || !worldEditPlugin.isEnabled()) {
            Bukkit.getLogger().severe("[Duels] WorldEdit não está instalado ou habilitado.");
            return;
        }

        if (schematic == null || schematic.isBlank()) {
            Bukkit.getLogger().warning("[Duels] Nome da schematic não informado.");
            return;
        }

        if (location == null || location.getWorld() == null) {
            Bukkit.getLogger().warning("[Duels] Localização inválida para colar a schematic.");
            return;
        }

        // WorldEdit altera blocos do mundo; a colagem deve ocorrer na thread principal.
        if (!Bukkit.isPrimaryThread()) {
            Location copiedLocation = location.clone();
            Bukkit.getScheduler().runTask(worldEditPlugin,
                    () -> paste(schematic, copiedLocation));
            return;
        }

        try {
            File file = resolveSchematicFile(worldEditPlugin, schematic);
            if (file == null) {
                Bukkit.getLogger().warning(
                        "[Duels] Schematic não encontrada: " + schematic
                                + " (pasta: " + new File(worldEditPlugin.getDataFolder(), SCHEMATICS_FOLDER)
                                .getPath() + ")"
                );
                return;
            }

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                Bukkit.getLogger().warning(
                        "[Duels] Formato de schematic não reconhecido: " + file.getName()
                );
                return;
            }

            Clipboard clipboard;
            try (InputStream input = Files.newInputStream(file.toPath());
                 ClipboardReader reader = format.getReader(input)) {
                clipboard = reader.read();
            }

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(
                                location.getBlockX(),
                                location.getBlockY(),
                                location.getBlockZ()
                        ))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
            }

            Bukkit.getLogger().info(
                    "[Duels] Schematic " + file.getName() + " colada em "
                            + location.getWorld().getName() + " ("
                            + location.getBlockX() + ", "
                            + location.getBlockY() + ", "
                            + location.getBlockZ() + ")."
            );

        } catch (IOException exception) {
            Bukkit.getLogger().severe(
                    "[Duels] Não foi possível ler a schematic " + schematic + ": "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        } catch (Exception exception) {
            Bukkit.getLogger().severe(
                    "[Duels] Erro ao colar a schematic " + schematic + ": "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    /**
     * Resolve .schem (preferencial) ou .schematic (formato legado),
     * impedindo que o nome escape da pasta de schematics.
     */
    private static File resolveSchematicFile(Plugin worldEditPlugin, String schematic)
            throws IOException {
        String name = schematic.trim();
        boolean hasExtension = name.endsWith(".schem") || name.endsWith(".schematic");

        Path directory = new File(worldEditPlugin.getDataFolder(), SCHEMATICS_FOLDER)
                .toPath()
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(directory);

        if (hasExtension) {
            Path requested = directory.resolve(name).normalize();
            if (!requested.startsWith(directory)) {
                Bukkit.getLogger().warning("[Duels] Caminho de schematic inválido: " + schematic);
                return null;
            }
            return Files.isRegularFile(requested) ? requested.toFile() : null;
        }

        Path modern = directory.resolve(name + ".schem").normalize();
        if (!modern.startsWith(directory)) {
            Bukkit.getLogger().warning("[Duels] Caminho de schematic inválido: " + schematic);
            return null;
        }
        if (Files.isRegularFile(modern)) {
            return modern.toFile();
        }

        Path legacy = directory.resolve(name + ".schematic").normalize();
        if (!legacy.startsWith(directory)) {
            Bukkit.getLogger().warning("[Duels] Caminho de schematic inválido: " + schematic);
            return null;
        }
        return Files.isRegularFile(legacy) ? legacy.toFile() : null;
    }
}
