package me.rafaelauler.duels;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLManager {

    private HikariDataSource dataSource;
    // Cache separado das estatísticas de duelo para que os placeholders não consultem
    // o MySQL na thread principal em cada atualização de scoreboard.
    private final ConcurrentHashMap<UUID, EventStats> eventStatsCache = new ConcurrentHashMap<UUID, EventStats>();
    private final Set<UUID> eventStatsLoading = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final ConcurrentHashMap<UUID, AtomicLong> eventStatsRevision = new ConcurrentHashMap<UUID, AtomicLong>();

    public void loadStatsAsync(UUID uuid, Consumer<PlayerStats> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(DuelPlugin.getInstance(), () -> {
            try {
                PlayerStats stats = getStats(uuid);
                PlayerStatsCache.put(stats);

                Bukkit.getScheduler().runTask(DuelPlugin.getInstance(), () -> {
                    callback.accept(stats);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


	public void connect(String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
            "jdbc:mysql://" + host + ":" + port + "/" + database +
            "?useSSL=false&autoReconnect=true"
        );
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
    }


    /** Cria a tabela independente das estatísticas de eventos. */
    public void ensureEventStatsTable() {
        if (dataSource == null || dataSource.isClosed()) return;
        String sql = "CREATE TABLE IF NOT EXISTS kpduels_event_stats (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "kills INT NOT NULL DEFAULT 0," +
                "deaths INT NOT NULL DEFAULT 0," +
                "wins INT NOT NULL DEFAULT 0," +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            DuelPlugin.getInstance().getLogger().severe("Não foi possível criar kpduels_event_stats: " + ex.getMessage());
            return;
        }

        // Migra automaticamente instalações que já tinham a tabela sem a coluna deaths.
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "ALTER TABLE kpduels_event_stats ADD COLUMN deaths INT NOT NULL DEFAULT 0 AFTER kills")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            // MySQL/MariaDB retornam 1060 quando a coluna já existe; nesse caso está tudo certo.
            if (ex.getErrorCode() != 1060) {
                DuelPlugin.getInstance().getLogger().warning("Não foi possível migrar a coluna deaths de kpduels_event_stats: " + ex.getMessage());
            }
        }
    }

    /** Incrementa uma kill de evento sem tocar nas estatísticas normais de duelo. */
    public void addEventKill(final UUID uuid, final String playerName) {
        updateEventCounter(uuid, playerName, "kills");
    }

    /** Incrementa uma morte dentro do evento sem tocar nas estatísticas normais de duelo. */
    public void addEventDeath(final UUID uuid, final String playerName) {
        updateEventCounter(uuid, playerName, "deaths");
    }

    /** Incrementa uma vitória de evento sem tocar nas estatísticas normais de duelo. */
    public void addEventWin(final UUID uuid, final String playerName) {
        updateEventCounter(uuid, playerName, "wins");
    }

    /** Retorna kills persistidas do evento; a primeira leitura carrega os dados em background. */
    public int getEventKills(UUID uuid) {
        EventStats stats = getCachedEventStats(uuid);
        return stats == null ? 0 : stats.kills;
    }

    /** Retorna deaths persistidas do evento; a primeira leitura carrega os dados em background. */
    public int getEventDeaths(UUID uuid) {
        EventStats stats = getCachedEventStats(uuid);
        return stats == null ? 0 : stats.deaths;
    }

    /** Retorna vitórias persistidas do evento. */
    public int getEventWins(UUID uuid) {
        EventStats stats = getCachedEventStats(uuid);
        return stats == null ? 0 : stats.wins;
    }

    /** Pré-carrega as estatísticas ao entrar no servidor, evitando consultas em placeholders. */
    public void loadEventStatsAsync(final UUID uuid) {
        if (uuid == null || dataSource == null || dataSource.isClosed()
                || eventStatsCache.containsKey(uuid) || !eventStatsLoading.add(uuid)) return;

        final long revision = revisionFor(uuid).get();
        Bukkit.getScheduler().runTaskAsynchronously(DuelPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                try (Connection con = dataSource.getConnection()) {
                    EventStats loaded = readEventStats(con, uuid);
                    if (revisionFor(uuid).get() == revision) {
                        eventStatsCache.put(uuid, loaded);
                    }
                } catch (SQLException ex) {
                    DuelPlugin.getInstance().getLogger().warning("Falha ao carregar estatísticas de evento de " + uuid + ": " + ex.getMessage());
                } finally {
                    eventStatsLoading.remove(uuid);
                }
            }
        });
    }

    private EventStats getCachedEventStats(UUID uuid) {
        if (uuid == null) return null;
        EventStats stats = eventStatsCache.get(uuid);
        if (stats == null) loadEventStatsAsync(uuid);
        return stats;
    }

    private void updateEventCounter(final UUID uuid, final String playerName, final String column) {
        if (uuid == null || dataSource == null || dataSource.isClosed()) return;
        // Somente nomes de colunas internos são aceitos; não há entrada do jogador neste SQL.
        if (!("kills".equals(column) || "deaths".equals(column) || "wins".equals(column))) return;
        revisionFor(uuid).incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(DuelPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO kpduels_event_stats (uuid, player_name, " + column + ") " +
                        "VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE " +
                        "player_name=VALUES(player_name), " + column + "=" + column + "+1";
                try (Connection con = dataSource.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, playerName == null ? "Unknown" : playerName);
                    ps.executeUpdate();
                    EventStats refreshed = readEventStats(con, uuid);
                    revisionFor(uuid).incrementAndGet();
                    eventStatsCache.put(uuid, refreshed);
                } catch (SQLException ex) {
                    DuelPlugin.getInstance().getLogger().warning("Falha ao salvar estatística de evento de " + playerName + ": " + ex.getMessage());
                }
            }
        });
    }

    private AtomicLong revisionFor(UUID uuid) {
        AtomicLong current = eventStatsRevision.get(uuid);
        if (current != null) return current;
        AtomicLong created = new AtomicLong();
        AtomicLong previous = eventStatsRevision.putIfAbsent(uuid, created);
        return previous == null ? created : previous;
    }

    private EventStats readEventStats(Connection con, UUID uuid) throws SQLException {
        String sql = "SELECT kills, deaths, wins FROM kpduels_event_stats WHERE uuid=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EventStats(rs.getInt("kills"), rs.getInt("deaths"), rs.getInt("wins"));
                }
            }
        }
        return new EventStats(0, 0, 0);
    }

    private static final class EventStats {
        private final int kills;
        private final int deaths;
        private final int wins;

        private EventStats(int kills, int deaths, int wins) {
            this.kills = kills;
            this.deaths = deaths;
            this.wins = wins;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    public void saveStatsBatch(List<PlayerStats> batch) throws SQLException {

        if (batch.isEmpty()) return;

        String sql =
            "INSERT INTO duel_stats (uuid, wins, losses, winstreak) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "wins = VALUES(wins), " +
            "losses = VALUES(losses), " +
            "winstreak = VALUES(winstreak)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (PlayerStats s : batch) {
                ps.setString(1, s.getUuid().toString());
                ps.setInt(2, s.getWins());
                ps.setInt(3, s.getLosses());
                ps.setInt(4, s.getWinstreak());
                ps.addBatch();
            }

            ps.executeBatch();
        }

        // 🔥 marca todos como salvos
        batch.forEach(PlayerStats::markClean);
    }
    public PlayerStats getStats(UUID uuid) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT wins, losses, winstreak FROM duel_stats WHERE uuid=?")) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new PlayerStats(
                        uuid,
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("winstreak")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new PlayerStats(uuid, 0, 0, 0);
    }

    public void saveStats(PlayerStats stats) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO duel_stats (uuid, wins, losses, winstreak) VALUES (?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE wins=?, losses=?, winstreak=?")) {

            ps.setString(1, stats.getUuid().toString());
            ps.setInt(2, stats.getWins());
            ps.setInt(3, stats.getLosses());
            ps.setInt(4, stats.getWinstreak());

            ps.setInt(5, stats.getWins());
            ps.setInt(6, stats.getLosses());
            ps.setInt(7, stats.getWinstreak());

            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
