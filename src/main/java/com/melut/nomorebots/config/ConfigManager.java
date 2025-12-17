package com.melut.nomorebots.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigManager {
    private final Path dataDirectory;
    private final Logger logger;
    private CommentedConfigurationNode rootNode;
    private final String fileName = "config.yml";

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        loadConfig();
    }

    public void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configPath = dataDirectory.resolve(fileName);
            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getResourceAsStream("/" + fileName)) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            rootNode = loader.load();
        } catch (IOException e) {
            logger.error("Could not load config.yml", e);
        }
    }

    public String getLanguage() {
        return rootNode.node("general", "language").getString("tr");
    }
    
    public boolean isDebug() {
        return rootNode.node("general", "debug").getBoolean(false);
    }
    
    public String getDatabaseType() {
        return rootNode.node("database", "type").getString("sqlite");
    }
    
    public String getSQLiteFile() {
        return rootNode.node("database", "sqlite", "file").getString("nomorebots.db");
    }

    // Limbo settings
    public String getLimboHost() {
        return rootNode.node("limbo", "host").getString("127.0.0.1");
    }

    public int getLimboPort() {
        return rootNode.node("limbo", "port").getInt(25566);
    }
    
    public String getLimboBrand() {
        return rootNode.node("limbo", "brand-name").getString("&6NoMoreBots &7Verification");
    }

    // Verification settings - Chat + Movement Hybrid
    public int getCodeLength() {
        return rootNode.node("verification", "code", "length").getInt(3);
    }
    
    public String getCodeCharacters() {
        return rootNode.node("verification", "code", "characters").getString("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
    }
    
    public boolean isCodeCaseSensitive() {
        return rootNode.node("verification", "code", "case-sensitive").getBoolean(false);
    }
    
    // New multi-direction movement settings
    public List<String> getMovementDirections() {
        try {
            return rootNode.node("verification", "movement", "directions").getList(String.class,
                java.util.Arrays.asList("up:2", "left:2"));
        } catch (SerializationException e) {
            logger.warn("Could not deserialize movement directions, using default values", e);
            return java.util.Arrays.asList("up:2", "left:2");
        }
    }
    
    public double getMovementTolerance() {
        return rootNode.node("verification", "movement", "tolerance").getDouble(15.0);
    }
    
    public int getResponseTimeout() {
        return rootNode.node("verification", "movement", "response-timeout").getInt(20);
    }
    
    public boolean isKickOnTimeout() {
        return rootNode.node("verification", "movement", "kick-on-timeout").getBoolean(true);
    }
    
    // Direction angle settings
    public double getDirectionAngle(String direction, String type) {
        return rootNode.node("verification", "movement", "angles", direction, type).getDouble(0.0);
    }
    
    // Legacy methods for backward compatibility
    public float getRequiredPitch() {
        return (float) getDirectionAngle("up", "pitch-max");
    }
    
    public double getMovementHoldDuration() {
        // Extract from first direction if exists
        List<String> directions = getMovementDirections();
        if (!directions.isEmpty()) {
            String firstDir = directions.get(0);
            if (firstDir.contains(":")) {
                return Double.parseDouble(firstDir.split(":")[1]);
            }
        }
        return 2.0;
    }

    public int getMaxAttempts() {
        return rootNode.node("verification", "attempts", "max-attempts").getInt(3);
    }
    
    public int getTimeoutDuration() {
        return rootNode.node("verification", "timeout", "duration").getInt(600);
    }
    
    public String getTargetServer() {
        return rootNode.node("verification", "success", "target-server").getString("lobby");
    }
    
    public String getBypassPermission() {
        return rootNode.node("bypass", "permission").getString("nomorebots.bypass");
    }
    
    // Cooldown system settings
    public boolean isTrackByUser() {
        return rootNode.node("verification", "cooldown", "track-by-user").getBoolean(true);
    }
    
    public boolean isTrackByIP() {
        return rootNode.node("verification", "cooldown", "track-by-ip").getBoolean(true);
    }
    
    public int getCooldownDuration() {
        return rootNode.node("verification", "cooldown", "duration").getInt(86400);
    }

    public CommentedConfigurationNode getRoot() {
        return rootNode;
    }
}