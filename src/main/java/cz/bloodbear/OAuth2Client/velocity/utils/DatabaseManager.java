package cz.bloodbear.OAuth2Client.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.OAuth2Client.core.records.OAuth2Account;
import cz.bloodbear.OAuth2Client.core.utils.DB;
import cz.bloodbear.OAuth2Client.velocity.OAuth2Client;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DatabaseManager implements DB {
    private Connection connection;

    public DatabaseManager(String host, int port, String database, String username, String password, boolean useSSL) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            connection = DriverManager.getConnection(url, username, password);
            createTable();
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS linked_accounts (" +
                "minecraft_uuid VARCHAR(36) PRIMARY KEY, " +
                "oauth2_id VARCHAR(36) NOT NULL, " +
                "oauth2_username VARCHAR(255) NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }

        String sql2 = "CREATE TABLE IF NOT EXISTS link_requests ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "minecraft_uuid VARCHAR(36) NOT NULL, "
                + "code VARCHAR(64) NOT NULL UNIQUE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql2);
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
    }

    public void linkAccount(String minecraftUUID, String OAuth2AccountId, String OAuth2AccountUsername) {
        String sql = "INSERT INTO linked_accounts (oauth2_id, oauth2_username, uuid) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE oauth2_id = ?, oauth2_username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, OAuth2AccountId);
            stmt.setString(2, OAuth2AccountUsername);
            stmt.setString(3, minecraftUUID);
            stmt.setString(4, OAuth2AccountId);
            stmt.setString(5, OAuth2AccountUsername);
            stmt.executeUpdate();
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }

        String sql2 = "DELETE FROM link_requests WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql2)) {
            stmt.setString(1, minecraftUUID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }

        Optional<Player> player = OAuth2Client.getInstance().getServer().getPlayer(UUID.fromString(minecraftUUID));
    }

    public OAuth2Account getOAuth2Account(String uuid) {
        String sql = "SELECT oauth2_id, oauth2_username FROM linked_accounts WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new OAuth2Account(rs.getString("oauth2_id"), rs.getString("oauth2_username"));
            }
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        return null;
    }

    public boolean isOAuth2AccountLinked(String OAuth2AccountId) {
        String sql = "SELECT uuid FROM linked_accounts WHERE oauth2_id = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, OAuth2AccountId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public void unlinkAccount(String uuid) {

        String sql = "DELETE FROM linked_accounts WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
    }

    public boolean isLinked(String uuid) {
        String query = "SELECT COUNT(*) FROM linked_accounts WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        return false;
    }

    public void saveLinkRequest(String uuid, String code) {
        String query = "INSERT INTO link_requests (uuid, code) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE code = VALUES(code)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
    }

    public String getPlayerByCode(String code) {
        String query = "SELECT uuid FROM link_requests WHERE code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("uuid");
            }
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        return null;
    }

    public void deleteLinkCodes(String uuid) {
        String sql = "DELETE FROM link_requests WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteLinkCodes() {
        String sql = "DELETE FROM link_requests";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getAllLinkedAccounts() {
        Map<String, String> linkedAccounts = new HashMap<>();

        String query = "SELECT uuid, oauth2_id FROM linked_accounts";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                linkedAccounts.put(rs.getString("uuid"), rs.getString("oauth2_id"));
            }
        } catch (Exception e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }

        return linkedAccounts;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
    }
}
