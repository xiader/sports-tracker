package com.spgroup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating event status via REST API
 * Used to mark events as "live" or "not live"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusRequest {

    @NotBlank(message = "Event ID is required")
    @JsonProperty("eventId")
    private String eventId;

    @NotNull(message = "Status is required")
    @JsonProperty("status")
    private EventStatus status;

    /**
     * Event status enum
     */
    public enum EventStatus {
        @JsonProperty("live")
        LIVE,

        @JsonProperty("not_live")
        NOT_LIVE;

        public boolean isLive() {
            return this == LIVE;
        }
    }
}