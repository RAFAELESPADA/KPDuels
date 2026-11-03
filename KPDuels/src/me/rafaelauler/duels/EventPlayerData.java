package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/** Guarda o estado do jogador antes de entrar no evento para restaurá-lo ao sair. */
final class EventPlayerData {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final int level;
    private final int totalExperience;
    private final float exp;
    private final int foodLevel;
    private final float saturation;
    private final double health;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final Collection<PotionEffect> potionEffects;

    private EventPlayerData(Player player) {
        PlayerInventory inventory = player.getInventory();
        this.contents = cloneItems(inventory.getContents());
        this.armor = cloneItems(inventory.getArmorContents());
        this.offHand = cloneItem(inventory.getItemInOffHand());
        this.level = player.getLevel();
        this.totalExperience = player.getTotalExperience();
        this.exp = player.getExp();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.health = player.getHealth();
        this.gameMode = player.getGameMode();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.potionEffects = new ArrayList<PotionEffect>(player.getActivePotionEffects());
    }

    static EventPlayerData capture(Player player) {
        return new EventPlayerData(player);
    }

    void restore(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setGameMode(gameMode);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(cloneItems(contents));
        inventory.setArmorContents(cloneItems(armor));
        inventory.setItemInOffHand(cloneItem(offHand));

        player.setTotalExperience(totalExperience);
        player.setLevel(level);
        player.setExp(exp);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }

        double maxHealth = 20.0D;
        try {
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
        } catch (NoSuchFieldError ignored) {
            // Compatibilidade com implementações que expõem apenas o valor padrão.
        }
        if (!player.isDead()) {
            player.setHealth(Math.max(0.5D, Math.min(health, maxHealth)));
        }
        player.setAllowFlight(allowFlight);
        if (allowFlight && flying) {
            player.setFlying(true);
        }
        player.updateInventory();
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) return new ItemStack[0];
        ItemStack[] result = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = cloneItem(source[i]);
        }
        return result;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
