
package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetEventLocationCommand implements CommandExecutor {

    public enum Type {
        SPAWN,
        EXIT,
        PULL
    }

    private final EventManager events;
    private final Type type;

    public SetEventLocationCommand(EventManager events, Type type) {
        this.events = events;
        this.type = type;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

    	if (!sender.hasPermission("event.admin")
    	        && !sender.hasPermission("events.admin")) {
            sender.sendMessage(
                "§cVocê não tem permissão para isso. (event.admin)"
            );
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                "§cEste comando deve ser usado por um jogador."
            );
            return true;
        }

        Player player = (Player) sender;

        if (type == Type.PULL) {

            if (args.length < 1 || args.length > 2) {
                sendPullHelp(player);
                return true;
            }

            String key = args[0].toLowerCase();

            // =====================================
            // LOCAL EXCLUSIVO DOS ADMINS DO MDR
            // =====================================

            if (key.equals("mdr-admin")) {

                if (args.length != 1) {
                    player.sendMessage(
                        "§cUse: /seteventlocal mdr-admin"
                    );
                    return true;
                }

                events.setMdrAdminLocation(
                    player.getLocation()
                );

                player.sendMessage(
                    "§aLocal exclusivo dos administradores do MDR configurado!"
                );

                player.sendMessage(
                    "§7Jogadores com events.admin nascerão neste local."
                );

                return true;
            }

            // =====================================
            // LOCAIS NORMAIS
            // =====================================

            if (!key.matches("[a-z0-9_-]+")) {
                player.sendMessage(
                    "§cNome de local inválido."
                );
                return true;
            }

            if (args.length == 2) {

                String side = args[1].toLowerCase();

                boolean paired = (key.equals("1v1") || key.equals("sumo"))
                        && (side.equals("loc1") || side.equals("loc2"));
                boolean finish = (key.equals("corrida") || key.equals("parkour")) && side.equals("finish");
                boolean point = key.equals("rei_do_morro") && side.equals("point");
                if (!paired && !finish && !point) {

                    player.sendMessage(
                        "§cUse: /seteventlocal <1v1|sumo> <loc1|loc2> ou <parkour|corrida> finish ou rei_do_morro point"
                    );
                    return true;
                }

                if (paired) events.setPullDuelSpawn(key, side, player.getLocation());
                else events.setExtraLocation(key, side, player.getLocation());

                player.sendMessage(
                    "§aPosição " + side
                    + " do evento " + key + " definida."
                );

            } else {

                events.setPullLocation(
                    key,
                    player.getLocation()
                );

                player.sendMessage(
                    "§aÁrea de espera de '" + key
                    + "' definida nesta posição."
                );
            }

            return true;
        }

        // =====================================
        // SPAWN PRINCIPAL DOS EVENTOS
        // =====================================

        if (type == Type.SPAWN) {

            events.setEventSpawn(player.getLocation());

            player.sendMessage(
                "§aSpawn de entrada dos eventos definido."
            );

            return true;
        }

        // =====================================
        // SAÍDA DOS EVENTOS
        // =====================================

        if (type == Type.EXIT) {

            events.setExit(player.getLocation());

            player.sendMessage(
                "§aLocal de saída dos eventos definido."
            );

            return true;
        }

        return true;
    }

    private void sendPullHelp(Player player) {

        player.sendMessage("§e§lKPDuels §7- Configuração de locais");

        player.sendMessage(
            "§e/seteventlocal <local> §7- Definir local de modalidade"
        );

        player.sendMessage(
            "§e/seteventlocal <1v1|sumo> <loc1|loc2> §7- Definir arena"
        );

        player.sendMessage(
            "§e/seteventlocal <parkour|corrida> finish §7- Linha de chegada"
        );
        player.sendMessage(
            "§e/seteventlocal rei_do_morro point §7- Ponto de captura"
        );
        player.sendMessage(
            "§e/seteventlocal mdr-admin §7- Spawn exclusivo dos admins MDR"
        );
    }
}
