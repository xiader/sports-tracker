package com.spgroup.externalapi;


import com.spgroup.model.ExternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/mock/events")
public class MockApiController {

    private final Random random = new Random();

    /**
     * Mock endpoint that returns event data
     * GET /mock/events/data?eventId=123
     */
    @GetMapping("/data")
    public ResponseEntity<ExternalApiResponse> getMockEventData(
            @RequestParam("eventId") String eventId) {

        log.info("Mock API called for event [{}]", eventId);

        int homeScore = random.nextInt(5);
        int awayScore = random.nextInt(5);
        String score = homeScore + ":" + awayScore;

        ExternalApiResponse response = ExternalApiResponse.builder()
                .eventId(eventId)
                .currentScore(score)
                .build();

        log.debug("Returning mock data for event [{}]: score={}", eventId, score);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Mock API is running");
    }
}