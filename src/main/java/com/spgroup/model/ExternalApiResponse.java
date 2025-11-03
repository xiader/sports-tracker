package com.spgroup.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiResponse {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("currentScore")
    private String currentScore;
}
