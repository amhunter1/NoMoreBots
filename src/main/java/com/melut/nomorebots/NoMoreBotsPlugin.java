package com.melut.nomorebots;

import com.google.inject.Inject;
import com.melut.nomorebots.config.ConfigManager;
import com.melut.nomorebots.config.LanguageManager;
import com.melut.nomorebots.database.DatabaseManager;
import com.melut.nomorebots.verification.VerificationManager;
import com.melut.nomorebots.limbo.LimboManager;
import com.melut.nomorebots.commands.AdminCommands;
import com.melut.nomorebots.events.PlayerConnectionHandler;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.bstats.velocity.Metrics;

import java.nio.file.Path;

@Plugin(
    id = "nomorebots",
    name = "NoMoreBots",
    version = "1.0.0",
    authors = {"melut"},
    description = "Advanced bot verification plugin for Velocity using NanoLimbo",
    dependencies = {
        @com.velocitypowered.api.plugin.Dependency(id = "limboapi", optional = false)
    }
)
public class NoMoreBotsPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private VerificationManager verificationManager;
    private LimboManager limboManager;
    private Metrics metrics;
    private final Metrics.Factory metricsFactory;

    @Inject
    public NoMoreBotsPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading NoMoreBots...");

        // Config
        this.configManager = new ConfigManager(dataDirectory, logger);
        this.languageManager = new LanguageManager(dataDirectory, configManager.getLanguage(), logger);

        // Database
        this.databaseManager = new DatabaseManager(configManager, logger, dataDirectory);

        // Managers - Instantiating even if classes don't exist yet, we will create them next
        // Note: VerificationManager might need LimboManager or vice versa, so order matters if they have dependencies.
        // Assuming loose coupling for now or setters.
        this.limboManager = new LimboManager(this);
        this.verificationManager = new VerificationManager(this);

        // Events
        server.getEventManager().register(this, new PlayerConnectionHandler(this));

        // Commands
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("nomorebots").aliases("nmb").build(),
                new AdminCommands(this)
        );

        // Initialize bStats metrics
        try {
            this.metrics = metricsFactory.make(this, 28400);
            logger.info("bStats metrics initialized for NoMoreBots (ID: 28400)");
        } catch (Exception e) {
            logger.warn("Could not initialize bStats metrics: " + e.getMessage());
        }

        logger.info("NoMoreBots loaded successfully!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    // Getters
    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public VerificationManager getVerificationManager() { return verificationManager; }
    public LimboManager getLimboManager() { return limboManager; }
}