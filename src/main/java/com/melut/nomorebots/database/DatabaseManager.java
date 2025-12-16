package com.melut.nomorebots.database;

import com.melut.nomorebots.config.ConfigManager;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final ConfigManager configManager;
    private final Logger logger;
    private final Path dataDirectory;
    private Connection connection;
    private final ExecutorService executor;
    private final java.util.Map<UUID, PlayerData> playerDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    public DatabaseManager(ConfigManager configManager, Logger logger, Path dataDirectory) {
        this.configManager = configManager;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.executor = Executors.newFixedThreadPool(2); // Simple thread pool for async ops
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = dataDirectory.resolve(configManager.getSQLiteFile()).toFile();
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            try (Statement stmt = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS player_verification (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "username VARCHAR(16) NOT NULL, " +
                        "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "last_verification TIMESTAMP, " +
                        "verification_status INTEGER DEFAULT 0, " +
                        "total_attempts INTEGER DEFAULT 0, " +
                        "failed_attempts INTEGER DEFAULT 0, " +
                        "success_count INTEGER DEFAULT 0, " +
                        "timeout_until TIMESTAMP NULL, " +
                        "remember_until TIMESTAMP NULL, " +
                        "bypass_granted BOOLEAN DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");";
                stmt.execute(sql);
                
                // Index
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_verification(uuid);");
            }
            
            logger.info("Database initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
        }
    }

    public Optional<PlayerData> getCachedPlayerData(UUID uuid) {
        return Optional.ofNullable(playerDataCache.get(uuid));
    }

    public void cachePlayerData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
    }

    public void removeCachedPlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(Optional.of(playerDataCache.get(uuid)));
        }
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_verification WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    PlayerData data = new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("username"),
                            rs.getInt("verification_status") == 1,
                            rs.getInt("total_attempts"),
                            rs.getInt("failed_attempts"),
                            rs.getTimestamp("timeout_until"),
                            rs.getBoolean("bypass_granted")
                    );
                    playerDataCache.put(uuid, data);
                    return Optional.of(data);
                }
            } catch (SQLException e) {
                logger.error("Error fetching player data for " + uuid, e);
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<Void> createPlayerData(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR IGNORE INTO player_verification (uuid, username) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error creating player data for " + uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> updatePlayerData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_verification SET " +
                    "username = ?, " +
                    "verification_status = ?, " +
                    "total_attempts = ?, " +
                    "failed_attempts = ?, " +
                    "timeout_until = ?, " +
                    "bypass_granted = ?, " +
                    "updated_at = CURRENT_TIMESTAMP " +
                    "WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, data.getUsername());
                pstmt.setInt(2, data.isVerified() ? 1 : 0);
                pstmt.setInt(3, data.getTotalAttempts());
                pstmt.setInt(4, data.getFailedAttempts());
                pstmt.setTimestamp(5, data.getTimeoutUntil());
                pstmt.setBoolean(6, data.isBypassGranted());
                pstmt.setString(7, data.getUuid().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error updating player data for " + data.getUuid(), e);
            }
        }, executor);
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            if (executor != null) {
                executor.shutdown();
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
}