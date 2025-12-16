package com.melut.nomorebots.database;

import java.sql.Timestamp;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String username;
    private boolean verified;
    private int totalAttempts;
    private int failedAttempts;
    private Timestamp timeoutUntil;
    private boolean bypassGranted;

    public PlayerData(UUID uuid, String username, boolean verified, int totalAttempts, int failedAttempts, Timestamp timeoutUntil, boolean bypassGranted) {
        this.uuid = uuid;
        this.username = username;
        this.verified = verified;
        this.totalAttempts = totalAttempts;
        this.failedAttempts = failedAttempts;
        this.timeoutUntil = timeoutUntil;
        this.bypassGranted = bypassGranted;
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
}