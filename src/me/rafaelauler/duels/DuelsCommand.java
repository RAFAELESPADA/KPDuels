package me.rafaelauler.duels;


import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelsCommand implements CommandExecutor {

    private static final String DUELS_WORLD = "duels";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Somente jogadores.");
            return true;
        }

        Player p = (Player) sender;

        // ❌ Bloquear se estiver em duelo
        if (DuelManager.isInDuel(p)) {
            p.sendMessage("§cVocê não pode sair enquanto está em um duelo!");
            return true;
        }

        World world = Bukkit.getWorld(DUELS_WORLD);

        if (world == null) {
            p.sendMessage("§cMundo de duels não encontrado.");
            return true;
        }

        // 📍 Teleportar para o spawn do mundo
        p.teleport(world.getSpawnLocation());

        // 🧹 Limpar inventário
        p.getInventory().clear();

        // 🎒 Dar itens do lobby
        LobbyItems.give(p);

        p.sendMessage("§aVocê entrou no lobby de Duels!");
        return true;
    }
}
