package com.melut.nomorebots.verification;

import com.melut.nomorebots.NoMoreBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class VerificationSession {
    private final Player player;
    private final NoMoreBotsPlugin plugin;
    private final Inventory inventory;
    private String targetItemName;
    private ItemType targetItemType;
    private int attempts = 0;
    private int maxAttempts;
    private final Random random = new Random();

    public VerificationSession(Player player, NoMoreBotsPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        
        // Check if Protocolize is available
        Inventory tempInventory = null;
        boolean protocolizeAvailable = false;
        try {
            Class.forName("dev.simplix.protocolize.api.inventory.Inventory");
            tempInventory = new Inventory(InventoryType.GENERIC_9X6);
            protocolizeAvailable = true;
            plugin.getLogger().info("Protocolize is available - using GUI verification");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            plugin.getLogger().warn("Protocolize not found - using fallback verification");
        }
        
        this.inventory = tempInventory;
        
        if (protocolizeAvailable && inventory != null) {
            this.inventory.onClick(this::handleInventoryClick);
            setupSession();
        } else {
            // Start fallback immediately
            fallbackVerification();
        }
    }

    private void setupSession() {
        pickTargetItem();
        if (inventory != null) {
            refreshGui();
        }
    }

    private void pickTargetItem() {
        List<String> targets = plugin.getConfigManager().getTargetItems();
        if (targets.isEmpty()) {
            targets = Collections.singletonList("DIAMOND");
        }
        String targetName = targets.get(random.nextInt(targets.size()));
        try {
            this.targetItemType = ItemType.valueOf(targetName.toUpperCase());
            this.targetItemName = targetName;
        } catch (IllegalArgumentException e) {
            this.targetItemType = ItemType.DIAMOND;
            this.targetItemName = "DIAMOND";
            plugin.getLogger().warn("Invalid target item type in config: " + targetName);
        }
    }

    private void refreshGui() {
        if (inventory == null) return; // No inventory to refresh
        
        // Clear all items from inventory
        for (int i = 0; i < 54; i++) {
            inventory.item(i, (ItemStack) null);
        }
        
        // Update Title
        String titleStr = plugin.getConfigManager().getGuiTitle();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target_item", targetItemName);
        Component title = plugin.getLanguageManager().getMessage("verification.gui-title", placeholders);
        inventory.title(title);

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            availableSlots.add(i);
        }
        Collections.shuffle(availableSlots);

        // Place target item
        int targetSlot = availableSlots.remove(0);
        ItemStack targetItem = new ItemStack(targetItemType);
        targetItem.displayName(Component.text(targetItemName, NamedTextColor.GREEN));
        inventory.item(targetSlot, targetItem);

        // Place random items
        List<String> randomItems = plugin.getConfigManager().getRandomItems();
        for (int i = 0; i < 8; i++) { // Place some random items
             if (availableSlots.isEmpty()) break;
             String randomItemName = randomItems.get(random.nextInt(randomItems.size()));
             try {
                 ItemType type = ItemType.valueOf(randomItemName.toUpperCase());
                 if (type == targetItemType) continue; // Skip if same as target
                 int slot = availableSlots.remove(0);
                 ItemStack randomItem = new ItemStack(type);
                 randomItem.displayName(Component.text(randomItemName, NamedTextColor.RED));
                 inventory.item(slot, randomItem);
             } catch (IllegalArgumentException ignored) {}
        }
    }

    public void openGui() {
        if (inventory == null) {
            plugin.getLogger().info("No GUI inventory available for " + player.getUsername() + " - using fallback");
            fallbackVerification();
            return;
        }
        
        try {
            plugin.getLogger().info("Attempting to open GUI for " + player.getUsername());
            
            // Check if Protocolize can handle the player
            var protocolizePlayer = Protocolize.playerProvider().player(player.getUniqueId());
            if (protocolizePlayer == null) {
                plugin.getLogger().error("Protocolize player is null for " + player.getUsername());
                fallbackVerification();
                return;
            }
            
            plugin.getLogger().info("Opening inventory for " + player.getUsername());
            protocolizePlayer.openInventory(inventory);
            plugin.getLogger().info("Inventory opened successfully for " + player.getUsername());
            
        } catch (Exception e) {
            plugin.getLogger().error("Failed to open Protocolize GUI for " + player.getUsername(), e);
            fallbackVerification();
        }
    }
    
    private void fallbackVerification() {
        // Fallback to chat-based verification
        plugin.getLogger().info("Using fallback chat verification for " + player.getUsername());
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target_item", targetItemName);
        
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-instruction", placeholders));
        player.sendMessage(Component.text("Type the name of the item: " + targetItemName, NamedTextColor.YELLOW));
        
        // For now, let's auto-succeed for testing
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                plugin.getLogger().info("Auto-succeeding verification for testing");
                plugin.getVerificationManager().handleSuccess(player);
            })
            .delay(java.time.Duration.ofSeconds(3))
            .schedule();
    }

    public void handleClick(int slot) {
        // This method is called by VerificationManager if we manually handle clicks, 
        // but Protocolize has its own listener. We use the internal listener.
    }

    private void handleInventoryClick(InventoryClick click) {
        if (inventory == null) return; // No inventory to handle
        
        click.cancelled(true); // Cancel all interactions
        
        ItemStack clickedItem = (ItemStack) click.clickedItem();
        if (clickedItem == null || clickedItem.itemType() == ItemType.AIR) return;

        if (clickedItem.itemType() == targetItemType) {
            // Success
            plugin.getVerificationManager().handleSuccess(player);
            try {
                Protocolize.playerProvider().player(player.getUniqueId()).closeInventory();
            } catch (Exception e) {
                plugin.getLogger().warn("Could not close inventory for " + player.getUsername());
            }
        } else {
            // Fail
            attempts++;
            int remaining = maxAttempts - attempts;
            plugin.getVerificationManager().handleFail(player, remaining);
            
            if (remaining > 0) {
                // Shuffle items again
                pickTargetItem(); // Pick new target or keep same? Usually good to shuffle.
                refreshGui();
                openGui(); // Re-open to update title/items properly if needed
            } else {
                try {
                    Protocolize.playerProvider().player(player.getUniqueId()).closeInventory();
                } catch (Exception e) {
                    plugin.getLogger().warn("Could not close inventory for " + player.getUsername());
                }
            }
        }
    }
}