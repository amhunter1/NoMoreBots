package com.melut.nomorebots.verification;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class VerificationSession {
    private final Player player;
    private final NoMoreBotsPlugin plugin;
    private String targetCode;
    private int attempts = 0;
    private int maxAttempts;
    private final Random random = new Random();
    
    // Verification stages
    private VerificationStage currentStage = VerificationStage.CHAT;
    private boolean chatCompleted = false;
    private boolean movementCompleted = false;
    
    public enum VerificationStage {
        CHAT,      // Player needs to type the code in chat
        MOVEMENT,  // Player needs to look up
        COMPLETED  // Both stages done
    }

    public VerificationSession(Player player, NoMoreBotsPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        
        // Generate random code for chat verification
        generateTargetCode();
        
        // Start hybrid verification
        startChatVerification();
    }

    private void generateTargetCode() {
        // Generate random code using config settings
        String characters = plugin.getConfigManager().getCodeCharacters();
        int length = plugin.getConfigManager().getCodeLength();
        
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        this.targetCode = code.toString();
        plugin.getLogger().info("Generated verification code for " + player.getUsername() + ": " + targetCode);
    }
    
    private void startChatVerification() {
        currentStage = VerificationStage.CHAT;
        
        // Send instructions
        player.sendMessage(Component.text("=== BOT VERIFICATION ===", NamedTextColor.RED));
        player.sendMessage(Component.text("Step 1/2: Chat Verification", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Type in chat: " + targetCode, NamedTextColor.GREEN));
        player.sendMessage(Component.text("(Copy and paste the code above)", NamedTextColor.GRAY));
        
        plugin.getLogger().info("Started chat verification for " + player.getUsername() + " with code: " + targetCode);
    }
    
    private void startMovementVerification() {
        currentStage = VerificationStage.MOVEMENT;
        
        // Send instructions for movement
        player.sendMessage(Component.text("Step 2/2: Movement Verification", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Look UP (towards the sky)", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Hold your mouse upward for 2 seconds", NamedTextColor.GRAY));
        
        plugin.getLogger().info("Started movement verification for " + player.getUsername());
    }
    
    public void handleChatMessage(String message) {
        if (currentStage != VerificationStage.CHAT) return;
        
        plugin.getLogger().info("Chat verification attempt by " + player.getUsername() + ": " + message);
        
        String userInput = message.trim();
        String expectedCode = targetCode;
        
        // Handle case sensitivity based on config
        if (!plugin.getConfigManager().isCodeCaseSensitive()) {
            userInput = userInput.toUpperCase();
            expectedCode = expectedCode.toUpperCase();
        }
        
        if (userInput.equals(expectedCode)) {
            // Chat verification successful
            chatCompleted = true;
            player.sendMessage(Component.text("✓ Chat verification successful!", NamedTextColor.GREEN));
            
            // Move to movement verification
            startMovementVerification();
        } else {
            // Wrong code
            attempts++;
            int remaining = maxAttempts - attempts;
            
            if (remaining > 0) {
                player.sendMessage(Component.text("✗ Wrong code! Try again: " + targetCode, NamedTextColor.RED));
                player.sendMessage(Component.text("Attempts remaining: " + remaining, NamedTextColor.YELLOW));
            } else {
                // Failed verification
                plugin.getVerificationManager().handleFail(player, 0);
            }
        }
    }
    
    public void handleMovement(double x, double y, double z, float yaw, float pitch) {
        if (currentStage != VerificationStage.MOVEMENT) return;
        
        // Check if player is looking up based on config settings
        float requiredPitch = plugin.getConfigManager().getRequiredPitch();
        float tolerance = (float) plugin.getConfigManager().getMovementTolerance();
        
        // Pitch ranges from -90 (straight up) to 90 (straight down)
        if (pitch < requiredPitch + tolerance) { // Looking up enough
            if (!movementCompleted) {
                movementCompleted = true;
                player.sendMessage(Component.text("✓ Movement verification successful!", NamedTextColor.GREEN));
                
                // Complete verification
                currentStage = VerificationStage.COMPLETED;
                
                // Schedule success after a brief delay
                plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        player.sendMessage(Component.text("=== VERIFICATION COMPLETE ===", NamedTextColor.GREEN));
                        player.sendMessage(Component.text("Welcome to the server!", NamedTextColor.AQUA));
                        plugin.getVerificationManager().handleSuccess(player);
                    })
                    .delay(java.time.Duration.ofMillis(500))
                    .schedule();
            }
        }
    }
    
    // Getters
    public boolean isChatCompleted() { return chatCompleted; }
    public boolean isMovementCompleted() { return movementCompleted; }
    public VerificationStage getCurrentStage() { return currentStage; }
    public String getTargetCode() { return targetCode; }
}