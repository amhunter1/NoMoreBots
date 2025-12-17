package com.melut.nomorebots.database;

import java.sql.Timestamp;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String username;
    private boolean verified;
    private int totalAttempts;
    private int failedAttempts;
    private Timestamp timeoutUntil;
    private boolean bypassGranted;
    
    // IP-based tracking
    private String lastIP;
    private Timestamp verifiedUntil; // When cooldown expires

    public PlayerData(UUID uuid, String username, boolean verified, int totalAttempts, int failedAttempts,
                     Timestamp timeoutUntil, boolean bypassGranted, String lastIP, Timestamp verifiedUntil) {
        this.uuid = uuid;
        this.username = username;
        this.verified = verified;
        this.totalAttempts = totalAttempts;
        this.failedAttempts = failedAttempts;
        this.timeoutUntil = timeoutUntil;
        this.bypassGranted = bypassGranted;
        this.lastIP = lastIP;
        this.verifiedUntil = verifiedUntil;
    }
    
    // Backward compatibility constructor
    public PlayerData(UUID uuid, String username, boolean verified, int totalAttempts, int failedAttempts, Timestamp timeoutUntil, boolean bypassGranted) {
        this(uuid, username, verified, totalAttempts, failedAttempts, timeoutUntil, bypassGranted, null, null);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void incrementTotalAttempts() {
        this.totalAttempts++;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
    
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }
    
    public void incrementSessionAttempts() {
        this.totalAttempts++;
    }
    
    public int getSessionAttempts() {
        return totalAttempts;
    }
    
    public void resetTotalAttempts() {
        this.totalAttempts = 0;
    }

    public Timestamp getTimeoutUntil() {
        return timeoutUntil;
    }

    public void setTimeoutUntil(Timestamp timeoutUntil) {
        this.timeoutUntil = timeoutUntil;
    }

    public boolean isBypassGranted() {
        return bypassGranted;
    }

    public void setBypassGranted(boolean bypassGranted) {
        this.bypassGranted = bypassGranted;
    }
    
    public boolean isTimedOut() {
        return timeoutUntil != null && timeoutUntil.after(new Timestamp(System.currentTimeMillis()));
    }
    
    // IP-based methods
    public String getLastIP() {
        return lastIP;
    }
    
    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }
    
    public Timestamp getVerifiedUntil() {
        return verifiedUntil;
    }
    
    public void setVerifiedUntil(Timestamp verifiedUntil) {
        this.verifiedUntil = verifiedUntil;
    }
    
    public boolean isInCooldown() {
        return verifiedUntil != null && verifiedUntil.after(new Timestamp(System.currentTimeMillis()));
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}