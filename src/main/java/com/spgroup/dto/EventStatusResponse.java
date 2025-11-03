package com.spgroup.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for event status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusResponse {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;

    public static EventStatusResponse success(String eventId, String status) {
        return EventStatusResponse.builder()
                .eventId(eventId)
                .status(status)
                .message("Event status updated successfully")
                .timestamp(Instant.now().toString())
                .build();
    }

    public static EventStatusResponse error(String eventId, String errorMessage) {
        return EventStatusResponse.builder()
                .eventId(eventId)
                .status("ERROR")
                .message(errorMessage)
                .timestamp(Instant.now().toString())
                .build();
    }
}
