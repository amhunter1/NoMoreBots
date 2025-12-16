package com.melut.nomorebots.limbo;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.LimboSessionHandler;

public class LimboManager {
    private final NoMoreBotsPlugin plugin;
    private final LimboFactory limboFactory;

    public LimboManager(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
        this.limboFactory = (LimboFactory) plugin.getServer().getPluginManager()
                .getPlugin("limboapi").flatMap(container -> container.getInstance())
                .orElseThrow(() -> new RuntimeException("LimboAPI not found!"));
    }

    public void sendToLimbo(Player player) {
        Limbo limbo = limboFactory.createLimbo(player.getVirtualHost().orElse(null));
        limbo.spawnPlayer(player, new LimboFilter(plugin, player));
    }
}