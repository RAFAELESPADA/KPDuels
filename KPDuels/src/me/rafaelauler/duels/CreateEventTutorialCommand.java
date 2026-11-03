package me.rafaelauler.duels;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Exibe no chat o passo a passo para configurar e organizar um evento. */
public final class CreateEventTutorialCommand implements CommandExecutor {

    private static final String[] TUTORIAL = new String[] {
        "&8&m-----------------------------------------------------",
        "&b&lKPDuels &7» &fTUTORIAL COMPLETO DE EVENTOS",
        "&7Use esta sequência para preparar e executar um evento.",
        "",
        "&e&l1. CONFIGURE OS PONTOS GERAIS",
        "&fFique no local onde os jogadores devem aparecer ao entrar:",
        "&a/set-eventos-spawn",
        "&fDepois, fique no local para onde todos voltarão ao sair:",
        "&a/set-eventos-saida",
        "",
        "&e&l2. CONFIGURE AS ÁREAS DE CADA MODALIDADE",
        "&fDefina a área de espera de cada modalidade estando no local desejado:",
        "&a/seteventlocal lava &7- espera do Lava (entrega 36 sopas e remove armadura)",
        "&a/seteventlocal mdr &7- espera do MDR",
        "&a/seteventlocal 1v1 &7- espera do 1v1",
        "&a/seteventlocal sumo &7- espera do Sumo",
        "&fPara 1v1 e Sumo, configure também os dois lados do confronto:",
        "&a/seteventlocal 1v1 loc1 &7e &a/seteventlocal 1v1 loc2",
        "&a/seteventlocal sumo loc1 &7e &a/seteventlocal sumo loc2",
        "&7As posições loc1/loc2 são usadas para um confronto por modalidade.",
        "",
        "&e&l3. ABRA A SALA E RECEBA OS JOGADORES",
        "&a/evento abrir &7- abre a sala e inicia os anúncios globais configurados.",
        "&a/evento join &7- jogadores entram na sala (também: /evento entrar).",
        "&a/evento fechar &7- fecha a sala e interrompe imediatamente os anúncios.",
        "",
        "&e&l4. INICIE O EVENTO",
        "&a/evento start &7- inicia o countdown global configurado (padrão: 10 a 0).",
        "&7Os participantes ficam protegidos contra dano até o contador chegar a 0.",
        "&a/evento pvp on &7ou &c/evento pvp off &7- controla o PvP geral.",
        "",
        "&e&l5. PUXE OS JOGADORES PARA UMA MODALIDADE",
        "&a/puxarevento lava &7- leva todos à espera de Lava e dá inventário cheio de sopa, sem armadura.",
        "&a/puxarevento mdr &7- leva todos à área de MDR.",
        "&a/puxarevento 1v1 &7- leva todos à espera do 1v1.",
        "&a/evento puxar1v1 &7- forma um confronto usando loc1/loc2 e countdown de 5 a 0.",
        "&a/puxarevento sumo &7- leva todos à espera do Sumo.",
        "&a/evento puxarsumo &7- forma um confronto usando loc1/loc2 e countdown de 5 a 0.",
        "&7No 1v1: espada de diamante, armadura de ferro e sopas. No Sumo: hit com knockback, sem dano de vida.",
        "",
        "&e&l6. TERMINE E MARQUE O VENCEDOR",
        "&a/setwinner <jogador> &7- registra a vitória no MySQL e encerra o evento.",
        "&a/evento stop &7- encerra sem definir vencedor; também: /evento parar.",
        "&7Todos retornam ao local definido em /set-eventos-saida.",
        "",
        "&e&l7. PERSONALIZE O EVENTO NO CONFIG.YML",
        "&fEdite &eevents.broadcast.messages &fpara os anúncios globais.",
        "&fEdite &eevents.pull-locations.<modalidade>.instructions &fpara instruções após puxar.",
        "&fEdite &eevents.commands.on-join-player, on-stop e on-winner &fpara comandos automáticos.",
        "&fUse &e%kpduels_event_kills%&f, &e%kpduels_event_deaths%&f e &e%kpduels_esta_evento%&f nos plugins compatíveis.",
        "&fA permissão administrativa é &eevent.admin&f; bypass da whitelist: &eevents.blockedcmds&f.",
        "",
        "&e&l8. NOVAS MODALIDADES E PAINEL ADMINISTRATIVO",
        "&a/eventoadmin &7- abre o painel: sala, PvP, tempo, clima, kits, modalidades.",
        "&a/seteventlocal luckyblock|arena|sopa|arco|parkour|corrida|rei_do_morro",
        "&a/seteventlocal parkour finish &7- chegada do Parkour",
        "&a/seteventlocal corrida finish &7- chegada da Corrida",
        "&a/seteventlocal rei_do_morro point &7- ponto a capturar",
        "&a/puxarevento <modo> &7- transporta e equipa os participantes.",
        "&a/eventokit salvar <nome> &7- salva todos os itens do seu inventario.",
        "&a/eventokit dar <jogador> <nome> &7- entrega kit a qualquer momento.",
        "&a/eventokit vincular <modo> <nome> &7- kit automatico da modalidade.",
        "&eClima/hora: &7/eventoadmin mundo habilitar (somente mundo configurado).",
        "&7Ajustes completos em README_EVENTOS_AVANCADOS.md.",
        "",
        "&a&lDICA: &fconfigure e teste todos os locais antes de abrir a sala.",
        "&8&m-----------------------------------------------------"
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para isso. (event.admin)");
            return true;
        }
        for (String line : TUTORIAL) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        return true;
    }
}
