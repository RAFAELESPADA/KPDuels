package me.rafaelauler.duels;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

public class SetDuelArena implements CommandExecutor {

    private static final String PREFIX = "§8[§bDuels§8] ";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cEste comando só pode ser utilizado por um jogador.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(PREFIX + "§cSem permissão.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(PREFIX + "§cUse: /setduelarena <nome> <kit>");
            return true;
        }

        // Confere se o WorldEdit está instalado e habilitado.
        Plugin worldEditPlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEditPlugin == null || !worldEditPlugin.isEnabled()) {
            player.sendMessage(PREFIX + "§cO WorldEdit não está instalado ou habilitado.");
            return true;
        }

        String arenaName = args[0].toLowerCase(Locale.ROOT);
        if (!arenaName.matches("[a-z0-9_-]{1,32}")) {
            player.sendMessage(PREFIX + "§cNome inválido. Use até 32 caracteres: letras, números, _ ou -.");
            return true;
        }

        final KitType kit;
        try {
            kit = KitType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(PREFIX + "§cKit inválido: §f" + args[1]);
            return true;
        }

        // API moderna do WorldEdit: obtém a sessão e a região selecionada.
        final Region selection;
        try {
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(BukkitAdapter.adapt(player));

            selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException exception) {
            player.sendMessage(PREFIX + "§cSelecione os dois pontos da arena com a ferramenta do WorldEdit.");
            return true;
        } catch (Exception exception) {
            Bukkit.getLogger().severe("[Duels] Erro ao obter a seleção do WorldEdit para " + player.getName());
            exception.printStackTrace();
            player.sendMessage(PREFIX + "§cNão foi possível ler sua seleção do WorldEdit. Veja o console.");
            return true;
        }

        // Converte os vetores do WorldEdit para Locations do Bukkit.
        BlockVector3 minimum = selection.getMinimumPoint();
        BlockVector3 maximum = selection.getMaximumPoint();

        Location pos1 = new Location(
                player.getWorld(),
                minimum.x(),
                minimum.y(),
                minimum.z()
        );

        Location pos2 = new Location(
                player.getWorld(),
                maximum.x(),
                maximum.y(),
                maximum.z()
        );

        // Os dois spawns começam na posição do administrador.
        // Depois, /setduelspawn define cada spawn individualmente.
        Location initialSpawn = player.getLocation().clone();
        Arena arena = new Arena(
                arenaName,
                kit,
                pos1,
                pos2,
                initialSpawn.clone(),
                initialSpawn.clone()
        );

        ArenaManager.add(arena);
        ArenaManager.saveArena(arena);

        player.sendMessage(PREFIX + "§aArena §f" + arenaName + " §acriada com sucesso!");
        player.sendMessage("§7Kit: §f" + kit.name());
        player.sendMessage("§7Mundo: §f" + player.getWorld().getName());
        player.sendMessage("§7Ponto 1: §f" + minimum.x() + ", " + minimum.y() + ", " + minimum.z());
        player.sendMessage("§7Ponto 2: §f" + maximum.x() + ", " + maximum.y() + ", " + maximum.z());
        player.sendMessage("§7Defina os spawns com:");
        player.sendMessage("§f/setduelspawn " + arenaName + " 1");
        player.sendMessage("§f/setduelspawn " + arenaName + " 2");

        if (kit == KitType.GLADIATOR) {
            player.sendMessage("§6Gladiator: §7a jaula 56x7x56 será criada automaticamente no centro dos spawns.");
            player.sendMessage("§7Defina os dois spawns em lados opostos, na mesma altura, dentro da seleção.");
            player.sendMessage("§7A região do WorldEdit precisa comportar toda a jaula.");
        }

        return true;
    }
}
