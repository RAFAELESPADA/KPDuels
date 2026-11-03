package me.rafaelauler.duels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/** Modalidades isoladas no sistema de eventos; não modifica arenas do Duels. */
public final class EventExtras implements Listener {
    public static final List<String> MODES = Collections.unmodifiableList(Arrays.asList(
            "luckyblock", "arena", "sopa", "arco", "parkour", "corrida", "rei_do_morro"));
    private final DuelPlugin plugin;
    private final EventManager events;
    private final EventKits kits;
    private final Random random = new Random();
    private final File journal;
    private final Map<Block, Material> luckyBlocks = new LinkedHashMap<>();
    private final Map<UUID, Integer> hillPoints = new LinkedHashMap<>();
    private final Set<String> pendingRecovery = new LinkedHashSet<>();
    private BukkitTask modeTask;
    private String selectedMode = "";
    private String activeMode = "";
    private int elapsedSeconds;
    private boolean finishing;

    public EventExtras(DuelPlugin plugin, EventManager events, EventKits kits) {
        this.plugin = plugin;
        this.events = events;
        this.kits = kits;
        this.journal = new File(plugin.getDataFolder(), "event-luckyblocks.yml");
        // Recupera blocos remanescentes após reinício inesperado (ou ao carregar o mundo).
        recoverLuckyBlocks();
        String selected = plugin.getConfig().getString("events.extra.selected", "");
        if (isExtraMode(selected)) selectedMode = selected;
    }

    public static boolean isExtraMode(String mode) { return MODES.contains(mode); }
    public String getSelectedMode() { return selectedMode; }
    public String getActiveMode() { return activeMode; }

    public boolean selectMode(String mode) {
        if (!isExtraMode(mode) || events.isEventStarted()) return false;
        selectedMode = mode;
        plugin.getConfig().set("events.extra.selected", mode);
        plugin.saveConfig();
        return true;
    }

    /** Reutiliza EXCLUSIVAMENTE os locais de events.pull-locations.<modo>.location. */
    public boolean pull(String mode) {
        if (!isExtraMode(mode) || events.isEventStarted() || events.getPullLocation(mode) == null) return false;
        if (mode.equals("rei_do_morro") && !sameWorld(events.getPullLocation(mode), extraLocation(mode, "point"))) return false;
        if ((mode.equals("corrida") || mode.equals("parkour"))
                && !sameWorld(events.getPullLocation(mode), extraLocation(mode, "finish"))) return false;
        if (!events.pullPlayers(mode)) return false;
        cleanup();
        activeMode = mode;
        selectMode(mode);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (events.isMember(player) && !events.isSpectator(player) && mode.equals(events.getPulledMode(player))) {
                preparePlayer(player, mode);
                player.sendMessage("§6§lEVENTO §7» §e" + display(mode) + " §7- Aguarde /evento iniciar.");
            }
        }
        return true;
    }

    public void onStart() {
        if (!isExtraMode(activeMode)) return;
        finishing = false;
        elapsedSeconds = 0;
        hillPoints.clear();
        boolean combat = !(activeMode.equals("corrida") || activeMode.equals("parkour"));
        events.setPvp(combat);
        if (activeMode.equals("luckyblock")) spawnLuckyBlocks();
        modeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!events.isEventStarted() || !isExtraMode(activeMode)) return;
            elapsedSeconds++;
            if (activeMode.equals("rei_do_morro")) tickHill();
            if (activeMode.equals("luckyblock")) {
                int interval = Math.max(5, plugin.getConfig().getInt("events.extra.luckyblock.spawn-interval-seconds", 20));
                if (elapsedSeconds % interval == 0) spawnLuckyBlocks();
            }
        }, 20L, 20L);
        announce("§aModalidade §e" + display(activeMode) + "§a iniciada!");
    }

    public void onStop() {
        cleanup();
        activeMode = "";
    }

    public void shutdown() { onStop(); }

    /** Se o administrador voltar aos modos antigos, não mantém tarefas do modo extra. */
    public void onLegacyPull(String mode) {
        if (!isExtraMode(mode) && !activeMode.isEmpty()) onStop();
    }

    private void cleanup() {
        if (modeTask != null) { modeTask.cancel(); modeTask = null; }
        // Só remove ouro criado pelo próprio plugin; nunca restaura/edita outro bloco.
        for (Map.Entry<Block, Material> entry : luckyBlocks.entrySet()) {
            if (entry.getKey().getType() == Material.GOLD_BLOCK) {
                entry.getKey().setType(entry.getValue(), false);
            }
        }
        luckyBlocks.clear();
        saveJournal();
        hillPoints.clear();
        finishing = false;
    }

    private Location extraLocation(String mode, String part) {
        return events.getExtraLocation(mode, part);
    }

    private boolean sameWorld(Location a, Location b) {
        return a != null && b != null && a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    public static String display(String mode) {
        switch (mode) {
            case "luckyblock": return "Lucky Block";
            case "arena": return "Arena PvP - Todos contra Todos";
            case "sopa": return "Batalha de Sopas";
            case "arco": return "Arqueiros";
            case "parkour": return "Parkour";
            case "corrida": return "Corrida";
            case "rei_do_morro": return "Rei do Morro";
            default: return mode;
        }
    }

    private void preparePlayer(Player player, String mode) {
        String configured = plugin.getConfig().getString("events.extra.modes." + mode + ".kit", "");
        if (configured != null && !configured.isEmpty()) {
            if (kits.apply(configured, player)) return;
            player.sendMessage("§cO kit vinculado '" + configured + "' não existe. Usando kit padrão.");
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        switch (mode) {
            case "luckyblock":
                player.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
                // Picareta padrão para quebrar os blocos de ouro especiais.
                player.getInventory().setItem(1, new ItemStack(Material.STONE_PICKAXE));
                player.getInventory().addItem(new ItemStack(Material.BREAD, 8));
                break;
            case "arena":
                player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
                player.getInventory().setArmorContents(new ItemStack[] {
                        new ItemStack(Material.IRON_BOOTS), new ItemStack(Material.IRON_LEGGINGS),
                        new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET)});
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 4));
                break;
            case "sopa":
                player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
                player.getInventory().setArmorContents(new ItemStack[] {
                        new ItemStack(Material.IRON_BOOTS), new ItemStack(Material.IRON_LEGGINGS),
                        new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET)});
                for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Material.MUSHROOM_STEW));
                break;
            case "arco":
                player.getInventory().setItem(0, new ItemStack(Material.BOW));
                player.getInventory().setItem(1, new ItemStack(Material.ARROW, 64));
                player.getInventory().setArmorContents(new ItemStack[] {
                        new ItemStack(Material.CHAINMAIL_BOOTS), new ItemStack(Material.CHAINMAIL_LEGGINGS),
                        new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.CHAINMAIL_HELMET)});
                break;
            case "rei_do_morro":
                player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
                player.getInventory().setItem(1, new ItemStack(Material.SHIELD));
                player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
                break;
            case "corrida":
                player.getInventory().setItem(0, new ItemStack(Material.COOKED_BEEF, 16));
                break;
            default: break;
        }
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.updateInventory();
    }

    private void announce(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (events.isMember(player)) player.sendMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFinish(PlayerMoveEvent event) {
        if (!events.isEventStarted() || finishing || !events.isMember(event.getPlayer())
                || events.isSpectator(event.getPlayer()) || event.getTo() == null) return;
        String mode = events.getPulledMode(event.getPlayer());
        if (!activeMode.equals(mode) || !(mode.equals("parkour") || mode.equals("corrida"))) return;
        Location finish = extraLocation(mode, "finish");
        if (sameWorld(event.getTo(), finish)
                && event.getTo().distanceSquared(finish) <= Math.pow(Math.max(1.0,
                    plugin.getConfig().getDouble("events.extra.finish-radius", 2.5)), 2)) {
            finishing = true;
            events.stopEvent(event.getPlayer());
        }
    }

    private void tickHill() {
        Location point = extraLocation("rei_do_morro", "point");
        if (point == null) return;
        double radius = Math.max(1, Math.min(15, plugin.getConfig().getDouble("events.extra.hill.radius", 4)));
        List<Player> kings = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (events.isMember(player) && !events.isSpectator(player) && "rei_do_morro".equals(events.getPulledMode(player))
                    && sameWorld(point, player.getLocation()) && player.getLocation().distanceSquared(point) <= radius * radius) {
                kings.add(player);
            }
        }
        if (kings.size() != 1 || finishing) return; // ponto disputado não pontua
        Player king = kings.get(0);
        int points = hillPoints.merge(king.getUniqueId(), 1, Integer::sum);
        int required = Math.max(5, plugin.getConfig().getInt("events.extra.hill.points-to-win", 30));
        if (points % 5 == 0) announce("§6Rei do Morro §7» §e" + king.getName() + " §f" + points + "/" + required + " pontos");
        if (points >= required) { finishing = true; events.stopEvent(king); }
    }

    /** Só cria ouro em AIR sobre chão sólido, na altura configurada e no mundo do modo. */
    public int spawnLuckyBlocks() {
        if (!"luckyblock".equals(activeMode)) return 0;
        Location center = events.getPullLocation("luckyblock");
        if (center == null || center.getWorld() == null) return 0;
        World world = center.getWorld();
        int radius = Math.max(3, Math.min(32, plugin.getConfig().getInt("events.extra.luckyblock.radius", 12)));
        int maximum = Math.max(1, Math.min(64, plugin.getConfig().getInt("events.extra.luckyblock.max-blocks", 12)));
        int created = 0;
        for (int attempt = 0; attempt < 120 && luckyBlocks.size() < maximum; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = center.getBlockY();
            if (y <= world.getMinHeight() || y >= world.getMaxHeight()) continue;
            Block block = world.getBlockAt(x, y, z);
            Block floor = block.getRelative(0, -1, 0);
            if (block.getType() != Material.AIR || !floor.getType().isSolid() || luckyBlocks.containsKey(block)) continue;
            boolean nearby = false;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(block.getLocation().add(0.5, 0, 0.5)) < 4) { nearby = true; break; }
            }
            if (nearby) continue;
            luckyBlocks.put(block, Material.AIR);
            saveJournal();
            block.setType(Material.GOLD_BLOCK, false);
            created++;
        }
        return created;
    }

    /**
     * Só os blocos de ouro GERADOS pelo próprio evento podem ser minerados.
     * Blocos de ouro existentes no schematic não fazem parte de luckyBlocks.
     */
    private boolean canMineLucky(Player player, Block block) {
        return block.getType() == Material.GOLD_BLOCK
                && luckyBlocks.containsKey(block)
                && "luckyblock".equals(activeMode)
                && events.isEventStarted()
                && events.isMember(player)
                && !events.isSpectator(player)
                && "luckyblock".equals(events.getPulledMode(player));
    }

    /** Permite a animação de mineração mesmo em áreas protegidas (só ouro rastreado). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLuckyDamage(BlockDamageEvent event) {
        if (!luckyBlocks.containsKey(event.getBlock())) return;
        event.setCancelled(!canMineLucky(event.getPlayer(), event.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (luckyBlocks.containsKey(block)) {
            if (!canMineLucky(player, block)) {
                event.setCancelled(true);
                return;
            }
            // Desfaz a proteção SOMENTE para ouro gerado pelo plugin, neste evento.
            // Outros blocos (inclusive GOLD_BLOCK decorativo) permanecem protegidos.
            event.setCancelled(false);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }
        if (events.isMember(player) && isExtraMode(events.getPulledMode(player))) {
            event.setCancelled(true);
        }
    }

    /** A recompensa só acontece após uma quebra que nenhum outro listener cancelou. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulLuckyBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!canMineLucky(event.getPlayer(), block)) return;
        if (luckyBlocks.remove(block) == null) return;
        saveJournal();
        reward(event.getPlayer());
        // O próprio Minecraft remove o bloco após o BlockBreakEvent.
    }

    /** Permite abrir Lucky Block também com clique direito (sem quebrar outros blocos). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLuckyInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!luckyBlocks.containsKey(block)) return;
        if (!events.isEventStarted() || !events.isMember(event.getPlayer()) || events.isSpectator(event.getPlayer())
                || !"luckyblock".equals(events.getPulledMode(event.getPlayer()))) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        consumeLucky(block, event.getPlayer());
    }

    private void consumeLucky(Block block, Player player) {
        if (luckyBlocks.remove(block) == null) return;
        if (block.getType() == Material.GOLD_BLOCK) block.setType(Material.AIR, false);
        saveJournal();
        reward(player);
    }

    /** Journal dos blocos gerados para recuperação após crash; não registra nenhum outro bloco. */
    private void saveJournal() {
        YamlConfiguration data = new YamlConfiguration();
        List<String> locations = new ArrayList<>(pendingRecovery);
        for (Block block : luckyBlocks.keySet()) {
            locations.add(block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ());
        }
        data.set("temporary-gold-blocks", locations);
        try {
            if (!plugin.getDataFolder().isDirectory()) plugin.getDataFolder().mkdirs();
            data.save(journal);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar journal dos Lucky Blocks: " + ex.getMessage());
        }
    }

    private void recoverLuckyBlocks() {
        if (!journal.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(journal);
        pendingRecovery.clear();
        for (String raw : data.getStringList("temporary-gold-blocks")) {
            try {
                String[] loc = raw.split(";");
                if (loc.length != 4) continue;
                World world = Bukkit.getWorld(loc[0]);
                if (world == null) { pendingRecovery.add(raw); continue; }
                int x = Integer.parseInt(loc[1]);
                int y = Integer.parseInt(loc[2]);
                int z = Integer.parseInt(loc[3]);
                if (world.getBlockAt(x, y, z).getType() == Material.GOLD_BLOCK) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Entrada inválida no journal Lucky Block: " + raw);
            }
        }
        saveJournal();
        if (!pendingRecovery.isEmpty()) plugin.getLogger().warning("Há blocos de eventos pendentes em mundos não carregados: " + pendingRecovery.size());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (pendingRecovery.isEmpty()) return;
        List<String> handled = new ArrayList<>();
        for (String raw : pendingRecovery) {
            try {
                String[] loc = raw.split(";");
                if (loc.length != 4 || !loc[0].equals(event.getWorld().getName())) continue;
                Block block = event.getWorld().getBlockAt(Integer.parseInt(loc[1]), Integer.parseInt(loc[2]), Integer.parseInt(loc[3]));
                if (block.getType() == Material.GOLD_BLOCK) block.setType(Material.AIR, false);
                handled.add(raw);
            } catch (IllegalArgumentException ex) {
                handled.add(raw);
            }
        }
        if (!handled.isEmpty()) {
            pendingRecovery.removeAll(handled);
            saveJournal();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(luckyBlocks::containsKey);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (events.isMember(event.getPlayer()) && isExtraMode(events.getPulledMode(event.getPlayer()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(luckyBlocks::containsKey);
    }

    private void reward(Player player) {
        int roll = random.nextInt(10);
        String label;
        switch (roll) {
            case 0: player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD)); label = "Espada de diamante"; break;
            case 1: player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 6)); label = "Maçãs douradas"; break;
            case 2: player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 32)); label = "Arco e flechas"; break;
            case 3: player.getInventory().addItem(new ItemStack(Material.IRON_CHESTPLATE)); label = "Peitoral de ferro"; break;
            case 4: player.getInventory().addItem(new ItemStack(Material.DIAMOND_HELMET)); label = "Capacete de diamante"; break;
            case 5: player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 3)); label = "Pérolas do fim"; break;
            case 6: player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1)); label = "Velocidade II por 20s"; break;
            case 7: player.setHealth(Math.min(player.getHealth() + 10, player.getMaxHealth())); label = "Cura instantânea"; break;
            case 8: player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 7, 1)); label = "Azar: lentidão"; break;
            default: player.getInventory().addItem(new ItemStack(Material.TNT, 4)); label = "4 TNT (não podem ser colocadas neste evento)"; break;
        }
        player.sendMessage(ChatColor.GOLD + "Lucky Block §7» §f" + label + "!");
        player.updateInventory();
    }
}
