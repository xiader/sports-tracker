package com.spgroup.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.spgroup.dto.EventStatusRequest;
import com.spgroup.service.EventStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for EventStatusController
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventStateManager eventStateManager;

    @BeforeEach
    void setUp() {
        eventStateManager.clearAll();
    }

    @Test
    void testUpdateEventStatusToLive() throws Exception {
        // Given
        EventStatusRequest request = EventStatusRequest.builder()
                .eventId("event123")
                .status(EventStatusRequest.EventStatus.LIVE)
                .build();

        // When & Then
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is("event123")))
                .andExpect(jsonPath("$.status", is("LIVE")))
                .andExpect(jsonPath("$.message", containsString("successfully")));
    }

    @Test
    void testUpdateEventStatusToNotLive() throws Exception {
        // Given
        eventStateManager.markEventAsLive("event123");

        EventStatusRequest request = EventStatusRequest.builder()
                .eventId("event123")
                .status(EventStatusRequest.EventStatus.NOT_LIVE)
                .build();

        // When & Then
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is("event123")))
                .andExpect(jsonPath("$.status", is("NOT_LIVE")));
    }

    @Test
    void testGetEventStatus() throws Exception {
        // Given
        eventStateManager.markEventAsLive("event123");

        // When & Then
        mockMvc.perform(get("/events/event123/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is("event123")))
                .andExpect(jsonPath("$.isLive", is(true)));
    }

    @Test
    void testGetEventStatusNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/events/nonexistent/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.found", is(false)));
    }

    @Test
    void testGetAllLiveEvents() throws Exception {
        // Given
        eventStateManager.markEventAsLive("event1");
        eventStateManager.markEventAsLive("event2");

        // When & Then
        mockMvc.perform(get("/events/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)))
                .andExpect(jsonPath("$.events", hasSize(2)));
    }

    @Test
    void testGetStats() throws Exception {
        // Given
        eventStateManager.markEventAsLive("event1");
        eventStateManager.markEventAsLive("event2");

        // When & Then
        mockMvc.perform(get("/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents", is(2)))
                .andExpect(jsonPath("$.liveEvents", is(2)))
                .andExpect(jsonPath("$.pollingIntervalMs", notNullValue()));
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/events/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("sports-tracker")));
    }

    @Test
    void testInvalidRequest() throws Exception {
        // Given - missing required fields
        String invalidRequest = "{}";

        // When & Then
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
