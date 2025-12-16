package com.melut.nomorebots.limbo;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.chunk.Dimension;

public class LimboManager {
    private final NoMoreBotsPlugin plugin;
    private final LimboFactory limboFactory;
    private final VirtualWorld limboWorld;

    public LimboManager(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
        this.limboFactory = (LimboFactory) plugin.getServer().getPluginManager()
                .getPlugin("limboapi").flatMap(container -> container.getInstance())
                .orElseThrow(() -> new RuntimeException("LimboAPI not found!"));
        
        // Create a simple virtual world for verification
        this.limboWorld = limboFactory.createVirtualWorld(
            Dimension.OVERWORLD, // Using overworld dimension
            0, 64, 0, // Spawn coordinates (x, y, z)
            0, 0 // Yaw, Pitch
        );
    }

    public void sendToLimbo(Player player) {
        Limbo limbo = limboFactory.createLimbo(limboWorld);
        limbo.spawnPlayer(player, new LimboFilter(plugin, player));
    }
}