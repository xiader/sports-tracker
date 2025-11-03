package com.spgroup.service;


import com.spgroup.externalapi.ExternalApiClientConfiguration;
import com.spgroup.model.ExternalApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Service for calling external REST API
 * Implements retry logic for transient failures
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiClientConfiguration config;

    /**
     * Fetches event data from external API with retry logic
     * Expected response format: { "eventId": "1234", "currentScore": "0:0" }
     *
     * @param eventId Event ID to fetch
     * @return ExternalApiResponse or null if failed
     */
    @Retryable(
            retryFor = {WebClientResponseException.class, Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExternalApiResponse fetchEventData(String eventId) {
        try {
            log.debug("Fetching data for event [{}] from: {}", eventId, config.getUrl());

            ExternalApiResponse response = webClientBuilder.build()
                    .get()
                    .uri(config.getUrl(), uriBuilder -> uriBuilder
                            .queryParam("eventId", eventId)
                            .build())
                    .retrieve()
                    .bodyToMono(ExternalApiResponse.class)
                    .timeout(config.getTimeout())
                    .retryWhen(Retry.backoff(config.getRetryAttempts(), Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying API call for event [{}], attempt: {}",
                                            eventId, retrySignal.totalRetries() + 1)))
                    .onErrorResume(error -> {
                        log.error("Failed to fetch data for event [{}]: {}",
                                eventId, error.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null) {
                log.info("Successfully fetched data for event [{}]: score={}",
                        eventId, response.getCurrentScore());
            } else {
                log.warn("Received null response for event [{}]", eventId);
            }

            return response;

        } catch (Exception e) {
            log.error("Error fetching event data for [{}]", eventId, e);
            return null;
        }
    }
}
