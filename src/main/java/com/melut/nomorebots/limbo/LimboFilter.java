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

    @Override
    public void onSpawn(LimboPlayer limboPlayer) {
        // Player joined Limbo. Open the GUI.
        plugin.getVerificationManager().startVerification(player);
    }
    
    // We can handle other events here if needed, or rely on Velocity events/packets.
    // Since LimboAPI handles the world/physics, we focus on the GUI.
}