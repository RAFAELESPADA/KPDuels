
package me.rafaelauler.duels;

import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PullEventCommand implements CommandExecutor {

    private final EventManager events;
    private final EventExtras extras;

    public PullEventCommand(EventManager events, EventExtras extras) {
        this.events = events;
        this.extras = extras;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        // Compatibilidade com as duas permissões.
        if (!sender.hasPermission("event.admin")
                && !sender.hasPermission("events.admin")) {

            sender.sendMessage(
                "§cVocê não tem permissão para isso."
            );

            return true;
        }

        if (args.length != 1) {

            sender.sendMessage(
                "§cUse: /puxarevento <sumo|1v1|mdr|lava|spawn|luckyblock|arena|sopa|arco|parkour|corrida|rei_do_morro>"
            );

            return true;
        }

        String key = args[0].trim().toLowerCase(Locale.ROOT);

        if (!key.equals("sumo")
                && !key.equals("1v1")
                && !key.equals("mdr")
                && !key.equals("lava")
                && !key.equals("spawn")
                && !EventExtras.isExtraMode(key)) {

            sender.sendMessage(
                "§cModalidade inválida."
            );

            return true;
        }

        boolean success = EventExtras.isExtraMode(key) ? extras.pull(key) : events.pullPlayers(key);

        if (!success) {

            sender.sendMessage(
                "§cNão foi possível puxar os jogadores para "
                + key.toUpperCase() + "."
            );

            sender.sendMessage(
                "§7Verifique se existem participantes no evento."
            );

            if (key.equals("sumo") || key.equals("1v1")) {

                sender.sendMessage(
                    "§7Verifique os três locais: location, loc1 e loc2."
                );
            }

            return true;
        }

        sender.sendMessage(
            "§aSolicitação de transporte para "
            + key.toUpperCase()
            + " executada com sucesso!"
        );

        return true;
    }
}
