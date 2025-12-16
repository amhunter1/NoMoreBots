package com.melut.nomorebots.verification;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.melut.nomorebots.database.PlayerData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationManager {
    private final NoMoreBotsPlugin plugin;
    private final Map<UUID, VerificationSession> sessions = new ConcurrentHashMap<>();

    public VerificationManager(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
    }

    public void startVerification(Player player) {
        // Create and start hybrid verification session
        VerificationSession session = new VerificationSession(player, plugin);
        sessions.put(player.getUniqueId(), session);
        plugin.getLogger().info("Started hybrid verification session for " + player.getUsername());
    }
    
    public VerificationSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }
    
    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }
    
    public void handleSuccess(Player player) {
        removeSession(player.getUniqueId());
        
        // Update database
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.setVerified(true);
                data.resetFailedAttempts();
                plugin.getDatabaseManager().updatePlayerData(data);
            }
        });

        // Send to lobby
        String targetServerName = plugin.getConfigManager().getTargetServer();
        Optional<RegisteredServer> server = plugin.getServer().getServer(targetServerName);
        if (server.isPresent()) {
            player.createConnectionRequest(server.get()).connect();
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.success"));
        } else {
            player.sendMessage(Component.text("Target server not found! Contact admin."));
        }
    }
    
    public void handleFail(Player player, int attemptsLeft) {
        if (attemptsLeft <= 0) {
            // Timeout
            handleTimeout(player);
        } else {
            // Message for wrong attempt
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("attempts", String.valueOf(attemptsLeft));
            player.sendMessage(Component.text("✗ Wrong! Attempts left: " + attemptsLeft, net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }
    
    public void handleTimeout(Player player) {
        removeSession(player.getUniqueId());
        
        // Update DB
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                // Set timeout for configured duration
                long timeoutMillis = System.currentTimeMillis() + (plugin.getConfigManager().getTimeoutDuration() * 1000L);
                data.setTimeoutUntil(new Timestamp(timeoutMillis));
                data.incrementFailedAttempts();
                plugin.getDatabaseManager().updatePlayerData(data);
            }
        });

        player.disconnect(Component.text("Verification failed! Too many attempts.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }
}