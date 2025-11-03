package com.spgroup.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a live event with its scheduled task
 * Used for in-memory state management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveEvent {

    private String eventId;
    private boolean isLive;
    private Instant lastUpdated;
    private Instant lastPolled;
    private String externalApiUrl;

    // Reference to the scheduled task
    private transient ScheduledFuture<?> scheduledTask;

    public void markAsLive() {
        this.isLive = true;
        this.lastUpdated = Instant.now();
    }

    public void markAsNotLive() {
        this.isLive = false;
        this.lastUpdated = Instant.now();

        // Cancel scheduled task if exists
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
    }

    public void updateLastPolled() {
        this.lastPolled = Instant.now();
    }
}
