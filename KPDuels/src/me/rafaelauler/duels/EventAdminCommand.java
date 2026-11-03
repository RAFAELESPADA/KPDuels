package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Painel de eventos com checagem de permissão em todos os cliques e subcomandos. */
public final class EventAdminCommand implements CommandExecutor, Listener {
    private final DuelPlugin plugin;
    private final EventManager events;
    private final EventKits kits;
    private final EventExtras extras;

    /** Modos originais que já são aceitos por /puxarevento. */
    private static final List<String> LEGACY_MODES = Arrays.asList("sumo", "1v1", "mdr", "lava", "spawn");

    public EventAdminCommand(DuelPlugin plugin, EventManager events, EventKits kits, EventExtras extras) {
        this.plugin = plugin;
        this.events = events;
        this.kits = kits;
        this.extras = extras;
    }

    private boolean admin(CommandSender sender) {
        return sender.hasPermission("event.admin") || sender.hasPermission("events.admin");
    }

    private static class AdminView implements InventoryHolder {
        private final Inventory inventory;
        private final String page;
        private AdminView(String page, int size) {
            this.page = page;
            this.inventory = Bukkit.createInventory(this, size, "§8§lEvento Admin §7- " + page);
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    private ItemStack item(Material material, String name, String... description) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            for (String line : description) lore.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void open(Player player) {
        AdminView view = new AdminView("principal", 54);
        Inventory inv = view.getInventory();
        inv.setItem(10, item(events.isRoomOpen() ? Material.LIME_DYE : Material.RED_DYE,
                "&eAbrir / fechar sala", "&7Agora: " + (events.isRoomOpen() ? "&aABERTA" : "&cFECHADA")));
        inv.setItem(11, item(Material.EMERALD_BLOCK, "&aIniciar evento", "&7Inicia a contagem configurada"));
        inv.setItem(12, item(Material.BARRIER, "&cEncerrar evento", "&7Devolve os inventários originais"));
        inv.setItem(13, item(Material.IRON_SWORD, "&eAlternar PvP", "&7Agora: " + events.isPvpEnabled()));
        inv.setItem(14, item(Material.NETHER_STAR, "&6Eventos novos (7)", "&7Lucky Block, Arena, Sopa, Arco...", "&7Selecionado: " + extras.getSelectedMode()));
        inv.setItem(16, item(Material.TOTEM_OF_UNDYING, "&6Eventos clássicos (5)",
                "&7Puxar: Sumo, 1v1, MDR, Lava e Spawn",
                "&7Também permite iniciar os confrontos",
                "&eClique para administrar"));
        inv.setItem(15, item(Material.CHEST, "&bKits salvos", "&7Clique num kit para aplicar no evento", "&7Comando: /eventokit salvar <nome>"));
        inv.setItem(19, item(Material.REDSTONE, "&cCountdown -5s", "&7Atual: " + seconds() + " segundos"));
        inv.setItem(20, item(Material.GLOWSTONE_DUST, "&aCountdown +5s", "&7Atual: " + seconds() + " segundos"));
        inv.setItem(21, item(Material.BELL, "&eAlternar anúncios", "&7Agora: " + plugin.getConfig().getBoolean("events.broadcast.enabled", true)));
        inv.setItem(22, item(Material.WATER_BUCKET, "&bCiclar clima do mundo de eventos", "&7Requer habilitação explícita", "&7Afeta todos no mundo configurado"));
        inv.setItem(23, item(Material.CLOCK, "&eAlternar dia / noite", "&7Somente no mundo de eventos", "&7Requer habilitação explícita"));
        inv.setItem(24, item(Material.COMPASS, "&aDefinir spawn do evento", "&7Usa sua posição atual"));
        inv.setItem(25, item(Material.OAK_DOOR, "&eDefinir saída do evento", "&7Usa sua posição atual"));
        inv.setItem(28, item(Material.ENDER_PEARL, "&bTeleportar ao spawn", "&7Teleporta somente você"));
        inv.setItem(29, item(Material.GOLD_BLOCK, "&6Gerar Lucky Blocks", "&7Somente durante o evento Lucky Block"));
        inv.setItem(31, item(Material.SHIELD, "&eProteção dos controles de mundo",
                "&7Alterações habilitadas: " + plugin.getConfig().getBoolean("events.admin.world-controls-enabled", false),
                "&7/eventoadmin mundo habilitar|desabilitar"));
        inv.setItem(40, item(Material.BOOK, "&fAjuda e configuração", "&7/eventoadmin ajuda", "&7/seteventlocal <modo>", "&7/eventokit vincular <modo> <kit>"));
        player.openInventory(inv);
    }

    private void openModes(Player player) {
        AdminView view = new AdminView("modalidades", 27);
        Material[] materials = {Material.GOLD_BLOCK, Material.DIAMOND_SWORD, Material.MUSHROOM_STEW,
                Material.BOW, Material.FEATHER, Material.LEATHER_BOOTS, Material.BEACON};
        for (int i = 0; i < EventExtras.MODES.size(); i++) {
            String mode = EventExtras.MODES.get(i);
            view.getInventory().setItem(10 + i, item(materials[i], "&e" + EventExtras.display(mode),
                    "&7Config: events.pull-locations." + mode,
                    "&7Clique para selecionar e puxar participantes"));
        }
        view.getInventory().setItem(21, item(Material.TOTEM_OF_UNDYING, "&6Eventos clássicos",
                "&7Sumo, 1v1, MDR, Lava e Spawn", "&eClique para abrir"));
        view.getInventory().setItem(22, item(Material.ARROW, "&cVoltar"));
        player.openInventory(view.getInventory());
    }

    /** Painel dos eventos antigos: usa as coordenadas e regras originais do EventManager. */
    private void openLegacyModes(Player player) {
        AdminView view = new AdminView("classicos", 45);
        Inventory inv = view.getInventory();
        Material[] materials = {Material.SLIME_BLOCK, Material.IRON_SWORD, Material.CROSSBOW,
                Material.LAVA_BUCKET, Material.COMPASS};
        String[] labels = {"Sumo", "1v1", "MDR", "Lava", "Spawn"};
        for (int i = 0; i < LEGACY_MODES.size(); i++) {
            String mode = LEGACY_MODES.get(i);
            Location location = events.getPullLocation(mode);
            String configured = location != null ? "&aConfigurado: " + location.getWorld().getName()
                    : "&cLocal não configurado ou mundo indisponível";
            if (mode.equals("1v1") || mode.equals("sumo")) {
                inv.setItem(10 + i, item(materials[i], "&ePuxar " + labels[i], configured,
                        "&7Exige location, loc1 e loc2", "&7/seteventlocal " + mode + " [loc1|loc2]",
                        "&aClique para levar à área de espera"));
            } else if (mode.equals("mdr")) {
                inv.setItem(10 + i, item(materials[i], "&ePuxar MDR", configured,
                        "&7Admins: /seteventlocal mdr-admin", "&aClique para puxar os participantes"));
            } else if (mode.equals("lava")) {
                inv.setItem(10 + i, item(materials[i], "&ePuxar Lava", configured,
                        "&7Kit de sopas; entrada na arena ao iniciar", "&aClique para puxar os participantes"));
            } else {
                inv.setItem(10 + i, item(materials[i], "&ePuxar Spawn", configured,
                        "&7Usa events.pull-locations.spawn.location", "&7Diferente de events.spawn (sala de eventos)",
                        "&aClique para puxar os participantes"));
            }
        }
        inv.setItem(28, item(Material.DIAMOND_SWORD, "&aIniciar confronto 1v1",
                "&7Equivale a /evento puxar1v1", "&7É preciso iniciar o evento primeiro",
                "&7Necessita dois jogadores puxados para 1v1"));
        inv.setItem(30, item(Material.STICK, "&aIniciar confronto Sumo",
                "&7Equivale a /evento puxarsumo", "&7É preciso iniciar o evento primeiro",
                "&7Necessita dois jogadores puxados para Sumo"));
        inv.setItem(22, item(Material.NETHER_STAR, "&bVer eventos novos", "&7Lucky Block, Arena, Sopa..."));
        inv.setItem(40, item(Material.ARROW, "&cVoltar ao painel"));
        player.openInventory(inv);
    }

    private void openKits(Player player) {
        AdminView view = new AdminView("kits", 54);
        List<String> names = kits.names();
        for (int i = 0; i < Math.min(45, names.size()); i++) {
            view.getInventory().setItem(i, item(Material.CHEST, "&a" + names.get(i),
                    "&7Aplica aos participantes ativos", "&7Inventários dos participantes serão substituídos"));
        }
        view.getInventory().setItem(49, item(Material.ARROW, "&cVoltar"));
        player.openInventory(view.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AdminView) event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminView)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        Player player = (Player) event.getWhoClicked();
        if (!admin(player)) { player.closeInventory(); return; }
        AdminView view = (AdminView) event.getView().getTopInventory().getHolder();
        int slot = event.getRawSlot();
        if (view.page.equals("modalidades")) {
            if (slot == 22) { open(player); return; }
            if (slot == 21) { openLegacyModes(player); return; }
            if (slot >= 10 && slot < 10 + EventExtras.MODES.size()) {
                String mode = EventExtras.MODES.get(slot - 10);
                String message = selectAndPull(mode);
                player.sendMessage(message);
                open(player);
            }
            return;
        }
        if (view.page.equals("classicos")) {
            if (slot == 40) { open(player); return; }
            if (slot == 22) { openModes(player); return; }
            if (slot >= 10 && slot < 10 + LEGACY_MODES.size()) {
                player.sendMessage(pullLegacy(LEGACY_MODES.get(slot - 10)));
                openLegacyModes(player);
                return;
            }
            if (slot == 28 || slot == 30) {
                player.sendMessage(startLegacyMatch(slot == 28 ? "1v1" : "sumo"));
                openLegacyModes(player);
            }
            return;
        }
        if (view.page.equals("kits")) {
            if (slot == 49) { open(player); return; }
            List<String> names = kits.names();
            if (slot < Math.min(45, names.size())) {
                String name = names.get(slot);
                int count = 0;
                for (Player participant : Bukkit.getOnlinePlayers()) {
                    if (events.isMember(participant) && !events.isSpectator(participant) && kits.apply(name, participant)) count++;
                }
                player.sendMessage("§aKit " + name + " entregue a " + count + " participantes.");
                open(player);
            }
            return;
        }
        switch (slot) {
            case 10: if (events.isRoomOpen()) events.closeRoom(); else events.openRoom(); break;
            case 11: player.sendMessage(events.startEvent() ? "§aCountdown iniciado." : "§cEvento já iniciado ou sem participantes."); break;
            case 12: events.stopEvent(null); player.sendMessage("§cEvento encerrado."); break;
            case 13: events.setPvp(!events.isPvpEnabled()); break;
            case 14: openModes(player); return;
            case 15: openKits(player); return;
            case 16: openLegacyModes(player); return;
            case 19: setSeconds(seconds() - 5); break;
            case 20: setSeconds(seconds() + 5); break;
            case 21: setBroadcast(!plugin.getConfig().getBoolean("events.broadcast.enabled", true)); break;
            case 22: changeClimate(player, "ciclo"); break;
            case 23: changeTime(player, "alternar"); break;
            case 24: if (!events.isEventStarted()) events.setEventSpawn(player.getLocation()); else player.sendMessage("§cNão é permitido mudar o spawn durante o evento."); break;
            case 25: if (!events.isEventStarted()) events.setExit(player.getLocation()); else player.sendMessage("§cNão é permitido mudar a saída durante o evento."); break;
            case 28: if (events.getEventSpawn() != null) player.teleport(events.getEventSpawn()); else player.sendMessage("§cSpawn não configurado."); break;
            case 29: player.sendMessage("§eLucky Blocks criados: " + extras.spawnLuckyBlocks()); break;
            case 31: player.sendMessage("§eUse /eventoadmin mundo habilitar para permitir clima/hora no mundo do evento."); break;
            case 40: help(player); break;
            default: return;
        }
        open(player);
    }

    private int seconds() { return plugin.getConfig().getInt("events.countdown.seconds", 10); }
    private void setSeconds(int value) {
        plugin.getConfig().set("events.countdown.seconds", Math.max(0, Math.min(120, value)));
        plugin.saveConfig();
    }
    private void setBroadcast(boolean enabled) {
        plugin.getConfig().set("events.broadcast.enabled", enabled);
        plugin.saveConfig();
        events.refreshBroadcast();
    }

    private String selectAndPull(String mode) {
        if (events.isEventStarted() || !extras.selectMode(mode)) return "§cEncerre o evento antes de trocar de modalidade.";
        if (events.getPullLocation(mode) == null) return "§eModo selecionado. Defina /seteventlocal " + mode + " para puxar.";
        if (!extras.pull(mode)) return "§eModo selecionado, mas não foi possível puxar. Confira pontos especiais e participantes.";
        return "§a" + EventExtras.display(mode) + " selecionado; participantes transportados e equipados.";
    }

    /** Mesma rotina do /puxarevento: não altera as coordenadas nem muda outros mundos. */
    private String pullLegacy(String mode) {
        if (!LEGACY_MODES.contains(mode)) return "§cModo clássico inválido.";
        if (events.getPullLocation(mode) == null) {
            return "§cLocal de " + mode + " não configurado ou mundo descarregado. Use /seteventlocal " + mode + ".";
        }
        if (!events.pullPlayers(mode)) {
            return "§cNão foi possível puxar " + mode.toUpperCase(Locale.ROOT)
                    + ". Confira os participantes e os locais necessários (Sumo/1v1: loc1 e loc2; MDR: mdr-admin).";
        }
        return "§aParticipantes puxados para " + mode.toUpperCase(Locale.ROOT) + " usando o local da config.";
    }

    /** Comando manual correspondente a /evento puxar1v1 e /evento puxarsumo. */
    private String startLegacyMatch(String mode) {
        if (!mode.equals("1v1") && !mode.equals("sumo")) return "§cTipo de confronto inválido.";
        int matches = events.startPairMatches(mode);
        if (matches < 0) return "§cInicie o evento e configure loc1 e loc2 de " + mode + ".";
        if (matches == 0) return "§eNão há dois participantes disponíveis de " + mode
                + " ou a arena já possui um confronto. Use /puxarevento " + mode + " antes.";
        return "§a" + matches + " confronto(s) de " + mode.toUpperCase(Locale.ROOT) + " iniciado(s).";
    }

    private World safeWorld(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("events.admin.world-controls-enabled", false)) {
            sender.sendMessage("§cMudanças de mundo bloqueadas. Use /eventoadmin mundo habilitar conscientemente.");
            return null;
        }
        Location location = null;
        String mode = extras.getActiveMode();
        if (EventExtras.isExtraMode(mode)) location = events.getPullLocation(mode);
        if (location == null && EventExtras.isExtraMode(extras.getSelectedMode())) location = events.getPullLocation(extras.getSelectedMode());
        if (location == null) location = events.getEventSpawn();
        if (location == null || location.getWorld() == null) {
            sender.sendMessage("§cConfigure o mundo de evento com /seteventlocal <modo> ou /set-eventos-spawn.");
            return null;
        }
        // Evita modificar acidentalmente o mundo de saída ou o mundo do jogador.
        return location.getWorld();
    }

    private void changeClimate(CommandSender sender, String climate) {
        World world = safeWorld(sender);
        if (world == null) return;
        String state = climate.toLowerCase(Locale.ROOT);
        if (state.equals("ciclo")) state = world.isThundering() ? "limpo" : (world.hasStorm() ? "trovoada" : "chuva");
        if (!state.equals("limpo") && !state.equals("chuva") && !state.equals("trovoada")) {
            sender.sendMessage("§cUse /eventoadmin clima limpo|chuva|trovoada"); return;
        }
        world.setStorm(!state.equals("limpo"));
        world.setThundering(state.equals("trovoada"));
        world.setWeatherDuration(20 * 60 * 10);
        sender.sendMessage("§aClima '" + state + "' aplicado SOMENTE ao mundo: " + world.getName());
    }

    private void changeTime(CommandSender sender, String time) {
        World world = safeWorld(sender);
        if (world == null) return;
        String state = time.toLowerCase(Locale.ROOT);
        if (state.equals("alternar")) state = world.getTime() < 13000 ? "noite" : "dia";
        if (!state.equals("dia") && !state.equals("noite")) {
            sender.sendMessage("§cUse /eventoadmin hora dia|noite"); return;
        }
        world.setTime(state.equals("dia") ? 1000 : 18000);
        sender.sendMessage("§aHorário alterado SOMENTE em " + world.getName() + ".");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!admin(sender)) { sender.sendMessage("§cSem permissão: event.admin"); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("painel")) {
            if (sender instanceof Player) open((Player) sender);
            else help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("ajuda")) { help(sender); return true; }
        if (sub.equals("classicos") || sub.equals("antigos") || sub.equals("legacy")) {
            if (sender instanceof Player) openLegacyModes((Player) sender);
            else sender.sendMessage("§eEventos clássicos: " + String.join(", ", LEGACY_MODES));
            return true;
        }
        if (sub.equals("modos") || sub.equals("eventos")) {
            if (sender instanceof Player) openModes((Player) sender);
            else sender.sendMessage("§eModos: " + String.join(", ", EventExtras.MODES));
            return true;
        }
        if (sub.equals("kits")) {
            if (sender instanceof Player) openKits((Player) sender);
            else sender.sendMessage("§eKits: " + String.join(", ", kits.names()));
            return true;
        }
        if (sub.equals("modo") && args.length == 2) {
            String mode = args[1].toLowerCase(Locale.ROOT);
            sender.sendMessage(EventExtras.isExtraMode(mode) ? selectAndPull(mode) : pullLegacy(mode));
            return true;
        }
        if (sub.equals("puxar") && args.length == 2) {
            String mode = args[1].toLowerCase(Locale.ROOT);
            sender.sendMessage(EventExtras.isExtraMode(mode) ? selectAndPull(mode) : pullLegacy(mode));
            return true;
        }
        if (sub.equals("confronto") && args.length == 2) {
            sender.sendMessage(startLegacyMatch(args[1].toLowerCase(Locale.ROOT)));
            return true;
        }
        if (sub.equals("countdown") && args.length == 2) {
            try { setSeconds(Integer.parseInt(args[1])); sender.sendMessage("§aCountdown: " + seconds() + "s."); }
            catch (NumberFormatException ex) { sender.sendMessage("§cInforme um número de 0 a 120."); }
            return true;
        }
        if (sub.equals("broadcast") && args.length == 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            setBroadcast(args[1].equalsIgnoreCase("on")); sender.sendMessage("§aAnúncios atualizados."); return true;
        }
        if (sub.equals("mundo") && args.length == 2) {
            if (!args[1].equalsIgnoreCase("habilitar") && !args[1].equalsIgnoreCase("desabilitar")) {
                sender.sendMessage("§cUse /eventoadmin mundo habilitar|desabilitar"); return true;
            }
            boolean enabled = args[1].equalsIgnoreCase("habilitar");
            plugin.getConfig().set("events.admin.world-controls-enabled", enabled);
            plugin.saveConfig();
            sender.sendMessage(enabled ? "§eATENÇÃO: clima e hora afetam TODOS neste mundo configurado, inclusive outros plugins."
                    : "§aControles de mundo desabilitados.");
            return true;
        }
        if (sub.equals("clima") && args.length == 2) { changeClimate(sender, args[1]); return true; }
        if (sub.equals("hora") && args.length == 2) { changeTime(sender, args[1]); return true; }
        if (sub.equals("blocos")) { sender.sendMessage("§eLucky Blocks criados: " + extras.spawnLuckyBlocks()); return true; }
        if (sub.equals("status")) {
            sender.sendMessage("§6Evento Admin §7| sala: " + events.isRoomOpen() + ", iniciado: " + events.isEventStarted()
                    + ", PvP: " + events.isPvpEnabled() + ", modalidade: " + extras.getActiveMode()
                    + ", selecionado: " + extras.getSelectedMode()); return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6§lEVENTO ADMIN §7- /eventoadmin abre o painel");
        sender.sendMessage("§e/eventoadmin modos §7- Seleção dos 7 modos novos");
        sender.sendMessage("§e/eventoadmin classicos §7- Sumo, 1v1, MDR, Lava e Spawn");
        sender.sendMessage("§e/eventoadmin modo <modo> §7- Selecionar e puxar participantes (novos ou antigos)");
        sender.sendMessage("§e/eventoadmin puxar <modo> §7- Teleportar participantes via config");
        sender.sendMessage("§e/eventoadmin confronto <1v1|sumo> §7- Iniciar um confronto manual");
        sender.sendMessage("§e/eventoadmin kits §7- Aplicar kits salvos");
        sender.sendMessage("§e/eventoadmin countdown <0..120> §7- Tempo pré-evento");
        sender.sendMessage("§e/eventoadmin broadcast on|off §7- Anúncios");
        sender.sendMessage("§e/eventoadmin mundo habilitar|desabilitar §7- Autoriza mundo");
        sender.sendMessage("§e/eventoadmin clima limpo|chuva|trovoada §7- Clima");
        sender.sendMessage("§e/eventoadmin hora dia|noite §7- Horário");
        sender.sendMessage("§e/eventoadmin blocos §7- Repor Lucky Blocks");
        sender.sendMessage("§e/seteventlocal <modo> §7- Arena; <parkour|corrida> finish, <rei_do_morro> point");
        sender.sendMessage("§e/eventokit §7- Gerenciador de kits completos");
    }
}
