package me.rafaelauler.duels;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLManager {

    private HikariDataSource dataSource;

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
