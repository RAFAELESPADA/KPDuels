
package me.rafaelauler.duels;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class EventListener implements Listener {

    private final DuelPlugin plugin;
    private final EventManager events;

    private final Set<UUID> oneVsOneEliminated =
            new HashSet<UUID>();

    private final Set<UUID> sumoWaterHandled =
            new HashSet<UUID>();

    private final Set<UUID> sumoEliminated =
            new HashSet<UUID>();

    public EventListener(DuelPlugin plugin, EventManager events) {
        this.plugin = plugin;
        this.events = events;
    }

    // =====================================================
    // SUMO - VERIFICAR ULTIMO PARTICIPANTE
    // =====================================================

    /**
     * Conta somente os participantes ativos que foram puxados
     * para o Sumo.
     */
    private void checkSumoRemaining() {

        if (!events.isEventStarted()) return;

        if (events.isVictoryAnimationActive()) return;

        Player remaining = null;
        int aliveCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!events.isMember(player)) continue;

            if (events.isSpectator(player)) continue;

            if (player.hasPermission("events.ignoreadmin")) {
                continue;
            }

            if (!"sumo".equalsIgnoreCase(
                    events.getPulledMode(player))) {
                continue;
            }

            aliveCount++;

            if (aliveCount > 1) {
                return;
            }

            remaining = player;
        }

        if (aliveCount == 1 && remaining != null) {

            remaining.sendMessage(
                ChatColor.GOLD
                + "Você é o último sobrevivente do Sumo!"
            );

            // Inicia a animação de vitória de 5 segundos.
            events.stopEvent(remaining);
        }
    }

    private void scheduleSumoCheck() {

        Bukkit.getScheduler().runTaskLater(
            plugin,
            new Runnable() {

                @Override
                public void run() {
                    checkSumoRemaining();
                }

            },
            1L
        );
    }

    // =====================================================
    // SUMO - DETECTAR AGUA
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onSumoWater(PlayerMoveEvent event) {

        Player loser = event.getPlayer();

        if (!events.isMember(loser)) return;

        if (events.isSpectator(loser)) return;

        if (!events.isInSumoMatch(loser)) {
            sumoWaterHandled.remove(loser.getUniqueId());
            return;
        }

        Location to = event.getTo();

        if (to == null || !isWaterContact(to)) return;

        Player winner = findPairOpponent(loser);

        if (winner == null) return;

        // Não elimina durante o countdown.
        if (!events.canEventPlayersFight(loser, winner)) {
            return;
        }

        if (!sumoWaterHandled.add(loser.getUniqueId())) {
            return;
        }

        // Registra estatísticas antes de remover o confronto.
        events.handleDeath(loser, winner);

        events.finishPairMatch(loser);

        loser.sendMessage(
            ChatColor.RED
            + "Você caiu na água e foi eliminado do Sumo!"
        );

        winner.sendMessage(
            ChatColor.GREEN
            + "Seu adversário caiu na água. Você venceu!"
        );

        Bukkit.getScheduler().runTask(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    // Remove o perdedor completamente do evento.
                    if (loser.isOnline()
                            && events.isMember(loser)) {

                        events.leave(loser);
                    }

                    // O vencedor continua no evento.
                    if (winner.isOnline()
                            && events.isMember(winner)
                            && !events.isSpectator(winner)) {

                    	Location spawn = getPairWaitingSpawn("sumo");

                        if (spawn != null) {
                            winner.teleport(spawn);
                        }

                        winner.setFallDistance(0.0F);
                    }

                    sumoWaterHandled.remove(
                        loser.getUniqueId()
                    );
                  
                    // Verifica se resta somente um jogador no Sumo.
                    checkSumoRemaining();
                    if (events.isEventStarted()
                            && !events.isVictoryAnimationActive()) {

                        events.scheduleNextPairMatch("sumo");
                    }  
                }
            }
        );
    }

    private boolean isWaterContact(Location location) {

        if (location.getBlock().getType() == Material.WATER) {
            return true;
        }

        if (location.clone().add(0, 0.4, 0)
                .getBlock().getType() == Material.WATER) {
            return true;
        }

        return location.clone().add(0, -0.1, 0)
                .getBlock().getType() == Material.WATER;
    }

    // =====================================================
    // 1V1 - BLOQUEAR DANO AMBIENTAL
    // =====================================================


@EventHandler(
    priority = EventPriority.HIGHEST,
    ignoreCancelled = false
)
public void onOneVsOneDamage(EntityDamageEvent event) {

    if (!(event.getEntity() instanceof Player)) return;

    Player victim = (Player) event.getEntity();

    if (!events.isInMatch(victim)) return;

    if (!"1v1".equalsIgnoreCase(
            events.getMatchMode(victim))) {
        return;
    }

    // Ataques entre jogadores são tratados
    // pelo onPlayerVsPlayer().
    if (event instanceof EntityDamageByEntityEvent) {
        return;
    }

    // Bloqueia todo dano ambiental.
    event.setCancelled(true);
}


    // =====================================================
    // PVP DOS PARTICIPANTES
    // =====================================================


@EventHandler(
    priority = EventPriority.HIGHEST,
    ignoreCancelled = true
)
public void onPlayerVsPlayer(EntityDamageByEntityEvent event) {

    if (!(event.getEntity() instanceof Player)) return;

    Player victim = (Player) event.getEntity();

    Player attacker = getAttackingPlayer(
        event.getDamager()
    );

    // Bloqueia ataques de mobs ou outras entidades
    // contra participantes que estejam em confrontos.
    if (attacker == null) {

        if (events.isMember(victim)
                && (events.isSpectator(victim)
                    || events.isInMatch(victim))) {

            event.setCancelled(true);
        }

        return;
    }

    boolean victimInEvent = events.isMember(victim);
    boolean attackerInEvent = events.isMember(attacker);

    if (!victimInEvent && !attackerInEvent) return;

    // Só permite ataques autorizados pelo EventManager.
    if (!events.canEventPlayersFight(attacker, victim)) {

        event.setCancelled(true);

        return;
    }

    // SUMO: sem dano, mas com knockback.
    if (events.areInSameSumoMatch(attacker, victim)) {

        event.setDamage(0.0D);

        applySumoKnockback(attacker, victim);
    }
}


    // =====================================================
    // SUMO - KNOCKBACK E PROTECAO
    // =====================================================


@EventHandler(
    priority = EventPriority.HIGHEST,
    ignoreCancelled = false
)
public void onSumoDamage(EntityDamageEvent event) {

    if (!(event.getEntity() instanceof Player)) return;

    Player victim = (Player) event.getEntity();

    if (!events.isInSumoMatch(victim)) return;

    // O dano entre jogadores é tratado em
    // onPlayerVsPlayer().
    if (event instanceof EntityDamageByEntityEvent) {
        return;
    }

    // Impede que o jogador morra no Void.
    if (event.getCause()
            == EntityDamageEvent.DamageCause.VOID) {

        Location matchSpawn = events.getMatchSpawn(victim);

        if (matchSpawn != null) {

            victim.setFallDistance(0.0F);

            victim.teleport(matchSpawn);
        }
    }

    // Bloqueia queda, fogo, lava e qualquer
    // outro dano ambiental durante o Sumo.
    event.setCancelled(true);
}


    private void applySumoKnockback(
            final Player attacker,
            final Player victim) {

        final double horizontal = Math.max(
            0.0D,
            plugin.getConfig().getDouble(
                "events.pair-matches.sumo-knockback.horizontal",
                0.55D
            )
        );

        final double vertical = Math.max(
            0.0D,
            plugin.getConfig().getDouble(
                "events.pair-matches.sumo-knockback.vertical",
                0.32D
            )
        );

        Bukkit.getScheduler().runTask(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    if (!attacker.isOnline()
                            || !victim.isOnline()
                            || !events.areInSameSumoMatch(
                                attacker, victim)
                            || !events.canEventPlayersFight(
                                attacker, victim)) {
                        return;
                    }

                    org.bukkit.util.Vector direction =
                        victim.getLocation().toVector()
                            .subtract(
                                attacker.getLocation().toVector()
                            );

                    direction.setY(0.0D);

                    if (direction.lengthSquared() < 0.0001D) {

                        direction = attacker.getLocation()
                            .getDirection().multiply(-1.0D);

                        direction.setY(0.0D);
                    }

                    if (direction.lengthSquared() < 0.0001D) {
                        return;
                    }

                    direction.normalize().multiply(horizontal);

                    direction.setY(
                        Math.max(
                            vertical,
                            victim.getVelocity().getY()
                        )
                    );

                    victim.setVelocity(direction);
                }
            }
        );
    }

    // =====================================================
    // ESPECTADORES - PROTECAO
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onSpectatorDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (events.isMember(player)
                && events.isSpectator(player)) {

            event.setCancelled(true);
        }
    }

    // =====================================================
    // PROTECAO DURANTE ANIMACAO DE VITORIA
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = false
    )
    public void onVictoryAnimationDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!events.isVictoryAnimationActive()) return;

        if (!events.isMember(player)) return;

        event.setCancelled(true);

        player.setFireTicks(0);
        player.setFallDistance(0.0F);
    }

    // =====================================================
    // PROTECAO ANTES DO INICIO DO EVENTO
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = false
    )
    public void onPreStartDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!events.isMember(player) || events.isEventStarted()) {
            return;
        }

        event.setCancelled(true);

        clearPreStartHazard(player, event.getCause());
    }

    private void clearPreStartHazard(
            Player player,
            EntityDamageEvent.DamageCause cause) {

        switch (cause) {

            case FIRE:
            case FIRE_TICK:
            case LAVA:
                player.setFireTicks(0);
                break;

            case POISON:
                player.removePotionEffect(
                    org.bukkit.potion.PotionEffectType.POISON
                );
                break;

            case WITHER:
                player.removePotionEffect(
                    org.bukkit.potion.PotionEffectType.WITHER
                );
                break;

            case FREEZE:
                player.setFreezeTicks(0);
                break;

            case DROWNING:
                player.setRemainingAir(
                    player.getMaximumAir()
                );
                break;

            case STARVATION:
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
                break;

            case VOID:
            case SUFFOCATION:

                Location safeSpawn = events.getEventSpawn();

                if (safeSpawn != null) {
                    player.setFallDistance(0.0F);
                    player.teleport(safeSpawn);
                }

                break;

            default:
                break;
        }
    }

    // =====================================================
    // MORTE DOS JOGADORES
    // =====================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {

        Player victim = event.getEntity();

        if (!events.isMember(victim)) return;

        // Identifica a modalidade pela qual o jogador foi puxado.
        boolean sumo = "sumo".equalsIgnoreCase(
            events.getPulledMode(victim)
        );

        boolean sumoMatch = events.isInSumoMatch(victim);

        boolean oneVsOne = events.isInMatch(victim)
                && "1v1".equalsIgnoreCase(
                    events.getMatchMode(victim)
                );

        Player winner;

        if (sumoMatch || oneVsOne) {
            winner = findPairOpponent(victim);
        } else {
            winner = victim.getKiller();
        }

        // Registra estatísticas antes de encerrar o confronto.
        events.handleDeath(victim, winner);

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        // =============================================
        // ELIMINACAO NO SUMO
        // =============================================

        if (sumo) {

            if (sumoMatch) {
                events.finishPairMatch(victim);
            }

            sumoEliminated.add(victim.getUniqueId());

            victim.sendMessage(
                ChatColor.RED
                + "Você morreu e foi eliminado do evento Sumo!"
            );

            if (winner != null) {

                winner.sendMessage(
                    ChatColor.GREEN
                    + "Você venceu o confronto de Sumo!"
                );

                final Player matchWinner = winner;

                Bukkit.getScheduler().runTask(
                    plugin,
                    new Runnable() {

                        @Override
                        public void run() {

                            if (!matchWinner.isOnline()
                                    || !events.isMember(matchWinner)
                                    || events.isSpectator(matchWinner)) {
                                return;
                            }

                            Location spawn = getPairWaitingSpawn("sumo");

                            if (spawn != null) {
                                matchWinner.teleport(spawn);
                            }

                            matchWinner.setFallDistance(0.0F);
                        }
                    }
                );
            }

            return;
        }

        // =============================================
        // ELIMINACAO NO 1V1
        // =============================================

        if (!oneVsOne) return;

        events.finishPairMatch(victim);

        oneVsOneEliminated.add(victim.getUniqueId());
     // Após a eliminação, o vencedor voltará à fila
     // e será puxado novamente em 10 segundos.
        victim.sendMessage(
            ChatColor.RED + "Você perdeu o duelo 1v1!"
        );

        if (winner == null) return;

        final Player matchWinner = winner;

        Bukkit.getScheduler().runTask(
            plugin,
            new Runnable() {

                @Override
                public void run() {

                    if (!matchWinner.isOnline()
                            || !events.isMember(matchWinner)
                            || events.isSpectator(matchWinner)) {
                        return;
                    }

                    events.clearPlayerInventory(matchWinner);

                    Location spawn = getPairWaitingSpawn("1v1");

                    if (spawn != null) {
                        matchWinner.teleport(spawn);
                    }

                    matchWinner.setFallDistance(0.0F);

                    matchWinner.sendMessage(
                        ChatColor.GREEN
                        + "Você venceu o duelo 1v1!"
                    );
                }
            }
        );
    }

    // =====================================================
    // RESPAWN
    // =====================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {

        final Player player = event.getPlayer();

        if (!events.isMember(player)) return;

        // =============================================
        // SUMO - REMOVER JOGADOR APOS MORTE
        // =============================================

        if (sumoEliminated.remove(player.getUniqueId())) {

            Location exit = events.getExit();

            if (exit == null) {
                exit = events.getEventSpawn();
            }

            if (exit != null) {
                event.setRespawnLocation(exit);
            }

            new BukkitRunnable() {

                @Override
                public void run() {

                    if (player.isOnline()
                            && events.isMember(player)) {

                        // Remove o jogador completamente.
                        // Restaura inventário e estado anteriores.
                        events.leave(player);
                    }

                    scheduleSumoCheck();
                    if (events.isEventStarted()
                            && !events.isVictoryAnimationActive()) {

                        events.scheduleNextPairMatch("sumo");
                    }
                }

            }.runTaskLater(plugin, 2L);

            return;
        }

        // =============================================
        // SUMO - REINICIO DE ROUND NORMAL
        // =============================================

        if (events.isInMatch(player)
                && events.isInSumoMatch(player)) {

            Location matchSpawn = events.getMatchSpawn(player);

            if (matchSpawn != null) {
                event.setRespawnLocation(matchSpawn);
            }

            new BukkitRunnable() {

                @Override
                public void run() {

                    if (player.isOnline()
                            && events.isMember(player)) {

                        events.restartMatchAfterDeath(player);
                    }
                }

            }.runTaskLater(plugin, 2L);

            return;
        }

        // =============================================
        // DEMAIS MODALIDADES
        // =============================================

        if (events.getEventSpawn() != null) {

            event.setRespawnLocation(events.getEventSpawn());

        } else if (events.getExit() != null) {

            event.setRespawnLocation(events.getExit());
        }

        final boolean pairEliminated =
                oneVsOneEliminated.remove(
                    player.getUniqueId()
                );

        new BukkitRunnable() {

            @Override
            public void run() {

                if (!player.isOnline()
                        || !events.isMember(player)) {
                    return;
                }

                events.makeSpectator(
                    player,
                    !pairEliminated
                );

                // O perdedor já está como espectador.
                // O vencedor retorna à espera por 20 segundos.
                if (pairEliminated
                        && events.isEventStarted()
                        && !events.isVictoryAnimationActive()) {

                    events.scheduleNextPairMatch("1v1");
                }
            }

        }.runTaskLater(plugin, 2L);
    }
    private Location getPairWaitingSpawn(String mode) {

        Location spawn = events.getPullLocation(mode);

        if (spawn == null) {
            spawn = events.getEventSpawn();
        }

        return spawn;
    }
    // =====================================================
    // DROP DE ESPADAS E ARCOS
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onEventWeaponDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (!events.isMember(player)) return;

        Material material = event.getItemDrop()
                .getItemStack()
                .getType();

        String type = material.name();

        if (type.endsWith("_SWORD")
                || type.equals("BOW")
                || type.equals("CROSSBOW")) {

            event.setCancelled(true);

            player.sendMessage(
                ChatColor.RED
                + "Você não pode dropar espadas ou arcos durante o evento!"
            );
        }
    }

    // =====================================================
    // JOIN
    // =====================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        oneVsOneEliminated.remove(uuid);
        sumoWaterHandled.remove(uuid);
        sumoEliminated.remove(uuid);

        events.handleJoin(event.getPlayer());
    }

    // =====================================================
    // QUIT
    // =====================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        boolean wasSumo = "sumo".equalsIgnoreCase(
            events.getPulledMode(player)
        );

        UUID uuid = player.getUniqueId();

        oneVsOneEliminated.remove(uuid);
        sumoWaterHandled.remove(uuid);
        sumoEliminated.remove(uuid);

        events.handleQuit(player);

        if (wasSumo) {
            scheduleSumoCheck();
        }
    }

    // =====================================================
    // ESPECTADOR - INTERAGIR
    // =====================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (!events.isMember(player)
                || !events.isSpectator(player)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();

        if (item.getType() == Material.COMPASS
                && name.equals("§aTeletransportar jogadores")) {

            events.openSpectatorMenu(player);

        } else if (item.getType() == Material.RED_BED
                && name.equals("§cSair do evento")) {

            events.leave(player);
        }
    }

    // =====================================================
    // ESPECTADOR - INVENTARIO
    // =====================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!events.isMember(player)
                || !events.isSpectator(player)) {
            return;
        }

        event.setCancelled(true);

        if (!event.getView().getTitle()
                .equals("§8Espectadores do Evento")) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null
                || clicked.getType() != Material.PLAYER_HEAD
                || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) return;

        String targetName = ChatColor.stripColor(
            meta.getDisplayName()
        );

        events.teleportSpectatorTo(player, targetName);

        player.closeInventory();
    }

    // =====================================================
    // ESPECTADOR - DROP
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onSpectatorDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (events.isMember(player)
                && events.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    // =====================================================
    // ESPECTADOR - PICKUP
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onSpectatorPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (events.isMember(player)
                && events.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    // =====================================================
    // ADVERSARIO DO CONFRONTO
    // =====================================================

    private Player findPairOpponent(Player player) {

        if (player == null) return null;

        return events.getMatchOpponent(player);
    }

    // =====================================================
    // COMANDOS DO EVENTO
    // =====================================================

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onEventCommand(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();

        if (!events.isMember(player)) return;

        if (player.hasPermission("events.blockedcmds")) return;

        String command = event.getMessage()
                .trim()
                .toLowerCase();

        if (!events.isCommandAllowedInEvent(event.getMessage())) {

            event.setCancelled(true);

            player.sendMessage(
                events.getBlockedCommandMessage()
            );

            return;
        }

        // Atualiza o contador quando alguém usa /evento sair.
        if (command.matches(
                "^/?evento\\s+(sair|leave)(\\s.*)?$")) {

            scheduleSumoCheck();
        }
    }
    /**
     * Impede movimentos durante o countdown de Sumo e 1v1.
     * Permite apenas movimentar a câmera.
     */
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onPairMatchCountdownMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (!events.isPairMatchCountdown(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        // Permite somente olhar para os lados e para cima/baixo.
        if (from.getX() == to.getX()
                && from.getY() == to.getY()
                && from.getZ() == to.getZ()) {
            return;
        }

        // Mantém o jogador na posição original.
        event.setTo(new Location(
            from.getWorld(),
            from.getX(),
            from.getY(),
            from.getZ(),
            to.getYaw(),
            to.getPitch()
        ));

        // Remove qualquer impulso ou knockback durante o countdown.
        player.setVelocity(
            new org.bukkit.util.Vector(0, 0, 0)
        );

        player.setFallDistance(0.0F);
    }
    // =====================================================
    // IDENTIFICAR ATACANTE
    // =====================================================

    private Player getAttackingPlayer(Entity damager) {

        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {

            Object shooter = ((Projectile) damager).getShooter();

            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }

        return null;
    }
}
