package com.melut.nomorebots.limbo;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.player.GameMode;

public class LimboFilter implements LimboSessionHandler {
    private final NoMoreBotsPlugin plugin;
    private final Player player;

    public LimboFilter(NoMoreBotsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        plugin.getLogger().info("LimboFilter created for player: " + player.getUsername());
    }

    // Try different method signatures to find the correct one
    public void onSpawn(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onSpawn(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onSpawn(LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onSpawn(LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onSpawn() {
        plugin.getLogger().info("LimboFilter.onSpawn() called for player: " + player.getUsername());
        handleSpawn(null);
    }
    
    private void handleSpawn(LimboPlayer limboPlayer) {
        // Player joined Limbo. Open the GUI.
        plugin.getLogger().info("Player " + player.getUsername() + " spawned in Limbo!");
        
        // Set gamemode to survival for better experience
        if (limboPlayer != null) {
            try {
                limboPlayer.setGameMode(GameMode.SURVIVAL);
            } catch (Exception e) {
                plugin.getLogger().warn("Could not set gamemode for " + player.getUsername() + ": " + e.getMessage());
            }
        }
        
        // Send welcome message first
        try {
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome"));
        } catch (Exception e) {
            plugin.getLogger().error("Error sending welcome message to " + player.getUsername(), e);
        }
        
        // Start verification after a short delay to ensure connection is stable
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                plugin.getLogger().info("Starting verification for " + player.getUsername());
                plugin.getVerificationManager().startVerification(player);
            })
            .delay(java.time.Duration.ofSeconds(1))
            .schedule();
    }
    
    public void onDisconnect() {
        plugin.getLogger().info("Player " + player.getUsername() + " disconnected from Limbo");
        // Clean up any verification sessions
        plugin.getVerificationManager().removeSession(player.getUniqueId());
    }
    
    // Try to implement more possible LimboSessionHandler methods
    public void onChat(String message) {
        plugin.getLogger().info("Player " + player.getUsername() + " sent chat in Limbo: " + message);
    }
    
    public void onMove(double x, double y, double z, float yaw, float pitch) {
        // Player moved in Limbo - no action needed for verification
    }
}