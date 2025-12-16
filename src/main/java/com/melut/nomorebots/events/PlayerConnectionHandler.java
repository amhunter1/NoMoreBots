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
        // If the player is just joining the network, this event fires before they connect to any server.
        // We want to intercept the initial connection.
        // But WAIT, if we send them to Limbo, that's a server connection.
        // We need to differentiate between "initial join" and "server switch".
        
        // Actually, if we use LimboAPI, we might just cancel the connection and spawn them in Limbo?
        // Or we redirect them to Limbo.
        
        if (event.getPreviousServer() == null) { // Initial Join
            Player player = event.getPlayer();
            
            // Check bypass permission
            if (player.hasPermission(plugin.getConfigManager().getBypassPermission())) {
                return;
            }

            plugin.getLogger().info("Player " + player.getUsername() + " attempting initial server connect");
            
            // Check cached data (should be available from LoginEvent)
            Optional<PlayerData> optData = plugin.getDatabaseManager().getCachedPlayerData(player.getUniqueId());
            if (!optData.isPresent()) {
                plugin.getLogger().warn("No cached data for " + player.getUsername() + " - allowing connection");
                return; // Allow normal connection if no data
            }
            
            PlayerData data = optData.get();
            plugin.getLogger().info("Player " + player.getUsername() + " verified: " + data.isVerified() + ", bypass: " + data.isBypassGranted());
            
            if (!data.isVerified() && !data.isBypassGranted()) {
                plugin.getLogger().info("Sending " + player.getUsername() + " to Limbo for verification");
                
                // Deny the normal server connection
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                
                // Send to Limbo after a short delay
                plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        plugin.getLogger().info("Executing Limbo spawn for " + player.getUsername());
                        plugin.getLimboManager().sendToLimbo(player);
                    })
                    .delay(java.time.Duration.ofMillis(100))
                    .schedule();
            } else {
                plugin.getLogger().info("Player " + player.getUsername() + " is verified or has bypass - allowing normal connection");
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getVerificationManager().removeSession(uuid);
        plugin.getDatabaseManager().removeCachedPlayerData(uuid);
    }
}