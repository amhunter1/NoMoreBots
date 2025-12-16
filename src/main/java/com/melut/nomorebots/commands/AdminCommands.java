package com.melut.nomorebots.commands;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.melut.nomorebots.database.PlayerData;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AdminCommands implements SimpleCommand {
    private final NoMoreBotsPlugin plugin;

    public AdminCommands(NoMoreBotsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("nomorebots.admin")) {
            source.sendMessage(plugin.getLanguageManager().getMessage("errors.no-permission"));
            return;
        }

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                plugin.getConfigManager().loadConfig();
                // We should also reload language manager usually, but let's assume config manager handles file reload
                // Actually LanguageManager needs to be re-instantiated or have a reload method.
                // For simplicity, we just reload main config message.
                source.sendMessage(plugin.getLanguageManager().getMessage("admin.reload-success"));
                break;
            case "verify":
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /nmb verify <player>"));
                    return;
                }
                handleVerify(source, args[1]);
                break;
            case "reset":
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /nmb reset <player>"));
                    return;
                }
                handleReset(source, args[1]);
                break;
            case "timeout":
                if (args.length < 3) {
                    source.sendMessage(Component.text("Usage: /nmb timeout <player> <seconds>"));
                    return;
                }
                handleTimeout(source, args[1], args[2]);
                break;
            case "bypass":
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /nmb bypass <player>"));
                    return;
                }
                handleBypass(source, args[1]);
                break;
            case "stats": // TODO: Implement stats
                source.sendMessage(Component.text("Stats not implemented yet."));
                break;
            default:
                source.sendMessage(plugin.getLanguageManager().getMessage("errors.unknown-command"));
                break;
        }
    }

    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("§6=== NoMoreBots Help ==="));
        source.sendMessage(Component.text("§e/nmb reload §7- Reload config"));
        source.sendMessage(Component.text("§e/nmb verify <player> §7- Manually verify player"));
        source.sendMessage(Component.text("§e/nmb reset <player> §7- Reset player data"));
        source.sendMessage(Component.text("§e/nmb timeout <player> <seconds> §7- Timeout player"));
        source.sendMessage(Component.text("§e/nmb bypass <player> §7- Toggle bypass"));
    }

    private void handleVerify(CommandSource source, String playerName) {
        Optional<Player> target = plugin.getServer().getPlayer(playerName);
        UUID uuid;
        if (target.isPresent()) {
            uuid = target.get().getUniqueId();
        } else {
            // Need to look up UUID? For now support online only or requires UUID lookup logic which is complex
            // Let's assume online or we fail.
            // Or we fetch from DB by username? DB stores username.
            // Let's try to fetch by UUID if we had it, but we don't.
            source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-not-found"));
            return;
        }

        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.setVerified(true);
                data.resetFailedAttempts();
                plugin.getDatabaseManager().updatePlayerData(data);
                
                Map<String, String> placeholders = Collections.singletonMap("player", playerName);
                source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-verified", placeholders));
            }
        });
    }

    private void handleReset(CommandSource source, String playerName) {
        Optional<Player> target = plugin.getServer().getPlayer(playerName);
        if (target.isEmpty()) {
            source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-not-found"));
            return;
        }
        
        UUID uuid = target.get().getUniqueId();
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.setVerified(false);
                data.resetFailedAttempts();
                data.setTimeoutUntil(null);
                plugin.getDatabaseManager().updatePlayerData(data);
                
                Map<String, String> placeholders = Collections.singletonMap("player", playerName);
                source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-reset", placeholders));
            }
        });
    }

    private void handleTimeout(CommandSource source, String playerName, String secondsStr) {
        Optional<Player> target = plugin.getServer().getPlayer(playerName);
        if (target.isEmpty()) {
            source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-not-found"));
            return;
        }

        try {
            int seconds = Integer.parseInt(secondsStr);
            UUID uuid = target.get().getUniqueId();
            plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
                if (optData.isPresent()) {
                    PlayerData data = optData.get();
                    data.setTimeoutUntil(new Timestamp(System.currentTimeMillis() + (seconds * 1000L)));
                    plugin.getDatabaseManager().updatePlayerData(data);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", playerName);
                    placeholders.put("duration", String.valueOf(seconds));
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-timeout", placeholders));
                }
            });
        } catch (NumberFormatException e) {
            source.sendMessage(Component.text("Invalid number of seconds!"));
        }
    }

    private void handleBypass(CommandSource source, String playerName) {
        Optional<Player> target = plugin.getServer().getPlayer(playerName);
        if (target.isEmpty()) {
            source.sendMessage(plugin.getLanguageManager().getMessage("admin.player-not-found"));
            return;
        }

        UUID uuid = target.get().getUniqueId();
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                boolean newState = !data.isBypassGranted();
                data.setBypassGranted(newState);
                plugin.getDatabaseManager().updatePlayerData(data);
                
                Map<String, String> placeholders = Collections.singletonMap("player", playerName);
                if (newState) {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.bypass-added", placeholders));
                } else {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.bypass-removed", placeholders));
                }
            }
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return Arrays.asList("reload", "verify", "reset", "timeout", "bypass", "stats");
        }
        return Collections.emptyList();
    }
}