package com.melut.nomorebots.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
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

    // Verification settings
    public String getGuiTitle() {
        return rootNode.node("verification", "gui", "title").getString("&c&l%target_item% &f&litemine tıkla!");
    }
    
    public int getGuiSize() {
        return rootNode.node("verification", "gui", "size").getInt(54);
    }
    
    public List<String> getRandomItems() {
        try {
             return rootNode.node("verification", "gui", "random-items").getList(String.class);
        } catch (Exception e) {
            return List.of("DIAMOND", "EMERALD");
        }
    }
    
    public List<String> getTargetItems() {
        try {
            return rootNode.node("verification", "gui", "target-items").getList(String.class);
        } catch (Exception e) {
            return List.of("DIAMOND", "EMERALD");
        }
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

    public CommentedConfigurationNode getRoot() {
        return rootNode;
    }
}