package com.melut.nomorebots.limbo;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

public class LimboFilter implements LimboSessionHandler {
    private final NoMoreBotsPlugin plugin;
    private final Player player;

    public LimboFilter(NoMoreBotsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void onSpawn(LimboPlayer limboPlayer) {
        // Player joined Limbo. Open the GUI.
        plugin.getLogger().info("Player " + player.getUsername() + " spawned in Limbo!");
        
        // Send welcome message first
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome"));
        
        // Start verification after a short delay to ensure connection is stable
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                plugin.getLogger().info("Starting verification for " + player.getUsername());
                plugin.getVerificationManager().startVerification(player);
            })
            .delay(java.time.Duration.ofSeconds(1))
            .schedule();
    }
    
    // We can handle other events here if needed, or rely on Velocity events/packets.
    // Since LimboAPI handles the world/physics, we focus on the GUI.
}