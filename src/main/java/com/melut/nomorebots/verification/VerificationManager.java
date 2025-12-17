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
        
        // Get player IP
        String playerIP = player.getRemoteAddress().getAddress().getHostAddress();
        
        // Update database with cooldown
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.setVerified(true);
                data.resetFailedAttempts();
                
                // Set cooldown based on config
                long cooldownMillis = System.currentTimeMillis() + (plugin.getConfigManager().getCooldownDuration() * 1000L);
                data.setVerifiedUntil(new Timestamp(cooldownMillis));
                
                // Update IP tracking
                data.setLastIP(playerIP);
                data.setUsername(player.getUsername()); // Update username in case it changed
                
                plugin.getDatabaseManager().updatePlayerData(data);
                plugin.getLogger().info("Player " + player.getUsername() + " (" + playerIP + ") verified successfully. Cooldown until: " + new Timestamp(cooldownMillis));
            }
        });

        // Disconnect player with success message so they can reconnect to main server
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.reconnect"));
        
        // Schedule disconnect after brief delay
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                player.disconnect(plugin.getLanguageManager().getMessage("verification.final-success"));
            })
            .delay(java.time.Duration.ofSeconds(2))
            .schedule();
    }
    
    public void handleFail(Player player, int attemptsLeft) {
        if (attemptsLeft <= 0) {
            // Timeout
            handleTimeout(player);
        } else {
            // Message for wrong attempt - handled in VerificationSession
            plugin.getLogger().info("Player " + player.getUsername() + " failed attempt, " + attemptsLeft + " attempts remaining");
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

        // Use language manager for timeout message
        Map<String, String> placeholders = new HashMap<>();
        int timeoutMinutes = plugin.getConfigManager().getTimeoutDuration() / 60;
        placeholders.put("time", String.valueOf(timeoutMinutes));
        
        player.disconnect(plugin.getLanguageManager().getMessage("verification.timeout", placeholders));
    }
}