package com.spgroup.service;


import com.spgroup.model.LiveEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages in-memory state of live events
 * Thread-safe implementation using ConcurrentHashMap
 */
@Slf4j
@Service
public class EventStateManager {

    private final Map<String, LiveEvent> liveEvents = new ConcurrentHashMap<>();

    /**
     * Marks event as live
     *
     * @param eventId Event ID
     * @return Updated LiveEvent
     */
    public LiveEvent markEventAsLive(String eventId) {
        LiveEvent event = liveEvents.computeIfAbsent(eventId, id ->
                LiveEvent.builder()
                        .eventId(id)
                        .lastUpdated(Instant.now())
                        .build()
        );

        event.markAsLive();
        log.info("Event [{}] marked as LIVE", eventId);
        return event;
    }

    /**
     * Marks event as not live and cancels scheduled task
     *
     * @param eventId Event ID
     * @return Updated LiveEvent or null if not found
     */
    public LiveEvent markEventAsNotLive(String eventId) {
        LiveEvent event = liveEvents.get(eventId);

        if (event != null) {
            event.markAsNotLive();
            log.info("Event [{}] marked as NOT LIVE", eventId);
            return event;
        }

        log.warn("Attempted to mark non-existent event [{}] as NOT LIVE", eventId);
        return null;
    }

    /**
     * Gets event by ID
     *
     * @param eventId Event ID
     * @return LiveEvent or null if not found
     */
    public LiveEvent getEvent(String eventId) {
        return liveEvents.get(eventId);
    }

    /**
     * Gets all live events
     *
     * @return List of live events
     */
    public List<LiveEvent> getAllLiveEvents() {
        return liveEvents.values().stream()
                .filter(LiveEvent::isLive)
                .collect(Collectors.toList());
    }

    /**
     * Checks if event exists and is live
     *
     * @param eventId Event ID
     * @return true if event is live, false otherwise
     */
    public boolean isEventLive(String eventId) {
        LiveEvent event = liveEvents.get(eventId);
        return event != null && event.isLive();
    }

    /**
     * Updates the last polled timestamp for an event
     *
     * @param eventId Event ID
     */
    public void updateLastPolled(String eventId) {
        LiveEvent event = liveEvents.get(eventId);
        if (event != null) {
            event.updateLastPolled();
        }
    }

    /**
     * Removes event from storage
     *
     * @param eventId Event ID
     */
    public void removeEvent(String eventId) {
        LiveEvent removed = liveEvents.remove(eventId);
        if (removed != null) {
            removed.markAsNotLive(); // Cancel scheduled task
            log.info("Event [{}] removed from state", eventId);
        }
    }

    /**
     * Gets total count of events
     *
     * @return Total number of events
     */
    public int getTotalEventsCount() {
        return liveEvents.size();
    }

    /**
     * Gets count of live events
     *
     * @return Number of live events
     */
    public int getLiveEventsCount() {
        return (int) liveEvents.values().stream()
                .filter(LiveEvent::isLive)
                .count();
    }

    /**
     * Clears all events (useful for testing)
     */
    public void clearAll() {
        liveEvents.values().forEach(LiveEvent::markAsNotLive);
        liveEvents.clear();
        log.info("All events cleared from state");
    }
}
