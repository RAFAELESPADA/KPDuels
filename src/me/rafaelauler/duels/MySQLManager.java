package me.rafaelauler.duels;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLManager {

    private final String host, database, user, password;
    private final int port;
    private Connection connection;

    public MySQLManager(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    // Conecta ao MySQL
    public void connect() throws SQLException, ClassNotFoundException {
        if (isConnected()) return;
        Class.forName("com.mysql.jdbc.Driver"); // compatível 1.8.8
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false",
                user,
                password
        );
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void disconnect() throws SQLException {
        if (isConnected()) connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    // Cria tabela caso não exista
    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS duel_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "wins INT DEFAULT 0," +
                "losses INT DEFAULT 0," +
                "winstreak INT DEFAULT 0" +
                ");";
        getConnection().createStatement().execute(sql);
    }

    // Carrega stats de um jogador
    public PlayerStats getStats(UUID uuid) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM duel_stats WHERE uuid=?"
        );
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new PlayerStats(
                    uuid,
                    rs.getInt("wins"),
                    rs.getInt("losses"),
                    rs.getInt("winstreak")
            );
        } else {
            // Cria registro se não existir
            ps = getConnection().prepareStatement(
                    "INSERT INTO duel_stats(uuid) VALUES(?)"
            );
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            return new PlayerStats(uuid, 0, 0, 0);
        }
    }

    // Salva stats de um jogador
    public void saveStats(PlayerStats stats) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE duel_stats SET wins=?, losses=?, winstreak=? WHERE uuid=?"
        );
        ps.setInt(1, stats.getWins());
        ps.setInt(2, stats.getLosses());
        ps.setInt(3, stats.getWinstreak());
        ps.setString(4, stats.getUuid().toString());
        ps.executeUpdate();
    }
}

