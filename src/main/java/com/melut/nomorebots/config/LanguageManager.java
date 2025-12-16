package com.melut.nomorebots.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final Path dataDirectory;
    private final String language;
    private final Logger logger;
    private CommentedConfigurationNode rootNode;
    private final Map<String, String> messageCache = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LanguageManager(Path dataDirectory, String language, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.language = language;
        this.logger = logger;
        loadLanguages();
    }

    private void loadLanguages() {
        Path langDir = dataDirectory.resolve("lang");
        try {
            if (!Files.exists(langDir)) {
                Files.createDirectories(langDir);
            }

            // Save default languages
            saveResource(langDir, "tr.yml");
            saveResource(langDir, "en.yml");

            Path langFile = langDir.resolve(language + ".yml");
            if (!Files.exists(langFile)) {
                logger.warn("Language file {}.yml not found, falling back to en.yml", language);
                langFile = langDir.resolve("en.yml");
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(langFile)
                    .build();
            rootNode = loader.load();
            messageCache.clear();
        } catch (IOException e) {
            logger.error("Could not load language file", e);
        }
    }

    private void saveResource(Path langDir, String resourceName) {
        Path target = langDir.resolve(resourceName);
        if (!Files.exists(target)) {
            try (InputStream in = getClass().getResourceAsStream("/lang/" + resourceName)) {
                if (in != null) {
                    Files.copy(in, target);
                }
            } catch (IOException e) {
                logger.error("Could not save default language file " + resourceName, e);
            }
        }
    }

    public String getRawMessage(String path) {
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }

        String message = getMessageFromNode(rootNode, path.split("\\."));
        if (message == null) {
            return "Missing message: " + path;
        }

        messageCache.put(path, message);
        return message;
    }

    private String getMessageFromNode(CommentedConfigurationNode node, String[] path) {
        CommentedConfigurationNode current = node;
        for (String key : path) {
            current = current.node(key);
        }
        return current.getString();
    }

    public Component getMessage(String path) {
        String raw = getRawMessage(path);
        // Replace legacy colors with MiniMessage if needed, or just parse
        // For simplicity assuming config uses & codes which MiniMessage doesn't support by default directly in standard mode usually,
        // but often plugins convert & to § or use a serializer.
        // Let's support standard legacy ampersands for compatibility by converting them first
        raw = raw.replace("&", "<legacy_ampersand>"); // Hacky, better to use LegacyComponentSerializer if needed
        // Actually, Velocity uses Adventure. Let's use LegacyComponentSerializer to deserialize if configs use '&'
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }
    
    public Component getMessage(String path, Map<String, String> placeholders) {
        String raw = getRawMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }
}