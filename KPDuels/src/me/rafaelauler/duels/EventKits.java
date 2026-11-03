package me.rafaelauler.duels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/** Kits independentes, persistidos em event-kits.yml (inclui NBT/ItemMeta). */
public final class EventKits implements CommandExecutor {
    private final DuelPlugin plugin;
    private final EventManager events;
    private final File file;
    private final YamlConfiguration data;

    public EventKits(DuelPlugin plugin, EventManager events) {
        this.plugin = plugin;
        this.events = events;
        file = new File(plugin.getDataFolder(), "event-kits.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static String normalized(String name) {
        if (name == null) return null;
        String value = name.toLowerCase(Locale.ROOT);
        return value.matches("[a-z0-9_-]{1,32}") ? value : null;
    }

    public List<String> names() {
        if (!data.isConfigurationSection("kits")) return Collections.emptyList();
        List<String> names = new ArrayList<>(data.getConfigurationSection("kits").getKeys(false));
        Collections.sort(names);
        return names;
    }

    public boolean exists(String name) {
        String id = normalized(name);
        return id != null && data.isList("kits." + id + ".storage");
    }

    public boolean save(String name, Player player) {
        String id = normalized(name);
        if (id == null) return false;
        String path = "kits." + id;
        data.set(path + ".storage", Arrays.asList(cloneAll(player.getInventory().getStorageContents())));
        data.set(path + ".armor", Arrays.asList(cloneAll(player.getInventory().getArmorContents())));
        data.set(path + ".offhand", clone(player.getInventory().getItemInOffHand()));
        data.set(path + ".creator", player.getUniqueId().toString());
        data.set(path + ".created", System.currentTimeMillis());
        return flush();
    }

    private boolean flush() {
        try {
            if (!plugin.getDataFolder().isDirectory()) plugin.getDataFolder().mkdirs();
            data.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Não foi possível salvar event-kits.yml: " + ex.getMessage());
            return false;
        }
    }

    private ItemStack[] cloneAll(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) copy[i] = clone(items[i]);
        return copy;
    }

    private ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack[] read(String path, int slots) {
        List<?> serialized = data.getList(path);
        ItemStack[] result = new ItemStack[slots];
        if (serialized == null) return result;
        for (int i = 0; i < Math.min(slots, serialized.size()); i++) {
            Object value = serialized.get(i);
            if (value instanceof ItemStack) result[i] = ((ItemStack) value).clone();
        }
        return result;
    }

    /** Intencionalmente substitui todo o inventário, com os slots originais. */
    public boolean apply(String name, Player player) {
        if (!exists(name) || player == null) return false;
        String path = "kits." + normalized(name);
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setStorageContents(read(path + ".storage", player.getInventory().getStorageContents().length));
        player.getInventory().setArmorContents(read(path + ".armor", 4));
        Object offhand = data.get(path + ".offhand");
        player.getInventory().setItemInOffHand(offhand instanceof ItemStack ? ((ItemStack) offhand).clone() : null);
        player.updateInventory();
        return true;
    }

    public boolean delete(String name) {
        if (!exists(name)) return false;
        data.set("kits." + normalized(name), null);
        return flush();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("event.admin") && !sender.hasPermission("events.admin")) {
            sender.sendMessage("§cSem permissão: event.admin");
            return true;
        }
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("listar")) {
            sender.sendMessage("§eKits salvos: §f" + (names().isEmpty() ? "nenhum" : String.join(", ", names())));
            return true;
        }
        if (args.length == 2 && sub.equals("salvar")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cSomente um jogador pode capturar seu inventário."); return true; }
            if (normalized(args[1]) == null) { sender.sendMessage("§cNome inválido: use letras, números, _ ou - (máximo 32)."); return true; }
            boolean ok = save(args[1], (Player) sender);
            sender.sendMessage(ok ? "§aKit '" + args[1] + "' salvo com inventário, armadura e mão secundária." : "§cErro ao salvar kit.");
            return true;
        }
        if (args.length == 2 && (sub.equals("apagar") || sub.equals("excluir"))) {
            sender.sendMessage(delete(args[1]) ? "§aKit apagado." : "§cKit inexistente ou falha ao salvar.");
            return true;
        }
        if (args.length == 3 && sub.equals("dar")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("§cJogador offline."); return true; }
            if (!apply(args[2], target)) { sender.sendMessage("§cKit não encontrado."); return true; }
            sender.sendMessage("§aKit aplicado a " + target.getName() + " (inventário substituído).");
            target.sendMessage("§eUm administrador aplicou o kit §f" + args[2] + "§e.");
            return true;
        }
        if (args.length == 2 && sub.equals("todos")) {
            if (!exists(args[1])) { sender.sendMessage("§cKit não encontrado."); return true; }
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (events.isMember(player) && !events.isSpectator(player)) {
                    if (apply(args[1], player)) count++;
                }
            }
            sender.sendMessage("§aKit entregue a " + count + " participantes ativos do evento.");
            return true;
        }
        if (args.length == 3 && sub.equals("vincular")) {
            String mode = normalized(args[1]);
            if (mode == null || !EventExtras.isExtraMode(mode)) {
                sender.sendMessage("§cModalidade inválida. Use /eventoadmin modos."); return true;
            }
            if (!args[2].equalsIgnoreCase("padrao") && !exists(args[2])) {
                sender.sendMessage("§cKit inexistente. Use 'padrao' para restaurar o kit original."); return true;
            }
            plugin.getConfig().set("events.extra.modes." + mode + ".kit",
                    args[2].equalsIgnoreCase("padrao") ? "" : normalized(args[2]));
            plugin.saveConfig();
            sender.sendMessage("§aKit da modalidade " + mode + " atualizado.");
            return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "KPDuels - Kits de evento");
        sender.sendMessage("§e/eventokit salvar <nome> §7- Captura seu inventário completo");
        sender.sendMessage("§e/eventokit dar <jogador> <nome> §7- Aplica a qualquer momento");
        sender.sendMessage("§e/eventokit todos <nome> §7- Aplica a todos os participantes");
        sender.sendMessage("§e/eventokit vincular <modo> <nome|padrao> §7- Kit automático");
        sender.sendMessage("§e/eventokit listar §7- Lista kits; §e/eventokit apagar <nome>");
    }
}
