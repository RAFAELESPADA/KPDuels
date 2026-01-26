package me.rafaelauler.duels;

import java.io.File;
import java.io.FileInputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

public class WorldEditUtil {

    private static WorldEditPlugin we() {
        return (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    public static void paste(String schematic, Location loc) {

        try {

            File file = new File(
                    we().getDataFolder(),
                    "schematics/" + schematic + ".schematic"
            );

            if (!file.exists()) {
                Bukkit.getLogger().warning("[Duels] Schematic não encontrada: " + file.getName());
                return;
            }

            ClipboardFormat format = ClipboardFormat.findByFile(file);
            if (format == null) {
                Bukkit.getLogger().warning("[Duels] Formato inválido da schematic.");
                return;
            }

            WorldData worldData = BukkitUtil.getLocalWorld(loc.getWorld()).getWorldData();

            ClipboardReader reader = format.getReader(new FileInputStream(file));
            Clipboard clipboard = reader.read(worldData);

            EditSession session = WorldEdit.getInstance()
                    .getEditSessionFactory()
                    .getEditSession(
                            BukkitUtil.getLocalWorld(loc.getWorld()),
                            -1
                    );

            ClipboardHolder holder = new ClipboardHolder(clipboard, worldData);

            holder.createPaste(session, worldData)
                    .to(new Vector(
                            loc.getBlockX(),
                            loc.getBlockY(),
                            loc.getBlockZ()
                    ))
                    .ignoreAirBlocks(false)
                    .build();

            session.flushQueue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
