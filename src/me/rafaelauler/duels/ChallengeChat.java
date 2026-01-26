package me.rafaelauler.duels;


import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChallengeChat {

    public static void sendChallenge(Player challenger, Player target) {

        TextComponent msg = new TextComponent("§e" + challenger.getName() + " te desafiou para um duelo! ");

        TextComponent accept = new TextComponent("§a[ACEITAR]");
        accept.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/accept"
        ));

        TextComponent deny = new TextComponent(" §c[NEGAR]");
        deny.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/deny"
        ));

        msg.addExtra(accept);
        msg.addExtra(deny);

        target.spigot().sendMessage(msg);

        challenger.sendMessage("§aDesafio enviado para §f" + target.getName());
    }
}
