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
    private boolean spawned = false;
    private LimboPlayer limboPlayer;
    private static final double SPAWN_X = 0.5;
    private static final double SPAWN_Y = 64.0;
    private static final double SPAWN_Z = 0.5;
    
    // Kafa hareketi için son bilinen rotasyon değerleri
    private volatile float lastYaw = 0.0f;
    private volatile float lastPitch = 0.0f;

    public LimboFilter(NoMoreBotsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        plugin.getLogger().info("LimboFilter created for player: " + player.getUsername());
        
        // Try to start verification immediately as a fallback
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (!spawned) {
                    plugin.getLogger().warn("No spawn callback called for " + player.getUsername() + " - using fallback");
                    handleSpawn(null);
                }
            })
            .delay(java.time.Duration.ofSeconds(2))
            .schedule();
    }

    // Try ALL possible callback method names
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
    
    // Try other common callback names
    public void onConnect(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onConnect(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onConnect(LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onConnect(LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onConnect() {
        plugin.getLogger().info("LimboFilter.onConnect() called for player: " + player.getUsername());
        handleSpawn(null);
    }
    
    public void onJoin(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onJoin(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onJoin(LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onJoin(LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onJoin() {
        plugin.getLogger().info("LimboFilter.onJoin() called for player: " + player.getUsername());
        handleSpawn(null);
    }
    
    public void onPlayerConnect(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onPlayerConnect(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onPlayerJoin(LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onPlayerJoin(LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    public void onSessionStart(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.onSessionStart(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    // Try generic handler methods
    public void handle(Limbo server, LimboPlayer limboPlayer) {
        plugin.getLogger().info("LimboFilter.handle(Limbo, LimboPlayer) called for player: " + player.getUsername());
        handleSpawn(limboPlayer);
    }
    
    private void handleSpawn(LimboPlayer limboPlayer) {
        if (spawned) return; // Prevent double execution
        spawned = true;
        
        // Player joined Limbo. Open the GUI.
        plugin.getLogger().info("Player " + player.getUsername() + " spawned in Limbo!");
        
        // Set gamemode to adventure to prevent block breaking/placing
        if (limboPlayer != null) {
            this.limboPlayer = limboPlayer;
            try {
                limboPlayer.setGameMode(GameMode.ADVENTURE);
                // Oyuncuyu spawn pozisyonuna ışınla
                limboPlayer.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0f, 0.0f);
                
                // İlk rotasyon değerlerini kaydet
                lastYaw = 0.0f;
                lastPitch = 0.0f;
                
                // Start position enforcer task
                startPositionEnforcer();
            } catch (Exception e) {
                plugin.getLogger().warn("Could not set gamemode/position for " + player.getUsername() + ": " + e.getMessage());
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
        
        // Pass message to verification session
        var session = plugin.getVerificationManager().getSession(player.getUniqueId());
        if (session != null) {
            session.handleChatMessage(message);
        }
    }
    
    public void onMove(double x, double y, double z, float yaw, float pitch) {
        // Kafa hareketini (yaw/pitch) kaydet - bu serbest kalacak
        lastYaw = yaw;
        lastPitch = pitch;
        
        // Pozisyon değişikliklerini kontrol et - ANLIK TELEPORT için düşük tolerance
        double deltaX = Math.abs(x - SPAWN_X);
        double deltaZ = Math.abs(z - SPAWN_Z);
        double deltaY = Math.abs(y - SPAWN_Y);
        
        // Çok düşük tolerance - herhangi bir hareket girişiminde ANLIK teleport
        // Göz yormaması için hızlı ve keskin müdahale
        if (deltaX > 0.02 || deltaZ > 0.02 || deltaY > 0.05) {
            if (limboPlayer != null) {
                try {
                    // ANLIK teleport - kafa hareketini KORU
                    limboPlayer.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, yaw, pitch);
                } catch (Exception e) {
                    plugin.getLogger().warn("Could not teleport " + player.getUsername() + " back to spawn", e);
                }
            }
        }
        
        // Verification session'a her zaman gerçek rotasyonu gönder
        var session = plugin.getVerificationManager().getSession(player.getUniqueId());
        if (session != null) {
            // Spawn pozisyonunda ama gerçek kafa hareketini gönder
            session.handleMovement(SPAWN_X, SPAWN_Y, SPAWN_Z, yaw, pitch);
        }
    }
    
    private void startPositionEnforcer() {
        // Position enforcer - ANLIK RESPONSE için sık çalışır
        // Backup sistem olarak 200ms'de bir pozisyonu garantiler
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (limboPlayer != null && spawned) {
                    try {
                        // ANLIK pozisyon düzeltme - kafa hareketini koru
                        limboPlayer.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, lastYaw, lastPitch);
                    } catch (Exception e) {
                        // Silently fail, player might have disconnected
                    }
                }
            })
            .repeat(java.time.Duration.ofMillis(200)) // 200ms - ANLIK response için sık kontrol
            .schedule();
    }
    
    // Override toString for debugging
    @Override
    public String toString() {
        return "LimboFilter{player=" + player.getUsername() + ", spawned=" + spawned + "}";
    }
}