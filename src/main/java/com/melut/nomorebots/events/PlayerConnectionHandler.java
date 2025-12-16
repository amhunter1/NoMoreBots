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

            // Check if verified
            plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
                if (optData.isPresent()) {
                    PlayerData data = optData.get();
                    if (!data.isVerified() && !data.isBypassGranted()) {
                        // Not verified, send to Limbo
                        // Cancelling the event and using LimboManager to spawn player
                        // NOTE: ServerPreConnectEvent expectation is that we set the target server.
                        // But Limbo isn't a RegisteredServer usually.
                        // LimboAPI has its own way.
                        // If we use spawnPlayer, it handles the connection.
                        
                        // BUT, we cannot block the thread here easily if it's async-ish, 
                        // though PreConnect is synchronous usually.
                        // However, we are in a callback.
                        // Wait, 'thenAccept' is async. We can't use it like this for PreConnectEvent effectively if we want to block NOW.
                        // We should have loaded data in LoginEvent (which we did and cached).
                    }
                }
            });
            
            // Using cached data
            Optional<PlayerData> optData = plugin.getDatabaseManager().getCachedPlayerData(player.getUniqueId());
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                if (!data.isVerified() && !data.isBypassGranted()) {
                    // Send to limbo
                    // We set the result to denied? No, that kicks them.
                    // We can't redirect to "Limbo" as a RegisteredServer if it's virtual.
                    // LimboAPI documentation says: "Spawn player in Limbo"
                    
                    // If we spawn them in Limbo, we should probably cancel the event?
                    // Or let them connect to a dummy server?
                    // Ideally, we redirect the connection request to the virtual host handled by Limbo.
                    
                    // Actually, typical pattern:
                    // event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    // plugin.getLimboManager().sendToLimbo(player);
                    
                    // BUT if we deny, they might get disconnected.
                    // Let's see how LimboAPI works. usually `limbo.spawnPlayer(player)` takes over the connection.
                    // So we should probably let the event proceed to "nowhere" or cancel it if Limbo takes over?
                    // If we cancel, they disconnect.
                    
                    // Strategy:
                    // Set result to allowed but redirect logic?
                    // LimboAPI usually requires the player to be connected.
                    // But here they are PRE-connecting.
                    
                    // Correct approach for Velocity with LimboAPI often involves:
                    // On ServerPreConnect, if we want Limbo, we spawn them and cancel the event or something?
                    // Actually, `spawnPlayer` effectively connects them.
                    
                    // Let's try:
                    event.setResult(ServerPreConnectEvent.ServerResult.denied()); 
                    // This prevents connecting to the default server (e.g. lobby).
                    // Then we spawn them in Limbo immediately.
                    
                    plugin.getLimboManager().sendToLimbo(player);
                }
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