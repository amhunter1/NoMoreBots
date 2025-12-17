package com.melut.nomorebots.verification;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.List;

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
    
    // Multi-direction movement verification
    private List<String> movementDirections;
    private int currentDirectionIndex = 0;
    private long currentDirectionStartTime = 0;
    private String currentDirection = "";
    private int currentDirectionDuration = 0;
    
    // Timeout handling
    private long lastActionTime = System.currentTimeMillis();
    private boolean timeoutHandled = false;
    
    public enum VerificationStage {
        CHAT,      // Player needs to type the code in chat
        MOVEMENT,  // Player needs to look up
        COMPLETED  // Both stages done
    }

    public VerificationSession(Player player, NoMoreBotsPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        
        // Load movement directions from config
        this.movementDirections = plugin.getConfigManager().getMovementDirections();
        
        // Generate random code for chat verification
        generateTargetCode();
        
        // Start hybrid verification
        startChatVerification();
        
        // Start timeout checker
        startTimeoutChecker();
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
        
        // Send instructions using language manager
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome"));
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-stage"));
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("code", targetCode);
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-instruction", placeholders));
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-hint"));
        
        plugin.getLogger().info("Started chat verification for " + player.getUsername() + " with code: " + targetCode);
    }
    
    private void startMovementVerification() {
        currentStage = VerificationStage.MOVEMENT;
        
        // Send instructions for movement using language manager
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.movement-stage"));
        
        // Start first direction
        startNextDirection();
        
        plugin.getLogger().info("Started multi-direction movement verification for " + player.getUsername());
    }
    
    private void startNextDirection() {
        if (currentDirectionIndex >= movementDirections.size()) {
            // All directions completed
            completeMovementVerification();
            return;
        }
        
        String directionData = movementDirections.get(currentDirectionIndex);
        String[] parts = directionData.split(":");
        currentDirection = parts[0];
        currentDirectionDuration = Integer.parseInt(parts[1]);
        currentDirectionStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();
        
        // Send direction-specific message
        String messageKey = "verification.movement-" + currentDirection;
        player.sendMessage(plugin.getLanguageManager().getMessage(messageKey));
        
        plugin.getLogger().info("Direction " + (currentDirectionIndex + 1) + "/" + movementDirections.size() +
                              ": " + currentDirection + " for " + currentDirectionDuration + "s");
    }
    
    private void completeMovementVerification() {
        movementCompleted = true;
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.movement-success"));
        
        // Complete verification
        currentStage = VerificationStage.COMPLETED;
        
        // Schedule success after a brief delay
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.complete"));
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome-server"));
                plugin.getVerificationManager().handleSuccess(player);
            })
            .delay(java.time.Duration.ofMillis(500))
            .schedule();
    }
    
    private void startTimeoutChecker() {
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (currentStage != VerificationStage.COMPLETED && !timeoutHandled) {
                    long now = System.currentTimeMillis();
                    int timeoutSeconds = plugin.getConfigManager().getResponseTimeout();
                    
                    if (now - lastActionTime > timeoutSeconds * 1000L) {
                        if (plugin.getConfigManager().isKickOnTimeout()) {
                            timeoutHandled = true; // Prevent multiple timeout handling
                            plugin.getLogger().info("Player " + player.getUsername() + " timed out during verification");
                            plugin.getVerificationManager().handleTimeout(player);
                        }
                    }
                }
            })
            .repeat(java.time.Duration.ofSeconds(1))
            .schedule();
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
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-success"));
            
            // Move to movement verification
            startMovementVerification();
        } else {
            // Wrong code
            attempts++;
            int remaining = maxAttempts - attempts;
            
            if (remaining > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("code", targetCode);
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-wrong", placeholders));
                
                Map<String, String> attemptsPlaceholders = new HashMap<>();
                attemptsPlaceholders.put("attempts", String.valueOf(remaining));
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-attempts", attemptsPlaceholders));
            } else {
                // Failed verification
                plugin.getVerificationManager().handleFail(player, 0);
            }
        }
    }
    
    public void handleMovement(double x, double y, double z, float yaw, float pitch) {
        if (currentStage != VerificationStage.MOVEMENT) return;
        if (currentDirectionIndex >= movementDirections.size()) return;
        
        lastActionTime = System.currentTimeMillis();
        
        boolean isLookingCorrectDirection = false;
        double tolerance = plugin.getConfigManager().getMovementTolerance();
        
        switch (currentDirection.toLowerCase()) {
            case "up":
                double upPitchMin = plugin.getConfigManager().getDirectionAngle("up", "pitch-min");
                double upPitchMax = plugin.getConfigManager().getDirectionAngle("up", "pitch-max");
                isLookingCorrectDirection = pitch >= upPitchMin && pitch <= upPitchMax;
                break;
                
            case "down":
                double downPitchMin = plugin.getConfigManager().getDirectionAngle("down", "pitch-min");
                double downPitchMax = plugin.getConfigManager().getDirectionAngle("down", "pitch-max");
                isLookingCorrectDirection = pitch >= downPitchMin && pitch <= downPitchMax;
                break;
                
            case "left":
                double leftYawMin = plugin.getConfigManager().getDirectionAngle("left", "yaw-min");
                double leftYawMax = plugin.getConfigManager().getDirectionAngle("left", "yaw-max");
                // Normalize yaw to 0-360
                float normalizedYaw = ((yaw % 360) + 360) % 360;
                isLookingCorrectDirection = normalizedYaw >= leftYawMin && normalizedYaw <= leftYawMax;
                break;
                
            case "right":
                double rightYawMin = plugin.getConfigManager().getDirectionAngle("right", "yaw-min");
                double rightYawMax = plugin.getConfigManager().getDirectionAngle("right", "yaw-max");
                // Handle negative ranges for right
                isLookingCorrectDirection = (yaw >= rightYawMin && yaw <= rightYawMax);
                break;
        }
        
        if (isLookingCorrectDirection) {
            long holdTime = System.currentTimeMillis() - currentDirectionStartTime;
            long requiredHoldTime = currentDirectionDuration * 1000L;
            
            if (holdTime >= requiredHoldTime) {
                // Direction completed, move to next
                currentDirectionIndex++;
                startNextDirection();
            }
        } else {
            // Reset timer if not looking in the correct direction
            currentDirectionStartTime = System.currentTimeMillis();
        }
    }
    
    // Getters
    public boolean isChatCompleted() { return chatCompleted; }
    public boolean isMovementCompleted() { return movementCompleted; }
    public VerificationStage getCurrentStage() { return currentStage; }
    public String getTargetCode() { return targetCode; }
}