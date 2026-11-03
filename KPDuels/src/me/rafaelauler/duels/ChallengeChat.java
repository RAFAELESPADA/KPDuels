package me.rafaelauler.duels;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChallengeChat {
    private static final long COOLDOWN_TIME = 10; // segundos

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
	public static boolean inCooldown(Player p) {

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        return (now - last) < TimeUnit.SECONDS.toMillis(COOLDOWN_TIME);
    }

    public static long getCooldown(Player p) {

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        long left = TimeUnit.SECONDS.toMillis(COOLDOWN_TIME) - (now - last);
        return Math.max(0, left / 1000);
    }

    private static void applyCooldown(Player p) {
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
    }
    public static void sendChallenge(Player challenger, Player target, KitType kt) {

        if (inCooldown(challenger)) {
            challenger.sendMessage("§cAguarde §f" + getCooldown(challenger) + "s §cpara desafiar novamente.");
            return;
        }

        TextComponent msg = new TextComponent("§e" + challenger.getName() + " te desafiou para um duelo! ");

        TextComponent accept = new TextComponent("§a[ACEITAR]");
        accept.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/accept " + kt.name().toLowerCase()
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

        applyCooldown(challenger);
    }

}
