package com.spgroup.service;


import com.spgroup.model.LiveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for EventStateManager
 */
class EventStateManagerTest {

    private EventStateManager eventStateManager;

    @BeforeEach
    void setUp() {
        eventStateManager = new EventStateManager();
        eventStateManager.clearAll();
    }

    @Test
    void testMarkEventAsLive() {
        // Given
        String eventId = "event123";

        // When
        LiveEvent event = eventStateManager.markEventAsLive(eventId);

        // Then
        assertNotNull(event);
        assertEquals(eventId, event.getEventId());
        assertTrue(event.isLive());
        assertNotNull(event.getLastUpdated());
    }

    @Test
    void testMarkEventAsNotLive() {
        // Given
        String eventId = "event123";
        eventStateManager.markEventAsLive(eventId);

        // When
        LiveEvent event = eventStateManager.markEventAsNotLive(eventId);

        // Then
        assertNotNull(event);
        assertFalse(event.isLive());
    }

    @Test
    void testMarkNonExistentEventAsNotLive() {
        // When
        LiveEvent event = eventStateManager.markEventAsNotLive("nonexistent");

        // Then
        assertNull(event);
    }

    @Test
    void testGetAllLiveEvents() {
        // Given
        eventStateManager.markEventAsLive("event1");
        eventStateManager.markEventAsLive("event2");
        eventStateManager.markEventAsLive("event3");
        eventStateManager.markEventAsNotLive("event2");

        // When
        List<LiveEvent> liveEvents = eventStateManager.getAllLiveEvents();

        // Then
        assertEquals(2, liveEvents.size());
        assertTrue(liveEvents.stream().allMatch(LiveEvent::isLive));
    }

    @Test
    void testIsEventLive() {
        // Given
        String eventId = "event123";
        eventStateManager.markEventAsLive(eventId);

        // When & Then
        assertTrue(eventStateManager.isEventLive(eventId));
        assertFalse(eventStateManager.isEventLive("nonexistent"));
    }

    @Test
    void testGetLiveEventsCount() {
        // Given
        eventStateManager.markEventAsLive("event1");
        eventStateManager.markEventAsLive("event2");
        eventStateManager.markEventAsLive("event3");
        eventStateManager.markEventAsNotLive("event1");

        // When
        int count = eventStateManager.getLiveEventsCount();

        // Then
        assertEquals(2, count);
    }

    @Test
    void testRemoveEvent() {
        // Given
        String eventId = "event123";
        eventStateManager.markEventAsLive(eventId);

        // When
        eventStateManager.removeEvent(eventId);

        // Then
        assertNull(eventStateManager.getEvent(eventId));
    }

    @Test
    void testClearAll() {
        // Given
        eventStateManager.markEventAsLive("event1");
        eventStateManager.markEventAsLive("event2");

        // When
        eventStateManager.clearAll();

        // Then
        assertEquals(0, eventStateManager.getTotalEventsCount());
    }
}
