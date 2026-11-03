package me.rafaelauler.duels;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public class Duel {

    private static MySQLManager mysql;
    private final Map<Block, BlockState> placedBlocks = new HashMap<>();
    private final Map<Block, UUID> placedBlockOwners = new HashMap<>();
    private GladiatorCage gladiatorCage;

    public static void setMySQL(MySQLManager manager) {
        mysql = manager;
    }

    private final Player p1;
    private final Player p2;
    private final Arena arena;
    private final KitType kit;
    private DuelState state;

    public Duel(Player p1, Player p2, Arena arena, KitType kit) {
        this.p1 = p1;
        this.p2 = p2;
        this.arena = arena;
        this.kit = kit;
        this.state = DuelState.STARTING;
    }

    public boolean start() {
        if (arena == null || arena.isInUse()) {
            p1.sendMessage("§cTodas as arenas estão ocupadas. Tente novamente mais tarde...");
            p2.sendMessage("§cTodas as arenas estão ocupadas. Tente novamente mais tarde...");
            return false;
        }

        arena.setInUse(true);

        if (kit == KitType.GLADIATOR) {
            try {
                gladiatorCage = new GladiatorCage(arena);
                gladiatorCage.build();
            } catch (Exception exception) {
                if (gladiatorCage != null) {
                    gladiatorCage.remove();
                    gladiatorCage = null;
                }
                ArenaManager.release(arena);
                String reason = exception.getMessage() == null
                        ? "Não foi possível construir a jaula."
                        : exception.getMessage();
                p1.sendMessage("§cNão foi possível iniciar o Gladiator: §7" + reason);
                p2.sendMessage("§cNão foi possível iniciar o Gladiator: §7" + reason);
                DuelPlugin.getPlugin(DuelPlugin.class).getLogger().warning(
                        "[Duels] Falha ao construir jaula Gladiator na arena " + arena.getName() + ": " + reason
                );
                return false;
            }
        }

        preparePlayer(p1);
        preparePlayer(p2);

        if (gladiatorCage != null) {
            p1.teleport(gladiatorCage.getSpawn1());
            p2.teleport(gladiatorCage.getSpawn2());
        } else {
            arena.teleport(p1, p2);
        }

        kit.apply(p1);
        kit.apply(p2);

        state = DuelState.FIGHTING;

        p1.sendMessage("§aDuelo iniciado contra §f" + p2.getName());
        p2.sendMessage("§aDuelo iniciado contra §f" + p1.getName());

        return true;
    }
    public boolean isInsideArena(Location loc) {

        if (loc == null) return false;
        if (!loc.getWorld().equals(arena.getMin().getWorld())) return false;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= arena.getMin().getX() && x <= arena.getMax().getX()
            && y >= arena.getMin().getY() && y <= arena.getMax().getY()
            && z >= arena.getMin().getZ() && z <= arena.getMax().getZ();
    }
public void registerBlock(Block block, BlockState replacedState, Player placer) {
    if (block == null || replacedState == null || placer == null) {
        return;
    }

    // Guarda o estado original apenas na primeira colocação para restaurar a arena.
    placedBlocks.putIfAbsent(block, replacedState);
    // Atualiza o dono caso outro jogador coloque um bloco nessa mesma posição depois.
    placedBlockOwners.put(block, placer.getUniqueId());
}

/**
 * Registra um bloco alterado por água/lava durante o Gladiator.
 * O estado é capturado antes do fluxo, para que a área volte ao estado original
 * quando o duelo terminar.
 */
public void registerTemporaryBlock(Block block) {
    if (block == null) {
        return;
    }
    placedBlocks.putIfAbsent(block, block.getState());
}

public boolean containsBlock(Block block) {
    return placedBlocks.containsKey(block);
}

    public boolean wasPlacedBy(Block block, Player player) {
        if (block == null || player == null) {
            return false;
        }
        return player.getUniqueId().equals(placedBlockOwners.get(block));
    }

    public void forgetPlacedBlockOwner(Block block) {
        if (block != null) {
            placedBlockOwners.remove(block);
        }
    }

    public void clearPlacedBlocks() {
        for (BlockState previousState : placedBlocks.values()) {
            previousState.update(true, false);
        }
        placedBlocks.clear();
        placedBlockOwners.clear();
    }
public void end(Player winner) {
    end(winner, null);
}

public void end(Player winner, Player skipReset) {

    if (state == DuelState.ENDED) {
        return;
    }

    state = DuelState.ENDED;

    // A jaula e os blocos temporários são removidos imediatamente, em qualquer forma de encerramento.
    clearPlacedBlocks();
    if (gladiatorCage != null) {
        gladiatorCage.remove();
        gladiatorCage = null;
    }

    Player loser = winner != null ? getOpponent(winner) : null;

    DuelPlugin plugin = DuelPlugin.getPlugin(DuelPlugin.class);

    // Atualiza estatísticas somente enquanto o plugin ainda aceita tarefas.
    if (plugin.isEnabled() && mysql != null && winner != null && loser != null) {
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            () -> updateStats(winner, loser)
        );
    }

    // Limpa Boxing
    DuelManager.getBoxinghits().remove(p1.getUniqueId());
    DuelManager.getBoxinghits().remove(p2.getUniqueId());

    // Libera arena
    if (arena != null) {
        ArenaManager.release(arena);

        if (arena.getKitType() == KitType.BUILD) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "ar regenerate build"
            );
        }
    }

    // Reseta somente quem não está aguardando respawn. Durante o disable não agenda tarefas.
    if (plugin.isEnabled()) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (winner != null
                    && winner.isOnline()
                    && (skipReset == null ||
                        !winner.getUniqueId().equals(skipReset.getUniqueId()))) {
                resetPlayer(winner);
            }

            if (loser != null
                    && loser.isOnline()
                    && (skipReset == null ||
                        !loser.getUniqueId().equals(skipReset.getUniqueId()))) {
                resetPlayer(loser);
            }
        });
    }

    DuelManager.remove(p1);
    DuelManager.remove(p2);
}
    private void updateStats(Player winner, Player loser) {

        PlayerStats win = PlayerStatsCache.get(winner.getUniqueId());
        PlayerStats lose = PlayerStatsCache.get(loser.getUniqueId());

        if (win == null) {
            win = mysql.getStats(winner.getUniqueId());
            PlayerStatsCache.put(win);
        }

        if (lose == null) {
            lose = mysql.getStats(loser.getUniqueId());
            PlayerStatsCache.put(lose);
        }

        // 🔥 Atualiza SOMENTE EM MEMÓRIA
        win.addWin();
        lose.addLoss();
        lose.resetWinstreak();

        // 🔥 Marca para persistência

        StatsSaveQueue.enqueue(win);
        StatsSaveQueue.enqueue(lose);
    }

    private void resetPlayers(Player winner, Player loser) {
        if (winner != null && winner.isOnline()) {
            resetPlayer(winner);
        }
        if (loser != null && loser.isOnline()) {
            resetPlayer(loser);
        }
    }

    
    public void handleQuit(Player quitter) {
        if (state == DuelState.ENDED) return;

        Player winner = getOpponent(quitter);
        if (winner == null || !winner.isOnline()) {
            end(null);
            return;
        }
        end(winner);
    }

    /* ========================= */

    private void preparePlayer(Player p) {
        p.closeInventory();
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    private void resetPlayer(Player p) {

        World world = Bukkit.getWorld("spawnd");

        if (world == null || !p.isOnline()) {
            return;
        }

        preparePlayer(p);

        p.teleport(world.getSpawnLocation());

        LobbyItems.give(p);

        p.updateInventory();
    }

    public Player getOpponent(Player p) {
        return p.equals(p1) ? p2 : p1;
    }

    public boolean hasPlayer(Player p) {
        return p.equals(p1) || p.equals(p2);
    }

    public Player[] getPlayers() {
        return new Player[]{p1, p2};
    }

    public Arena getArena() {
        return arena;
    }

    public Player getP1() {
        return p1;
    }

    public Player getP2() {
        return p2;
    }

    public KitType getKit() {
        return kit;
    }

    public DuelState getState() {
        return state;
    }
}
