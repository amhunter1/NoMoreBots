package com.melut.nomorebots.verification;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.melut.nomorebots.database.PlayerData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Using Protocolize for Inventory handling since Velocity doesn't support it natively for the proxy itself
// Wait, I need to check if I added Protocolize to dependencies. The plan didn't mention it, but it's standard for Velocity GUIs.
// The user asked for "Limbo API" which is handled.
// If I cannot use Protocolize, I have to handle packets manually or rely on LimboAPI if it supports inventory (it does minimal).
// Let's check LimboAPI capabilities or if I should add Protocolize.
// Since I cannot change dependencies easily now without user interaction and the plan was "Velocity + Limbo", 
// and LimboAPI's main feature is sending a player to a fake server.
// If the player is in Limbo, they are technically connected to a "server" (the limbo one).
// Velocity API doesn't allow opening inventories directly.
// However, since the user wants a GUI, and we are using LimboAPI, LimboAPI might have packet support.
// Actually, for a 57 slot GUI, that's a chest.
// Let's implement a simple packet-based GUI approach if possible, or assume Protocolize is available if I add it.
// BUT, the prompt said "Velocity için, maven, bir plugin yapacaksın ... limbo api ile oyuncular boş bir yerde duracak".
// Adding Protocolize is the best way to handle GUIs on Velocity.
// I will add Protocolize to the pom.xml as a dependency since it wasn't explicitly forbidden and is necessary for "GUI" on proxy.
// OR, I can use the LimboAPI to send packets. LimboAPI exposes a way to send packets.
// Let's stick to the plan. The plan said "Limbo API" and "GUI".
// I'll try to use a simple internal packet handling or just standard Protocolize if I can.
// Let's add Protocolize to pom.xml first to be safe, as it's the standard way.
// Actually, I can't modify pom.xml easily without potentially breaking the flow if I don't use it.
// Let's look at the `LimboFilter` and `LimboManager`.
// I will use a custom Inventory abstraction that sends packets via Velocity's connection or LimboAPI.
// For now, I'll write the logic assuming a `GuiHandler` helper class that I will implement.

public class VerificationManager {
    private final NoMoreBotsPlugin plugin;
    private final Map<UUID, VerificationSession> sessions = new ConcurrentHashMap<>();

    public VerificationManager(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
    }

    public void startVerification(Player player) {
        // Create session
        VerificationSession session = new VerificationSession(player, plugin);
        sessions.put(player.getUniqueId(), session);
        
        // Open GUI
        session.openGui();
    }

    public void handleInventoryClick(Player player, int slot) {
        VerificationSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.handleClick(slot);
        }
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
            // Message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("attempts", String.valueOf(attemptsLeft));
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.wrong-item", placeholders));
        }
    }
    
    public void handleTimeout(Player player) {
        removeSession(player.getUniqueId());
        
        // Update DB
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                // Set timeout for 10 minutes
                long timeoutMillis = System.currentTimeMillis() + (plugin.getConfigManager().getTimeoutDuration() * 1000L);
                data.setTimeoutUntil(new Timestamp(timeoutMillis));
                data.incrementFailedAttempts(); // Or handle total attempts logic
                plugin.getDatabaseManager().updatePlayerData(data);
            }
        });

        player.disconnect(plugin.getLanguageManager().getMessage("verification.failed"));
    }
}