package me.rafaelauler.duels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/** Gerencia a sala, participantes, espectadores, teletransportes e estatísticas dos eventos. */
public final class EventManager {

    private static EventManager instance;
    private boolean victoryAnimationActive = false;
    private BukkitTask victoryAnimationTask;
    private final DuelPlugin plugin;
    private EventExtras extras;
    private final Set<UUID> members = Collections.synchronizedSet(new LinkedHashSet<UUID>());
    private final Set<UUID> spectators = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Map<UUID, EventPlayerData> playerData = new ConcurrentHashMap<UUID, EventPlayerData>();
    // Modalidade para a qual cada participante foi puxado por /puxarevento.
    private final Map<UUID, String> pulledModes = new ConcurrentHashMap<UUID, String>();
    // Uma mesma sessão é associada aos dois UUIDs do par.
    private final Map<UUID, EventMatch> eventMatches = new ConcurrentHashMap<UUID, EventMatch>();
    private final Map<String, BukkitTask> pairQueueTasks =
            new ConcurrentHashMap<String, BukkitTask>();
    private boolean roomOpen;
    private boolean eventStarted;
    private boolean pvpEnabled;
    private BukkitTask broadcastTask;
    private BukkitTask countdownTask;
    private int broadcastIndex;

    public EventManager(DuelPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.roomOpen = false; // A sala sempre inicia fechada após reiniciar o servidor.
    }

    public void setEventExtras(EventExtras extras) { this.extras = extras; }

    public Location getExtraLocation(String mode, String part) {
        if (!EventExtras.isExtraMode(mode)) return null;
        if (!(part.equals("finish") || part.equals("point"))) return null;
        return readLocation("events.pull-locations." + mode + "." + part);
    }

    public void setExtraLocation(String mode, String part, Location location) {
        if (!EventExtras.isExtraMode(mode) || !(part.equals("finish") || part.equals("point"))) return;
        saveLocation("events.pull-locations." + mode + "." + part, location);
    }

    public void refreshBroadcast() {
        if (roomOpen) startBroadcastTask();
        else stopBroadcastTask();
    }

    public static EventManager getInstance() {
        return instance;
    }

    public static boolean isInEvent(Player player) {
        return instance != null && player != null && instance.members.contains(player.getUniqueId());
    }
    public boolean isVictoryAnimationActive() {
        return victoryAnimationActive;
    }
    public static boolean isSpectator(Player player) {
        return instance != null && player != null && instance.spectators.contains(player.getUniqueId());
    }

    public boolean isRoomOpen() { return roomOpen; }
    public boolean isEventStarted() { return eventStarted; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public boolean isMember(Player player) { return player != null && members.contains(player.getUniqueId()); }
    public boolean isSpectator(UUID uuid) { return spectators.contains(uuid); }

    public void setEventSpawn(Location location) {
        saveLocation("events.spawn", location);
    }

    public void setExit(Location location) {
        saveLocation("events.exit", location);
    }

    public void setPullLocation(String key, Location location) {
        saveLocation("events.pull-locations." + normalizeKey(key) + ".location", location);
    }

    public void setPullDuelSpawn(String key, String side, Location location) {
        String normalizedKey = normalizeKey(key);
        String normalizedSide = normalizeKey(side);
        if (!(normalizedKey.equals("1v1") || normalizedKey.equals("sumo"))) return;
        if (!(normalizedSide.equals("loc1") || normalizedSide.equals("loc2"))) return;
        saveLocation("events.pull-locations." + normalizedKey + "." + normalizedSide, location);
    }

    private void saveLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) return;
        String value = location.getWorld().getName() + ";" + location.getX() + ";" +
                location.getY() + ";" + location.getZ() + ";" +
                location.getYaw() + ";" + location.getPitch();
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }

    public Location getEventSpawn() {
        return readLocation("events.spawn");
    }

    public Location getExit() {
        return readLocation("events.exit");
    }

    private Location readLocation(String path) {
        String raw = plugin.getConfig().getString(path);
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            String[] parts = raw.split(";");
            if (parts.length < 4) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0F;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0F;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception ex) {
            plugin.getLogger().warning("Localização inválida em " + path + ": " + raw);
            return null;
        }
    }

    public boolean join(Player player) {
        if (!roomOpen) {
            player.sendMessage(color(message("messages.room-closed", "&cA sala de eventos está fechada.")));
            return false;
        }
        if (eventStarted || countdownTask != null) {
            player.sendMessage("§cO evento já está em andamento e não aceita novas entradas.");
            return false;
        }
        if (isMember(player)) {
            player.sendMessage("§cVocê já está no evento.");
            return false;
        }
        if (DuelManager.isInDuel(player)) {
            player.sendMessage("§cVocê não pode entrar no evento enquanto estiver em um duelo.");
            return false;
        }
        Location spawn = getEventSpawn();
        if (spawn == null) {
            player.sendMessage("§cO spawn dos eventos não foi configurado. Avise um administrador.");
            return false;
        }

        player.closeInventory();
        playerData.put(player.getUniqueId(), EventPlayerData.capture(player));
        members.add(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        if (plugin.getMySQL() != null) {
            plugin.getMySQL().loadEventStatsAsync(player.getUniqueId());
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setHealth(Math.min(20.0D, getMaxHealth(player)));
        giveEventEntryKit(player);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.teleport(spawn);
        player.sendMessage(color(message("messages.join", "&aVocê entrou no evento!")));
        refreshVisibility();

        // Executa os comandos configurados como o próprio jogador, somente após
        // a entrada ter sido concluída com sucesso.
        dispatchPlayerCommands(player, "events.commands.on-join-player", "evento");
        return true;
    }
    /**
     * Retorna o adversário do jogador, mesmo que o PvP já tenha sido bloqueado
     * pelo encerramento do confronto.
     */
    public Player getMatchOpponent(Player player) {

        if (player == null) return null;

        EventMatch match = eventMatches.get(player.getUniqueId());

        if (match == null) return null;

        UUID opponentId = match.first.equals(player.getUniqueId())
                ? match.second : match.first;

        Player opponent = Bukkit.getPlayer(opponentId);

        if (opponent == null || !opponent.isOnline()) return null;
        if (!isMember(opponent) || isSpectator(opponent)) return null;

        return opponent;
    }

    /**
     * Encerra definitivamente o confronto dos dois jogadores.
     * Não reinicia o round e não remove a modalidade do vencedor.
     */
    public void finishPairMatch(Player player) {

        if (player == null) return;

        EventMatch match = eventMatches.get(player.getUniqueId());

        if (match == null) return;

        cancelMatch(match);

        match.resetScheduled = false;

        eventMatches.remove(match.first);
        eventMatches.remove(match.second);
    }
    public void leave(Player player) {
        if (!isMember(player)) {
            player.sendMessage("§cVocê não está em um evento.");
            return;
        }
        removeMember(player, true, true);
        player.sendMessage(color(message("messages.leave", "&aVocê saiu do evento.")));
    }

    private void removeMember(Player player, boolean teleport, boolean restore) {
        UUID uuid = player.getUniqueId();
        breakMatch(uuid, true);
        String removedMode = pulledModes.remove(uuid);
        members.remove(uuid);
        spectators.remove(uuid);
        EventPlayerData data = playerData.remove(uuid);
        player.closeInventory();

        if (teleport) {
            Location exit = getExit();
            if (exit == null) exit = player.getWorld().getSpawnLocation();
            player.teleport(exit);
        }
        if (restore && data != null) {
            data.restore(player);
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
        }
        refreshVisibility();
        checkLastEventPlayer();
        if ("sumo".equals(removedMode)
                && eventStarted
                && !victoryAnimationActive) {

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                @Override
                public void run() {

                    if (!eventStarted || victoryAnimationActive) return;

                    Player remaining = null;
                    int count = 0;

                    for (UUID id : snapshotMembers()) {

                        Player p = Bukkit.getPlayer(id);

                        if (p == null || !p.isOnline()) continue;
                        if (isSpectator(id)) continue;
                        if (p.hasPermission("events.ignoreadmin")) continue;

                        if (!"sumo".equals(pulledModes.get(id))) continue;

                        count++;

                        if (count > 1) return;

                        remaining = p;
                    }

                    if (count == 1 && remaining != null) {
                        stopEvent(remaining);
                    }
                }

            }, 1L);
        }
    }
    /**
     * Verifica se o jogador está no countdown de um confronto.
     */
    public boolean isPairMatchCountdown(Player player) {

        if (player == null) return false;

        EventMatch match = eventMatches.get(player.getUniqueId());

        return match != null
                && match.countdownTask != null
                && !match.pvpAllowed;
    }

    public void handleQuit(Player player) {
        if (!isMember(player)) return;
        // Restaura antes do salvamento de dados do Bukkit para não perder o inventário ao relogar.
        removeMember(player, true, true);
    }

    public void openRoom() {
        if (roomOpen) return;
        roomOpen = true;
        startBroadcastTask();
        Bukkit.broadcastMessage(color(message("messages.room-open", "&aA sala de eventos está aberta! Use /evento entrar.")));
    }

    public void closeRoom() {
        if (!roomOpen) {
            stopBroadcastTask();
            return;
        }
        roomOpen = false;
        stopBroadcastTask();
        Bukkit.broadcastMessage(color(message("messages.room-closed-broadcast", "&cA sala de eventos foi fechada.")));
    }

    private void startBroadcastTask() {
        stopBroadcastTask();
        if (!plugin.getConfig().getBoolean("events.broadcast.enabled", true)) return;
        final List<String> messages = plugin.getConfig().getStringList("events.broadcast.messages");
        if (messages.isEmpty()) return;
        long seconds = Math.max(1L, plugin.getConfig().getLong("events.broadcast.interval-seconds", 30L));
        broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!roomOpen) {
                    stopBroadcastTask();
                    return;
                }
                String line = messages.get(broadcastIndex++ % messages.size());
                Bukkit.broadcastMessage(color(line));
            }
        }, seconds * 20L, seconds * 20L);
    }

    private void stopBroadcastTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    public void setPvp(boolean enabled) {
        pvpEnabled = enabled;
        String key = enabled ? "messages.pvp-enabled" : "messages.pvp-disabled";
        String fallback = enabled ? "&aPvP dos eventos ativado." : "&cPvP dos eventos desativado.";
        broadcastToMembers(color(message(key, fallback)));
    }

    public boolean startEvent() {
        if (eventStarted || countdownTask != null) {
            return false;
        }
        if (members.isEmpty()) {
            return false;
        }
        final int startAt = Math.max(0, plugin.getConfig().getInt("events.countdown.seconds", 10));
        final int[] remaining = new int[] { startAt };
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (members.isEmpty()) {
                    countdownTask.cancel();
                    countdownTask = null;
                    return;
                }
                String chat = message("countdown.chat", "&eO evento começa em &f%time%&e...");
                String title = message("countdown.title", "&a%time%");
                String subtitle = message("countdown.subtitle", "&ePrepare-se!");
                chat = replace(chat, null, null, remaining[0]);
                title = replace(title, null, null, remaining[0]);
                subtitle = replace(subtitle, null, null, remaining[0]);
                for (UUID uuid : snapshotMembers()) {
                    Player target = Bukkit.getPlayer(uuid);
                    if (target != null) {
                        target.sendMessage(color(chat));
                        target.sendTitle(color(title), color(subtitle), 0, 20, 0);
                    }
                }
                if (remaining[0] == 0) {
                    countdownTask.cancel();
                    countdownTask = null;
                    eventStarted = true;

                 // Transporta os participantes do Lava para sua arena.
                 teleportLavaPlayersAtStart();
                 if (extras != null) extras.onStart();

                 broadcastToMembers(
                     color(message("messages.started", "&aO evento começou! Boa sorte!"))
                 );

                 checkLastEventPlayer();
                    if (eventStarted) {
                        scheduleAssignedPairModes();
                    }
                } else {
                    remaining[0]--;
                }
            }
        }, 0L, 20L);
        return true;
    }

    public void stopEvent(Player winner) {

        // Permite que o administrador encerre imediatamente com /evento parar.
        if (victoryAnimationActive) {
            if (winner == null) {
                finishEventNow(null);
            }
            return;
        }

        if (!eventStarted && countdownTask == null && members.isEmpty()) {
            return;
        }

        if (winner != null &&
                (!isMember(winner) || isSpectator(winner))) {

            winner.sendMessage(
                "§cEsse jogador não é um participante ativo do evento."
            );
            return;
        }

        // Com vencedor, inicia a animação antes de finalizar.
        if (winner != null) {
            startVictoryAnimation(winner);
            return;
        }

        finishEventNow(null);
    }
    /**
     * Animação de vitória de 5 segundos.
     */
    private void startVictoryAnimation(final Player winner) {

        if (victoryAnimationActive) return;

        if (winner == null || !winner.isOnline()) return;

        victoryAnimationActive = true;
        cancelPairQueueTasks();
        // Desativa o PvP durante a animação.
        pvpEnabled = false;

        // Nenhum confronto pode continuar ativo.
        clearMatches();

        final int[] elapsedTicks = {0};

        victoryAnimationTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    if (!victoryAnimationActive) {
                        if (victoryAnimationTask != null) {
                            victoryAnimationTask.cancel();
                            victoryAnimationTask = null;
                        }
                        return;
                    }

                    // Caso o vencedor saia durante a animação.
                    if (!winner.isOnline()
                            || !isMember(winner)
                            || isSpectator(winner)) {

                        finishEventNow(null);
                        return;
                    }

                    // Cinco segundos completos.
                    if (elapsedTicks[0] >= 100) {

                        finishEventNow(winner);
                        return;
                    }

                    // Atualiza a animação uma vez por segundo.
                    if (elapsedTicks[0] % 20 == 0) {

                        final int remaining =
                                5 - (elapsedTicks[0] / 20);

                        for (UUID uuid : snapshotMembers()) {

                            Player target = Bukkit.getPlayer(uuid);

                            if (target == null || !target.isOnline()) {
                                continue;
                            }

                            if (target.getUniqueId().equals(
                                    winner.getUniqueId())) {

                                target.sendTitle(
                                    "§6§l✦ VITÓRIA! ✦",
                                    "§eVocê venceu o evento!",
                                    0, 25, 5
                                );

                            } else {

                                target.sendTitle(
                                    "§c§lEVENTO FINALIZANDO",
                                    "§eVencedor: §f" + winner.getName(),
                                    0, 25, 5
                                );
                            }

                            target.sendMessage(
                                "§eO evento será encerrado em §c"
                                + remaining + " §esegundos."
                            );
                        }

                        // Efeito visual de fogos ao redor do vencedor.
                        spawnVictoryFirework(winner);
                    }

                    elapsedTicks[0] += 10;
                }

            },
            0L,
            10L
        );
    }

    /**
     * Cria fogos de artifício para a animação.
     */
    private void spawnVictoryFirework(Player winner) {

        if (winner == null || !winner.isOnline()) return;

        Location location = winner.getLocation().clone().add(
            (Math.random() - 0.5D) * 3.0D,
            1.0D,
            (Math.random() - 0.5D) * 3.0D
        );

        final Firework firework = location.getWorld().spawn(
            location,
            Firework.class
        );

        FireworkMeta meta = firework.getFireworkMeta();

        meta.addEffect(
            FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(
                    Color.YELLOW,
                    Color.ORANGE,
                    Color.WHITE
                )
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build()
        );

        meta.setPower(0);

        firework.setFireworkMeta(meta);

        Bukkit.getScheduler().runTaskLater(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    if (!firework.isDead()) {
                        firework.detonate();
                    }
                }

            },
            2L
        );
    }
    private void finishEventNow(Player winner) {
        if (extras != null) extras.onStop();

        if (victoryAnimationTask != null) {
            victoryAnimationTask.cancel();
            victoryAnimationTask = null;
        }

        victoryAnimationActive = false;
        cancelPairQueueTasks();
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        eventStarted = false;
        pvpEnabled = false;

        clearMatches();
        pulledModes.clear();

        Set<UUID> ending = snapshotMembers();

        String winnerName = winner == null
                ? ""
                : winner.getName();

        if (winner != null && plugin.getMySQL() != null) {
            plugin.getMySQL().addEventWin(
                winner.getUniqueId(),
                winner.getName()
            );
        }

        Bukkit.broadcastMessage(
            color(
                replace(
                    message(
                        winner == null
                            ? "messages.stopped"
                            : "messages.winner",

                        winner == null
                            ? "&cO evento foi encerrado."
                            : "&a%player% venceu o evento!"
                    ),
                    winnerName,
                    null,
                    -1
                )
            )
        );

        for (UUID uuid : ending) {

            Player target = Bukkit.getPlayer(uuid);

            if (target == null) continue;

            removeMember(target, true, true);

            dispatchCommands(
                "events.commands.on-stop",
                target,
                ""
            );

            if (winner != null &&
                    winner.getUniqueId().equals(uuid)) {

                dispatchCommands(
                    "events.commands.on-winner",
                    target,
                    ""
                );
            }
        }

        members.clear();
        spectators.clear();
        playerData.clear();

        refreshVisibility();
    }
    public void setMdrAdminLocation(Location location) {
        setPullLocation("mdr-admin", location);
    }

    public Location getMdrAdminLocation() {
        return readLocation("events.pull-locations.mdr-admin.location");
    }
    /**
     * Encerra automaticamente o evento quando resta apenas um participante válido.
     * Jogadores com events.ignoreadmin não entram na contagem.
     */
    private void checkLastEventPlayer() {
    	if (!eventStarted || victoryAnimationActive) return;

        Player remaining = null;
        int aliveCount = 0;

        for (UUID uuid : snapshotMembers()) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) continue;
            if (isSpectator(uuid)) continue;
            if (player.hasPermission("events.ignoreadmin")) continue;

            aliveCount++;

            if (aliveCount > 1) {
                return;
            }

            remaining = player;
        }

        if (aliveCount == 1 && remaining != null) {
            stopEvent(remaining);
        }
    }
    public boolean pullPlayers(String key) {

        final String mode = normalizeKey(key);

        if (mode.isEmpty()) return false;
        if (extras != null && !EventExtras.isExtraMode(mode)) extras.onLegacyPull(mode);

        Set<UUID> current = snapshotMembers();

        if (current.isEmpty()) return false;

        // =========================================
        // 1V1 E SUMO - FILA AUTOMÁTICA
        // =========================================

        if ("1v1".equals(mode) || "sumo".equals(mode)) {

            // Local de espera.
            Location staging = getPullLocation(mode);

            // Locais dos dois lados do confronto.
            Location loc1 = readLocation(
                "events.pull-locations." + mode + ".loc1"
            );

            Location loc2 = readLocation(
                "events.pull-locations." + mode + ".loc2"
            );

            // Verifica todos os locais necessários.
            if (staging == null || loc1 == null || loc2 == null) {

                plugin.getLogger().warning(
                    "[KPDuels] Não foi possível iniciar " + mode.toUpperCase()
                    + ". Configure os spawns location, loc1 e loc2."
                );

                broadcastToMembers(
                    "§cOs locais do evento " + mode.toUpperCase()
                    + " não foram configurados corretamente."
                );

                return false;
            }

            int registered = 0;

            for (UUID uuid : current) {

                Player player = Bukkit.getPlayer(uuid);

                if (player == null || !player.isOnline()) continue;

                if (!isMember(player)) continue;

                if (isSpectator(player)) continue;

                // Retira o jogador de um confronto anterior.
                breakMatch(uuid, true);

                // Registra a modalidade.
                pulledModes.put(uuid, mode);

                player.closeInventory();

                clearPlayerInventory(player);

                player.setGameMode(GameMode.SURVIVAL);
                player.setFlying(false);
                player.setAllowFlight(false);

                player.removePotionEffect(
                    PotionEffectType.INVISIBILITY
                );

                player.setFireTicks(0);
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
                player.setFallDistance(0.0F);

                player.setVelocity(
                    new org.bukkit.util.Vector(0, 0, 0)
                );

                // Se o evento ainda não começou,
                // já transporta para a área de espera.
                if (!eventStarted) {

                    player.teleport(staging.clone());

                    player.sendTitle(
                        "§6§l" + mode.toUpperCase(),
                        "§eAguarde o início do evento!",
                        0, 40, 10
                    );

                    player.sendMessage(
                        "§aVocê foi registrado no evento "
                        + mode.toUpperCase() + "!"
                    );

                    player.sendMessage(
                        "§eAguarde o administrador iniciar o evento."
                    );
                }

                registered++;
            }

            if (registered == 0) {
                return false;
            }

            // Se já estiver iniciado, começa a fila de confrontos.
            if (eventStarted) {

                schedulePairQueue(mode, 400L);

            } else {

                broadcastToMembers(
                    "§aOs participantes de "
                    + mode.toUpperCase()
                    + " foram registrados e estão aguardando o início."
                );
            }

            return true;
        }

        // =========================================
        // DEMAIS MODALIDADES
        // =========================================

        Location destination = readLocation(
            "events.pull-locations." + mode + ".location"
        );

        if (destination == null) return false;

        Location adminDestination = null;

        if ("mdr".equals(mode)) {

            adminDestination = getMdrAdminLocation();

            if (adminDestination == null) {

                for (UUID uuid : current) {

                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null
                            && isMember(player)
                            && !isSpectator(player)
                            && player.hasPermission("events.admin")) {

                        player.sendMessage(
                            "§cO local exclusivo dos administradores do MDR não foi configurado."
                        );

                        return false;
                    }
                }
            }
        }

        List<String> instructions = plugin.getConfig()
            .getStringList(
                "events.pull-locations." + mode + ".instructions"
            );

        for (UUID uuid : current) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !isMember(player)) continue;

            breakMatch(uuid, true);

            Location playerDestination = destination;

            if (!isSpectator(player)) {

                pulledModes.put(uuid, mode);

                clearPlayerInventory(player);

                if ("lava".equals(mode)) {

                    giveLavaKit(player);

                } else if ("mdr".equals(mode)) {

                    player.removePotionEffect(PotionEffectType.SPEED);

                    if (player.hasPermission("events.admin")) {

                        giveMdrAdminKit(player);
                        playerDestination = adminDestination;
                    }
                }

            } else {

                continue;
            }

            player.closeInventory();

            player.teleport(playerDestination.clone());

            player.setFallDistance(0.0F);

            player.setVelocity(
                new org.bukkit.util.Vector(0, 0, 0)
            );

            player.updateInventory();

            for (String instruction : instructions) {

                player.sendMessage(
                    color(
                        replace(
                            instruction,
                            player.getName(),
                            mode,
                            -1
                        )
                    )
                );
            }
        }

        return true;
    }
    /**
     * Agenda o transporte e o pareamento automático.
     */
    private void schedulePairQueue(final String mode, long delayTicks) {

        if (!eventStarted || victoryAnimationActive) return;

        Location staging = getPullLocation(mode);

        Location loc1 = readLocation(
            "events.pull-locations." + mode + ".loc1"
        );

        Location loc2 = readLocation(
            "events.pull-locations." + mode + ".loc2"
        );

        if (staging == null || loc1 == null || loc2 == null) {

            plugin.getLogger().warning(
                "[KPDuels] Spawns incompletos para " + mode.toUpperCase()
            );

            broadcastToMembers(
                "§cNão foi possível iniciar os confrontos de "
                + mode.toUpperCase() + ". Verifique os spawns."
            );

            return;
        }

        // Cancela agendamento anterior.
        BukkitTask previous = pairQueueTasks.remove(mode);

        if (previous != null) {
            previous.cancel();
        }

        int queued = 0;

        // =========================================
        // PRIMEIRA ETAPA: ÁREA DE ESPERA
        // =========================================

        for (UUID uuid : snapshotMembers()) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) continue;

            if (!isMember(player) || isSpectator(player)) continue;

            if (isInMatch(player)) continue;

            if (!mode.equals(pulledModes.get(uuid))) continue;

            player.closeInventory();

            player.setGameMode(GameMode.SURVIVAL);

            player.setFlying(false);
            player.setAllowFlight(false);

            player.removePotionEffect(
                PotionEffectType.INVISIBILITY
            );

            clearPlayerInventory(player);

            player.setHealth(
                Math.min(20.0D, getMaxHealth(player))
            );

            player.setFireTicks(0);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFallDistance(0.0F);

            player.teleport(staging.clone());

            player.setVelocity(
                new org.bukkit.util.Vector(0, 0, 0)
            );

            player.sendTitle(
                "§6§l" + mode.toUpperCase(),
                "§eOs duelos começam em 20 segundos!",
                0, 40, 10
            );

            player.sendMessage(
                "§eVocê está na área de espera. "
                + "Os confrontos começarão em 20 segundos."
            );

            queued++;
        }

        if (queued == 0) return;

        // =========================================
        // SEGUNDA ETAPA: INICIAR CONFRONTOS
        // =========================================

        BukkitTask task = Bukkit.getScheduler().runTaskLater(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    pairQueueTasks.remove(mode);

                    if (!eventStarted || victoryAnimationActive) {
                        return;
                    }

                    int matches = startPairMatches(mode);

                    if (matches > 0) {

                        Bukkit.broadcastMessage(
                            "§a§lDuelos §7- §e"
                            + matches
                            + " confronto(s) de "
                            + mode.toUpperCase()
                            + " iniciado(s) no evento!"
                        );

                    } else if (matches == 0) {

                        broadcastToMembers(
                            "§eAguardando jogadores disponíveis para "
                            + mode.toUpperCase() + "..."
                        );

                    } else {

                        plugin.getLogger().warning(
                            "[KPDuels] Falha ao iniciar os confrontos de "
                            + mode.toUpperCase()
                        );
                    }
                }

            },
            delayTicks
        );

        pairQueueTasks.put(mode, task);
    }

    /**
     * Chamar depois de cada eliminação para iniciar
     * outra rodada após 10 segundos.
     */
    public void scheduleNextPairMatch(String mode) {

        String key = normalizeKey(mode);

        if (!key.equals("1v1") && !key.equals("sumo")) return;

        if (!eventStarted || victoryAnimationActive) return;

        // Nova rodada após 20 segundos.
        schedulePairQueue(key, 400L);
    }

    /**
     * Detecta quais modalidades possuem jogadores registrados.
     */
    private void scheduleAssignedPairModes() {

        String[] modes = { "1v1", "sumo" };

        for (String mode : modes) {

            boolean found = false;

            for (UUID uuid : snapshotMembers()) {

                Player player = Bukkit.getPlayer(uuid);

                if (player == null || isSpectator(player)) continue;

                if (mode.equals(pulledModes.get(uuid))) {
                    found = true;
                    break;
                }
            }

            if (found) {

                // Teleporta imediatamente para o spawn de espera
                // e inicia a contagem de 20 segundos.
                schedulePairQueue(mode, 400L);
            }
        }
    }

    /**
     * Cancela transportes pendentes.
     */
    private void cancelPairQueueTasks() {

        for (BukkitTask task : pairQueueTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }

        pairQueueTasks.clear();
    }
    public void setLavaSpawn(Location location) {
        saveLocation("events.pull-locations.lava.location", location);
    }

    public Location getLavaSpawn() {
        return readLocation("events.pull-locations.lava.location");
    }
    public Location getLavaDamageSpawn() {
        return readLocation("events.pull-locations.lavadamage.location");
    }
    /**
     * Teleporta os participantes do Lava quando o evento começa.
     */
    private void teleportLavaPlayersAtStart() {

        Location lavaSpawn = getLavaDamageSpawn();

        if (lavaSpawn == null) {

            plugin.getLogger().warning(
                "[KPDuels] Spawn do evento Lava Damage não configurado."
            );

            return;
        }

        for (UUID uuid : snapshotMembers()) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) continue;
            if (!isMember(player) || isSpectator(player)) continue;

            // Somente participantes registrados no Lava.
            if (!"lava".equalsIgnoreCase(pulledModes.get(uuid))) {
                continue;
            }

            player.closeInventory();

            player.setGameMode(GameMode.SURVIVAL);

            player.setFlying(false);
            player.setAllowFlight(false);

            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Entrega novamente o kit completo de sopas.
            giveLavaKit(player);

            // Teleporta para o spawn exclusivo do Lava.
            player.teleport(lavaSpawn.clone());

            player.setFallDistance(0.0F);

            player.setVelocity(
                new org.bukkit.util.Vector(0, 0, 0)
            );

            player.sendMessage(
                "§aVocê foi teleportado para o evento Lava!"
            );

            player.sendTitle(
                "§6§lEVENTO LAVA",
                "§ePrepare-se para lutar!",
                0, 40, 10
            );

            player.updateInventory();
        }
    }
    /**
     * Inicia um confronto 1v1 entre participantes previamente puxados
     * para a modalidade indicada por /puxarevento.
     * @return 1 quando iniciado; zero quando faltam jogadores ou já há confronto;
     *         -1 quando o evento/local não está pronto.
     */
    public int startPairMatches(String mode) {
        final String key = normalizeKey(mode);
        if (!(key.equals("1v1") || key.equals("sumo")) || !eventStarted) return -1;

        final Location loc1 = readLocation("events.pull-locations." + key + ".loc1");
        final Location loc2 = readLocation("events.pull-locations." + key + ".loc2");
        if (loc1 == null || loc2 == null) return -1;

        // Há apenas um par de posições loc1/loc2 por modalidade. Portanto,
        // somente um confronto pode ocupar essa arena por vez.
        for (EventMatch active : new HashSet<EventMatch>(eventMatches.values())) {
            if (key.equals(active.mode)) return 0;
        }

        List<Player> waiting = new ArrayList<Player>();
        for (UUID uuid : snapshotMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || isSpectator(player) || eventMatches.containsKey(uuid)) continue;
            if (key.equals(pulledModes.get(uuid))) waiting.add(player);
        }
        if (waiting.size() < 2) return 0;

        Player first = waiting.get(0);
        Player second = waiting.get(1);
        EventMatch match = new EventMatch(key, first.getUniqueId(), second.getUniqueId(), loc1, loc2);
        eventMatches.put(first.getUniqueId(), match);
        eventMatches.put(second.getUniqueId(), match);

        prepareMatchPlayer(first, loc1, key);
        prepareMatchPlayer(second, loc2, key);
        String matchMessage = message("pair-matches.match-found", "&aVocê foi pareado contra &f%opponent%&a!");
        first.sendMessage(color(replace(matchMessage.replace("%opponent%", second.getName()), second.getName(), key, -1)));
        second.sendMessage(color(replace(matchMessage.replace("%opponent%", first.getName()), first.getName(), key, -1)));
        startMatchCountdown(match);

        for (int i = 2; i < waiting.size(); i++) {
            waiting.get(i).sendMessage(color(message("pair-matches.waiting", "&eVocê está aguardando o próximo adversário.")));
        }
        return 1;
    }
    public Location getPullLocation(String mode) {
        return readLocation(
            "events.pull-locations." + normalizeKey(mode) + ".location"
        );
    }
    /**
     * Kit padrão recebido ao entrar no evento.
     */
    public void giveEventEntryKit(Player player) {

        if (player == null || !player.isOnline()) return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Espada de diamante com Sharpness I.
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(
                1,
                new ItemStack(Material.BOW)
            );
        // 64 maçãs douradas.
        player.getInventory().setItem(
            2,
            new ItemStack(Material.GOLDEN_APPLE, 64)
        );

        // 2 packs de flechas (128 flechas).
        player.getInventory().setItem(
            3,
            new ItemStack(Material.ARROW, 32)
        );

        // Armadura: capacete e botas de diamante.
        player.getInventory().setHelmet(
            new ItemStack(Material.DIAMOND_HELMET)
        );

        player.getInventory().setBoots(
            new ItemStack(Material.DIAMOND_BOOTS)
        );

        // Peitoral e calça de ferro.
        player.getInventory().setChestplate(
            new ItemStack(Material.IRON_CHESTPLATE)
        );

        player.getInventory().setLeggings(
            new ItemStack(Material.IRON_LEGGINGS)
        );

        // Mantém a fome e a saturação completas.
        player.setFoodLevel(20);
        player.setSaturation(20.0F);

        player.updateInventory();
    }
    private void prepareMatchPlayer(Player player, Location spawn, String mode) {
        if (player == null || !player.isOnline()) return;
        player.closeInventory();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
       
        player.setHealth(Math.min(20.0D, getMaxHealth(player)));

        // Cada modalidade recebe um inventário controlado pelo evento. Isso
        // impede que itens trazidos de outro modo sejam usados no confronto.
        if ("1v1".equals(mode)) {

            giveEventOneVsOneKit(player);

        } else {

            clearPlayerInventory(player);
        }

        player.teleport(spawn.clone());
        player.setFallDistance(0.0F);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.updateInventory();
    }

    /** Espada no primeiro slot e 35 sopas nos demais slots do inventário. */
    /**
     * Kit oficial do evento 1v1.
     * Armadura completa de ferro e equipamentos de combate.
     */
    
    public void giveEventOneVsOneKit(Player player) {

        if (player == null || !player.isOnline()) return;

        clearPlayerInventory(player);

        // Armadura
        player.getInventory().setHelmet(
            new ItemStack(Material.IRON_HELMET)
        );

        player.getInventory().setChestplate(
            new ItemStack(Material.IRON_CHESTPLATE)
        );

        player.getInventory().setLeggings(
            new ItemStack(Material.IRON_LEGGINGS)
        );

        player.getInventory().setBoots(
            new ItemStack(Material.IRON_BOOTS)
        );

        // Espada de diamante Sharpness I
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);

        player.getInventory().setItem(0, sword);

        // Inventário completo de sopas
        for (int slot = 1; slot < 36; slot++) {
            player.getInventory().setItem(
                slot,
                new ItemStack(Material.MUSHROOM_STEW)
            );
        }

        player.updateInventory();
    }
    public void clearPlayerInventory(Player player) {
        if (player == null) return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setItemOnCursor(null);

        player.updateInventory();
    }

    public void giveOneVsOneKit(Player player) {
        giveEventOneVsOneKit(player);
    }
    /** Kit de Lava: inventário inteiro com sopa e nenhuma peça de armadura. */
    private void giveLavaKit(Player player) {
        if (player == null || !player.isOnline()) return;
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        int storageSlots = player.getInventory().getStorageContents().length;
        for (int slot = 0; slot < storageSlots; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.MUSHROOM_STEW));
        }
        player.setHealth(Math.min(20.0D, getMaxHealth(player)));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.updateInventory();
    }

    private void startMatchCountdown(final EventMatch match) {
        if (match == null) return;
        if (match.countdownTask != null) {
            match.countdownTask.cancel();
            match.countdownTask = null;
        }
        match.pvpAllowed = false;
        match.resetScheduled = false;
        final int[] remaining = new int[] { Math.max(0, plugin.getConfig().getInt("events.pair-matches.countdown-seconds", 5)) };
        match.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                Player first = Bukkit.getPlayer(match.first);
                Player second = Bukkit.getPlayer(match.second);
                if (first == null || second == null || !isMember(first) || !isMember(second)
                        || eventMatches.get(match.first) != match || eventMatches.get(match.second) != match) {
                    cancelMatch(match);
                    return;
                }

                String chat = message("pair-matches.countdown-chat", "&eA luta começa em &f%time%&e...");
                String title = message("pair-matches.countdown-title", "&a%time%");
                String subtitle = message("pair-matches.countdown-subtitle", "&ePrepare-se!");
                for (Player target : new Player[] { first, second }) {
                    target.sendMessage(color(replace(chat, null, match.mode, remaining[0])));
                    target.sendTitle(color(replace(title, null, match.mode, remaining[0])),
                            color(replace(subtitle, null, match.mode, remaining[0])), 0, 20, 0);
                }

                if (remaining[0] == 0) {
                    match.countdownTask.cancel();
                    match.countdownTask = null;
                    match.pvpAllowed = true;
                    String started = color(message("pair-matches.started", "&a&lLUTE! &7PvP liberado para este confronto."));
                    first.sendMessage(started);
                    second.sendMessage(started);
                    first.sendTitle("§a§lLUTE!", "§7Boa sorte!", 0, 30, 10);
                    second.sendTitle("§a§lLUTE!", "§7Boa sorte!", 0, 30, 10);
                } else {
                    remaining[0]--;
                }
            }
        }, 0L, 20L);
    }

    private void breakMatch(UUID uuid, boolean notifyOpponent) {
        EventMatch match = eventMatches.remove(uuid);
        if (match == null) return;
        cancelMatch(match);
        eventMatches.remove(match.first);
        eventMatches.remove(match.second);
        UUID opponentId = match.first.equals(uuid) ? match.second : match.first;
        Player opponent = Bukkit.getPlayer(opponentId);
        if (notifyOpponent && opponent != null && isMember(opponent)) {
            opponent.sendMessage(color(message("pair-matches.opponent-left", "&cSeu adversário saiu do confronto.")));
        }
    }

    private void cancelMatch(EventMatch match) {
        if (match == null) return;
        match.pvpAllowed = false;
        if (match.countdownTask != null) {
            match.countdownTask.cancel();
            match.countdownTask = null;
        }
    }

    private void clearMatches() {
        Set<EventMatch> unique = new HashSet<EventMatch>(eventMatches.values());
        for (EventMatch match : unique) cancelMatch(match);
        eventMatches.clear();
    }

    public boolean isInMatch(Player player) {
        return player != null && eventMatches.containsKey(player.getUniqueId());
    }

    /** True enquanto o participante estiver associado ao modo Lava puxado pelo admin. */
    public boolean isLavaParticipant(Player player) {
        return player != null && isMember(player) && !isSpectator(player)
                && "lava".equals(pulledModes.get(player.getUniqueId()));
    }

    public String getPulledMode(Player player) {
        return player == null ? null : pulledModes.get(player.getUniqueId());
    }

    public Location getMatchSpawn(Player player) {
        if (player == null) return null;
        EventMatch match = eventMatches.get(player.getUniqueId());
        if (match == null) return null;
        return match.first.equals(player.getUniqueId()) ? match.firstSpawn.clone() : match.secondSpawn.clone();
    }

    /** Chamado após o respawn para reiniciar os dois lados do round. */
    public void restartMatchAfterDeath(Player respawned) {
        if (respawned == null || !respawned.isOnline()) return;
        final EventMatch match = eventMatches.get(respawned.getUniqueId());
        if (match == null || !match.resetScheduled) return;
        match.resetScheduled = false;
        Player first = Bukkit.getPlayer(match.first);
        Player second = Bukkit.getPlayer(match.second);
        if (first == null || second == null || !isMember(first) || !isMember(second)) {
            breakMatch(respawned.getUniqueId(), true);
            return;
        }
        prepareMatchPlayer(first, match.firstSpawn, match.mode);
        prepareMatchPlayer(second, match.secondSpawn, match.mode);
        String resetMessage = color(message("pair-matches.round-reset", "&eUm jogador morreu. O confronto será reiniciado!"));
        first.sendMessage(resetMessage);
        second.sendMessage(resetMessage);
        startMatchCountdown(match);
    }

    /** Retorna a modalidade do confronto atual do jogador, ou null se não estiver pareado. */
    public String getMatchMode(Player player) {
        if (player == null) return null;
        EventMatch match = eventMatches.get(player.getUniqueId());
        return match == null ? null : match.mode;
    }

    public boolean isInSumoMatch(Player player) {
        return "sumo".equals(getMatchMode(player));
    }

    /** Confirma que os dois jogadores pertencem ao mesmo confronto de Sumo. */
    public boolean areInSameSumoMatch(Player first, Player second) {
        if (first == null || second == null) return false;
        EventMatch firstMatch = eventMatches.get(first.getUniqueId());
        EventMatch secondMatch = eventMatches.get(second.getUniqueId());
        return firstMatch != null && firstMatch == secondMatch && "sumo".equals(firstMatch.mode);
    }

    public boolean canEventPlayersFight(Player attacker, Player victim) {

        if (!eventStarted || victoryAnimationActive) return false;

        if (attacker == null || victim == null
                || attacker.equals(victim)
                || !isMember(attacker)
                || !isMember(victim)
                || isSpectator(attacker)
                || isSpectator(victim)) {
            return false;
        }

        EventMatch attackerMatch =
            eventMatches.get(attacker.getUniqueId());

        EventMatch victimMatch =
            eventMatches.get(victim.getUniqueId());

        if (attackerMatch != null || victimMatch != null) {

            return attackerMatch != null
                    && attackerMatch == victimMatch
                    && attackerMatch.pvpAllowed;
        }

        String attackerMode = pulledModes.get(attacker.getUniqueId());
        String victimMode = pulledModes.get(victim.getUniqueId());

        // Jogadores aguardando 1v1 ou Sumo não podem lutar
        // fora de um confronto pareado.
        if ("1v1".equals(attackerMode)
                || "sumo".equals(attackerMode)
                || "1v1".equals(victimMode)
                || "sumo".equals(victimMode)) {
            return false;
        }

        return pvpEnabled;
    }

    public void setWinner(Player winner) {
        if (!eventStarted) {
            winner.sendMessage("§cO evento ainda não foi iniciado.");
            return;
        }
        if (!isMember(winner)) {
            winner.sendMessage("§cEsse jogador não está no evento.");
            return;
        }
        if (isSpectator(winner)) {
            winner.sendMessage("§cUm espectador não pode ser marcado como vencedor.");
            return;
        }
        stopEvent(winner);
    }

    public void handleDeath(Player victim, Player killer) {
        if (!isMember(victim)) return;
        // Toda morte enquanto membro do evento conta como death do evento,
        // inclusive eliminações, mortes ambientais e rounds de 1v1/Sumo.
        if (plugin.getMySQL() != null) {
            plugin.getMySQL().addEventDeath(victim.getUniqueId(), victim.getName());
        }
        EventMatch match = eventMatches.get(victim.getUniqueId());
        boolean validMatchKill = match != null && match.pvpAllowed && killer != null
                && !killer.equals(victim) && eventMatches.get(killer.getUniqueId()) == match
                && isMember(killer) && !isSpectator(killer);
        boolean validGeneralKill = match == null && eventStarted && pvpEnabled && killer != null
                && !killer.equals(victim) && isMember(killer) && !isSpectator(killer) && !isSpectator(victim);
        if ((validMatchKill || validGeneralKill) && plugin.getMySQL() != null) {
            plugin.getMySQL().addEventKill(killer.getUniqueId(), killer.getName());
        }
        if (match != null) {
            match.pvpAllowed = false;
            match.resetScheduled = true;
            if (match.countdownTask != null) {
                match.countdownTask.cancel();
                match.countdownTask = null;
            }
        }
    }

    public void makeSpectator(Player player) {
        makeSpectator(player, true);
    }

    public void makeSpectator(Player player, boolean checkLastPlayer) {

        if (!isMember(player) || isInMatch(player)) return;

        boolean firstTime = spectators.add(player.getUniqueId());

        player.closeInventory();

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);

        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.INVISIBILITY,
                PotionEffect.INFINITE_DURATION,
                0,
                true,
                false,
                false
            )
        );

        player.getInventory().setItem(
            0,
            namedItem(
                Material.COMPASS,
                "§aTeletransportar jogadores",
                "§7Clique para assistir aos participantes."
            )
        );

        player.getInventory().setItem(
            8,
            namedItem(
                Material.RED_BED,
                "§cSair do evento",
                "§7Clique para voltar ao spawn de saída."
            )
        );

        player.getInventory().setHeldItemSlot(0);

        if (firstTime) {
            player.sendMessage(
                "§eVocê foi eliminado e agora é um espectador."
            );
        }

        refreshVisibility();

        if (checkLastPlayer) {
            checkLastEventPlayer();
        }
    }

    public void openSpectatorMenu(Player spectator) {
        if (!isMember(spectator) || !isSpectator(spectator)) return;
        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(null, 54, "§8Espectadores do Evento");
        int slot = 0;
        for (UUID uuid : snapshotMembers()) {
            if (slot >= inventory.getSize()) break;
            if (spectators.contains(uuid)) continue;
            Player target = Bukkit.getPlayer(uuid);
            if (target == null) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§a" + target.getName());
                meta.setLore(Collections.singletonList("§7Clique para assistir a este jogador."));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot++, head);
        }
        if (slot == 0) {
            spectator.sendMessage("§cNão há jogadores vivos no evento.");
            return;
        }
        spectator.openInventory(inventory);
    }

    public void teleportSpectatorTo(Player spectator, String targetName) {
        if (!isMember(spectator) || !isSpectator(spectator)) return;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !isMember(target) || isSpectator(target)) {
            spectator.sendMessage("§cEsse jogador não está mais participando.");
            return;
        }
        spectator.teleport(target.getLocation());
        spectator.sendMessage("§aVocê está assistindo: §f" + target.getName());
    }

    public void refreshVisibility() {
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (!isSpectator(spectator)) continue;
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(spectator)) continue;
                if (isSpectator(viewer)) {
                    viewer.showPlayer(plugin, spectator);
                } else {
                    viewer.hidePlayer(plugin, spectator);
                }
            }
        }
    }

    private void broadcastToMembers(String message) {
        for (UUID uuid : snapshotMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(message);
        }
    }

    private Set<UUID> snapshotMembers() {
        synchronized (members) {
            return new LinkedHashSet<UUID>(members);
        }
    }

    private void dispatchCommands(String path, Player player, String eventName) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String configured : plugin.getConfig().getStringList(path)) {
            String command = replace(configured, player.getName(), player.getUniqueId(), eventName, -1);
            if (command.startsWith("/")) command = command.substring(1);
            if (!command.trim().isEmpty()) Bukkit.dispatchCommand(console, command);
        }
    }

    /**
     * Executa comandos de configuração usando a permissão e a identidade do jogador.
     * Não usa o console: comandos como kits, menus e permissões enxergam o jogador real.
     */
    private void dispatchPlayerCommands(Player player, String path, String eventName) {
        if (player == null || !player.isOnline()) return;
        for (String configured : plugin.getConfig().getStringList(path)) {
            if (configured == null || configured.trim().isEmpty()) continue;
            String command = replace(configured.trim(), player.getName(), player.getUniqueId(), eventName, -1);
            if (command.startsWith("/")) command = command.substring(1);
            if (command.trim().isEmpty()) continue;
            try {
                Bukkit.dispatchCommand(player, command);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Não foi possível executar como " + player.getName()
                        + " o comando configurado em " + path + ": " + command + " (" + ex.getMessage() + ")");
            }
        }
    }

    /** Retorna true quando o comando deve ser permitido para um membro do evento. */
    public boolean isCommandAllowedInEvent(String input) {
        if (!plugin.getConfig().getBoolean("events.command-whitelist.enabled", true)) return true;
        if (input == null) return false;

        String command = normalizeCommandForWhitelist(input);
        if (command.isEmpty()) return false;

        for (String configured : plugin.getConfig().getStringList("events.command-whitelist.allowed")) {
            if (configured == null || configured.trim().isEmpty()) continue;
            String allowed = normalizeCommandForWhitelist(configured);
            if (allowed.isEmpty()) continue;
            // A entrada pode ser apenas o comando (ex.: "msg") ou um caminho
            // específico (ex.: "evento sair"). O limite evita casar "sair2".
            if (command.equals(allowed) || command.startsWith(allowed + " ")) return true;
        }
        return false;
    }

    private String normalizeCommandForWhitelist(String input) {
        String command = input.trim().toLowerCase(java.util.Locale.ROOT);
        while (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isEmpty()) return "";

        // Remove namespace padrão ou de plugins (ex.: minecraft:msg, essentials:spawn).
        int firstSpace = command.indexOf(' ');
        String label = firstSpace >= 0 ? command.substring(0, firstSpace) : command;
        String arguments = firstSpace >= 0 ? command.substring(firstSpace) : "";
        int namespace = label.indexOf(':');
        if (namespace >= 0 && namespace + 1 < label.length()) {
            label = label.substring(namespace + 1);
        }
        return (label + arguments).trim();
    }

    public String getBlockedCommandMessage() {
        return color(plugin.getConfig().getString("events.command-whitelist.blocked-message",
                "&cEsse comando não está permitido durante o evento."));
    }

    private String message(String path, String fallback) {
        return plugin.getConfig().getString("events." + path, fallback);
    }

    private String replace(String text, String player, String eventName, int time) {
        return replace(text, player, null, eventName, time);
    }

    private String replace(String text, String player, UUID uuid, String eventName, int time) {
        if (text == null) return "";
        String result = text;
        if (player != null) result = result.replace("%player%", player);
        if (uuid != null) result = result.replace("%uuid%", uuid.toString());
        if (eventName != null) result = result.replace("%event%", eventName).replace("%local%", eventName);
        if (time >= 0) result = result.replace("%time%", String.valueOf(time));
        return result;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase().replaceAll("[^a-z0-9_-]", "");
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private double getMaxHealth(Player player) {
        try {
            if (player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                return player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            }
        } catch (NoSuchFieldError ignored) {
            // Usa o padrão em implementações antigas.
        }
        return 20.0D;
    }

    private ItemStack namedItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleJoin(Player player) {
        if (player != null && plugin.getMySQL() != null) {
            plugin.getMySQL().loadEventStatsAsync(player.getUniqueId());
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                refreshVisibility();
            }
        });
    }

    private static final class EventMatch {
        private final String mode;
        private final UUID first;
        private final UUID second;
        private final Location firstSpawn;
        private final Location secondSpawn;
        private boolean pvpAllowed;
        private boolean resetScheduled;
        private BukkitTask countdownTask;

        private EventMatch(String mode, UUID first, UUID second, Location firstSpawn, Location secondSpawn) {
            this.mode = mode;
            this.first = first;
            this.second = second;
            this.firstSpawn = firstSpawn.clone();
            this.secondSpawn = secondSpawn.clone();
        }
    }
    /**
     * Kit exclusivo dos administradores no MDR.
     */
    private void giveMdrAdminKit(Player player) {

        if (player == null || !player.isOnline()) return;

        // Stick com Sharpness 1000.
        ItemStack stick = new ItemStack(Material.STICK);

        stick.addUnsafeEnchantment(
            Enchantment.SHARPNESS,
            1000
        );

        player.getInventory().setItem(0, stick);

        // Velocidade II com duração infinita.
        player.removePotionEffect(PotionEffectType.SPEED);

        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION,
                1,
                true,
                false,
                false
            )
        );

        player.updateInventory();
    }
    public void shutdown() {
        if (extras != null) extras.shutdown();
        stopBroadcastTask();
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        cancelPairQueueTasks();
        eventStarted = false;
        pvpEnabled = false;
        clearMatches();
        pulledModes.clear();
        for (UUID uuid : snapshotMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) removeMember(player, true, true);
        }
        members.clear();
        spectators.clear();
        playerData.clear();
        if (victoryAnimationTask != null) {
            victoryAnimationTask.cancel();
            victoryAnimationTask = null;
        }

        victoryAnimationActive = false;
        instance = null;
    }
}
