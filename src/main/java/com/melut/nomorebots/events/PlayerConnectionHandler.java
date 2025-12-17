package com.melut.nomorebots.events;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.melut.nomorebots.database.PlayerData;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PlayerConnectionHandler {
    private final NoMoreBotsPlugin plugin;

    public PlayerConnectionHandler(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();

        try {
            // Ensure DB entry exists
            plugin.getDatabaseManager().createPlayerData(uuid, username).get();
            Optional<PlayerData> optData = plugin.getDatabaseManager().getPlayerData(uuid).get();
            
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                // Check if timed out
                if (data.isTimedOut()) {
                    long remaining = (data.getTimeoutUntil().getTime() - System.currentTimeMillis()) / 1000 / 60;
                    event.setResult(LoginEvent.ComponentResult.denied(
                            plugin.getLanguageManager().getMessage("verification.timeout", 
                                    java.util.Collections.singletonMap("time", String.valueOf(remaining + 1)))));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().error("Error loading player data for " + username, e);
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (event.getPreviousServer() == null) { // Initial Join
            Player player = event.getPlayer();
            
            // Check bypass permission
            if (player.hasPermission(plugin.getConfigManager().getBypassPermission())) {
                return;
            }

            String playerIP = player.getRemoteAddress().getAddress().getHostAddress();
            String username = player.getUsername();
            
            plugin.getLogger().info("Player " + username + " (" + playerIP + ") attempting initial server connect");
            
            // Check if verification is needed based on cooldown system
            try {
                boolean needsVerification = checkIfVerificationNeeded(player, playerIP, username);
                
                if (needsVerification) {
                    plugin.getLogger().info("Sending " + username + " to Limbo for verification");
                    
                    // Deny the normal server connection
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    
                    // Send to Limbo after a short delay
                    plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> {
                            plugin.getLogger().info("Executing Limbo spawn for " + username);
                            plugin.getLimboManager().sendToLimbo(player);
                        })
                        .delay(java.time.Duration.ofMillis(100))
                        .schedule();
                } else {
                    plugin.getLogger().info("Player " + username + " is in cooldown period - allowing normal connection");
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error checking verification status for " + username, e);
                // On error, allow connection to prevent blocking legitimate players
            }
        }
    }
    
    private boolean checkIfVerificationNeeded(Player player, String playerIP, String username) throws Exception {
        // Check cached data first
        Optional<PlayerData> optData = plugin.getDatabaseManager().getCachedPlayerData(player.getUniqueId());
        if (!optData.isPresent()) {
            // No cached data, load from DB
            optData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).get();
        }
        
        if (optData.isPresent()) {
            PlayerData data = optData.get();
            
            // Check if player has bypass
            if (data.isBypassGranted()) {
                return false;
            }
            
            // Check if player is in cooldown period
            if (data.isInCooldown()) {
                // Additional checks based on config
                boolean trackByUser = plugin.getConfigManager().isTrackByUser();
                boolean trackByIP = plugin.getConfigManager().isTrackByIP();
                
                boolean ipMatches = playerIP.equals(data.getLastIP());
                boolean userMatches = username.equals(data.getUsername());
                
                if (trackByUser && trackByIP) {
                    // Both IP and user must match
                    if (ipMatches && userMatches) {
                        return false; // No verification needed
                    }
                } else if (trackByUser && userMatches) {
                    // Only user tracking enabled and user matches
                    return false;
                } else if (trackByIP && ipMatches) {
                    // Only IP tracking enabled and IP matches
                    return false;
                }
                
                // If we reach here, either:
                // - Same user from different IP (if tracking by user+IP)
                // - Different user from same IP (if tracking by user+IP)
                // - Or tracking settings don't allow bypass
                plugin.getLogger().info("Cooldown bypass denied for " + username + " (" + playerIP + ") - " +
                    "User match: " + userMatches + ", IP match: " + ipMatches +
                    ", Track user: " + trackByUser + ", Track IP: " + trackByIP);
            }
        }
        
        // Default: verification needed
        return true;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getVerificationManager().removeSession(uuid);
        plugin.getDatabaseManager().removeCachedPlayerData(uuid);
    }
}