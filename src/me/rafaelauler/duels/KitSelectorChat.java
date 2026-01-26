package me.rafaelauler.duels;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class KitSelectorChat {

    public static void send(Player challenger, Player target) {

        TextComponent base = new TextComponent("§e" + challenger.getName() + " te desafiou! Escolha um kit:\n");

        for (KitType kit : KitType.values()) {

            TextComponent kitButton = new TextComponent("§8» §a" + kit.getDisplay() + "\n");
            kitButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/accept " + kit.name()
            ));

            base.addExtra(kitButton);
        }

        target.spigot().sendMessage(base);
        challenger.sendMessage("§aDesafio enviado para §f" + target.getName());
    }
}

