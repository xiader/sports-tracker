package com.spgroup.controller;

import com.spgroup.dto.EventStatusRequest;
import com.spgroup.dto.EventStatusResponse;
import com.spgroup.model.LiveEvent;
import com.spgroup.service.EventStateManager;
import com.spgroup.service.SportsTrackerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing event status
 * Endpoint: POST /events/status
 */
@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventStatusController {

    private final EventStateManager eventStateManager;
    private final SportsTrackerService sportsTrackerService;

    /**
     * Updates event status (live / not live)
     * When event is marked as "live", starts polling task
     * When event is marked as "not live", stops polling task
     * <p>
     * POST /events/status
     * Body: { "eventId": "1234", "status": "live" }
     *
     * @param request Event status request
     * @return Event status response
     */
    @PostMapping("/status")
    public ResponseEntity<EventStatusResponse> updateEventStatus(
            @Valid @RequestBody EventStatusRequest request) {

        try {
            String eventId = request.getEventId();
            boolean isLive = request.getStatus().isLive();

            log.info("Received status update for event [{}]: status={}", eventId, request.getStatus());

            if (isLive) {
                // Mark event as live and start polling
                LiveEvent event = eventStateManager.markEventAsLive(eventId);
                sportsTrackerService.startPollingForEvent(eventId);

                EventStatusResponse response = EventStatusResponse.success(
                        eventId,
                        "LIVE"
                );

                return ResponseEntity.ok(response);

            } else {
                // Mark event as not live and stop polling
                sportsTrackerService.stopPollingForEvent(eventId);
                LiveEvent event = eventStateManager.markEventAsNotLive(eventId);

                if (event == null) {
                    log.warn("Event [{}] not found when marking as NOT_LIVE", eventId);
                }

                EventStatusResponse response = EventStatusResponse.success(
                        eventId,
                        "NOT_LIVE"
                );

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error updating event status", e);

            EventStatusResponse errorResponse = EventStatusResponse.error(
                    request.getEventId(),
                    "Internal server error: " + e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Gets status of a specific event
     * GET /events/{eventId}/status
     *
     * @param eventId Event ID
     * @return Event status information
     */
    @GetMapping("/{eventId}/status")
    public ResponseEntity<Map<String, Object>> getEventStatus(@PathVariable("eventId") String eventId) {
        LiveEvent event = eventStateManager.getEvent(eventId);

        if (event == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("eventId", eventId);
            notFound.put("found", false);
            notFound.put("message", "Event not found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("eventId", event.getEventId());
        status.put("isLive", event.isLive());
        status.put("lastUpdated", event.getLastUpdated());
        status.put("lastPolled", event.getLastPolled());
        status.put("hasScheduledTask", event.getScheduledTask() != null && !event.getScheduledTask().isCancelled());

        return ResponseEntity.ok(status);
    }

    /**
     * Gets all live events
     * GET /events/live
     *
     * @return List of live events
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> getAllLiveEvents() {
        List<LiveEvent> liveEvents = eventStateManager.getAllLiveEvents();

        Map<String, Object> response = new HashMap<>();
        response.put("count", liveEvents.size());
        response.put("events", liveEvents.stream()
                .map(event -> {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("eventId", event.getEventId());
                    eventMap.put("lastUpdated", event.getLastUpdated());
                    eventMap.put("lastPolled", event.getLastPolled());
                    return eventMap;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Gets polling statistics
     * GET /events/stats
     *
     * @return Polling statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(sportsTrackerService.getPollingStats());
    }

    /**
     * Health check endpoint
     * GET /events/health
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "sports-tracker");
        health.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(health);
    }
}
