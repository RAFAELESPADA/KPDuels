package me.rafaelauler.duels;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventCommand implements CommandExecutor {

    private final EventManager events;
    private final EventExtras extras;

    public EventCommand(EventManager events, EventExtras extras) {
        this.events = events;
        this.extras = extras;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("entrar") || sub.equals("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cSomente jogadores podem entrar no evento.");
                return true;
            }
            events.join((Player) sender);
            return true;
        }
        if (sub.equals("sair") || sub.equals("leave")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cSomente jogadores podem sair do evento.");
                return true;
            }
            events.leave((Player) sender);
            return true;
        }

        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§cVocê não tem permissão para isso. (event.admin)");
            return true;
        }

        if (sub.equals("modo") && args.length == 2) {
            String mode = args[1].toLowerCase();
            if (!EventExtras.isExtraMode(mode) || !extras.selectMode(mode)) {
                sender.sendMessage("§cModo inválido ou evento já iniciado.");
            } else if (extras.pull(mode)) {
                sender.sendMessage("§aJogadores puxados para " + EventExtras.display(mode) + ".");
            } else {
                sender.sendMessage("§eModo selecionado, mas participantes ou locais especiais não foram encontrados.");
            }
            return true;
        }

        if (sub.equals("pvp")) {
            if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                sender.sendMessage("§cUse: /evento pvp on|off");
                return true;
            }
            boolean enabled = args[1].equalsIgnoreCase("on");
            events.setPvp(enabled);
            sender.sendMessage(enabled ? "§aPvP ativado." : "§cPvP desativado.");
            return true;
        }

        if (sub.equals("abrir") || sub.equals("open")) {
            events.openRoom();
            sender.sendMessage("§aSala de eventos aberta.");
            return true;
        }
        if (sub.equals("fechar") || sub.equals("close")) {
            events.closeRoom();
            sender.sendMessage("§cSala de eventos fechada. Os broadcasts foram interrompidos.");
            return true;
        }
        if (sub.equals("iniciar") || sub.equals("start")) {
            if (events.startEvent()) {
                sender.sendMessage("§aCountdown do evento iniciado.");
            } else {
                sender.sendMessage("§cNão foi possível iniciar: o evento já está iniciado ou não há participantes.");
            }
            return true;
        }
        if (sub.equals("parar") || sub.equals("stop")) {
            events.stopEvent(null);
            sender.sendMessage("§cEvento encerrado.");
            return true;
        }

        if (sub.equals("puxar1v1") || sub.equals("puxarsumo")
                || (sub.equals("puxar") && args.length >= 2)) {
            String mode = sub.equals("puxar1v1") ? "1v1"
                    : (sub.equals("puxarsumo") ? "sumo" : args[1].toLowerCase());
            if (!mode.equals("1v1") && !mode.equals("sumo")) {
                sender.sendMessage("§cUse: /evento puxar1v1 ou /evento puxarsumo");
                return true;
            }
            int matches = events.startPairMatches(mode);
            if (matches < 0) {
                sender.sendMessage("§cO evento precisa estar iniciado e os locais loc1/loc2 de " + mode + " precisam estar configurados.");
            } else if (matches == 0) {
                sender.sendMessage("§cNão há pelo menos dois participantes aguardando em " + mode + ". Use /puxarevento " + mode + " primeiro.");
            } else {
                sender.sendMessage("§aForam iniciados " + matches + " confronto(s) de " + mode + ".");
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§lKPDuels §7- Comandos de eventos");
        sender.sendMessage("§a/evento join §7- Entrar na sala de eventos");
        sender.sendMessage("§a/evento leave §7- Sair do evento");
        if (sender.hasPermission("event.admin")) {
            sender.sendMessage("§e/evento abrir|fechar §7- Abrir ou fechar a sala");
            sender.sendMessage("§e/evento pvp on|off §7- Ativar/desativar PvP");
            sender.sendMessage("§e/evento iniciar|parar §7- Iniciar ou encerrar o evento");
            sender.sendMessage("§e/evento puxar1v1|puxarsumo §7- Formar confrontos em pares");
            sender.sendMessage("§e/puxarevento <sumo|1v1|mdr|lava|spawn> §7- Levar participantes à espera");
            sender.sendMessage("§e/eventoadmin §7- Painel completo, novos modos e controle de mundo");
            sender.sendMessage("§e/eventokit §7- Salvar e aplicar kits completos");
            sender.sendMessage("§e/evento modo <modo> §7- Puxar jogadores para um novo modo");
            sender.sendMessage("§e/setwinner <jogador>");
            sender.sendMessage("§e/set-eventos-spawn §7- Definir entrada dos eventos");
            sender.sendMessage("§e/set-eventos-saida §7- Definir saída dos eventos");
            sender.sendMessage("§e/seteventlocal <local> §7- Definir área de espera");
            sender.sendMessage("§e/seteventlocal <1v1|sumo> <loc1|loc2> §7- Definir posições dos confrontos");
        }
    }
}
