package com.spgroup.service;

import com.spgroup.model.ExternalApiResponse;
import com.spgroup.model.LiveEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Main service for tracking live sports events
 * Manages dynamic scheduling of polling tasks for each live event
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTrackerService {

    private final EventStateManager eventStateManager;
    private final ExternalApiService externalApiService;
    private final KafkaProducerService kafkaProducerService;
    private final TaskScheduler taskScheduler;

    @Value("${app.polling.interval:10000}")
    private long pollingIntervalMs;

    /**
     * Starts polling for a live event
     * Creates a scheduled task that runs every 10 seconds
     *
     * @param eventId Event ID to start polling
     */
    public void startPollingForEvent(String eventId) {
        LiveEvent event = eventStateManager.getEvent(eventId);

        if (event == null) {
            log.warn("Cannot start polling for non-existent event [{}]", eventId);
            return;
        }

        if (!event.isLive()) {
            log.warn("Cannot start polling for non-live event [{}]", eventId);
            return;
        }

        // Cancel existing task if any
        if (event.getScheduledTask() != null && !event.getScheduledTask().isCancelled()) {
            event.getScheduledTask().cancel(false);
            log.info("Cancelled existing polling task for event [{}]", eventId);
        }

        // Create new scheduled task
        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                () -> pollEventData(eventId),
                Instant.now().plusMillis(1000), // Start after 1 second
                Duration.ofMillis(pollingIntervalMs)
        );

        event.setScheduledTask(scheduledTask);
        log.info("Started polling for event [{}] with interval {}ms", eventId, pollingIntervalMs);
    }

    /**
     * Stops polling for an event
     *
     * @param eventId Event ID to stop polling
     */
    public void stopPollingForEvent(String eventId) {
        LiveEvent event = eventStateManager.getEvent(eventId);

        if (event == null) {
            log.warn("Cannot stop polling for non-existent event [{}]", eventId);
            return;
        }

        if (event.getScheduledTask() != null && !event.getScheduledTask().isCancelled()) {
            event.getScheduledTask().cancel(false);
            event.setScheduledTask(null);
            log.info("Stopped polling for event [{}]", eventId);
        }
    }

    /**
     * Polls data for a specific event
     * Called by scheduled task every 10 seconds
     *
     * @param eventId Event ID to poll
     */
    private void pollEventData(String eventId) {
        try {
            log.debug("Polling data for event [{}]", eventId);

            // Check if event is still live
            if (!eventStateManager.isEventLive(eventId)) {
                log.info("Event [{}] is no longer live, stopping polling", eventId);
                stopPollingForEvent(eventId);
                return;
            }

            // Fetch data from external API
            ExternalApiResponse apiResponse = externalApiService.fetchEventData(eventId);

            if (apiResponse == null) {
                log.warn("Received null response for event [{}]", eventId);
                return;
            }

            // Update last polled timestamp
            eventStateManager.updateLastPolled(eventId);

            // Transform and publish to Kafka
            Map<String, Object> kafkaMessage = transformToKafkaMessage(apiResponse);
            boolean success = kafkaProducerService.sendMessage(eventId, kafkaMessage);

            if (success) {
                log.info("Successfully processed and published data for event [{}]", eventId);
            } else {
                log.error("Failed to publish data for event [{}]", eventId);
            }

        } catch (Exception e) {
            log.error("Error polling data for event [{}]", eventId, e);
        }
    }

    /**
     * Transforms external API response into Kafka message format
     *
     * @param apiResponse External API response
     * @return Kafka message as Map
     */
    private Map<String, Object> transformToKafkaMessage(ExternalApiResponse apiResponse) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", apiResponse.getEventId());
        message.put("currentScore", apiResponse.getCurrentScore());
        message.put("timestamp", Instant.now().toString());
        message.put("source", "sports-tracker");

        log.debug("Transformed message: {}", message);
        return message;
    }

    /**
     * Gets statistics about polling
     *
     * @return Statistics map
     */
    public Map<String, Object> getPollingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", eventStateManager.getTotalEventsCount());
        stats.put("liveEvents", eventStateManager.getLiveEventsCount());
        stats.put("pollingIntervalMs", pollingIntervalMs);
        stats.put("timestamp", Instant.now().toString());

        return stats;
    }

    /**
     * Cleanup on shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SportsTrackerService, stopping all polling tasks...");

        eventStateManager.getAllLiveEvents().forEach(event -> {
            if (event.getScheduledTask() != null && !event.getScheduledTask().isCancelled()) {
                event.getScheduledTask().cancel(false);
                log.info("Cancelled polling task for event [{}]", event.getEventId());
            }
        });

        log.info("All polling tasks stopped");
    }
}
